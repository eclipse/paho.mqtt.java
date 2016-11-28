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

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttConnAckTest {
	
	private static final boolean sessionPresent = true;
	private static final int returnCode = MqttConnAck.RETURN_CODE_SERVER_MOVED;
	private static final int receiveMaximum = 100;
	private static final boolean retainUnavailableAdvertisement = true;
	private static final String assignedClientIdentifier = "AssignedClientId";
	private static final int topicAliasMaximum = 100;
	private static final String reasonString = "Everything is fine.";
	private static final int serverKeepAlive = 60;
	private static final String replyInfo = "assignedTopicForSession";
	private static final String serverReference = "127.0.0.1";
	private static final String authMethod = "PASSWORD";
	private static final byte[] authData = "secretPassword123".getBytes();

	

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
		
		Assert.assertEquals(sessionPresent, decodedConnAckPacket.getSessionPresent());
		Assert.assertEquals(returnCode, decodedConnAckPacket.getReturnCode());
		Assert.assertEquals(receiveMaximum, decodedConnAckPacket.getReceiveMaximum());
		Assert.assertEquals(retainUnavailableAdvertisement, decodedConnAckPacket.getRetainUnavailableAdvertisement());
		Assert.assertEquals(assignedClientIdentifier, decodedConnAckPacket.getAssignedClientIdentifier());
		Assert.assertEquals(topicAliasMaximum, decodedConnAckPacket.getTopicAliasMaximum());
		Assert.assertEquals(reasonString, decodedConnAckPacket.getReasonString());
		Assert.assertEquals(serverKeepAlive, decodedConnAckPacket.getServerKeepAlive());
		Assert.assertEquals(replyInfo, decodedConnAckPacket.getReplyInfo());
		Assert.assertEquals(serverReference, decodedConnAckPacket.getServerReference());
		Assert.assertEquals(authMethod, decodedConnAckPacket.getAuthMethod());
		Assert.assertArrayEquals(authData, decodedConnAckPacket.getAuthData());
	}
	
	/**
	 * Tests that you cannot assign a server reference if the return code
	 * is not 0x9C or 0x9D
	 * @throws MqttException 
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testServerReferenceException() throws MqttException{
		MqttConnAck mqttConnAckPacket = new MqttConnAck(sessionPresent, MqttConnAck.RETURN_CODE_SUCCESS);
		mqttConnAckPacket.setServerReference(serverReference);
	}
	
	
	private MqttConnAck generateMqttConnAckPacket() throws MqttException{
		MqttConnAck mqttConnAckPacket = new MqttConnAck(sessionPresent, returnCode);
		mqttConnAckPacket.setReceiveMaximum(100);
		
		mqttConnAckPacket.setReceiveMaximum(receiveMaximum);
		mqttConnAckPacket.setRetainUnavailableAdvertisement(retainUnavailableAdvertisement);
		mqttConnAckPacket.setAssignedClientIdentifier(assignedClientIdentifier);
		mqttConnAckPacket.setTopicAliasMaximum(topicAliasMaximum);
		mqttConnAckPacket.setReasonString(reasonString);
		mqttConnAckPacket.setServerKeepAlive(serverKeepAlive);
		mqttConnAckPacket.setReplyInfo(replyInfo);
		mqttConnAckPacket.setServerReference(serverReference);
		mqttConnAckPacket.setAuthMethod(authMethod);
		mqttConnAckPacket.setAuthData(authData);
		
		return mqttConnAckPacket;
	}

}
