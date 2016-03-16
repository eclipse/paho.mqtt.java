/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 *    James Sutton - Initial Contribution for Automatic Reconnect & Offline Buffering
 */
package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 * A BufferedMessage contains an MqttWire Message and token
 * it allows both message and token to be buffered when the client
 * is in resting state
 */
public class BufferedMessage {
	
	private MqttWireMessage message;
	private MqttToken token; 
	
	public BufferedMessage(MqttWireMessage message, MqttToken token){
		this.message = message;
		this.token = token;
	}

	public MqttWireMessage getMessage() {
		return message;
	}

	public MqttToken getToken() {
		return token;
	}
}
