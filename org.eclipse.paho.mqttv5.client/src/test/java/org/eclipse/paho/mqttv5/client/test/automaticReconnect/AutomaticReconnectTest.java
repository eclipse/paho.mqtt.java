package org.eclipse.paho.mqttv5.client.test.automaticReconnect;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.common.test.categories.MQTTV5Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.ConnectionManipulationProxyServer;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category({OnlineTest.class, MQTTV5Test.class})
public class AutomaticReconnectTest {

	static final Class<?> cclass = AutomaticReconnectTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static URI serverURI;
	private String clientId = "device-client-id";
	static ConnectionManipulationProxyServer proxy;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);
			serverURI = TestProperties.getServerURI();
			// Use 0 for the first time.
			proxy = new ConnectionManipulationProxyServer(serverURI.getHost(), serverURI.getPort(), 0);
			proxy.startProxy();
			while (!proxy.isPortSet()) {
				Thread.sleep(0);
			}
			log.log(Level.INFO, "Proxy Started, port set to: " + proxy.getLocalPort());
		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			throw exception;
		}

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info("Tests finished, stopping proxy");
		proxy.stopProxy();

	}

	/**
	 * Tests that if a connection is opened and then is lost that the client
	 * automatically reconnects
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAutomaticReconnectAfterDisconnect() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setAutomaticReconnect(true);
		final MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(),
				methodName, new MemoryPersistence());

		proxy.enableProxy();
		client.connect(options);

		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		proxy.enableProxy();
		// give it some time to reconnect
		long currentTime = System.currentTimeMillis();
		int timeout = 4000;
		while (client.isConnected() == false) {
			long now = System.currentTimeMillis();
			if ((currentTime + timeout) < now) {
				log.warning("Timeout Exceeded");
				break;
			}
			Thread.sleep(500);
		}
		isConnected = client.isConnected();
		log.info("Proxy Re-Enabled isConnected: " + isConnected);
		Assert.assertTrue(isConnected);
		client.disconnect();
		Assert.assertFalse(client.isConnected());
		client.close();
	}

	/**
	 * Tests that if a connection is opened and lost, that when the user calls
	 * reconnect() that the client will attempt to reconnect straight away
	 */
	@Test
	public void testManualReconnectAfterDisconnect() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setAutomaticReconnect(true);

		final MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(),
				methodName, new MemoryPersistence());

		proxy.enableProxy();
		client.connect(options);

		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		proxy.enableProxy();
		client.reconnect();
		// give it some time to reconnect
		Thread.sleep(4000);
		isConnected = client.isConnected();
		log.info("Proxy Re-Enabled isConnected: " + isConnected);
		Assert.assertTrue(isConnected);
		client.disconnect();
		Assert.assertFalse(client.isConnected());
		client.close();
	}

	/**
	 * Tests that if the initial connection attempt fails, that the automatic
	 * reconnect code does NOT engage.
	 */
	@Test
	public void testNoAutomaticReconnectWithNoInitialConnect() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setAutomaticReconnect(true);
		options.setConnectionTimeout(15);
		final MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(),
				methodName, new MemoryPersistence());

		// Make sure the proxy is disabled and give it a second to close everything down
		proxy.disableProxy();
		try {
			client.connect(options);
		} catch (MqttException ex) {
			// Exceptions are good in this case!
		}
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Enable The Proxy
		proxy.enableProxy();

		// Give it some time to make sure we are still not connected
		long currentTime = System.currentTimeMillis();
		int timeout = 4000;
		while (client.isConnected() == false) {
			long now = System.currentTimeMillis();
			if ((currentTime + timeout) < now) {
				Assert.assertFalse(isConnected);
				break;
			}
			Thread.sleep(500);
		}
		isConnected = client.isConnected();
		log.info("Proxy Re-Enabled isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		log.info("Disconnect and close: ");
		client.disconnect();
		client.close();
	}
}
