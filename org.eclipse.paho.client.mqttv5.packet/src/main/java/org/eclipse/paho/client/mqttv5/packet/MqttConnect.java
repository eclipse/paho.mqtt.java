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
package org.eclipse.paho.client.mqttv5.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv5.util.MqttException;

public class MqttConnect extends MqttWireMessage {
	
	public static final String KEY = "Con";
	
	private byte info;
	private String clientId;
	private boolean cleanSession;
	private MqttMessage willMessage;
	private String userName;
	private byte[] password;
	private int keepAliveInterval;
	private String willDestination;
	private int mqttVersion;
	
	private static final byte SESSION_EXPIRY_INTERVAL_IDENTIFIER = 17;
	private static final byte WILL_DELAY_INTERVAL_IDENTIFIER = 24;
	private static final byte RECEIVE_MAXIMUM_IDENTIFIER = 33;
	private static final byte TOPIC_ALIAS_MAXIMUM_IDENTIFIER = 34;
	private static final byte REQUEST_REPLY_INFO_IDENTIFIER = 25;
	private static final byte REQUEST_PROBLEM_INFO_IDENTIFIER = 23;
	private static final byte USER_DEFINED_PAIR_IDENTIFIER = 38;
	private static final byte AUTH_METHOD_IDENTIFIER = 21;
	private static final byte AUTH_DATA_IDENTIFIER = 22;
	
	// Identifier / Value Fields
	private Integer sessionExpiryInterval;
	private Integer willDelayInterval;
	private Integer receiveMaximum;
	private Integer topicAliasMaximum;
	private Boolean requestReplyInfo;
	private Boolean requestProblemInfo;
	private Map<String, String> userDefinedPairs = new HashMap<>();
	private String authMethod;
	private byte[] authData;
	

	
	/**
	 * Constructor for an on the wire MQTT connect message
	 * 
	 * @param info
	 * @param data
	 * @throws IOException
	 * @throws MqttException
	 */
	public MqttConnect(byte info, byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.info = info;
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
		
		String protocolName = decodeUTF8(dis);
		if(!protocolName.equalsIgnoreCase(MQTT)){
			throw new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION);
		}
		mqttVersion = dis.readByte();
		
		byte connectFlags = dis.readByte();
		cleanSession = (connectFlags & 0x02) != 0;
		boolean willFlag = (connectFlags & 0x04) != 0;
		int willQoS = (connectFlags >>3) & 0x03;
		boolean willRetain = (connectFlags & 0x20) != 0;
		boolean passwordFlag = (connectFlags & 0x40) !=0;
		boolean usernameFlag = (connectFlags & 0x80) !=0;
		
		keepAliveInterval = dis.readUnsignedShort();
		parseIdentifierValueFields(dis);
		clientId = decodeUTF8(dis);
		if(willFlag){
			willDestination = decodeUTF8(dis);
			int willMessageLength = dis.readShort();
			byte[] willMessageBytes = new byte[willMessageLength];
			dis.read(willMessageBytes);
			willMessage = new MqttMessage(willMessageBytes);
			willMessage.setQos(willQoS);
			willMessage.setRetained(willRetain);
		}
		if(usernameFlag){
			userName = decodeUTF8(dis);
		}
		if(passwordFlag){
			int passwordLength = dis.readShort();
			password = new byte[passwordLength];
			dis.read(password);
		}
		
		
		dis.close();
	}
	
	public MqttConnect(String clientId, int mqttVersion, boolean cleanSession, int keepAliveInterval, String userName, byte[] password, MqttMessage willMessage, String willDestination) {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.clientId = clientId;
		this.cleanSession = cleanSession;
		this.keepAliveInterval = keepAliveInterval;
		this.userName = userName;
		this.password = password;
		this.willMessage = willMessage;
		this.willDestination = willDestination;
		this.mqttVersion = mqttVersion;
	}
	
	
	@Override
	protected byte getMessageInfo() {
		return (byte) 0;
	}
	
	public boolean isCleanSession(){
		return cleanSession;
	}
	
	
	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			
			// Encode the Protocol Name
			encodeUTF8(dos, "MQTT");
			
			// Encode the MQTT Version
			dos.write(mqttVersion);
			
			byte connectFlags = 0;
			
			if(cleanSession){
				connectFlags |= 0x02;
			}
			
			if(willMessage != null){
				connectFlags |= 0x04;
				connectFlags |= (willMessage.getQos()<<3);
				if(willMessage.isRetained()){
					connectFlags |= 0x20;
				}
			}
			
			if(userName != null){
				connectFlags |= 0x80;
				if(password != null){
					connectFlags |= 0x40;
				}
			}
			
			dos.write(connectFlags);
			dos.writeShort(keepAliveInterval);
			
			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = getIdentifierValueFields();
			dos.write(encodeVariableByteInteger(identifierValueFieldsByteArray.length));
			dos.write(identifierValueFieldsByteArray);
			dos.flush();
			return baos.toByteArray();
		} catch(IOException ioe){
			throw new MqttException(ioe);
		}
	}
	
	
	
	private byte[] getIdentifierValueFields() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream= new DataOutputStream(baos);
			
			// If Present, encode the Session Expiry Interval (3.1.2.12)
			if(sessionExpiryInterval != null){
				outputStream.write(SESSION_EXPIRY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(sessionExpiryInterval);
			}
			
			// If Present, encode the Will Delay Interval (3.1.2.13)
			if(willDelayInterval != null){
				outputStream.write(WILL_DELAY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(willDelayInterval);
			}
			
			// If present, encode the Receive Maximum (3.1.2.14)
			if(receiveMaximum != null){
				outputStream.write(RECEIVE_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(receiveMaximum);
			}
			
			// If present, encode the Topic Alias Maximum (3.1.2.15)
			if(topicAliasMaximum != null){
				outputStream.write(TOPIC_ALIAS_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(topicAliasMaximum);
			}
			
			// If present, encode the Request Reply Info (3.1.2.16)
			if(requestReplyInfo != null){
				outputStream.write(REQUEST_REPLY_INFO_IDENTIFIER);
				outputStream.write(requestReplyInfo ? 1 : 0);
			}
			
			// If present, encode the Request Problem Info (3.1.2.17)
			if(requestProblemInfo != null){
				outputStream.write(REQUEST_PROBLEM_INFO_IDENTIFIER);
				outputStream.write(requestProblemInfo ? 1 : 0);
			}
			
			// If present, encode the User Defined Name-Value Pairs (3.1.2.18)
			if(userDefinedPairs.size() != 0){
				for(Map.Entry<String, String> entry : userDefinedPairs.entrySet()){
					String identifier = entry.getKey();
					String value = entry.getValue();
					outputStream.write(USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(outputStream, identifier);
					encodeUTF8(outputStream, value);
				}
			}
			
			// If present, encode the Auth Method (3.1.2.19)
			if(authMethod != null){
				outputStream.write(AUTH_METHOD_IDENTIFIER);
				encodeUTF8(outputStream, authMethod);
			}
			
			// If present, encode the Auth Data (3.1.2.20)
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
	
	private void parseIdentifierValueFields(DataInputStream dis) throws IOException, MqttException{
		// First get the length of the IV fields
		int lengthMbi = readVariableByteInteger(dis).getValue();
		if(lengthMbi > 0){
			byte[] identifierValueByteArray = new byte[lengthMbi];
			dis.read(identifierValueByteArray);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
		
			while(inputStream.available() > 0){
				// Get the first byte (Identifier)
				byte identifier = inputStream.readByte();
				
				if(identifier ==  SESSION_EXPIRY_INTERVAL_IDENTIFIER){
					sessionExpiryInterval = inputStream.readInt();
				} else if(identifier ==  WILL_DELAY_INTERVAL_IDENTIFIER){
					willDelayInterval = inputStream.readInt();
				} else if(identifier ==  RECEIVE_MAXIMUM_IDENTIFIER){
					receiveMaximum = (int) inputStream.readShort();
				} else if(identifier ==  TOPIC_ALIAS_MAXIMUM_IDENTIFIER){
					topicAliasMaximum = (int) inputStream.readShort();
				} else if(identifier ==  REQUEST_REPLY_INFO_IDENTIFIER){
					requestReplyInfo = inputStream.read() !=0;
				} else if(identifier ==  REQUEST_PROBLEM_INFO_IDENTIFIER){
					requestProblemInfo = inputStream.read() != 0;
				} else if(identifier ==  USER_DEFINED_PAIR_IDENTIFIER){
					String key = decodeUTF8(inputStream);
					String value = decodeUTF8(inputStream);
					userDefinedPairs.put(key, value);
				} else if(identifier ==  AUTH_METHOD_IDENTIFIER){
					authMethod = decodeUTF8(inputStream);
				} else if(identifier ==  AUTH_DATA_IDENTIFIER){
					int authDataLength = inputStream.readShort();
					authData = new byte[authDataLength];
					inputStream.read(authData);
				} else {
					// Unidentified Identifier
					throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
				}
			}
		}
	}
	
	
	@Override
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			encodeUTF8(dos, clientId);
			
			if(willMessage != null){
				encodeUTF8(dos, willDestination);
				dos.writeShort(willMessage.getPayload().length);
				dos.write(willMessage.getPayload());
			}
			
			if(userName != null){
				encodeUTF8(dos, userName);
				if(password != null){
					encodeUTF8(dos, new String(password));
				}
			}
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe){
			throw new MqttException(ioe);
		}
	}
	
	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	@Override
	public boolean isMessageIdRequired(){
		return false;
	}
	
	@Override
	public String getKey() {
		return KEY;
	}

	public void setSessionExpiryInterval(int sessionExpiryInterval) {
		this.sessionExpiryInterval = sessionExpiryInterval;
	}

	public void setWillDelayInterval(int willDelayInterval) {
		this.willDelayInterval = willDelayInterval;
	}

	public void setReceiveMaximum(int receiveMaximum) {
		this.receiveMaximum = receiveMaximum;
	}

	public void setTopicAliasMaximum(int topicAliasMaximum) {
		this.topicAliasMaximum = topicAliasMaximum;
	}

	public void setRequestReplyInfo(boolean requestReplyInfo) {
		this.requestReplyInfo = requestReplyInfo;
	}

	public void setRequestProblemInfo(boolean requestProblemInfo) {
		this.requestProblemInfo = requestProblemInfo;
	}

	public void setUserDefinedPairs(Map<String, String> userDefinedPairs) {
		this.userDefinedPairs = userDefinedPairs;
	}
	
	
	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	public void setAuthData(byte[] authData) {
		this.authData = authData;
	}

	public byte getInfo() {
		return info;
	}

	public String getClientId() {
		return clientId;
	}

	public MqttMessage getWillMessage() {
		return willMessage;
	}

	public String getUserName() {
		return userName;
	}

	public byte[] getPassword() {
		return password;
	}

	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public String getWillDestination() {
		return willDestination;
	}

	public int getMqttVersion() {
		return mqttVersion;
	}

	public int getSessionExpiryInterval() {
		return sessionExpiryInterval;
	}

	public int getWillDelayInterval() {
		return willDelayInterval;
	}

	public int getReceiveMaximum() {
		return receiveMaximum;
	}

	public int getTopicAliasMaximum() {
		return topicAliasMaximum;
	}

	public Boolean getRequestReplyInfo() {
		return requestReplyInfo;
	}

	public Boolean getRequestProblemInfo() {
		return requestProblemInfo;
	}

	public Map<String, String> getUserDefinedPairs() {
		return userDefinedPairs;
	}

	public String getAuthMethod() {
		return authMethod;
	}

	public byte[] getAuthData() {
		return authData;
	}
	
	
	@Override
	public String toString() {
		return "MqttConnect [info=" + info + ", clientId=" + clientId + ", cleanSession=" + cleanSession
				+ ", willMessage=" + willMessage + ", userName=" + userName + ", password=" + Arrays.toString(password)
				+ ", keepAliveInterval=" + keepAliveInterval + ", willDestination=" + willDestination + ", mqttVersion="
				+ mqttVersion + ", sessionExpiryInterval=" + sessionExpiryInterval + ", willDelayInterval="
				+ willDelayInterval + ", receiveMaximum=" + receiveMaximum + ", topicAliasMaximum=" + topicAliasMaximum
				+ ", requestReplyInfo=" + requestReplyInfo + ", requestProblemInfo=" + requestProblemInfo
				+ ", userDefinedPairs=" + userDefinedPairs + ", authMethod=" + authMethod + ", authData="
				+ Arrays.toString(authData) + "]";
	}


}
