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
import java.util.ArrayList;
import java.util.List;

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
	private Integer sessionExpiryInterval;
	private String reasonString;
	private String serverReference;
	private List<UserProperty> userDefinedProperties = new ArrayList<>();

	public MqttDisconnect(byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_DISCONNECT);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream inputStream = new DataInputStream(counter);
		if(data.length - counter.getCounter() >= 1) {
			returnCode = inputStream.readUnsignedByte();
			validateReturnCode(returnCode, validReturnCodes);
		}
		
		long remainder = (long) data.length - counter.getCounter();
		if (remainder >= 2) {
			parseIdentifierValueFields(inputStream);
		}

		inputStream.close();
	}

	public MqttDisconnect(int returnCode) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_DISCONNECT);
		validateReturnCode(returnCode, validReturnCodes);
		this.returnCode = returnCode;
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Encode the Return Code
			outputStream.writeByte(returnCode);

			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = getIdentifierValueFields();
			if (identifierValueFieldsByteArray.length != 0) {
				outputStream.write(encodeVariableByteInteger(identifierValueFieldsByteArray.length));
				outputStream.write(identifierValueFieldsByteArray);
				outputStream.flush();
			}
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	private byte[] getIdentifierValueFields() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// If Present, encode the Session Expiry Interval (3.14.2.2.2)
			if (sessionExpiryInterval != null) {
				outputStream.write(MqttPropertyIdentifiers.SESSION_EXPIRY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(sessionExpiryInterval);
			}

			// If Present, encode the Reason String (3.14.2.2.3)
			if (reasonString != null) {
				outputStream.write(MqttPropertyIdentifiers.REASON_STRING_IDENTIFIER);
				encodeUTF8(outputStream, reasonString);
			}

			// If Present, encode the User Defined Name-Value Pairs (3.14.2.2.4)
			if (!userDefinedProperties.isEmpty()) {
				for (UserProperty property : userDefinedProperties) {
					outputStream.write(MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(outputStream, property.getKey());
					encodeUTF8(outputStream, property.getValue());
				}
			}

			// If present, encode the Server Reference (3.14.2.2.5)
			if (serverReference != null) {
				outputStream.write(MqttPropertyIdentifiers.SERVER_REFERENCE_IDENTIFIER);
				encodeUTF8(outputStream, serverReference);
			}

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	private void parseIdentifierValueFields(DataInputStream dis) throws IOException, MqttException {
		// First, get the length of the IV fields
		int lengthVBI = readVariableByteInteger(dis).getValue();
		if (lengthVBI > 0) {
			byte[] identifierValueByteArray = new byte[lengthVBI];
			dis.read(identifierValueByteArray, 0, lengthVBI);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
			while (inputStream.available() > 0) {
				// Get the first byte (identifier)
				byte identifier = inputStream.readByte();
				if (identifier == MqttPropertyIdentifiers.SESSION_EXPIRY_INTERVAL_IDENTIFIER) {
					sessionExpiryInterval = inputStream.readInt();
				} else if (identifier == MqttPropertyIdentifiers.REASON_STRING_IDENTIFIER) {
					reasonString = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER) {
					String key = decodeUTF8(inputStream);
					String value = decodeUTF8(inputStream);
					userDefinedProperties.add(new UserProperty(key, value));
				} else if (identifier == MqttPropertyIdentifiers.SERVER_REFERENCE_IDENTIFIER) {
					serverReference = decodeUTF8(inputStream);
				} else {
					// Unidentified Identifier
					throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
				}
			}
		}
	}

	public int getSessionExpiryInterval() {
		return sessionExpiryInterval;
	}

	public void setSessionExpiryInterval(Integer sessionExpiryInterval) {
		this.sessionExpiryInterval = sessionExpiryInterval;
	}

	public String getReasonString() {
		return reasonString;
	}

	public void setReasonString(String reasonString) {
		this.reasonString = reasonString;
	}

	public String getServerReference() {
		return serverReference;
	}

	public void setServerReference(String serverReference) {
		this.serverReference = serverReference;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public List<UserProperty> getUserDefinedProperties() {
		return userDefinedProperties;
	}

	public void setUserDefinedProperties(List<UserProperty> userDefinedProperties) {
		this.userDefinedProperties = userDefinedProperties;
	}

	@Override
	public String toString() {
		return "MqttDisconnect [returnCode=" + returnCode + ", sessionExpiryInterval=" + sessionExpiryInterval
				+ ", reasonString=" + reasonString + ", serverReference=" + serverReference + ", userDefinedProperties="
				+ userDefinedProperties + "]";
	}

	@Override
	protected byte getMessageInfo() {
		return (byte) 0;
	}

}
