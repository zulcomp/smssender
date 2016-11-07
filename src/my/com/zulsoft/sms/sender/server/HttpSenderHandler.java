/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.com.zulsoft.sms.sender.server;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 *
 * @author XPMUser
 */

public class HttpSenderHandler extends AbstractHandler {

    private static final Logger logger = Logger.getLogger("com.fsc.sm.smssender");
    SmsSenderWorker worker;

    HttpSenderHandler(SmsSenderWorker worker) throws SQLException, ClassNotFoundException {
        if(worker == null) throw new NullPointerException();
        this.worker = worker;
    }

    
    public void handle(String string, Request rqst, HttpServletRequest hsr, HttpServletResponse hsr1) throws IOException, ServletException {
        boolean senderOk = true;

        logger.info("Getting HTTP Request from " + hsr.getRemoteHost());
        logger.info("Request line " + hsr.getQueryString());
        logger.info("request.getMethod:" + hsr.getMethod());
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
