package org.eclipse.paho.sample.mqttclient.mqttv5;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.IMqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.sample.mqttclient.Mode;

public class MqttV5Executor implements MqttCallback {

	MqttV5Connection connectionParams;
	MqttV5Publish publishParams;
	MqttV5Subscribe subscribeParams;
	boolean quiet = false;
	boolean debug = false;
	MqttAsyncClient v5Client;
	Mode mode;
	private int actionTimeout;

	// To allow for a graceful disconnect
	final Thread mainThread = Thread.currentThread();
	static volatile boolean keepRunning = true;

	/**
	 * Initialises the MQTTv5 Executor
	 * 
	 * @param line
	 *            - Command Line Parameters
	 * @param mode
	 *            - The mode to run in (PUB / SUB)
	 * @param debug
	 *            - Whether to print debug data to the console
	 * @param quiet
	 *            - Whether to hide error messages
	 * @param actionTimeout
	 *            - How long to wait to complete an action before failing.
	 */
	public MqttV5Executor(CommandLine commandLineParams, Mode mode, boolean debug, boolean quiet, int actionTimeout) {

		this.connectionParams = new MqttV5Connection(commandLineParams);
		if (mode == Mode.PUB) {
			this.publishParams = new MqttV5Publish(commandLineParams);
		}
		if (mode == Mode.SUB) {
			this.subscribeParams = new MqttV5Subscribe(commandLineParams);
		}
		this.debug = debug;
		this.quiet = quiet;
		this.mode = mode;
		this.actionTimeout = actionTimeout;
	}

	public void execute() {
		try {
			// Create the client.
			MemoryPersistence persistence = new MemoryPersistence();
			this.v5Client = new MqttAsyncClient(connectionParams.getHostURI(), connectionParams.getClientID(), persistence);
			this.v5Client.setCallback(this);

			// Connect to the server
			logMessage(String.format("Connecting to MQTT Broker %s, Client ID: %s", v5Client.getServerURI(),
					v5Client.getClientId()), true);

			IMqttToken connectToken = v5Client.connect(connectionParams.getConOpts());
			connectToken.waitForCompletion(actionTimeout);
			
			
			// Execute Action based on mode.
			if (mode == Mode.PUB) {
				if (publishParams.isStdInLine() == true) {
					logMessage(String.format("Publishing lines from STDIN to %s", publishParams.getTopic()), true);
					addShutdownHook();
					InputStreamReader isReader = new InputStreamReader(System.in);
					BufferedReader bufReader = new BufferedReader(isReader);

					while (keepRunning) {
						String inputStr = null;
						if ((inputStr = bufReader.readLine()) != null) {
							publishMessage(inputStr.getBytes(), this.publishParams.getQos(),
									this.publishParams.isRetain(), this.publishParams.getTopic());
						}
					}
					disconnectClient();
					closeClientAndExit();
				} else if(publishParams.isStdInWhole() == true) {
					logMessage(String.format("Publishing all input from STDIN to %s", publishParams.getTopic()), true);
					addShutdownHook();
					InputStreamReader isReader = new InputStreamReader(System.in);
					BufferedReader bufReader = new BufferedReader(isReader);
					
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					int inputChar;
					while((inputChar = bufReader.read()) != -1) {
						out.write(inputChar);
					}
					byte[] payload = out.toByteArray();
					publishMessage(payload, this.publishParams.getQos(), this.publishParams.isRetain(), this.publishParams.getTopic());
					disconnectClient();
					closeClientAndExit();
				} else if(publishParams.getFile() != null) {
					String filename = publishParams.getFile();
					logMessage(String.format("Publishing file from %s to %s.", filename, publishParams.getTopic()), true);
					Path path = Paths.get(filename);
					byte[] data = Files.readAllBytes(path);
					publishMessage(data, this.publishParams.getQos(), this.publishParams.isRetain(), this.publishParams.getTopic());
					disconnectClient();
					closeClientAndExit();
				}
			} else if(mode == Mode.SUB) {
				logMessage(String.format("Subscribing to %s with QoS %d.", subscribeParams.getTopic(), subscribeParams.getQos()), true);
				IMqttToken subToken = this.v5Client.subscribe(subscribeParams.getTopic(), subscribeParams.getQos());
				subToken.waitForCompletion(actionTimeout);
				addShutdownHook();
				while(keepRunning) {
					// Do nothing
				}
				disconnectClient();
				closeClientAndExit();
			}

		} catch (MqttException | IOException ex) {
			logError(String.format("Exception occured whilst attempting to publish: %s", ex.getMessage()));
			ex.printStackTrace();
			closeClientAndExit();
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
		MqttMessage v5Message = new MqttMessage(payload);
		v5Message.setQos(qos);
		v5Message.setRetained(retain);
		IMqttDeliveryToken deliveryToken = v5Client.publish(topic, v5Message);
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

	private void disconnectClient() throws MqttException {
		// Disconnect
		logMessage("Disconnecting from server.", true);
		IMqttToken disconnectToken = v5Client.disconnect();
		disconnectToken.waitForCompletion(actionTimeout);
	}

	private void closeClientAndExit() {
		// Close the client
		logMessage("Closing Connection.", true);
		try {
			this.v5Client.close();
			logMessage("Client Closed.", true);
			System.exit(0);
			mainThread.join();
		} catch (MqttException | InterruptedException e) {
			// End the Application
			System.exit(1);
		}

	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		String cause = null;
		if (disconnectResponse.getException().getMessage() != null) {
			cause = disconnectResponse.getException().getMessage();
		} else {
			cause = disconnectResponse.getReasonString();
		}
		if (connectionParams.isAutomaticReconnectEnabled()) {
			logMessage(String.format("The connection to the server was lost, cause: %s. Waiting to reconnect.", cause),
					true);
		} else {
			logMessage(String.format("The connection to the server was lost, cause: %s. Closing Client", cause), true);
			closeClientAndExit();
		}

	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		logError(String.format("An MQTT error occurred: %s", exception.getMessage()));
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String messageContent = new String(message.getPayload());
		if (subscribeParams.isVerbose()) {
			logMessage(String.format("%s %s", topic, messageContent), false);
		} else {
			logMessage(messageContent, false);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// No Required Implementation
		// logMessage(String.format("Message %d was delivered.", token.getMessageId()),
		// true);
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		logMessage(String.format("Connection to %s complete. Reconnect=%b", serverURI, reconnect), true);
	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		logError(String.format("Auth packet received, this client does not currently support them. Reason Code: %d.",
				reasonCode));
	}

}
