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

public class MqttDisconnectTest {
	private static final int returnCode = MqttReturnCode.RETURN_CODE_PROTOCOL_ERROR;
	private static final String reasonString = "Reason String 123.";
	private static final int sessionExpiryInterval = 60;
	private static final String serverReference = "127.0.0.1";
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
	
	@Test
	public void testEncodingMqttDisconnect() throws MqttException {
		MqttDisconnect mqttDisconnectPacket = generatemqttDisconnectPacket();
		mqttDisconnectPacket.getHeader();
		mqttDisconnectPacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttDisconnect() throws MqttException, IOException {
		MqttDisconnect mqttDisconnectPacket = generatemqttDisconnectPacket();
		byte[] header = mqttDisconnectPacket.getHeader();
		byte[] payload = mqttDisconnectPacket.getPayload();
		
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		//printByteArray(outputStream.toByteArray());
		
		MqttDisconnect decodedDisconnectPacket = (MqttDisconnect) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(returnCode, decodedDisconnectPacket.getReturnCode());
		Assert.assertEquals(reasonString, decodedDisconnectPacket.getReasonString());
		Assert.assertEquals(sessionExpiryInterval, decodedDisconnectPacket.getSessionExpiryInterval());
		Assert.assertEquals(serverReference, decodedDisconnectPacket.getServerReference());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(decodedDisconnectPacket.getUserDefinedProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(decodedDisconnectPacket.getUserDefinedProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(decodedDisconnectPacket.getUserDefinedProperties().get(2)));

		
	}
	
	@Test
	public void testDecodingMqttDisconnectWithNoProperties() throws MqttException, IOException {
		MqttDisconnect mqttDisconnectPacket = new MqttDisconnect(returnCode);
		byte[] header = mqttDisconnectPacket.getHeader();
		byte[] payload = mqttDisconnectPacket.getPayload();
		
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		//printByteArray(outputStream.toByteArray());
		
		MqttDisconnect decodedDisconnectPacket = (MqttDisconnect) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		
		Assert.assertEquals(returnCode, decodedDisconnectPacket.getReturnCode());
		

		
	}
	
	private MqttDisconnect generatemqttDisconnectPacket() throws MqttException{
		MqttDisconnect mqttDisconnectPacket = new MqttDisconnect(returnCode);
		mqttDisconnectPacket.setReasonString(reasonString);
		mqttDisconnectPacket.setSessionExpiryInterval(sessionExpiryInterval);
		mqttDisconnectPacket.setServerReference(serverReference);
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		mqttDisconnectPacket.setUserDefinedProperties(userDefinedProperties);
		return mqttDisconnectPacket;
	}
	


}
