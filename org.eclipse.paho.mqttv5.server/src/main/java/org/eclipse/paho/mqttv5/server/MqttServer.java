package org.eclipse.paho.mqttv5.server;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.paho.mqttv5.server.config.Configuration;
import org.eclipse.paho.mqttv5.server.config.MqttServerConfiguration;
import org.eclipse.paho.mqttv5.server.listener.ListenerController;

public class MqttServer 
{
	private static final Logger LOG = Logger.getLogger(MqttServer.class.getName());
	
	private Configuration configuration;
	ListenerController listenerController;
	
	public MqttServer(){
		 try {
				MqttServerConfiguration config = new MqttServerConfiguration();
				configuration = config.getConfiguration();
				
				listenerController = new ListenerController(configuration.getListeners());
			} catch (JAXBException e) {
				LOG.severe("Exception occured whilst loading configuration: " + e.getLocalizedMessage());
				System.exit(1);
			}
		 
		 // Start Admin Interface?
	        
	     // Start Broker
	        
	     // Start Listener Controller
	    
	}
	
    public static void main( String[] args )
    {
    	LOG.info("Starting MQTTv5 Server, Hello World!");
    	
    	MqttServer mqttServer =  new MqttServer();     
        
        
    }
}
