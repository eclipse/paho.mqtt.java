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

import java.net.URI;
import java.util.ArrayList;
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
 * Tests providing a basic general coverage for the MQTT client API
 */

public class BasicTest {

  static final Class<?> cclass = BasicTest.class;
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

      serverURI = TestProperties.getServerURI();
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
  public void testConnect() throws Exception {
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
  public void testHAConnect() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);

    // Some old clients do not support the new HA interface on the connect call
    if (clientFactory.isHighAvalabilitySupported() == false) {
      return;
    }

    IMqttClient client = null;
    try {
      try {
        String clientId = methodName;

        // If a client does not support the URI list in the connect options, then this test should fail.
        // We ensure this happens by using a junk URI when creating the client. 
        URI junk = new URI("tcp://junk:123");
        client = clientFactory.createMqttClient(junk, clientId);

        // The first URI has a good protocol, but has a garbage hostname. 
        // This ensures that a connect is attempted to the the second URI in the list 
        String[] urls = new String[]{"tcp://junk", serverURI.toString()};

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(urls);

        log.info("Connecting...");
        client.connect(options);

        log.info("Disconnecting...");
        client.disconnect();
      }
      catch (Exception e) {
        // logger.info(e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
        throw e;
      }
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
  public void testPubSub() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);

    IMqttClient client = null;
    try {
      String topicStr = "topic" + "_02";
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
   * @throws Exception 
   */
  @Test
  public void testMsgProperties() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);

    log.info("Check defaults for empty message");
    MqttMessage msg = new MqttMessage();
    Assert.assertTrue(msg.getQos() == 1);
    Assert.assertTrue(msg.isDuplicate() == false);
    Assert.assertTrue(msg.isRetained() == false);
    Assert.assertNotNull(msg.getPayload());
    Assert.assertTrue(msg.getPayload().length == 0);
    Assert.assertEquals(msg.toString(), "");

    log.info("Check defaults for message with payload");
    msg = new MqttMessage("foo".getBytes());
    Assert.assertTrue(msg.getQos() == 1);
    Assert.assertTrue(msg.isDuplicate() == false);
    Assert.assertTrue(msg.isRetained() == false);
    Assert.assertTrue(msg.getPayload().length == 3);
    Assert.assertEquals(msg.toString(), "foo");

    log.info("Check qos");
    msg.setQos(0);
    Assert.assertTrue(msg.getQos() == 0);
    msg.setQos(1);
    Assert.assertTrue(msg.getQos() == 1);
    msg.setQos(2);
    Assert.assertTrue(msg.getQos() == 2);

    boolean thrown = false;
    try {
      msg.setQos(-1);
    }
    catch (IllegalArgumentException iae) {
      thrown = true;
    }
    Assert.assertTrue(thrown);
    thrown = false;
    try {
      msg.setQos(3);
    }
    catch (IllegalArgumentException iae) {
      thrown = true;
    }
    Assert.assertTrue(thrown);
    thrown = false;

    log.info("Check payload");
    msg.setPayload("foobar".getBytes());
    Assert.assertTrue(msg.getPayload().length == 6);
    Assert.assertEquals(msg.toString(), "foobar");

    msg.clearPayload();
    Assert.assertNotNull(msg.getPayload());
    Assert.assertTrue(msg.getPayload().length == 0);
    Assert.assertEquals(msg.toString(), "");

    log.info("Check retained");
    msg.setRetained(true);
    Assert.assertTrue(msg.isRetained() == true);
    msg.setRetained(false);
    Assert.assertTrue(msg.isRetained() == false);
  }

  /**
   * @throws Exception 
   */
  @Test
  public void testConnOptDefaults() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);

    log.info("Check MqttConnectOptions defaults");
    MqttConnectOptions connOpts = new MqttConnectOptions();
    Assert.assertEquals(new Integer(connOpts.getKeepAliveInterval()), new Integer(60));
    Assert.assertNull(connOpts.getPassword());
    Assert.assertNull(connOpts.getUserName());
    Assert.assertNull(connOpts.getSocketFactory());
    Assert.assertTrue(connOpts.isCleanSession());
    Assert.assertNull(connOpts.getWillDestination());
    Assert.assertNull(connOpts.getWillMessage());
    Assert.assertNull(connOpts.getSSLProperties());
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
            messages.wait(1000);
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
