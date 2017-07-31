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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttAuth;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttAuthTest {
	private static final int returnCode = MqttReturnCode.RETURN_CODE_CONTINUE_AUTHENTICATION;
	private static final String authMethod = "AuthMethodString";
	private static final byte[] authData = "AuthData".getBytes();
	private static final String reasonString = "Reason String 123.";
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
	
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
		Assert.assertEquals(reasonString, decodedAuthPacket.getReasonString());
		Assert.assertEquals(3, decodedAuthPacket.getUserDefinedPairs().size());
		Assert.assertEquals(userValue1, decodedAuthPacket.getUserDefinedPairs().get(userKey1));
		Assert.assertEquals(userValue2, decodedAuthPacket.getUserDefinedPairs().get(userKey2));
		Assert.assertEquals(userValue3, decodedAuthPacket.getUserDefinedPairs().get(userKey3));
		
		
	}
	
	public MqttAuth generateMqttAuthPacket() throws MqttException{
		MqttAuth mqttAuthPacket = new MqttAuth(returnCode);
		mqttAuthPacket.setAuthMethod(authMethod);
		mqttAuthPacket.setAuthData(authData);
		mqttAuthPacket.setReasonString(reasonString);
		Map<String, String> userDefinedPairs = new HashMap<String,String>();
		userDefinedPairs.put(userKey1, userValue1);
		userDefinedPairs.put(userKey2, userValue2);
		userDefinedPairs.put(userKey3, userValue3);
		mqttAuthPacket.setUserDefinedPairs(userDefinedPairs);
		
		return mqttAuthPacket;
	}

}
