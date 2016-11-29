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
package org.eclipse.paho.mqttv5.server.config;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;;


public class MqttServerConfiguration {
	
	private Configuration configuration;
	JAXBContext jc = JAXBContext.newInstance(Configuration.class);

	public MqttServerConfiguration() throws JAXBException{
		File configurationFile = new File("src/main/resources/configuration.xml");
		if(configurationFile.exists()){
			loadConfiguration(configurationFile);
		}
	}

	
	private void loadConfiguration(File configurationFile) throws JAXBException{
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		this.configuration = (Configuration) unmarshaller.unmarshal(configurationFile);
	}
	
	private void saveConfiguration(File configurationFile) throws JAXBException{
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
		marshaller.marshal(this.configuration, configurationFile);
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
}
