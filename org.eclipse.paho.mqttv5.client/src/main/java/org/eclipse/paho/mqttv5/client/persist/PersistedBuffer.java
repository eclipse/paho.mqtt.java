/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp and others.
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
 *    Ian Craggs - fix duplicate message id (Bug 466853)
 *    Ian Craggs - ack control (bug 472172)
 *    James Sutton - Ping Callback (bug 473928)
 *    Ian Craggs - fix for NPE bug 470718
 *    James Sutton - Automatic Reconnect & Offline Buffering
 *    Jens Reimann - Fix issue #370
 *    James Sutton - Mqttv5 - Outgoing Topic Aliases
 */
package org.eclipse.paho.mqttv5.client.persist;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.packet.MqttPersistableWireMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

public class PersistedBuffer {
	private static final String CLASS_NAME = PersistedBuffer.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private MqttClientPersistence persistence;
	private ConcurrentHashMap<Integer, MqttWireMessage> buffer = null;

	public PersistedBuffer(MqttClientPersistence persistence) {
		//log.setResourceName(clientComms.getClient().getClientId());
		log.finer(CLASS_NAME, "<Init>", "");
		this.persistence = persistence;
		buffer = new ConcurrentHashMap<Integer, MqttWireMessage>();
	}
	
	public void clear() {
		buffer.clear();
	}
	
	/**
	 * Restores an entry that was obtained from persistence.  As it is already persisted,
	 * we don't need to persist it.
	 */
	public void restore(Integer key, MqttWireMessage value) {
		buffer.put(key, value);
	}
	
	/**
	 * Creates a new entry, both in the buffer, and in persistence.
	 */
	public void add(Integer key, MqttPersistableWireMessage message, String persistence_key_prefix) 
			throws MqttPersistenceException {
		if (persistence != null) {
			persistence.put(persistence_key_prefix + message.getMessageId(), message);
		}
		buffer.put(key, message);
	}
	
	public void remove(Integer key) {
		buffer.remove(key);
	}
	
	public Enumeration<Integer> getKeys() {
		return buffer.keys();
	}

}
