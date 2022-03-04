package io.github.zulcomp.sms.sender.server;

import io.github.zulcomp.sms.sender.client.SMSClient;
import io.github.zulcomp.sms.sender.client.SMSSendStatusChangeEvent;
import io.github.zulcomp.sms.sender.client.SMSSendStatusChangeListener;
import io.github.zulcomp.sms.sender.common.db.DBConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

public class SmsSenderWorker extends Thread implements SMSSendStatusChangeListener {

    private static final Logger LOGGER = LogManager.getLogger("my.com.zulsoft.sms.sender.server");

    Properties param;
    DBConnection dbconn;
    SMSClient smsclient;
    LinkedBlockingQueue<String> idqueue;

    public SmsSenderWorker(Properties p) throws SQLException, ClassNotFoundException {
        param = p;
        dbconn = new DBConnection(p);
        smsclient = new SMSClient(SMSClient.SYNCHRONOUS);
        smsclient.addChangeListener( this);
        idqueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        while (true)
        {
            String smshistId = idqueue.peek();
            boolean needToInsertAgain = false;
            if (smshistId != null) {
                try {
                    runSmsSender(smshistId);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                needToInsertAgain = checkMessageIsSend(smshistId);
            }
            if (needToInsertAgain) {
                addSmSmsHistId(smshistId);
            }
        }
    }

    private void runSmsSender(String smshistId) throws IOException, InterruptedException {
        LOGGER.info("run:smsmshist_id: {}",  smshistId);
        Runtime r = Runtime.getRuntime();
        Process p = r.exec("java -jar smssender.jar " + smshistId, null);
        LOGGER.info("Creating Process : {}", p);
        int exitVal = p.waitFor();
        LOGGER.info("Finished Process with exit code: {}", exitVal);
    }

    private boolean checkMessageIsSend(String smshistId) {
        //check database if msssage has been send
        String sql = "SELECT SEND_IND FROM SMSMSHIST WHERE SMSMSHIST_ID=?";
        Map<String, String> mp = new HashMap<>();
        mp.put("1", smshistId);
        boolean neeToSendAgain = true;
        try {
            List<Map<Integer,Object>> rset = dbconn.query(sql, mp);
            if (rset.size() == 1) {
                    Map<Integer,Object> map = rset.get(0);
                    String sendInd = (String) map.get(1);
                    LOGGER.info("Send Indicator for SMSMSHIST_ID {} = {}", smshistId , sendInd);
                    if ("Y".equals(sendInd)) {
                        idqueue.poll();
                        neeToSendAgain = false;
                    } else {
                        idqueue.poll();
                    }
            }
        } catch (SQLException ex) {
            LOGGER.info(ex.getLocalizedMessage());
        }
        return neeToSendAgain;
    }

    @Override
    public void sendStatusChanged(SMSSendStatusChangeEvent e) {
        Map<String,String> data = e.getResult();
        String status = data.get("status");
        String id = data.get("id");

        if (Integer.parseInt(status) == 0) {
            //update to 'Y'
            String lSqlUpd = param.getProperty("query_update_smshist");
            LOGGER.trace("Getting sql query: {}", lSqlUpd);
            try {
                Map<String,String> p = new HashMap<>();
                p.put("1", id);
                int updtCnt = dbconn.scalarQuery(lSqlUpd, p);
                if (updtCnt <= 0) {
                    LOGGER.error("Can't update SMS History for Id {} ", id);
                }
            } catch (SQLException ex) {
                LOGGER.info(ex.getMessage(), ex);
            }
        } else {
            LOGGER.error("SMS Sending Error for SMS History Id {}", id);
        }
        LOGGER.info("Send SMS Id {} with Status {}", id,  (status.equals("0") ? "OK" : "Failed"));
        String idpool = idqueue.poll();
        assert idpool != null;
        if (!idpool.equals(id)) {
            LOGGER.error("smsmshist_id {} is not the same as {}",id , idpool);
        }
    }

    public long sendMessageById() throws SQLException, InterruptedException {

        String smsmshistId = idqueue.peek();
        LOGGER.info("sendMessageById:smsmshist_id: {} ", smsmshistId);
        if (smsmshistId == null) {
            return -1L;
        }
        String sql = param.getProperty("query_by_id"); //get sql statement in property files
        LOGGER.info("Getting sql query: [{}]",sql);
        Map<String,String> mp = new HashMap<>();
        mp.put("1", smsmshistId);
        List<Map<Integer,Object>> rset = dbconn.query(sql, mp);

        param.put("id", smsmshistId); //for use in SMSClient.sendMessage
        String mobileNum;
        String message;

        if (!rset.isEmpty()) {
            if (rset.size() == 1) {
                Map<Integer, Object> m = rset.get(0);
                mobileNum = (String) m.get(1); //MOBILE_PHONE
                message = (String) m.get(2); // SMSMSHIST_MESSAGE
                LOGGER.info("mobile_Number= {}", mobileNum);
                LOGGER.info("message= {} ", message);
                message = message.replace("\\r\\n", String.valueOf(((char) 13)));
                if (mobileNum == null || "".equals(mobileNum)) {
                    LOGGER.warn("Mobile Number not found for SMS History Id {}", smsmshistId);
                    idqueue.poll();
                    return -1L;
                }
            } else {
                LOGGER.info("To Many SMSHistory data for id {}",  smsmshistId);
                idqueue.poll();
                return -1L;
            }

            int status = smsclient.sendMessageAndBlock(mobileNum, message, param);

            if (status == 0) {
                //update to 'Y', any sql error need to remove the smshist_id.
                String lSqlUpd = param.getProperty("query_update_smshist");
                LOGGER.info("Getting sql query: {}",  lSqlUpd);
                try {
                    Map<String, String> p = new HashMap<>();
                    p.put("1", smsmshistId);
                    int updtCnt = dbconn.scalarQuery(lSqlUpd, p);
                    if (updtCnt <= 0) {
                        LOGGER.error("Can't update SMS History for Id {}",  smsmshistId);
                        idqueue.poll();
                        return -1;
                    }
                } catch (SQLException ex) {
                    LOGGER.info(ex.getMessage(), ex);
                    idqueue.poll();
                    return -1;
                }
            }
            LOGGER.info("Send SMS Id {} with Status {} ",smsmshistId, (status == 0 ? "OK" : "Failed"));
            String idpool = idqueue.poll();
            assert idpool != null;
            if (!idpool.equals(smsmshistId)) {
                LOGGER.error("smsmshist_id {} is not the same as {}",smsmshistId , idpool);
                return -1;
            }
            return (status == 0 ? ((long) status) : Long.parseLong(smsmshistId));
        } else {
            //should not return empty?? some thing wrong with the sql , remove this id from queue
            idqueue.poll();
            return -1;
        }
    }

    public void addSmSmsHistId(String id) {
        if (!idqueue.contains(id) && idqueue.offer(id)) {
                LOGGER.info("SMS id: {} waiting to be send...", id);
                LOGGER.info("Waiting line {}", idqueue.size());
        }
    }

}
