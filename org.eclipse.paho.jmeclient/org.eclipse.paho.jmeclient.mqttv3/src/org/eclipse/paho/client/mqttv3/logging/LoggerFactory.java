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
 *    Adam Clark - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.logging;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.util.PropertyResourceBundle;

/**
 * A factory that returns a logger for use by the MQTT JME client. 
 * 
 * The default log and trace facility uses Java's build in log facility:-
 * com.oracle.util.logging..  For systems where this is not available or where
 */
public class LoggerFactory {
	/**
	 * Default message catalog.
	 */
	public final static String MQTT_CLIENT_MSG_CAT = "org.eclipse.paho.client.mqttv3.internal.nls.logcat";
	
	/**
	 * Default logger that uses com.oracle.util.logging. 
	 */
	private static String DefaultloggerClassName = "org.eclipse.paho.client.mqttv3.logging.Logging"; 
	private static String overrideloggerClassName = null;
	
	/**
	 * MIDP Logger
	 */
	private static String MIDPLoggerClassName = "org.eclipse.paho.client.mqttv3.logging.MIDPLogging";
	
	/**
	 * Find or create a logger for a named package/class. 
	 * If a logger has already been created with the given name 
	 * it is returned. Otherwise a new logger is created. By default a logger
	 * that uses com.oracle.util.logging will be returned.
	 * 
	 * @param messageCatalogName the resource bundle containing the logging messages.
	 * @param loggerID  unique name to identify this logger.
	 * @return a suitable Logger.
	 * @throws Exception
	 */
	public static Logger getLogger(String messageCatalogName, String loggerID) {
		String loggerClassName = overrideloggerClassName;
		Logger logger = null;
		
		if (loggerClassName == null) {
			loggerClassName = DefaultloggerClassName;
		}

		// Attempt to create an instance of the logger
		try {
			logger = getLogger(loggerClassName, PropertyResourceBundle.getBundle(messageCatalogName, true), loggerID, null) ;
		} catch (MqttException e) {
			throw new IllegalStateException("Error locating the logging class: " + loggerClassName + ", " + loggerID);
		}
		
		// If it's still Null create an EmptyLogger
		if (null == logger) {
			logger = new EmptyLogger();
		}
		
		return logger;
	}
	
	/**
	 * Return an instance of a logger
	 * 
	 * @param the class name of the load to load
	 * @param messageCatalog  the resource bundle containing messages 
	 * @param loggerID  an identifier for the logger 
	 * @param resourceName a name or context to associate with this logger instance.  
	 * @return a ready for use logger
	 */
	private static Logger getLogger(String loggerClassName, PropertyResourceBundle messageCatalog, String loggerID, String resourceName) { //, FFDC ffdc) {
		Logger logger  = null;
		Class logClass = null;
		
		try {
			logClass = Class.forName(loggerClassName);
		} catch (NoClassDefFoundError ncdfe) {
			return null;
		} catch (ClassNotFoundException cnfe) {
			return null;
		}
		if (null != logClass) {
			// Now instantiate the log
			try {
				logger = (Logger)logClass.newInstance();
			} catch (IllegalAccessException e) {
				return null;
			} catch (InstantiationException e) {
				return null;
			} catch (SecurityException e) {
				return null;
			}
			logger.initialise(messageCatalog, loggerID, resourceName);
		}

		return logger;
	}
	
	/**
	 * Set the class name of the logger that the LoggerFactory will load
	 * If not set getLogger will attempt to create a logger 
	 * appropriate for the platform.
	 * @param loggerClassName - Logger implementation class name to use.
	 */
	public static void setLogger(String loggerClassName) {
		LoggerFactory.overrideloggerClassName = loggerClassName;
	}
}
