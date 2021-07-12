package io.github.zulcomp.sms.sender.setup;

import java.util.Properties;


public class SmsSenderSetup
{
    public static void main( String[] args )
    {
        Properties properties = new Properties();
        SMSSenderDBConfigurator.doConfig(properties,args[0]);
    }
}
