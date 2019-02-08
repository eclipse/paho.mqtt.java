/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
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
 * Contributions:
 *   Ian Craggs - MQTT 3.1.1 support
 */

package org.eclipse.paho.mqttv5.client.vertx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttSubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttSubscribe;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

import io.vertx.core.*;
import io.vertx.core.net.*;

/**
 * Provides a mechanism for tracking the completion of an asynchronous action.
 * <p>
 * A token that implements the ImqttToken interface is returned from all
 * non-blocking method with the exception of publish.
 * </p>
 * 
 * @see IMqttToken
 */

public class MqttToken implements IMqttToken {
	/**
	 * A reference to the the class that provides most of the implementation of the
	 * MqttToken. MQTT application programs must not use the internal class.
	 */
	private MqttException exception = null;
	private final CountDownLatch countDownLatch = new CountDownLatch(1);
	private Object userContext = null;
	private MqttWireMessage response = null;
	private int messageId;
	private int[] reasonCodes;
		
	public MqttToken() {
	}
	
	public void setComplete() {
		countDownLatch.countDown();
	}
		
	public MqttToken(String logContext) {
		//this.logContext = logContext;
		this();
	}

	public MqttException getException() {
		return exception;
	}

	public boolean isComplete() {
		//return netfuture.isComplete();
		return countDownLatch.getCount() == 0;
	}

	public void setActionCallback(MqttActionListener listener) {
		//internalTok.setActionCallback(listener);

	}
  
	public MqttActionListener getActionCallback() {
		return null; //internalTok.getActionCallback();
	}

	public void waitForCompletion() throws MqttException {
		try {
			countDownLatch.await();
		} catch (Exception e) {
			throw new MqttException(e);
		}
	}

	public void waitForCompletion(long timeout) throws MqttException {
		//System.out.println("waiting for completion "+timeout);
		boolean result = false;
		try {
			result = countDownLatch.await((timeout == -1) ? 99999 : timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
		}
		
		if (result == false) {
			throw new MqttException(MqttClientException.REASON_CODE_CLIENT_TIMEOUT);
		}
		//System.out.println("wait complete");
	}

	public MqttClientInterface getClient() {
		return null; //internalTok.getClient();
	}

	public String[] getTopics() {
		return null; //((MqttSubscribe)response).getSubscriptions().getTopics();
	}

	public Object getUserContext() {
		return this.userContext;
	}

	public void setUserContext(Object userContext) {
		this.userContext = userContext;
	}

	public void setMessageId(int msgid) {
		this.messageId = msgid;
	}
	
	public int getMessageId() {
		return messageId;
	}

	public int[] getGrantedQos() {
		int[] val = new int[0];
		if (response instanceof MqttSubAck) {
			val = ((MqttSubAck)response).getReturnCodes();
		}
		return val;
	}

	public boolean getSessionPresent() {
		return ((MqttConnAck)response).getSessionPresent();
	}

	public MqttWireMessage getResponse() {
		return response;
	}
	
	public void setResponse(MqttWireMessage response) {
		this.response = response;
	}

	public MqttProperties getMessageProperties() {
		return (response == null) ? null : response.getProperties();
	}
	
	
	public void setReasonCodes(int[] codes) {
		this.reasonCodes = codes;
	}

	@Override
	public int[] getReasonCodes() {
		if (this.reasonCodes == null) {
			return response.getReasonCodes();
		} else {
			return this.reasonCodes;
		}
	}

}
