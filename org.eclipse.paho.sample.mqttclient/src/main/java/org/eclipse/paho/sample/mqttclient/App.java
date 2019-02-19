package org.eclipse.paho.sample.mqttclient;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.paho.sample.mqttclient.mqttv3.MqttV3Executor;
import org.eclipse.paho.sample.mqttclient.mqttv5.MqttV5Executor;

/**
 * A sample application that demonstrates how to use the Eclipse Paho MQTT v3.1
 * and v5 Client APIs.
 * 
 * It can be run from the command line in one of two modes:
 * <ul>
 * <li>as a publisher, sending a single message to a topic on the server.</li>
 * <li>as a subscriber, listening for messages from the server.</li>
 * <ul>
 * 
 * If the application is run with the -h parameter, then the help information is
 * displayed that describes all of the options / parameters.
 */
public class App {
	
	private static int actionTimeout = 5000; // The time in ms to wait for any action to timeout.
	
	/**
	 * The main entry point for the sample application.
	 * 
	 * This method handles parsing the arguments specified on the command line
	 * before performing the specified action.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Define Default Settings
		boolean quiet = false;
		boolean debug = false;
		int mqttVersion = 3;

		// Define CLI Options
		Options cliOptions = new Options();
		// Mode
		cliOptions.addOption("pub", "publish", false, "Send a message to a topic.");
		cliOptions.addOption("sub", "subscribe", false, "Subscribe to a topic.");
		// Debug / Verbosity
		cliOptions.addOption("d", "debug", false, "Enable debug messages.");
		cliOptions.addOption("Q", "quiet", false, "Don't print error messages.");
		// Help
		cliOptions.addOption("H", "help", false, "Prints help");
		// Connection
		cliOptions.addOption("h", "host", true,
				"MQTT broker URI to connect to. Defaults to tcp://iot.eclipse.org:1883. Use ws:// for Websockets, wss:// for secure Websockets and ssl:// for TLS encrypted TCP connections.");

		cliOptions.addOption("i", "id", true,
				"The ID to use for this client. Defaults to mqtt-client- appended with the process id.");
		cliOptions.addOption("k", "keepalive", true, "The Keepalive in seconds. Defaults to 60.");
		cliOptions.addOption("c", "clean-session", false,
				"Whether the client should connect with a clean session. Defaults to False.");
		cliOptions.addOption("mi", "max-inflight", true,
				"The Maximum number of inflight messages allowed in QoS 1 and 2 flows. Default is 10.");
		cliOptions.addOption("ar", "automatic-reconnect", false,
				"If set, the client will automatically attempt to reconnect if the connection is lost.");

		cliOptions.addOption("P", "password", true, "Provide a Password for the connection.");
		cliOptions.addOption("u", "username", true, "Provide a username for the connection.");
		cliOptions.addOption("v", "version", true,
				"Specify the Version of MQTT to use for this connection (3 or 5). Defaults to 3 (3.1.1)");
		cliOptions.addOption("V", "verbose", false, "When recieving incoming messages, print the incoming topic name.");
		cliOptions.addOption("wp", "will-payload", true,
				"Payload for the client Will, which is sent by the broker in case of unexpected disconnection. If not given, and will-topic is set, a zero length message will be sent.");
		cliOptions.addOption("wq", "will-qos", true, "QoS level for the client Will message.");
		cliOptions.addOption("wr", "will-retain", false, "If given, Will make the client Will retained.");
		cliOptions.addOption("wt", "will-topic", true, "The topic on which to publish the client Will.");
		// SSL
		cliOptions.addOption("cafile", true,
				"Path to a file containing trusted CA certificates to enable encrypted communication.");
		cliOptions.addOption("capath", true,
				"Path to a directory containing trusted CA certificates to enable encrypted communication.");
		cliOptions.addOption("cert", true,
				"Path to a file containing the client certificate for authentication, if required by peer.");
		cliOptions.addOption("key", true, "Client private key for authentication, if required by server.");
		cliOptions.addOption("insecure", false,
				"Do not check that the server certificate hostname matches the remote hostname. Using this option means that you cannot be sure that the remote host is the server you wish to connect to and so is insecure. Do not use this option in a production environment.");

		// Publish
		cliOptions.addOption("f", "file", true, "Send the contents of a file as the message.");
		cliOptions.addOption("l", "stdin-line", false,
				"Read messages from stdin, sending a separate message for each line.");
		cliOptions.addOption("m", "message", true, "The message payload to send.");
		cliOptions.addOption("M", "max-inflight", true, "The maximum inflight messages for QoS 1/2.");
		cliOptions.addOption("n", "null", false, "Send a null (zero length) message.");
		cliOptions.addOption("r", "retain", false, "Message should be retained.");
		cliOptions.addOption("s", "stdin", false, "Read message from stdin, sending the entire input as a message.");

		// Subscribe

		// General
		cliOptions.addOption("t", "topic", true, "MQTT Topic to Publish / Subscribe to.");
		cliOptions.addOption("q", "qos", true, "Quality of Service level to use for all messages. Defaults to 0.");

		// cliOptions.addOption("", "", true, "");

		// Parse and Set Options
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(cliOptions, args);

			// Print Help
			if (line.hasOption("help")) {
				HelpFormatter helpFormatter = new HelpFormatter();
				helpFormatter.printHelp("mqtt-client", cliOptions);
				System.exit(0);
			}

			// Process Verbosity / Debug Options
			if (line.hasOption("debug")) {
				debug = true;
			}
			if (line.hasOption("quiet")) {
				quiet = true;
			}
			if (line.hasOption("version")) {
				mqttVersion = Integer.parseInt(line.getOptionValue("version"));
				if (mqttVersion != 3 && mqttVersion != 5) {
					System.err.println("MQTT version must be either 3 or 5");
					System.exit(1);
				}
			}

			// Get Mode
			Mode mode = Mode.PUB;
			if (line.hasOption("pub") && !line.hasOption("sub")) {
				// Process Publish Arguments
				mode = Mode.PUB;
			} else if (line.hasOption("sub") && !line.hasOption("pub")) {
				// Process Subscribe Arguments
				mode = Mode.SUB;
			} else {
				System.err.println("Please use either the -pub (--publish) OR -sub (--subscribe) modes");
				System.exit(1);
			}

			if (mqttVersion == 3) {
				MqttV3Executor v3Executor = new MqttV3Executor(line, mode, debug, quiet, actionTimeout);
				v3Executor.execute();
			} else if (mqttVersion == 5) {
				MqttV5Executor v5Executor = new MqttV5Executor(line, mode, debug, quiet, actionTimeout);
				v5Executor.execute();
			}

		} catch (ParseException exp) {
			System.err.println("Parsing Failed. Reason: " + exp.getMessage());
		}
	}

}
