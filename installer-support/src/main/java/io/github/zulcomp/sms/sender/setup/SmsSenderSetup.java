package io.github.zulcomp.sms.sender.setup;

import java.util.Properties;


public class SmsSenderSetup
{
    public static void main( String[] args )
    {
        Properties properties = new Properties();
        //get from environment variable or populated from args[1],args[2]....
        System.exit(SMSSenderDBConfigurator.doConfig(properties,args[0]));
    }
}
