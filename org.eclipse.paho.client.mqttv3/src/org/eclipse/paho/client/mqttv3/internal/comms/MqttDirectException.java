/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.comms;

/**
 * MQTTDirect exception. Uses a log error message instead of a string.
 * The root cause of the exception is also attached.
 * <p>
 */
public class MqttDirectException extends Exception {

	private static final long serialVersionUID = 1L;
	protected long msgId=0;
	protected Object[] inserts=null;
	protected Throwable linkt=null;
	
    /**
     * Creates a new MqttDirectException without log message
     * and linked root throwable.
     */
    public MqttDirectException() {
    }
 
	/**
	 * Attaches a message to the exception and links the root cuase.
	 * @param reason The reason why this exception has been thrown. The reason will be a localized String.
	 * @param theCause  The Throwable which caused the failure.
	 */
	public MqttDirectException(String reason, Throwable theCause) {
		super(reason);
		linkt = theCause;
	}
   /**
     * Attaches  a log message to the exception.
     * @param theMsgId The msgID from the catalogue
     * @param theInserts The parameters to the message
     */
    public MqttDirectException(long theMsgId, Object[] theInserts) {
		msgId = theMsgId;
		inserts = theInserts;    	
    }
    
    /**
     * Attaches a log message to the exception and links the root cause.
     * @param theMsgId The msgID from the catalogue
     * @param theInserts The parameters to the message
     * @param cause The cause of the exception.
     */
    public MqttDirectException(long theMsgId, Object[] theInserts, Throwable cause) {
		msgId = theMsgId;
		inserts = theInserts;    	
		linkt=cause;
    }
    
    /**
     * @return Returns the cause throwable or null
     */
    public Throwable getCause() {
    	return linkt;
    }
    
	/**
	 * @return Returns the log message inserts or null
	 */
	public Object[] getInserts() {
		return inserts;
	}
	/**
	 * @return Returns the log message identifier or 0.
	 */
	public long getMsgId() {
		return msgId;
	}    
    
}
