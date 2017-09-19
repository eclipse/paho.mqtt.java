/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.client.internal;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

public interface MqttState {

	/**
	 * Submits a message for delivery. This method will block until there is
	 * room in the inFlightWindow for the message. The message is put into
	 * persistence before returning.
	 * 
	 * @param message  the message to send
	 * @param token the token that can be used to track delivery of the message
	 * @throws MqttException if an exception occurs whilst sending the message
	 */
	void send(MqttWireMessage message, MqttToken token) throws MqttException;

	/**
	 * Persists a buffered message to the persistence layer
	 * 
	 * @param message The {@link MqttWireMessage} to persist
	 */
	void persistBufferedMessage(MqttWireMessage message);

	/**
	 * @param message The {@link MqttWireMessage} to un-persist
	 */
	void unPersistBufferedMessage(MqttWireMessage message);

	/**
	 * Check and send a ping if needed and check for ping timeout.
	 * Need to send a ping if nothing has been sent or received  
	 * in the last keepalive interval. It is important to check for 
	 * both sent and received packets in order to catch the case where an 
	 * app is solely sending QoS 0 messages or receiving QoS 0 messages.
	 * QoS 0 message are not good enough for checking a connection is
	 * alive as they are one way messages.
	 * 
	 * If a ping has been sent but no data has been received in the 
	 * last keepalive interval then the connection is deamed to be broken. 
	 * @param pingCallback The {@link MqttActionListener} to be called
	 * @return token of ping command, null if no ping command has been sent.
	 * @throws MqttException if an exception occurs during the Ping
	 */
	MqttToken checkForActivity(MqttActionListener pingCallback) throws MqttException;

	void setKeepAliveInterval(long interval);

	void notifySentBytes(int sentBytesCount);

	void notifyReceivedBytes(int receivedBytesCount);

	/**
	 * Called when the client has successfully connected to the broker
	 */
	void connected();

	/**
	 * Called during shutdown to work out if there are any tokens still
	 * to be notified and waiters to be unblocked.  Notifying and unblocking 
	 * takes place after most shutdown processing has completed. The tokenstore
	 * is tidied up so it only contains outstanding delivery tokens which are
	 * valid after reconnect (if clean session is false)
	 * @param reason The root cause of the disconnection, or null if it is a clean disconnect
	 * @return {@link Vector} 
	 */
	Vector<MqttToken> resolveOldTokens(MqttException reason);

	/**
	 * Called when the client has been disconnected from the broker.
	 * @param reason The root cause of the disconnection, or null if it is a clean disconnect
	 */
	void disconnected(MqttException reason);

	/**
	 * Quiesce the client state, preventing any new messages getting sent,
	 * and preventing the callback on any newly received messages.
	 * After the timeout expires, delete any pending messages except for
	 * outbound ACKs, and wait for those ACKs to complete.
	 * @param timeout How long to wait during Quiescing
	 */
	void quiesce(long timeout);

	void notifyQueueLock();

	int getActualInFlight();

	int getMaxInFlight();

	Properties getDebug();

}