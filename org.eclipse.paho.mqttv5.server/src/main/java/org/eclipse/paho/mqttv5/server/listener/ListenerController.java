/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.server.listener;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.server.config.ListenerConfiguration;

public class ListenerController {
	
	private static final Logger LOG = Logger.getLogger(ListenerController.class.getName());

	
	private List<Runnable> listeners;
	

	public ListenerController(List<ListenerConfiguration> listenerConfigurations){
		
		LOG.info("Initialising Listeners...");
		 for(ListenerConfiguration listenerConfiguration : listenerConfigurations){
	    	 if(listenerConfiguration.getType() == "mqtt"){
	    		 listeners.add(new MqttListener(listenerConfiguration));
	    	 }
	     }
		 
		 for(Runnable listener : listeners){
			 listener.run();
		 }
	}
}
