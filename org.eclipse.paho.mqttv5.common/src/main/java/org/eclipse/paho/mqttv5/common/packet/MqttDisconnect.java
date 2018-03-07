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
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.util.CountingInputStream;

public class MqttDisconnect extends MqttWireMessage {

	public static final String KEY = "Disc";

	private static final int[] validReturnCodes = { MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_DISCONNECT_WITH_WILL_MESSAGE, MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR,
			MqttReturnCode.RETURN_CODE_MALFORMED_CONTROL_PACKET, MqttReturnCode.RETURN_CODE_PROTOCOL_ERROR,
			MqttReturnCode.RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR, MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED,
			MqttReturnCode.RETURN_CODE_SERVER_BUSY, MqttReturnCode.RETURN_CODE_SERVER_SHUTTING_DOWN,
			MqttReturnCode.RETURN_CODE_KEEP_ALIVE_TIMEOUT, MqttReturnCode.RETURN_CODE_SESSION_TAKEN_OVER,
			MqttReturnCode.RETURN_CODE_TOPIC_FILTER_NOT_VALID, MqttReturnCode.RETURN_CODE_TOPIC_NAME_INVALID,
			MqttReturnCode.RETURN_CODE_RECEIVE_MAXIMUM_EXCEEDED, MqttReturnCode.RETURN_CODE_TOPIC_ALIAS_NOT_ACCEPTED,
			MqttReturnCode.RETURN_CODE_PACKET_TOO_LARGE, MqttReturnCode.RETURN_CODE_MESSAGE_RATE_TOO_HIGH,
			MqttReturnCode.RETURN_CODE_QUOTA_EXCEEDED, MqttReturnCode.RETURN_CODE_ADMINISTRITIVE_ACTION,
			MqttReturnCode.RETURN_CODE_PAYLOAD_FORMAT_INVALID, MqttReturnCode.RETURN_CODE_RETAIN_NOT_SUPPORTED,
			MqttReturnCode.RETURN_CODE_QOS_NOT_SUPPORTED, MqttReturnCode.RETURN_CODE_USE_ANOTHER_SERVER,
			MqttReturnCode.RETURN_CODE_SERVER_MOVED, MqttReturnCode.RETURN_CODE_SHARED_SUB_NOT_SUPPORTED,
			MqttReturnCode.RETURN_CODE_CONNECTION_RATE_EXCEEDED, MqttReturnCode.RETURN_CODE_MAXIMUM_CONNECT_TIME,
			MqttReturnCode.RETURN_CODE_SUB_IDENTIFIERS_NOT_SUPPORTED,
			MqttReturnCode.RETURN_CODE_WILDCARD_SUB_NOT_SUPPORTED };

	// Fields
	private int returnCode = MqttReturnCode.RETURN_CODE_SUCCESS;
	
	private static final Byte[] validProperties = { MqttProperties.SESSION_EXPIRY_INTERVAL_IDENTIFIER,
			MqttProperties.SERVER_REFERENCE_IDENTIFIER, MqttProperties.REASON_STRING_IDENTIFIER,
			MqttProperties.USER_DEFINED_PAIR_IDENTIFIER };
	
	private MqttProperties properties;

	public MqttDisconnect(byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_DISCONNECT);
		this.properties = new MqttProperties(validProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream inputStream = new DataInputStream(counter);
		if(data.length - counter.getCounter() >= 1) {
			returnCode = inputStream.readUnsignedByte();
			validateReturnCode(returnCode, validReturnCodes);
		}
		
		long remainder = (long) data.length - counter.getCounter();
		if (remainder >= 2) {
			this.properties.decodeProperties(inputStream);
		}

		inputStream.close();
	}

	public MqttDisconnect(int returnCode, MqttProperties properties) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_DISCONNECT);
		validateReturnCode(returnCode, validReturnCodes);
		this.returnCode = returnCode;
		if (properties != null) {
			this.properties = properties;
		} else {
			this.properties = new MqttProperties();
		}
		this.properties.setValidProperties(validProperties);
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Encode the Return Code
			outputStream.writeByte(returnCode);

			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();
			if (identifierValueFieldsByteArray.length != 0) {
				outputStream.write(identifierValueFieldsByteArray);
				outputStream.flush();
			}
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}
	
	public int getReturnCode() {
		return returnCode;
	}
	
	@Override
	protected byte getMessageInfo() {
		return (byte) 0;
	}
	
	@Override
	public MqttProperties getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "MqttDisconnect [returnCode=" + returnCode + ", properties=" + properties + "]";
	}

}
