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
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttConnectTest{
	
	private static final String clientId = "testClientId";
	private static final int mqttVersion = 5;
	private static final boolean cleanSession = true;
	private static final int keepAliveInterval = 60;
	private static final String userName = "username";
	private static final byte[] password = "password".getBytes();
	private static final String willPayload = "Will Message";
	private static final String willDestination = "will/destination";
	private static final int willQoS = 1;
	
	private static final int sessionExpiryInterval = 60;
	private static final int maxPacketSize = 128000;
	private static final int topicAliasMax = 5;
	private static final boolean requestReplyInfo = true;
	private static final boolean requestPropblemInfo = true;
	private static final String authMethod = "PASSWORD";
	private static final byte[] authData = "secretPassword123".getBytes();
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
	
	private static final int willDelayInterval = 60;
	private static final boolean willIsUTF8 = true;
	private static final int willPublicationExpiryInterval = 60;
	private static final String willResponseTopic = "replyTopic";
	private static final byte[] willCorrelationData = "correlationData".getBytes();
	private static final String willContentType = "JSON";

	/**
	 * Tests that an MqttConnect packet can be encoded successfully
	 * without throwing any exceptions.
	 * @throws MqttException 
	 */
	@Test
	public void testEncodingMqttConnect() throws MqttException{
		MqttConnect mqttConnectPacket = generateConnectPacket();
		mqttConnectPacket.getHeader();
		mqttConnectPacket.getPayload();
	}
	
	/**
	 * Tests that an MqttConnect packet can be decoded 
	 * successfully.
	 * @throws IOException
	 * @throws MqttException
	 */
	@Test
	public void testDecodingMqttConnect() throws IOException, MqttException{
		MqttConnect mqttConnectPacket = generateConnectPacket();
		byte[] header = mqttConnectPacket.getHeader();
		byte[] payload = mqttConnectPacket.getPayload();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
					
		MqttConnect decodedConnectPacket = (MqttConnect) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(clientId, decodedConnectPacket.getClientId());
		Assert.assertEquals(mqttVersion, decodedConnectPacket.getMqttVersion());
		Assert.assertEquals(cleanSession, decodedConnectPacket.isCleanSession());
		Assert.assertEquals(keepAliveInterval, decodedConnectPacket.getKeepAliveInterval());
		Assert.assertEquals(userName, decodedConnectPacket.getUserName());
		Assert.assertArrayEquals(password, decodedConnectPacket.getPassword());
		Assert.assertArrayEquals(willPayload.getBytes(), decodedConnectPacket.getWillMessage().getPayload());
		Assert.assertEquals(willQoS, decodedConnectPacket.getWillMessage().getQos());
		Assert.assertEquals(willDestination, decodedConnectPacket.getWillDestination());
		Assert.assertEquals(sessionExpiryInterval, decodedConnectPacket.getSessionExpiryInterval());
		Assert.assertEquals(maxPacketSize,  decodedConnectPacket.getMaximumPacketSize());
		Assert.assertEquals(topicAliasMax, decodedConnectPacket.getTopicAliasMaximum());
		Assert.assertEquals(requestReplyInfo, decodedConnectPacket.getRequestReplyInfo());
		Assert.assertEquals(requestPropblemInfo, decodedConnectPacket.getRequestProblemInfo());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(decodedConnectPacket.getUserDefinedProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(decodedConnectPacket.getUserDefinedProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(decodedConnectPacket.getUserDefinedProperties().get(2)));

		Assert.assertEquals(authMethod, decodedConnectPacket.getAuthMethod());
		Assert.assertArrayEquals(authData, decodedConnectPacket.getAuthData());
		
		Assert.assertEquals(willDelayInterval, decodedConnectPacket.getWillDelayInterval());
		Assert.assertEquals(willIsUTF8, decodedConnectPacket.isWillIsUTF8());
		Assert.assertEquals(willPublicationExpiryInterval, decodedConnectPacket.getWillPublicationExpiryInterval());
		Assert.assertEquals(willContentType, decodedConnectPacket.getWillContentType());
		Assert.assertEquals(willResponseTopic, decodedConnectPacket.getWillResponseTopic());
		Assert.assertEquals(willCorrelationData, decodedConnectPacket.getWillCorrelationData());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(decodedConnectPacket.getWillUserDefinedProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(decodedConnectPacket.getWillUserDefinedProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(decodedConnectPacket.getWillUserDefinedProperties().get(2)));

		}
	
	private MqttConnect generateConnectPacket(){
		MqttMessage willMessage = new MqttMessage(willPayload.getBytes());
		willMessage.setQos(willQoS);
		MqttConnect mqttConnectPacket = new MqttConnect(clientId,
														mqttVersion,
														cleanSession,
														keepAliveInterval);
		mqttConnectPacket.setUserName(userName);
		mqttConnectPacket.setPassword(password);
		mqttConnectPacket.setWillMessage(willMessage);
		mqttConnectPacket.setWillDestination(willDestination);
		mqttConnectPacket.setSessionExpiryInterval(sessionExpiryInterval);
		mqttConnectPacket.setMaximumPacketSize(maxPacketSize);
		mqttConnectPacket.setTopicAliasMaximum(topicAliasMax);
		mqttConnectPacket.setRequestReplyInfo(requestReplyInfo);
		mqttConnectPacket.setRequestProblemInfo(requestPropblemInfo);
		mqttConnectPacket.setAuthMethod(authMethod);
		mqttConnectPacket.setAuthData(authData);
		
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		mqttConnectPacket.setUserDefinedProperties(userDefinedProperties);
		
		mqttConnectPacket.setWillDelayInterval(willDelayInterval);
		mqttConnectPacket.setWillIsUTF8(willIsUTF8);
		mqttConnectPacket.setWillContentType(willContentType);
		mqttConnectPacket.setWillResponseTopic(willResponseTopic);
		mqttConnectPacket.setWillCorrelationData(willCorrelationData);
		mqttConnectPacket.setWillUserDefinedProperties(userDefinedProperties);
		
		
		return mqttConnectPacket;
	}
}
