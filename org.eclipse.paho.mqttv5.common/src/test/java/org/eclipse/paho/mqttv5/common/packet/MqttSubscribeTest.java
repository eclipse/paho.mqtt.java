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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.Assert;
import org.junit.Test;

public class MqttSubscribeTest {
	private static final Integer subscriptionIdentifier = 42424242;
	private static final String topicAB = "a/b";
	private static final String topicCD = "c/d";
	private static final String topicEF = "e/f";
	private static final String topicGH = "g/h";
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";

	@Test
	public void testEncodingMqttSubscribe() throws MqttException {
		MqttSubscription subscription = new MqttSubscription("a/b");
		MqttProperties properties =  new MqttProperties();
		MqttSubscribe mqttSubscribePacket = new MqttSubscribe(subscription, properties);
		mqttSubscribePacket.getHeader();
		mqttSubscribePacket.getPayload();
	}
	
	@Test
	public void testEncodingMqttSubscribeWithMultipleTopics() throws MqttException {
		ArrayList<MqttSubscription> subscriptions = new ArrayList<>();
		subscriptions.add(new MqttSubscription(topicAB));
		subscriptions.add(new MqttSubscription(topicCD));
		subscriptions.add(new MqttSubscription(topicEF));
		subscriptions.add(new MqttSubscription(topicGH));
		MqttSubscription[] subArray = subscriptions.toArray(new MqttSubscription[subscriptions.size()]);
		MqttProperties properties = new MqttProperties();
		MqttSubscribe mqttSubscribePacket = new MqttSubscribe(subArray, properties);
		mqttSubscribePacket.getHeader();
		mqttSubscribePacket.getPayload();
	}
	
	@Test
	public void testDecodingSimpleMqttSubscribe() throws MqttException, IOException {
		MqttSubscription subscription = new MqttSubscription(topicAB);
		MqttProperties properties = new MqttProperties();
		properties.setSubscriptionIdentifier(subscriptionIdentifier);
		MqttSubscribe mqttSubscribePacket = new MqttSubscribe(subscription, properties);
		
		byte[] header = mqttSubscribePacket.getHeader();
		byte[] payload = mqttSubscribePacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttSubscribe decodedMqttSubscribePacket = (MqttSubscribe) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		MqttProperties decodedProperties = decodedMqttSubscribePacket.getProperties();
		
		MqttSubscription[] decodedSubscriptions = decodedMqttSubscribePacket.getSubscriptions();
		Assert.assertEquals(1, decodedSubscriptions.length);
		
		MqttSubscription decodedSub = decodedSubscriptions[0];

		// Assert that apart from the topic, all items are set to defaults
		Assert.assertEquals(topicAB, decodedSub.getTopic());
		Assert.assertEquals(1, decodedSub.getQos());
		Assert.assertEquals(false, decodedSub.isNoLocal());
		Assert.assertEquals(false, decodedSub.isRetainAsPublished());
		Assert.assertEquals(0, decodedSub.getRetainHandling());
		
		
		Assert.assertEquals(subscriptionIdentifier, decodedProperties.getSubscriptionIdentifier());
	}

	@Test
	public void testDecodingMqttSubscribeWithMultipleTopics() throws MqttException, IOException {
		ArrayList<MqttSubscription> subscriptions = new ArrayList<>();
		subscriptions.add(new MqttSubscription(topicAB));
		subscriptions.add(new MqttSubscription(topicCD));
		subscriptions.add(new MqttSubscription(topicEF));
		subscriptions.add(new MqttSubscription(topicGH));
		MqttSubscription[] subArray = subscriptions.toArray(new MqttSubscription[subscriptions.size()]);
		MqttProperties properties = new MqttProperties();
		properties.setSubscriptionIdentifier(subscriptionIdentifier);
		
		MqttSubscribe mqttSubscribePacket = new MqttSubscribe(subArray, properties);
		
		byte[] header = mqttSubscribePacket.getHeader();
		byte[] payload = mqttSubscribePacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttSubscribe decodedMqttSubscribePacket = (MqttSubscribe) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		MqttProperties decodedProperties = decodedMqttSubscribePacket.getProperties();
		
		MqttSubscription[] decodedSubscriptions = decodedMqttSubscribePacket.getSubscriptions();
		
		Assert.assertEquals(4, decodedSubscriptions.length);
		
		Assert.assertEquals(topicAB, decodedSubscriptions[0].getTopic());
		Assert.assertEquals(topicCD, decodedSubscriptions[1].getTopic());
		Assert.assertEquals(topicEF, decodedSubscriptions[2].getTopic());
		Assert.assertEquals(topicGH, decodedSubscriptions[3].getTopic());
		
		Assert.assertEquals(subscriptionIdentifier, decodedProperties.getSubscriptionIdentifier());
	}
	
	@Test
	public void testDecodingComplexMqttSubscribe() throws MqttException, IOException {
		MqttSubscription subscription = new MqttSubscription(topicAB);
		subscription.setQos(2);
		subscription.setNoLocal(true);
		subscription.setRetainAsPublished(true);
		subscription.setRetainHandling(2);
		MqttProperties properties = new MqttProperties();
		
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		properties.setUserProperties(userDefinedProperties);
		properties.setSubscriptionIdentifier(subscriptionIdentifier);
		MqttSubscribe mqttSubscribePacket = new MqttSubscribe(subscription, properties);
		byte[] header = mqttSubscribePacket.getHeader();
		byte[] payload = mqttSubscribePacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		MqttSubscribe decodedMqttSubscribePacket = (MqttSubscribe) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		MqttProperties decodedProperties = decodedMqttSubscribePacket.getProperties();
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(decodedProperties.getUserProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(decodedProperties.getUserProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(decodedProperties.getUserProperties().get(2)));
		
		MqttSubscription[] decodedSubscriptions = decodedMqttSubscribePacket.getSubscriptions();
		Assert.assertEquals(decodedSubscriptions.length, 1);
		
		MqttSubscription decodedSub = decodedSubscriptions[0];
		
		// Assert that apart from the topic, all items are set to defaults
		Assert.assertEquals(topicAB, decodedSub.getTopic());
		Assert.assertEquals(2, decodedSub.getQos());
		Assert.assertEquals(true, decodedSub.isNoLocal());
		Assert.assertEquals(true, decodedSub.isRetainAsPublished());
		Assert.assertEquals(2, decodedSub.getRetainHandling());
		
		Assert.assertEquals(subscriptionIdentifier, decodedProperties.getSubscriptionIdentifier());
	}
	
}
