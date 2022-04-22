/** Copyright (c)  2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 *******************************************************************************/

package org.eclipse.paho.mqttv5.common.utils;

import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests MQTT topic wildcards
 */
public class MqttTopicTest {


	@Test
	public void testValidTopicFilterWildcards() throws Exception {
		String[] topics = new String[] { "+", "+/+", "+/foo", "+/tennis/#", "foo/+", "foo/+/bar", "/+",
				"/+/sport/+/player1", "#", "/#", "sport/#", "sport/tennis/#" };

		for (String topic : topics) {
			MqttTopicValidator.validate(topic, true, true);
		}
	}

	@Test
	public void testMatchedTopicFilterWildcards() throws Exception {
		String[][] matchingTopics = new String[][] { { "sport/tennis/player1/#", "sport/tennis/player1" },
				{ "sport/tennis/player1/#", "sport/tennis/player1/ranking" },
				{ "sport/tennis/player1/#", "sport/tennis/player1/score/wimbledon" }, { "sport/#", "sport" },
				{ "#", "sport/tennis/player1" } , {"sport/+/player1/ranking/#","sport/tennis/player1/ranking"} };

		for (String[] pair : matchingTopics) {
			Assert.assertTrue(pair[0] + " should match " + pair[1], MqttTopicValidator.isMatched(pair[0], pair[1]));
		}
	}

	@Test
	public void testNonMatchedTopicFilterWildcards() throws Exception {
		String[][] matchingTopics = new String[][] { { "sport/tennis/player1/#", "sport/tennis/player2" },
				{ "sport1/#", "sport2" }, { "sport/tennis1/player/#", "sport/tennis2/player" } };

		for (String[] pair : matchingTopics) {
			Assert.assertFalse(pair[0] + " should NOT match " + pair[1], MqttTopicValidator.isMatched(pair[0], pair[1]));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards1() throws Exception {
		MqttTopicValidator.validate("sport/tennis#", true, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards2() throws Exception {
		MqttTopicValidator.validate("sport/tennis/#/ranking", true, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards3() throws Exception {
		MqttTopicValidator.validate("sport+", true, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards4() throws Exception {
		MqttTopicValidator.validate("sport/+aa", true, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTopicFilterWildcards5() throws Exception {
		MqttTopicValidator.validate("sport/#/ball/+/aa", true, true);
	}
	
	@Test
	public void testValidG11NTopic() {
		MqttTopicValidator.validate("$shared/葛渚噓/GVTDurTopic02/葛渚噓", true, true);
	}

}
