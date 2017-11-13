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
import java.util.List;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.util.CountingInputStream;

public class MqttSubscribe extends MqttWireMessage {

	// Fields
	private MqttSubscription[] subscriptions;
	private Integer subscriptionIdentifier;
	private List<UserProperty> userDefinedProperties = new ArrayList<>();

	/**
	 * Constructor for an on the Wire MQTT Subscribe message
	 * 
	 * @param info
	 *            - Info Byte
	 * @param data
	 *            - The variable header and payload bytes.
	 * @throws IOException
	 *             - if an exception occurs when decoding an input stream
	 * @throws MqttException
	 *             - If an exception occurs decoding this packet
	 */
	public MqttSubscribe(byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream inputStream = new DataInputStream(counter);
		msgId = inputStream.readUnsignedShort();

		parseIdentifierValueFields(inputStream);

		ArrayList<MqttSubscription> subscriptionList = new ArrayList<>();
		// Whilst we are reading data
		while (counter.getCounter() < data.length) {
			String topic = decodeUTF8(inputStream);
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
	 */
	public MqttSubscribe(MqttSubscription[] subscriptions) {
		super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
		this.subscriptions = subscriptions;
	}

	/**
	 * Constructor for an on the Wire MQTT Subscribe message
	 * 
	 * @param subscription
	 *            - An {@link MqttSubscription}
	 */
	public MqttSubscribe(MqttSubscription subscription) {
		super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
		this.subscriptions = new MqttSubscription[] { subscription };
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Encode the Message ID
			outputStream.writeShort(msgId);

			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = getIdentifierValueFields();
			outputStream.write(encodeVariableByteInteger(identifierValueFieldsByteArray.length));
			outputStream.write(identifierValueFieldsByteArray);

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	private byte[] getIdentifierValueFields() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// If Present, encode the Subscription Identifier
			if (subscriptionIdentifier != null) {
				outputStream.write(MqttPropertyIdentifiers.SUBSCRIPTION_IDENTIFIER);
				outputStream.write(encodeVariableByteInteger(subscriptionIdentifier));
			}

			// If Present, encode the User Properties
			if (!userDefinedProperties.isEmpty()) {
				for (UserProperty property : userDefinedProperties) {
					outputStream.write(MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(outputStream, property.getKey());
					encodeUTF8(outputStream, property.getValue());
				}
			}

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	/**
	 * Parses the Variable Header for Identifier Value fields and populates the
	 * relevant fields in this MqttSubscribe message.
	 * 
	 * @param dis
	 * @throws IOException
	 * @throws MqttException
	 */
	private void parseIdentifierValueFields(DataInputStream dis) throws IOException, MqttException {
		int length = readVariableByteInteger(dis).getValue();
		if (length > 0) {
			byte[] identifierValueByteArray = new byte[length];
			dis.read(identifierValueByteArray, 0, length);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
			while (inputStream.available() > 0) {
				// Get the first byte (identifier)
				byte identifier = inputStream.readByte();
				if (identifier == MqttPropertyIdentifiers.SUBSCRIPTION_IDENTIFIER) {
					subscriptionIdentifier = readVariableByteInteger(inputStream).getValue();
				} else if (identifier == MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER) {
					String key = decodeUTF8(inputStream);
					String value = decodeUTF8(inputStream);
					userDefinedProperties.add(new UserProperty(key, value));
				} else {
					// Unidentified Identifier
					throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
				}
			}

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

			encodeUTF8(outputStream, subscription.getTopic());

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

	public int getSubscriptionIdentifier() {
		return subscriptionIdentifier;
	}

	public void setSubscriptionIdentifier(int subscriptionIdentifier) {
		this.subscriptionIdentifier = subscriptionIdentifier;
	}

	public List<UserProperty> getUserDefinedProperties() {
		return userDefinedProperties;
	}

	public void setUserDefinedProperties(List<UserProperty> userDefinedProperties) {
		this.userDefinedProperties = userDefinedProperties;
	}

}
