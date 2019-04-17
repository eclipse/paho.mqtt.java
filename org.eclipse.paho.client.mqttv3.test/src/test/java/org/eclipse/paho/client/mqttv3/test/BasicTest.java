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
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.eclipse.paho.common.test.categories.ExternalTest;
import org.eclipse.paho.common.test.categories.MQTTV3Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;


/**
 * Tests providing a basic general coverage for the MQTT client API
 */
@Category({OnlineTest.class, MQTTV3Test.class})
public class BasicTest {
  private static final Logger log = Logger.getLogger(BasicTest.class.getName());

  private static URI serverURI;
  private static MqttClientFactoryPaho clientFactory;
  private static String topicPrefix;

  /**
   * @throws Exception 
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    try {
      String methodName = Utility.getMethodName();
      LoggingUtilities.banner(log, BasicTest.class, methodName);

      serverURI = TestProperties.getServerURI();
      clientFactory = new MqttClientFactoryPaho();
      clientFactory.open();
      topicPrefix = "BasicTest-" + UUID.randomUUID().toString() + "-";

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
    LoggingUtilities.banner(log, BasicTest.class, methodName);

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
    LoggingUtilities.banner(log, BasicTest.class, methodName);

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
    LoggingUtilities.banner(log, BasicTest.class, methodName);

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
    LoggingUtilities.banner(log, BasicTest.class, methodName);

    IMqttClient client = null;
    try {
      String topicStr = topicPrefix + "topic" + "_02";
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
    LoggingUtilities.banner(log, BasicTest.class, methodName);

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
    LoggingUtilities.banner(log, BasicTest.class, methodName);

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



  @Category(ExternalTest.class)
  @Test
  public void test330() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, BasicTest.class, methodName);

    int before_thread_count = Thread.activeCount();
    URI uri = new URI("tcp://iot.eclipse.org:1882");
    IMqttAsyncClient client = clientFactory.createMqttAsyncClient(uri, "client-1");

    MqttConnectOptions options = new MqttConnectOptions();
    options.setAutomaticReconnect(true);
    options.setUserName("foo");
    options.setPassword("bar".toCharArray());
    options.setConnectionTimeout(2);
    client.connect(options);

    Thread.sleep(1000);

    try {
    	  // this would deadlock before fix
      client.disconnect(0).waitForCompletion();
    } finally {
      client.close();
    }
    
    int after_count = Thread.activeCount();
    Thread[] tarray = new Thread[after_count];
    while (after_count > before_thread_count) {
      after_count = Thread.enumerate(tarray);
      for (int i = 0; i < after_count; ++i) {
    	    log.info(i + " " + tarray[i].getName());
      }
      Thread.sleep(100);
    }
    Assert.assertEquals(before_thread_count, after_count);
  }
  
  
  @Test
  public void test402() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, BasicTest.class, methodName);

    IMqttClient client = null;
    int before_thread_count = Thread.activeCount();
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
	  
	  int after_count = Thread.activeCount();
	  Thread[] tarray = new Thread[after_count];
	  int count = 0;
	  while (after_count > before_thread_count) {
	    after_count = Thread.enumerate(tarray);
	    for (int i = 0; i < after_count; ++i) {
	      log.info(i + " " + tarray[i].getName());
	    }
	    if (++count == 10) {
	    	  break;
	    }
	    	Thread.sleep(100);
	  }
	  Assert.assertEquals(before_thread_count, after_count);
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
    int after_count = Thread.activeCount();
    Thread[] tarray = new Thread[after_count];
    after_count = Thread.enumerate(tarray);
    for (int i = 0; i < after_count; ++i) {
    	  log.info(i + " " + tarray[i].getName());
    }
    Assert.assertEquals(before_thread_count, after_count);
  }
  
  
  @Test
  public void test402a() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, BasicTest.class, methodName);

    IMqttClient client = null;
    int before_thread_count = Thread.activeCount();
    final int pool_size = 10;
    try {
      String clientId = methodName;
      client = new MqttClient(serverURI.toString(), clientId, null, Executors.newScheduledThreadPool(pool_size));

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
    int after_count = Thread.activeCount();
    Thread[] tarray = new Thread[after_count];
    after_count = Thread.enumerate(tarray);
    for (int i = 0; i < after_count; ++i) {
      log.info(i + " " + tarray[i].getName());
    }
    Assert.assertEquals(after_count, before_thread_count + pool_size);
  }

  /**
   *
   */
  class MessageListener implements MqttCallback {
    private final Logger logger2 = Logger.getLogger(MessageListener.class.getCanonicalName());

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
