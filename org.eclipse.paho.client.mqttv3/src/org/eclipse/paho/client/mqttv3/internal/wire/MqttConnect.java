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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;


/**
 * An on-the-wire representation of an MQTT CONNECT message.
 */
public class MqttConnect extends MqttWireMessage {
	private String clientId;
	private boolean cleanSession;
	private MqttMessage willMessage;
	private String userName;
	private char[] password;
	private int keepAliveInterval;
	private MqttTopic willDestination;
	
	
	public MqttConnect(String clientId,
			boolean cleanSession,
			int keepAliveInterval,
			String userName,
			char[] password,
			MqttMessage willMessage,
			MqttTopic willDestination) {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.clientId = clientId;
		this.cleanSession = cleanSession;
		this.keepAliveInterval = keepAliveInterval;
		this.userName = userName;
		this.password = password;
		this.willMessage = willMessage;
		this.willDestination = willDestination;
	}
	
	protected byte getMessageInfo() {
		return (byte) 0;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}
	
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeUTF("MQIsdp");
			dos.write(3);
			byte connectFlags = 0;
			
			if (cleanSession) {
				connectFlags |= 0x02;
			}
			
			if (willMessage != null ) {
				connectFlags |= 0x04;
				connectFlags |= (willMessage.getQos()<<3);
				if (willMessage.isRetained()) {
					connectFlags |= 0x20;
				}
			}
			
			if (userName != null) {
				connectFlags |= 0x80;
				if (password != null) {
					connectFlags |= 0x40;
				}
			}
			dos.write(connectFlags);
			dos.writeShort(keepAliveInterval);
			dos.flush();
			return baos.toByteArray();
		} catch(IOException ioe) {
			throw new MqttException(ioe);
		}
	}
	
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeUTF(clientId);
			
			if (willMessage != null) {
				dos.writeUTF(willDestination.getName());
				dos.writeShort(willMessage.getPayload().length);
				dos.write(willMessage.getPayload());
			}
			
			if (userName != null) {
				dos.writeUTF(userName);
				if (password != null) {
					dos.writeUTF(new String(password));
				}
			}
			dos.flush();
			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw new MqttException(ex);
		}
	}
	
	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	public boolean isMessageIdRequired() {
		return false;
	}
}
