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

import org.eclipse.paho.client.mqttv3.logging.SimpleLogFormatter;

import com.oracle.util.logging.Handler;
import com.oracle.util.logging.Level;
import com.oracle.util.logging.LogRecord;

/**
 * Handler that configures trace to be continuously collected in memory with 
 * minimal impact on performance. When the push trigger (by default a Severe level message) 
 * or a specific request is made to "push", the in memory trace is "pushed" to the configured 
 * target handler. By default this is the FileHandler. The Paho Debug class can be used 
 * to push the memory trace to its target. 
 *
 */

public class MemoryHandler extends Handler {
	
    private final static int DEFAULT_SIZE = 1000;
    private final static Level DEFAULT_PUSHLEVEL = Level.SEVERE;
    private final static String DEFAULT_HANDLER = "ConsoleHandler";
    private Level pushLevelC;
    private Handler targetC;
    private LogRecord buffer[];
    int start, count;
    
	/**
	 * Constructs a MemoryHandler object.
	 * @param Size the number of log records to buffer (must be greater than zero)
	 * @param pushLevel - message level to push on
	 */
    public MemoryHandler(int size, Level pushLevel, String handler) {
    	this.pushLevelC = (pushLevel == null) ? DEFAULT_PUSHLEVEL : pushLevel;
    	if (size <= 0) size = DEFAULT_SIZE;
    	handler = (handler.equals("")) ? DEFAULT_HANDLER : handler;
    	buffer = new LogRecord[size];
        start = 0;
        count = 0;
        try {
			targetC = (Handler) Class.forName("org.eclipse.paho.client.mqttv3.util." + handler).newInstance();
			targetC.setLevel(pushLevelC);
			targetC.setFormatter(new SimpleLogFormatter());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
	/**
	 * Constructs a MemoryHandler object.
	 * @param Size the number of log records to buffer (must be greater than zero)
	 * @param pushLevel - message level to push on
	 */
    public MemoryHandler(int size, Level pushLevel, FileHandler handler) {
    	this.pushLevelC = (pushLevel == null) ? DEFAULT_PUSHLEVEL : pushLevel;
    	if (size <= 0) size = DEFAULT_SIZE;
    	buffer = new LogRecord[size];
        start = 0;
        count = 0;
        try {
			targetC = handler;
			targetC.setLevel(pushLevelC);
			targetC.setFormatter(new SimpleLogFormatter());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	public void close() {
		targetC.close();
		setLevel(Level.OFF);
	}

	public void flush() {
		targetC.flush();
	}

	public void publish(LogRecord record) {
		if (isLoggable(record)) {
			int ix = (start+count)%buffer.length;
			buffer[ix] = record;
			if (count < buffer.length) {
			    count++;
			} else {
			    start++;
			}
			if (record.getLevel().intValue() >= pushLevelC.intValue()) {
			    push();
			}
		}
	}
	
    public synchronized void push() {
		for (int i = 0; i < count; i++) {
		    int ix = (start+i)%buffer.length;
		    LogRecord record = buffer[ix];
		    targetC.publish(record);
		}
		// Empty the buffer.
		start = 0;
		count = 0;
	}
    
    public void setPushLevel(Level pushLevel) {
    	this.pushLevelC = pushLevel;
    }
}
