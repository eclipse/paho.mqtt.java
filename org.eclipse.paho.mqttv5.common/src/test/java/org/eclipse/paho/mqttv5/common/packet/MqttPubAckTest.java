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
import org.eclipse.paho.mqttv5.common.packet.MqttPubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttPubAckTest {
	private static final int returnCode = MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR;
	private static final String reasonString = "Reason String 123.";
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
	
	@Test
	public void testEncodingMqttPuback() throws MqttException {
		MqttPubAck mqttPubackPacket = generateMqttPubackPacket();
		mqttPubackPacket.getHeader();
		mqttPubackPacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttPuback() throws MqttException, IOException {
		MqttPubAck mqttPubackPacket = generateMqttPubackPacket();
		byte[] header = mqttPubackPacket.getHeader();
		byte[] payload = mqttPubackPacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttPubAck decodedPubackPacket = (MqttPubAck) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(returnCode, decodedPubackPacket.getReturnCode());
		Assert.assertEquals(reasonString, decodedPubackPacket.getReasonString());
		Assert.assertEquals(3, decodedPubackPacket.getUserDefinedPairs().size());
		Assert.assertEquals(userValue1, decodedPubackPacket.getUserDefinedPairs().get(userKey1));
		Assert.assertEquals(userValue2, decodedPubackPacket.getUserDefinedPairs().get(userKey2));
		Assert.assertEquals(userValue3, decodedPubackPacket.getUserDefinedPairs().get(userKey3));
		
		
	}
	
	public MqttPubAck generateMqttPubackPacket() throws MqttException{
		MqttPubAck mqttPubackPacket = new MqttPubAck(returnCode,1);
		mqttPubackPacket.setReasonString(reasonString);
		Map<String, String> userDefinedPairs = new HashMap<String,String>();
		userDefinedPairs.put(userKey1, userValue1);
		userDefinedPairs.put(userKey2, userValue2);
		userDefinedPairs.put(userKey3, userValue3);
		mqttPubackPacket.setUserDefinedPairs(userDefinedPairs);
		
		return mqttPubackPacket;
	}

}
