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
import org.eclipse.paho.mqttv5.common.packet.MqttSubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttSubAckTest {
	private static final int[] returnCodes = {
		MqttReturnCode.RETURN_CODE_MAX_QOS_0,
		MqttReturnCode.RETURN_CODE_MAX_QOS_1,
		MqttReturnCode.RETURN_CODE_MAX_QOS_2,
		MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR,
		MqttReturnCode.RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR,
		MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED,
		MqttReturnCode.RETURN_CODE_TOPIC_FILTER_NOT_VALID,
		MqttReturnCode.RETURN_CODE_PACKET_ID_IN_USE,
		MqttReturnCode.RETURN_CODE_SHARED_SUB_NOT_SUPPORTED	};
	
	private static final String reasonString = "Reason String 123.";
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
	
	@Test
	public void testEncodingMqttSubAck() throws MqttException {
		MqttSubAck mqttSubAckPacket = generatemqttSubAckPacket();
		mqttSubAckPacket.getHeader();
		mqttSubAckPacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttSubAck() throws MqttException, IOException {
		MqttSubAck mqttSubAckPacket = generatemqttSubAckPacket();
		byte[] header = mqttSubAckPacket.getHeader();
		byte[] payload = mqttSubAckPacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttSubAck decodedSubAckPacket = (MqttSubAck) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(reasonString, decodedSubAckPacket.getReasonString());
		Assert.assertEquals(3, decodedSubAckPacket.getUserDefinedPairs().size());
		Assert.assertEquals(userValue1, decodedSubAckPacket.getUserDefinedPairs().get(userKey1));
		Assert.assertEquals(userValue2, decodedSubAckPacket.getUserDefinedPairs().get(userKey2));
		Assert.assertEquals(userValue3, decodedSubAckPacket.getUserDefinedPairs().get(userKey3));
		Assert.assertArrayEquals(returnCodes, decodedSubAckPacket.getReturnCodes());
		
		
	}
	
	private MqttSubAck generatemqttSubAckPacket() throws MqttException{
		MqttSubAck mqttSubAckPacket = new MqttSubAck(returnCodes);
		mqttSubAckPacket.setReasonString(reasonString);
		Map<String, String> userDefinedPairs = new HashMap<String,String>();
		userDefinedPairs.put(userKey1, userValue1);
		userDefinedPairs.put(userKey2, userValue2);
		userDefinedPairs.put(userKey3, userValue3);
		mqttSubAckPacket.setUserDefinedPairs(userDefinedPairs);
		
		return mqttSubAckPacket;
	}

}
