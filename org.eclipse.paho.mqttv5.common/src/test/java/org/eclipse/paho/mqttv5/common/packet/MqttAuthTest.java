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

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttAuth;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttAuthTest {
	private static final int returnCode = MqttAuth.RETURN_CODE_CONTINUE_AUTHENTICATION;
	private static final String authMethod = "AuthMethodString";
	private static final byte[] authData = "AuthData".getBytes();
	
	@Test
	public void testEncodingMqttAuth() throws MqttException {
		MqttAuth mqttAuthPacket = generateMqttAuthPacket();
		mqttAuthPacket.getHeader();
		mqttAuthPacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttAuth() throws MqttException, IOException {
		MqttAuth mqttAuthPacket = generateMqttAuthPacket();
		byte[] header = mqttAuthPacket.getHeader();
		byte[] payload = mqttAuthPacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttAuth decodedAuthPacket = (MqttAuth) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(returnCode, decodedAuthPacket.getReturnCode());
		Assert.assertEquals(authMethod, decodedAuthPacket.getAuthMethod());
		Assert.assertArrayEquals(authData, decodedAuthPacket.getAuthData());
		
		
	}
	
	public MqttAuth generateMqttAuthPacket() throws MqttException{
		MqttAuth mqttAuthPacket = new MqttAuth(returnCode);
		mqttAuthPacket.setAuthMethod(authMethod);
		mqttAuthPacket.setAuthData(authData);
		
		return mqttAuthPacket;
	}

}
