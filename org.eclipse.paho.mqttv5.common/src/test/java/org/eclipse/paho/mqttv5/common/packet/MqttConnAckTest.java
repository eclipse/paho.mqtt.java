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
	private static final int receiveMaximum = 100;
	private static final int maximumQoS = 1;
	private static final boolean retainAvailableAdvertisement = true;
	private static final int maximumPacketSize = 128000;
	private static final String assignedClientIdentifier = "AssignedClientId";
	private static final int topicAliasMaximum = 100;
	private static final String reasonString = "Everything is fine.";
	private static final int sessionExpiryInterval = 60;
	private static final boolean wildcardSubscriptionsAvailable = true;
	private static final boolean subscriptionIdentifiersAvailable = true;
	private static final boolean sharedSubscriptionAvailable = true;
	private static final int serverKeepAlive = 60;
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
		
		Assert.assertEquals(receiveMaximum, decodedConnAckPacket.getReceiveMaximum());
		Assert.assertEquals(maximumQoS, decodedConnAckPacket.getMaximumQoS());
		Assert.assertEquals(retainAvailableAdvertisement, decodedConnAckPacket.getRetainUnavailableAdvertisement());
		Assert.assertEquals(maximumPacketSize, decodedConnAckPacket.getMaximumPacketSize());
		Assert.assertEquals(assignedClientIdentifier, decodedConnAckPacket.getAssignedClientIdentifier());
		Assert.assertEquals(topicAliasMaximum, decodedConnAckPacket.getTopicAliasMaximum());
		Assert.assertEquals(reasonString, decodedConnAckPacket.getReasonString());
		Assert.assertEquals(sessionExpiryInterval, decodedConnAckPacket.getSessionExpiryInterval());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(decodedConnAckPacket.getUserDefinedProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(decodedConnAckPacket.getUserDefinedProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(decodedConnAckPacket.getUserDefinedProperties().get(2)));
		Assert.assertEquals(wildcardSubscriptionsAvailable, decodedConnAckPacket.isWildcardSubscriptionsAvailable());
		Assert.assertEquals(subscriptionIdentifiersAvailable, decodedConnAckPacket.isSubscriptionIdentifiersAvailable());
		Assert.assertEquals(sharedSubscriptionAvailable, decodedConnAckPacket.isSharedSubscriptionAvailable());
		Assert.assertEquals(serverKeepAlive, decodedConnAckPacket.getServerKeepAlive());
		Assert.assertEquals(responseInfo, decodedConnAckPacket.getResponseInfo());
		Assert.assertEquals(serverReference, decodedConnAckPacket.getServerReference());
		Assert.assertEquals(authMethod, decodedConnAckPacket.getAuthMethod());
		Assert.assertArrayEquals(authData, decodedConnAckPacket.getAuthData());
		Assert.assertEquals(sessionPresent, decodedConnAckPacket.getSessionPresent());
		Assert.assertEquals(returnCode, decodedConnAckPacket.getReturnCode());
	}
	
	/**
	 * Tests that you cannot assign a server reference if the return code
	 * is not 0x9C or 0x9D
	 * @throws MqttException 
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testServerReferenceException() throws MqttException{
		MqttConnAck mqttConnAckPacket = new MqttConnAck(sessionPresent, MqttReturnCode.RETURN_CODE_SUCCESS);
		mqttConnAckPacket.setServerReference(serverReference);
	}
	
	
	private MqttConnAck generateMqttConnAckPacket() throws MqttException{
		MqttConnAck mqttConnAckPacket = new MqttConnAck(sessionPresent, returnCode);
		mqttConnAckPacket.setReceiveMaximum(100);
		
		mqttConnAckPacket.setReceiveMaximum(receiveMaximum);
		mqttConnAckPacket.setMaximumQoS(maximumQoS);
		mqttConnAckPacket.setRetainAvailableAdvertisement(retainAvailableAdvertisement);
		mqttConnAckPacket.setMaximumPacketSize(maximumPacketSize);
		mqttConnAckPacket.setAssignedClientIdentifier(assignedClientIdentifier);
		mqttConnAckPacket.setTopicAliasMaximum(topicAliasMaximum);
		mqttConnAckPacket.setReasonString(reasonString);
		mqttConnAckPacket.setSessionExpiryInterval(sessionExpiryInterval);
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		mqttConnAckPacket.setUserDefinedProperties(userDefinedProperties);
		mqttConnAckPacket.setWildcardSubscriptionsAvailable(wildcardSubscriptionsAvailable);
		mqttConnAckPacket.setSubscriptionIdentifiersAvailable(subscriptionIdentifiersAvailable);
		mqttConnAckPacket.setSharedSubscriptionAvailable(sharedSubscriptionAvailable);
		mqttConnAckPacket.setServerKeepAlive(serverKeepAlive);
		mqttConnAckPacket.setResponseInfo(responseInfo);
		mqttConnAckPacket.setServerReference(serverReference);
		mqttConnAckPacket.setAuthMethod(authMethod);
		mqttConnAckPacket.setAuthData(authData);
		
		return mqttConnAckPacket;
	}

}



