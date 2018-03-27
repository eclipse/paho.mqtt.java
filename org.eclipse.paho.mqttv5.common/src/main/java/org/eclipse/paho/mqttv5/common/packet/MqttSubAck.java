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
import java.util.Arrays;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.util.CountingInputStream;

public class MqttSubAck extends MqttAck {

	private static final int[] validReturnCodes = { MqttReturnCode.RETURN_CODE_MAX_QOS_0,
			MqttReturnCode.RETURN_CODE_MAX_QOS_1, MqttReturnCode.RETURN_CODE_MAX_QOS_2,
			MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR, MqttReturnCode.RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR,
			MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED, MqttReturnCode.RETURN_CODE_TOPIC_FILTER_NOT_VALID,
			MqttReturnCode.RETURN_CODE_PACKET_ID_IN_USE, MqttReturnCode.RETURN_CODE_SHARED_SUB_NOT_SUPPORTED };

	private static final Byte[] validProperties = { MqttProperties.REASON_STRING_IDENTIFIER,
			MqttProperties.USER_DEFINED_PAIR_IDENTIFIER };

	// Fields
	private MqttProperties properties;

	public MqttSubAck(byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_SUBACK);
		properties = new MqttProperties(validProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream inputStream = new DataInputStream(counter);
		msgId = inputStream.readUnsignedShort();
		this.properties.decodeProperties(inputStream);

		int remainingLength = data.length - counter.getCounter();
		reasonCodes = new int[remainingLength];

		for (int i = 0; i < remainingLength; i++) {
			int returnCode = inputStream.readUnsignedByte();
			validateReturnCode(returnCode, validReturnCodes);
			reasonCodes[i] = returnCode;
		}

		inputStream.close();
	}

	public MqttSubAck(int[] returnCodes, MqttProperties properties) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_SUBACK);
		for (int returnCode : returnCodes) {
			validateReturnCode(returnCode, validReturnCodes);
		}
		this.reasonCodes = returnCodes;
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

			// Encode the Message ID
			outputStream.writeShort(msgId);

			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();
			// Write Identifier / Value Fields
			outputStream.write(identifierValueFieldsByteArray);

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	@Override
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			for (int returnCode : reasonCodes) {
				outputStream.writeByte(returnCode);
			}

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	public int[] getReturnCodes() {
		return reasonCodes;
	}

	public void setReturnCodes(int[] returnCodes) {
		this.reasonCodes = returnCodes;
	}

	@Override
	public MqttProperties getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "MqttSubAck [returnCodes=" + Arrays.toString(reasonCodes) + ", properties=" + properties + "]";
	}

}
