package org.eclipse.paho.mqttv5.client.internal;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Test;

import static org.junit.Assert.*;

public class NetworkModuleServiceTest {

	@Test
	public void testValidateURI() {
		NetworkModuleService.validateURI("tcp://host_literal:1883");
		NetworkModuleService.validateURI("ssl://host_literal:8883");
		NetworkModuleService.validateURI("ws://host_literal:80/path/to/ws");
		NetworkModuleService.validateURI("wss://host_literal:443/path/to/ws");
	}

	public void failEmptyUri() {
		try {
			NetworkModuleService.validateURI("");
			fail("Must fail: Empty URI");
		} catch (IllegalArgumentException e) {
			assertEquals("missing scheme in broker URI: ", e.getMessage());
		}
	}

	@Test
	public void failInvalidUri() {
		try {
			NetworkModuleService.validateURI("no URI at all");
			fail("Must fail: Can't parse string to URI");
		} catch (IllegalArgumentException e) {
			assertEquals("Can't parse string to URI \"no URI at all\"", e.getMessage());
		}
	}

	@Test
	public void failWithPathOnTcpUri() {
		try {
			NetworkModuleService.validateURI("tcp://host_literal:1883/somePath");
			fail("Must fail: URI path must be empty");
		} catch (IllegalArgumentException e) {
			assertEquals("URI path must be empty \"tcp://host_literal:1883/somePath\"", e.getMessage());
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithPathOnSslUri() {
		NetworkModuleService.validateURI("ssl://host_literal:1883/somePath");
	}

	@Test
	public void failWithUnsuppurtedSchemeUri() {
		try {
			NetworkModuleService.validateURI("unknown://host_literal:1883");
			fail("Must fail: Unknow scheme");
		} catch (IllegalArgumentException e) {
			assertEquals("no NetworkModule installed for scheme \"unknown\" of URI \"unknown://host_literal:1883\"", e
					.getMessage());
		}
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
		MqttConnectionOptions options = new MqttConnectionOptions();
		int conTimeout = 234;
		options.setConnectionTimeout(conTimeout);
		String clientId = "";

		NetworkModule result = NetworkModuleService.createInstance(brokerUri, options, clientId);

		assertTrue(result instanceof TCPNetworkModule);
		assertEquals(brokerUri, result.getServerURI());
	}
}
