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
package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * An on-the-wire representation of an MQTT CONNACK.
 */
public class MqttConnAck extends MqttAck {
	public static final String KEY = "Con";

	private static final int[] validReturnCodes = { MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR, MqttReturnCode.RETURN_CODE_MALFORMED_CONTROL_PACKET,
			MqttReturnCode.RETURN_CODE_PROTOCOL_ERROR, MqttReturnCode.RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR,
			MqttReturnCode.RETURN_CODE_UNSUPPORTED_PROTOCOL_VERSION, MqttReturnCode.RETURN_CODE_IDENTIFIER_NOT_VALID,
			MqttReturnCode.RETURN_CODE_BAD_USERNAME_OR_PASSWORD, MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED,
			MqttReturnCode.RETURN_CODE_SERVER_UNAVAILABLE, MqttReturnCode.RETURN_CODE_SERVER_BUSY,
			MqttReturnCode.RETURN_CODE_BANNED, MqttReturnCode.RETURN_CODE_BAD_AUTHENTICATION,
			MqttReturnCode.RETURN_CODE_TOPIC_NAME_INVALID, MqttReturnCode.RETURN_CODE_PACKET_TOO_LARGE,
			MqttReturnCode.RETURN_CODE_QUOTA_EXCEEDED, MqttReturnCode.RETURN_CODE_RETAIN_NOT_SUPPORTED,
			MqttReturnCode.RETURN_CODE_USE_ANOTHER_SERVER, MqttReturnCode.RETURN_CODE_SERVER_MOVED,
			MqttReturnCode.RETURN_CODE_CONNECTION_RATE_EXCEEDED };

	private static final Byte[] validProperties = { MqttProperties.SESSION_EXPIRY_INTERVAL_IDENTIFIER,
			MqttProperties.RECEIVE_MAXIMUM_IDENTIFIER, MqttProperties.MAXIMUM_QOS_IDENTIFIER,
			MqttProperties.RETAIN_AVAILABLE_IDENTIFIER, MqttProperties.MAXIMUM_PACKET_SIZE_IDENTIFIER,
			MqttProperties.ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER, MqttProperties.TOPIC_ALIAS_MAXIMUM_IDENTIFIER,
			MqttProperties.WILDCARD_SUB_AVAILABLE_IDENTIFIER, MqttProperties.SUBSCRIPTION_AVAILABLE_IDENTIFIER,
			MqttProperties.SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER, MqttProperties.SERVER_KEEP_ALIVE_IDENTIFIER,
			MqttProperties.RESPONSE_INFO_IDENTIFIER, MqttProperties.SERVER_REFERENCE_IDENTIFIER,
			MqttProperties.AUTH_METHOD_IDENTIFIER, MqttProperties.AUTH_DATA_IDENTIFIER,
			MqttProperties.REASON_STRING_IDENTIFIER, MqttProperties.USER_DEFINED_PAIR_IDENTIFIER };

	private boolean sessionPresent;
	private MqttProperties properties;

	public MqttConnAck(byte[] variableHeader) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNACK);
		this.properties = new MqttProperties(validProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(variableHeader);
		DataInputStream dis = new DataInputStream(bais);
		sessionPresent = (dis.readUnsignedByte() & 0x01) == 0x01;
		reasonCode = dis.readUnsignedByte();
		validateReturnCode(reasonCode, validReturnCodes);
		this.properties.decodeProperties(dis);
		dis.close();
	}

	public MqttConnAck(boolean sessionPresent, int returnCode, MqttProperties properties) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNACK);
		if (properties != null) {
			this.properties = properties;
		} else {
			this.properties = new MqttProperties();
		}
		this.properties.setValidProperties(validProperties);
		this.sessionPresent = sessionPresent;
		validateReturnCode(returnCode, validReturnCodes);
		this.reasonCode = returnCode;
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			// Encode the Session Present Flag
			byte connectAchnowledgeFlag = 0;
			if (sessionPresent) {
				connectAchnowledgeFlag |= 0x01;
			}
			dos.write(connectAchnowledgeFlag);

			// Encode the Connect Return Code
			dos.write((byte) reasonCode);

			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();
			dos.write(identifierValueFieldsByteArray);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}

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

	public boolean getSessionPresent() {
		return sessionPresent;
	}

	public void setSessionPresent(boolean sessionPresent) {
		this.sessionPresent = sessionPresent;
	}

	public int getReturnCode() {
		return reasonCode;
	}

	public void setReturnCode(int returnCode) {
		this.reasonCode = returnCode;
	}

	@Override
	public MqttProperties getProperties() {
		return properties;
	}

	public static int[] getValidreturncodes() {
		return validReturnCodes;
	}

	@Override
	public String toString() {
		return "MqttConnAck [returnCode=" + reasonCode + ", sessionPresent=" + sessionPresent + ", properties="
				+ properties + "]";
	}

}
