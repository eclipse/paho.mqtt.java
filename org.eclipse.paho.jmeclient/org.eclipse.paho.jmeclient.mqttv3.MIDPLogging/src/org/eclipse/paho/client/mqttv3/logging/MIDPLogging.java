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
package org.eclipse.paho.client.mqttv3.logging;

import java.util.Hashtable;

import net.sf.microlog.core.LoggerFactory;
import net.sf.microlog.core.PropertyConfigurator;

import org.eclipse.paho.client.mqttv3.util.PropertyResourceBundle;

/**
 * Implementation of the the logger interface that uses microlog
 * 
 * A Logger that utilises the microlog logging classes
 * <p>A sample logging properties file - microlog.properties is provided that demonstrates
 * how to run with a memory based trace facility that runs with minimal performance 
 * overhead. 
 */
public class MIDPLogging implements Logger {
	private static net.sf.microlog.core.Logger	    julLogger   		= null;
	private PropertyResourceBundle					logMessageCatalog	= null;
	private PropertyResourceBundle					traceMessageCatalog	= null;
	private String									catalogID			= null;
	private String									resourceName		= null;
	private String									loggerName			= null;
	
	public MIDPLogging() {
		// default no arg constructor
	}
	
	/**
	 * 
	 * @param logMsgCatalog  The resource bundle associated with this logger
	 * @param loggerID			The suffix for the loggerName (will be appeneded to org.eclipse.paho.client.mqttv3
	 * @param resourceContext	A context for the logger e.g. clientID or appName...
	 */
	public void initialise(PropertyResourceBundle logMsgCatalog, String loggerID, String resourceContext) {
		this.traceMessageCatalog = logMessageCatalog;
		this.resourceName = resourceContext;
		loggerName = loggerID;
		
		if (julLogger == null) {
			julLogger = LoggerFactory.getLogger(getClass());
			PropertyConfigurator.configure("/microlog.properties");
			for (int i=0; i<julLogger.getNumberOfAppenders();i++) {
				System.out.println("Index " + julLogger.getAppender(i));
				julLogger.getAppender(i).setFormatter(new MIDPLogFormatter());
			} 
 		}
		
		this.logMessageCatalog = logMsgCatalog;
		this.traceMessageCatalog = logMsgCatalog;
		this.catalogID = logMessageCatalog.getString("0");
	}

	public void setResourceName(String logContext) {
		this.resourceName = logContext;
	}

	public void severe(String sourceClass, String sourceMethod, String msg) {
		log(SEVERE, sourceClass, sourceMethod, msg, null, null);
	}

	public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		log(SEVERE, sourceClass, sourceMethod, msg, inserts, null);
	}

	public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		log(SEVERE, sourceClass, sourceMethod, msg, inserts, thrown);
	}

	public void warning(String sourceClass, String sourceMethod, String msg) {
		log(WARNING, sourceClass, sourceMethod, msg, null, null);
	}

	public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		log(WARNING, sourceClass, sourceMethod, msg, inserts, null);
	}

	public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		log(WARNING, sourceClass, sourceMethod, msg, inserts, thrown);
	}

	public void info(String sourceClass, String sourceMethod, String msg) {
		log(INFO, sourceClass, sourceMethod, msg, null, null);
	}

	public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		log(INFO, sourceClass, sourceMethod, msg, inserts, null);
	}

	public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		log(INFO, sourceClass, sourceMethod, msg, inserts, thrown);
	}

	public void config(String sourceClass, String sourceMethod, String msg) {
		log(CONFIG, sourceClass, sourceMethod, msg, null, null);
	}

	public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		log(CONFIG, sourceClass, sourceMethod, msg, inserts, null);
	}

	public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		log(CONFIG, sourceClass, sourceMethod, msg, inserts, thrown);
	}
	
	public void fine(String sourceClass, String sourceMethod, String msg) {
		trace(FINE, sourceClass, sourceMethod, msg, null, null);
	}

	public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		trace(FINE, sourceClass, sourceMethod, msg, inserts, null);
	}
	
	public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		trace(FINE, sourceClass, sourceMethod, msg, inserts, ex);
	}

	public void finer(String sourceClass, String sourceMethod, String msg) {
		trace(FINER, sourceClass, sourceMethod, msg, null, null);
	}

	public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		trace(FINER, sourceClass, sourceMethod, msg, inserts, null);
	}
	
	public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		trace(FINER, sourceClass, sourceMethod, msg, inserts, ex);
	}

	public void finest(String sourceClass, String sourceMethod, String msg) {
		trace(FINEST, sourceClass, sourceMethod, msg, null, null);
	}

	public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		trace(FINEST, sourceClass, sourceMethod, msg, inserts, null);
	}
	
	public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		trace(FINEST, sourceClass, sourceMethod, msg, inserts, ex);
	}


	public void trace(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		logToMicroLog(mapJULLevel(level), sourceClass, sourceMethod, this.catalogID, this.traceMessageCatalog, msg, inserts, ex);
	}
	
	public void log(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		logToMicroLog(mapJULLevel(level), sourceClass, sourceMethod, this.catalogID, this.logMessageCatalog, msg, inserts, thrown);
	}
	
	private void logToMicroLog(net.sf.microlog.core.Level julLevel, String sourceClass, String sourceMethod, String catalogName,
			PropertyResourceBundle messageCatalog, String msg, Object[] inserts, Throwable thrown) {
		
		String formattedWithArgs = msg;
		if (msg.indexOf("=====")== -1) {
			formattedWithArgs = getResourceMessage(messageCatalog, msg);
			if (inserts != null && inserts.length > 0) {
				formattedWithArgs = formatMessage(formattedWithArgs, inserts);
			}
		}
		
		String message = System.currentTimeMillis() + "###" + sourceClass + "###" + sourceMethod + "###" + Thread.currentThread().getName() 
				+ "###" + resourceName + ": " + formattedWithArgs;

		julLogger.log(julLevel, message);
	}


	public String formatMessage(String msg, Object[] inserts) {
		for (int i=0; i<inserts.length; i++) {
			String replace = "{"+i+"}";
			int id = msg.indexOf(replace);
			if (id != -1) {
				msg = msg.substring(0, id) + inserts[i] + msg.substring(id + replace.length());
			}
		}
		return msg;
	}
	
	private String getResourceMessage(PropertyResourceBundle messageCatalog, String msg) {
		String message;
		try {
			message = messageCatalog.getString(msg);
		} catch (Exception e) {
			// This is acceptable, simply return the given msg string.
			message = msg;
		}
		if (message == null || message.equals("")) message = msg;
		return message;
	}

	public void dumpTrace() {
		dumpMemoryTrace47(julLogger);
	}
	
	protected static void dumpMemoryTrace47(net.sf.microlog.core.Logger julLogger2) {
	}

	public Hashtable getProperties() {
		Hashtable hash = new Hashtable();
		hash.put("microedition.platform", System.getProperty("microedition.platform"));
		hash.put("microedition.encoding", System.getProperty("microedition.encoding"));
		hash.put("microedition.configuration", System.getProperty("microedition.configuration"));
		hash.put("microedition.profiles", System.getProperty("microedition.profiles"));
		hash.put("microedition.locale", System.getProperty("microedition.locale"));
		hash.put("microedition.hostname", System.getProperty("microedition.hostname"));
    	return hash;
	}
	
	private net.sf.microlog.core.Level mapJULLevel(int level) {
		net.sf.microlog.core.Level julLevel = null;

		switch (level) {
			case SEVERE:
				julLevel = net.sf.microlog.core.Level.ERROR;
				break;
			case WARNING:
				julLevel = net.sf.microlog.core.Level.WARN;
				break;
			case INFO:
				julLevel = net.sf.microlog.core.Level.INFO;
				break;
			case CONFIG:
				julLevel = net.sf.microlog.core.Level.TRACE;
				break;
			case FINE:
				julLevel = net.sf.microlog.core.Level.TRACE;
				break;
			case FINER:
				julLevel = net.sf.microlog.core.Level.TRACE;
				break;
			case FINEST:
				julLevel = net.sf.microlog.core.Level.TRACE;
				break;
			default:
				julLevel = null;
		}

		return julLevel;
	}

	public boolean isLoggable(int level) {
		return true;
	}

}
