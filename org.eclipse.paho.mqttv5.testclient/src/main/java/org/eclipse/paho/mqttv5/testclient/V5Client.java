package org.eclipse.paho.mqttv5.testclient;


import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

public class V5Client implements MqttCallback {

	private String broker = "tcp://localhost:1883";
	//private String broker = "tcp://9.20.87.69:1883";
	private String clientId = "TestV5Client";
	private String topic = "MQTT Examples";
	private String content = "Message from MqttPublishSample";
	private String willContent = "I've Disconnected, sorry!";
	private int qos = 1;
	Object runningLock;
	boolean running = true;
	int x = 0;
	public V5Client() throws InterruptedException {

		try {
			MemoryPersistence persistence = new MemoryPersistence();
			MqttAsyncClient asyncClient = new MqttAsyncClient(broker, clientId, persistence);

			// Lets build our Connection Options:
			MqttConnectionOptionsBuilder conOptsBuilder = new MqttConnectionOptionsBuilder();
			MqttConnectionOptions conOpts = conOptsBuilder.serverURI(broker).cleanSession(true)
					.sessionExpiryInterval(120).automaticReconnect(true)
					.will(topic, new MqttMessage(willContent.getBytes(), qos, false)).topicAliasMaximum(1000).build();
			asyncClient.setCallback(this);


			System.out.println("Connecting to broker: " + broker);

			asyncClient.connect(conOpts, null, new MqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					System.out.println("Connected");

					printConnectDetails((MqttToken) asyncActionToken);
					try {
						IMqttToken subToken = asyncClient.subscribe(topic, qos);
						subToken.waitForCompletion();
						printSubscriptionDetails((MqttToken) subToken);
						MqttMessage msg = new MqttMessage(content.getBytes());
						msg.setQos(qos);
						asyncClient.publish(topic, msg);
					} catch (MqttException e) {
						System.err.println("Exception Occured whilst Subscribing:");
						e.printStackTrace();
					}

				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					System.out.println("Failed to Connect: " + exception.getLocalizedMessage());

				}
			});
			
			

			while (running) {

				// asyncClient.publish(topic, new MqttMessage(content.getBytes(), 2, false));
				Thread.sleep(1000);
				System.out.println("Sending Message: " + x);
				String message = content + " " + x;
				if(x == 5) {
					message = "FINISH";
					running = false;
				}
				asyncClient.publish(topic, new MqttMessage(message.getBytes(), 2, false));
				x++;

			}
			asyncClient.disconnect(5000);
			System.out.println("Disconnected");
			asyncClient.close();
			System.exit(0);

		} catch (MqttException e) {
			System.err.println("Exception Occured whilst connecting the client: ");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws MqttException, InterruptedException {
		// Create and start this sample client
		V5Client client = new V5Client();

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

	

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String incomingMessage = new String(message.getPayload());
		System.out.println("Incoming Message: [" +incomingMessage + "], topic:[" + topic + "]");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		System.out.println("Delivery Complete: " + token.getMessageId());

	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		System.out.println("Disconnection Complete!");
		
	}

	@Override
	public void mqttErrorOccured(MqttException exception) {
		// TODO Auto-generated method stub
		
	}

}
