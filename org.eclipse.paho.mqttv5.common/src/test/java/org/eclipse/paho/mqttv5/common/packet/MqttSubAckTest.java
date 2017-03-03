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
		Assert.assertArrayEquals(returnCodes, decodedSubAckPacket.getReturnCodes());
		
		
	}
	
	private MqttSubAck generatemqttSubAckPacket(){
		MqttSubAck mqttSubAckPacket = new MqttSubAck(returnCodes);
		mqttSubAckPacket.setReasonString(reasonString);
		
		return mqttSubAckPacket;
	}

}
