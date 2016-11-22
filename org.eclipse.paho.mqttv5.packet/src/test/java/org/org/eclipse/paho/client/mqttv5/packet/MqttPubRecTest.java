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

import org.eclipse.paho.mqttv5.packet.MqttPubRec;
import org.eclipse.paho.mqttv5.packet.MqttWireMessage;
import org.eclipse.paho.mqttv5.util.MqttException;
import org.junit.Assert;
import org.junit.Test;

public class MqttPubRecTest {
	private static final int returnCode = MqttPubRec.RETURN_CODE_UNSPECIFIED_ERROR;
	private static final String reasonString = "Reason String 123.";
	
	@Test
	public void testEncodingMqttPubrec() throws MqttException {
		MqttPubRec mqttPubrecPacket = generateMqttPubrecPacket();
		mqttPubrecPacket.getHeader();
		mqttPubrecPacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttPubrec() throws MqttException, IOException {
		MqttPubRec mqttPubrecPacket = generateMqttPubrecPacket();
		byte[] header = mqttPubrecPacket.getHeader();
		byte[] payload = mqttPubrecPacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttPubRec decodedPubrecPacket = (MqttPubRec) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(decodedPubrecPacket.getReturnCode(), returnCode);
		Assert.assertEquals(decodedPubrecPacket.getReasonString(), reasonString);
		
		
	}
	
	public MqttPubRec generateMqttPubrecPacket(){
		MqttPubRec mqttPubrecPacket = new MqttPubRec(returnCode);
		mqttPubrecPacket.setReasonString(reasonString);
		
		return mqttPubrecPacket;
	}

}
