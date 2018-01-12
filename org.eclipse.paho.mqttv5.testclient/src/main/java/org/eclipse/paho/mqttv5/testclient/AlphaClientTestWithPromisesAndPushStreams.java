package org.eclipse.paho.mqttv5.testclient;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.alpha.IMqttToken;
import org.eclipse.paho.mqttv5.client.alpha.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttConnectionResult;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttSubscriptionResult;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.osgi.util.promise.Promise;

public class AlphaClientTestWithPromisesAndPushStreams {

	public static void main(String[] args) throws MqttException, InterruptedException, InvocationTargetException {
		String broker = "tcp://localhost:1883";
		String clientId = "TestV5Client";

		String topic = "MQTT Examples";
		String content = "Message from MqttPublishSample";
		int qos = 2;

		// MqttAsyncClient asyncClient = new MqttAsyncClient(broker, clientId,
		// persistence);

		// Test for Server Generated Client ID
		MqttAsyncClient asyncClient = new MqttAsyncClient(broker, clientId);
		MqttConnectionOptions connOpts = new MqttConnectionOptions();
		connOpts.setCleanSession(true);
		System.out.println("Connecting to broker: " + broker);


		IMqttToken<IMqttConnectionResult<Void>, Void> token = asyncClient.connect(connOpts);

		Promise<Optional<String>> result = token.getPromise().then(p -> {
				// Print the ConAck Properties
				printConnectDetails(p.getValue());
				// Do a retained send
				return asyncClient.publish(topic, content.getBytes(UTF_8), qos, true).getPromise();
			}).then(p ->  {
				System.out.println("Delivered");
				
				// Subscribe and decode the message
				return asyncClient.subscribe(topic, 2).getStream()
						// Give up after 10 seconds with no message
						.timeout(Duration.of(10, SECONDS))
						// Get the payload
						.map(msg -> msg.payload())
						// Turn the payload back into a String
						.map(b -> UTF_8.decode(b).toString())
						// Finish once we receive the first message
						.findFirst();
			});


		System.out.println("Received the message " + result.getValue().get());

		asyncClient.disconnect().getPromise().getValue();
		System.out.println("Disconnected");

	}

	public static void printSubscriptionDetails(IMqttSubscriptionResult<Void> result) {
		System.out.println("Subscription Response: [qos=" + Arrays.toString(result.getGrantedQos()) + "]");
//		System.out.println("Subscription Response: [reasonString=" + token.getReasonString() + ", user"
//				+ ", userDefinedProperties=" + token.getUserDefinedProperties());

	}

	public static void printConnectDetails(IMqttConnectionResult<Void> token) {
		
		System.out.println("Connection Response: [ sessionPresent=" + token.getSessionPresent() + "]");
		
//		System.out.println("Connection Response: [ sessionPresent=" + token.getSessionPresent() + ", responseInfo="
//				+ token.getResponseInformation() + ", assignedClientIdentifier=" + token.getAssignedClientIdentifier()
//				+ ", serverKeepAlive=" + token.getServerKeepAlive() + ", authMethod=" + token.getAuthMethod()
//				+ ", authData=" + token.getAuthData() + ", serverReference=" + token.getServerReference()
//				+ ", reasonString=" + token.getReasonString() + ", recieveMaximum=" + token.getRecieveMaximum()
//				+ ", topicAliasMaximum=" + token.getTopicAliasMaximum() + ", maximumQoS=" + token.getMaximumQoS()
//				+ ", retainAvailable=" + token.isRetainAvailable() + ", userDefinedProperties="
//				+ token.getUserDefinedProperties() + ", maxPacketSize=" + token.getMaximumPacketSize()
//				+ ", wildcardSubscriptionAvailable=" + token.isWildcardSubscriptionAvailable()
//				+ ", subscriptionIdentifiersAvailable=" + token.isSubscriptionIdentifiersAvailable()
//				+ ", sharedSubscriptionAvailable=" + token.isSharedSubscriptionAvailable() + "]");
	}

}
