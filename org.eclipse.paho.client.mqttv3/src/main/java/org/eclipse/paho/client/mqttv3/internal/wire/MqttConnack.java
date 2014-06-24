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
 *    Ian Craggs - MQTT 3.1.1 support
 */
package org.eclipse.paho.client.mqttv3.internal.wire;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * An on-the-wire representation of an MQTT CONNACK.
 */
public class MqttConnack extends MqttAck {
	public static final String KEY = "Con";

	private int returnCode;
	private boolean sessionPresent;
	
	public MqttConnack(byte info, byte[] variableHeader) throws IOException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNACK);
		ByteArrayInputStream bais = new ByteArrayInputStream(variableHeader);
		DataInputStream dis = new DataInputStream(bais);
		sessionPresent = (dis.readUnsignedByte() & 0x01) == 0x01;
		returnCode = dis.readUnsignedByte();
		dis.close();
	}
	
	public int getReturnCode() {
		return returnCode;
	}

	protected byte[] getVariableHeader() throws MqttException {
		// Not needed, as the client never encodes a CONNACK
		return new byte[0];
	}
	
	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	public boolean isMessageIdRequired() {
		return false;
	}
	
	public String getKey() {
		return KEY;
	}
	
	public String toString() {
		return super.toString() + " session present:" + sessionPresent + " return code: " + returnCode;
	}
	
	public boolean getSessionPresent() {
		return sessionPresent;
	}
}
