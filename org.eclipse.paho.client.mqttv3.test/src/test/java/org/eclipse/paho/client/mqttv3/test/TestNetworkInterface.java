package org.eclipse.paho.client.mqttv3.test;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestNetworkInterface { 
	private String WLAN_NETWORK_INTERFACE = "wlan0";

	@Test
	public void testValidateSetNetworkInterface() {
		MqttConnectOptions connectOptions = new MqttConnectOptions();

		connectOptions.setNetworkInterface(WLAN_NETWORK_INTERFACE);
	}

	@Test
	public void testInvalidSetNetworkInterface() {
		MqttConnectOptions connectOptions = new MqttConnectOptions();

		try {
			connectOptions.setNetworkInterface("");
			fail("MQTT Network Interface is not valid");
		} catch (IllegalArgumentException e) {
			assertEquals("Set network interface options must be valid strings. Null and empty strings are not acceptable.", e.getMessage());
		}
	}
}
