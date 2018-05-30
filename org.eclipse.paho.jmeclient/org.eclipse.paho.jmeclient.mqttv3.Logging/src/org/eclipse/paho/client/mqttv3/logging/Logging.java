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
import com.oracle.util.logging.Handler;
import com.oracle.util.logging.Level;
import com.oracle.util.logging.LogRecord;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.util.ConsoleHandler;
import org.eclipse.paho.client.mqttv3.util.FileHandler;
import org.eclipse.paho.client.mqttv3.util.MemoryHandler;
import org.eclipse.paho.client.mqttv3.util.PropertyResourceBundle;

/**
 * Implementation of the the logger interface that uses com.oracle.util.logging
 * 
 * A Logger that utilises Java's built in logging facility - com.oracle.util.logging.
 * <p>A sample logging properties file - microlog.properties is provided that demonstrates
 * how to run with a memory based trace facility that runs with minimal performance 
 * overhead. The memory buffer can be dumped when a log/trace record is written matching 
 * the MemoryHandlers trigger level or when the push method is invoked on the MemoryHandler. 
 * {@link org.eclipse.paho.client.mqttv3.util.Debug Debug} provides method to make it easy
 * to dump the memory buffer as well as other useful debug info. 
 */
public class Logging implements Logger {
	private static com.oracle.util.logging.Logger	julLogger   		= null;
	private PropertyResourceBundle					logMessageCatalog	= null;
	private PropertyResourceBundle					traceMessageCatalog	= null;
	private String									catalogID			= null;
	private String									resourceName		= null;
	private String									loggerName			= null;
	private Handler									logHandler 			= null;
	
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
			julLogger = com.oracle.util.logging.Logger.getAnonymousLogger();
			setLoggerProperties();
		}
		
		this.logMessageCatalog = logMsgCatalog;
		this.traceMessageCatalog = logMsgCatalog;
		this.catalogID = logMessageCatalog.getString("0");
	}

	/**
	 * Method to retrieve the logging properties from the microlog.properties file
	 * and create the required logger.
	 */
	private void setLoggerProperties() {
		try {
			
			PropertyResourceBundle bundle = PropertyResourceBundle.getBundle("logging", false);

			// Get the level
			Level level = mapJULLevel(mapLeveltoInt(bundle.getString("logging.level").trim()));
			
			// Get the handler
			String handler = bundle.getString("logging.handler").trim();
			
			// Get the Formatter if present
			
			if (handler.equals("MemoryHandler")) {
				// Get the push level
				Level pushLevel = mapJULLevel(mapLeveltoInt(bundle.getString("logging.MemoryHandler.push").trim()));
				
				// Get the size
				int size = 0;
				try {
					size = Integer.parseInt(bundle.getString("logging.MemoryHandler.size").trim());
				} catch (NumberFormatException e) {
					// We will use the default in this case
				}
				
				// Get the target, default to console handler
				String target = bundle.getString("logging.MemoryHandler.target").trim();
				
				// If the target is a FileHandler we need to retrieve the properties and pass the constructed object
				// to the MemoryHandler.
				if (target.equals("FileHandler")) {
					int limit = 0;
					try {
						limit = Integer.parseInt(bundle.getString("logging.FileHandler.limit").trim());
					} catch (NumberFormatException e) {
						// We will use the default in this case
					}
					
					int numberOfFiles = 0;
					try {
						numberOfFiles = Integer.parseInt(bundle.getString("logging.FileHandler.count").trim());
					} catch (NumberFormatException e) {
						// We will use the default in this case
					}
					
					String directory = bundle.getString("logging.FileHandler.directory").trim();
					FileHandler fileH = new FileHandler(limit, numberOfFiles, directory);
					logHandler = new MemoryHandler(size,pushLevel,fileH);
				} else {
					// Initialise the handler
					logHandler = new MemoryHandler(size, pushLevel, target);
				}
				logHandler.setLevel(level);
			} else if (handler.equals("ConsoleHandler")) {
				logHandler = new ConsoleHandler();
				logHandler.setLevel(level);
			} else if (handler.equals("FileHandler")) {
				int size = 0;
				try {
					size = Integer.parseInt(bundle.getString("logging.FileHandler.limit").trim());
				} catch (NumberFormatException e) {
					// We will use the default in this case
				}
				
				int numberOfFiles = 0;
				try {
					numberOfFiles = Integer.parseInt(bundle.getString("logging.FileHandler.count").trim());
				} catch (NumberFormatException e) {
					// We will use the default in this case
				}
				
				String directory = bundle.getString("logging.FileHandler.directory").trim();
				logHandler = new FileHandler(size, numberOfFiles, directory);
			} else {
				// unrecognised format
			}
			
			// set the log
			if (logHandler != null) {
				julLogger.setLevel(level);
				logHandler.setFormatter(new SimpleLogFormatter());
				julLogger.addHandler(logHandler);
			}
		} catch (MqttException e) {
			System.out.println("Unable to configure tracing please check your configuration is correct");
		}
	}

	public void setResourceName(String logContext) {
		this.resourceName = logContext;
	}

	public boolean isLoggable(int level) {
		return julLogger.isLoggable(mapJULLevel(level));
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
		com.oracle.util.logging.Level julLevel = mapJULLevel(level);
		
		boolean isJULLoggable = julLogger.isLoggable(julLevel);
		if (isJULLoggable) {
			logToJsr47(julLevel, sourceClass, sourceMethod, this.catalogID, this.traceMessageCatalog, msg, inserts, ex);
		}
	}
	
	public void log(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		
		com.oracle.util.logging.Level julLevel = mapJULLevel(level);
		if (julLogger.isLoggable(julLevel)) {
			logToJsr47(julLevel, sourceClass, sourceMethod, this.catalogID, this.logMessageCatalog, msg, inserts, thrown);
		}
	}
	
	private void logToJsr47(com.oracle.util.logging.Level julLevel, String sourceClass, String sourceMethod, String catalogName,
			PropertyResourceBundle messageCatalog, String msg, Object[] inserts, Throwable thrown) {
		
		String formattedWithArgs = msg;
		if (msg.indexOf("=====")== -1) {
			formattedWithArgs = getResourceMessage(messageCatalog, msg);
			if (inserts != null && inserts.length > 0) {
				formattedWithArgs = formatMessage(formattedWithArgs, inserts);
			}
		}
		LogRecord logRecord = new LogRecord(julLevel, resourceName + ": " +formattedWithArgs);
		
		logRecord.setSourceClassName(sourceClass);
		logRecord.setSourceMethodName(sourceMethod);
		logRecord.setLoggerName(loggerName);

		if (null != thrown) {
			logRecord.setThrown(thrown);
		}

		julLogger.log(logRecord);
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
	
	protected static void dumpMemoryTrace47(com.oracle.util.logging.Logger logger) {
		com.oracle.util.logging.Handler mHand = null;

		if (logger!= null) {
			com.oracle.util.logging.Handler[] handlers = logger.getHandlers();
			
		    for (int i=0; i<handlers.length; i++) {
		      if (handlers[i] instanceof com.oracle.util.logging.Handler) {
		        synchronized (handlers[i]) {
		        	mHand = ((com.oracle.util.logging.StreamHandler)handlers[i]);
		        	mHand.flush();
		        	return;
		        } // synchronized (handler).
		      }      
		    } // for handlers...
		    //dumpMemoryTrace47(logger.getParent());
		}
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
	
	private com.oracle.util.logging.Level mapJULLevel(int level) {
		com.oracle.util.logging.Level julLevel = null;

		switch (level) {
			case SEVERE:
				julLevel = com.oracle.util.logging.Level.SEVERE;
				break;
			case WARNING:
				julLevel = com.oracle.util.logging.Level.WARNING;
				break;
			case INFO:
				julLevel = com.oracle.util.logging.Level.INFO;
				break;
			case CONFIG:
				julLevel = com.oracle.util.logging.Level.CONFIG;
				break;
			case FINE:
				julLevel = com.oracle.util.logging.Level.FINE;
				break;
			case FINER:
				julLevel = com.oracle.util.logging.Level.FINER;
				break;
			case FINEST:
				julLevel = com.oracle.util.logging.Level.FINEST;
				break;
			default:
				julLevel = com.oracle.util.logging.Level.OFF;
		}

		return julLevel;
	}
	
	private int mapLeveltoInt(String level) {
		if (level.equals("SEVERE")) {
			return 1;
		} else if (level.equals("WARNING")) {
			return 2;
		} else if (level.equals("INFO")) {
			return 3;
		} else if (level.equals("CONFIG")) {
			return 4;
		} else if (level.equals("FINE")) {
			return 5;
		} else if (level.equals("FINER")) {
			return 6;
		} else if (level.equals("FINEST")) {
			return 7;
		}
		return 0;
	}

}
