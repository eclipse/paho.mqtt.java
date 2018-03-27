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

public class MqttPubAck extends MqttAck {

	private static final int[] validReturnCodes = { MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_NO_MATCHING_SUBSCRIBERS, MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR,
			MqttReturnCode.RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR, MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED,
			MqttReturnCode.RETURN_CODE_TOPIC_NAME_INVALID, MqttReturnCode.RETURN_CODE_QUOTA_EXCEEDED,
			MqttReturnCode.RETURN_CODE_PAYLOAD_FORMAT_INVALID };
	
	private static final Byte[] validProperties = {MqttProperties.REASON_STRING_IDENTIFIER, MqttProperties.USER_DEFINED_PAIR_IDENTIFIER};
	
	private MqttProperties properties;
	

	public MqttPubAck( byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_PUBACK);
		properties = new MqttProperties(validProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream dis = new DataInputStream(counter);
		msgId = dis.readUnsignedShort();
		long remainder = (long)data.length - counter.getCounter();
		if (remainder > 2) {
			reasonCode = dis.readUnsignedByte();
			validateReturnCode(reasonCode, validReturnCodes);
		}
		 if( remainder >= 4) {
			 this.properties.decodeProperties(dis);
		 }
		dis.close();
	}

	public MqttPubAck(int returnCode, int msgId, MqttProperties properties) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_PUBACK);
		this.reasonCode = returnCode;
		this.msgId = msgId;
		if (properties != null) {
			this.properties = properties;
		} else {
			this.properties = new MqttProperties();
		}
		this.properties.setValidProperties(validProperties);
		validateReturnCode(returnCode, validReturnCodes);
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Encode the Message ID
			outputStream.writeShort(msgId);

			byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();

			if (reasonCode != MqttReturnCode.RETURN_CODE_SUCCESS || identifierValueFieldsByteArray.length != 0) {

				// Encode the Return Code
				outputStream.write((byte) reasonCode);

				// Write Identifier / Value Fields
				outputStream.write(identifierValueFieldsByteArray);
			}

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
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
		return "MqttPubAck [returnCode=" + reasonCode + ", properties=" + properties + "]";
	}

}
