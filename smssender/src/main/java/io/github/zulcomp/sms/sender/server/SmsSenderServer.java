/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.zulcomp.sms.sender.server;

import io.github.zulcomp.sms.sender.common.db.DBConnection;
import io.github.zulcomp.sms.sender.client.SMSClient;
import io.github.zulcomp.sms.sender.client.SMSSendStatusChangeEvent;
import io.github.zulcomp.sms.sender.client.SMSSendStatusChangeListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;

/**
 *
 * @author XPMUser
 */

public class SmsSenderServer {

    private static final Logger LOGGER = LogManager.getLogger("my.com.zulsoft.sms.sender.server");
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
