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

package org.eclipse.paho.mqttv5.client;

import org.eclipse.paho.mqttv5.client.internal.Token;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

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
	public Token internalTok = null;

	public MqttToken() {
	}

	public MqttToken(String logContext) {
		internalTok = new Token(logContext);
	}

	public MqttException getException() {
		return internalTok.getException();
	}

	public boolean isComplete() {
		return internalTok.isComplete();
	}

	public void setActionCallback(MqttActionListener listener) {
		internalTok.setActionCallback(listener);

	}
  
	public MqttActionListener getActionCallback() {
		return internalTok.getActionCallback();
	}

	public void waitForCompletion() throws MqttException {
		internalTok.waitForCompletion(-1);
	}

	public void waitForCompletion(long timeout) throws MqttException {
		internalTok.waitForCompletion(timeout);
	}

	public MqttClientInterface getClient() {
		return internalTok.getClient();
	}

	public String[] getTopics() {
		return internalTok.getTopics();
	}

	public Object getUserContext() {
		return internalTok.getUserContext();
	}

	public void setUserContext(Object userContext) {
		internalTok.setUserContext(userContext);
	}

	public int getMessageId() {
		return internalTok.getMessageID();
	}

	public int[] getGrantedQos() {
		return internalTok.getGrantedQos();
	}

	public boolean getSessionPresent() {
		return internalTok.getSessionPresent();
	}

	public MqttWireMessage getResponse() {
		return internalTok.getResponse();
	}

	public MqttProperties getMessageProperties() {
		return internalTok.getWireMessage().getProperties();
	}

	@Override
	public int[] getReasonCodes() {
		return internalTok.getWireMessage().getReasonCodes();
	}

}
