/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.com.zulsoft.sms.sender.client;

import java.util.HashMap;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author XPMUser
 */
public class SMSSendStatusChangeEvent extends ChangeEvent {

    HashMap data;

    public SMSSendStatusChangeEvent(Object src, HashMap data) {
        super(src);
        this.data = data;
    }

    public HashMap getResult() {
        return data;
    }
}
