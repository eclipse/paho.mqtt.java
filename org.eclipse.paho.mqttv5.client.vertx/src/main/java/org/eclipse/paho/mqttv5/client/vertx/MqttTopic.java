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
package org.eclipse.paho.mqttv5.client.vertx;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;

/**
 * Represents a topic destination, used for publish/subscribe messaging.
 */
public class MqttTopic {

	private String name;

	/**
	 * @param name
	 *            The Name of the topic
	 * @param comms
	 *            The {@link ClientComms}
	 */
	public MqttTopic(String name) {
		this.name = name;
	}

	/**
	 * Publishes a message on the topic. This is a convenience method, which will
	 * create a new {@link MqttMessage} object with a byte array payload and the
	 * specified QoS, and then publish it. All other values in the message will be
	 * set to the defaults.
	 * 
	 * @param payload
	 *            the byte array to use as the payload
	 * @param qos
	 *            the Quality of Service. Valid values are 0, 1 or 2.
	 * @param retained
	 *            whether or not this message should be retained by the server.
	 * @return {@link MqttDeliveryToken}
	 * @throws MqttException
	 *             If an error occurs publishing the message
	 * @throws MqttPersistenceException
	 *             If an error occurs persisting the message
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @see #publish(MqttMessage)
	 * @see MqttMessage#setQos(int)
	 * @see MqttMessage#setRetained(boolean)
	 */
	public MqttDeliveryToken publish(byte[] payload, int qos, boolean retained)
			throws MqttException, MqttPersistenceException {
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		return this.publish(message);
	}

	/**
	 * Publishes the specified message to this topic, but does not wait for delivery
	 * of the message to complete. The returned {@link MqttDeliveryToken token} can
	 * be used to track the delivery status of the message. Once this method has
	 * returned cleanly, the message has been accepted for publication by the
	 * client. Message delivery will be completed in the background when a
	 * connection is available.
	 *
	 * @param message
	 *            the message to publish
	 * @return an MqttDeliveryToken for tracking the delivery of the message
	 * @throws MqttException
	 *             if an error occurs publishing the message
	 * @throws MqttPersistenceException
	 *             if an error occurs persisting the message
	 */
	public MqttDeliveryToken publish(MqttMessage message) throws MqttException, MqttPersistenceException {
		MqttDeliveryToken token = null; //new MqttDeliveryToken(comms.getClient().getClientId());
		//token.setMessage(message);
		//comms.sendNoWait(createPublish(message, new MqttProperties()), token);
		//token.internalTok.waitUntilSent();
		return token;
	}

	/**
	 * Returns the name of the queue or topic.
	 *
	 * @return the name of this destination.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Create a PUBLISH packet from the specified message.
	 */
	private MqttPublish createPublish(MqttMessage message, MqttProperties properties) {
		return new MqttPublish(this.getName(), message, properties);
	}

	/**
	 * Returns a string representation of this topic.
	 * 
	 * @return a string representation of this topic.
	 */
	public String toString() {
		return getName();
	}

}
