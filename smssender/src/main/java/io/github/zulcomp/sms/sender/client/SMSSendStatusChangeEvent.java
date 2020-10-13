/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.zulcomp.sms.sender.client;

import java.util.HashMap;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author XPMUser
 */
public class SMSSendStatusChangeEvent extends ChangeEvent {

    HashMap<String,String> data;

    public SMSSendStatusChangeEvent(Object src, HashMap<String,String> data) {
        super(src);
        this.data = data;
    }

    public HashMap<String,String> getResult() {
        return data;
    }
}
