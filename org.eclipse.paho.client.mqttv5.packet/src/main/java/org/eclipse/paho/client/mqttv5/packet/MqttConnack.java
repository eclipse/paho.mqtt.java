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
 * 	  Dave Locke - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.client.mqttv5.packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv5.util.MqttException;


/**
 * An on-the-wire representation of an MQTT CONNACK.
 */
public class MqttConnack extends MqttAck {
	public static final String KEY = "Con";
	
	private static final byte RECEIVE_MAXIMUM_IDENTIFIER = 33;
	private static final byte RETAIN_UNAVAILABLE_ADVERTISEMENT_IDENTIFIER = 37;
	private static final byte ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER = 18;
	private static final byte TOPIC_ALIAS_MAXIMUM_IDENTIFIER = 34;
	private static final byte REASON_STRING_IDENTIFIER = 31;
	private static final byte SERVER_KEEP_ALIVE_IDENTIFIER = 19;
	private static final byte REPLY_INFO_IDENTIFIER = 26;
	private static final byte SERVER_REFERENCE_IDENTIFIER = 28;
	private static final byte AUTH_METHOD_IDENTIFIER = 21;
	private static final byte AUTH_DATA_IDENTIFIER = 22;
	

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

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		// Not needed, as the client never encodes a CONNACK
		return new byte[0];
	}
	
	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	@Override
	public boolean isMessageIdRequired() {
		return false;
	}
	
	@Override
	public String getKey() {
		return KEY;
	}
	
	@Override
	public String toString() {
		return super.toString() + " session present:" + sessionPresent + " return code: " + returnCode;
	}
	
	public boolean getSessionPresent() {
		return sessionPresent;
	}
}
