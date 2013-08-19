/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.wire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * An on-the-wire representation of an MQTT SEND message.
 */
public class MqttPublish extends MqttPersistableWireMessage {
	
	private MqttMessage message;
	private String topicName;
	
	private byte[] encodedPayload = null;
	
	public MqttPublish(String name, MqttMessage message) {
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		topicName = name;
		this.message = message;
	}
	
	/**
	 * Constructs a new MqttPublish object.
	 * @param info the message info byte
	 * @param data the variable header and payload bytes
	 */
	public MqttPublish(byte info, byte[] data) throws MqttException, IOException {
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		message = new MqttReceivedMessage();
		message.setQos((info >> 1) & 0x03);
		if ((info & 0x01) == 0x01) {
			message.setRetained(true);
		}
		if ((info & 0x08) == 0x08) {
			((MqttReceivedMessage) message).setDuplicate(true);
		}
		
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream dis = new DataInputStream(counter);
		topicName = decodeUTF8(dis);
		if (message.getQos() > 0) {
			msgId = dis.readUnsignedShort();
		}
		byte[] payload = new byte[data.length-counter.getCounter()];
		dis.readFully(payload);
		dis.close();
		message.setPayload(payload);
	}

	public String toString() {

		// Convert the first few bytes of the payload into a hex string
		StringBuffer hex = new StringBuffer();
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
		// useful
		String string = null;
		try {
			string = new String(payload, 0, limit, "UTF-8");
		} catch (Exception e) {
			string = "?";
		}

		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append(" qos:" + message.getQos());
		if (message.getQos() > 0) {
			sb.append(" msgId:" + msgId);
		}
		sb.append(" retained:" + message.isRetained());
		sb.append(" dup:" + duplicate);
		sb.append(" topic:\"" + topicName + "\"");
		sb.append(" payload:[hex:" + hex);
		sb.append(" utf8:\"" + string + "\"");
		sb.append(" length:" + payload.length + "]");

		return sb.toString();
	}
	
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
	
	public String getTopicName() {
		return topicName;
	}
	
	public MqttMessage getMessage() {
		return message;
	}
	
	protected static byte[] encodePayload(MqttMessage message) {
		return message.getPayload();
	}

	public byte[] getPayload() throws MqttException {
		if (encodedPayload == null) {
			encodedPayload = encodePayload(message);
		}
		return encodedPayload;
	}

	public int getPayloadLength() {
		int length = 0;
		try {
			length = getPayload().length;
		} catch(MqttException me) {
		}
		return length;
	}
	
	public void setMessageId(int msgId) {
		super.setMessageId(msgId);
		if (message instanceof MqttReceivedMessage) {
			((MqttReceivedMessage)message).setMessageId(msgId);
		}
	}
	
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			encodeUTF8(dos, topicName);
			if (message.getQos() > 0) {
				dos.writeShort(msgId);
			}
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ex) {
			throw new MqttException(ex);
		}
	}
	
	public boolean isMessageIdRequired() {
		// all publishes require a message ID as it's used as the key to the token store
		return true;
	}
}