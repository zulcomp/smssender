/*
 * 
 * @author : William Alexander, Faizul Ngsrimin
 * @updates : 2012/11/19 (faizul) - add PDU support for message > 160 char
 */
package io.github.zulcomp.sms.sender.common;

import java.util.Date;
import java.util.List;
import java.util.Random;
import org.ajwcc.pduUtils.gsm3040.PduFactory;
import org.ajwcc.pduUtils.gsm3040.PduGenerator;
import org.ajwcc.pduUtils.gsm3040.PduUtils;
import org.ajwcc.pduUtils.gsm3040.SmsSubmitPdu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class Sender implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger("io.github.zulcomp.sms.sender.common");
    private static final long STANDARD = 500;
    private static final long LONG = 2000;
    private static final long VERYLONG = 20000;
    SerialConnection mySerial = null;
    static final private char CNTRLZ = (char) 26;
    String in, out;
    Thread aThread = null;
    private long delay = STANDARD;
    String recipient = null;
    String message = null;
    private String csca = ""; // the message center -- use sim card default
    private final SerialParameters serialParam;
    public int step;
    public int status = -1;
    public long messageNo = -1;

    public Sender(String recipient, String message, SerialParameters serialParam, String cscaNum) {

        this.recipient = recipient;
        this.message = message;
        this.serialParam = serialParam;
        this.csca = cscaNum;
    }

    /**
     * connect to the port and start the dialogue thread
     * @throws java.lang.Exception
     */
    public void send() throws Exception {

        mySerial = new SerialConnection(serialParam);
        try {
            mySerial.openConnection();
        } catch(ClassNotFoundException | IllegalAccessException | InstantiationException | SerialConnectionException e) {
            LOGGER.info(e.getMessage() + " : Port Open " +  mySerial.isOpen());
            throw e;
        }
        aThread = new Thread(this);

        aThread.start();
        log("sender thread start");

    }

    /**
     * implement the dialogue thread,
     * message / response via steps,
     * handle time out
     */
    @Override
    public void run() {
        status = -1;
        boolean timeOut = false;
        long startTime = (new Date()).getTime();
        boolean usePDUMode = (message.length() > 150);
        int currentPduStrIdx = 0;
        List<String> pduString = null;
        SmsSubmitPdu pdu;
        int msgRef = 0;
        if(message.length() > 160) {
            pdu = PduFactory.newSmsSubmitPdu(PduUtils.TP_UDHI_WITH_UDH | PduUtils.TP_SRR_REPORT | PduUtils.TP_VPF_INTEGER);
        } else {
            pdu = PduFactory.newSmsSubmitPdu(PduUtils.TP_SRR_REPORT | PduUtils.TP_VPF_INTEGER);
        }
        PduGenerator pdugen = new PduGenerator();
        Random rnd = new Random();
        if(usePDUMode) {
            //SmsSubmitPdu pdu = null;
            //PduGenerator pdugen = null;
            //byte[] udData = {0x00,(byte)(message.length()/160),0x01};
            //pdugen = new PduGenerator();
            //pdu = PduFactory.newSmsSubmitPdu();
            //pdu = PduFactory.newSmsSubmitPdu(PduUtils.TP_UDHI_WITH_UDH);
            //ConcatInformationElement concat =
            //    (ConcatInformationElement) InformationElementFactory.createInformationElement(
            //                               ConcatInformationElement.CONCAT_8BIT_REF, udData);
            //pdu.addInformationElement(concat);
            //pdu.setSmscInfoLength(1+(csca.length()/2));
            //pdu.setSmscAddress(csca);
            pdu.setValidityPeriod(12);
            pdu.setMessageReference(0);
            pdu.setDataCodingScheme(PduUtils.DCS_ENCODING_7BIT);
            pdu.setProtocolIdentifier(0);
            pdu.setAddress(recipient);
            pdu.setDecodedText(message);
            msgRef=rnd.nextInt(254) + 1;
            pduString = pdugen.generatePduList(pdu, msgRef);
            LOGGER.info("PDU Strings count " + (pduString == null ? 0 : pduString.size()));

        }

        while ((step < 7) && (!timeOut)) {
            log("Timeout in " + ((new Date()).getTime() - startTime));
            //check where we are in specified delay
            timeOut = ((new Date()).getTime() - startTime) > delay;

            //if atz does not work, type to send cntrlZ and retry, in case a message was stuck
            if (timeOut && (step == 1)) {
                step = -1;
                mySerial.send("        " + CNTRLZ);
            }

            //read incoming string
            String result = mySerial.getIncommingString();

            //log("<- " + result + "\n--------");
            int expectedResult; //= -1;

            try {
                //log("Step:" + step);

                switch (step) {
                    case 0:

                        mySerial.send("atz");
                        delay = LONG;
                        startTime = (new Date()).getTime();
                        break;

                    case 1:
                        delay = STANDARD;
                        mySerial.send("ath0");
                        startTime = (new Date()).getTime();
                        break;
                    case 2:
                        expectedResult = result.indexOf("OK");

                        //log("received ok =" + expectedResult);
                        if (expectedResult > -1) {
                            if(usePDUMode)
                                mySerial.send("at+cmgf=0");
                            else
                                mySerial.send("at+cmgf=1");

                            startTime = (new Date()).getTime();
                            if(usePDUMode) {step = step + 1;  } //skip step 3 if using pdu mode
                        } else {
                            step = step - 1;
                        }
                        break;
                    case 3:

                        expectedResult = result.indexOf("OK");

                        //log("received ok =" + expectedResult);
                        if (expectedResult > -1) {
                            int n = result.indexOf("+CSCA:");
                            if (n > -1) {
                                //use default SMS Center
                                csca = result.substring(n + 6, result.indexOf(",")).trim();
                                csca = csca.replace("\"","");
                                LOGGER.info("csca variable set to " + csca);
                            }

                            if (csca == null || "".equals(csca)) {
                                //get default sms center from sim
                                mySerial.send("at+csca?");
                                startTime = (new Date()).getTime();
                                step = step - 1;

                            } else {
                                if(usePDUMode) {
                                    //pdu.setSmscInfoLength(1+(csca.length()/2));
                                    //pdu.setSmscAddress(csca);
                                    pduString = pdugen.generatePduList(pdu, msgRef);
                                    LOGGER.info("PDU Strings count " + (pduString == null ? 0 : pduString.size()));
            
                                } else {
                                    mySerial.send("at+csca=\"" + csca + "\"");
                                    startTime = (new Date()).getTime();
                                }
                            }
                        } else {
                            step = step - 1;
                        }
                        break;
                    case 4:
                        expectedResult = result.indexOf("OK");
                        //simulate expectedResult if usePDUMode=true just for step 4
                        //if(usePDUMode) expectedResult=0;
                        //log("received ok =" + expectedResult);
                        if (expectedResult > -1) {
                            if(usePDUMode) {
                                LOGGER.info("---Sending message ref: "+ msgRef +" part "+ (currentPduStrIdx+1));
                                int pdulength = pduString.get(currentPduStrIdx).length();
                                mySerial.send("at+cmgs="  + ((int)(pdulength/2) - 1));
                            }
                            else
                                mySerial.send("at+cmgs=\"" + recipient + "\"");
                            startTime = (new Date()).getTime();
                        } else {
                            step = step - 1;
                        }
                        break;

                    case 5:
                        expectedResult = result.indexOf(">");

                        //log("received ok =" + expectedResult);
                        if (expectedResult > -1) {
                            if(usePDUMode) {
                                String pduStr = pduString.get(currentPduStrIdx);
                                mySerial.send(pduStr + CNTRLZ);
                            } else
                                mySerial.send(message + CNTRLZ);
                            startTime = (new Date()).getTime();
                        } else {
                            step = step - 1;
                        }
                        delay = VERYLONG;//waitning for message ack
                        break;

                    case 6:
                        expectedResult = result.indexOf("OK");
                        //read message number
                        if (expectedResult > -1) {
                            int n = result.indexOf("CMGS:");
                            result = result.substring(n + 5);
                            n = result.indexOf("\n");
                            status = 0;
                            messageNo = Long.parseLong(result.substring(0, n).trim());

                            log("sent message no:" + messageNo);

                            if(usePDUMode) {
                                if(pduString!=null)
                                {
                                    currentPduStrIdx = currentPduStrIdx + 1;
                                    if(!pduString.isEmpty() && currentPduStrIdx <  pduString.size() )
                                    {
                                        LOGGER.info("---Sending message ref: "+ msgRef +" part "+ (currentPduStrIdx+1));
                                        int pdulength = pduString.get(currentPduStrIdx).length();

                                        mySerial.send("at+cmgs="  + ((int)(pdulength/2) - 1));
                                        step = step - 2; // goto step 5 to submit another multipart message
                                    }
                                }
                            }
                        } else {
                            expectedResult = result.indexOf("ERROR");
                            if(expectedResult > -1) {
                                status = -3;
                            } else {
                                step = step - 1;
                            }
                        }
                        break;
                }
                step = step + 1;

                Thread.sleep(100);

            } catch (InterruptedException | NumberFormatException e) {
            }
        }

        mySerial.closeConnection();
        mySerial = null;
        //if timed out set status

        if (timeOut) {
            status = -2;
            log("*** time out at step " + step + "***");
        }
    }

    /**
     * logging function, includes date and class name
     */
    private void log(String s) {
        //System.out.println (new java.util.Date()+":"+this.getClass().getName()+":"+s);
        LOGGER.info(s);
    }
}