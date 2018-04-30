package org.eclipse.paho.mqttv5.client.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.internal.MqttPersistentData;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.TestByteArrayMemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistable;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class PersistenceTests {
	private static URI serverURI;
	private static final String className = PersistenceTests.class.getName();
	private static final Logger log = Logger.getLogger(className);
	
	
	@Test
	/**
	 * Tests that an MQTTv5 client persists MQTTPublish Messages correctly,
	 * specifically that Topic Aliases are not persisted.
	 */
	public void persistMessageWithTopicAlias() throws URISyntaxException, MqttException {
		serverURI = TestProperties.getServerURI();
		TestByteArrayMemoryPersistence memoryPersistence = new TestByteArrayMemoryPersistence();

		// Create an MqttAsyncClient with a null Client ID.
		MqttAsyncClient client = new MqttAsyncClient(serverURI.toString(), "testClientId", memoryPersistence, null, null);
		
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setTopicAliasMaximum(10);
		
		IMqttToken connectToken = client.connect(options);
		connectToken.waitForCompletion(1000);
		Assert.assertTrue("The client should be connected.", client.isConnected());
		
		// Publish a message at QoS 2
		MqttMessage message = new MqttMessage("Test Message".getBytes(), 2, false, null);
		IMqttDeliveryToken deliveryToken = client.publish("testTopic", message);
		deliveryToken.waitForCompletion(1000);
		
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

	

	
}
