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
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.util.CountingInputStream;

/**
 * An on-the-wire representation of an MQTT Publish message.
 */
public class MqttPublish extends MqttPersistableWireMessage{

	// Payload format identifiers
	public static final byte PAYLOAD_FORMAT_UNSPECIFIED = 0x00;
	public static final byte PAYLOAD_FORMAT_UTF8 = 0x01;

	// Fields
	private MqttMessage message;
	private String topicName;
	private Integer payloadFormat;
	private Integer publicationExpiryInterval;
	private Integer topicAlias;
	private byte[] correlationData;
	private ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
	private Integer subscriptionIdentifier;
	private String contentType;
	private String responseTopic;

	/**
	 * Constructs a new MqttPublish message
	 * 
	 * @param topic
	 *            - The Destination Topic.
	 * @param message
	 *            - The Message being sent.
	 */
	public MqttPublish(String topic, MqttMessage message) {
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		this.topicName = topic;
		this.message = message;
	}

	/**
	 * Constructs a new MqttPublish message from a byte array
	 * 
	 * @param info
	 *            - Info Byte
	 * @param data
	 *            - The variable header and payload bytes.
	 * @throws IOException
	 *             - if an exception occurs when decoding an input stream
	 * @throws MqttException
	 *             - If an exception occurs decoding this packet
	 */
	public MqttPublish(byte info, byte[] data) throws MqttException, IOException {
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		message = new MqttReceivedMessage();
		message.setQos((info >> 1) & 0x03);
		if ((info & 0x01) == 0x01) {
			message.setRetained(true);
		}

		if ((info & 0x08) == 0x08) {
			message.setDuplicate(true);
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream dis = new DataInputStream(counter);

		topicName = decodeUTF8(dis);
		if (message.getQos() > 0) {
			msgId = dis.readUnsignedShort();
		}
		parseIdentifierValueFields(dis);
		byte[] payload = new byte[data.length - counter.getCounter()];
		dis.readFully(payload);
		dis.close();
		message.setPayload(payload);
	}

	/**
	 * Parses the Variable Header for Identifier Value fields and populates the
	 * relevant fields in this MqttPublish message.
	 * 
	 * @param dis
	 * @throws IOException
	 * @throws MqttException
	 */
	private void parseIdentifierValueFields(DataInputStream dis) throws IOException, MqttException {
		int length = readVariableByteInteger(dis).getValue();
		if (length > 0) {
			byte[] identifierValueByteArray = new byte[length];
			dis.read(identifierValueByteArray, 0, length);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
			while (inputStream.available() > 0) {
				// Get the first byte (identifier)
				byte identifier = inputStream.readByte();
				if (identifier == MqttPropertyIdentifiers.PAYLOAD_FORMAT_INDICATOR_IDENTIFIER) {
					payloadFormat = (int) inputStream.readByte();
				} else if (identifier == MqttPropertyIdentifiers.PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER) {
					publicationExpiryInterval = inputStream.readInt();
				} else if (identifier == MqttPropertyIdentifiers.TOPIC_ALIAS_IDENTIFIER) {
					topicAlias = (int) inputStream.readShort();
				} else if (identifier == MqttPropertyIdentifiers.RESPONSE_TOPIC_IDENTIFIER) {
					responseTopic = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.CORRELATION_DATA_IDENTIFIER) {
					int correlationDataLength = (int) inputStream.readShort();
					correlationData = new byte[correlationDataLength];
					inputStream.read(correlationData, 0, correlationDataLength);
				} else if (identifier == MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER) {
					String key = decodeUTF8(inputStream);
					String value = decodeUTF8(inputStream);
					userDefinedProperties.add(new UserProperty(key, value));
				} else if (identifier == MqttPropertyIdentifiers.CONTENT_TYPE_IDENTIFIER) {
					contentType = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.SUBSCRIPTION_IDENTIFIER) {
					subscriptionIdentifier = readVariableByteInteger(inputStream).getValue();
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
			if (payloadFormat != null) {
				outputStream.write(MqttPropertyIdentifiers.PAYLOAD_FORMAT_INDICATOR_IDENTIFIER);
				outputStream.writeByte(payloadFormat);
			}

			// If Present, encode the Publication Expiry Interval (3.3.2.5)
			if (publicationExpiryInterval != null) {
				outputStream.write(MqttPropertyIdentifiers.PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(publicationExpiryInterval);
			}

			// If Present, encode the Topic Alias (3.3.2.6)
			if (topicAlias != null) {
				outputStream.write(MqttPropertyIdentifiers.TOPIC_ALIAS_IDENTIFIER);
				outputStream.writeShort(topicAlias);
			}

			// If Present, encode the Reply Topic (3.3.2.7)
			if (responseTopic != null) {
				outputStream.write(MqttPropertyIdentifiers.RESPONSE_TOPIC_IDENTIFIER);
				encodeUTF8(outputStream, responseTopic);
			}

			// If Present, encode the Correlation Data (3.3.2.8)
			if (correlationData != null) {
				outputStream.write(MqttPropertyIdentifiers.CORRELATION_DATA_IDENTIFIER);
				outputStream.writeShort(correlationData.length);
				outputStream.write(correlationData);
			}

			// If Present, encode the User Defined Name-Value Pairs (3.3.2.9)
			if (userDefinedProperties.size() != 0) {
				for (UserProperty property : userDefinedProperties) {
					outputStream.write(MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(outputStream, property.getKey());
					encodeUTF8(outputStream, property.getValue());
				}
			}
			
			// If Present, encode the Subscription Identifier (3.3.2.3.8)
			if (subscriptionIdentifier != null){
				outputStream.write(MqttPropertyIdentifiers.SUBSCRIPTION_IDENTIFIER);
				outputStream.write(encodeVariableByteInteger(subscriptionIdentifier));
			}
			
		
			// If Present, encode the Content Type (3.3.2.3.9)
			if (contentType != null) {
				outputStream.write(MqttPropertyIdentifiers.CONTENT_TYPE_IDENTIFIER);
				encodeUTF8(outputStream, contentType);
			}

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			
			// If we are using a Topic Alias, then the topic should be empty
			if(topicName != null) {
				encodeUTF8(dos, topicName);
			} else {
				encodeUTF8(dos, "");
			}

			
			if (message.getQos() > 0) {
				dos.writeShort(msgId);
			}
			// Write Identifier / Value Fields
			byte[] identifierValueFieldsArray = getIdentifierValueFields();
			dos.write(encodeVariableByteInteger(identifierValueFieldsArray.length));
			dos.write(identifierValueFieldsArray);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	@Override
	protected byte getMessageInfo() {
		byte info = (byte) (message.getQos() << 1);
		if (message.isRetained()) {
			info |= 0x01;
		}
		if (message.isDuplicate() || duplicate) {
			info |= 0x08;
		}
		return info;
	}

	@Override
	public byte[] getPayload() {
		return message.getPayload();
	}

	public int getPayloadLength() {
		if (message.getPayload() != null) {
			return message.getPayload().length;
		} else {
			return 0;
		}
	}

	@Override
	public void setMessageId(int msgId) {
		super.setMessageId(msgId);
		if (message instanceof MqttReceivedMessage) {
			((MqttReceivedMessage) message).setMessageId(msgId);
		}
	}

	@Override
	public boolean isMessageIdRequired() {
		// all publishes require a message ID as it's used as the key to the
		// token store
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
		if(topicAlias == null) {
			return 0;
		}
		return topicAlias;
	}

	public void setTopicAlias(Integer topicAlias) {
		this.topicAlias = topicAlias;
	}

	public byte[] getCorrelationData() {
		return correlationData;
	}

	public void setCorrelationData(byte[] correlationData) {
		this.correlationData = correlationData;
	}

	public ArrayList<UserProperty> getUserDefinedProperties() {
		return userDefinedProperties;
	}

	public void setUserDefinedProperties(ArrayList<UserProperty> userDefinedProperties) {
		this.userDefinedProperties = userDefinedProperties;
	}

	public MqttMessage getMessage() {
		return message;
	}
	
	public void setMessage(MqttMessage message) {
		this.message = message;
	}

	public String getTopicName() {
		return topicName;
	}
	
	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public int getSubscriptionIdentifier() {
		return subscriptionIdentifier;
	}

	public void setSubscriptionIdentifier(int subscriptionIdentifier) {
		this.subscriptionIdentifier = subscriptionIdentifier;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getResponseTopic() {
		return responseTopic;
	}

	public void setResponseTopic(String responseTopic) {
		this.responseTopic = responseTopic;
	}

	@Override
	public String toString() {
		// Convert the first few bytes of the payload into a hex string
		StringBuilder hex = new StringBuilder();
		byte[] payload = message.getPayload();
		int limit = Math.min(payload.length, 20);
		for (int i = 0; i < limit; i++) {
			byte b = payload[i];
			String ch = Integer.toHexString(b);
			if (ch.length() == 1) {
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
		if (message.getQos() > 0) {
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
		sb.append(", correlationData=").append(Arrays.toString(correlationData));
		sb.append(", userDefinedProperties=").append(userDefinedProperties);

		return sb.toString();
	}

}