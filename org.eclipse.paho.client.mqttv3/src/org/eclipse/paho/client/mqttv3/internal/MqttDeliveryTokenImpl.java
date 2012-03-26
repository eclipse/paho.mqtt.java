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
package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

public class MqttDeliveryTokenImpl implements MqttDeliveryToken {
	private Object responseLock = new Object();
	private Object sentLock = new Object();
	private MqttMessage message;
	private MqttWireMessage response = null;
	private MqttException exception = null;
	private boolean sent = false;
	private boolean completed = false;
	private int msgId = 0;
	
	private Trace trace;
	
	MqttDeliveryTokenImpl(Trace trace) {
		this.message = null;
		this.trace = trace;
	}
	
	/**
	 * Constructs a new delivery token.
	 * 
	 * @param wait whether or not a wait is planned on this token.  This is needed
	 * to eliminate a timing window where a response is received before the call
	 * to {@link #waitForCompletion()} is made.
	 */
	MqttDeliveryTokenImpl(Trace trace, MqttPublish send) {
		this.trace = trace;
		this.message = send.getMessage();
		this.msgId = send.getMessageId();
	}
	
	public void waitForCompletion(long timeout) throws MqttException {
		MqttWireMessage response = (MqttWireMessage)waitForResponse(timeout);
		if (response == null && !completed) {
			if (trace.isOn()) {
				//@TRACE 406=waitForCompletion timed out timeout={0}
				trace.trace(Trace.FINE,406,new Object[]{new Long(timeout)});
			}
			throw new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
		}
	}
	
	public void waitForCompletion() throws MqttException {
		waitForCompletion(-1);
	}
	
	/**
	 * Waits for the message delivery to complete, but doesn't throw an exception
	 * in the case of a NACK.  It does still throw an exception if something else
	 * goes wrong (e.g. an IOException).  This is used for packets like CONNECT, 
	 * which have useful information in the ACK that needs to be accessed.
	 */
	protected MqttWireMessage waitForResponse() throws MqttException {
		return waitForResponse(-1);
	}
	
	protected MqttWireMessage waitForResponse(long timeout) throws MqttException {
		synchronized (responseLock) {
			if (trace.isOn()) {
				//@TRACE 400=waitForResponse token={0} timeout={1} sent={2} completed={3} hasException={4} response={5}
				trace.trace(Trace.FINE,400,new Object[]{this, new Long(timeout),new Boolean(sent),new Boolean(completed),(exception==null)?"false":"true",response},exception);
			}
			if (this.completed) {
				return this.response;
			}
			if (this.exception == null) {
				try {
					if (timeout == -1) {
						responseLock.wait();
					} else {
						responseLock.wait(timeout);
					}
				} catch (InterruptedException e) {
				}
			}
			if (!this.completed) {
				if (this.exception != null) {
					MqttException e = this.exception;
					this.exception = null;
					//@TRACE 401=waitForResponse exception
					trace.trace(Trace.FINE,401,null,exception);
					throw e;
				}
			}
		}
		//@TRACE 402=waitForResponse response={0}
		trace.trace(Trace.FINE,402,new Object[]{this.response});
		return this.response;
	}
	
	protected void waitUntilSent() throws MqttException {
		synchronized (sentLock) {
			synchronized (responseLock) {
				if (this.exception != null) {
					throw this.exception;
				}
			}
			if (!sent) {
				try {
					sentLock.wait();
				} catch (InterruptedException e) {
				}
			}
			
			if (!sent) {
				if (this.exception == null) {
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
				} else {
					throw this.exception;
				}
			}
		}
	}
	
	/**
	 * Notifies this token that the associated message has been sent
	 * (i.e. written to the TCP/IP socket).
	 */
	protected void notifySent() {
		//@TRACE 403=notifySent token={0}
		trace.trace(Trace.FINE,403,new Object[]{this});
		synchronized (responseLock) {
			this.response = null;
			this.completed = false;
		}
		synchronized (sentLock) {
			sent = true;
			sentLock.notifyAll();
		}
	}
	
	/**
	 * Notifies this token that a response message (an ACK or NACK) has been
	 * received.
	 */
	protected void notifyReceived(MqttWireMessage msg) {
		//@TRACE 404=notifyReceived token={0} response={1}
		trace.trace(Trace.FINE,404,new Object[]{this,msg});
		synchronized (responseLock) {
			// ACK means that everything was OK, so mark the message for garbage collection.
			if (msg instanceof MqttAck) {
				this.message = null;
			}
			this.response = msg;
			this.completed = true;
			responseLock.notifyAll();
		}
	}
	
	/**
	 * Notifies this token that an exception has occurred.  This is only
	 * used for things like IOException, and not for MQTT NACKs.
	 */
	protected void notifyException(MqttException exception) {
		//@TRACE 405=notifyException token={0}
		trace.trace(Trace.FINE,405,new Object[]{this},exception);
		synchronized (responseLock) {
			this.exception = exception;
			responseLock.notifyAll();
		}
		synchronized (sentLock) {
			sentLock.notifyAll();
		}
	}

	public MqttMessage getMessage() throws MqttException {
		return message;
	}

	public boolean isComplete() {
		return completed;
	}
	
	public int getMessageId() {
		return this.msgId;
	}
}
