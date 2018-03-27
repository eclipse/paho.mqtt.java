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

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.util.CountingInputStream;

/**
 * An on-the-wire representation of an MQTT Publish message.
 */
public class MqttPublish extends MqttPersistableWireMessage {

	private static final Byte[] validProperties = { MqttProperties.PAYLOAD_FORMAT_INDICATOR_IDENTIFIER,
			MqttProperties.MESSAGE_EXPIRY_INTERVAL_IDENTIFIER, MqttProperties.TOPIC_ALIAS_IDENTIFIER,
			MqttProperties.RESPONSE_TOPIC_IDENTIFIER, MqttProperties.CORRELATION_DATA_IDENTIFIER,
			MqttProperties.USER_DEFINED_PAIR_IDENTIFIER, MqttProperties.CONTENT_TYPE_IDENTIFIER,
			MqttProperties.SUBSCRIPTION_IDENTIFIER_MULTI, MqttProperties.SUBSCRIPTION_IDENTIFIER };

	private MqttProperties properties;

	// Fields
	private byte[] payload;
	private int qos = 1;
	private boolean retained = false;
	private boolean dup = false;
	private String topicName;

	/**
	 * Constructs a new MqttPublish message
	 *
	 * @param topic
	 *            - The Destination Topic.
	 * @param message
	 *            - The Message being sent.
	 * @param properties
	 *            - The {@link MqttProperties} for the packet.
	 */
	public MqttPublish(String topic, MqttMessage message, MqttProperties properties) {
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		this.topicName = topic;
		this.payload = message.getPayload();
		this.qos = message.getQos();
		this.dup = message.isDuplicate();
		this.retained = message.isRetained();
		if (properties != null) {
			this.properties = properties;
		} else {
			this.properties = new MqttProperties();
		}
		this.properties.setValidProperties(validProperties);
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
		this.properties = new MqttProperties(validProperties);
		this.qos = (info >> 1) & 0x03;
		if ((info & 0x01) == 0x01) {
			this.retained = true;
		}

		if ((info & 0x08) == 0x08) {
			this.dup = true;
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream dis = new DataInputStream(counter);

		topicName = MqttDataTypes.decodeUTF8(dis);
		if (this.qos > 0) {
			msgId = dis.readUnsignedShort();
		}
		this.properties.decodeProperties(dis);
		this.payload = new byte[data.length - counter.getCounter()];
		dis.readFully(this.payload);
		dis.close();
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			// If we are using a Topic Alias, then the topic should be empty
			if (topicName != null) {
				MqttDataTypes.encodeUTF8(dos, topicName);
			} else {
				MqttDataTypes.encodeUTF8(dos, "");
			}

			if (this.qos > 0) {
				dos.writeShort(msgId);
			}
			// Write Identifier / Value Fields
			byte[] identifierValueFieldsArray = this.properties.encodeProperties();
			dos.write(identifierValueFieldsArray);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	@Override
	protected byte getMessageInfo() {
		byte info = (byte) (this.qos << 1);
		if (this.retained) {
			info |= 0x01;
		}
		if (this.dup || duplicate) {
			info |= 0x08;
		}
		return info;
	}

	@Override
	public byte[] getPayload() {
		return this.payload;
	}

	@Override
	public int getPayloadLength() {
		if (this.payload != null) {
			return this.payload.length;
		} else {
			return 0;
		}
	}

	@Override
	public boolean isMessageIdRequired() {
		// all publishes require a message ID as it's used as the key to the
		// token store
		return true;
	}

	public MqttMessage getMessage() {
		MqttMessage message = new MqttMessage(payload, qos, retained, properties);
		return message;
	}

	public void setMessage(MqttMessage message) {
		this.payload = message.getPayload();
		this.qos = message.getQos();
		this.dup = message.isDuplicate();
		this.retained = message.isRetained();
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	@Override
	public MqttProperties getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		// Convert the first few bytes of the payload into a hex string
		StringBuilder hex = new StringBuilder();
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
		sb.append(", qos=").append(this.qos);
		if (this.qos > 0) {
			sb.append(", messageId=").append(msgId);
		}
		sb.append(", retained=").append(this.retained);
		sb.append(", duplicate=").append(duplicate);
		sb.append(", topic=").append(topicName);
		sb.append(", payload=[hex=").append(hex);
		sb.append(", utf8=").append(string);
		sb.append(", length=").append(payload.length).append("]");
		sb.append(", properties=").append(this.properties.toString());

		return sb.toString();
	}

}
