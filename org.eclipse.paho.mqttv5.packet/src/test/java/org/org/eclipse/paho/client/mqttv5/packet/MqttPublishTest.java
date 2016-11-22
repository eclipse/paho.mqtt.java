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
package org.org.eclipse.paho.client.mqttv5.packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.mqttv5.packet.MqttMessage;
import org.eclipse.paho.mqttv5.packet.MqttPublish;
import org.eclipse.paho.mqttv5.packet.MqttWireMessage;
import org.eclipse.paho.mqttv5.util.MqttException;
import org.junit.Assert;
import org.junit.Test;



public class MqttPublishTest {
	
	private static final String topic = "testTopic";
	private static final String payloadMessage = "Hello World";
	private static final int qos = 1;
	private static final boolean retained = true;
	private static final int payloadFormat = MqttPublish.PAYLOAD_FORMAT_UTF8;
	private static final int publicationExpiryInterval = 60;
	private static final int topicAlias = 1;
	private static final String replyTopic = "replyTopic";
	private static final byte[] correlationData = "correlationData".getBytes();
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
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
		
		Assert.assertEquals(decodedPublishPacket.getTopicName(), topic);
		Assert.assertEquals(decodedPublishPacket.getMessage().getQos(), qos);
		Assert.assertArrayEquals(decodedPublishPacket.getMessage().getPayload(), payloadMessage.getBytes());
		Assert.assertEquals(decodedPublishPacket.getMessage().isRetained(), retained);
		Assert.assertEquals(decodedPublishPacket.getPayloadFormat(), payloadFormat);
		Assert.assertEquals(decodedPublishPacket.getPublicationExpiryInterval(), publicationExpiryInterval);
		Assert.assertEquals(decodedPublishPacket.getTopicAlias(), topicAlias);
		Assert.assertEquals(decodedPublishPacket.getReplyTopic(), replyTopic);
		Assert.assertArrayEquals(decodedPublishPacket.getCorrelationData(), correlationData);
		Assert.assertEquals(decodedPublishPacket.getUserDefinedPairs().size(), 3);
		Assert.assertEquals(decodedPublishPacket.getUserDefinedPairs().get(userKey1), userValue1);
		Assert.assertEquals(decodedPublishPacket.getUserDefinedPairs().get(userKey2), userValue2);
		Assert.assertEquals(decodedPublishPacket.getUserDefinedPairs().get(userKey3), userValue3);
		Assert.assertEquals(decodedPublishPacket.getMessageId(), messageId);
		Assert.assertEquals(decodedPublishPacket.isDuplicate(), duplicate);
		
		
	}

	
	private MqttPublish generateMqttPublishPacket(){
		MqttMessage message = new MqttMessage(payloadMessage.getBytes());
		message.setQos(qos);
		message.setRetained(retained);
		MqttPublish mqttPublish = new MqttPublish(topic, message);
		mqttPublish.setMessageId(messageId);
		mqttPublish.setDuplicate(duplicate);
		
		mqttPublish.setPayloadFormat(payloadFormat);
		mqttPublish.setPublicationExpiryInterval(publicationExpiryInterval);
		mqttPublish.setTopicAlias(topicAlias);
		mqttPublish.setReplyTopic(replyTopic);
		mqttPublish.setCorrelationData(correlationData);
		
		Map<String, String> userDefinedPairs = new HashMap<String,String>();
		userDefinedPairs.put(userKey1, userValue1);
		userDefinedPairs.put(userKey2, userValue2);
		userDefinedPairs.put(userKey3, userValue3);
		mqttPublish.setUserDefinedPairs(userDefinedPairs);
		
		return mqttPublish;
	}
}
