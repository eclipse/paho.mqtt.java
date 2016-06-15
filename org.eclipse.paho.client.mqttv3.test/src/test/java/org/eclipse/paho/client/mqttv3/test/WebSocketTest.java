/* Copyright (c) 2009, 2014 IBM Corp.
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
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests providing a basic general coverage for the MQTT WebSocket Functionality
 */

public class WebSocketTest {

  static final Class<?> cclass = WebSocketTest.class;
  private static final String className = cclass.getName();
  private static final Logger log = Logger.getLogger(className);

  private static URI serverURI;
  private static MqttClientFactoryPaho clientFactory;

  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    try {
      String methodName = Utility.getMethodName();
      LoggingUtilities.banner(log, cclass, methodName);

      serverURI = TestProperties.getWebSocketServerURI();
      clientFactory = new MqttClientFactoryPaho();
      clientFactory.open();
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      throw exception;
    }
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);

    try {
      if (clientFactory != null) {
        clientFactory.close();
        clientFactory.disconnect();
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWebSocketConnect() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);

    IMqttClient client = null;
    try {
      String clientId = methodName;
      client = clientFactory.createMqttClient(serverURI, clientId);

      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + clientId);
      client.connect();

      String clientId2 = client.getClientId();
      log.info("clientId = " + clientId2);

      boolean isConnected = client.isConnected();
      log.info("isConnected = " + isConnected);

      String id = client.getServerURI();
      log.info("ServerURI = " + id);

      log.info("Disconnecting...");
      client.disconnect();

      log.info("Re-Connecting...");
      client.connect();

      log.info("Disconnecting...");
      client.disconnect();
    }
    catch (MqttException exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Unexpected exception: " + exception);
    }
    finally {
      if (client != null) {
        log.info("Close...");
        client.close();
      }
    }
  }


  /**
   * @throws Exception
   */
  @Test
  public void testWebSocketPubSub() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);

    IMqttClient client = null;
    try {
      String topicStr = UUID.randomUUID() + "/topic" + "_02";
      String clientId = methodName;
      client = clientFactory.createMqttClient(serverURI, clientId);

      log.info("Assigning callback...");
      MessageListener listener = new MessageListener();
      client.setCallback(listener);

      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + clientId);
      client.connect();

      log.info("Subscribing to..." + topicStr);
      client.subscribe(topicStr);

      log.info("Publishing to..." + topicStr);
      MqttTopic topic = client.getTopic(topicStr);
      MqttMessage message = new MqttMessage("foo".getBytes());
      topic.publish(message);

      log.info("Checking msg");
      MqttMessage msg = listener.getNextMessage();
      Assert.assertNotNull(msg);
      Assert.assertEquals("foo", msg.toString());

      log.info("getTopic name");
      String topicName = topic.getName();
      log.info("topicName = " + topicName);
      Assert.assertEquals(topicName, topicStr);

      log.info("Disconnecting...");
      client.disconnect();
    }
    finally {
      if (client != null) {
        log.info("Close...");
        client.close();
      }
    }
  }

  /**
   * Tests Websocker support for packets over 16KB
   * Prompted by Bug: 482432
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=482432
   * This test connects to a broker via WebSockets, subscribes
   * to a topic, publishes a large payload to it and checks
   * that it recieves the same payload.
 * @throws Exception
   */
  @Test
  public void largePayloadTest() throws Exception{
	  // Generate large byte array;
	  byte[] largeByteArray = new byte[32000];
	  new Random().nextBytes(largeByteArray);
	  String methodName = Utility.getMethodName();
	  LoggingUtilities.banner(log, cclass, methodName);

	    IMqttClient client = null;
	    try {
	      String topicStr = UUID.randomUUID() + "/topic_largeFile_01";
	      String clientId = methodName;
	      client = clientFactory.createMqttClient(serverURI, clientId);

	      log.info("Assigning callback...");
	      MessageListener listener = new MessageListener();
	      client.setCallback(listener);

	      log.info("Connecting... serverURI:" + serverURI + ", ClientId:" + clientId);
	      client.connect();

	      log.info("Subscribing to..." + topicStr);
	      client.subscribe(topicStr);

	      log.info("Publishing to..." + topicStr);
	      MqttTopic topic = client.getTopic(topicStr);
	      MqttMessage message = new MqttMessage(largeByteArray);
	      topic.publish(message);

	      log.info("Checking msg");
	      MqttMessage msg = listener.getNextMessage();
	      Assert.assertNotNull(msg);
	      Assert.assertTrue(Arrays.equals(largeByteArray, msg.getPayload()));
	      log.info("Disconnecting...");
	      client.disconnect();
	      log.info("Disconnected...");
	    } catch (Exception e){
	    	e.printStackTrace();
	    } finally {
	      if (client != null) {
	        log.info("Close...");
	        client.close();
	      }
	    }

  }




  // -------------------------------------------------------------
  // Helper methods/classes
  // -------------------------------------------------------------

  static final Class<MessageListener> cclass2 = MessageListener.class;
  static final String classSimpleName2 = cclass2.getSimpleName();
  static final String classCanonicalName2 = cclass2.getCanonicalName();
  static final Logger logger2 = Logger.getLogger(classCanonicalName2);

  /**
   *
   */
  class MessageListener implements MqttCallback {

    ArrayList<MqttMessage> messages;

    public MessageListener() {
      messages = new ArrayList<MqttMessage>();
    }

    public MqttMessage getNextMessage() {
      synchronized (messages) {
        if (messages.size() == 0) {
          try {
        	// Wait a bit longer than usual because of the largePayloadTest
            messages.wait(10000);
          }
          catch (InterruptedException e) {
            // empty
          }
        }

        if (messages.size() == 0) {
          return null;
        }
        return messages.remove(0);
      }
    }

    public void connectionLost(Throwable cause) {
      logger2.info("connection lost: " + cause.getMessage());
    }

    /**
     * @param token
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
      logger2.info("delivery complete");
    }

    /**
     * @param topic
     * @param message
     * @throws Exception
     */
    public void messageArrived(String topic, MqttMessage message) throws Exception {
      logger2.info("message arrived: " + new String(message.getPayload()) + "'");

      synchronized (messages) {
        messages.add(message);
        messages.notifyAll();
      }
    }
  }
}
