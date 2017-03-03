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
 * An on-the-wire representation of an MQTT AUTH message.
 * MQTTv5 - 3.15
 */
public class MqttAuth extends MqttWireMessage {

	// Return codes

	
	private static final int[] validReturnCodes = {
			MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_CONTINUE_AUTHENTICATION,
			MqttReturnCode.RETURN_CODE_RE_AUTHENTICATE
	};
	
	// Identifier / Value Identifiers
	private static final byte AUTH_METHOD_IDENTIFIER 	= 0x15;
	private static final byte AUTH_DATA_IDENTIFIER 		= 0x16;
	
	// Fields
	private int returnCode;
	private String authMethod;
	private byte[] authData;
	
	
	/**
	 * Constructs an Auth message from a raw byte array
	 * @param info - Info Byte
	 * @param data - The Data
	 * @throws IOException
	 * @throws MqttException
	 */
	public MqttAuth(byte info, byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_AUTH);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream inputStream  = new DataInputStream(bais);
		returnCode = inputStream.readUnsignedByte();
		parseIdentifierValueFields(inputStream);
		inputStream.close();
	}
	
	
	/**
	 * Constructs an Auth message from the return code
	 * @param returnCode - The Auth Return Code
	 * @throws MqttException
	 */
	public MqttAuth(int returnCode) throws MqttException{
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
			
			// If Present, encode the Auth Method (3.15.2.3)
			if (authMethod != null) {
				outputStream.write(AUTH_METHOD_IDENTIFIER);
				encodeUTF8(outputStream, authMethod);
			}
			
			// If present, encode the Auth Data (3.15.2.4)
			if(authData != null){
				outputStream.write(AUTH_DATA_IDENTIFIER);
				outputStream.writeShort(authData.length);
				outputStream.write(authData);
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
		if(lengthVBI > 0){
			byte[] identifierValueByteArray = new byte[lengthVBI];
			dis.read(identifierValueByteArray, 0, lengthVBI);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
			while(inputStream.available() > 0){
				// Get the first byte (identifier)
				byte identifier = inputStream.readByte();
				if(identifier ==  AUTH_METHOD_IDENTIFIER){
					authMethod = decodeUTF8(inputStream);
				} else if(identifier ==  AUTH_DATA_IDENTIFIER){
					int authDataLength = inputStream.readShort();
					authData = new byte[authDataLength];
					inputStream.read(authData, 0, authDataLength);
				} else {
					// Unidentified Identifier
					throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
				}
			}
		}
	}
	
	

	@Override
	protected byte getMessageInfo() {
		return (byte)(1);
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
	
	
}
