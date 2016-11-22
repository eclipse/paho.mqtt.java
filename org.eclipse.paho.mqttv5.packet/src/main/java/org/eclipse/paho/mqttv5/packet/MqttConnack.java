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
package org.eclipse.paho.mqttv5.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.paho.mqttv5.util.MqttException;


/**
 * An on-the-wire representation of an MQTT CONNACK.
 */
public class MqttConnack extends MqttAck {
	public static final String KEY = "Con";
	
	// CONNACK Return Codes
	public static final int RETURN_CODE_SUCCESS 						= 0x00;
	public static final int RETURN_CODE_UNSPECIFIED_ERROR 				= 0x80;
	public static final int RETURN_CODE_MALFORMED_CONTROL_PACKET 		= 0x81;
	public static final int RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR 	= 0x83;
	public static final int RETURN_CODE_UNSUPPORTED_PROTOCOL_VERSION 	= 0x84;
	public static final int RETURN_CODE_IDENTIFIER_NOT_VALID			= 0x85;
	public static final int RETURN_CODE_BAD_USERNAME_OR_PASSWORD		= 0x86;
	public static final int RETURN_CODE_NOT_AUTHORIZED					= 0x87;
	public static final int RETURN_CODE_SERVER_UNAVAILABLE				= 0x88;
	public static final int RETURN_CODE_SERVER_BUSY						= 0x89;
	public static final int RETURN_CODE_BANNED							= 0x8A;
	public static final int RETURN_CODE_BAD_AUTHENTICATION				= 0x8C;
	public static final int RETURN_CODE_TOPIC_INVALID					= 0x90;
	public static final int RETURN_CODE_PACKET_TOO_LARGE				= 0x95;
	public static final int RETURN_CODE_USE_ANOTHER_SERVER				= 0x9C;
	public static final int RETURN_CODE_SERVER_MOVED					= 0x9D;
	
	// Identifier / Value Identifiers
	private static final byte RECEIVE_MAXIMUM_IDENTIFIER 					= 0x21;
	private static final byte RETAIN_UNAVAILABLE_ADVERTISEMENT_IDENTIFIER 	= 0x25;
	private static final byte ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER 		= 0x12;
	private static final byte TOPIC_ALIAS_MAXIMUM_IDENTIFIER 				= 0x22;
	private static final byte REASON_STRING_IDENTIFIER 						= 0x1F;
	private static final byte SERVER_KEEP_ALIVE_IDENTIFIER 					= 0x13;
	private static final byte REPLY_INFO_IDENTIFIER							= 0x1A;
	private static final byte SERVER_REFERENCE_IDENTIFIER 					= 0x1C;
	private static final byte AUTH_METHOD_IDENTIFIER 						= 0x15;
	private static final byte AUTH_DATA_IDENTIFIER 							= 0x16;
	
	// Fields
	private Integer receiveMaximum;
	private boolean retainUnavailableAdvertisement = false;
	private String assignedClientIdentifier;
	private Integer topicAliasMaximum;
	private String reasonString;
	private Integer serverKeepAlive;
	private String replyInfo;
	private String serverReference;
	private String authMethod;
	private byte[] authData;
	

	private int returnCode;
	private boolean sessionPresent;
	
	public MqttConnack(byte info, byte[] variableHeader) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNACK);
		ByteArrayInputStream bais = new ByteArrayInputStream(variableHeader);
		DataInputStream dis = new DataInputStream(bais);
		sessionPresent = (dis.readUnsignedByte() & 0x01) == 0x01;
		returnCode = dis.readUnsignedByte();
		parseIdentifierValueFields(dis);
		dis.close();
	}
	
	public MqttConnack(boolean sessionPresent, int returnCode){
		super(MqttWireMessage.MESSAGE_TYPE_CONNACK);
		this.sessionPresent = sessionPresent;
		this.returnCode = returnCode;
	}
	
	

	
	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
						
			// Encode the Session Present Flag
			byte connectAchnowledgeFlag = 0;
			if(sessionPresent){
				connectAchnowledgeFlag |= 0x01;
			}
			dos.write(connectAchnowledgeFlag);
			
			// Encode the Connect Return Code
			dos.write((byte) returnCode); 
			
			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = getIdentifierValueFields();
			dos.write(encodeVariableByteInteger(identifierValueFieldsByteArray.length));
			dos.write(identifierValueFieldsByteArray);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe){
			throw new MqttException(ioe);
		}
		
	}
	
	private byte[] getIdentifierValueFields() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);
			
			// If present, encode the Receive Maximum (3.2.2.5)
			if(receiveMaximum != null){
				outputStream.write(RECEIVE_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(receiveMaximum);
			}
			
			// If present, encode the Retain Unavailable Advertisement (3.2.2.6)
			if(retainUnavailableAdvertisement){
				outputStream.write(RETAIN_UNAVAILABLE_ADVERTISEMENT_IDENTIFIER);
			}
			
			// If present, encode the Assigned Client Identifier (3.2.2.7)
			if(assignedClientIdentifier != null){
				outputStream.write(ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER);
				encodeUTF8(outputStream, assignedClientIdentifier);
			}
			
			// If present, encode the Topic Alias Maximum (3.2.2.8)
			if(topicAliasMaximum != null){
				outputStream.write(TOPIC_ALIAS_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(topicAliasMaximum);
			}
			
			// If present, encode the Reason String (3.2.2.9)
			if(reasonString != null){
				outputStream.write(REASON_STRING_IDENTIFIER);
				encodeUTF8(outputStream, reasonString);
			}
			
			// If present, encode the Server Keep Alive (3.2.2.10)
			if(serverKeepAlive != null){
				outputStream.write(SERVER_KEEP_ALIVE_IDENTIFIER);
				outputStream.writeShort(serverKeepAlive);
			}
			
			// If present, encode the Reply Info (3.2.2.11)
			if(replyInfo != null){
				outputStream.write(REPLY_INFO_IDENTIFIER);
				encodeUTF8(outputStream, replyInfo);
			}
			
			// If present, encode the Server Reference (3.2.2.12)
			if(serverReference != null){
				outputStream.write(SERVER_REFERENCE_IDENTIFIER);
				encodeUTF8(outputStream, serverReference);
			}
			
			// If present, encode the Auth Method (3.2.2.13)
			if(authMethod != null){
				outputStream.write(AUTH_METHOD_IDENTIFIER);
				encodeUTF8(outputStream, authMethod);
			}
			
			// If present, encode the Auth Data (3.2.2.14)
			if(authData != null){
				outputStream.write(AUTH_DATA_IDENTIFIER);
				outputStream.writeShort(authData.length);
				outputStream.write(authData);
			}
			
			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe){
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
				if(identifier == RECEIVE_MAXIMUM_IDENTIFIER){
					receiveMaximum = (int) inputStream.readShort();
				} else if(identifier == RETAIN_UNAVAILABLE_ADVERTISEMENT_IDENTIFIER){
					retainUnavailableAdvertisement = true;
				} else if(identifier == ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER){
					assignedClientIdentifier = decodeUTF8(inputStream);
				} else if(identifier == TOPIC_ALIAS_MAXIMUM_IDENTIFIER){
					topicAliasMaximum = (int) inputStream.readShort();
				} else if(identifier == REASON_STRING_IDENTIFIER){
					reasonString = decodeUTF8(inputStream);
				} else if(identifier == SERVER_KEEP_ALIVE_IDENTIFIER){
					serverKeepAlive = (int) inputStream.readShort();
				} else if(identifier == REPLY_INFO_IDENTIFIER){
					replyInfo = decodeUTF8(inputStream);
				} else if(identifier == SERVER_REFERENCE_IDENTIFIER){
					serverReference = decodeUTF8(inputStream);
				} else if(identifier == AUTH_METHOD_IDENTIFIER){
					authMethod = decodeUTF8(inputStream);
				} else if(identifier == AUTH_DATA_IDENTIFIER){
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
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
	
	public int getReceiveMaximum() {
		return receiveMaximum;
	}

	public void setReceiveMaximum(Integer receiveMaximum) {
		this.receiveMaximum = receiveMaximum;
	}

	public boolean getRetainUnavailableAdvertisement() {
		return retainUnavailableAdvertisement;
	}

	public void setRetainUnavailableAdvertisement(Boolean retainUnavailableAdvertisement) {
		this.retainUnavailableAdvertisement = retainUnavailableAdvertisement;
	}

	public String getAssignedClientIdentifier() {
		return assignedClientIdentifier;
	}

	public void setAssignedClientIdentifier(String assignedClientIdentifier) {
		this.assignedClientIdentifier = assignedClientIdentifier;
	}

	public int getTopicAliasMaximum() {
		return topicAliasMaximum;
	}

	public void setTopicAliasMaximum(Integer topicAliasMaximum) {
		this.topicAliasMaximum = topicAliasMaximum;
	}

	public String getReasonString() {
		return reasonString;
	}

	public void setReasonString(String reasonString) {
		this.reasonString = reasonString;
	}

	public int getServerKeepAlive() {
		return serverKeepAlive;
	}

	public void setServerKeepAlive(int serverKeepAlive) {
		this.serverKeepAlive = serverKeepAlive;
	}

	public String getReplyInfo() {
		return replyInfo;
	}

	public void setReplyInfo(String replyInfo) {
		this.replyInfo = replyInfo;
	}

	public String getServerReference() {
		return serverReference;
	}

	public void setServerReference(String serverReference) {
		if((returnCode == RETURN_CODE_USE_ANOTHER_SERVER) || (returnCode == RETURN_CODE_SERVER_MOVED)){
			this.serverReference = serverReference;
		} else {
			// FIXME
			throw new IllegalArgumentException("The Server MUST only send a Server Reference along with a Return Code of 0x9C - Use another Server or 0x9D - Server Moved. (3.2.2.12)");
		}
		
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
	
	@Override
	public String toString() {
		return "MqttConnack [receiveMaximum=" + receiveMaximum + ", retainUnavailableAdvertisment="
				+ retainUnavailableAdvertisement + ", assignedClientIdentifier=" + assignedClientIdentifier
				+ ", topicAliasMaximum=" + topicAliasMaximum + ", reasonString=" + reasonString + ", serverKeepAlive="
				+ serverKeepAlive + ", replyInfo=" + replyInfo + ", serverReference=" + serverReference
				+ ", authMethod=" + authMethod + ", authData=" + Arrays.toString(authData) + ", returnCode="
				+ returnCode + ", sessionPresent=" + sessionPresent + "]";
	}
	
}
