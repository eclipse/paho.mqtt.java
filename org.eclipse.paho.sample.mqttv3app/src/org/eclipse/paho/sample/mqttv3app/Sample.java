/*
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */

package org.eclipse.paho.sample.mqttv3app;

import java.io.IOException;
import java.sql.Timestamp;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;


/**
 * This sample application demonstrates basic usage
 * of the MQTT v3 Client api.
 *
 * It can be run in one of two modes:
 *  - as a publisher, sending a single message to a topic on the server
 *  - as a subscriber, listening for messages from the server
 *
 */
public class Sample implements MqttCallback {

	// default values for broker connection
	static String broker = "m2m.eclipse.org";
	static int port = 1883;

	/**
	 * The main entry point of the sample.
	 *
	 * This method handles parsing the arguments specified on the
	 * command-line before performing the specified action.
	 */
	public static void main(String[] args) {

		// Default settings:
		boolean quietMode = false;
		String action = "publish";
		String topic = "";
		String message = "Message from MQTTv3 Java client";
		int qos = 2;
		String broker = "m2m.eclipse.org";
		int port = 1883;
		String clientId = null;

		// Parse the arguments -
		for (int i=0; i<args.length; i++) {
			// Check this is a valid argument
			if (args[i].length() == 2 && args[i].startsWith("-")) {
				char arg = args[i].charAt(1);
				// Handle the no-value arguments
				switch(arg) {
				case 'h': case '?':	printHelp(); return;
				case 'q': quietMode = true;	continue;
				}
				// Validate there is a value associated with the argument
				if (i == args.length -1 || args[i+1].charAt(0) == '-') {
					System.out.println("Missing value for argument: "+args[i]);
					printHelp();
					return;
				}
				switch(arg) {
				case 'a': action = args[++i];                 break;
				case 't': topic = args[++i];                  break;
				case 'm': message = args[++i];                break;
				case 's': qos = Integer.parseInt(args[++i]);  break;
				case 'b': broker = args[++i];                 break;
				case 'p': port = Integer.parseInt(args[++i]); break;
				case 'i': clientId = args[++i];               break;
				default:
					System.out.println("Unrecognised argument: "+args[i]);
					printHelp();
					return;
				}
			} else {
				System.out.println("Unrecognised argument: "+args[i]);
				printHelp();
				return;
			}
		}

		// Validate the provided arguments
		if (!action.equals("publish") && !action.equals("subscribe")) {
			System.out.println("Invalid action: "+action);
			printHelp();
			return;
		}
		if (qos < 0 || qos > 2) {
			System.out.println("Invalid QoS: "+qos);
			printHelp();
			return;
		}
		if (topic.equals("")) {
			// Set the default topic according to the specified action
			if (action.equals("publish")) {
				topic = "Sample/Java/v3";
			} else {
				topic = "Sample/#";
			}
		}

		String url = "tcp://"+broker+":"+port;

		if (clientId == null || clientId.equals("")) {
			clientId = "SampleJavaV3_"+action;
		}

		// With a valid set of arguments, the real work of
		// driving the client API can begin

		try {
			// Create an instance of the Sample client wrapper
			Sample sampleClient = new Sample(url,clientId,quietMode);

			// Perform the specified action
			if (action.equals("publish")) {
				sampleClient.publish(topic,qos,message.getBytes());
			} else if (action.equals("subscribe")) {
				sampleClient.subscribe(topic,qos);
			}
		} catch(MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}
	}

	// Private instance variables
	private MqttClient client;
	private String brokerUrl;
	private boolean quietMode;
	private MqttConnectOptions conOpt;

	/**
	 * Constructs an instance of the sample client wrapper
	 * @param brokerUrl the url to connect to
	 * @param clientId the client id to connect with
	 * @param quietMode whether debug should be printed to standard out
	 * @throws MqttException
	 */
    public Sample(String brokerUrl, String clientId, boolean quietMode) throws MqttException {
    	this.brokerUrl = brokerUrl;
    	this.quietMode = quietMode;

    	//This sample stores files in a temporary directory...
    	//..a real application ought to store them somewhere
    	//where they are not likely to get deleted or tampered with
    	String tmpDir = System.getProperty("java.io.tmpdir");
    	MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

    	try {
    		// Construct the object that contains connection parameters
    		// such as cleansession and LWAT
	    	conOpt = new MqttConnectOptions();
	    	conOpt.setCleanSession(false);

    		// Construct the MqttClient instance
			client = new MqttClient(this.brokerUrl,clientId, dataStore);

			// Set this wrapper as the callback handler
	    	client.setCallback(this);

		} catch (MqttException e) {
			e.printStackTrace();
			log("Unable to set up client: "+e.toString());
			System.exit(1);
		}
    }

    /**
     * Performs a single publish
     * @param topicName the topic to publish to
     * @param qos the qos to publish at
     * @param payload the payload of the message to publish
     * @throws MqttException
     */
    public void publish(String topicName, int qos, byte[] payload) throws MqttException {

    	// Connect to the server
    	client.connect();
    	log("Connected to "+brokerUrl + " with client ID "+client.getClientId());

    	// Get an instance of the topic
    	MqttTopic topic = client.getTopic(topicName);

   		MqttMessage message = new MqttMessage(payload);
    	message.setQos(qos);

    	// Publish the message
    	String time = new Timestamp(System.currentTimeMillis()).toString();
    	log("Publishing at: "+time+ " to topic \""+topicName+"\" qos "+qos);
    	MqttDeliveryToken token = topic.publish(message);

    	// Wait until the message has been delivered to the server
    	token.waitForCompletion();

    	// Disconnect the client
    	client.disconnect();
    	log("Disconnected");
    }

    /**
     * Subscribes to a topic and blocks until Enter is pressed
     * @param topicName the topic to subscribe to
     * @param qos the qos to subscibe at
     * @throws MqttException
     */
    public void subscribe(String topicName, int qos) throws MqttException {

    	// Connect to the server
    	client.connect();
    	log("Connected to "+brokerUrl+" with client ID "+client.getClientId());

    	// Subscribe to the topic
    	log("Subscribing to topic \""+topicName+"\" qos "+qos);
    	client.subscribe(topicName, qos);

    	// Block until Enter is pressed
    	log("Press <Enter> to exit");
		try {
			System.in.read();
		} catch (IOException e) {
			//If we can't read we'll just exit
		}

		// Disconnect the client
		client.disconnect();
		log("Disconnected");
    }

    /**
     * Utility method to handle logging. If 'quietMode' is set, this method does nothing
     * @param message the message to log
     */
    private void log(String message) {
    	if (!quietMode) {
    		System.out.println(message);
    	}
    }



	/****************************************************************/
	/* Methods to implement the MqttCallback interface              */
	/****************************************************************/

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
	public void connectionLost(Throwable cause) {
		// Called when the connection to the server has been lost.
		// An application may choose to implement reconnection
		// logic at this point.
		// This sample simply exits.
		log("Connection to " + brokerUrl + " lost!" + cause);
		System.exit(1);
	}

    /**
     * @see MqttCallback#deliveryComplete(MqttDeliveryToken)
     */
	public void deliveryComplete(MqttDeliveryToken token) {
		// Called when a message has completed delivery to the
		// server. The token passed in here is the same one
		// that was returned in the original call to publish.
		// This allows applications to perform asychronous
		// delivery without blocking until delivery completes.

		// This sample demonstrates synchronous delivery, by
		// using the token.waitForCompletion() call in the main thread.
	}

    /**
     * @see MqttCallback#messageArrived(MqttTopic, MqttMessage)
     */
	public void messageArrived(MqttTopic topic, MqttMessage message) throws MqttException {
		// Called when a message arrives from the server.

		String time = new Timestamp(System.currentTimeMillis()).toString();

		System.out.println("Time:\t" +time +
                           "  Topic:\t" + topic.getName() +
                           "  Message:\t" + new String(message.getPayload()) +
                           "  QoS:\t" + message.getQos());
	}

	/****************************************************************/
	/* End of MqttCallback methods                                  */
	/****************************************************************/



    static void printHelp() {
        System.out.println(
            "Syntax:\n\n"+
            "    Sample [-h] [-a publish|subscribe] [-t <topic>] [-m <message text>]\n"+
            "            [-s 0|1|2] -b <hostname|IP address>] [-p <brokerport>] [-i <clientID>]\n\n"+
            "    -h  Print this help text and quit\n"+
            "    -q  Quiet mode (default is false)\n"+
            "    -a  Perform the relevant action (default is publish)\n" +
            "    -t  Publish/subscribe to <topic> instead of the default\n" +
            "            (publish: \"Sample/Java/v3\", subscribe: \"Sample/#\")\n" +
            "    -m  Use <message text> instead of the default\n" +
            "            (\"Message from MQTTv3 Java client\")\n" +
            "    -s  Use this QoS instead of the default (2)\n" +
            "    -b  Use this name/IP address instead of the default (" + broker +")\n" +
            "    -p  Use this port instead of the default (" + port +")\n\n" +
            "    -i  Use this client ID instead of SampleJavaV3_<action>\n" +
            "Delimit strings containing spaces with \"\"\n\n"+
            "Publishers transmit a single message then disconnect from the server.\n"+
            "Subscribers remain connected to the server and receive appropriate\n"+
            "messages until <enter> is pressed.\n\n"
            );
    }

}
