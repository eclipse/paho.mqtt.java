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

/**
 * An on-the-wire representation of an MQTT AUTH message. MQTTv5 - 3.15
 */
public class MqttAuth extends MqttWireMessage {

	// Return codes
	private static final int[] validReturnCodes = { MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_CONTINUE_AUTHENTICATION, MqttReturnCode.RETURN_CODE_RE_AUTHENTICATE };

	private static final Byte[] validProperties = { MqttProperties.AUTH_METHOD_IDENTIFIER,
			MqttProperties.AUTH_DATA_IDENTIFIER, MqttProperties.REASON_STRING_IDENTIFIER,
			MqttProperties.USER_DEFINED_PAIR_IDENTIFIER };

	// Fields
	private MqttProperties properties;

	/**
	 * Constructs an Auth message from a raw byte array
	 * 
	 * @param data
	 *            - The variable header and payload bytes.
	 * @throws IOException
	 *             - if an exception occurs when decoding an input stream
	 * @throws MqttException
	 *             - If an exception occurs decoding this packet
	 */
	public MqttAuth(byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_AUTH);
		properties = new MqttProperties(validProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream inputStream = new DataInputStream(bais);
		reasonCode = inputStream.readUnsignedByte();
		validateReturnCode(reasonCode, validReturnCodes);
		this.properties.decodeProperties(inputStream);
		inputStream.close();
	}

	/**
	 * Constructs an Auth message from the return code
	 * 
	 * @param returnCode
	 *            - The Auth Return Code
	 * @param properties
	 *            - The {@link MqttProperties} for the packet.
	 * @throws MqttException
	 *             - If an exception occurs encoding this packet
	 */
	public MqttAuth(int returnCode, MqttProperties properties) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_AUTH);
		if (properties != null) {
			this.properties = properties;
		} else {
			this.properties = new MqttProperties();
		}
		this.properties.setValidProperties(validProperties);
		validateReturnCode(returnCode, validReturnCodes);
		this.reasonCode = returnCode;
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Encode the Return Code
			outputStream.writeByte(reasonCode);

			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();
			outputStream.write(identifierValueFieldsByteArray);
			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	@Override
	protected byte getMessageInfo() {
		return (byte) (1);
	}

	public int getReturnCode() {
		return reasonCode;
	}

	@Override
	public MqttProperties getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "MqttAuth [returnCode=" + reasonCode + ", properties=" + properties + "]";
	}
}
