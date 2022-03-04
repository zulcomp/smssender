/* @(#)SmsSender.java
 *
 * smssender application
 *
 * @author Faizul Ngsrimin
 * @version 1.00 2012/11/19
 */
package io.github.zulcomp.sms.sender;

import io.github.zulcomp.sms.sender.client.SMSClient;
import io.github.zulcomp.sms.sender.client.SMSSendStatusChangeEvent;
import io.github.zulcomp.sms.sender.common.db.DBConnection;
import io.github.zulcomp.sms.sender.server.SmsSenderServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SmsSender {

    private static final Logger LOGGER = LogManager.getLogger("com.fsc.sm.smssender");
    private final DBConnection dbconn;
    private final SMSClient smsclient;
    private final Properties param;
    private int sendStatus = -1;

    public SmsSender(Properties p) throws SQLException, ClassNotFoundException {
        param = p;
        dbconn = new DBConnection(p);
        smsclient = new SMSClient(SMSClient.SYNCHRONOUS);
        smsclient.addChangeListener((SMSSendStatusChangeEvent e) -> {
            Map<String,String> data = e.getResult();
            String status = data.get("status");
            String id = data.get("id");
            if (Integer.parseInt(status) == 0) {
                //update to 'Y'
                String lSqlUpd = param.getProperty("query_update_smshist");
                LOGGER.trace("Getting sql query: {}",lSqlUpd);
                try {
                    Map<String, String> p1 = new HashMap<>();
                    p1.put("1", id);
                    int updtCnt = dbconn.scalarQuery(lSqlUpd, p1);
                    if (updtCnt <= 0) {
                        LOGGER.error("Can't update SMS History for Id " + id);
                    }
                }catch (SQLException ex) {
                    LOGGER.info(ex.getMessage(), ex);
                }
            } else {
                LOGGER.error("SMS Sending Error for SMS History Id " + id);
            }
            LOGGER.info("Send SMS Id " + id + " with Status " + (status.equals("0") ? "OK" : "Failed"));
            sendStatus = Integer.parseInt(status);
        });
    }

    public void sendMessageById(String smsmshistId) throws SQLException {

        //this.param = p; //store current parameter
        String sql = param.getProperty("query_by_id"); //get sql statement in property files
        LOGGER.trace("Getting sql query: {}", sql);
        Map<String,String> mp = new HashMap<>();
        mp.put("1", smsmshistId);
        List<Map<Integer, Object>> rset = dbconn.query(sql, mp);
        param.put("id", smsmshistId); //for use in SMSClient.sendMessage

        String mobileNum;
        String message;

        if (!rset.isEmpty()) {
            if (rset.size() == 1) {

                Map<Integer,Object> m =  rset.get(0);
                mobileNum = (String) m.get(1); //MOBILE_PHONE
                message = (String) m.get(2); // SMSMSHIST_MESSAGE
                LOGGER.trace("mobile_Number= {}", mobileNum);
                LOGGER.trace("message={}", message);
                message = message.replace("\\r\\n", String.valueOf(((char) 13)));
                if (mobileNum == null || "".equals(mobileNum)) {
                    LOGGER.warn("Mobile Number not found for SMS History Id {}",  smsmshistId);
                    return;
                }
            } else {
                LOGGER.warn("To Many SMSHistory data for id {}",  smsmshistId);
                return;
            }

            int status = smsclient.sendMessageAndBlock(mobileNum, message, param);
            Map<String,String> hm = new HashMap<>();
            hm.put("id", smsmshistId);
            hm.put("status", String.valueOf(status));
        }

    }

    public void close() throws SQLException {
        dbconn.closeConnection();
        smsclient.removeChangeListener(this);
    }

    public void sendUnsendMessage() throws SQLException, ClassNotFoundException, IOException {
        //
        //this.param = p; //store current parameter
        String sql = param.getProperty("query_all_unsend"); //get sql statement in property files
        LOGGER.trace("Getting sql query: {}",  sql);
        String sqlInterval = param.getProperty("query_interval");
        LOGGER.trace("Getting sql query: {}",  sqlInterval);
        //get interval
        int interval = 0;
        List<Map<Integer, Object>> rset = dbconn.query(sqlInterval, null);
        if (rset.size() == 1) {
            Map<Integer, Object> mf = rset.get(0);
            Object obj = mf.get(1);
            if (obj == null) {
                LOGGER.warn("Can't find Interval value in GNPARAMETER table");
                return;
            }

            if (obj instanceof BigDecimal) {
                interval = ((BigDecimal) obj).intValue();
            } else {
                interval = Integer.parseInt(((String) obj));
            }
        }
        LOGGER.trace("Get Interval from DB {}",  interval);
        Map<String,String> mIn = new HashMap<>();
        //get current date and format it as string
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, interval);
        mIn.put("1", sdf.format(cal.getTime()));
        rset = dbconn.query(sql, mIn);

        String mobileNum = "";
        String message = "";
        String smsmshistId = "";
        int sendCnt = 0;
        if (!rset.isEmpty()) {
            int rCnt = rset.size();
            for (int c = 0; c < rCnt; c++) {
                Map<Integer,Object> m = rset.get(c);
                smsmshistId = ((BigDecimal) m.get(1)).toPlainString(); //SMSMSHIST_ID
                mobileNum = (String) m.get(2); //MOBILE_PHONE
                message = (String) m.get(3); //SMSMSHIST_MESSAGE
                message = message.replace("\\r\\n", String.valueOf(((char) 13)));
                if (mobileNum == null || "".equals(mobileNum)) {
                    LOGGER.warn("Mobile Number not found for SMS History Id {}",  smsmshistId);
                    continue;
                }

                Runtime r = Runtime.getRuntime();
                try {
                    Process p = r.exec("java -jar smssender.jar " + smsmshistId, null);
                    LOGGER.trace("Creating Process : {}",  p);
                    int exitVal = p.waitFor();
                    LOGGER.trace("Finished Process with exit code: {}",  exitVal);

                } catch (IOException | InterruptedException ex) {
                    LOGGER.debug(ex.getLocalizedMessage());
                }
                //check database if msssage has been send

                String sqls = "SELECT SEND_IND FROM SMSMSHIST WHERE SMSMSHIST_ID=?";
                Map<String,String> mp = new HashMap<>();
                mp.put("1", smsmshistId);
                try {
                    List<Map<Integer, Object>> rsets = dbconn.query(sqls, mp);
                    if (rsets.size() == 1) {
                            Map<Integer, Object> map = rsets.get(0);
                            String sendInd = (String) map.get(1);
                            LOGGER.trace("Send Indicator for SMSMSHIST_ID {} = {}",  smsmshistId , sendInd);
                            if ("Y".equals(sendInd)) {
                                sendCnt = sendCnt + 1;
                            }
                    }
                } catch (SQLException sqle) {
                    LOGGER.debug(sqle.getLocalizedMessage());
                }
            }
        }
        LOGGER.trace("Total SMS Message {}", rset.size());
        LOGGER.trace("Total SMS Message Send {}",  sendCnt);

    }

    public static void main(String[] args) {

        SmsSender sender = null;

        try {
            //check properties files for default options
            Properties p = new Properties();
            p.load(new FileInputStream(new File("smssender.properties")));
            switch (args.length) {
                case 1:
                    if ("server".equalsIgnoreCase(args[0])) {
                        SmsSenderServer senderServer = new SmsSenderServer(p);
                        senderServer.start();
                    } else {
                        sender = new SmsSender(p);
                        sender.sendMessageById(args[0]);
                    }   break;
                case 0:
                    //run by checking unsend item and send it
                    sender = new SmsSender(p);
                    sender.sendUnsendMessage();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            try {
                if (sender != null) {
                    sender.close();
                }
            } catch (SQLException ex) {
                LOGGER.info(ex.getMessage());
            }
        }
        try {
            if (sender != null) {
                sender.close();
            }
        } catch (SQLException ex) {
            LOGGER.info(ex.getMessage(), ex);
        }
        System.exit(0);
    }

    /**
     * @return the sendStatus
     */
    public int getSendStatus() {
        return sendStatus;
    }
}
