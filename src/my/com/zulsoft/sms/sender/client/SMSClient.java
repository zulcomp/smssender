/*
 * 
 * @author : William Alexander, Faizul Ngsrimin
 *
 */
package my.com.zulsoft.sms.sender.client;

import my.com.zulsoft.sms.sender.common.Sender;
import my.com.zulsoft.sms.sender.common.SerialParameters;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.HashMap;
import java.util.ListIterator;

public class SMSClient implements Runnable
{
    private static final Logger LOGGER = Logger.getLogger("my.com.zulsoft.sms.client");
    private final ArrayList changeListenerObj;
    public final static int SYNCHRONOUS = 0;
    public final static int ASYNCHRONOUS = 1;
    private Thread myThread = null;
    private int mode = -1;
    private String recipient = null;
    private String message = null;
    private SerialParameters serialParam = null;
    private String id;
    public int status = -1;
    public long messageNo = -1;

    public SMSClient(int mode) {
        this.mode = mode;
        changeListenerObj = new ArrayList();
    }

    public int sendMessageAndBlock(String recipient, String message, Properties pSend) {
        status = -1;
        //get all listener object and remove all SMSSendStatusChangeListener
        Object[] l = changeListenerObj.toArray();
        changeListenerObj.removeAll(changeListenerObj);
        //logger.info("Is changeListenerObj empty? " + changeListenerObj.isEmpty());
        this.recipient = recipient;
        this.message = message;
        this.id = pSend.getProperty("id"); 
        if(this.id == null) this.id ="";
        
        if (serialParam == null) {
            serialParam = new SerialParameters();
        }
        
        if (serialParam.getPortName() == null) {
            serialParam.setPortName(pSend.getProperty("portName"));
            serialParam.setBaudRate(pSend.getProperty("baudRate"));
            serialParam.setDatabits(pSend.getProperty("databits"));
            serialParam.setFlowControlIn(pSend.getProperty("flowControlIn"));
            serialParam.setFlowControlOut(pSend.getProperty("flowControlOut"));
            serialParam.setParity(pSend.getProperty("parity"));
            serialParam.setStopbits(pSend.getProperty("stopbits"));
        }
        myThread = new Thread(this);
        myThread.start();
        
        //while(status == -1)
        while(true)
        {
            try {
                myThread.join();
            } catch (InterruptedException ex) {
                
            }

            if(!myThread.isAlive()) {
                myThread = null;
                break;
            }
        }

        changeListenerObj.addAll(Arrays.asList(l));

        return status;
    }

    public int sendMessage(String recipient, String message, Properties pSend) {

        this.recipient = recipient;
        this.message = message;
        this.id = (pSend.getProperty("id") == null ? "" : pSend.getProperty("id"));
        if (serialParam == null) {
            serialParam = new SerialParameters();
            serialParam.setPortName(pSend.getProperty("portName"));
            serialParam.setBaudRate(pSend.getProperty("baudRate"));
            serialParam.setDatabits(pSend.getProperty("databits"));
            serialParam.setFlowControlIn(pSend.getProperty("flowControlIn"));
            serialParam.setFlowControlOut(pSend.getProperty("flowControlOut"));
            serialParam.setParity(pSend.getProperty("parity"));
            serialParam.setStopbits(pSend.getProperty("stopbits"));
        }
        
        myThread = new Thread(this);
        myThread.start();
        return status;
    }

    @Override
    public void run() {

        Sender aSender = new Sender(recipient, message, serialParam, null);
        //int retry=0;
        //do {
            try {
        //        int timeoutRetry=3;
        //        do {
                    //send message
                    aSender.send();

                    //in SYNCHRONOUS mode wait for return : 0 for OK, -2 for timeout, -1 for other errors
                    if (mode == SYNCHRONOUS) {
                        while (aSender.status == -1) {
                            Thread.sleep(1000);
                        }
                    }
                    if (aSender.status == 0) {
                        messageNo = aSender.messageNo;
                    }

                    if(aSender.status==-2) {
          //              timeoutRetry = timeoutRetry - 1;
          //              logger.info("Timeout! Retry sending!!");
         //               Thread.sleep(1000);
                    }
           //     } while(timeoutRetry !=0 && aSender.status == -2);
           //     break;
            } catch (Exception e) {
                LOGGER.info(e.getMessage());
        //        if(e instanceof SerialConnectionException) {
        //            logger.info("Unable to open port. Retry sending!!");
        //            retry+=1;
        //            try {
        //                Thread.sleep(1000);
        //            } catch (InterruptedException ex) {
                        //ignore exception
        //            }
               // }
            }
        //} while(retry !=3);
        this.status = aSender.status;
        aSender = null;
        fireStatusChange();
    }

    public void addChangeListener(SMSSendStatusChangeListener cl) {
        changeListenerObj.add(cl);
    }

    public void removeChangeListerner(Object cl) {
        changeListenerObj.remove(cl);
    }

    private void fireStatusChange() {
        HashMap mp = new HashMap();
        mp.put("status", String.valueOf(this.status));
        mp.put("id", this.id);
        mp.put("messageNo", String.valueOf(messageNo));
        
        SMSSendStatusChangeEvent e = new SMSSendStatusChangeEvent(this, mp);
        ListIterator li = changeListenerObj.listIterator();
        while (li.hasNext()) {
            ((SMSSendStatusChangeListener) li.next()).sendStatusChanged(e);
        }
    }
}