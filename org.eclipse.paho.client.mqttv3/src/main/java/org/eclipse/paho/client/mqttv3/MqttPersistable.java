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

/**
 * Represents an object used to pass data to be persisted across the
 * {@link org.eclipse.paho.client.mqttv3.MqttClientPersistence MqttClientPersistence}
 * interface.
 * <p>
 * When data is passed across the interface the header and payload are 
 * separated, so that unnecessary message copies may be avoided.
 * For example, if a 10 MB payload was published it would be inefficient
 * to create a byte array a few bytes larger than 10 MB and copy the
 * MQTT message header and payload into a contiguous byte array.</p>
 * <p>
 * When the request to persist data is made a separate byte array and offset
 * is passed for the header and payload. Only the data between
 * offset and length need be persisted.
 * So for example, a message to be persisted consists of a header byte
 * array starting at offset 1 and length 4, plus a payload byte array
 * starting at offset 30 and length 40000. There are three ways in which
 * the persistence implementation may return data to the client on
 * recovery:</p> 
 * <ul>
 * <li>It could return the data as it was passed in
 * originally, with the same byte arrays and offsets.</li>
 * <li>It could safely just persist and return the bytes from the offset
 * for the specified length. For example, return a header byte array with
 * offset 0 and length 4, plus a payload byte array with offset 0 and length
 * 40000</li>
 * <li>It could return the header and payload as a contiguous byte array
 * with the header bytes preceeding the payload. The contiguous byte array
 * should be set as the header byte array, with the payload byte array being
 * null. For example, return a single byte array with offset 0 
 * and length 40004.
 * This is useful when recovering from a file where the header and payload
 * could be written as a contiguous stream of bytes.</li>
 * </ul> 
 */
public interface MqttPersistable {

	/**
	 * Returns the header bytes in an array.
	 * The bytes start at {@link #getHeaderOffset()}
	 * and continue for {@link #getHeaderLength()}.
	 * @return the header bytes. 
	 * @throws MqttPersistenceException if an error occurs getting the Header Bytes
	 */
	public byte[] getHeaderBytes() throws MqttPersistenceException;

	/**
	 * Returns the length of the header.
	 * @return the header length
	 * @throws MqttPersistenceException if an error occurs getting the Header length
	 */
	public int getHeaderLength() throws MqttPersistenceException;

	/**
	 * Returns the offset of the header within the byte array returned by {@link #getHeaderBytes()}.
	 * @return the header offset.
	 * @throws MqttPersistenceException if an error occurs getting the Header offset
	 * 
	 */
	public int getHeaderOffset() throws MqttPersistenceException;

	/**
	 * Returns the payload bytes in an array.
	 * The bytes start at {@link #getPayloadOffset()}
	 * and continue for {@link #getPayloadLength()}.
	 * @return the payload bytes.  
	 * @throws MqttPersistenceException if an error occurs getting the Payload Bytes
	 */
	public byte[] getPayloadBytes() throws MqttPersistenceException;

	/**
	 * Returns the length of the payload.
	 * @return the payload length.
	 * @throws MqttPersistenceException if an error occurs getting the Payload length
	 */
	public int getPayloadLength() throws MqttPersistenceException;

	/**
	 * Returns the offset of the payload within the byte array returned by {@link #getPayloadBytes()}.
	 * @return the payload offset.
	 * @throws MqttPersistenceException if an error occurs getting the Payload Offset
	 * 
	 */
	public int getPayloadOffset() throws MqttPersistenceException;
}
