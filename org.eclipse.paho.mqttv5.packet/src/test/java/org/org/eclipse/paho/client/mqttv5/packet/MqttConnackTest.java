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
package org.org.eclipse.paho.client.mqttv5.packet;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.paho.mqttv5.packet.MqttConnack;
import org.eclipse.paho.mqttv5.packet.MqttWireMessage;
import org.eclipse.paho.mqttv5.util.MqttException;
import org.junit.Assert;
import org.junit.Test;

public class MqttConnackTest {
	private boolean sessionPresent = true;
	private int returnCode = MqttConnack.RETURN_CODE_SERVER_MOVED;
	
	private int receiveMaximum = 100;
	private boolean retainUnavailableAdvertisement = true;
	private String assignedClientIdentifier = "AssignedClientId";
	private int topicAliasMaximum = 100;
	private String reasonString = "Everything is fine.";
	private int serverKeepAlive = 60;
	private String replyInfo = "assignedTopicForSession";
	private String serverReference = "127.0.0.1";
	private String authMethod = "PASSWORD";
	private static final byte[] authData = "secretPassword123".getBytes();

	

	/**
	 * Tests that an MqttConnack packet can be encoded successfully
	 * without throwing any exceptions. 
	 * @throws MqttException 
	 */
	@Test
	public void testEncodingMqttConnack() throws MqttException {
		MqttConnack mqttConnackPacket = generateMqttConnackPacket();
		mqttConnackPacket.getHeader();
		mqttConnackPacket.getPayload();
	}
	
	
	/**
	 * Tests that an MqttConnack packet can be decoded
	 * successfully
	 */
	@Test
	public void testDecodingMqttConnack() throws IOException, MqttException {
		MqttConnack mqttConnackPacket = generateMqttConnackPacket();
		byte[] header = mqttConnackPacket.getHeader();
		byte[] payload = mqttConnackPacket.getPayload();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttConnack decodedConnackPacket = (MqttConnack) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(decodedConnackPacket.getSessionPresent(), sessionPresent);
		Assert.assertEquals(decodedConnackPacket.getReturnCode(), returnCode);
		Assert.assertEquals(decodedConnackPacket.getReceiveMaximum(), receiveMaximum);
		Assert.assertEquals(decodedConnackPacket.getRetainUnavailableAdvertisement(), retainUnavailableAdvertisement);
		Assert.assertEquals(decodedConnackPacket.getAssignedClientIdentifier(), assignedClientIdentifier);
		Assert.assertEquals(decodedConnackPacket.getTopicAliasMaximum(), topicAliasMaximum);
		Assert.assertEquals(decodedConnackPacket.getReasonString(), reasonString);
		Assert.assertEquals(decodedConnackPacket.getServerKeepAlive(), serverKeepAlive);
		Assert.assertEquals(decodedConnackPacket.getReplyInfo(), replyInfo);
		Assert.assertEquals(decodedConnackPacket.getServerReference(), serverReference);
		Assert.assertEquals(decodedConnackPacket.getAuthMethod(), authMethod);
		Assert.assertArrayEquals(decodedConnackPacket.getAuthData(), authData);
	}
	
	/**
	 * Tests that you cannot assign a server reference if the return code
	 * is not 0x9C or 0x9D
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testServerReferenceException(){
		MqttConnack mqttConnackPacket = new MqttConnack(sessionPresent, MqttConnack.RETURN_CODE_SUCCESS);
		mqttConnackPacket.setServerReference(serverReference);
	}
	
	
	private MqttConnack generateMqttConnackPacket(){
		MqttConnack mqttConnackPacket = new MqttConnack(sessionPresent, returnCode);
		mqttConnackPacket.setReceiveMaximum(100);
		
		mqttConnackPacket.setReceiveMaximum(receiveMaximum);
		mqttConnackPacket.setRetainUnavailableAdvertisement(retainUnavailableAdvertisement);
		mqttConnackPacket.setAssignedClientIdentifier(assignedClientIdentifier);
		mqttConnackPacket.setTopicAliasMaximum(topicAliasMaximum);
		mqttConnackPacket.setReasonString(reasonString);
		mqttConnackPacket.setServerKeepAlive(serverKeepAlive);
		mqttConnackPacket.setReplyInfo(replyInfo);
		mqttConnackPacket.setServerReference(serverReference);
		mqttConnackPacket.setAuthMethod(authMethod);
		mqttConnackPacket.setAuthData(authData);
		
		return mqttConnackPacket;
	}

}
