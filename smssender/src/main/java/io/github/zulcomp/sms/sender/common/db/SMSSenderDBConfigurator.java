/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.github.zulcomp.sms.sender.common.db;

import java.io.File;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author Faizul
 */
public class SMSSenderDBConfigurator {
    
    

    private static InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException {
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) en.nextElement();
            for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                InetAddress addr = (InetAddress) en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        return addr;
                    }
                    if (addr instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        return addr;
                    }
                }
            }
        }
        return null;
    }

    public static void doConfig(Properties connProp, String arg) {
        DBConnection dbc = null;// = new DBConnection(connProp);
        try {
            //dbc.openConnection(connProp);
            dbc = new DBConnection(connProp);
            boolean noserver;
            try {
                noserver = Integer.parseInt(arg) == 1;
            } catch (NumberFormatException nfe) {
                noserver = true;
            }
            //check if SMSSENDER_SVR_URL exists
            String sqlCnt = "SELECT COUNT(*) AS NCNT FROM GNPARAMETER WHERE PARAMETER_CODE=?";
            String sqlAddParam = "INSERT INTO GNPARAMETER "
                    + "( PARAMETER_CODE, PARAMETER_CAT, VALUE, PARAMETER_DESC, MODULE, "
                    + "PARAMETER_RANGE, ACTIVE_IND, LAST_UPDATED_BY, LAST_UPDATED_DATETIME ) VALUES"
                    + " (?,'SYS',?, ?, 'SM', NULL, 'Y', 3,  SYSDATE)";

            String sqlUpdateParam = "update gnparameter set value=? where parameter_code=?";
            String svrUrl = "http://@ip@:8888/?smsmshist_id=";
            //check ip address of this machine
            InetAddress ipAdds = getFirstNonLoopbackAddress(true, false);
            if (ipAdds != null) {
                svrUrl = svrUrl.replaceAll("@ip@", ipAdds.getHostAddress());
            }

            HashMap paramMap = new HashMap();
            paramMap.put("1", "SMSSENDER_SVR_URL");

            ArrayList<HashMap> result = dbc.query(sqlCnt, paramMap);
            if (result.size() > 0) {
                HashMap mp = result.get(0);
                Integer c = ((BigDecimal) mp.get(new Integer(1))).intValue();
                if (c == 0) {
                    //add SMSSENDER_SVR_URL
                    paramMap.clear();
                    paramMap.put("1", "SMSSENDER_SVR_URL");
                    if (noserver) {
                        paramMap.put("2", "-");
                    } else {
                        paramMap.put("2", svrUrl);
                    }
                    paramMap.put("3", "SmsSender Server URL");
                    dbc.scalaQuery(sqlAddParam, paramMap);
                } else {
                    //update SMSSENDER_SVR_URL
                    paramMap.clear();
                    if (noserver) {
                        paramMap.put("1", "-");
                    } else {
                        paramMap.put("1", svrUrl);
                    }
                    paramMap.put("2", "SMSSENDER_SVR_URL");
                    dbc.scalaQuery(sqlUpdateParam, paramMap);
                }
            } else {
                // add SMSSENDER_SVR_URL parameter
                paramMap.clear();
                paramMap.put("1", "SMSSENDER_SVR_URL");
                if (noserver) {
                    paramMap.put("2", "-");
                } else {
                    paramMap.put("2", svrUrl);
                }
                paramMap.put("3", "SmsSender Server URL");
                dbc.scalaQuery(sqlAddParam, paramMap);
            }

            // SMSSENDER_PATH
            paramMap.clear();
            paramMap.put("1", "SMSSENDER_PATH");
            result = dbc.query(sqlCnt, paramMap);
            if (result.size() > 0) {
                HashMap mp = result.get(0);
                Integer c = ((BigDecimal) mp.get(new Integer(1))).intValue();
                if (c == 0) {
                    paramMap.clear();
                    paramMap.put("1", "SMSSENDER_PATH");
                    File f = new File("src/main/resources/smssender.properties");
                    if (f.exists()) {
                        //get full path
                        String path = f.getCanonicalPath();
                        path = path.substring(0, path.indexOf("src/main/resources/smssender.properties"));
                        path = path + "src/main/resources/smssender.bat";
                        paramMap.put("2", path);
                    }
                    paramMap.put("3", "SmsSender Install Path");
                    dbc.scalaQuery(sqlAddParam, paramMap);
                } else {
                    paramMap.clear();
                    paramMap.put("2", "SMSSENDER_PATH");
                    File f = new File("src/main/resources/smssender.properties");
                    if (f.exists()) {
                        //get full path
                        String path = f.getCanonicalPath();
                        path = path.substring(0, path.indexOf("src/main/resources/smssender.properties"));
                        path = path + "src/main/resources/smssender.bat";
                        paramMap.put("1", path);
                    }
                    dbc.scalaQuery(sqlUpdateParam, paramMap);
                }
            } else {
                paramMap.clear();
                paramMap.put("1", "SMSSENDER_PATH");
                File f = new File("src/main/resources/smssender.properties");
                if (f.exists()) {
                    //get full path
                    String path = f.getCanonicalPath();
                    path = path.substring(0, path.indexOf("src/main/resources/smssender.properties"));
                    paramMap.put("2", path);
                }
                paramMap.put("3", "SmsSender Install Path");
                dbc.scalaQuery(sqlAddParam, paramMap);

            }
        } catch (Exception er) {
            try {
                if (dbc != null) {
                    dbc.closeConnection();
                }
            } catch (SQLException ex) {
                //java.util.logging.LogManager.getLogger(SmsSender.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(-1);
        } finally {
            try {
                if (dbc != null) {
                    dbc.closeConnection();
                }
            } catch (SQLException ex) {
                //java.util.logging.LogManager.getLogger(SmsSender.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
