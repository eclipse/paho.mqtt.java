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
import org.eclipse.paho.mqttv5.common.packet.MqttPubRec;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttPubRecTest {
	private static final int returnCode = MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR;
	private static final String reasonString = "Reason String 123.";
	
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
		
		Assert.assertEquals(returnCode, decodedPubRecPacket.getReturnCode());
		Assert.assertEquals(reasonString, decodedPubRecPacket.getReasonString());
		
		
	}
	
	public MqttPubRec generateMqttPubRecPacket() throws MqttException{
		MqttPubRec mqttPubRecPacket = new MqttPubRec(returnCode);
		mqttPubRecPacket.setReasonString(reasonString);
		
		return mqttPubRecPacket;
	}

}
