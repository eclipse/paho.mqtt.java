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

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public abstract class MqttPersistableWireMessage extends MqttWireMessage
		implements MqttPersistable {
	
	public MqttPersistableWireMessage(byte type) {
		super(type);
	}
	
	public byte[] getHeaderBytes() throws MqttPersistenceException {
		try {
			return getHeader();
		}
		catch (MqttException ex) {
			throw new MqttPersistenceException(ex.getCause());
		}
	}
	
	public int getHeaderLength() throws MqttPersistenceException {
		return getHeaderBytes().length;
	}

	public int getHeaderOffset() throws MqttPersistenceException{
		return 0;
	}

//	public String getKey() throws MqttPersistenceException {
//		return new Integer(getMessageId()).toString();
//	}

	public byte[] getPayloadBytes() throws MqttPersistenceException {
		try {
			return getPayload();
		}
		catch (MqttException ex) {
			throw new MqttPersistenceException(ex.getCause());
		}
	}
	
	public int getPayloadLength() throws MqttPersistenceException {
		return 0;
	}

	public int getPayloadOffset() throws MqttPersistenceException {
		return 0;
	}

}
