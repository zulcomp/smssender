/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.github.zulcomp.sms.sender.setup;

import io.github.zulcomp.sms.sender.common.db.DBConnection;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author Faizul
 */
public class SMSSenderDBConfigurator {

    private static final String SMS_SENDER_SVR_URL = "SMSSENDER_SVR_URL";
    private static final String SMS_SENDER_PATH = "SMSSENDER_PATH";
    private static final String SQL_ADD_SETTING = "INSERT INTO GNPARAMETER "
            + "( PARAMETER_CODE, PARAMETER_CAT, VALUE, PARAMETER_DESC, MODULE, "
            + "PARAMETER_RANGE, ACTIVE_IND, LAST_UPDATED_BY, LAST_UPDATED_DATETIME ) VALUES"
            + " (?,'SYS',?, ?, 'SM', NULL, 'Y', 3,  SYSDATE)";
    private static final String SQL_UPDATE_SETTING = "update gnparameter set value=? where parameter_code=?";
    private static final String SQL_COUNT_SETTING = "SELECT COUNT(*) AS NCNT FROM GNPARAMETER WHERE PARAMETER_CODE=?";

    private static DBConnection dbc = null;

    private SMSSenderDBConfigurator() {}

    private static InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException {
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = en.nextElement();
            for (Enumeration<InetAddress> en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                InetAddress addr = en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (preferIPv6 && addr instanceof Inet4Address ||
                            preferIpv4 && addr instanceof Inet6Address) {
                        continue;
                    }
                    return addr;
                }
            }
        }
        return null;
    }

    public static int doConfig(Properties connProp, String arg) {
        int configResult = 0;
        try {
            dbc = new DBConnection(connProp);
            boolean noserver = checkNoServerArguments(arg);
            String svrUrl = populateServerUrl();

            if (existSetting(SMS_SENDER_SVR_URL)) {
                updateSetting(SMS_SENDER_SVR_URL, noserver ? "-" : svrUrl);
            } else {
                insertSetting(SMS_SENDER_SVR_URL,"SmsSender Server URL", noserver ? "-" : svrUrl);
            }

            String path = checkSmsSenderInstalledPath();
            if (existSetting(SMS_SENDER_PATH)) {
                updateSetting(SMS_SENDER_PATH,  path);
            } else {
                insertSetting(SMS_SENDER_PATH,"SmsSender Install Path", path);
            }

        } catch (Exception er) {
            System.out.println("Error: " + er.getMessage());
            configResult = -1;
        } finally {
            try {
                if (dbc != null) {
                    dbc.closeConnection();
                }
            } catch (SQLException er) {
                System.out.println("Connection Close Error: " + er.getMessage());
            }
        }
        return configResult;
    }

    private static String checkSmsSenderInstalledPath() throws IOException {
        File f = new File("src/main/resources/smssender.properties");
        String path = "";
        if (f.exists()) {
            //get full path
            path = f.getCanonicalPath();
            path = path.substring(0, path.indexOf("src/main/resources/smssender.properties"));
            path = path + "src/main/resources/smssender.bat";
        }
        return path;
    }

    private static String populateServerUrl() throws SocketException {
        String svrUrl = "http://@ip@:8888/?smsmshist_id=";
        //check ip address of this machine
        InetAddress ipAdds = getFirstNonLoopbackAddress(true, false);
        if (ipAdds != null) {
            svrUrl = svrUrl.replace("@ip@", ipAdds.getHostAddress());
        }
        return svrUrl;
    }

    private static void insertSetting(String settingName,String settingDesc, String value) throws SQLException {

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("1", settingName);
        paramMap.put("2", value);
        paramMap.put("3", settingDesc);
        dbc.scalarQuery(SQL_ADD_SETTING, paramMap);
    }

    private static void updateSetting(String settingName, String value) throws SQLException {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("1", value);
        paramMap.put("2", settingName);
        dbc.scalarQuery(SQL_UPDATE_SETTING, paramMap);
    }

    private static boolean existSetting(String settingName) throws SQLException {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("1", settingName);
        List<Map<Integer,Object>> result = dbc.query(SQL_COUNT_SETTING, paramMap);
        if (!result.isEmpty()) {
            Map<Integer,Object> mp = result.get(0);
            return ((BigDecimal) mp.get(1)).intValue() > 0;
        }
        return false;
    }

    private static boolean checkNoServerArguments(String arg) {
        try {
            return Integer.parseInt(arg) > 0;
        } catch (NumberFormatException nfe) {
            return true; // return default
        }
    }
}
