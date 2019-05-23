package org.eclipse.paho.mqttv5.client.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.common.test.categories.MQTTV5Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.MqttV5Receiver;
import org.eclipse.paho.mqttv5.client.test.utilities.TestClientUtilities;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.client.test.utilities.MqttV5Receiver.ReceivedMessage;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category({OnlineTest.class, MQTTV5Test.class})
public class SubscribeTests {

	static final Class<?> cclass = SubscribeTests.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static URI serverURI;
	private static MqttClientFactoryPaho clientFactory;
	private static String topicPrefix;

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
	public void testPublishRecieveUserProperties() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		String clientId = methodName;
		String exampleKey = "exampleKey";
		String exampleValue = "exampleValue";
		MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(clientId, LoggingUtilities.getPrintStream());
		MqttAsyncClient asyncClient = TestClientUtilities.connectAndGetClient(serverURI.toString(), clientId, mqttV5Receiver, null, 5000);

		// Subscribe to a topic
		log.info("Subscribing to: " + topicPrefix + methodName);
		MqttSubscription subscription = new MqttSubscription(topicPrefix + methodName);
		IMqttToken subscribeToken = asyncClient.subscribe(new MqttSubscription[] { subscription });
		subscribeToken.waitForCompletion(5000);

		// Publish a message to a random topic
		MqttProperties messageProps = new MqttProperties();
		List<UserProperty> userProps = new ArrayList<UserProperty>();
		userProps.add(new UserProperty(exampleKey, exampleValue));

		messageProps.setUserProperties(userProps);
		MqttMessage testMessage = new MqttMessage("Test Payload".getBytes(), 2, false, messageProps);
		log.info("Publishing Message with User Properties to: " + topicPrefix + methodName);
		IMqttToken deliveryToken = asyncClient.publish(topicPrefix + methodName, testMessage);
		deliveryToken.waitForCompletion(5000);

		log.info("Waiting for delivery and validating message.");
		ReceivedMessage receivedMessage = mqttV5Receiver.receiveNext(10000);
		MqttProperties receivedProps = receivedMessage.message.getProperties();
		Assert.assertEquals(1, receivedProps.getUserProperties().size());
		UserProperty recievedUserProps = receivedProps.getUserProperties().get(0);
		Assert.assertEquals(exampleKey, recievedUserProps.getKey());
		Assert.assertEquals(exampleValue, recievedUserProps.getValue());
		log.info("Received User Property: " + recievedUserProps.toString());

		TestClientUtilities.disconnectAndCloseClient(asyncClient, 5000);

	}

}
