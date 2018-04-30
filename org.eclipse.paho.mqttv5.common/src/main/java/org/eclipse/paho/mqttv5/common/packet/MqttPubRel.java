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

public class MqttPubRel extends MqttPersistableWireMessage {

	private static final int[] validReturnCodes = { MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_PACKET_ID_NOT_FOUND };

	private static final Byte[] validProperties = { MqttProperties.REASON_STRING_IDENTIFIER,
			MqttProperties.USER_DEFINED_PAIR_IDENTIFIER };

	// Fields
	private MqttProperties properties;

	public MqttPubRel(byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_PUBREL);
		properties = new MqttProperties(validProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream dis = new DataInputStream(counter);
		msgId = dis.readUnsignedShort();
		long remainder = (long) data.length - counter.getCounter();
		if (remainder > 2) {
			reasonCode = dis.readUnsignedByte();
			validateReturnCode(reasonCode, validReturnCodes);
		} else {
			reasonCode = 0;
		}
		if (remainder >= 4) {
			this.properties.decodeProperties(dis);
		}
		dis.close();
	}

	public MqttPubRel(int returnCode, int msgId, MqttProperties properties) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_PUBREL);
		validateReturnCode(returnCode, validReturnCodes);
		this.reasonCode = returnCode;
		this.msgId = msgId;
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

	@Override
	protected byte getMessageInfo() {
		return (byte) (2 | (this.duplicate ? 8 : 0));
	}

	public int getReturnCode() {
		return reasonCode;
	}

	public void setReturnCode(int returnCode) {
		this.reasonCode = returnCode;
	}

	@Override
	public MqttProperties getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "MqttPubRel [returnCode=" + reasonCode + ", properties=" + properties + "]";
	}

}
