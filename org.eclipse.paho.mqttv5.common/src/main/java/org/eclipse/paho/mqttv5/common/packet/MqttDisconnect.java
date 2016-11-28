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

	// Return codes
	public static final int RETURN_CODE_NORMAL_COMPLETION 				= 0x00;
	public static final int RETURN_CODE_UNSPECIFIED_ERROR 				= 0x80;
	public static final int RETURN_CODE_MALFORMED_CONTROL_PACKET 		= 0x81;
	public static final int RETURN_CODE_PROTOCOL_ERROR					= 0x82;
	public static final int RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR	= 0x83;
	public static final int RETURN_CODE_NOT_AUTHORIZED					= 0x87;
	public static final int RETURN_CODE_SERVER_BUSY						= 0x89;
	public static final int RETURN_CODE_SERVER_SHUTTING_DOWN			= 0x8B;
	public static final int RETURN_CODE_MAXIMUM_CONNECT_TIME			= 0x8C;
	public static final int RETURN_CODE_SESSION_TAKEN_OVER				= 0x8E;
	public static final int RETURN_CODE_KEEP_ALIVE_TIMEOUT				= 0x8F;
	public static final int RETURN_CODE_TOPIC_NAME_OR_FILTER_NOT_VALID	= 0x90;
	public static final int RETURN_CODE_PACKET_TOO_LARGE				= 0x95;
	public static final int RETURN_CODE_MESSAGE_RATE_TOO_HIGH			= 0x96;
	public static final int RETURN_CODE_QUOTA_EXCEEDED					= 0x97;
	public static final int RETURN_CODE_ADMINISTRITIVE_ACTION			= 0x98;
	public static final int RETURN_CODE_DISCONNECT_WITH_WILL_MESSAGE	= 0x99;
	public static final int RETURN_CODE_ALIAS_NOT_ACCEPTED				= 0x9A;
	public static final int RETURN_CODE_NO_TOPIC						= 0x9B;
	public static final int RETURN_CODE_USE_ANOTHER_SERVER				= 0x9C;
	public static final int RETURN_CODE_SERVER_MOVED					= 0x9D;
	
	private static final int[] validReturnCodes = {
			RETURN_CODE_NORMAL_COMPLETION,
			RETURN_CODE_UNSPECIFIED_ERROR,
			RETURN_CODE_MALFORMED_CONTROL_PACKET,
			RETURN_CODE_PROTOCOL_ERROR,
			RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR,
			RETURN_CODE_NOT_AUTHORIZED,
			RETURN_CODE_SERVER_BUSY,
			RETURN_CODE_SERVER_SHUTTING_DOWN,
			RETURN_CODE_MAXIMUM_CONNECT_TIME,
			RETURN_CODE_SESSION_TAKEN_OVER,
			RETURN_CODE_KEEP_ALIVE_TIMEOUT,
			RETURN_CODE_TOPIC_NAME_OR_FILTER_NOT_VALID,
			RETURN_CODE_PACKET_TOO_LARGE,
			RETURN_CODE_MESSAGE_RATE_TOO_HIGH,
			RETURN_CODE_QUOTA_EXCEEDED,
			RETURN_CODE_ADMINISTRITIVE_ACTION,
			RETURN_CODE_DISCONNECT_WITH_WILL_MESSAGE,
			RETURN_CODE_ALIAS_NOT_ACCEPTED,
			RETURN_CODE_NO_TOPIC,
			RETURN_CODE_USE_ANOTHER_SERVER,
			RETURN_CODE_SERVER_MOVED
	};
	
	// Identifier / Value Identifiers
	private static final byte SESSION_EXPIRY_INTERVAL_IDENTIFIER 	= 0x11;
	private static final byte REASON_STRING_IDENTIFIER				= 0x1F;
	private static final byte SERVER_REFERENCE_IDENTIFIER			= 0x1C;
	
	// Fields
	private int returnCode;
	private Integer sessionExpiryInterval;
	private String reasonString;
	private String serverReference;
	
	public MqttDisconnect(byte info, byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_DISCONNECT);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream inputStream = new DataInputStream(counter);
		returnCode = inputStream.readUnsignedByte();
		validateReturnCode(returnCode, validReturnCodes);
		parseIdentifierValueFields(inputStream);
		
		inputStream.close();
	}
	
	public MqttDisconnect(int returnCode) throws MqttException{
		super(MqttWireMessage.MESSAGE_TYPE_DISCONNECT);
		validateReturnCode(returnCode, validReturnCodes);
		this.returnCode = returnCode;
	}
	
	@Override
	protected byte[] getVariableHeader() throws MqttException{
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
			
			// If Present, encode the Session Expiry Interval (3.14.2.3)
			if(sessionExpiryInterval != null){
				outputStream.write(SESSION_EXPIRY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(sessionExpiryInterval);
			}
			
			// If Present, encode the Reason String (3.14.2.4)
			if (reasonString != null) {
				outputStream.write(REASON_STRING_IDENTIFIER);
				encodeUTF8(outputStream, reasonString);
			}
			
			// If present, encode the Server Reference (3.14.2.5)
			if(serverReference != null){
				outputStream.write(SERVER_REFERENCE_IDENTIFIER);
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
		if(lengthVBI > 0){
			byte[] identifierValueByteArray = new byte[lengthVBI];
			dis.read(identifierValueByteArray, 0, lengthVBI);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
			while(inputStream.available() > 0){
				// Get the first byte (identifier)
				byte identifier = inputStream.readByte();
				if(identifier == SESSION_EXPIRY_INTERVAL_IDENTIFIER){
					sessionExpiryInterval = inputStream.readInt();
				}  else if(identifier == REASON_STRING_IDENTIFIER){
					reasonString = decodeUTF8(inputStream);
				}  else if(identifier == SERVER_REFERENCE_IDENTIFIER){
					serverReference = decodeUTF8(inputStream);
				}  else {
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
	@Override
	public String toString() {
		return "MqttDisconnect [returnCode=" + returnCode + ", sessionExpiryInterval=" + sessionExpiryInterval
				+ ", reasonString=" + reasonString + ", serverReference=" + serverReference + "]";
	}

	@Override
	protected byte getMessageInfo() {
		return (byte) 0;
	}
	
	
	
}
