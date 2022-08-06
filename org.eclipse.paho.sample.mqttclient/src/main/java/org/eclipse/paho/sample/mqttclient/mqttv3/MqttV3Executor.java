package org.eclipse.paho.sample.mqttclient.mqttv3;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.sample.mqttclient.Mode;

public class MqttV3Executor implements MqttCallback {

	MqttV3Connection v3ConnectionParameters;
	MqttV3Publish v3PublishParameters;
	MqttV3Subscribe v3SubscriptionParameters;
	boolean quiet = false;
	boolean debug = false;
	MqttAsyncClient v3Client;
	Mode mode;
	private int actionTimeout;

	// To allow a graceful disconnect.
	final Thread mainThread = Thread.currentThread();
	static volatile boolean keepRunning = true;

	/**
	 * Initialises the MQTTv3 Executor
	 * @param line - Command Line Parameters
	 * @param mode - The mode to run in (PUB / SUB)
	 * @param debug - Whether to print debug data to the console
	 * @param quiet - Whether to hide error messages
	 * @param actionTimeout - How long to wait to complete an action before failing.
	 */
	public MqttV3Executor(CommandLine commandLineParams, Mode mode, boolean debug, boolean quiet, int actionTimeout) {
		try {
			this.v3ConnectionParameters = new MqttV3Connection(commandLineParams);
			if (mode == Mode.PUB) {
				this.v3PublishParameters = new MqttV3Publish(commandLineParams);
			}
			if (mode == Mode.SUB) {
				this.v3SubscriptionParameters = new MqttV3Subscribe(commandLineParams);
			}
			this.debug = debug;
			this.quiet = quiet;
			this.mode = mode;
			this.actionTimeout = actionTimeout;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void execute() {
		try {

			// Create Client
			this.v3Client = new MqttAsyncClient(this.v3ConnectionParameters.getHostURI(),
					this.v3ConnectionParameters.getClientID(), new MemoryPersistence());
			this.v3Client.setCallback(this);

			// Connect to Server
			logMessage(String.format("Connecting to MQTT Broker: %s, Client ID: %s", v3Client.getServerURI(),
					v3Client.getClientId()), true);
			IMqttToken connectToken = v3Client.connect(v3ConnectionParameters.getConOpts());
			connectToken.waitForCompletion(actionTimeout);

			// Execute action based on mode
			if (mode == Mode.PUB) {
				if (v3PublishParameters.isStdInLine() == true) {
					logMessage(String.format("Publishing lines from STDIN to %s", v3PublishParameters.getTopic()),
							true);
					addShutdownHook();
					InputStreamReader isReader = new InputStreamReader(System.in);
					BufferedReader bufReader = new BufferedReader(isReader);

					while (keepRunning) {
						String inputStr = null;
						try {
							if ((inputStr = bufReader.readLine()) != null) {
								publishMessage(inputStr.getBytes(), this.v3PublishParameters.getQos(),
										this.v3PublishParameters.isRetain(), this.v3PublishParameters.getTopic());
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					disconnectClient();
					closeClientAndExit();

				} else if (v3PublishParameters.isStdInWhole() ==  true) {
					logMessage(String.format("Publishing all input from STDIN to %s", v3PublishParameters.getTopic()),
							true);
					addShutdownHook();
					InputStreamReader isReader = new InputStreamReader(System.in);
					BufferedReader bufReader = new BufferedReader(isReader);

					ByteArrayOutputStream out = new ByteArrayOutputStream();
					int inputChar;
					try {
						while ((inputChar = bufReader.read()) != -1) {
							out.write(inputChar);
						}
						byte[] payload = out.toByteArray();
						publishMessage(payload, this.v3PublishParameters.getQos(), this.v3PublishParameters.isRetain(),
								this.v3PublishParameters.getTopic());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					disconnectClient();
					closeClientAndExit();
					
					
				} else if(v3PublishParameters.getFile() != null) {
					String filename = v3PublishParameters.getFile();
					logMessage(String.format("Publishing file from %s to %s", filename, v3PublishParameters.getTopic()), true);
					Path path = Paths.get(filename);
					try {
						byte[] data  = Files.readAllBytes(path);
						publishMessage(data, this.v3PublishParameters.getQos(),
								this.v3PublishParameters.isRetain(), this.v3PublishParameters.getTopic());

						disconnectClient();

						// Close the client
						closeClientAndExit();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					

				} else {
					// Publish a message
					logMessage(String.format("Publishing message to %s", v3PublishParameters.getTopic()), true);
					publishMessage(this.v3PublishParameters.getPayload(), this.v3PublishParameters.getQos(),
							this.v3PublishParameters.isRetain(), this.v3PublishParameters.getTopic());

					disconnectClient();

					// Close the client
					closeClientAndExit();
				}

			} else if (mode == Mode.SUB) {
				// Subscribe to a topic
				logMessage(String.format("Subscribing to %s, with QoS %d", v3SubscriptionParameters.getTopic(),
						v3SubscriptionParameters.getQos()), true);
				IMqttToken subToken = this.v3Client.subscribe(v3SubscriptionParameters.getTopic(),
						v3SubscriptionParameters.getQos());
				subToken.waitForCompletion(actionTimeout);

				addShutdownHook();

				while (keepRunning) {
					// Do nothing
				}
				disconnectClient();
				closeClientAndExit();

			}

		} catch (MqttException e) {
			// TODO Auto-generated catch block
			logError(e.getMessage());
		}
	}

	/**
	 * Simple helper function to publish a message.
	 * 
	 * @param payload
	 * @param qos
	 * @param retain
	 * @param topic
	 * @throws MqttPersistenceException
	 * @throws MqttException
	 */
	private void publishMessage(byte[] payload, int qos, boolean retain, String topic)
			throws MqttPersistenceException, MqttException {
		MqttMessage v3Message = new MqttMessage(payload);
		v3Message.setQos(qos);
		v3Message.setRetained(retain);
		IMqttDeliveryToken deliveryToken = v3Client.publish(topic, v3Message);
		deliveryToken.waitForCompletion(actionTimeout);
	}

	/**
	 * Log a message to the console, nothing fancy.
	 * 
	 * @param message
	 * @param isDebug
	 */
	private void logMessage(String message, boolean isDebug) {
		if ((this.debug == true && isDebug == true) || isDebug == false) {
			System.out.println(message);
		}
	}

	/**
	 * Log an error to the console
	 * 
	 * @param error
	 */
	private void logError(String error) {
		if (this.quiet == false) {
			System.err.println(error);
		}
	}

	private void disconnectClient() throws MqttException {
		// Disconnect
		logMessage("Disconnecting from server.", true);
		IMqttToken disconnectToken = v3Client.disconnect();
		disconnectToken.waitForCompletion(actionTimeout);
	}

	private void closeClientAndExit() {
		// Close the client
		logMessage("Closing Connection.", true);
		try {
			this.v3Client.close();
			logMessage("Client Closed.", true);
			System.exit(0);
			mainThread.join();
		} catch (MqttException | InterruptedException e) {
			// End the Application
			System.exit(1);
		}

	}

	/**
	 * Adds a shutdown hook, that will gracefully disconnect the client when a
	 * CTRL+C rolls in.
	 */
	public void addShutdownHook() {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				keepRunning = false;
			}
		});
	}

	@Override
	public void connectionLost(Throwable cause) {

		if (v3ConnectionParameters.isAutomaticReconnectEnabled()) {
			logMessage(String.format("The connection to the server was lost, cause: %s. Waiting to reconnect.",
					cause.getMessage()), true);
		} else {
			logMessage(String.format("The connection to the server was lost, cause: %s. Closing Client",
					cause.getMessage()), true);
			closeClientAndExit();
		}

	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String messageContent = new String(message.getPayload());
		if (v3SubscriptionParameters.isVerbose()) {
			logMessage(String.format("%s %s", topic, messageContent), false);
		} else {
			logMessage(messageContent, false);
		}

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//logMessage(String.format("Message %d was delivered.", token.getMessageId()), true);
	}

}
