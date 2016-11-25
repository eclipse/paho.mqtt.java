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
package org.org.eclipse.paho.client.mqttv5.packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.paho.mqttv5.packet.MqttSubAck;
import org.eclipse.paho.mqttv5.packet.MqttWireMessage;
import org.eclipse.paho.mqttv5.util.MqttException;
import org.junit.Assert;
import org.junit.Test;

public class MqttSubAckTest {
	private static final int[] returnCodes = {
		MqttSubAck.RETURN_CODE_MAX_QOS_0,
		MqttSubAck.RETURN_CODE_MAX_QOS_1,
		MqttSubAck.RETURN_CODE_MAX_QOS_2,
		MqttSubAck.RETURN_CODE_UNSPECIFIED_ERROR,
		MqttSubAck.RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR,
		MqttSubAck.RETURN_CODE_NOT_AUTHORIZED,
		MqttSubAck.RETURN_CODE_TOPIC_FILTER_NOT_VALID,
		MqttSubAck.RETURN_CODE_PACKET_ID_IN_USE,
		MqttSubAck.RETURN_CODE_SHARED_SUB_NOT_SUPPORTED	};
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
