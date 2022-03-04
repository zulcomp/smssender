/*
 * 
 * @author : William Alexander, Faizul Ngsrimin
 *
 */
package io.github.zulcomp.sms.sender.client;

import io.github.zulcomp.sms.sender.common.Sender;
import io.github.zulcomp.sms.sender.common.SerialParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class SMSClient implements Runnable
{
    private static final Logger LOGGER = LogManager.getLogger("my.com.zulsoft.sms.sender.client");
    private final ArrayList<SMSSendStatusChangeListener> changeListenerObj;
    public static final  int SYNCHRONOUS = 0;
    public static final  int ASYNCHRONOUS = 1;
    private Thread myThread = null;
    private final int mode;
    private String recipient = null;
    private String message = null;
    private SerialParameters serialParam = null;
    private String id;
    private int status = -1;
    private long messageNo = -1;

    public SMSClient(int mode) {
        this.mode = mode;
        changeListenerObj = new ArrayList<>();
    }

    public int sendMessageAndBlock(String recipient, String message, Properties pSend) throws InterruptedException {
        status = -1;
        //get all listener object and remove all SMSSendStatusChangeListener
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
        
        while(true)
        {
            myThread.join();
            if(!myThread.isAlive()) {
                myThread = null;
                break;
            }
        }

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
            try {
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

            } catch (Exception e) {
                LOGGER.info(e.getMessage());
            }
        this.status = aSender.status;
        if(mode == ASYNCHRONOUS) fireStatusChange();
    }

    public void addChangeListener(SMSSendStatusChangeListener cl) {
        changeListenerObj.add(cl);
    }

    public void removeChangeListener(Object cl) {
        changeListenerObj.remove(cl);
    }

    private void fireStatusChange() {
        HashMap<String, String> mp = new HashMap<>();
        mp.put("status", String.valueOf(this.status));
        mp.put("id", this.id);
        mp.put("messageNo", String.valueOf(messageNo));
        
        SMSSendStatusChangeEvent e = new SMSSendStatusChangeEvent(this, mp);
        for (SMSSendStatusChangeListener smsSendStatusChangeListener : changeListenerObj) {
            smsSendStatusChangeListener.sendStatusChanged(e);
        }
    }
}