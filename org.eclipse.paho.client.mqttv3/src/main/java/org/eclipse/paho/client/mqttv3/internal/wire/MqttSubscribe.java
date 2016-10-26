/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 * An on-the-wire representation of an MQTT SUBSCRIBE message.
 */
public class MqttSubscribe extends MqttWireMessage {
	private String[] names;
	private int[] qos;
	private int count;

	/**
	 * Constructor for an on the wire MQTT subscribe message
	 * 
	 * @param info the info byte
	 * @param data the data byte array
	 * @throws IOException if an exception occurs whilst reading the input stream
	 */
	public MqttSubscribe(byte info, byte[] data) throws IOException {
		super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
		msgId = dis.readUnsignedShort();

		count = 0;
		names = new String[10];
		qos = new int[10];
		boolean end = false;
		while (!end) {
			try {
				names[count] = decodeUTF8(dis);
				qos[count++] = dis.readByte();
			} catch (Exception e) {
				end = true;
			}
		}
		dis.close();
	}

	/**
	 * Constructor for an on the wire MQTT subscribe message
	 * @param names - one or more topics to subscribe to 
	 * @param qos - the max QoS that each each topic will be subscribed at 
	 */
	public MqttSubscribe(String[] names, int[] qos) {
		super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
		this.names = names;
		this.qos = qos;
		
		if (names.length != qos.length) {
		throw new IllegalArgumentException();
		}
		this.count = names.length;
		
		for (int i=0;i<qos.length;i++) {
			MqttMessage.validateQos(qos[i]);
		}
	}

	/**
	 * @return string representation of this subscribe packet
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append(" names:[");
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append("\"").append(names[i]).append("\"");
		}
		sb.append("] qos:[");
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(qos[i]);
		}
		sb.append("]");

		return sb.toString();
	}
	
	protected byte getMessageInfo() {
		return (byte) (2 | (duplicate ? 8 : 0));
	}
	
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(msgId);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ex) {
			throw new MqttException(ex);
		}
	}
	
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			for (int i=0; i<names.length; i++) {
				encodeUTF8(dos,names[i]);
				dos.writeByte(qos[i]);
			}
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ex) {
			throw new MqttException(ex);
		}
	}
	
	public boolean isRetryable() {
		return true;
	}
}
