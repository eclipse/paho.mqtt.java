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
package org.eclipse.paho.mqttv5.common.packet;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttPersistable;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;

public abstract class MqttPersistableWireMessage extends MqttWireMessage
		implements MqttPersistable {
	
	public MqttPersistableWireMessage(byte type) {
		super(type);
	}
	
	public byte[] getHeaderBytes() throws MqttPersistenceException {
		byte[] headerBytes = null;
		try {
			if(this.getClass() == MqttPublish.class && this.getProperties().getTopicAlias() != null) {
				// Remove the Topic Alias temporarily.
				MqttProperties props = this.getProperties();
				Integer topicAlias = props.getTopicAlias();
				props.setTopicAlias(null);
				headerBytes = getHeader();
				// Re Set Topic Alias
				props.setTopicAlias(topicAlias);
				this.properties = props;
			} else {
				headerBytes = getHeader();
			}
			return headerBytes;
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
