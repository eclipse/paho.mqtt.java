package org.eclipse.paho.mqttv5.client.test.automaticReconnect;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.common.test.categories.MQTTV5Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.test.automaticReconnect.TopicAliasReconnectTest;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.ConnectionManipulationProxyServer;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category({OnlineTest.class, MQTTV5Test.class})
public class TopicAliasReconnectTest {

	static final Class<?> cclass = TopicAliasReconnectTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static final MemoryPersistence DATA_STORE = new MemoryPersistence();

	private static URI serverURI;
	private String clientId = "device-client-id";
	private String exampleTopic = "topicAliasTest";
	private String exampleContent = "topicAliasContent";
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
	 * This test requires an MQTTv5 compliant server to verify, if the test fails,
	 * then the server should automatically disconnect the client on step 5.
	 * <ol>
	 * <li>Client connects with Clean session=false, session expiry=Max (no
	 * expiry)</li>
	 * <li>Client publishes messages to topic x, after the first messages, all
	 * subsequent messages should have a topic Alias.</li>
	 * <li>Connection is lost.</li>
	 * <li>Client uses reconnect logic and sets cleanStart = false to continue using
	 * existing session.</li>
	 * <li>Upon reconnect, the client should NOT continue using the topic
	 * alias.</li>
	 * </ol>
	 * 
	 */
	@Test
	public void testTopicAliasOverReconnect() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		try {

			// 1. Client connects with Clean session=false, session expiry=Max (no expiry)
			MqttConnectionOptions options = new MqttConnectionOptions();
			options.setCleanStart(false);
			options.setSessionExpiryInterval(null);
			options.setAutomaticReconnect(true);
			options.setTopicAliasMaximum(1);
			final MqttAsyncClient asyncClient = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), clientId,
					DATA_STORE);
			proxy.enableProxy();
			IMqttToken connectToken = asyncClient.connect(options);
			connectToken.waitForCompletion(5000);
			boolean isConnected = asyncClient.isConnected();
			log.info("First Connection isConnected: " + isConnected);
			Assert.assertTrue(isConnected);

			// 2.1 - Send first message, this will have a topic string AND a topic alias.
			MqttMessage message = new MqttMessage(exampleContent.getBytes(), 2, false, new MqttProperties());
			log.info("Sending first message, should have a topic String and a topic alias.");
			IMqttDeliveryToken firstMessageToken = asyncClient.publish(exampleTopic, message);
			firstMessageToken.waitForCompletion(5000);

			// 2.2 - Send second message, this won't have a topic string, but will have a
			// topic Alias.
			log.info("Sending second message, should have a blank topic String and a topic alias.");
			IMqttDeliveryToken secondMessageToken = asyncClient.publish(exampleTopic, message);
			secondMessageToken.waitForCompletion(5000);

			// 3 - Drop the connection
			proxy.disableProxy();
			isConnected = asyncClient.isConnected();
			log.info("Proxy Disconnect isConnected: " + isConnected);
			Assert.assertFalse(isConnected);

			// 4 - Client uses Reconnect Logic to connect
			proxy.enableProxy();
			// give it some time to reconnect
			long currentTime = System.currentTimeMillis();
			int timeout = 4000;
			while (asyncClient.isConnected() == false) {
				long now = System.currentTimeMillis();
				if ((currentTime + timeout) < now) {
					log.warning("Timeout Exceeded");
					break;
				}
				Thread.sleep(500);
			}
			isConnected = asyncClient.isConnected();
			log.info("Proxy Re-Enabled isConnected: " + isConnected);
			Assert.assertTrue(isConnected);

			// 5 - Publish a final message, this message should have both a topic string AND
			// a topic Alias.
			log.info("Sending First message after reconnect, should have a topic String and a topic alias.");
			IMqttDeliveryToken thirdMessageToken = asyncClient.publish(exampleTopic, message);
			thirdMessageToken.waitForCompletion(5000);

			// We should still be connected.
			isConnected = asyncClient.isConnected();
			log.info("The client should still be connected: " + isConnected);
			Assert.assertTrue(isConnected);

			IMqttToken token = asyncClient.disconnect();
			token.waitForCompletion();
			Assert.assertFalse(asyncClient.isConnected());
		} catch (MqttException ex) {
			Assert.fail("An unexpected exception occurred during this test: " + ex.getMessage());
		}
	}

}
