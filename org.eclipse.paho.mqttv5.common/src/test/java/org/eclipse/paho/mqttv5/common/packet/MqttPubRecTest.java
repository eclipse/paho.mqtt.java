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

public class MqttPubRecTest {
	private static final int returnCode = MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR;
	private static final String reasonString = "Reason String 123.";
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
	
	@Test
	public void testEncodingMqttPubRec() throws MqttException {
		MqttPubRec mqttPubRecPacket = generateMqttPubRecPacket();
		mqttPubRecPacket.getHeader();
		mqttPubRecPacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttPubRec() throws MqttException, IOException {
		MqttPubRec mqttPubRecPacket = generateMqttPubRecPacket();
		byte[] header = mqttPubRecPacket.getHeader();
		byte[] payload = mqttPubRecPacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttPubRec decodedPubRecPacket = (MqttPubRec) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		MqttProperties properties = decodedPubRecPacket.getProperties();
		
		Assert.assertEquals(returnCode, decodedPubRecPacket.getReturnCode());
		Assert.assertEquals(reasonString, properties.getReasonString());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(properties.getUserProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(properties.getUserProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(properties.getUserProperties().get(2)));

		
	}
	
	public MqttPubRec generateMqttPubRecPacket() throws MqttException{
		MqttProperties properties = new MqttProperties();
		
		properties.setReasonString(reasonString);
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		properties.setUserProperties(userDefinedProperties);
		MqttPubRec mqttPubRecPacket = new MqttPubRec(returnCode, 1, properties);
		
		return mqttPubRecPacket;
	}

}
