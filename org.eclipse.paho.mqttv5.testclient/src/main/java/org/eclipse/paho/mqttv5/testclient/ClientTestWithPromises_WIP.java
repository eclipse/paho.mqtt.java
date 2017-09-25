package org.eclipse.paho.mqttv5.testclient;

import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.osgi.util.promise.Promise;

public class ClientTestWithPromises_WIP {

	public static void main(String[] args) throws MqttException, InterruptedException {
		String broker = "tcp://localhost:1883";
		String clientId = "TestV5Client";

		String topic = "MQTT Examples";
		String content = "Message from MqttPublishSample";
		int qos = 2;
		boolean running = true;

		MemoryPersistence persistence = new MemoryPersistence();

		// MqttAsyncClient asyncClient = new MqttAsyncClient(broker, clientId,
		// persistence);

		// Test for Server Generated Client ID
		MqttAsyncClient asyncClient = new MqttAsyncClient(broker, clientId, persistence);
		MqttConnectionOptions connOpts = new MqttConnectionOptions();
		connOpts.setCleanSession(true);
		System.out.println("Connecting to broker: " + broker);

		asyncClient.setCallback(new MqttCallback() {

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				// TODO Auto-generated method stub
				System.out.println("Incoming Message: " + new String(message.getPayload()));

			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub
				System.out.println("Delivery Complete: " + token.getMessageId());

			}

			@Override
			public void disconnected(MqttDisconnectResponse disconnectResponse) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mqttErrorOccured(MqttException exception) {
				// TODO Auto-generated method stub
				
			}
		});

		MqttSubscription sub = new MqttSubscription(topic, 1);

		Promise<MqttToken> promise = asyncClient.connectWithPromise(connOpts);

		promise.then(t -> {
			// Print the ConAck Properties
			printConnectDetails(t.getValue());

			// Prepare our initial Subscription
			Promise<MqttToken> subPromise = asyncClient.subscribeWithPromise(sub, null);

			subPromise.then(p -> {
				System.out.println("Doing sub Print");
				printSubscriptionDetails(p.getValue());
				return null;

			}, p -> {
				System.out.println("SubErr");
				p.getFailure().printStackTrace();
			});

			return null;
		}, t -> t.getFailure().printStackTrace());

		while (running) {

		}
		asyncClient.disconnect();
		System.out.println("Disconnected");

	}

	public static void printSubscriptionDetails(MqttToken token) {
		System.out.println("Subscription Response: [reasonString=" + token.getReasonString() + ", user"
				+ ", userDefinedProperties=" + token.getUserDefinedProperties());

	}

	public static void printConnectDetails(MqttToken token) {
		System.out.println("Connection Response: [ sessionPresent=" + token.getSessionPresent() + ", responseInfo="
				+ token.getResponseInformation() + ", assignedClientIdentifier=" + token.getAssignedClientIdentifier()
				+ ", serverKeepAlive=" + token.getServerKeepAlive() + ", authMethod=" + token.getAuthMethod()
				+ ", authData=" + token.getAuthData() + ", serverReference=" + token.getServerReference()
				+ ", reasonString=" + token.getReasonString() + ", recieveMaximum=" + token.getRecieveMaximum()
				+ ", topicAliasMaximum=" + token.getTopicAliasMaximum() + ", maximumQoS=" + token.getMaximumQoS()
				+ ", retainAvailable=" + token.isRetainAvailable() + ", userDefinedProperties="
				+ token.getUserDefinedProperties() + ", maxPacketSize=" + token.getMaximumPacketSize()
				+ ", wildcardSubscriptionAvailable=" + token.isWildcardSubscriptionAvailable()
				+ ", subscriptionIdentifiersAvailable=" + token.isSubscriptionIdentifiersAvailable()
				+ ", sharedSubscriptionAvailable=" + token.isSharedSubscriptionAvailable() + "]");
	}

}
