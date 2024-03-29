/**
 * @(#)SerialConnection.java
 *
 *
 * @author Faizul Ngsrimin
 * @version 1.00 2012/11/19
 */
package io.github.zulcomp.sms.sender.common;

import gnu.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.TooManyListenersException;

/**
A class that handles the details of a serial connection. 
Holds the state of the connection.
 */
public class SerialConnection implements SerialPortEventListener,
        CommPortOwnershipListener {

    private static final Logger LOGGER = LogManager.getLogger("my.com.zulsoft.sms.common");
    private static final String PORT_OWNER_NAME="SMSSenderApps";
    private final SerialParameters parameters;
    private OutputStream os;
    private InputStream is;
    private SerialPort sPort;
    private boolean open;
    private String receptionString = "";
    CommPortIdentifier portId;

    public String getIncommingString() {
        byte[] bVal = receptionString.getBytes();
        receptionString = "";
        return new String(bVal);
    }

    /**
    Creates a SerialConnection object and initializes variables passed in
    as parameters.

    @param parameters A SerialParameters object.
     */
    public SerialConnection(SerialParameters parameters) {
        this.parameters = parameters;
        open = false;
    }

    /**
    Attempts to open a serial connection and streams using the parameters
    in the SerialParameters object. If it is  unsuccessful at any step it
    returns the port to a closed state, throws a
    <code>SerialConnectionException</code>, and returns.

    Gives a timeout of 30 seconds on the portOpen to allow other applications
    to reliquish the port if have it open and no longer need it.
     * @throws io.github.zulcomp.sms.sender.common.SerialConnectionException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     */
    public void openConnection() throws SerialConnectionException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

	String driverName = "gnu.io.RXTXCommDriver"; 
        CommDriver commDriver = (CommDriver) Class.forName(driverName).getConstructor().newInstance();
        commDriver.initialize();

        // Obtain a CommPortIdentifier object for the port you want to open.

        try {
            portId = CommPortIdentifier.getPortIdentifier(parameters.getPortName());
        } catch (NoSuchPortException e) {
            throw new SerialConnectionException(e.getMessage());
        }
        // check if this port is already open
        if(portId.isCurrentlyOwned() && PORT_OWNER_NAME.equals(portId.getCurrentOwner()) && sPort != null) {
                sPort.close();
        }
        // Open the port represented by the CommPortIdentifier object. Give
        // the open call a relatively long timeout of 30 seconds to allow
        // a different application to reliquish the port if the user
        // wants to.
        try {
            sPort = portId.open(PORT_OWNER_NAME, 30000);
        } catch (PortInUseException e) {
            throw new SerialConnectionException(e.getMessage());
        }
        sPort.sendBreak(1000);

        // Set the parameters of the connection. If they won't set, close the
        // port before throwing an exception.
        try {
            setConnectionParameters();
        } catch (SerialConnectionException e) {
            sPort.close();
            throw e;
        }
        // Open the input and output streams for the connection. If they won't
        // open, close the port before throwing an exception.
        try {
            os = sPort.getOutputStream();
            is = sPort.getInputStream();
        } catch (IOException e) {
            sPort.close();
            throw new SerialConnectionException("Error opening i/o streams " + e.getMessage());
        }

        // Add this object as an event listener for the serial port.
        try {
            sPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            sPort.close();
            throw new SerialConnectionException("too many listeners added " + e.getMessage());
        }
        // Set notifyOnDataAvailable to true to allow event driven input.
        sPort.notifyOnDataAvailable(true);

        // Set notifyOnBreakInterrup to allow event driven break handling.
        sPort.notifyOnBreakInterrupt(true);

        // Set receive timeout to allow breaking out of polling loop during
        // input handling.
        try {
            sPort.enableReceiveTimeout(30);
        } catch (UnsupportedCommOperationException e) {
            LOGGER.error(e.getMessage());
        }
        // Add ownership listener to allow ownership event handling.
        portId.addPortOwnershipListener(this);

        open = true;
    }

    /**
    Sets the connection parameters to the setting in the parameters object.
    If set fails return the parameters object to original settings and
    throw exception.
     * @throws io.github.zulcomp.sms.sender.common.SerialConnectionException
     */
    public void setConnectionParameters() throws SerialConnectionException {

        // Save state of parameters before trying a set.
        int oldBaudRate = sPort.getBaudRate();
        int oldDatabits = sPort.getDataBits();
        int oldStopbits = sPort.getStopBits();
        int oldParity = sPort.getParity();

        // Set connection parameters, if set fails return parameters object
        // to original state.
        try {
            sPort.setSerialPortParams(parameters.getBaudRate(),
                    parameters.getDatabits(),
                    parameters.getStopbits(),
                    parameters.getParity());
        } catch (UnsupportedCommOperationException e) {
            parameters.setBaudRate(oldBaudRate);
            parameters.setDatabits(oldDatabits);
            parameters.setStopbits(oldStopbits);
            parameters.setParity(oldParity);
            throw new SerialConnectionException("Unsupported parameter");
        }

        // Set flow control.
        try {
            sPort.setFlowControlMode(parameters.getFlowControlIn() | parameters.getFlowControlOut());
        } catch (UnsupportedCommOperationException e) {
            throw new SerialConnectionException("Unsupported flow control");
        }
    }

    /**
    Close the port and clean up associated elements.
     */
    public void closeConnection() {
        // If port is alread closed just return.
        if (!open) {
            return;
        }

        // Check to make sure sPort has reference to avoid a NPE.
        if (sPort != null) {
            try {
                // close the i/o streams.
                os.close();
                is.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
			
			sPort.removeEventListener();
            // Close the port.
            sPort.close();

            // Remove the ownership listener.
            portId.removePortOwnershipListener(this);
        }

        open = false;
    }

    /**
    Send a one second break signal.
     */
    public void sendBreak() {
        sPort.sendBreak(1000);
    }

    /**
    Reports the open status of the port.
    @return true if port is open, false if port is closed.
     */
    public boolean isOpen() {
        return open;
    }

    /**
    Handles SerialPortEvents. The two types of SerialPortEvents that this
    program is registered to listen for are DATA_AVAILABLE and BI. During
    DATA_AVAILABLE the port buffer is read until it is drained, when no more
    data is available and 30 milliseconds has passed the method returns. When a BI
    event occurs the words BREAK RECEIVED are written to the messageAreaIn.
     * @param e
     */
    @Override
    public void serialEvent(SerialPortEvent e) {
        StringBuilder inputBuffer = new StringBuilder();
        int newData = 0;

        // Determine type of event.

        int eventType = e.getEventType();// Read data until -1 is returned. If \r is received substitute
// \n for correct newline handling.
        if (eventType == SerialPortEvent.DATA_AVAILABLE) {
            while (newData != -1) {
                try {
                    newData = is.read();
                    if (newData == -1) {
                        break;
                    }
                    if ('\r' == (char) newData) {
                        inputBuffer.append('\n');
                    } else {
                        inputBuffer.append((char) newData);
                    }
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage());
                    return;
                }
            }

            // Append received data to messageAreaIn.
            receptionString = receptionString + (new String(inputBuffer));

            // If break event append BREAK RECEIVED message.
        } else if (eventType == SerialPortEvent.BI) {
            receptionString = receptionString + ("\n--- BREAK RECEIVED ---\n");
        }
    }

    /**
    Handles ownership events. If a PORT_OWNERSHIP_REQUESTED event is
    received a dialog box is created asking the user if they are
    willing to give up the port. No action is taken on other types
    of ownership events.
     * @param type
     */
    @Override
    public void ownershipChange(int type) {

        if (type == CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED && portId.isCurrentlyOwned() && !PORT_OWNER_NAME.equals(portId.getCurrentOwner()) && sPort!=null) {
            sPort.close();
        }
    }


    public void send(String message) {
        byte[] theBytes = (message + (char)13 + (char)10).getBytes();
        for (int i = 0; i < theBytes.length; i++) {

            char newCharacter = (char) theBytes[i];
            try {
                os.write(newCharacter);
            } catch (IOException e) {
                LOGGER.error("OutputStream write error: {} ", e.getMessage());
            }

        }
    }
}
