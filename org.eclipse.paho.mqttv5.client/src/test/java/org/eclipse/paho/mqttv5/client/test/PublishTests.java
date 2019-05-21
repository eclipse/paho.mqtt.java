package org.eclipse.paho.mqttv5.client.test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.common.test.categories.MQTTV5Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@Category({OnlineTest.class, MQTTV5Test.class})
@RunWith(Parameterized.class)
public class PublishTests {

	static final Class<?> cclass = PublishTests.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private URI serverURI;
	private static MqttClientFactoryPaho clientFactory;
	private static String topicPrefix;
	
	@Parameters
	public static Collection<Object[]> data() throws Exception {
		
		return Arrays.asList(new Object[][] {     
            { TestProperties.getServerURI() }, { TestProperties.getWebSocketServerURI() }  
      });
		
	}
	
	public PublishTests(URI serverURI) {
		this.serverURI = serverURI;
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);

			clientFactory = new MqttClientFactoryPaho();
			clientFactory.open();
			topicPrefix = "Mqttv5PublishTests-" + UUID.randomUUID().toString() + "-";

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
	public void testPublishRC() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		String clientId = methodName;
		MqttAsyncClient asyncClient = new MqttAsyncClient(serverURI.toString(), clientId);

		// Connect to the server
		log.info("Connecting: [serverURI: " + serverURI + ", ClientId: " + clientId + "]");
		IMqttToken connectToken = asyncClient.connect();
		connectToken.waitForCompletion(5000);
		String clientId2 = asyncClient.getClientId();
		log.info("Client ID = " + clientId2);
		boolean isConnected = asyncClient.isConnected();
		log.info("isConnected: " + isConnected);

		// Publish a message to a random topic
		MqttMessage testMessage = new MqttMessage("Test Payload".getBytes(), 2, false, new MqttProperties());
		log.info("Publishing Message to: " + topicPrefix + methodName);
		IMqttDeliveryToken deliveryToken = asyncClient.publish(topicPrefix + methodName, testMessage);
		deliveryToken.waitForCompletion(5000);
		log.info(deliveryToken.getResponse().toString());
		log.info("Return codes: " + Arrays.toString(deliveryToken.getReasonCodes()));
		int[] expectedRC = new int[] {MqttReturnCode.RETURN_CODE_NO_MATCHING_SUBSCRIBERS, 0}; 
		Assert.assertArrayEquals(expectedRC, deliveryToken.getReasonCodes());

		log.info("Disconnecting...");
		IMqttToken disconnectToken = asyncClient.disconnect();
		disconnectToken.waitForCompletion(5000);
		Assert.assertFalse(asyncClient.isConnected());
		asyncClient.close();

	}
	
	@Test
	public void testPublishManyQoS0Messages() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		String clientId = methodName;
		MqttAsyncClient asyncClient = new MqttAsyncClient(serverURI.toString(), clientId);

		// Connect to the server
		log.info("Connecting: [serverURI: " + serverURI + ", ClientId: " + clientId + "]");
		IMqttToken connectToken = asyncClient.connect();
		connectToken.waitForCompletion(5000);
		String clientId2 = asyncClient.getClientId();
		log.info("Client ID = " + clientId2);
		boolean isConnected = asyncClient.isConnected();
		log.info("isConnected: " + isConnected);
		
		MqttMessage testMessage = new MqttMessage("Test Payload".getBytes(), 0, false, new MqttProperties());
		long lStartTime = System.nanoTime();
		int no_of_messages = 10000; // or 70000 
		for(int i = 0; i < no_of_messages; i++) {
			IMqttDeliveryToken deliveryToken = asyncClient.publish(topicPrefix + methodName, testMessage);
			try
			{
				deliveryToken.waitForCompletion(5000);
			} catch (Exception e) {
				System.out.println("wait failed "+i);
			}
		}

        long lEndTime = System.nanoTime();
        long output = lEndTime - lStartTime; 		//time elapsed
        log.info("Sending "+no_of_messages+" of messages  took : " + output / 1000000 + " milliseconds.");

		log.info("Disconnecting...");
		IMqttToken disconnectToken = asyncClient.disconnect();
		disconnectToken.waitForCompletion(5000);
		Assert.assertFalse(asyncClient.isConnected());
		asyncClient.close();

	}

}
