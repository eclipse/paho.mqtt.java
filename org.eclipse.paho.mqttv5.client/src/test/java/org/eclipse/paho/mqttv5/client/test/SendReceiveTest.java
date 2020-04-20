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

package org.eclipse.paho.mqttv5.client.test;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttTopic;
import org.eclipse.paho.mqttv5.client.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.MqttV5Receiver;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.common.test.categories.MQTTV3Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * This test expects an MQTT Server to be listening on the port 
 * given by the SERVER_URI property (which is 1883 by default)
 */
@Category({OnlineTest.class, MQTTV3Test.class})
public class SendReceiveTest {
  private static final Logger log = Logger.getLogger(SendReceiveTest.class.getName());

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
      LoggingUtilities.banner(log, SendReceiveTest.class, methodName);

      serverURI = TestProperties.getServerURI();
      clientFactory = new MqttClientFactoryPaho();
      clientFactory.open();
      topicPrefix = "SendReceiveTest-" + UUID.randomUUID().toString() + "-";

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
    LoggingUtilities.banner(log, SendReceiveTest.class, methodName);

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
    LoggingUtilities.banner(log, SendReceiveTest.class, methodName);
    log.entering(SendReceiveTest.class.getName(), methodName);

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

    log.exiting(SendReceiveTest.class.getName(), methodName);
  }

  /**
   * Test connection using a remote host name for the local host.
   * @throws Exception 
   */
  @Test
  public void testRemoteConnect() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, SendReceiveTest.class, methodName);
    log.entering(SendReceiveTest.class.getName(), methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();
      log.info("Disconnecting...");
      mqttClient.disconnect();

      MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV5Receiver);
      MqttConnectionOptions mqttConnectOptions = new MqttConnectionOptions();
      mqttConnectOptions.setCleanStart(false);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: false");
      mqttClient.connect(mqttConnectOptions);

      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};
      int[] topicQos = {0};
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);

      byte[] payload = ("Message payload " + SendReceiveTest.class.getName() + "." + methodName).getBytes();
      MqttTopic mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 1, false);
      boolean ok = mqttV5Receiver.validateReceipt(topicNames[0], 0, payload);
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

    log.exiting(SendReceiveTest.class.getName(), methodName);
  }

  /**
   * Test client pubSub using largish messages
   */
  @Test
  public void testLargeMessage() {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, SendReceiveTest.class, methodName);
    log.entering(SendReceiveTest.class.getName(), methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV5Receiver);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();

      int largeSize = 10000;
      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};
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

      boolean ok = mqttV5Receiver.validateReceipt(topicNames[0], 0, message);
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

    log.exiting(SendReceiveTest.class.getName(), methodName);
  }

  /**
   * Test that QOS values are preserved between MQTT publishers and subscribers.
   */
  @Test
  public void testQoSPreserved() {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, SendReceiveTest.class, methodName);
    log.entering(SendReceiveTest.class.getName(), methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV5Receiver);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();

      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic0", topicPrefix + methodName + "/Topic1", topicPrefix + methodName + "/Topic2"};
      int[] topicQos = {0, 1, 2};
      for (int i = 0; i < topicNames.length; i++) {
    	  log.info("Subscribing to..." + topicNames[i] + " at Qos " + topicQos[i]);
      }
      mqttClient.subscribe(topicNames, topicQos);

      for (int i = 0; i < topicNames.length; i++) {
        byte[] message = ("Message payload " + SendReceiveTest.class.getName() + "." + methodName + " " + topicNames[i]).getBytes();
        MqttTopic mqttTopic = mqttClient.getTopic(topicNames[i]);
        for (int iQos = 0; iQos < 3; iQos++) {
          log.info("Publishing to..." + topicNames[i] + " at Qos " + iQos);
          mqttTopic.publish(message, iQos, false);
          boolean ok = mqttV5Receiver.validateReceipt(topicNames[i], Math.min(iQos, topicQos[i]), message);
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

    log.exiting(SendReceiveTest.class.getName(), methodName);
  }
  
  /**
   * Multiple publishers and subscribers.
   * @throws Exception 
   */
  @Test
  public void testMultipleClients() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, SendReceiveTest.class, methodName);
    log.entering(SendReceiveTest.class.getName(), methodName);

    IMqttClient[] mqttPublisher = new IMqttClient[2];
    IMqttClient[] mqttSubscriber = new IMqttClient[10];
    try {
      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};
      int[] topicQos = {0};

      MqttTopic[] mqttTopic = new MqttTopic[mqttPublisher.length];
      for (int i = 0; i < mqttPublisher.length; i++) {
        mqttPublisher[i] = clientFactory.createMqttClient(serverURI, "MultiPub" + i);
        log.info("Connecting...(serverURI:" + serverURI + ", ClientId: MultiPub" + i);
        mqttPublisher[i].connect();
        mqttTopic[i] = mqttPublisher[i].getTopic(topicNames[0]);
      } // for...

      MqttV5Receiver[] mqttV5Receiver = new MqttV5Receiver[mqttSubscriber.length];
      for (int i = 0; i < mqttSubscriber.length; i++) {
        mqttSubscriber[i] = clientFactory.createMqttClient(serverURI, "MultiSubscriber" + i);
        mqttV5Receiver[i] = new MqttV5Receiver(mqttSubscriber[i].getClientId(), LoggingUtilities.getPrintStream());
        log.info("Assigning callback...");
        mqttSubscriber[i].setCallback(mqttV5Receiver[i]);
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
            boolean ok = mqttV5Receiver[i].validateReceipt(topicNames[0], 0, payload);
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

    log.exiting(SendReceiveTest.class.getName(), methodName);
  }

  /**
   * Test the behaviour of the cleanStart flag, used to clean up before re-connecting.
   * @throws Exception 
   */
  @Test
  public void testCleanStart() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, SendReceiveTest.class, methodName);
    log.entering(SendReceiveTest.class.getName(), methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV5Receiver);
      MqttConnectionOptions mqttConnectOptions = new MqttConnectionOptions();
      // Clean start: true  - The broker cleans up all client state, including subscriptions, when the client is disconnected.
      // Clean start: false - The broker remembers all client state, including subscriptions, when the client is disconnected.
      //                      Matching publications will get queued in the broker whilst the client is disconnected.
      // For Mqtt V3 cleanSession=false, implies new subscriptions are durable.
      mqttConnectOptions.setCleanStart(false);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: false");
      mqttClient.connect(mqttConnectOptions);

      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};
      int[] topicQos = {0};
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);

      byte[] payload = ("Message payload " + SendReceiveTest.class.getName() + "." + methodName + " First").getBytes();
      MqttTopic mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 1, false);
      boolean ok = mqttV5Receiver.validateReceipt(topicNames[0], 0, payload);
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
      mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV5Receiver);
      mqttConnectOptions = new MqttConnectionOptions();
      mqttConnectOptions.setCleanStart(true);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + "Other, cleanSession: true");
      mqttClient.connect(mqttConnectOptions);
      // Receive the publication so that we can be sure the first client has also received it.
      // Otherwise the first client may reconnect with its clean session before the message has arrived.
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);
      payload = ("Message payload " + SendReceiveTest.class.getName() + "." + methodName + " Other client").getBytes();
      mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 1, false);
      ok = mqttV5Receiver.validateReceipt(topicNames[0], 0, payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }
      log.info("Disconnecting...");
      mqttClient.disconnect();
      log.info("Close...");
      mqttClient.close();

      // Reconnect and check we have no messages.
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV5Receiver);
      mqttConnectOptions = new MqttConnectionOptions();
      mqttConnectOptions.setCleanStart(true);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: true");
      mqttClient.connect(mqttConnectOptions);
      MqttV5Receiver.ReceivedMessage receivedMessage = mqttV5Receiver.receiveNext(100);
      if (receivedMessage != null) {
        Assert.fail("Receive messaqe:" + new String(receivedMessage.message.getPayload()));
      }

      // Also check that subscription is cancelled.
      payload = ("Message payload " + SendReceiveTest.class.getName() + "." + methodName + " Cancelled Subscription").getBytes();
      mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 1, false);

      receivedMessage = mqttV5Receiver.receiveNext(100);
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

    log.exiting(SendReceiveTest.class.getName(), methodName);
  }
}
