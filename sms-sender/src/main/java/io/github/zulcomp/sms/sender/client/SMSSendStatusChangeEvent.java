/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.zulcomp.sms.sender.client;

import javax.swing.event.ChangeEvent;
import java.util.Map;

/**
 *
 * @author XPMUser
 */
public class SMSSendStatusChangeEvent extends ChangeEvent {

    Map<String,String> data;

    public SMSSendStatusChangeEvent(Object src, Map<String,String> data) {
        super(src);
        this.data = data;
    }

    public Map<String,String> getResult() {
        return data;
    }
}
