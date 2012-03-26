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
package org.eclipse.paho.client.mqttv3.internal;

import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;


/**
 * Default no-op version of persistence, to use if no persistence is required.
 */
public class MemoryPersistence implements MqttClientPersistence {

	private Hashtable data;
	
	public void close() throws MqttPersistenceException {
		data.clear();
	}

	public Enumeration keys() throws MqttPersistenceException {
		return data.keys();
	}

	public MqttPersistable get(String key) throws MqttPersistenceException {
		return (MqttPersistable)data.get(key);
	}

	public void open(String clientId, String serverURI) throws MqttPersistenceException {
		this.data = new Hashtable();
	}

	public void put(String key, MqttPersistable persistable) throws MqttPersistenceException {
		data.put(key, persistable);
	}

	public void remove(String key) throws MqttPersistenceException {
		data.remove(key);
	}

	public void clear() throws MqttPersistenceException {
		data.clear();
	}

	public boolean containsKey(String key) throws MqttPersistenceException {
		return data.containsKey(key);
	}
}
