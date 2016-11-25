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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * An on-the-wire representation of an MQTT Publish message.
 */
public class MqttPublish extends MqttWireMessage {
	
	// Payload format identifiers
	public static final byte PAYLOAD_FORMAT_UNSPECIFIED = 0x00;
	public static final byte PAYLOAD_FORMAT_UTF8 = 0x01;
	
	
	// Identifier / Value Identifiers
	private static final byte PAYLOAD_FORMAT_INDICATOR_IDENTIFIER 		= 0x01;
	private static final byte PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER 	= 0x02;
	private static final byte TOPIC_ALIAS_IDENTIFIER					= 0x23;
	private static final byte REPLY_TOPIC_IDENTIFIER					= 0x08;
	private static final byte CORRELATION_DATA_IDENTIFIER				= 0x09;
	private static final byte USER_DEFINED_PAIR_IDENTIFIER				= 0x26;
	
	// Fields
	private MqttMessage message;
	private String topicName;
	private Integer payloadFormat;
	private Integer publicationExpiryInterval;
	private Integer topicAlias;
	private String replyTopic;
	private byte[] correlationData;
	private Map<String, String> userDefinedPairs = new HashMap<>();
	
	
	/**
	 * Constructs a new MqttPublish message
	 * @param topic - The Destination Topic.
	 * @param message - The Message being sent.
	 */
	public MqttPublish(String topic, MqttMessage message){
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		this.topicName = topic;
		this.message = message;
	}

	/**
	 * Constructs a new MqttPublish message from a byte array
	 * @param info - The message info byte.
	 * @param data - The variable header and payload bytes.
	 * @throws MqttException 
	 * @throws IOException 
	 */
	public MqttPublish(byte info, byte[] data) throws MqttException, IOException {
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		message = new MqttReceivedMessage();
		message.setQos((info >> 1) & 0x03);
		if((info & 0x01) == 0x01){
			message.setRetained(true);
		}
		
		if((info & 0x08) == 0x08){
			message.setDuplicate(true);
		}
		
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream dis = new DataInputStream(counter);
		
		topicName = decodeUTF8(dis);
		if(message.getQos() > 0){
			msgId = dis.readUnsignedShort();
		}
		parseIdentifierValueFields(dis);
		byte[] payload = new byte[data.length - counter.getCounter()];
		dis.readFully(payload);
		dis.close();
		message.setPayload(payload);
	}
	

	
	/**
	 * Parses the Variable Header for Identifier Value fields and populates the relevant fields in
	 * this MqttPublish message.
	 * @param dis
	 * @throws IOException
	 * @throws MqttException
	 */
	private void parseIdentifierValueFields(DataInputStream dis) throws IOException, MqttException {
		int length = readVariableByteInteger(dis).getValue();
		if(length > 0){
			byte[] identifierValueByteArray = new byte[length];
			dis.read(identifierValueByteArray, 0, length);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
			while (inputStream.available() > 0){
				// Get the first byte (identifier)
				byte identifier = inputStream.readByte();
				if(identifier == PAYLOAD_FORMAT_INDICATOR_IDENTIFIER){
					payloadFormat = (int) inputStream.readByte();
				} else if (identifier == PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER){
					publicationExpiryInterval = inputStream.readInt();
				} else if (identifier == TOPIC_ALIAS_IDENTIFIER) {
					topicAlias = (int) inputStream.readShort();
				} else if (identifier == REPLY_TOPIC_IDENTIFIER) {
					replyTopic = decodeUTF8(inputStream);
				} else if (identifier == CORRELATION_DATA_IDENTIFIER){
					int correlationDataLength = (int) inputStream.readShort();
					correlationData = new byte[correlationDataLength];
					inputStream.read(correlationData, 0, correlationDataLength);
				} else if (identifier == USER_DEFINED_PAIR_IDENTIFIER){
					String key = decodeUTF8(inputStream);
					String value = decodeUTF8(inputStream);
					userDefinedPairs.put(key, value);
				} else {
					// Unidentified Identifier
					throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
				}
			}
			
		}
		
	}
	
	private byte[] getIdentifierValueFields() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);
			
			// If Present, encode the Payload Format Indicator (3.3.2.4)
			if(payloadFormat != null){
				outputStream.write(PAYLOAD_FORMAT_INDICATOR_IDENTIFIER);
				outputStream.writeByte(payloadFormat);
			}
			
			// If Present, encode the Publication Expiry Interval (3.3.2.5)
			if(publicationExpiryInterval != null){
				outputStream.write(PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(publicationExpiryInterval);
			}
			
			// If Present, encode the Topic Alias (3.3.2.6)
			if(topicAlias != null){
				outputStream.write(TOPIC_ALIAS_IDENTIFIER);
				outputStream.writeShort(topicAlias);
			}
			
			// If Present, encode the Reply Topic (3.3.2.7)
			if(replyTopic != null){
				outputStream.write(REPLY_TOPIC_IDENTIFIER);
				encodeUTF8(outputStream, replyTopic);
			}
			
			// If Present, encode the Correlation Data (3.3.2.8)
			if(correlationData != null){
				outputStream.write(CORRELATION_DATA_IDENTIFIER);
				outputStream.writeShort(correlationData.length);
				outputStream.write(correlationData);
			}
			
			// If Present, encode the User Defined Name-Value Pairs (3.3.2.9)
			if(userDefinedPairs.size() != 0){
				for(Map.Entry<String, String> entry : userDefinedPairs.entrySet()){
					outputStream.write(USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(outputStream, entry.getKey());
					encodeUTF8(outputStream, entry.getValue());
				}
			}
			
			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe){
			throw new MqttException(ioe);
		}
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			
			encodeUTF8(dos, topicName);
			if(message.getQos() > 0){
				dos.writeShort(msgId);
			}
			// Write Identifier / Value Fields
			byte[] identifierValueFieldsArray = getIdentifierValueFields();
			dos.write(encodeVariableByteInteger(identifierValueFieldsArray.length));
			dos.write(identifierValueFieldsArray);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe){
			throw new MqttException(ioe);
		}
	}
	
	
	@Override
	protected byte getMessageInfo() {
		byte info = (byte) (message.getQos() << 1);
		if (message.isRetained()) {
			info |= 0x01;
		}
		if (message.isDuplicate() || duplicate ) {
			info |= 0x08;
		}
		return info;
	}
	
	
	@Override
	public byte[] getPayload(){
		return message.getPayload();
	}

	public int getPayloadLength(){
		if(message.getPayload() != null){
			return message.getPayload().length;
		} else {
			return 0;
		}
	}
	
	@Override
	public void setMessageId(int msgId){
		super.setMessageId(msgId);
		if(message instanceof MqttReceivedMessage){
			((MqttReceivedMessage) message).setMessageId(msgId);
		}
	}
	
	@Override
	public boolean isMessageIdRequired() {
		// all publishes require a message ID as it's used as the key to the token store
		return true;
	}
	
	public int getPayloadFormat() {
		return payloadFormat;
	}

	public void setPayloadFormat(Integer payloadFormat) {
		this.payloadFormat = payloadFormat;
	}

	public int getPublicationExpiryInterval() {
		return publicationExpiryInterval;
	}

	public void setPublicationExpiryInterval(Integer publicationExpiryInterval) {
		this.publicationExpiryInterval = publicationExpiryInterval;
	}

	public int getTopicAlias() {
		return topicAlias;
	}

	public void setTopicAlias(Integer topicAlias) {
		this.topicAlias = topicAlias;
	}

	public String getReplyTopic() {
		return replyTopic;
	}

	public void setReplyTopic(String replyTopic) {
		this.replyTopic = replyTopic;
	}

	public byte[] getCorrelationData() {
		return correlationData;
	}

	public void setCorrelationData(byte[] correlationData) {
		this.correlationData = correlationData;
	}

	public Map<String, String> getUserDefinedPairs() {
		return userDefinedPairs;
	}

	public void setUserDefinedPairs(Map<String, String> userDefinedPairs) {
		this.userDefinedPairs = userDefinedPairs;
	}


	public MqttMessage getMessage() {
		return message;
	}

	public String getTopicName() {
		return topicName;
	}

	@Override
	public String toString() {
		// Convert the first few bytes of the payload into a hex string
		StringBuilder hex = new StringBuilder();
		byte[] payload = message.getPayload();
		int limit = Math.min(payload.length, 20);
		for(int i = 0; i < limit; i++){
			byte b = payload[i];
			String ch = Integer.toHexString(b);
			if(ch.length() == 1){
				ch = "0" + ch;
			}
			hex.append(ch);
		}
		
		// It will not always be possible to convert the binary payload into
		// characters, but never-the-less we attempt to do this as it is often
		// useful.
		String string = null;
		try {
			string = new String(payload, 0, limit, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			string = "?";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("MqttPublish [");
		sb.append(", qos=").append(message.getQos());
		if(message.getQos() > 0){
			sb.append(", messageId=").append(msgId);
		}
		sb.append(", retained=").append(message.isRetained());
		sb.append(", duplicate=").append(duplicate);
		sb.append(", topic=").append(topicName);
		sb.append(", payload=[hex=").append(hex);
		sb.append(", utf8=").append(string);
		sb.append(", length=").append(payload.length).append("]");
		sb.append(", payloadFormat=").append(payloadFormat);
		sb.append(", publicationExpiryInterval=").append(publicationExpiryInterval);
		sb.append(", topicAlias=").append(topicAlias);
		sb.append(", replyTopic=").append(replyTopic);
		sb.append(", correlationData=").append(Arrays.toString(correlationData));
		sb.append(", userDefinedPairs=").append(userDefinedPairs);
		
		return sb.toString();
	}

	

}