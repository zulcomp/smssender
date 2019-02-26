/* @(#)SmsSender.java
 *
 * smssender application
 *
 * @author Faizul Ngsrimin
 * @version 1.00 2012/11/19
 */
package my.com.zulsoft.sms.sender;

import my.com.zulsoft.sms.sender.common.db.SMSSenderDBConfigurator;
import my.com.zulsoft.sms.sender.client.SMSSendStatusChangeEvent;
import my.com.zulsoft.sms.sender.common.db.DBConnection;
import my.com.zulsoft.sms.sender.client.SMSClient;
import my.com.zulsoft.sms.sender.server.SmsSenderServer;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.logging.log4j.Logger;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import org.apache.logging.log4j.LogManager;

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
            HashMap data = e.getResult();
            String status = (String) data.get("status");
            String id = (String) data.get("id");
            if (Integer.parseInt(status) == 0) {
                //update to 'Y'
                String lSqlUpd = param.getProperty("query_update_smshist");
                LOGGER.info("Getting sql query: " + lSqlUpd);
                try {
                    HashMap p1 = new HashMap();
                    p1.put("1", id);
                    int updtCnt = dbconn.scalaQuery(lSqlUpd, p1);
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

    public void sendMessageById(String smsmshist_id) throws SQLException, ClassNotFoundException {

        //this.param = p; //store current parameter
        String sql = param.getProperty("query_by_id"); //get sql statement in property files
        LOGGER.info("Getting sql query: " + sql);
        HashMap mp = new HashMap();
        mp.put("1", smsmshist_id);
        ArrayList<HashMap> rset = dbconn.query(sql, mp);
        param.put("id", smsmshist_id); //for use in SMSClient.sendMessage

        String mobileNum;
        String message;

        if (!rset.isEmpty()) {
            if (rset.size() == 1) {

                HashMap m = (HashMap) rset.get(0);
                mobileNum = (String) m.get(1); //MOBILE_PHONE
                message = (String) m.get(2); // SMSMSHIST_MESSAGE
                LOGGER.info("mobile_Number=" + mobileNum);
                LOGGER.info("message=" + message);
                message = message.replace("\\r\\n", String.valueOf(((char) 13)));
                if (mobileNum == null || "".equals(mobileNum)) {
                    LOGGER.warn("Mobile Number not found for SMS History Id " + smsmshist_id);
                    return;
                }
            } else {
                LOGGER.info("To Many SMSHistory data for id " + smsmshist_id);
                return;
            }

            //smsclient.sendMessage(mobileNum, message, param);
            int status = smsclient.sendMessageAndBlock(mobileNum, message, param);
            HashMap hm = new HashMap();
            hm.put("id", smsmshist_id);
            hm.put("status", String.valueOf(status));
            //SMSSendStatusChangeEvent e = new SMSSendStatusChangeEvent(this, hm);
            //sendStatusChanged(e);
        }

    }

    public void close() throws SQLException {
        dbconn.closeConnection();
        smsclient.removeChangeListerner(this);
    }

    public void sendUnsendMessage() throws SQLException, ClassNotFoundException, IOException {
        //
        //this.param = p; //store current parameter
        String sql = param.getProperty("query_all_unsend"); //get sql statement in property files
        LOGGER.info("Getting sql query: " + sql);
        String sqlInterval = param.getProperty("query_interval");
        LOGGER.info("Getting sql query: " + sqlInterval);
        //get interval
        int interval = 0;
        ArrayList<HashMap> rset = dbconn.query(sqlInterval, null);
        if (rset.size() == 1) {
            HashMap mf = rset.get(0);
            Object obj = mf.get(1);
            if (obj == null) {
                LOGGER.warn("Can't find Interval value in GNPARAMETER table");
                return;
            }

            if (java.math.BigDecimal.class.isInstance(obj)) {
                interval = ((BigDecimal) obj).intValue();
            } else {
                interval = Integer.parseInt(((String) obj));
            }
        }
        LOGGER.warn("Get Interval from DB " + interval);
        HashMap mIn = new HashMap();
        //get current date and format it as string
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, interval);
        mIn.put("1", sdf.format(cal.getTime()));
        rset = dbconn.query(sql, mIn);

        String mobileNum = "";
        String message = "";
        String smsmshist_id = "";
        int sendCnt = 0;
        if (!rset.isEmpty()) {
            int rCnt = rset.size();
            for (int c = 0; c < rCnt; c++) {
                HashMap m = (HashMap) rset.get(c);
                smsmshist_id = ((BigDecimal) m.get(1)).toPlainString(); //SMSMSHIST_ID
                mobileNum = (String) m.get(2); //MOBILE_PHONE
                message = (String) m.get(3); //SMSMSHIST_MESSAGE
                message = message.replace("\\r\\n", String.valueOf(((char) 13)));
                if (mobileNum == null || "".equals(mobileNum)) {
                    LOGGER.warn("Mobile Number not found for SMS History Id " + smsmshist_id);
                    continue;
                }

                Runtime r = Runtime.getRuntime();
                try {
                    Process p = r.exec("java -jar smssender.jar " + smsmshist_id, null);
                    LOGGER.info("Creating Process : " + p.toString());
                    int exitVal = p.waitFor();
                    LOGGER.info("Finished Process with exit code: " + exitVal);

                } catch (IOException | InterruptedException ex) {
                    //java.util.logging.LogManager.getLogger(SmsSenderWorker.class.getName()).log(Level.SEVERE, null, ex);
                    LOGGER.info(ex.getLocalizedMessage());
                }
                //java.util.logging.LogManager.getLogger(SmsSenderWorker.class.getName()).log(Level.SEVERE, null, ie);
                //check database if msssage has been send
                
                //check database if msssage has been send

                String sqls = "SELECT SEND_IND FROM SMSMSHIST WHERE SMSMSHIST_ID=?";
                HashMap mp = new HashMap();
                mp.put("1", smsmshist_id);
                try {
                    ArrayList<HashMap> rsets = dbconn.query(sqls, mp);
                    if (!rsets.isEmpty()) {
                        if (rsets.size() == 1) {
                            HashMap map = rsets.get(0);
                            String send_ind = (String) map.get(new Integer(1));
                            LOGGER.info("Send Indicator for SMSMSHIST_ID " + smsmshist_id + " = " + send_ind);
                            if ("Y".equals(send_ind)) {
                                sendCnt = sendCnt + 1;
                            }
                        }
                    }
                } catch (SQLException sqle) {
                    LOGGER.info(sqle.getLocalizedMessage());
                }
                /*
                 param.put("id", smsmshist_id);
                 int status = smsclient.sendMessageAndBlock(mobileNum, message, param);
                 HashMap hm = new HashMap();
                 hm.put("id", smsmshist_id);
                 hm.put("status", String.valueOf(status));
                 SMSSendStatusChangeEvent e = new SMSSendStatusChangeEvent(this,hm);
                 sendStatusChanged(e);
                 */

            }
        }
        LOGGER.info("Total SMS Message " + rset.size());
        LOGGER.info("Total SMS Message Send" + sendCnt);

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
                case 2:
                    if ("config".equals(args[0])) {
                        SMSSenderDBConfigurator.doConfig(p, args[1]);
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
