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
import java.util.List;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.Assert;
import org.junit.Test;



public class MqttPublishTest {
	
	private static final String topic = "testTopic";
	private static final String payloadMessage = "Hello World";
	private static final int qos = 1;
	private static final boolean retained = true;
	private static final boolean isUTF8 = true;
	private static final Long publicationExpiryInterval = 60L;
	private static final Integer topicAlias = 1;
	private static final String responseTopic = "replyTopic";
	private static final byte[] correlationData = "correlationData".getBytes();
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
	private static final String contentType = "JSON";
	private static final List<Integer> subscriptionIdentifiers = Arrays.asList(1,2,3,4);
	
	private static final int messageId = 25;
	private static final boolean duplicate = false;
	

	@Test
	public void testEncodingMqttPublish() throws MqttException {
		MqttPublish mqttPublish = generateMqttPublishPacket();
		mqttPublish.getHeader();
		mqttPublish.getPayload();
	}
	
	@Test
	public void testDecodingMqttPublish() throws MqttException, IOException {
		MqttPublish mqttPublish = generateMqttPublishPacket();
		byte[] header = mqttPublish.getHeader();
		byte[] payload = mqttPublish.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttPublish decodedPublishPacket = (MqttPublish) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		MqttProperties properties = decodedPublishPacket.getProperties();
		
		Assert.assertEquals(topic, decodedPublishPacket.getTopicName());
		Assert.assertEquals(qos, decodedPublishPacket.getMessage().getQos());
		Assert.assertArrayEquals(payloadMessage.getBytes(), decodedPublishPacket.getMessage().getPayload());
		Assert.assertEquals(retained, decodedPublishPacket.getMessage().isRetained());
		Assert.assertEquals(isUTF8, properties.getPayloadFormat());
		Assert.assertEquals(publicationExpiryInterval, properties.getMessageExpiryInterval());
		Assert.assertEquals(topicAlias, properties.getTopicAlias());
		Assert.assertEquals(responseTopic, properties.getResponseTopic());
		Assert.assertArrayEquals(correlationData, properties.getCorrelationData());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(properties.getUserProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(properties.getUserProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(properties.getUserProperties().get(2)));

		Assert.assertEquals(contentType, properties.getContentType());
		Assert.assertArrayEquals(subscriptionIdentifiers.toArray(), properties.getSubscriptionIdentifiers().toArray());
		Assert.assertEquals(messageId, decodedPublishPacket.getMessageId());
		Assert.assertEquals(duplicate, decodedPublishPacket.isDuplicate());
		
		
	}

	
	private MqttPublish generateMqttPublishPacket(){
		MqttProperties properties = new MqttProperties();
		MqttMessage message = new MqttMessage(payloadMessage.getBytes());
		message.setQos(qos);
		message.setRetained(retained);
				
		properties.setPayloadFormat(isUTF8);
		properties.setMessageExpiryInterval(publicationExpiryInterval);
		properties.setResponseTopic(responseTopic);
		properties.setCorrelationData(correlationData);
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		properties.setUserProperties(userDefinedProperties);
		properties.setContentType(contentType);
		properties.setSubscriptionIdentifiers(subscriptionIdentifiers);
		properties.setTopicAlias(topicAlias);
		
		MqttPublish mqttPublish = new MqttPublish(topic, message, properties);
		mqttPublish.setMessageId(messageId);
		mqttPublish.setDuplicate(duplicate);
		
		
		return mqttPublish;
	}
}
