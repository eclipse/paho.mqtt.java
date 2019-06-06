package org.eclipse.paho.mqttv5.client.test.automaticReconnect;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.common.test.categories.MQTTV5Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.ConnectionManipulationProxyServer;
import org.eclipse.paho.mqttv5.client.test.utilities.MqttV5Receiver;
import org.eclipse.paho.mqttv5.client.test.utilities.TestMemoryPersistence;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@Category({OnlineTest.class, MQTTV5Test.class})
@RunWith(Parameterized.class)
public class OfflineBufferingTest {

	static final Class<?> cclass = OfflineBufferingTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private URI serverURI;
	private String serverURIString;
	static ConnectionManipulationProxyServer proxy;
	private static String topicPrefix;
	
	@Parameters
	public static Collection<Object[]> data() throws Exception {
		
		return Arrays.asList(new Object[][] {     
            { TestProperties.getServerURI() }, { TestProperties.getWebSocketServerURI() }
      });
		
	}
	
	public OfflineBufferingTest(URI serverURI) throws Exception {
		this.serverURI = serverURI;
		this.serverURIString = "tcp://" + serverURI.getHost() + ":" + serverURI.getPort();
		startProxy();
	}
	
	public void startProxy() throws Exception{
		// Use 0 for the first time.
		proxy = new ConnectionManipulationProxyServer(serverURI.getHost(), serverURI.getPort(), 0);
		proxy.startProxy();
		while (!proxy.isPortSet()) {
			Thread.sleep(0);
		}
		log.log(Level.INFO, "Proxy Started, port set to: " + proxy.getLocalPort());
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);
			//serverURI = TestProperties.getServerURI();
			//serverURIString = "tcp://" + serverURI.getHost() + ":" + serverURI.getPort();
		    topicPrefix = "OfflineBufferingTest-" + UUID.randomUUID().toString() + "-";

			// Use 0 for the first time.
			/*proxy = new ConnectionManipulationProxyServer(serverURI.getHost(), serverURI.getPort(), 2883);
			proxy.startProxy();
			while (!proxy.isPortSet()) {
				Thread.sleep(0);
			}
			log.log(Level.INFO, "Proxy Started, port set to: " + proxy.getLocalPort());*/
		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			throw exception;
		}

	}
	
	@After
	public void clearUpAfterTest() {
		proxy.disableProxy();
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
		IMqttToken pubToken;

		// Client Options
		MqttConnectionOptions options = new MqttConnectionOptions();
		
		options.setCleanStart(true);
		options.setAutomaticReconnect(true);
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), 
				methodName, new MemoryPersistence());
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion(5000);
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish Message
		pubToken = client.publish(topicPrefix + methodName, new MqttMessage(methodName.getBytes()));
		log.info("Publish attempted: isComplete:" + pubToken.isComplete());
		Assert.assertFalse(pubToken.isComplete());
		// Enable Proxy
		proxy.enableProxy();
		pubToken.waitForCompletion(5000);

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
		disconnectToken.waitForCompletion(5000);
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
		String clientId = methodName + "sub-client";

		// Tokens
		IMqttToken connectToken;

		int msg_count = 1000;
		
		// Client Options
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setAutomaticReconnect(true);
		
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), 
				methodName, new MemoryPersistence());
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferSize(msg_count);
		disconnectedOpts.setBufferEnabled(true);
		client.setBufferOpts(disconnectedOpts);
		
		// Create subscription client that won't be affected by proxy
		MqttAsyncClient subClient = new MqttAsyncClient(serverURIString, methodName + "sub-client", new TestMemoryPersistence());
		MqttV5Receiver mqttV3Receiver = new MqttV5Receiver(clientId, LoggingUtilities.getPrintStream());
		subClient.setCallback(mqttV3Receiver);
		IMqttToken subConnectToken = subClient.connect();
		subConnectToken.waitForCompletion(5000);
		// Subscribe to topic
		subClient.subscribe(new MqttSubscription(topicPrefix + methodName, 0));

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion(5000);
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish some messages
		for (int x = 0; x < msg_count; x++) {
			client.publish(topicPrefix + methodName, new MqttMessage(Integer.toString(x).getBytes()));
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
		//Debug clientDebug = client.getDebug();
		//clientDebug.dumpClientState();

		isConnected = client.isConnected();
		log.info("Proxy Re-Enabled isConnected: " + isConnected);
		Assert.assertTrue(isConnected);
		
		Thread.sleep(5000);

		// Check that all messages have been delivered
		for (int x = 0; x < msg_count; x++) {
			boolean received = mqttV3Receiver.validateReceipt(topicPrefix + methodName, 0, Integer.toString(x).getBytes());
			Assert.assertTrue(received);
		}
		log.info("All messages sent and received correctly.");
		IMqttToken disconnectToken = client.disconnect();
		disconnectToken.waitForCompletion(5000);
		client.close();
		client = null;

		IMqttToken subClientDisconnectToken = subClient.disconnect();
		subClientDisconnectToken.waitForCompletion(5000);
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
		int maxMessages = 10;

		// Tokens
		IMqttToken connectToken;

		// Client Options
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setAutomaticReconnect(true);
		options.setSessionExpiryInterval(120L);
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), 
				methodName, new MemoryPersistence());
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		// Set buffer to 100 to save time
		disconnectedOpts.setBufferSize(maxMessages);
		disconnectedOpts.setDeleteOldestMessages(true);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion(5000);
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish 100 messages
		for (int x = 0; x < maxMessages; x++) {
			client.publish(topicPrefix + methodName, new MqttMessage(Integer.toString(x).getBytes()));
		}

		// Publish one message too many
		log.info("About to publish one message too many");
		client.publish(topicPrefix + methodName, new MqttMessage(Integer.toString(101).getBytes()));
		// Make sure that the message now at index 0 in the buffer is '1'
		// instead of '0'
		MqttWireMessage messageAt0 = client.getBufferedMessage(0);
		String msg = new String(messageAt0.getPayload());
		Assert.assertEquals("1", msg);
		
		client.deleteBufferedMessage(0);
		messageAt0 = client.getBufferedMessage(0);
		msg = new String(messageAt0.getPayload());
		Assert.assertEquals("2", msg);
		
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
		int maxMessages = 10;

		// Tokens
		IMqttToken connectToken;

		// Client Options
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setAutomaticReconnect(true);
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), 
				methodName, new MemoryPersistence());
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		// Set buffer to 100 to save time
		disconnectedOpts.setBufferSize(maxMessages);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion(5000);
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish 100 messages
		for (int x = 0; x < maxMessages; x++) {
			client.publish(topicPrefix + methodName, new MqttMessage(Integer.toString(x).getBytes()));
		}
		log.info("About to publish one message too many");

		try {
			client.publish(topicPrefix + methodName, new MqttMessage(Integer.toString(101).getBytes()));
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
		IMqttToken pubToken;

		// Client Options
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setAutomaticReconnect(true);
		final MemoryPersistence persistence = new MemoryPersistence();
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), methodName,
				persistence);
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		disconnectedOpts.setPersistBuffer(true);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion(5000);
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Make Sure persistence is empty before publish
		List<String> keys = Collections.list(persistence.keys());
		Assert.assertEquals(0, keys.size());

		// Publish Message
		pubToken = client.publish(topicPrefix + methodName, new MqttMessage("test".getBytes()));
		log.info("Publish attempted: isComplete:" + pubToken.isComplete());
		Assert.assertFalse(pubToken.isComplete());
		// Check that message is now in persistence layer

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
		int qos = 2;
		String clientId = methodName + "sub-client";

		// Mock up an Mqtt Message to be stored in Persistence
		MqttMessage mqttMessage = new MqttMessage(methodName.getBytes());
		mqttMessage.setQos(qos);
		MqttPublish pubMessage = new MqttPublish(topicPrefix + methodName, mqttMessage, new MqttProperties());
		// If ID is not set, then the persisted message may be invalid for QoS 1 & 2
		pubMessage.setMessageId(1);
		final TestMemoryPersistence persistence = new TestMemoryPersistence();
		persistence.open(null, null);
		persistence.put("sb-1", (MqttPublish) pubMessage);
		List<String> persistedKeys = Collections.list(persistence.keys());
		log.info("There are now: " + persistedKeys.size() + " keys in persistence");
		Assert.assertEquals(1, persistedKeys.size());
	
		// Create Subscription client to watch for the message being published
		// as soon as the main client connects
		log.info("Creating subscription client");
		MqttAsyncClient subClient = new MqttAsyncClient(serverURIString, clientId, new TestMemoryPersistence());
		MqttV5Receiver mqttV3Receiver = new MqttV5Receiver(clientId, LoggingUtilities.getPrintStream());
		subClient.setCallback(mqttV3Receiver);
		IMqttToken subConnectToken = subClient.connect();
		subConnectToken.waitForCompletion(5000);
		Assert.assertTrue(subClient.isConnected());
		IMqttToken subToken = subClient.subscribe(new MqttSubscription(topicPrefix + methodName, qos));
		subToken.waitForCompletion(5000);

		// Create Real client
		log.info("Creating new client that uses existing persistence layer");
		MqttConnectionOptions optionsNew = new MqttConnectionOptions();
		optionsNew.setCleanStart(false);
		MqttAsyncClient newClient = new MqttAsyncClient(serverURIString, methodName + "new-client11", persistence);
		// Connect Client with existing persistence layer
		IMqttToken newClientConnectToken = newClient.connect(optionsNew);
		newClientConnectToken.waitForCompletion(5000);
		Assert.assertTrue(newClient.isConnected());

		// Check that message is published / delivered
		boolean recieved = mqttV3Receiver.validateReceipt(topicPrefix + methodName, qos, methodName.getBytes());
		Assert.assertTrue(recieved);
		log.info("Message was successfully delivered after connect");
		
		// Allow a few seconds for the QoS 2 flow to complete
		Thread.sleep(2000);

		List<String> postConnectKeys = Collections.list(persistence.keys());
		log.info("There are now: " + postConnectKeys.size() + " keys in persistence");
		Assert.assertEquals(0, postConnectKeys.size());

		IMqttToken newClientDisconnectToken = newClient.disconnect();
		newClientDisconnectToken.waitForCompletion(5000);
		newClient.close();
		newClient = null;

		IMqttToken subClientDisconnectToken = subClient.disconnect();
		subClientDisconnectToken.waitForCompletion(5000);
		subClient.close();
		subClient = null;

	}
	
	/**
	 * Tests that when a Publish message with a topic alias is buffered,
	 * that the topic alias is stripped from the message.
	 */
	@Test
	public void testBufferedMessageTopicAlias() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		// Tokens
		IMqttToken connectToken;
		IMqttToken pubToken;

		// Client Options
		MqttConnectionOptions options = new MqttConnectionOptions();
		
		options.setCleanStart(true);
		//options.setAutomaticReconnect(true);
		options.setSessionExpiryInterval(205L);
		options.setTopicAliasMaximum(10);
		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(),
				methodName, new MemoryPersistence());
		DisconnectedBufferOptions disconnectedOpts = new DisconnectedBufferOptions();
		disconnectedOpts.setBufferEnabled(true);
		client.setBufferOpts(disconnectedOpts);

		// Enable Proxy & Connect to server
		proxy.enableProxy();
		connectToken = client.connect(options);
		connectToken.waitForCompletion(5000);
		boolean isConnected = client.isConnected();
		log.info("First Connection isConnected: " + isConnected);
		Assert.assertTrue(isConnected);

		// Disable Proxy and cause disconnect
		proxy.disableProxy();
		isConnected = client.isConnected();
		log.info("Proxy Disconnect isConnected: " + isConnected);
		Assert.assertFalse(isConnected);

		// Publish Message
		pubToken = client.publish(topicPrefix + methodName, new MqttMessage(methodName.getBytes()));
		log.info("Publish attempted: isComplete:" + pubToken.isComplete());
		Assert.assertFalse(pubToken.isComplete());
		
		log.info(String.format("There is %d message buffered", client.getBufferedMessageCount()));
		Assert.assertEquals(1, client.getBufferedMessageCount());
		MqttWireMessage publishedMessage = client.getBufferedMessage(0);
		log.info(String.format("It's Topic alias is set: %b", publishedMessage.getProperties().getTopicAlias()));
		Assert.assertNull(publishedMessage.getProperties().getTopicAlias());
		
	}

}
