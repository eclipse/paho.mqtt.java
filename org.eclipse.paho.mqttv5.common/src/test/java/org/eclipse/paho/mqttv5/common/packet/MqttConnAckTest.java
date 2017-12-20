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
 * 	  Dave Locke - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Assert;
import org.junit.Test;

public class MqttConnAckTest {
	
	private static final boolean sessionPresent = true;
	private static final int returnCode = MqttReturnCode.RETURN_CODE_SERVER_MOVED;
	private static final Integer receiveMaximum = 100;
	private static final Integer maximumQoS = 1;
	private static final boolean retainAvailableAdvertisement = true;
	private static final Long maximumPacketSize = 128000L;
	private static final String assignedClientIdentifier = "AssignedClientId";
	private static final Integer topicAliasMaximum = 100;
	private static final String reasonString = "Everything is fine.";
	private static final Long sessionExpiryInterval = 60L;
	private static final boolean wildcardSubscriptionsAvailable = true;
	private static final boolean subscriptionIdentifiersAvailable = true;
	private static final boolean sharedSubscriptionAvailable = true;
	private static final Integer serverKeepAlive = 60;
	private static final String responseInfo = "assignedTopicForSession";
	private static final String serverReference = "127.0.0.1";
	private static final String authMethod = "PASSWORD";
	private static final byte[] authData = "secretPassword123".getBytes();
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";

	

	/**
	 * Tests that an MqttConnAck packet can be encoded successfully
	 * without throwing any exceptions. 
	 * @throws MqttException 
	 */
	@Test
	public void testEncodingMqttConnAck() throws MqttException {
		MqttConnAck mqttConnAckPacket = generateMqttConnAckPacket();
		mqttConnAckPacket.getHeader();
		mqttConnAckPacket.getPayload();
	}
	
	
	/**
	 * Tests that an MqttConnAck packet can be decoded
	 * successfully
	 */
	@Test
	public void testDecodingMqttConnAck() throws IOException, MqttException {
		MqttConnAck mqttConnAckPacket = generateMqttConnAckPacket();
		byte[] header = mqttConnAckPacket.getHeader();
		byte[] payload = mqttConnAckPacket.getPayload();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttConnAck decodedConnAckPacket = (MqttConnAck) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		MqttProperties properties = decodedConnAckPacket.getProperties();
		
		Assert.assertEquals(receiveMaximum, properties.getReceiveMaximum());
		Assert.assertEquals(maximumQoS, properties.getMaximumQoS());
		Assert.assertEquals(retainAvailableAdvertisement, properties.isRetainAvailable());
		Assert.assertEquals(maximumPacketSize, properties.getMaximumPacketSize());
		Assert.assertEquals(assignedClientIdentifier, properties.getAssignedClientIdentifier());
		Assert.assertEquals(topicAliasMaximum, properties.getTopicAliasMaximum());
		Assert.assertEquals(reasonString, properties.getReasonString());
		Assert.assertEquals(sessionExpiryInterval, properties.getSessionExpiryInterval());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(properties.getUserProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(properties.getUserProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(properties.getUserProperties().get(2)));
		Assert.assertEquals(wildcardSubscriptionsAvailable, properties.isWildcardSubscriptionsAvailable());
		Assert.assertEquals(subscriptionIdentifiersAvailable, properties.isSubscriptionIdentifiersAvailable());
		Assert.assertEquals(sharedSubscriptionAvailable, properties.isSharedSubscriptionAvailable());
		Assert.assertEquals(serverKeepAlive, properties.getServerKeepAlive());
		Assert.assertEquals(responseInfo, properties.getResponseInfo());
		Assert.assertEquals(serverReference, properties.getServerReference());
		Assert.assertEquals(authMethod, properties.getAuthenticationMethod());
		Assert.assertArrayEquals(authData, properties.getAuthenticationData());
		Assert.assertEquals(sessionPresent, decodedConnAckPacket.getSessionPresent());
		Assert.assertEquals(returnCode, decodedConnAckPacket.getReturnCode());
	}
	
	
	
	private MqttConnAck generateMqttConnAckPacket() throws MqttException{
		
		MqttProperties properties = new MqttProperties();
		
		properties.setReceiveMaximum(100);
		
		properties.setReceiveMaximum(receiveMaximum);
		properties.setMaximumQoS(maximumQoS);
		properties.setRetainAvailable(retainAvailableAdvertisement);
		properties.setMaximumPacketSize(maximumPacketSize);
		properties.setAssignedClientIdentifier(assignedClientIdentifier);
		properties.setTopicAliasMaximum(topicAliasMaximum);
		properties.setReasonString(reasonString);
		properties.setSessionExpiryInterval(sessionExpiryInterval);
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		properties.setUserProperties(userDefinedProperties);
		properties.setWildcardSubscriptionsAvailable(wildcardSubscriptionsAvailable);
		properties.setSubscriptionIdentifiersAvailable(subscriptionIdentifiersAvailable);
		properties.setSharedSubscriptionAvailable(sharedSubscriptionAvailable);
		properties.setServerKeepAlive(serverKeepAlive);
		properties.setResponseInfo(responseInfo);
		properties.setServerReference(serverReference);
		properties.setAuthenticationMethod(authMethod);
		properties.setAuthenticationData(authData);
		
		MqttConnAck mqttConnAckPacket = new MqttConnAck(sessionPresent, returnCode, properties);
		
		return mqttConnAckPacket;
	}

}



