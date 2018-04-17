/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */

package org.eclipse.paho.jmeclient.mqttv3.sampleGPIO.AsyncWait;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * A sample application that demonstrates how to use the MQTT v3 Client api in
 * non-blocking waiter mode.
 * 
 * It can be run from the command line in one of two modes: 
 *  - as a publisher, sending a single message to a topic on the server
 *  - as a subscriber, listening for messages from the server
 *  
 *  There are three versions of the sample that implement the same features
 *  but do so using using different programming styles:
 *  <ol>
 *  <li>Sample (this one) which uses the API which blocks until the operation completes</li>
 *  <li>SampleAsyncWait shows how to use the asynchronous API with waiters that block until 
 *  an action completes</li>
 *  <li>SampleAsyncCallBack shows how to use the asynchronous API where events are
 *  used to notify the application when an action completes<li>
 *  </ol>
 *  
 *  If the application is run with the -h parameter then info is displayed that 
 *  describes all of the options / parameters. 
 */

public class SampleAsyncWait implements MqttCallback {

	// Private instance variables
	private MqttAsyncClient 	client;
	private String 				brokerUrl;
	private boolean 			quietMode;
	private MqttConnectOptions 	conOpt;
	private boolean 			clean;
	private String password;
	private String userName;
	
	/**
	 * Constructs an instance of the sample client wrapper
	 * @param brokerUrl the url to connect to
	 * @param clientId the client id to connect with
	 * @param cleanSession clear state at end of connection or not (durable or non-durable subscriptions)
	 * @param quietMode whether debug should be printed to standard out
   * @param userName the username to connect with
	 * @param password the password for the user
	 * @throws MqttException
	 */
    public SampleAsyncWait(String brokerUrl, String clientId, boolean cleanSession, 
    		boolean quietMode, String userName, String password) throws MqttException {
    	
    	this.brokerUrl = brokerUrl;
    	this.quietMode = quietMode;
    	this.clean 	   = cleanSession;
    	this.userName = userName;
    	this.password = password;

    	try {
    		// Construct the connection options object that contains connection parameters 
    		// such as cleansession and LWAT
	    	conOpt = new MqttConnectOptions();
	    	conOpt.setCleanSession(clean);
	    	if(password != null ) {
	    		conOpt.setPassword(this.password.toCharArray());
	    	}
	    	if(userName != null) {
	    		conOpt.setUserName(this.userName);     
	    	}

    		// Construct a non blocking MQTT client instance
			client = new MqttAsyncClient(this.brokerUrl,clientId);
			
			// Set this wrapper as the callback handler
	    	client.setCallback(this);
	    	
		} catch (MqttException e) {
			e.printStackTrace();
			log("Unable to set up client: "+e.toString());
			System.exit(1);
		}
    }

    /**
     * Publish / send a message to an MQTT server
     * @param topicName the name of the topic to publish to
     * @param qos the quality of service to delivery the message at (0,1,2)
     * @param payload the set of bytes to send to the MQTT server 
     * @throws MqttException
     */
    public void publish(String topicName, int qos, byte[] payload) throws MqttException {
    	
    	// Connect to the MQTT server 
    	// issue a non-blocking connect and then use the token to wait until the
    	// connect completes. An exception is thrown if connect fails.
    	log("Connecting to "+brokerUrl + " with client ID "+client.getClientId());
    	IMqttToken conToken = client.connect(conOpt,null,null);
    	conToken.waitForCompletion();
    	log("Connected");

    	log("Publishing at: "+System.currentTimeMillis()+ " to topic \""+topicName+"\" qos "+qos);
    	
    	// Construct the message to send
   		MqttMessage message = new MqttMessage(payload);
    	message.setQos(qos);
	
    	// Send the message to the server, control is returned as soon 
    	// as the MQTT client has accepted to deliver the message. 
    	// Use the delivery token to wait until the message has been
    	// delivered	
    	IMqttDeliveryToken pubToken = client.publish(topicName, message, null, null);
    	pubToken.waitForCompletion(); 	
    	log("Published");   
    	
    	// Disconnect the client
    	// Issue the disconnect and then use a token to wait until 
    	// the disconnect completes.
    	log("Disconnecting");
    	IMqttToken discToken = client.disconnect(null, null);
    	discToken.waitForCompletion();
    	log("Disconnected");
    }
    
    /**
     * Subscribe to a topic on an MQTT server
     * Once subscribed this method waits for the messages to arrive from the server 
     * that match the subscription. It continues listening for messages until the enter key is 
     * pressed.
     * @param topicName to subscribe to (can be wild carded)
     * @param qos the maximum quality of service to receive messages at for this subscription 
     * @throws MqttException
     */
    public void subscribe(String topicName, int qos) throws MqttException {
    	
    	// Connect to the MQTT server 
    	// issue a non-blocking connect and then use the token to wait until the
    	// connect completes. An exception is thrown if connect fails.
    	log("Connecting to "+brokerUrl + " with client ID "+client.getClientId());
    	IMqttToken conToken = client.connect(conOpt,null, null);
    	conToken.waitForCompletion();
    	log("Connected");

    	// Subscribe to the requested topic.
    	// Control is returned as soon client has accepted to deliver the subscription. 
    	// Use a token to wait until the subscription is in place.
    	log("Subscribing to topic \""+topicName+"\" qos "+qos);
  
    	IMqttToken subToken = client.subscribe(topicName, qos, null, null);
    	subToken.waitForCompletion();
    	log("Subscribed to topic \""+topicName);

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
		// logic at this point. This sample simply exits.
		log("Connection to " + brokerUrl + " lost!" + cause);
		System.exit(1);
	}

    /**
     * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
     */
	public void deliveryComplete(IMqttDeliveryToken token) {
		// Called when a message has been delivered to the
		// server. The token passed in here is the same one
		// that was passed to or returned from the original call to publish.
		// This allows applications to perform asynchronous 
		// delivery without blocking until delivery completes.
		//
		// This sample demonstrates asynchronous deliver and 
		// uses the token.waitForCompletion() call in the main thread which
		// blocks until the delivery has completed. 
		// Additionally the deliveryComplete method will be called if 
		// the callback is set on the client
		// 
		// If the connection to the server breaks before delivery has completed
		// delivery of a message will complete after the client has re-connected.
		// The getPendinTokens method will provide tokens for any messages
		// that are still to be delivered.
		try {
			log("Delivery complete callback: Publish Completed "+token.getMessage());
		} catch (Exception ex) {
			log("Exception in delivery complete callback"+ex);
		}
	}

    /**
     * @see MqttCallback#messageArrived(String, MqttMessage)
     */
	public void messageArrived(String topic, MqttMessage message) throws MqttException {
		// Called when a message arrives from the server that matches any
		// subscription made by the client		
		System.out.println("Time:\t" +System.currentTimeMillis() +
                           "  Topic:\t" + topic + 
                           "  Message:\t" + new String(message.getPayload()) +
                           "  QoS:\t" + message.getQos());
	}

	/****************************************************************/
	/* End of MqttCallback methods                                  */
	/****************************************************************/

    static void printHelp() {
      System.out.println(
          "Syntax:\n\n" +
              "    Sample [-h] [-a publish|subscribe] [-t <topic>] [-m <message text>]\n" +
              "            [-s 0|1|2] -b <hostname|IP address>] [-p <brokerport>] [-i <clientID>]\n\n" +
              "    -h  Print this help text and quit\n" +
              "    -q  Quiet mode (default is false)\n" +
              "    -a  Perform the relevant action (default is publish)\n" +
              "    -t  Publish/subscribe to <topic> instead of the default\n" +
              "            (publish: \"Sample/Java/v3\", subscribe: \"Sample/#\")\n" +
              "    -m  Use <message text> instead of the default\n" +
              "            (\"Message from MQTTv3 Java client\")\n" +
              "    -s  Use this QoS instead of the default (2)\n" +
              "    -b  Use this name/IP address instead of the default (localhost)\n" +
              "    -p  Use this port instead of the default (1883)\n\n" +
              "    -i  Use this client ID instead of SampleJavaV3_<action>\n" +
              "    -c  Connect to the server with a clean session (default is false)\n" +
              "     \n\n Security Options \n" +
              "     -u Username \n" +
              "     -z Password \n" +
              "     \n\n SSL Options \n" +
              "    -v  SSL enabled; true - (default is false) " +
              "    -k  Use this JKS format key store to verify the client\n" +
              "    -w  Passpharse to verify certificates in the keys store\n" +
              "    -r  Use this JKS format keystore to verify the server\n" +
              " If javax.net.ssl properties have been set only the -v flag needs to be set\n" +
              "Delimit strings containing spaces with \"\"\n\n" +
              "Publishers transmit a single message then disconnect from the server.\n" +
              "Subscribers remain connected to the server and receive appropriate\n" +
              "messages until <enter> is pressed.\n\n"
          );
    }

    /**
     * Disconnect the client that has subscribed from the server
     * @throws MqttException
     */
	public void disconnect() throws MqttException {
		log("Disconnecting client");
    	IMqttToken discToken = client.disconnect(null, null);
    	discToken.waitForCompletion();
    	log("Disconnect complete");
	}

}