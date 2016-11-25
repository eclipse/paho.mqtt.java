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
import org.eclipse.paho.mqttv5.common.packet.MqttPingReq;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Test;

public class MqttPingReqTest {
	


	@Test
	public void testEncodingMqttPingReq() throws MqttException {
		MqttPingReq mqttPingReqPacket = new MqttPingReq();
		mqttPingReqPacket.getHeader();
		mqttPingReqPacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttPingReq() throws MqttException, IOException {
		MqttPingReq mqttPingReqPacket = new MqttPingReq();
		byte[] header = mqttPingReqPacket.getHeader();
		byte[] payload = mqttPingReqPacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttWireMessage.createWireMessage(outputStream.toByteArray());
	}
	
	

}
