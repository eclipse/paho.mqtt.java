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
 *    Ian Craggs - ack control (bug 472172)
 */
package org.eclipse.paho.client.mqttv3;

/**
 * An MQTT message holds the application payload and options
 * specifying how the message is to be delivered
 * The message includes a "payload" (the body of the message)
 * represented as a byte[].
 */
public class MqttMessage {

	private boolean mutable = true;
	private byte[] payload;
	private int qos = 1;
	private boolean retained = false;
	private boolean dup = false;
	private int messageId;

	/**
	 * Utility method to validate the supplied QoS value.
	 * @param qos The QoS Level
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 */
	public static void validateQos(int qos) {
		if ((qos < 0) || (qos > 2)) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Constructs a message with an empty payload, and all other values
	 * set to defaults.
	 *
	 * The defaults are:
	 * <ul>
	 *    <li>Message QoS set to 1</li>
	 *    <li>Message will not be "retained" by the server</li>
	 * </ul>
	 */
	public MqttMessage() {
		setPayload(new byte[]{});
	}

	/**
	 * Constructs a message with the specified byte array as a payload,
	 * and all other values set to defaults.
	 * @param payload The Bytearray of the payload
	 */
	public MqttMessage(byte[] payload) {
		setPayload(payload);
	}

	/**
	 * Returns the payload as a byte array.
	 *
	 * @return the payload as a byte array.
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Clears the payload, resetting it to be empty.
	 * @throws IllegalStateException if this message cannot be edited
	 */
	public void clearPayload() {
		checkMutable();
		this.payload = new byte[] {};
	}

	/**
	 * Sets the payload of this message to be the specified byte array.
	 *
	 * @param payload the payload for this message.
	 * @throws IllegalStateException if this message cannot be edited
	 * @throws NullPointerException if no payload is provided
	 */
	public void setPayload(byte[] payload) {
		checkMutable();
		if (payload == null) {
			throw new NullPointerException();
		}
		this.payload = payload;
	}

	/**
	 * Returns whether or not this message should be/was retained by the server.
	 * For messages received from the server, this method returns whether or not
	 * the message was from a current publisher, or was "retained" by the server as
	 * the last message published on the topic.
	 *
	 * @return <code>true</code> if the message should be, or was, retained by
	 * the server.
	 * @see #setRetained(boolean)
	 */
	public boolean isRetained() {
		return retained;
	}

	/**
	 * Whether or not the publish message should be retained by the messaging engine.
	 * Sending a message with retained set to <code>true</code> and with an empty
         * byte array as the payload e.g. <code>new byte[0]</code> will clear the
	 * retained message from the server.  The default value is <code>false</code>
	 *
	 * @param retained whether or not the messaging engine should retain the message.
	 * @throws IllegalStateException if this message cannot be edited
	 */
	public void setRetained(boolean retained) {
		checkMutable();
		this.retained = retained;
	}

	/**
	 * Returns the quality of service for this message.
	 * @return the quality of service to use, either 0, 1, or 2.
	 * @see #setQos(int)
	 */
	public int getQos() {
		return qos;
	}

	/**
	 * Sets the quality of service for this message.
	 * <ul>
	 * <li>Quality of Service 0 - indicates that a message should
	 * be delivered at most once (zero or one times).  The message will not be persisted to disk,
	 * and will not be acknowledged across the network.  This QoS is the fastest,
	 * but should only be used for messages which are not valuable - note that
	 * if the server cannot process the message (for example, there
	 * is an authorization problem), then an
	 * {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)}.
	 * Also known as "fire and forget".</li>
	 *
	 * <li>Quality of Service 1 - indicates that a message should
	 * be delivered at least once (one or more times).  The message can only be delivered safely if
	 * it can be persisted, so the application must supply a means of
	 * persistence using <code>MqttConnectOptions</code>.
	 * If a persistence mechanism is not specified, the message will not be
	 * delivered in the event of a client failure.
	 * The message will be acknowledged across the network.
	 * This is the default QoS.</li>
	 *
	 * <li>Quality of Service 2 - indicates that a message should
	 * be delivered once.  The message will be persisted to disk, and will
	 * be subject to a two-phase acknowledgement across the network.
	 * The message can only be delivered safely if
	 * it can be persisted, so the application must supply a means of
	 * persistence using <code>MqttConnectOptions</code>.
	 * If a persistence mechanism is not specified, the message will not be
	 * delivered in the event of a client failure.</li>
	 *
	 *</ul>
	 * If persistence is not configured, QoS 1 and 2 messages will still be delivered
	 * in the event of a network or server problem as the client will hold state in memory.
	 * If the MQTT client is shutdown or fails and persistence is not configured then
	 * delivery of QoS 1 and 2 messages can not be maintained as client-side state will
	 * be lost.
	 *
	 * @param qos the "quality of service" to use.  Set to 0, 1, 2.
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @throws IllegalStateException if this message cannot be edited
	 */
	public void setQos(int qos) {
		checkMutable();
		validateQos(qos);
		this.qos = qos;
	}

	/**
	 * Returns a string representation of this message's payload.
	 * Makes an attempt to return the payload as a string. As the
	 * MQTT client has no control over the content of the payload
	 * it may fail.
	 * @return a string representation of this message.
	 */
	public String toString() {
		return new String(payload);
	}

	/**
	 * Sets the mutability of this object (whether or not its values can be
	 * changed.
	 * @param mutable <code>true</code> if the values can be changed,
	 * <code>false</code> to prevent them from being changed.
	 */
	protected void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	protected void checkMutable() throws IllegalStateException {
		if (!mutable) {
			throw new IllegalStateException();
		}
	}

	protected void setDuplicate(boolean dup) {
		this.dup = dup;
	}

	/**
	 * Returns whether or not this message might be a duplicate of one which has
	 * already been received.  This will only be set on messages received from
	 * the server.
	 * @return <code>true</code> if the message might be a duplicate.
	 */
	public boolean isDuplicate() {
		return this.dup;
	}
	
	/**
	 * This is only to be used internally to provide the MQTT id of a message
	 * received from the server.  Has no effect when publishing messages.
	 * @param messageId The Message ID
	 */
	public void setId(int messageId) {
		this.messageId = messageId;
	}

	/**
	 * Returns the MQTT id of the message.  This is only applicable to messages
	 * received from the server.
	 * @return the MQTT id of the message
	 */
	public int getId() {
		return this.messageId;
	}
	
	
	
}
