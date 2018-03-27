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
package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.util.CountingInputStream;

public class MqttSubscribe extends MqttWireMessage {

	private static final Byte[] validProperties = { MqttProperties.SUBSCRIPTION_IDENTIFIER,
			MqttProperties.SUBSCRIPTION_IDENTIFIER_SINGLE,
			MqttProperties.USER_DEFINED_PAIR_IDENTIFIER };

	// Fields
	private MqttProperties properties;
	private MqttSubscription[] subscriptions;

	/**
	 * Constructor for an on the Wire MQTT Subscribe message
	 * 
	 * @param data
	 *            - The variable header and payload bytes.
	 * @throws IOException
	 *             - if an exception occurs when decoding an input stream
	 * @throws MqttException
	 *             - If an exception occurs decoding this packet
	 */
	public MqttSubscribe(byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
		this.properties = new MqttProperties(validProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream inputStream = new DataInputStream(counter);
		msgId = inputStream.readUnsignedShort();

		this.properties.decodeProperties(inputStream);

		ArrayList<MqttSubscription> subscriptionList = new ArrayList<>();
		// Whilst we are reading data
		while (counter.getCounter() < data.length) {
			String topic = MqttDataTypes.decodeUTF8(inputStream);
			byte subscriptionOptions = inputStream.readByte();
			subscriptionList.add(decodeSubscription(topic, subscriptionOptions));
		}
		subscriptions = subscriptionList.toArray(new MqttSubscription[subscriptionList.size()]);
		inputStream.close();
	}

	/**
	 * Constructor for an on the Wire MQTT Subscribe message
	 * 
	 * @param subscriptions
	 *            - An Array of {@link MqttSubscription} subscriptions.
	 * @param properties
	 *            - The {@link MqttProperties} for the packet.
	 */
	public MqttSubscribe(MqttSubscription[] subscriptions, MqttProperties properties) {
		super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
		this.subscriptions = subscriptions;
		if (properties != null) {
			this.properties = properties;
		} else {
			this.properties = new MqttProperties();
		}
		this.properties.setValidProperties(validProperties);
	}

	/**
	 * Constructor for an on the Wire MQTT Subscribe message
	 * 
	 * @param subscription
	 *            - An {@link MqttSubscription}
	 * @param properties
	 *            - The {@link MqttProperties} for the packet.
	 */
	public MqttSubscribe(MqttSubscription subscription, MqttProperties properties) {
		super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
		this.subscriptions = new MqttSubscription[] { subscription };
		this.properties = properties;
		this.properties.setValidProperties(validProperties);
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Encode the Message ID
			outputStream.writeShort(msgId);

			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();
			outputStream.write(identifierValueFieldsByteArray);

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	@Override
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			for (MqttSubscription subscription : subscriptions) {
				outputStream.write(encodeSubscription(subscription));
			}

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	@Override
	public boolean isRetryable() {
		return true;
	}

	/**
	 * Encodes an {@link MqttSubscription} into it's on-the-wire representation.
	 * Assumes that the Subscription topic is valid.
	 * 
	 * @param subscription
	 *            - The {@link MqttSubscription} to encode.
	 * @return A byte array containing the encoded subscription.
	 * @throws MqttException
	 */
	private byte[] encodeSubscription(MqttSubscription subscription) throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			MqttDataTypes.encodeUTF8(outputStream, subscription.getTopic());

			// Encode Subscription QoS
			byte subscriptionOptions = (byte) subscription.getQos();

			// Encode NoLocal Option
			if (subscription.isNoLocal()) {
				subscriptionOptions |= 0x04;
			}

			// Encode Retain As Published Option
			if (subscription.isRetainAsPublished()) {
				subscriptionOptions |= 0x08;
			}

			// Encode Retain Handling Level
			subscriptionOptions |= (subscription.getRetainHandling() << 4);

			outputStream.write(subscriptionOptions);

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	private MqttSubscription decodeSubscription(String topic, byte subscriptionOptions) {
		MqttSubscription subscription = new MqttSubscription(topic);
		subscription.setQos(subscriptionOptions & 0x03);
		subscription.setNoLocal((subscriptionOptions & 0x04) != 0);
		subscription.setRetainAsPublished((subscriptionOptions & 0x08) != 0);
		subscription.setRetainHandling((subscriptionOptions >> 4) & 0x03);
		return subscription;
	}

	@Override
	protected byte getMessageInfo() {
		return (byte) (2 | (duplicate ? 8 : 0));
	}

	public MqttSubscription[] getSubscriptions() {
		return subscriptions;
	}

	@Override
	public MqttProperties getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "MqttSubscribe [properties=" + properties + ", subscriptions=" + Arrays.toString(subscriptions) + "]";
	}

}
