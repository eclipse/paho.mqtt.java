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
import org.eclipse.paho.mqttv5.common.packet.MqttUnsubscribe;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttUnsubscribeTest {
	private static final String[] topics = { "a/b","c/d",	"e/f",	"g/g"};
	private static final String reasonString = "Reason String 123.";
	
	@Test
	public void testEncodingMqttUnsubscribe() throws MqttException {
		MqttUnsubscribe MqttUnsubscribePacket = generateMqttUnsubscribePacket();
		MqttUnsubscribePacket.getHeader();
		MqttUnsubscribePacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttUnsubscribe() throws MqttException, IOException {
		MqttUnsubscribe MqttUnsubscribePacket = generateMqttUnsubscribePacket();
		byte[] header = MqttUnsubscribePacket.getHeader();
		byte[] payload = MqttUnsubscribePacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttUnsubscribe decodedUnsubscribePacket = (MqttUnsubscribe) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(reasonString, decodedUnsubscribePacket.getReasonString());
		Assert.assertArrayEquals(topics, decodedUnsubscribePacket.getTopics());
		
		
	}
	
	public MqttUnsubscribe generateMqttUnsubscribePacket(){
		MqttUnsubscribe MqttUnsubscribePacket = new MqttUnsubscribe(topics);
		MqttUnsubscribePacket.setReasonString(reasonString);
		
		return MqttUnsubscribePacket;
	}

}
