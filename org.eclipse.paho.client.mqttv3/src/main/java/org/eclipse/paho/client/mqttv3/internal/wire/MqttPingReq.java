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
package org.eclipse.paho.client.mqttv3.internal.wire;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * An on-the-wire representation of an MQTT PINGREQ message.
 */
public class MqttPingReq extends MqttWireMessage {
	public static final String KEY = "Ping";

	public MqttPingReq() {
		super(MqttWireMessage.MESSAGE_TYPE_PINGREQ);
	}

	public MqttPingReq(byte info, byte[] variableHeader) throws IOException {
		super(MqttWireMessage.MESSAGE_TYPE_PINGREQ);
	}
	
	/**
	 * Returns <code>false</code> as message IDs are not required for MQTT
	 * PINGREQ messages.
	 */
	public boolean isMessageIdRequired() {
		return false;
	}

	protected byte[] getVariableHeader() throws MqttException {
		return new byte[0];
	}
	
	protected byte getMessageInfo() {
		return 0;
	}
	
	public String getKey() {
		return KEY;
	}
}

