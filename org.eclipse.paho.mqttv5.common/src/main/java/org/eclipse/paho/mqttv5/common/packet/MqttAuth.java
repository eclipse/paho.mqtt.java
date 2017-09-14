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

import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * An on-the-wire representation of an MQTT AUTH message. MQTTv5 - 3.15
 */
public class MqttAuth extends MqttWireMessage {

	// Return codes

	private static final int[] validReturnCodes = { MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_CONTINUE_AUTHENTICATION, MqttReturnCode.RETURN_CODE_RE_AUTHENTICATE };

	// Fields
	private int returnCode;
	private String authMethod;
	private byte[] authData;
	private String reasonString;
	private ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();

	/**
	 * Constructs an Auth message from a raw byte array
	 * 
	 * @param info
	 *            - Info Byte
	 * @param data
	 *            - The Data
	 * @throws IOException
	 *             - if an exception occurs when decoding an input stream
	 * @throws MqttException
	 *             - If an exception occurs decoding this packet
	 */
	public MqttAuth(byte info, byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_AUTH);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream inputStream = new DataInputStream(bais);
		returnCode = inputStream.readUnsignedByte();
		validateReturnCode(returnCode, validReturnCodes);
		parseIdentifierValueFields(inputStream);
		inputStream.close();
	}

	/**
	 * Constructs an Auth message from the return code
	 * 
	 * @param returnCode
	 *            - The Auth Return Code
	 * @throws MqttException
	 *             - If an exception occurs encoding this packet
	 */
	public MqttAuth(int returnCode) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_AUTH);
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
			outputStream.write(encodeVariableByteInteger(identifierValueFieldsByteArray.length));
			outputStream.write(identifierValueFieldsByteArray);
			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	private byte[] getIdentifierValueFields() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// If Present, encode the Auth Method (3.15.2.2.2)
			if (authMethod != null) {
				outputStream.write(MqttPropertyIdentifiers.AUTH_METHOD_IDENTIFIER);
				encodeUTF8(outputStream, authMethod);
			}

			// If present, encode the Auth Data (3.15.2.2.3)
			if (authData != null) {
				outputStream.write(MqttPropertyIdentifiers.AUTH_DATA_IDENTIFIER);
				outputStream.writeShort(authData.length);
				outputStream.write(authData);
			}

			// If Present, encode the Reason String (3.15.2.2.?)
			if (reasonString != null) {
				outputStream.write(MqttPropertyIdentifiers.REASON_STRING_IDENTIFIER);
				encodeUTF8(outputStream, reasonString);
			}

			// If Present, encode the User Properties (3.15.2.2.4)
			if (userDefinedProperties.size() != 0) {
				for (UserProperty property : userDefinedProperties) {
					outputStream.write(MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(outputStream, property.getKey());
					encodeUTF8(outputStream, property.getValue());
				}
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
				if (identifier == MqttPropertyIdentifiers.AUTH_METHOD_IDENTIFIER) {
					authMethod = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.AUTH_DATA_IDENTIFIER) {
					int authDataLength = inputStream.readShort();
					authData = new byte[authDataLength];
					inputStream.read(authData, 0, authDataLength);
				} else if (identifier == MqttPropertyIdentifiers.REASON_STRING_IDENTIFIER) {
					reasonString = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER) {
					String key = decodeUTF8(inputStream);
					String value = decodeUTF8(inputStream);
					userDefinedProperties.add(new UserProperty(key, value));
				} else {
					// Unidentified Identifier
					throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
				}
			}
		}
	}

	@Override
	protected byte getMessageInfo() {
		return (byte) (1);
	}

	public String getAuthMethod() {
		return authMethod;
	}

	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	public byte[] getAuthData() {
		return authData;
	}

	public void setAuthData(byte[] authData) {
		this.authData = authData;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public String getReasonString() {
		return reasonString;
	}

	public void setReasonString(String reasonString) {
		this.reasonString = reasonString;
	}

	public ArrayList<UserProperty> getUserDefinedProperties() {
		return userDefinedProperties;
	}

	public void setUserDefinedProperties(ArrayList<UserProperty> userDefinedProperties) {
		this.userDefinedProperties = userDefinedProperties;
	}

}
