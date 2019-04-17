package org.eclipse.paho.client.mqttv3.test;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1_1;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_DEFAULT;
import static org.junit.Assert.*;

import org.eclipse.paho.common.test.categories.MQTTV3Test;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(MQTTV3Test.class)
public class MqttConnectOptionsTest {

	@Test
	public void testValidateMQTTVersions() {
		MqttConnectOptions connectOptions = new MqttConnectOptions();

		connectOptions.setMqttVersion(MQTT_VERSION_DEFAULT);
		connectOptions.setMqttVersion(MQTT_VERSION_3_1);
		connectOptions.setMqttVersion(MQTT_VERSION_3_1_1);
	}

	@Test
	public void testInvalidMQTTVersions() {
		MqttConnectOptions connectOptions = new MqttConnectOptions();

		try {
			connectOptions.setMqttVersion(9);
			fail("MQTT Version is not valid");
		} catch (IllegalArgumentException e) {
			assertEquals("An incorrect version was used \"9\". Acceptable version options are " + MQTT_VERSION_DEFAULT + ", " + MQTT_VERSION_3_1 + " and " + MQTT_VERSION_3_1_1 + ".", e.getMessage());
		}
	}
}
