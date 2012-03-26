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
	public static final byte DESTINATION_TYPE_TOPIC = 0;
	
	private MqttMessage message;
	private String topicName;
	
	private byte[] encodedPayload = null;
	
	public MqttPublish(String name, MqttMessage message) {
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		this.topicName = name;
		this.message = message;
	}
	
	/**
	 * Constructs a new MqttPublish object.
	 * @param info the message info byte
	 * @param data the variable header and payload bytes
	 */
	public MqttPublish(byte info, byte[] data) throws MqttException, IOException {
		super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
		this.message = new MqttReceivedMessage();
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
		topicName = dis.readUTF();
		if (message.getQos() > 0) {
			msgId = dis.readUnsignedShort();
		}
		dis.close();
		byte[] payload = new byte[data.length-counter.getCounter()];
		dis.readFully(payload);
		message.setPayload(payload);
	}
	
	protected byte getMessageInfo() {
		byte info = (byte) (message.getQos() << 1);
		if (message.isRetained()) {
			info |= 0x01;
		}
		if (message.isDuplicate()) {
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
	
	protected static byte[] encodePayload(MqttMessage message) throws MqttException {
//		byte payloadType = message.getPayloadType();
//		if (payloadType == MqttMessage.PAYLOAD_EMPTY) {
//			return new byte[0];
//		}
//		else if (payloadType == MqttMessage.PAYLOAD_BYTES) {
		return message.getPayload();
//		}
//		else if (payloadType == MqttMessage.PAYLOAD_TEXT) {
//			try {
//				return message.getStringPayload().getBytes(STRING_ENCODING);
//			} catch(UnsupportedEncodingException uee) {
//				throw new MqttException(uee);
//			}
//		}
//		else if (payloadType == MqttMessage.PAYLOAD_MAP) {
//			try {
//				return encodeUserProperties(message.getMapPayload());
//			}
//			catch (IOException ex) {
//				throw new MqttException(ex);
//			}
//		}
//		else {
//			// TODO: This is actually an error.  Throw an exception?
//			return new byte[0];
//		}
	}

	public byte[] getPayload() throws MqttException {
		if (encodedPayload == null) {
			// TODO: inefficient, as this puts two copies in memory
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
			dos.writeUTF(topicName);
			if (message.getQos() > 0) {
				dos.writeShort(msgId);
			}
			dos.flush();
			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw new MqttException(ex);
		}
	}
	
	public boolean isMessageIdRequired() {
		// all publishes require a message ID as it's used as the key to the token store
		return true;
	}
}