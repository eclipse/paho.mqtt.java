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
 * 	  Dave Locke - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Some tests to validate that each type of property can be set, encoded and
 * decoded correctly.
 *
 */
public class MqttPropertiesTest {

	private static final long FOUR_BYTE_INT_MIN = 0L;
	private static final long FOUR_BYTE_INT_MAX = 4294967295L;

	private static final int TWO_BYTE_INT_MIN = 0;
	private static final int TWO_BYTE_INT_MAX = 65535;

	private static final int VARIABLE_BYTE_INT_MAX = 268435455;

	/**
	 * Tests that a Two Byte Integer Property can be encoded and decoded correctly.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test
	public void testTwoByteIntPropertyValid() throws MqttException, IOException {
		// Test Min
		MqttProperties inputProps = new MqttProperties(new Byte[] { MqttProperties.SERVER_KEEP_ALIVE_IDENTIFIER });
		inputProps.setServerKeepAlive(TWO_BYTE_INT_MIN);
		byte[] encodedProperties = inputProps.encodeProperties();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encodedProperties));

		MqttProperties outputProps = new MqttProperties(new Byte[] { MqttProperties.SERVER_KEEP_ALIVE_IDENTIFIER });

		outputProps.decodeProperties(dis);
		Assert.assertEquals(TWO_BYTE_INT_MIN, outputProps.getServerKeepAlive().intValue());

		// Test Max
		inputProps = new MqttProperties(new Byte[] { MqttProperties.SERVER_KEEP_ALIVE_IDENTIFIER });
		inputProps.setServerKeepAlive(TWO_BYTE_INT_MAX);
		encodedProperties = inputProps.encodeProperties();
		dis = new DataInputStream(new ByteArrayInputStream(encodedProperties));
		// Useful for debugging.
		// System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(encodedProperties));

		outputProps = new MqttProperties(new Byte[] { MqttProperties.SERVER_KEEP_ALIVE_IDENTIFIER });

		outputProps.decodeProperties(dis);
		Assert.assertEquals(TWO_BYTE_INT_MAX, outputProps.getServerKeepAlive().intValue());
	}

	/**
	 * Tests that a Two Byte Integer cannot be set to a negative number.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testTwoByteIntPropertyInvalidNegative() throws MqttException, IOException {
		MqttProperties inputProps = new MqttProperties(new Byte[] { MqttProperties.SERVER_KEEP_ALIVE_IDENTIFIER });
		inputProps.setServerKeepAlive(-1);
	}

	/**
	 * Tests that a Two Byte Integer cannot be set to a number greater than 65535.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testTwoByteIntPropertyInvalidMax() throws MqttException, IOException {
		MqttProperties inputProps = new MqttProperties(new Byte[] { MqttProperties.SERVER_KEEP_ALIVE_IDENTIFIER });
		inputProps.setServerKeepAlive(TWO_BYTE_INT_MAX + 1);
	}

	/**
	 * Tests that a Four Byte Integer Property can be encoded and Decoded correctly.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test
	public void testFourByteIntPropertyValid() throws MqttException, IOException {
		// Test Min
		MqttProperties inputProps = new MqttProperties(
				new Byte[] { MqttProperties.MESSAGE_EXPIRY_INTERVAL_IDENTIFIER });
		inputProps.setMessageExpiryInterval(FOUR_BYTE_INT_MIN);
		byte[] encodedProperties = inputProps.encodeProperties();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encodedProperties));

		MqttProperties outputProps = new MqttProperties(
				new Byte[] { MqttProperties.MESSAGE_EXPIRY_INTERVAL_IDENTIFIER });

		outputProps.decodeProperties(dis);
		Assert.assertEquals(FOUR_BYTE_INT_MIN, outputProps.getMessageExpiryInterval().longValue());

		// Test Max
		inputProps = new MqttProperties(new Byte[] { MqttProperties.MESSAGE_EXPIRY_INTERVAL_IDENTIFIER });
		inputProps.setMessageExpiryInterval(FOUR_BYTE_INT_MAX);
		encodedProperties = inputProps.encodeProperties();
		dis = new DataInputStream(new ByteArrayInputStream(encodedProperties));

		outputProps = new MqttProperties(new Byte[] { MqttProperties.MESSAGE_EXPIRY_INTERVAL_IDENTIFIER });

		outputProps.decodeProperties(dis);
		Assert.assertEquals(FOUR_BYTE_INT_MAX, outputProps.getMessageExpiryInterval().longValue());
	}

	/**
	 * Tests that a Four Byte Integer cannot be set to a negative number.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFourByteIntPropertyInvalidNegative() throws MqttException, IOException {
		MqttProperties inputProps = new MqttProperties(
				new Byte[] { MqttProperties.MESSAGE_EXPIRY_INTERVAL_IDENTIFIER });
		inputProps.setMessageExpiryInterval(-1L);
	}

	/**
	 * Tests that a Four Byte Integer cannot be set to a number greater than
	 * 4294967295.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFourByteIntPropertyInvalidMax() throws MqttException, IOException {
		MqttProperties inputProps = new MqttProperties(
				new Byte[] { MqttProperties.MESSAGE_EXPIRY_INTERVAL_IDENTIFIER });
		inputProps.setMessageExpiryInterval(FOUR_BYTE_INT_MAX + 1L);
	}

	/**
	 * Tests that a Variable Byte Integer Property can be encoded and Decoded
	 * correctly.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test
	public void testVariableByteIntPropertyValid() throws MqttException, IOException {
		// Test Min
		MqttProperties inputProps = new MqttProperties(
				new Byte[] { MqttProperties.SUBSCRIPTION_IDENTIFIER, MqttProperties.SUBSCRIPTION_IDENTIFIER_SINGLE });
		inputProps.setSubscriptionIdentifier(0);
		byte[] encodedProperties = inputProps.encodeProperties();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encodedProperties));
		MqttProperties outputProps = new MqttProperties(
				new Byte[] { MqttProperties.SUBSCRIPTION_IDENTIFIER, MqttProperties.SUBSCRIPTION_IDENTIFIER_SINGLE });
		outputProps.decodeProperties(dis);
		Assert.assertEquals(0, outputProps.getSubscriptionIdentifier().intValue());

		// Test Max
		inputProps = new MqttProperties(
				new Byte[] { MqttProperties.SUBSCRIPTION_IDENTIFIER, MqttProperties.SUBSCRIPTION_IDENTIFIER_SINGLE });
		inputProps.setSubscriptionIdentifier(VARIABLE_BYTE_INT_MAX);
		encodedProperties = inputProps.encodeProperties();
		dis = new DataInputStream(new ByteArrayInputStream(encodedProperties));
		outputProps = new MqttProperties(
				new Byte[] { MqttProperties.SUBSCRIPTION_IDENTIFIER, MqttProperties.SUBSCRIPTION_IDENTIFIER_SINGLE });
		outputProps.decodeProperties(dis);
		Assert.assertEquals(VARIABLE_BYTE_INT_MAX, outputProps.getSubscriptionIdentifier().intValue());
	}

	/**
	 * Tests that a Variable Byte Integer cannot be set to a negative number.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testVariableByteIntPropertyInvalidNegative() throws MqttException, IOException {
		MqttProperties inputProps = new MqttProperties(new Byte[] { MqttProperties.SUBSCRIPTION_IDENTIFIER_SINGLE });
		List<Integer> subscriptionIdentifiers = new ArrayList<Integer>();
		subscriptionIdentifiers.add(-1);
		inputProps.setSubscriptionIdentifiers(subscriptionIdentifiers);

	}

	/**
	 * Tests that a Variable Byte Integer cannot be set to a number greater than
	 * 268435455.
	 * 
	 * @throws MqttException
	 * @throws IOException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testVariableByteIntPropertyInvalidMax() throws MqttException, IOException {
		MqttProperties inputProps = new MqttProperties(new Byte[] { MqttProperties.SUBSCRIPTION_IDENTIFIER_SINGLE });
		List<Integer> subscriptionIdentifiers = new ArrayList<Integer>();
		subscriptionIdentifiers.add(VARIABLE_BYTE_INT_MAX + 1);
		inputProps.setSubscriptionIdentifiers(subscriptionIdentifiers);

	}

}
