package org.eclipse.paho.mqttv5.client.vertx.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.vertx.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.vertx.IMqttToken;
import org.eclipse.paho.mqttv5.client.vertx.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.vertx.MqttCallback;
import org.eclipse.paho.mqttv5.client.vertx.MqttClient;
import org.eclipse.paho.mqttv5.client.vertx.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.vertx.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.vertx.MqttToken;
import org.eclipse.paho.mqttv5.client.vertx.internal.MqttPersistentData;
import org.eclipse.paho.mqttv5.client.vertx.test.connectionLoss.ConnectionLossTest;
import org.eclipse.paho.mqttv5.client.vertx.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.vertx.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.vertx.test.utilities.ConnectionManipulationProxyServer;
import org.eclipse.paho.mqttv5.client.vertx.test.utilities.TestByteArrayMemoryPersistence;
import org.eclipse.paho.mqttv5.client.vertx.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistable;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PersistenceTests implements MqttCallback {
	static final Class<?> cclass = PersistenceTests.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);
	
	private static URI serverURI;
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
	
	@After
	public void afterTest() {
		log.info("Disabling Proxy");
		proxy.disableProxy();
	}
	
	@Test
	public void testConnectionResume() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;

		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setSessionExpiryInterval(99999L); // Ensure the session state is not cleaned up on disconnect
		options.setKeepAliveInterval(keepAlive);

		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), "testClientId");
		client.setCallback(this);
		proxy.enableProxy();
		IMqttToken tok = client.connect(options);
		tok.waitForCompletion();
		
		String topic = "username/clientId/abc";
		tok = client.subscribe(topic, 2);
		tok.waitForCompletion();

		log.info((new Date()) + " - Connected.");
		
		// start a background task to disconnect the proxy while messages are being sent
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				log.info("Cutting connection");
				proxy.disableProxy();
			}
		}, 200); // delay in milliseconds
		
		// send some messages
		while (client.isConnected()) {
			client.publish("username/clientId/abc", "test".getBytes(), 2, false);
			Thread.sleep(10);
		}
		
		// Now check that there are some inflight messages
		
		// reconnect, so any inflight messages should be continued
		proxy.enableProxy();
		options.setCleanStart(false);
		tok = client.connect(options);
		tok.waitForCompletion();

		// Now ensure that any outstanding messages are received
		
		client.disconnect(0);
		client.close();
	}

	
	
	@Test
	/**
	 * Tests that an MQTTv5 client persists MQTTPublish Messages correctly,
	 * specifically that Topic Aliases are not persisted.
	 */
	public void persistMessageWithTopicAlias() throws URISyntaxException, MqttException {
		serverURI = TestProperties.getServerURI();
		TestByteArrayMemoryPersistence memoryPersistence = new TestByteArrayMemoryPersistence();

		// Create an MqttAsyncClient with a null Client ID.
		MqttAsyncClient client = new MqttAsyncClient(serverURI.toString(), "testClientId", memoryPersistence);
		
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setTopicAliasMaximum(10);
		
		IMqttToken connectToken = client.connect(options);
		connectToken.waitForCompletion(1000);
		Assert.assertTrue("The client should be connected.", client.isConnected());
		
		// Publish a message at QoS 2
		MqttMessage message = new MqttMessage("Test Message".getBytes(), 2, false, null);
		IMqttDeliveryToken deliveryToken = client.publish("testTopic", message);
		deliveryToken.waitForCompletion(1000);
		
		// wouldn't that message have been removed from persistence by now? - IGC
		
		// Validate that the message that was persisted does not have a topic alias.
		String expectedKey = "s-1";
		Assert.assertNotNull(memoryPersistence.getDataCache());
		Assert.assertNotNull(memoryPersistence.getDataCache().get(expectedKey));
		byte[] messageBytes = (byte[]) memoryPersistence.getDataCache().get(expectedKey);
		MqttPersistable persistable = new MqttPersistentData(expectedKey, messageBytes, 0, messageBytes.length, null, 0, 0);
		MqttWireMessage wireMessage = MqttWireMessage.createWireMessage(persistable);
		Assert.assertNull(wireMessage.getProperties().getTopicAlias());
		
		// Cleanup
		IMqttToken disconnectToken = client.disconnect();
		disconnectToken.waitForCompletion(1000);
		Assert.assertFalse("The client should now be disconnected.", client.isConnected());
		client.close();
		
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.info("Message Arrived on " + topic + " with " + new String(message.getPayload()));
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// log.info("Delivery Complete: " + token.getMessageId());
	}
	
	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		// TODO Auto-generated method stub

	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		// TODO Auto-generated method stub

	}
	
}
