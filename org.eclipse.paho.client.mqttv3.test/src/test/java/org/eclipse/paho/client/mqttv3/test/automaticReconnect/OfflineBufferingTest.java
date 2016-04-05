package org.eclipse.paho.client.mqttv3.test.automaticReconnect;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.ConnectionManipulationProxyServer;
import org.eclipse.paho.client.mqttv3.test.utilities.MqttV3Receiver;
import org.eclipse.paho.client.mqttv3.test.utilities.TestMemoryPersistence;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.eclipse.paho.client.mqttv3.util.Debug;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class OfflineBufferingTest {

	static final Class<?> cclass = OfflineBufferingTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static final MemoryPersistence DATA_STORE = new MemoryPersistence();

	private static URI serverURI;
	private static String serverURIString;
	private String testTopic = "OBTOPIC";
	static ConnectionManipulationProxyServer proxy;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);
			serverURI = TestProperties.getServerURI();
			serverURIString = "tcp://" + serverURI.getHost() + ":" + serverURI.getPort();
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

	/**
	 * Tests that A message can be buffered whilst the client is in a
	 * disconnected state and is then delivered once the client has reconnected
	 * automatically.
	 */
	@Test
	public void testSingleMessageBufferAndDeliver() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		// Tokens
		IMqttToken connectToken;
		IMqttDeliveryToken pubToken;

		// Client Options
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setAutomaticReconnect(true);
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), methodName, DATA_STORE);
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion();
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish Message
		pubToken = client.publish(testTopic + methodName, new MqttMessage(methodName.getBytes()));
		log.info("Publish attempted: isComplete:" + pubToken.isComplete());
		Assert.assertFalse(pubToken.isComplete());
		// Enable Proxy
		proxy.enableProxy();
		pubToken.waitForCompletion();

		// Check that we are connected
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

		// Check that Message has been delivered
		log.info("Message Delivered: " + pubToken.isComplete());
		Assert.assertTrue(pubToken.isComplete());
		IMqttToken disconnectToken = client.disconnect();
		disconnectToken.waitForCompletion();
		client.close();
		client = null;
		proxy.disableProxy();
	}

	/**
	 * Tests that multiple messages can be buffered whilst the client is in a
	 * disconnected state and that they are all then delivered once the client
	 * has connected automatically.
	 */
	@Test
	public void testManyMessageBufferAndDeliver() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		// Tokens
		IMqttToken connectToken;

		// Client Options
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(false);
		options.setAutomaticReconnect(true);
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), methodName, DATA_STORE);
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		client.setBufferOpts(disconnectedOpts);

		// Create subscription client that won't be affected by proxy
		MqttAsyncClient subClient = new MqttAsyncClient(serverURIString, methodName + "sub-client");
		MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(subClient, LoggingUtilities.getPrintStream());
		subClient.setCallback(mqttV3Receiver);
		IMqttToken subConnectToken = subClient.connect();
		subConnectToken.waitForCompletion();
		// Subscribe to topic
		subClient.subscribe(testTopic + methodName, 0);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion();
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish 100 messages
		for (int x = 0; x < 100; x++) {
			client.publish(testTopic + methodName, new MqttMessage(Integer.toString(x).getBytes()));
		}
		// Enable Proxy
		proxy.enableProxy();

		// Check that we are connected
		// give it some time to reconnect
		long currentTime = System.currentTimeMillis();
		int timeout = 8000;
		while (client.isConnected() == false) {

			long now = System.currentTimeMillis();
			if ((currentTime + timeout) < now) {
				log.warning("Timeout Exceeded");
				break;
			}
			Thread.sleep(500);
		}
		Debug clientDebug = client.getDebug();
		clientDebug.dumpClientState();

		isConnected = client.isConnected();
		log.info("Proxy Re-Enabled isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Check that all messages have been delivered
		for (int x = 0; x < 100; x++) {
			boolean recieved = mqttV3Receiver.validateReceipt(testTopic + methodName, 0, Integer.toString(x).getBytes());
			Assert.assertTrue(recieved);
		}
		log.info("All messages sent and Recieved correctly.");
		IMqttToken disconnectToken = client.disconnect();
		disconnectToken.waitForCompletion();
		client.close();
		client = null;

		IMqttToken subClientDisconnectToken = subClient.disconnect();
		subClientDisconnectToken.waitForCompletion();
		subClient.close();
		subClient = null;

		proxy.disableProxy();
	}

	/**
	 * Tests that the buffer correctly handles messages being buffered when the
	 * buffer is full and deleteOldestBufferedMessage is set to true.
	 */
	@Test
	public void testDeleteOldestBufferedMessages() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		// Tokens
		IMqttToken connectToken;

		// Client Options
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(false);
		options.setAutomaticReconnect(true);
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), methodName, DATA_STORE);
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		// Set buffer to 100 to save time
		disconnectedOpts.setBufferSize(100);
		disconnectedOpts.setDeleteOldestMessages(true);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion();
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish 100 messages
		for (int x = 0; x < 100; x++) {
			client.publish(testTopic + methodName, new MqttMessage(Integer.toString(x).getBytes()));
		}

		// Publish one message too many
		log.info("About to publish one message too many");
		client.publish(testTopic + methodName, new MqttMessage(Integer.toString(101).getBytes()));
		// Make sure that the message now at index 0 in the buffer is '1'
		// instead of '0'
		MqttMessage messageAt0 = client.getBufferedMessage(0);
		String msg = new String(messageAt0.getPayload());
		Assert.assertEquals("1", msg);
		client.close();
		client = null;
		proxy.disableProxy();
	}

	/**
	 * Tests that A message cannot be buffered when the buffer is full and
	 * deleteOldestBufferedMessage is set to false.
	 */
	@Test
	public void testNoDeleteOldestBufferedMessages() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		// Tokens
		IMqttToken connectToken;

		// Client Options
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(false);
		options.setAutomaticReconnect(true);
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), methodName, DATA_STORE);
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		// Set buffer to 100 to save time
		disconnectedOpts.setBufferSize(100);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion();
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish 100 messages
		for (int x = 0; x < 100; x++) {
			client.publish(testTopic + methodName, new MqttMessage(Integer.toString(x).getBytes()));
		}
		log.info("About to publish one message too many");

		try {
			client.publish(testTopic + methodName, new MqttMessage(Integer.toString(101).getBytes()));
			client.close();
			client = null;
			Assert.fail("An MqttException Should have been thrown.");
		} catch (MqttException ex) {
			client.close();
			client = null;
			proxy.disableProxy();
		} finally {
			proxy.disableProxy();
		}

	}

	/**
	 * Tests that if enabled, buffered messages are persisted to the persistence
	 * layer
	 */
	@Test
	public void testPersistBufferedMessages() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		// Tokens
		IMqttToken connectToken;
		IMqttDeliveryToken pubToken;

		// Client Options
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setAutomaticReconnect(true);
		final MemoryPersistence persistence = new MemoryPersistence();
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), methodName,
				persistence);
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion();
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Make Sure persistence is empty before publish
		@SuppressWarnings("unchecked")
		List<String> keys = Collections.list(persistence.keys());
		Assert.assertEquals(0, keys.size());

		// Publish Message
		pubToken = client.publish(testTopic + methodName, new MqttMessage("test".getBytes()));
		log.info("Publish attempted: isComplete:" + pubToken.isComplete());
		Assert.assertFalse(pubToken.isComplete());
		// Check that message is now in persistence layer

		@SuppressWarnings("unchecked")
		List<String> keysNew = Collections.list(persistence.keys());
		log.info("There are now: " + keysNew.size() + " keys in persistence");
		Assert.assertEquals(1, keysNew.size());

		client.close();
		client = null;
		proxy.disableProxy();
	}

	/**
	 * Tests that persisted buffered messages are published correctly when the
	 * client connects for the first time and are un persisted.
	 */
	@Test
	public void testUnPersistBufferedMessagesOnNewClient() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		// Mock up an Mqtt Message to be stored in Persistence
		MqttMessage mqttMessage = new MqttMessage(methodName.getBytes());
		mqttMessage.setQos(0);
		MqttPublish pubMessage = new MqttPublish(testTopic + methodName, mqttMessage);
		final TestMemoryPersistence persistence = new TestMemoryPersistence();
		persistence.open(null, null);
		persistence.put("sb-0", (MqttPublish) pubMessage);
		@SuppressWarnings("unchecked")
		List<String> persistedKeys = Collections.list(persistence.keys());
		log.info("There are now: " + persistedKeys.size() + " keys in persistence");
		Assert.assertEquals(1, persistedKeys.size());

		// Create Subscription client to watch for the message being published
		// as soon as the main client connects
		log.info("Creating subscription client");
		MqttAsyncClient subClient = new MqttAsyncClient(serverURIString, methodName + "sub-client");
		MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(subClient, LoggingUtilities.getPrintStream());
		subClient.setCallback(mqttV3Receiver);
		IMqttToken subConnectToken = subClient.connect();
		subConnectToken.waitForCompletion();
		Assert.assertTrue(subClient.isConnected());
		IMqttToken subToken = subClient.subscribe(testTopic + methodName, 0);
		subToken.waitForCompletion();

		// Create Real client
		log.info("Creating new client that uses existing persistence layer");
		MqttConnectOptions optionsNew = new MqttConnectOptions();
		optionsNew.setCleanSession(false);
		MqttAsyncClient newClient = new MqttAsyncClient(serverURIString, methodName + "new-client11", persistence);

		// Connect Client with existing persistence layer
		IMqttToken newClientConnectToken = newClient.connect(optionsNew);
		newClientConnectToken.waitForCompletion();
		Assert.assertTrue(newClient.isConnected());

		// Check that message is published / delivered
		boolean recieved = mqttV3Receiver.validateReceipt(testTopic + methodName, 0, methodName.getBytes());
		Assert.assertTrue(recieved);
		log.info("Message was successfully delivered after connect");

		@SuppressWarnings("unchecked")
		List<String> postConnectKeys = Collections.list(persistence.keys());
		log.info("There are now: " + postConnectKeys.size() + " keys in persistence");
		Assert.assertEquals(0, postConnectKeys.size());

		IMqttToken newClientDisconnectToken = newClient.disconnect();
		newClientDisconnectToken.waitForCompletion();
		newClient.close();
		newClient = null;

		IMqttToken subClientDisconnectToken = subClient.disconnect();
		subClientDisconnectToken.waitForCompletion();
		subClient.close();
		subClient = null;

	}

}
