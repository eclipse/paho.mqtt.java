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
package org.eclipse.paho.client.mqttv3;

import java.util.Enumeration;

/**
 * Represents a persistent data store, used to store outbound and inbound messages while they
 * are in flight, enabling delivery to the QoS specified. You can specify an implementation
 * of this interface using {@link MqttClient#MqttClient(String, String, MqttClientPersistence)},
 * which the {@link MqttClient} will use to persist QoS 1 and 2 messages.
 * <p>
 * If the methods defined throw the MqttPersistenceException then the state of the data persisted
 * should remain as prior to the method being called. For example, if {@link #put(String, MqttPersistable)}
 * throws an exception at any point then the data will be assumed to not be in the persistent store.
 * Similarly if {@link #remove(String)} throws an exception then the data will be
 * assumed to still be held in the persistent store.</p>
 * <p>
 * It is up to the persistence interface to log any exceptions or error information 
 * which may be required when diagnosing a persistence failure.</p>
 */
public interface MqttClientPersistence {
	/**
	 * Initialise the persistent store.
	 * If a persistent store exists for this client ID then open it, otherwise 
	 * create a new one. If the persistent store is already open then just return.
	 * An application may use the same client ID to connect to many different 
	 * servers, so the client ID in conjunction with the
	 * connection will uniquely identify the persistence store required.
	 * 
	 * @param clientId The client for which the persistent store should be opened.
	 * @param serverURI The connection string as specified when the MQTT client instance was created.
	 * @throws MqttPersistenceException if there was a problem opening the persistent store.
	 */
	public void open(String clientId, String serverURI) throws MqttPersistenceException;

	/**
	 * Close the persistent store that was previously opened.
	 * This will be called when a client application disconnects from the broker.
	 * @throws MqttPersistenceException if an error occurs closing the persistence store.
	 */	
	public void close() throws MqttPersistenceException;

	/**
	 * Puts the specified data into the persistent store.
	 * @param key the key for the data, which will be used later to retrieve it.
	 * @param persistable the data to persist
	 * @throws MqttPersistenceException if there was a problem putting the data
	 * into the persistent store.
	 */
	public void put(String key, MqttPersistable persistable) throws MqttPersistenceException;
	
	/**
	 * Gets the specified data out of the persistent store.
	 * @param key the key for the data, which was used when originally saving it.
	 * @return the un-persisted data
	 * @throws MqttPersistenceException if there was a problem getting the data
	 * from the persistent store.
	 */
	public MqttPersistable get(String key) throws MqttPersistenceException;
	
	/**
	 * Remove the data for the specified key.
	 * @param key The key for the data to remove
	 * @throws MqttPersistenceException if there was a problem removing the data.
	 */
	public void remove(String key) throws MqttPersistenceException;

	/**
	 * Returns an Enumeration over the keys in this persistent data store.
	 * @return an enumeration of {@link String} objects.
	 * @throws MqttPersistenceException if there was a problem getting they keys
	 */
	public Enumeration keys() throws MqttPersistenceException;
	
	/**
	 * Clears persistence, so that it no longer contains any persisted data.
	 * @throws MqttPersistenceException if there was a problem clearing all data from the persistence store
	 */
	public void clear() throws MqttPersistenceException;
	
	/**
	 * Returns whether or not data is persisted using the specified key.
	 * @param key the key for data, which was used when originally saving it.
	 * @return True if the persistence store contains the key
	 * @throws MqttPersistenceException if there was a problem checking whether they key existed.
	 */
	public boolean containsKey(String key) throws MqttPersistenceException;
}
