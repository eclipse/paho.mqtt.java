package org.eclipse.paho.client.mqttv3.test;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1_1;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_DEFAULT;
import static org.junit.Assert.*;
import org.junit.Test;

public class MqttConnectOptionsTest {

	@Test
	public void testValidateURI() {
		try {
			MqttConnectOptions.validateURI("");
			fail("Must fail: Empty URI");
		} catch (IllegalArgumentException e) {
			assertEquals("Unknown scheme \"null\" of URI \"\"", e.getMessage());
		}
		
		try {
			MqttConnectOptions.validateURI("udp://localhost:1883");
			fail("Must fail: Unknow scheme");
		} catch (IllegalArgumentException e) {
			assertEquals("Unknown scheme \"udp\" of URI \"udp://localhost:1883\"", e.getMessage());
		}
		
		MqttConnectOptions.validateURI("tcp://localhost:1883");
		
		try {
			MqttConnectOptions.validateURI("tcp://localhost:1883/mqtt");
			fail("Must fail: URI path must be empty");
		} catch (IllegalArgumentException e) {
			assertEquals("URI path must be empty \"tcp://localhost:1883/mqtt\"", e.getMessage());
		}
		
		MqttConnectOptions.validateURI("tcp://localhost");
		
		try {
			MqttConnectOptions.validateURI("localhost:1883");
			fail("Must fail: Unknow scheme");
		} catch (IllegalArgumentException e) {
			assertEquals("Unknown scheme \"localhost\" of URI \"localhost:1883\"", e.getMessage());
		}
		
		try {
			MqttConnectOptions.validateURI("localhost");
			fail("Must fail: URI path must be empty");
		} catch (IllegalArgumentException e) {
			assertEquals("URI path must be empty \"localhost\"", e.getMessage());
		}
		
		try {
			MqttConnectOptions.validateURI("tcp://");
			fail("Must fail: Can't parse string to URI");
		} catch (IllegalArgumentException e) {
			assertEquals("Can't parse string to URI \"tcp://\"", e.getMessage());
		}
	}

	@Test
	public void testValidateMQTTVersions() {
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		try {
			connectOptions.setMqttVersion(MQTT_VERSION_DEFAULT);
			connectOptions.setMqttVersion(MQTT_VERSION_3_1);
			connectOptions.setMqttVersion(MQTT_VERSION_3_1_1);
		} catch(IllegalArgumentException e) {
			fail("MQTT Versions are valid");
		}

		try {
			connectOptions.setMqttVersion(9);
			fail("MQTT Version is not valid");
		} catch (IllegalArgumentException e) {
			assertEquals("An incorrect version was used \"9\". Acceptable version options are " + MQTT_VERSION_DEFAULT + ", " + MQTT_VERSION_3_1 + " and " + MQTT_VERSION_3_1_1 + ".", e.getMessage());
		}

	}

}
