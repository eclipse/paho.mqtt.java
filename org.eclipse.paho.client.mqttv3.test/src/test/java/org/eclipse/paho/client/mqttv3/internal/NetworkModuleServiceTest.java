package org.eclipse.paho.client.mqttv3.internal;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NetworkModuleServiceTest {

	@Test
	public void testValidateURI() {
		NetworkModuleService.validateURI("tcp://host_literal:1883");
		NetworkModuleService.validateURI("ssl://host_literal:8883");
		NetworkModuleService.validateURI("ws://host_literal:80/path/to/ws");
		NetworkModuleService.validateURI("wss://host_literal:443/path/to/ws");
	}

	@Test(expected = IllegalArgumentException.class)
	public void failInvalidUri() {
		NetworkModuleService.validateURI("no URI at all");
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithPathOnTcpUri() {
		NetworkModuleService.validateURI("tcp://host_literal:1883/somePath");
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithPathOnSslUri() {
		NetworkModuleService.validateURI("ssl://host_literal:1883/somePath");
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithUnsuppurtedSchemeUri() {
		NetworkModuleService.validateURI("unknown://host_literal:1883");
	}

	/**
	 * Test for URI parsing with '_' in hostname.
	 */
	@Test
	public void testApplyRFC3986AuthorityPatch() throws URISyntaxException {
		URI uri = new URI("tcp://user:password@some_host:666/some_path");
		/*
		 * If the following asserts trigger, then the patch may be no longer required, as Java URI class does the
		 * RFC3986 parsing itself.
		 */
		assertNull("patch no longer necessary?", uri.getUserInfo());
		assertNull("patch no longer necessary?", uri.getHost());
		assertEquals("patch no longer necessary?", -1, uri.getPort());

		NetworkModuleService.applyRFC3986AuthorityPatch(uri);

		assertEquals("wrong user info", "user:password", uri.getUserInfo());
		assertEquals("wrong hostname", "some_host", uri.getHost());
		assertEquals("wrong port", 666, uri.getPort());
	}

	@Test
	public void testCreateInstance() throws MqttException {
		String brokerUri = "tcp://localhost:666";
		MqttConnectOptions options = new MqttConnectOptions();
		int conTimeout = 234;
		options.setConnectionTimeout(conTimeout);
		String clientId = "";

		NetworkModule result = NetworkModuleService.createInstance(brokerUri, options, clientId);

		assertTrue(result instanceof TCPNetworkModule);
		assertEquals(brokerUri, result.getServerURI());
	}
}
