/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.util;

import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

/**
 * Utility to help debug problems with the Paho MQTT client
 * Once initialised a call to dumpClientDebug will force any memory trace
 * together with pertinent client and system state to the main log facility.
 * 
 * No client wide lock is taken when the dump is progress. This means the 
 * set of client state may not be consistent as the client can still be 
 * processing work while the dump is in progress.
 */
public class Debug {
	
	private static final String CLASS_NAME = ClientComms.class.getName();
	private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,CLASS_NAME);
	private static final String separator = "==============";
	private static final String lineSep = System.getProperty("line.separator","\n");
	
	private String clientID;
	private ClientComms comms;
	
	/**
	 * Set the debug facility up for a specific client
	 * @param clientID  the ID of the client being debugged
	 * @param comms    the ClientComms object of the client being debugged
	 */
	public Debug(String clientID, ClientComms comms) {
		this.clientID = clientID;
		this.comms = comms;
		log.setResourceName(clientID);
	}

	/**
	 * Dump maximum debug info.
	 * This includes state specific to a client as well 
	 * as debug that is JVM wide like trace and system properties.
	 * All state is written as debug log entries. 
	 */
	public void dumpClientDebug() { 
		dumpClientComms();
		dumpConOptions();
		dumpClientState();
		dumpBaseDebug();
	}
	
	/**
	 * Dump of JVM wide debug info.
	 * This includes trace and system properties.
	 * Includes trace and system properties
	 */
	public void dumpBaseDebug() {
		dumpVersion();
		dumpSystemProperties();
		dumpMemoryTrace();
	}

	/**
	 * If memory trace is being used a request is made to push it 
	 * to the target handler.
	 */
	protected void dumpMemoryTrace() {
		log.dumpTrace();
	}
	
	/**
	 * Dump information that show the version of the MQTT client being used.
	 */
	protected void dumpVersion() {
		StringBuffer vInfo = new StringBuffer();
    	vInfo.append(lineSep+separator+" Version Info "+ separator+lineSep);
    	vInfo.append(left("Version",20,' ') + ":  "+ ClientComms.VERSION + lineSep);
    	vInfo.append(left("Build Level",20,' ') + ":  "+ ClientComms.BUILD_LEVEL + lineSep);
    	vInfo.append(separator+separator+separator+lineSep);
    	log.fine(CLASS_NAME,"dumpVersion", vInfo.toString());
	}

	/**
	 * Dump the current set of system.properties to a log record
	 */
	public void dumpSystemProperties() {
		
	    Properties sysProps = System.getProperties();
    	log.fine(CLASS_NAME,"dumpSystemProperties", dumpProperties(sysProps, "SystemProperties").toString());
	}

	/**
	 * Dump interesting variables from ClientState
	 */
	public void dumpClientState() {
		Properties props = null;
	    if (comms != null && comms.getClientState() != null ) {
	    	props = comms.getClientState().getDebug();
	    	log.fine(CLASS_NAME,"dumpClientState", dumpProperties(props, clientID + " : ClientState").toString());
	    }
	}

	/**
	 * Dump interesting variables from ClientComms
	 */
	public void dumpClientComms() {
		Properties props = null;
	    if (comms != null) {
	    	props = comms.getDebug();
	    	log.fine(CLASS_NAME,"dumpClientComms", dumpProperties(props, clientID + " : ClientComms").toString());
	    }
	}
	
	/**
	 * Dump Connection options
	 */
	public void dumpConOptions() {
		Properties props = null;
	    if (comms != null) {
	    	props = comms.getConOptions().getDebug();
	    	log.fine(CLASS_NAME,"dumpConOptions", dumpProperties(props, clientID + " : Connect Options").toString());
	    }
	}


	/**
	 * Return a set of properties as a formatted string
	 * @param props The Dump Properties
	 * @param name The associated name 
	 * @return a set of properties as a formatted string
	 */
	public static String dumpProperties(Properties props, String name) {
		
		StringBuffer propStr = new StringBuffer();
	    Enumeration propsE = props.propertyNames();
    	propStr.append(lineSep+separator+" "+name+" "+ separator+lineSep);
	    while (propsE.hasMoreElements()) {
	    	String key = (String)propsE.nextElement();
	    	propStr.append(left(key,28,' ') + ":  "+ props.get(key)+lineSep);
	    }
    	propStr.append(separator+separator+separator+lineSep);

    	return propStr.toString();
	}
	
	/**
	   * Left justify a string.
	   *
	   * @param s the string to justify
	   * @param width the field width to justify within
	   * @param fillChar the character to fill with
	   *
	   * @return the justified string.
	   */
	  public static String left(String s, int width, char fillChar) {
	    if (s.length() >= width) {
	      return s;
	    }
	    StringBuffer sb = new StringBuffer(width);
	    sb.append(s);
	    for (int i = width - s.length(); --i >= 0;) {
	      sb.append(fillChar);
	    }
	    return sb.toString();
	  }
	
}
