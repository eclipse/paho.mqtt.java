package org.eclipse.paho.mqttv5.client.vertx.test.utilities;

import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.vertx.IMqttToken;
import org.eclipse.paho.mqttv5.client.vertx.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.vertx.MqttCallback;
import org.eclipse.paho.mqttv5.client.vertx.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.vertx.test.SubscribeTests;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Assert;

public class TestClientUtilities {
	
	static final Class<?> cclass = SubscribeTests.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);
	
	public static MqttAsyncClient connectAndGetClient(String serverURI, String clientId, MqttCallback callback,
			MqttConnectionOptions connectionOptions, int timeout) throws MqttException {
		MqttAsyncClient client = new MqttAsyncClient(serverURI, clientId);
		if (callback != null) {
			client.setCallback(callback);
		}
		log.info("Connecting: [serverURI: " + serverURI + ", ClientId: " + clientId + "]");
		IMqttToken connectToken;
		if (connectionOptions != null) {
			connectToken = client.connect(connectionOptions);
		} else {
			connectToken = client.connect();
		}

		connectToken.waitForCompletion(timeout);
		Assert.assertTrue(client.isConnected());
		log.info("Client: [" + clientId + "] is connected.");
		return client;
	}
	
	public static void disconnectAndCloseClient(MqttAsyncClient client, int timeout) throws MqttException {
		log.info("Disconnecting client: [" + client.getClientId() + "]");
		IMqttToken disconnectToken = client.disconnect();
		disconnectToken.waitForCompletion(timeout);
		Assert.assertFalse(client.isConnected());
		client.close();
		log.info("Client [" + client.getClientId() + "] disconnected and closed.");
	}

}
