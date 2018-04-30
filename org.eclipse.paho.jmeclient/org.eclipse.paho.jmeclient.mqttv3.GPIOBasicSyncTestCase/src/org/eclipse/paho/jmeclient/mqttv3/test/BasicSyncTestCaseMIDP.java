/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.paho.jmeclient.mqttv3.test;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

/**
 * Tests providing a basic general coverage for the MQTT client API
 */

public class BasicSyncTestCaseMIDP {

  private static MqttClient client;
  private static String clientId;
  private static String serverURI;
  private static String broker;
  private static int port;

  /**
   * @throws Exception 
   */
  public static void setUpBeforeClass() throws Exception {

    try {
      
	  broker = "localhost";
	  port = 1883;
	  clientId = "BasicSyncTestCase_MIDP";
	  serverURI = "tcp://" + broker + ":" + port;
      
    }
    catch (Exception exception) {
      System.out.println("caught exception: "  + exception);
      throw exception;
    }
  }

  /**
   * @throws Exception
   */
  public static void tearDownAfterClass() throws Exception {
	
	if (client != null) {
		client.close();
		client.disconnect();
	}
	
  }

  /**
   * @throws Exception 
   */
  public void testConnect() throws Exception {

    IMqttClient client = null;
    try {
      String clientId = "testConnect";

      client = new MqttClient(serverURI, clientId);
      System.out.println("Connecting...(serverURI:" + serverURI + ", ClientId:" + clientId);
      client.connect();

      String clientId2 = client.getClientId();
      System.out.println("clientId = " + clientId2);

      boolean isConnected = client.isConnected();
      System.out.println("isConnected = " + isConnected);

      String id = client.getServerURI();
      System.out.println("ServerURI = " + id);

      client.disconnect();
      System.out.println("Disconnecting...");

      client.connect();
      System.out.println("Re-Connecting...");

      client.disconnect();
      System.out.println("Disconnecting...");
    }
    catch (MqttException exception) {
      System.out.println("Unexpected exception: " + exception);
    }
    finally {
      if (client != null) {
    	System.out.println("Close...");
        client.close();
      }
    }
  }

  /**
   * @throws Exception 
   */
  public void testHAConnect() throws Exception {

    IMqttClient client = null;
    try {
      try {
    	String clientId = "testHAConnect";

        // If a proxy client does not support the URI list in the connect options, then this test should fail.
        // We ensure this happens by using a junk URI when creating the client. 
        String junk = "tcp://junk:123";
        client = new MqttClient(junk, clientId);

        // The first URI has a good protocol, but has a garbage hostname. 
        // This ensures that a connect is attempted to the the second URI in the list 
        String[] urls = new String[]{"tcp://junk", serverURI.toString()};

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(urls);

        System.out.println("Connecting...");
        client.connect(options);

        System.out.println("Disconnecting...");
        client.disconnect();
      }
      catch (Exception e) {
    	System.out.println(e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
        throw e;
      }
    }
    finally {
      if (client != null) {
    	System.out.println("Close...");
        client.close();
      }
    }
  }

  /**
   * @throws Exception 
   */
  public void testPubSub() throws Exception {

    IMqttClient client = null;
    try {
      String topicStr = "topic" + "_02";
      String clientId = "testPubSub";
      client = new MqttClient(serverURI, clientId);

      System.out.println("Assigning callback...");
      MessageListener listener = new MessageListener();
      client.setCallback(listener);

      System.out.println("Connecting...(serverURI:" + serverURI + ", ClientId:" + clientId);
      client.connect();

      System.out.println("Subscribing to..." + topicStr);
      client.subscribe(topicStr);

      System.out.println("Publishing to..." + topicStr);
      MqttTopic topic = client.getTopic(topicStr);
      MqttMessage message = new MqttMessage("foo".getBytes());
      topic.publish(message);

      System.out.println("Checking msg");
      MqttMessage msg = listener.getNextMessage();
      
      if (msg == null) throw new Exception("message should not be null");
      if (!msg.toString().equals("foo")) throw new Exception("message should equal foo");

      System.out.println("getTopic name");
      String topicName = topic.getName();
      System.out.println("topicName = " + topicName);
      if (!topicName.equals(topicStr)) throw new Exception ("topicName should equal topicStr");

      System.out.println("Disconnecting...");
      client.disconnect();
      System.out.println("testPubSub completed successfully");
    }
    finally {
      if (client != null) {
    	System.out.println("Close...");
        client.close();
      }
    }
  }

  /**
   * @throws Exception 
   */
  public void testMsgProperties() throws Exception {

	System.out.println("Check defaults for empty message");
    MqttMessage msg = new MqttMessage();
    if (!(msg.getQos() == 1)) throw new Exception ("Qos should be 1");
    if (!(msg.isDuplicate() == false)) throw new Exception ("msg.isDulplicate should return false");
    if (!(msg.isRetained() == false)) throw new Exception ("msg.retained should return false");
    if (!(msg.getPayload()!=null)) throw new Exception ("payload should not be null");
    if (!(msg.getPayload().length == 0)) throw new Exception ("payload should be zero length");
    if (!(msg.toString().equals(""))) throw new Exception ("msg.toString() should be equal to empty string");

    System.out.println("Check defaults for message with payload");
    msg = new MqttMessage("foo".getBytes());
    if (!(msg.getQos() == 1)) throw new Exception ("Qos should be 1");
    if (!(msg.isDuplicate() == false)) throw new Exception ("msg.isDulplicate should return false");
    if (!(msg.isRetained() == false)) throw new Exception ("msg.retained should return false");
    if (!(msg.getPayload().length == 3)) throw new Exception ("payload length should be 3");
    if (!(msg.toString().equals("foo"))) throw new Exception ("msg.toString() should be equal to empty string");

    System.out.println("Check qos");
    msg.setQos(0);
    if (!(msg.getQos() == 0)) throw new Exception ("Qos should be 0");
    msg.setQos(1);
    if (!(msg.getQos() == 1)) throw new Exception ("Qos should be 1");
    msg.setQos(2);
    if (!(msg.getQos() == 2)) throw new Exception ("Qos should be 2");

    boolean thrown = false;
    try {
      msg.setQos(-1);
    }
    catch (IllegalArgumentException iae) {
      thrown = true;
    }
    
    if (thrown != true) throw new Exception ("IllegalArgumentException expected");
    
    thrown = false;
    try {
      msg.setQos(3);
    }
    catch (IllegalArgumentException iae) {
      thrown = true;
    }
    
    if (thrown != true) throw new Exception ("IllegalArgumentException expected");
    thrown = false;

    System.out.println("Check payload");
    msg.setPayload("foobar".getBytes());
    if (!(msg.getPayload().length == 6)) throw new Exception ("Incorrect payload length");
    if (!(msg.toString().equals("foobar"))) throw new Exception ("Message should be equal to foobar");

    msg.clearPayload();
    if (!(msg.getPayload() != null)) throw new Exception ("msg payload should not be null");
    if (!(msg.getPayload().length == 0)) throw new Exception ("mag payload should have 0 length");
    if (!(msg.toString().equals(""))) throw new Exception ("msg payload should be empty");

    System.out.println("Check retained");
    msg.setRetained(true);
    if (!(msg.isRetained() == true)) throw new Exception ("retained should be true");
    msg.setRetained(false);
    if (!(msg.isRetained() == false)) throw new Exception ("retained should be false");
  }

  /**
   * @throws Exception 
   */
  public void testConnOptDefaults() throws Exception {
    MqttConnectOptions connOpts = new MqttConnectOptions();
    if (!(connOpts.getKeepAliveInterval() == 60)) throw new Exception ("msg payload should be empty");
    if (!(connOpts.getPassword() == null)) throw new Exception ("password should be null");
    if (!(connOpts.getUserName() == null)) throw new Exception ("username should be null");
    if (!(connOpts.isCleanSession() == true)) throw new Exception ("cleansession should be true");
    if (!(connOpts.getWillDestination() == null)) throw new Exception ("will destination should be null");
    if (!(connOpts.getWillMessage() == null)) throw new Exception ("will message should be null");
    if (!(connOpts.getSSLProperties() == null)) throw new Exception ("ssl properties should be null");

  }

  // -------------------------------------------------------------
  // Helper methods/classes
  // -------------------------------------------------------------

  static final Class cclass2 = MessageListener.class;
  static final String classSimpleName2 = cclass2.getName();
  static final String classCanonicalName2 = cclass2.getName();

  /**
   *
   */
  class MessageListener implements MqttCallback {

	SimpleList messages;
	  
    public MessageListener() {
      messages = new SimpleList();
    }

    public MqttMessage getNextMessage() {
      synchronized (messages) {
        if (messages.size() == 0) {
          try {
            messages.wait(1000);
          }
          catch (InterruptedException e) {
            // empty
          }
        }

        if (messages.size() == 0) {
          return null;
        }
        return (MqttMessage) messages.removeFirst();
      }
    }

    public void connectionLost(Throwable cause) {
      System.out.println("connection lost: " + cause.getMessage());
    }

    /**
     * @param token  
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
     System.out.println("delivery complete");
    }

    /**
     * @param topic  
     * @param message 
     * @throws Exception 
     */
    public void messageArrived(String topic, MqttMessage message) throws Exception {
    	System.out.println("message arrived: " + new String(message.getPayload()) + "'");

      synchronized (messages) {
        messages.addLast(message);
        messages.notifyAll();
      }
    }
  }
}
