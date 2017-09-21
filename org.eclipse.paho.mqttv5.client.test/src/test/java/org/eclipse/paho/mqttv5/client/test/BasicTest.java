package org.eclipse.paho.mqttv5.client.test;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BasicTest {

	static final Class<?> cclass = BasicTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static URI serverURI;
	private static MqttClientFactoryPaho clientFactory;
	private static String topicPrefix;

	private String[] topics = { "TopicA", "TopicA/B", "Topic/C", "TopicA/C", "/TopicA" };
	private String[] wildTopics = { "TopicA/+", "+/C", "#", "/#", "/+", "+/+", "TopicA/#" };
	private String[] noSubscribeTopics = { "test/nosubscribe" };

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);

			serverURI = TestProperties.getServerURI();
			clientFactory = new MqttClientFactoryPaho();
			clientFactory.open();
			topicPrefix = "BasicTest-" + UUID.randomUUID().toString() + "-";

		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			throw exception;
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		try {
			if (clientFactory != null) {
				clientFactory.close();
				clientFactory.disconnect();
			}
		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
		}
	}

	@Test
	public void testConnect() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		Callbacks callbacks = new Callbacks(log);

		IMqttClient client = null;

		try {
			String clientId = methodName;
			client = clientFactory.createMqttClient(serverURI, clientId);

			log.info("Connecting...(serverURI: " + serverURI + ", ClientId: " + clientId);
			client.setCallback(callbacks);
			
			client.connect();

			String clientId2 = client.getClientId();
			log.info("Client ID = " + clientId2);

			boolean isConnected = client.isConnected();
			log.info("isConnected: " + isConnected);
			Assert.assertTrue(isConnected);
			
			String id = client.getServerURI();
			log.info("ServerURI = " + id);
			MqttSubscription sub = new MqttSubscription(topics[0]);
			client.subscribeWithResponse(new MqttSubscription[] {sub}, new IMqttMessageListener[] {callbacks}).waitForCompletion();
						
			
			client.publish(topics[0], new MqttMessage("qos0".getBytes(), 0, false));
			
			
			log.info("Publish complete...");
			Thread.sleep(2000);
						
			log.info("Disconnecting...");
			client.disconnect(0);

			Assert.assertEquals(1, callbacks.getMessages().size());
			
		} catch (MqttException exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			Assert.fail("Unexpected exception: " + exception);
		} finally {
			if (client != null) {
				log.info("Close...");
				client.close();
			}
		}

	}

}
