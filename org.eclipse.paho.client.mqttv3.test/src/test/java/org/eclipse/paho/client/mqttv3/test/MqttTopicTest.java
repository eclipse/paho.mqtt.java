/** Copyright (c)  2014 IBM Corp.
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
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.eclipse.paho.common.test.categories.MQTTV3Test;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests mqtt topic wildcards
 */
@Category(MQTTV3Test.class)
public class MqttTopicTest {
	private static final Logger log = Logger.getLogger(MqttTopicTest.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
	}

	@Test
	public void testValidTopicFilterWildcards() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
		String[] topics = new String[] { "+", "+/+", "+/foo", "+/tennis/#", "foo/+", "foo/+/bar", "/+",
				"/+/sport/+/player1", "#", "/#", "sport/#", "sport/tennis/#" };

		for (String topic : topics) {
			MqttTopic.validate(topic, true);
		}
	}

	@Test
	public void testMatchedTopicFilterWildcards() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
		String[][] matchingTopics = new String[][] { { "sport/tennis/player1/#", "sport/tennis/player1" },
				{ "sport/tennis/player1/#", "sport/tennis/player1/ranking" },
				{ "sport/tennis/player1/#", "sport/tennis/player1/score/wimbledon" }, { "sport/#", "sport" },
				{ "#", "sport/tennis/player1" } };

		for (String[] pair : matchingTopics) {
			Assert.assertTrue(pair[0] + " should match " + pair[1], MqttTopic.isMatched(pair[0], pair[1]));
		}
	}

	@Test
	public void testNonMatchedTopicFilterWildcards() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
		String[][] matchingTopics = new String[][] { { "sport/tennis/player1/#", "sport/tennis/player2" },
				{ "sport1/#", "sport2" }, { "sport/tennis1/player/#", "sport/tennis2/player" } };

		for (String[] pair : matchingTopics) {
			Assert.assertFalse(pair[0] + " should NOT match " + pair[1], MqttTopic.isMatched(pair[0], pair[1]));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards1() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
		MqttTopic.validate("sport/tennis#", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards2() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
		MqttTopic.validate("sport/tennis/#/ranking", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards3() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
		MqttTopic.validate("sport+", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards4() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
		MqttTopic.validate("sport/+aa", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards5() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, MqttTopicTest.class, methodName);
		MqttTopic.validate("sport/#/ball/+/aa", true);
	}

}
