/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.client.mqttv3.persist;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.internal.MqttPersistentData;


/**
 * An implementation of the {@link MqttClientPersistence} interface that provides
 * javax.microedition.rms.RecordStore based persistence for when running on MIDP devices.
 *
 */
public class MqttDefaultMicroPersistence implements MqttClientPersistence {

	private RecordStore store = null;
	private String recordStoreName = null;
	
	// This provides a mapping of Persistence Key Strings to RecordStore ID Integers
	Hashtable keyMap = null;
	
	
	public void open(String clientId, String serverURI)	throws MqttPersistenceException {
		keyMap = new Hashtable();
		recordStoreName = clientId+serverURI;
		if (recordStoreName.length() > 32) {
			recordStoreName = recordStoreName.substring(0,32);
		}
		try {
			store = RecordStore.openRecordStore(recordStoreName,true);
			
			RecordEnumeration en = store.enumerateRecords(null,null,false);
			while (en.hasNextElement()) {
				int id = en.nextRecordId();
				byte[] data = store.getRecord(id);
				// data format:
				// [ 0:key length 'n' ] [ 1...n:key bytes ] [ n+1 ... end:persistable ]
				String key = new String(data,1,data[0]);
				keyMap.put(key,new Integer(id));
			}
			
		} catch (RecordStoreFullException e) {
			throw new MqttPersistenceException(e);
		} catch (RecordStoreNotFoundException e) {
			throw new MqttPersistenceException(e);
		} catch (RecordStoreException e) {
			throw new MqttPersistenceException(e);
		}
	}

	public void clear() throws MqttPersistenceException {
		try {
			keyMap.clear();
			store.closeRecordStore();
			RecordStore.deleteRecordStore(recordStoreName);
			store = RecordStore.openRecordStore(recordStoreName,true);
		} catch (RecordStoreNotFoundException e) {
			throw new MqttPersistenceException(e);
		} catch (RecordStoreException e) {
			throw new MqttPersistenceException(e);
		}
	}
	public void close() throws MqttPersistenceException {
		try {
			keyMap.clear();
			store.closeRecordStore();
		} catch (RecordStoreNotOpenException e) {
			// Ignore RecordStoreNotOpenException
		} catch (RecordStoreException e) {
			throw new MqttPersistenceException(e);
		}
	}

	public boolean containsKey(String key) throws MqttPersistenceException {
		return keyMap.containsKey(key);
	}

	public MqttPersistable get(String key) throws MqttPersistenceException {
		if (keyMap.containsKey(key)) {
			Integer i = (Integer)keyMap.get(key);
			byte data[] = null;
			try {
				data = store.getRecord(i.intValue());
			} catch (RecordStoreNotOpenException e) {
				throw new MqttPersistenceException(e);
			} catch (InvalidRecordIDException e) {
				throw new MqttPersistenceException(e);
			} catch (RecordStoreException e) {
				throw new MqttPersistenceException(e);
			}
			String checkKey = new String(data,1,data[0]);
			if (!checkKey.equals(key)) {
				throw new MqttPersistenceException(new RecordStoreException("Invalid Record:"+key));
			}
			// data format:
			// [ 0:key length 'n' ] [ 1...n:key bytes ] [ n+1 ... end:persistable ]
			int dataIndex = data[0]+1;
			return new MqttPersistentData(key,data,dataIndex,data.length-dataIndex,new byte[]{},0,0);
		}
		return null;
	}

	public Enumeration keys() throws MqttPersistenceException {
		return keyMap.keys();
	}


	public void put(String key, MqttPersistable persistable) throws MqttPersistenceException {
		
		// data format:
		// [ 0:key length 'n' ] [ 1...n:key bytes ] [ n+1 ... end:persistable ]

		byte[] keyBytes = key.getBytes();
		byte[] data = new byte[1+keyBytes.length+persistable.getHeaderLength()+persistable.getPayloadLength()];
		
		// The longest possible key is: "sc-12345" - so safe to cast to a byte value
		data[0] = (byte)keyBytes.length;
		
		System.arraycopy(keyBytes,0,data,1,keyBytes.length);
		System.arraycopy(persistable.getHeaderBytes(),0,data,1+keyBytes.length,persistable.getHeaderLength());
		System.arraycopy(persistable.getPayloadBytes(),0,data,1+keyBytes.length+persistable.getHeaderLength(),persistable.getPayloadLength());
		
		try {
			int recordId = store.addRecord(data, 0, data.length);
			keyMap.put(key,new Integer(recordId));
		} catch (RecordStoreNotOpenException e) {
			throw new MqttPersistenceException(e);
		} catch (RecordStoreFullException e) {
			throw new MqttPersistenceException(e);
		} catch (RecordStoreException e) {
			throw new MqttPersistenceException(e);
		}

	}

	public void remove(String key) throws MqttPersistenceException {
		if (keyMap.containsKey(key)) {
			Integer i = (Integer)keyMap.remove(key);
			try {
				store.deleteRecord(i.intValue());
			} catch (RecordStoreNotOpenException e) {
				throw new MqttPersistenceException(e);
			} catch (InvalidRecordIDException e) {
				throw new MqttPersistenceException(e);
			} catch (RecordStoreException e) {
				throw new MqttPersistenceException(e);
			}
		}
	}

}
