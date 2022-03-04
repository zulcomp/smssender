/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.zulcomp.sms.sender.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;

import java.util.Properties;

/**
 *
 * @author XPMUser
 */

public class SmsSenderServer {

    private static final Logger LOGGER = LogManager.getLogger("my.com.zulsoft.sms.sender.server");
    private Properties p;

    public SmsSenderServer(Properties p) {
        if (p == null) {
            throw new java.lang.NullPointerException();
        }
        this.p = p;
    }

    public void start() throws Exception {
        int httpPort = Integer.parseInt(p.getProperty("httpPort"));
        SmsSenderWorker smsworker = new SmsSenderWorker(p);
        smsworker.start();

        HttpSenderHandler handler = new HttpSenderHandler(smsworker);
        Server server = new Server(httpPort);
        server.setHandler(handler);
        LOGGER.info("SMSSender Server started (Port {})...", httpPort);
        server.start();
        server.join();
    }
}
