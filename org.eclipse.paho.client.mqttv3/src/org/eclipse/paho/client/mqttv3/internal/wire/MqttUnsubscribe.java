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


/**
 * An on-the-wire representation of an MQTT UNSUBSCRIBE message.
 */
public class MqttUnsubscribe extends MqttWireMessage {
	
	private String[] names;

	/**
	 * Constructs an MqttUnsubscribe
	 */
	public MqttUnsubscribe(String[] names) {
		super(MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE);
		this.names = names;
	}
	
	protected byte getMessageInfo() {
		return (byte)( 2 | (this.duplicate?8:0));
	}
	
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(msgId);
			dos.flush();
			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw new MqttException(ex);
		}
	}

	public byte[] getPayload() throws MqttException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			for (int i=0; i<names.length; i++) {
				this.encodeUTF8(dos, names[i]);
			}
			return baos.toByteArray();
	}
	public boolean isRetryable() {
		return true;
	}
}
