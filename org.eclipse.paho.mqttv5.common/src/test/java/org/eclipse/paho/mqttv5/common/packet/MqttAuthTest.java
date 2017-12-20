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

import org.eclipse.paho.mqttv5.common.MqttException;
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
		MqttProperties properties = decodedAuthPacket.getProperties();
		
		Assert.assertEquals(returnCode, decodedAuthPacket.getReturnCode());
		Assert.assertEquals(authMethod, properties.getAuthenticationMethod());
		Assert.assertArrayEquals(authData, properties.getAuthenticationData());
		Assert.assertEquals(reasonString, properties.getReasonString());
		Assert.assertEquals(3, properties.getUserProperties().size());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(properties.getUserProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(properties.getUserProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(properties.getUserProperties().get(2)));

	}
	
	public MqttAuth generateMqttAuthPacket() throws MqttException{
		
		
		MqttProperties properties = new MqttProperties();
		properties.setAuthenticationMethod(authMethod);
		properties.setAuthenticationData(authData);
		properties.setReasonString(reasonString);
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		properties.setUserProperties(userDefinedProperties);
		MqttAuth mqttAuthPacket = new MqttAuth(returnCode, properties);
		return mqttAuthPacket;
	}

}
