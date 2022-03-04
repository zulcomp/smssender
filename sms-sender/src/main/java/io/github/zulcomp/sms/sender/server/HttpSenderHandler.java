/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.zulcomp.sms.sender.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;

/**
 *
 * @author XPMUser
 */

public class HttpSenderHandler extends AbstractHandler {

    private static final Logger LOGGER = LogManager.getLogger("com.fsc.sm.smssender");
    SmsSenderWorker worker;

    HttpSenderHandler(SmsSenderWorker worker)  {
        if(worker == null) throw new NullPointerException();
        this.worker = worker;
    }

    @Override
    public void handle(String string, Request rqst, HttpServletRequest hsr, HttpServletResponse hsr1) throws IOException, ServletException {
        boolean senderOk = true;

        LOGGER.info("Getting HTTP Request from {} ", hsr.getRemoteHost());
        LOGGER.info("Request line {}", hsr.getQueryString());
        LOGGER.info("request.getMethod: {}", hsr.getMethod());
        if ("GET".equals(hsr.getMethod())) {
            try {
                String qparam = hsr.getParameter("smsmshist_id");
                if (qparam != null && !"".equals(qparam)) {
                    worker.addSmSmsHistId(qparam);
                }
            } catch (Exception e) {
                senderOk = false;
            }
        }
        hsr1.setContentType("text/plain");
        hsr1.setStatus(HttpServletResponse.SC_OK);
        if (senderOk) {
            hsr1.getWriter().println("SMSSender-OK");
        } else {
            hsr1.getWriter().println("SMSSender-Error");
        }
        rqst.setHandled(true);
    }
}
