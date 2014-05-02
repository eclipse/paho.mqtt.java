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
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.MqttV3Receiver;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test expects an MQTT Server to be listening on the port 
 * given by the SERVER_URI property (which is 1883 by default)
 */
public class SendReceiveTest {

  static final Class<?> cclass = SendReceiveTest.class;
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
   * Tests that a client can be constructed and that it can connect to and disconnect from the
   * service
   * @throws Exception 
   */
  @Test
  public void testConnect() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();
      log.info("Disconnecting...");
      mqttClient.disconnect();
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();
      log.info("Disconnecting...");
      mqttClient.disconnect();
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      if (mqttClient != null) {
    	log.info("Close...");
        mqttClient.close();
      }
    }

    log.exiting(className, methodName);
  }

  /**
   * Test connection using a remote host name for the local host.
   * @throws Exception 
   */
  @Test
  public void testRemoteConnect() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();
      log.info("Disconnecting...");
      mqttClient.disconnect();

      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);
      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(false);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: false");
      mqttClient.connect(mqttConnectOptions);

      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);

      byte[] payload = ("Message payload " + className + "." + methodName).getBytes();
      MqttTopic mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 1, false);
      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }
      log.info("Disconnecting...");
      mqttClient.disconnect();
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      if (mqttClient != null) {
    	log.info("Close...");
        mqttClient.close();
      }
    }

    log.exiting(className, methodName);
  }

  /**
   * Test client pubSub using largish messages
   */
  @Test
  public void testLargeMessage() {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();

      int largeSize = 10000;
      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};
      byte[] message = new byte[largeSize];

      java.util.Arrays.fill(message, (byte) 's');

      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);
      log.info("Unsubscribing from..." + topicNames[0]);
      mqttClient.unsubscribe(topicNames);
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);
      MqttTopic mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(message, 0, false);

      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message);
      if (!ok) {
        Assert.fail("Receive failed");
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed to instantiate:" + methodName + " exception=" + exception);
    }
    finally {
      try {
        if (mqttClient != null) {
         log.info("Disconnecting...");
         mqttClient.disconnect();
         log.info("Close...");
         mqttClient.close();
       }
     }
     catch (Exception exception) {
        log.log(Level.SEVERE, "caught exception:", exception);
      }
    }

    log.exiting(className, methodName);
  }

  /**
   * Test that QOS values are preserved between MQTT publishers and subscribers.
   */
  @Test
  public void testQoSPreserved() {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();

      String[] topicNames = new String[]{methodName + "/Topic0", methodName + "/Topic1", methodName + "/Topic2"};
      int[] topicQos = {0, 1, 2};
      for (int i = 0; i < topicNames.length; i++) {
    	  log.info("Subscribing to..." + topicNames[i] + " at Qos " + topicQos[i]);
      }
      mqttClient.subscribe(topicNames, topicQos);

      for (int i = 0; i < topicNames.length; i++) {
        byte[] message = ("Message payload " + className + "." + methodName + " " + topicNames[i]).getBytes();
        MqttTopic mqttTopic = mqttClient.getTopic(topicNames[i]);
        for (int iQos = 0; iQos < 3; iQos++) {
          log.info("Publishing to..." + topicNames[i] + " at Qos " + iQos);
          mqttTopic.publish(message, iQos, false);
          boolean ok = mqttV3Receiver.validateReceipt(topicNames[i], Math.min(iQos, topicQos[i]), message);
          if (!ok) {
            Assert.fail("Receive failed sub Qos=" + topicQos[i] + " PublishQos=" + iQos);
          }
        }
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      try {
        if (mqttClient != null) {
         log.info("Disconnecting...");
         mqttClient.disconnect();
         log.info("Close...");
         mqttClient.close();
       }
     }
      catch (Exception exception) {
        log.log(Level.SEVERE, "caught exception:", exception);
      }
    }

    log.exiting(className, methodName);
  }
  
  /**
   * Multiple publishers and subscribers.
   * @throws Exception 
   */
  @Test
  public void testMultipleClients() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient[] mqttPublisher = new IMqttClient[2];
    IMqttClient[] mqttSubscriber = new IMqttClient[10];
    try {
      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};

      MqttTopic[] mqttTopic = new MqttTopic[mqttPublisher.length];
      for (int i = 0; i < mqttPublisher.length; i++) {
        mqttPublisher[i] = clientFactory.createMqttClient(serverURI, "MultiPub" + i);
        log.info("Connecting...(serverURI:" + serverURI + ", ClientId: MultiPub" + i);
        mqttPublisher[i].connect();
        mqttTopic[i] = mqttPublisher[i].getTopic(topicNames[0]);
      } // for...

      MqttV3Receiver[] mqttV3Receiver = new MqttV3Receiver[mqttSubscriber.length];
      for (int i = 0; i < mqttSubscriber.length; i++) {
        mqttSubscriber[i] = clientFactory.createMqttClient(serverURI, "MultiSubscriber" + i);
        mqttV3Receiver[i] = new MqttV3Receiver(mqttSubscriber[i], LoggingUtilities.getPrintStream());
        log.info("Assigning callback...");
        mqttSubscriber[i].setCallback(mqttV3Receiver[i]);
        log.info("Connecting...(serverURI:" + serverURI + ", ClientId: MultiSubscriber" + i);
        mqttSubscriber[i].connect();
        log.info("Subcribing to..." + topicNames[0]);
        mqttSubscriber[i].subscribe(topicNames, topicQos);
      } // for...

      for (int iMessage = 0; iMessage < 10; iMessage++) {
        byte[] payload = ("Message " + iMessage).getBytes();
        for (int i = 0; i < mqttPublisher.length; i++) {
          log.info("Publishing to..." + topicNames[0]);
          mqttTopic[i].publish(payload, 0, false);
        }

        for (int i = 0; i < mqttSubscriber.length; i++) {
          for (int ii = 0; ii < mqttPublisher.length; ii++) {
            boolean ok = mqttV3Receiver[i].validateReceipt(topicNames[0], 0, payload);
            if (!ok) {
              Assert.fail("Receive failed");
            }
          } // for publishers...
        } // for subscribers...
      } // for messages...

    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      throw exception;
    }
    finally {
      try {
        for (int i = 0; i < mqttPublisher.length; i++) {
          log.info("Disconnecting...MultiPub" + i);
          mqttPublisher[i].disconnect();
          log.info("Close...");
          mqttPublisher[i].close();
        }
        for (int i = 0; i < mqttSubscriber.length; i++) {
          log.info("Disconnecting...MultiSubscriber" + i);
          mqttSubscriber[i].disconnect();
          log.info("Close...");
          mqttSubscriber[i].close();
        }
        
        Thread.sleep(5000);
      }
      catch (Exception exception) {
        log.log(Level.SEVERE, "caught exception:", exception);
      }
    }

    log.exiting(className, methodName);
  }

  /**
   * Test the behaviour of the cleanStart flag, used to clean up before re-connecting.
   * @throws Exception 
   */
  @Test
  public void testCleanStart() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);
      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
      // Clean start: true  - The broker cleans up all client state, including subscriptions, when the client is disconnected.
      // Clean start: false - The broker remembers all client state, including subscriptions, when the client is disconnected.
      //                      Matching publications will get queued in the broker whilst the client is disconnected.
      // For Mqtt V3 cleanSession=false, implies new subscriptions are durable.
      mqttConnectOptions.setCleanSession(false);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: false");
      mqttClient.connect(mqttConnectOptions);

      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);

      byte[] payload = ("Message payload " + className + "." + methodName + " First").getBytes();
      MqttTopic mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 1, false);
      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }

      // Disconnect and reconnect to make sure the subscription and all queued messages are cleared.
      log.info("Disconnecting...");
      mqttClient.disconnect();
      log.info("Close");
      mqttClient.close();

      // Send a message from another client, to our durable subscription.
      mqttClient = clientFactory.createMqttClient(serverURI, methodName + "Other");
      mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);
      mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(true);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + "Other, cleanSession: true");
      mqttClient.connect(mqttConnectOptions);
      // Receive the publication so that we can be sure the first client has also received it.
      // Otherwise the first client may reconnect with its clean session before the message has arrived.
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);
      payload = ("Message payload " + className + "." + methodName + " Other client").getBytes();
      mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 1, false);
      ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }
      log.info("Disconnecting...");
      mqttClient.disconnect();
      log.info("Close...");
      mqttClient.close();

      // Reconnect and check we have no messages.
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);
      mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(true);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: true");
      mqttClient.connect(mqttConnectOptions);
      MqttV3Receiver.ReceivedMessage receivedMessage = mqttV3Receiver.receiveNext(100);
      if (receivedMessage != null) {
        Assert.fail("Receive messaqe:" + new String(receivedMessage.message.getPayload()));
      }

      // Also check that subscription is cancelled.
      payload = ("Message payload " + className + "." + methodName + " Cancelled Subscription").getBytes();
      mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 1, false);

      receivedMessage = mqttV3Receiver.receiveNext(100);
      if (receivedMessage != null) {
        log.info("Message I shouldn't have: " + new String(receivedMessage.message.getPayload()));
        Assert.fail("Receive messaqe:" + new String(receivedMessage.message.getPayload()));
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      throw exception;
    }
    finally {
      try {
    	log.info("Disconnecting...");
        mqttClient.disconnect();
      }
      catch (Exception exception) {
        // do nothing
      }

      try {
    	log.info("Close...");
        mqttClient.close();
      }
      catch (Exception exception) {
        // do nothing
      }
    }

    log.exiting(className, methodName);
  }
}
