/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *   
 * Contributions:
 *   Ian Craggs - MQTT 3.1.1 support
 */

package org.eclipse.paho.mqttv5.client;

import org.eclipse.paho.mqttv5.client.internal.Token;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
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
        private MqttAsyncClient client = null;
	/**
	 * A reference to the the class that provides most of the implementation of the
	 * MqttToken. MQTT application programs must not use the internal class.
	 */
	public Token internalTok = null;

        public MqttToken() {
        }

        public MqttToken(MqttAsyncClient client) {
                this.client = client;
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

	public MqttProperties getResponseProperties() {
		return (internalTok.getWireMessage() == null) ? null : internalTok.getWireMessage().getProperties();
	}

        public MqttWireMessage getRequestMessage() {
                return internalTok.getRequestMessage();
        }

        public void setRequestMessage(MqttWireMessage request) {
                internalTok.setRequestMessage(request);
        }

        public MqttProperties getRequestProperties() {
		return (internalTok.getRequestMessage() == null) ? null : internalTok.getRequestMessage().getProperties();
        }

	@Override
	public int[] getReasonCodes() {
		return internalTok.getReasonCodes();
	}

        /**
         * Returns the message associated with this token.
         * <p>Until the message has been delivered, the message being delivered will
         * be returned. Once the message has been delivered <code>null</code> will be
         * returned.
         * @return the message associated with this token or null if already delivered.
         * @throws MqttException if there was a problem completing retrieving the message
         */
        public MqttMessage getMessage() throws MqttException {
                return internalTok.getMessage();
        }

        protected void setMessage(MqttMessage msg) {
                internalTok.setMessage(msg);
        }

}
