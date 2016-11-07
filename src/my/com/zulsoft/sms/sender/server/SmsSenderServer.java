/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.com.zulsoft.sms.sender.server;

import my.com.zulsoft.sms.sender.common.db.DBConnection;
import my.com.zulsoft.sms.sender.client.SMSClient;
import my.com.zulsoft.sms.sender.client.SMSSendStatusChangeEvent;
import my.com.zulsoft.sms.sender.client.SMSSendStatusChangeListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

/**
 *
 * @author XPMUser
 */
class SmsSenderWorker extends Thread implements SMSSendStatusChangeListener {

    private static final Logger LOGGER = Logger.getLogger("my.com.zulsoft.sms.server");

    Properties param;
    DBConnection dbconn;
    SMSClient smsclient;
    LinkedBlockingQueue idqueue;

    public SmsSenderWorker(Properties p) throws SQLException, ClassNotFoundException {
        param = p;
        dbconn = new DBConnection(p);
        smsclient = new SMSClient(SMSClient.SYNCHRONOUS);
        smsclient.addChangeListener((SMSSendStatusChangeListener) this);
        idqueue = new LinkedBlockingQueue();
    }

    @Override
    public void run() {
        while (true) 
        {
            String smshist_id = (String) idqueue.peek();
            boolean needToInsertAgain = false;
            if (smshist_id != null) {
                LOGGER.info("run:smsmshist_id:" + smshist_id);
                Runtime r = Runtime.getRuntime();
                try {
                    Process p = r.exec("java -jar smssender.jar " + smshist_id, null);
                    LOGGER.info("Creating Process : " + p.toString());
                    int exitVal = p.waitFor();
                    LOGGER.info("Finished Process with exit code: " + exitVal);

                } catch (IOException ex) {
                    LOGGER.info(ex.getLocalizedMessage());
                } catch (InterruptedException ie) {
                    LOGGER.info(ie.getLocalizedMessage());
                }
                //check database if msssage has been send
                String sql = "SELECT SEND_IND FROM SMSMSHIST WHERE SMSMSHIST_ID=?";
                HashMap mp = new HashMap();
                mp.put("1", smshist_id);
                try {
                    ArrayList<HashMap> rset = dbconn.query(sql, mp);
                    if (!rset.isEmpty()) {
                        if (rset.size() == 1) {
                            HashMap map = rset.get(0);
                            String send_ind = (String) map.get(1);
                            LOGGER.info("Send Indicator for SMSMSHIST_ID " + smshist_id + " = " + send_ind);
                            if ("Y".equals(send_ind)) {
                                idqueue.poll();
                                needToInsertAgain = false;
                            } else {
                                idqueue.poll();
                                needToInsertAgain = true;
                            }
                        }
                    }
                } catch (SQLException ex) {
                    LOGGER.info(ex.getLocalizedMessage());
                }

            }

            if (needToInsertAgain) {
                if (smshist_id != null) {
                    addSmSmsHistId(smshist_id);
                }
            }

        }
    }

    @Override
    public void sendStatusChanged(SMSSendStatusChangeEvent e) {
        HashMap data = e.getResult();
        String status = (String) data.get("status");
        String id = (String) data.get("id");

        if (Integer.parseInt(status) == 0) {
            //update to 'Y'
            String lSqlUpd = param.getProperty("query_update_smshist");
            LOGGER.info("Getting sql query: " + lSqlUpd);
            try {
                HashMap p = new HashMap();
                p.put("1", id);
                int updtCnt = dbconn.scalaQuery(lSqlUpd, p);
                if (updtCnt <= 0) {
                    LOGGER.error("Can't update SMS History for Id " + id);
                }
            } catch (SQLException ex) {
                LOGGER.info(ex.getMessage(), ex);
            }
        } else {
            LOGGER.error("SMS Sending Error for SMS History Id " + id);
        }
        LOGGER.info("Send SMS Id " + id + " with Status " + (status.equals("0") ? "OK" : "Failed"));
        String idpool = (String) idqueue.poll();
        if (!idpool.equals(id)) {
            LOGGER.error("smsmshist_id " + id + " is not the same as " + idpool);
        }
    }

    public long sendMessageById() throws SQLException, ClassNotFoundException {

        String smsmshist_id = (String) idqueue.peek();
        LOGGER.info("sendMessageById:smsmshist_id:" + smsmshist_id);
        if (smsmshist_id == null) {
            return -1L;
        }
        String sql = param.getProperty("query_by_id"); //get sql statement in property files
        LOGGER.info("Getting sql query: " + sql);
        HashMap mp = new HashMap();
        mp.put("1", smsmshist_id);
        ArrayList<HashMap> rset = dbconn.query(sql, mp);

        param.put("id", smsmshist_id); //for use in SMSClient.sendMessage
        String mobileNum = "";
        String message = "";

        if (!rset.isEmpty()) {
            if (rset.size() == 1) {
                HashMap m = (HashMap) rset.get(0);
                mobileNum = (String) m.get(new Integer(1)); //MOBILE_PHONE
                message = (String) m.get(new Integer(2)); // SMSMSHIST_MESSAGE
                LOGGER.info("mobile_Number=" + mobileNum);
                LOGGER.info("message=" + message);
                message = message.replace("\\r\\n", String.valueOf(((char) 13)));
                if (mobileNum == null || "".equals(mobileNum)) {
                    LOGGER.warn("Mobile Number not found for SMS History Id " + smsmshist_id);
                    idqueue.poll();
                    return -1L;
                }
            } else {
                LOGGER.info("To Many SMSHistory data for id " + smsmshist_id);
                idqueue.poll();
                return -1L;
            }

            int status = smsclient.sendMessageAndBlock(mobileNum, message, param);

            if (status == 0) {
                //update to 'Y', any sql error need to remove the smshist_id.
                String lSqlUpd = param.getProperty("query_update_smshist");
                LOGGER.info("Getting sql query: " + lSqlUpd);
                try {
                    HashMap p = new HashMap();
                    p.put("1", smsmshist_id);
                    int updtCnt = dbconn.scalaQuery(lSqlUpd, p);
                    if (updtCnt <= 0) {
                        LOGGER.error("Can't update SMS History for Id " + smsmshist_id);
                        idqueue.poll();
                        return -1;
                    }
                } catch (SQLException ex) {
                    LOGGER.info(ex.getMessage(), ex);
                    idqueue.poll();
                    return -1;
                }
            }
            LOGGER.info("Send SMS Id " + smsmshist_id + " with Status " + (status == 0 ? "OK" : "Failed"));
            String idpool = (String) idqueue.poll();
            if (!idpool.equals(smsmshist_id)) {
                LOGGER.error("smsmshist_id " + smsmshist_id + " is not the same as " + idpool);
                return -1;
            }
            //return 0;
            return (status == 0 ? ((long) status) : Long.parseLong(smsmshist_id));
        } else {
            //should not return empty?? some thing wrong with the sql , remove this id from queue
            idqueue.poll();
            return -1;
        }
        //return (status == 0L ? ((long) status) : Long.parseLong(smsmshist_id));
    }

    public void addSmSmsHistId(String id) {
        if (!idqueue.contains(id)) {
            if (idqueue.offer(id)) {
                LOGGER.info("SMS id: " + id + " waiting to be send...");
                LOGGER.info("Waiting line " + idqueue.size());
            }
        }
    }

}

public class SmsSenderServer {

    private static final Logger LOGGER = Logger.getLogger("my.com.zulsoft.sms.server");
    private Properties p;
    private int httpPort = 8888;
    private Server server;

    public SmsSenderServer(Properties p) {
        if (p == null) {
            throw new java.lang.NullPointerException();
        }
        this.p = p;
    }

    public void start() throws InterruptedException, SQLException, ClassNotFoundException, Exception {
        httpPort = Integer.parseInt(p.getProperty("httpPort"));
        SmsSenderWorker smsworker = new SmsSenderWorker(p);
        smsworker.start();

        HttpSenderHandler handler = new HttpSenderHandler(smsworker);
        server = new Server(httpPort);
        server.setHandler(handler);
        LOGGER.info("SMSSender Server started (Port " + httpPort + ")...");
        server.start();
        server.join();
    }
}
