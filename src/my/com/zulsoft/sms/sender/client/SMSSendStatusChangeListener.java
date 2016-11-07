/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.com.zulsoft.sms.sender.client;

/**
 *
 * @author XPMUser
 */
public interface SMSSendStatusChangeListener {

    void sendStatusChanged(SMSSendStatusChangeEvent e);
}
