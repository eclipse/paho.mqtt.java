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

public class MqttUnsubAckTest {

	private static final int[] returnCodes = { MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_NO_SUBSCRIPTION_EXISTED, MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR,
			MqttReturnCode.RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR, MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED,
			MqttReturnCode.RETURN_CODE_TOPIC_FILTER_NOT_VALID, MqttReturnCode.RETURN_CODE_PACKET_ID_IN_USE };

	private static final String reasonString = "Reason String 123.";
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";


	@Test
	public void testEncodingMqttUnsubAck() throws MqttException {
		MqttUnsubAck mqttUnsubAckPacket = generateMqttUnsubAckPacket();
		mqttUnsubAckPacket.getHeader();
		mqttUnsubAckPacket.getPayload();
	}

	@Test
	public void testDecodingMqttUnsubAck() throws MqttException, IOException {
		MqttUnsubAck mqttUnsubAckPacket = generateMqttUnsubAckPacket();

		byte[] header = mqttUnsubAckPacket.getHeader();
		byte[] payload = mqttUnsubAckPacket.getPayload();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);

		MqttUnsubAck decodedUnsubAckPacket = (MqttUnsubAck) MqttWireMessage
				.createWireMessage(outputStream.toByteArray());
		MqttProperties properties = decodedUnsubAckPacket.getProperties();

		Assert.assertEquals(reasonString, properties.getReasonString());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(properties.getUserProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(properties.getUserProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(properties.getUserProperties().get(2)));

		Assert.assertArrayEquals(returnCodes, decodedUnsubAckPacket.getReturnCodes());

	}

	private MqttUnsubAck generateMqttUnsubAckPacket() throws MqttException {
		MqttProperties properties = new MqttProperties();
		properties.setReasonString(reasonString);
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		properties.setUserProperties(userDefinedProperties);
		MqttUnsubAck mqttUnsubAckPacket = new MqttUnsubAck(returnCodes, properties);

		return mqttUnsubAckPacket;
	}

}
