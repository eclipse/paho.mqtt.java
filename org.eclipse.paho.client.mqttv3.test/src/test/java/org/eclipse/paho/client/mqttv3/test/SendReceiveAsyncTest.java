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

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
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
 *
 */
public class SendReceiveAsyncTest {

  static final Class<?> cclass = SendReceiveAsyncTest.class;
  static final String className = cclass.getName();
  static final Logger log = Logger.getLogger(className);

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
   * Tests that a client can be constructed and that it can connect to and
   * disconnect from the service
   * 
   * @throws Exception
   */
  @Test
  public void testConnect() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttAsyncClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
      IMqttToken connectToken = null;
      IMqttToken disconnectToken = null;

      connectToken = mqttClient.connect(null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      disconnectToken = mqttClient.disconnect(null, null);
      log.info("Disconnecting...");
      disconnectToken.waitForCompletion();

      connectToken = mqttClient.connect(null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      disconnectToken = mqttClient.disconnect(null, null);
      log.info("Disconnecting...");
      disconnectToken.waitForCompletion();
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
   * 
   * @throws Exception
   */
  @Test
  public void testRemoteConnect() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttAsyncClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
      IMqttToken connectToken = null;
      IMqttToken subToken = null;
      IMqttDeliveryToken pubToken = null;
      IMqttToken disconnectToken = null;

      connectToken = mqttClient.connect(null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      disconnectToken = mqttClient.disconnect(null, null);
      log.info("Disconnecting...");
      disconnectToken.waitForCompletion();

      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);

      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(false);

      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: false");
      connectToken.waitForCompletion();

      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};
      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      byte[] payload = ("Message payload " + className + "." + methodName).getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();

      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0,
          payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }

      disconnectToken = mqttClient.disconnect(null, null);
      log.info("Disconnecting...");
      disconnectToken.waitForCompletion();

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
   * Test client pubSub using very large messages
   */
  @Test
  public void testLargeMessage() {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttAsyncClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
      IMqttToken connectToken;
      IMqttToken subToken;
      IMqttToken unsubToken;
      IMqttDeliveryToken pubToken;

      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);

      connectToken = mqttClient.connect(null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      int largeSize = 1000;
      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};
      byte[] message = new byte[largeSize];

      java.util.Arrays.fill(message, (byte) 's');

      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      unsubToken = mqttClient.unsubscribe(topicNames, null, null);
      log.info("Unsubscribing from..." + topicNames[0]);
      unsubToken.waitForCompletion();

      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();

      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0,
          message);
      if (!ok) {
        Assert.fail("Receive failed");
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed to instantiate:" + methodName + " exception="+ exception);
    }
    finally {
      try {
        if (mqttClient != null) {
          IMqttToken disconnectToken;
          disconnectToken = mqttClient.disconnect(null, null);
          log.info("Disconnecting...");
          disconnectToken.waitForCompletion();
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
   */
  @Test
  public void testMultipleClients() {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    int publishers = 2;
    int subscribers = 10;

    IMqttAsyncClient[] mqttPublisher = new IMqttAsyncClient[publishers];
    IMqttAsyncClient[] mqttSubscriber = new IMqttAsyncClient[subscribers];

    IMqttToken connectToken;
    IMqttToken subToken;
    IMqttDeliveryToken pubToken;
    IMqttToken disconnectToken;

    try {
      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};

      for (int i = 0; i < mqttPublisher.length; i++) {
        mqttPublisher[i] = clientFactory.createMqttAsyncClient(serverURI, "MultiPub" + i);
        connectToken = mqttPublisher[i].connect(null, null);
        log.info("Connecting...(serverURI:" + serverURI + ", ClientId: MultiPub" + i);
        connectToken.waitForCompletion();
      } // for...

      MqttV3Receiver[] mqttV3Receiver = new MqttV3Receiver[mqttSubscriber.length];
      for (int i = 0; i < mqttSubscriber.length; i++) {
        mqttSubscriber[i] = clientFactory.createMqttAsyncClient(serverURI, "MultiSubscriber" + i);
        mqttV3Receiver[i] = new MqttV3Receiver(mqttSubscriber[i], LoggingUtilities.getPrintStream());
        log.info("Assigning callback...");
        mqttSubscriber[i].setCallback(mqttV3Receiver[i]);
        connectToken = mqttSubscriber[i].connect(null, null);
        log.info("Connecting...(serverURI:" + serverURI + ", ClientId: MultiSubscriber" + i);
        connectToken.waitForCompletion();
        subToken = mqttSubscriber[i].subscribe(topicNames, topicQos, null, null);
        log.info("Subcribing to..." + topicNames[0]);
        subToken.waitForCompletion();
      } // for...

      for (int iMessage = 0; iMessage < 10; iMessage++) {
        byte[] payload = ("Message " + iMessage).getBytes();
        for (int i = 0; i < mqttPublisher.length; i++) {
          pubToken = mqttPublisher[i].publish(topicNames[0], payload, 0, false, null, null);
          log.info("Publishing to..." + topicNames[0]);
          pubToken.waitForCompletion();
        }

        for (int i = 0; i < mqttSubscriber.length; i++) {
          for (int ii = 0; ii < mqttPublisher.length; ii++) {
            boolean ok = mqttV3Receiver[i].validateReceipt(
                topicNames[0], 0, payload);
            if (!ok) {
              Assert.fail("Receive failed");
            }
          } // for publishers...
        } // for subscribers...
      } // for messages...

    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed to instantiate:" + methodName + " exception="+ exception);
    }
    finally {
      try {
        for (int i = 0; i < mqttPublisher.length; i++) {
          disconnectToken = mqttPublisher[i].disconnect(null, null);
          log.info("Disconnecting...MultiPub" + i);
          disconnectToken.waitForCompletion();
          log.info("Close...");
          mqttPublisher[i].close();
        }
        for (int i = 0; i < mqttSubscriber.length; i++) {
          disconnectToken = mqttSubscriber[i].disconnect(null, null);
          log.info("Disconnecting...MultiSubscriber" + i);
          disconnectToken.waitForCompletion();
          log.info("Close...");
          mqttSubscriber[i].close();
        }
      }
      catch (Exception exception) {
        log.log(Level.SEVERE, "caught exception:", exception);
      }
    }

    log.exiting(className, methodName);
  }

  /**
   * Test the behaviour of the cleanStart flag, used to clean up before
   * re-connecting.
   */
  @Test
  public void testCleanStart() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttAsyncClient mqttClient = null;

    IMqttToken connectToken;
    IMqttToken subToken;
    IMqttDeliveryToken pubToken;
    IMqttToken disconnectToken;

    try {
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);

      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
      // Clean start: true - The broker cleans up all client state, including subscriptions, when the client is disconnected.
      // Clean start: false - The broker remembers all client state, including subscriptions, when the client is disconnected.
      //                      Matching publications will get queued in the broker whilst the client is disconnected.
      // For Mqtt V3 cleanSession=false, implies new subscriptions are durable.
      mqttConnectOptions.setCleanSession(false);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: false");
      connectToken.waitForCompletion();

      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};
      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      byte[] payload = ("Message payload " + className + "." + methodName + " First").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();
      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0,
          payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }

      // Disconnect and reconnect to make sure the subscription and all queued messages are cleared.
      disconnectToken = mqttClient.disconnect(null, null);
      log.info("Disconnecting...");
      disconnectToken.waitForCompletion();
      log.info("Close");
      mqttClient.close();

      // Send a message from another client, to our durable subscription.
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName + "Other");
      mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);

      mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(true);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + "Other, cleanSession: true");
      connectToken.waitForCompletion();

      // Receive the publication so that we can be sure the first client has also received it.
      // Otherwise the first client may reconnect with its clean session before the message has arrived.
      subToken = mqttClient.subscribe(topicNames, topicQos, null, null);
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();
      payload = ("Message payload " + className + "." + methodName + " Other client").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();
      ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }
      disconnectToken = mqttClient.disconnect(null, null);
      log.info("Disconnecting...");
      disconnectToken.waitForCompletion();
      log.info("Close...");
      mqttClient.close();

      // Reconnect and check we have no messages.
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
      mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);
      mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(true);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: true");
      connectToken.waitForCompletion();
      MqttV3Receiver.ReceivedMessage receivedMessage = mqttV3Receiver.receiveNext(100);
      if (receivedMessage != null) {
        Assert.fail("Receive messaqe:" + new String(receivedMessage.message.getPayload()));
      }

      // Also check that subscription is cancelled.
      payload = ("Message payload " + className + "." + methodName + " Cancelled Subscription").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();

      receivedMessage = mqttV3Receiver.receiveNext(100);
      if (receivedMessage != null) {
        log.fine("Message I shouldn't have: " + new String(receivedMessage.message.getPayload()));
        Assert.fail("Receive messaqe:" + new String(receivedMessage.message.getPayload()));
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      try {
        if (mqttClient != null) {
          disconnectToken = mqttClient.disconnect(null, null);
          log.info("Disconnecting...");
          disconnectToken.waitForCompletion();
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
   * Short keep-alive intervals along with very large payloads (some MBis) results in the client being disconnected by
   * the broker.
   * 
   * In order to recreate the issue increase the value of waitMilliseconds in
   * org.eclipse.paho.client.mqttv3.test.utilities.MqttV3Receiver.validateReceipt to some large value (e.g.
   * 60*60*1000). This allows the test to wait for a longer time.
   * 
   * The issue occurs because while receiving such a large payload no PING is sent by the client to the broker. This
   * can be seen adding some debug statements in:
   * org.eclipse.paho.client.mqttv3.internal.ClientState.checkForActivity.
   * 
   * Since no other activity (messages from the client to the broker) is generated, the broker disconnects the client.
   */
  @Test
  public void testVeryLargeMessageWithShortKeepAlive() {
  	final String methodName = Utility.getMethodName();
  	LoggingUtilities.banner(log, cclass, methodName);
  	log.entering(className, methodName);
  
  	IMqttAsyncClient mqttClient = null;
  	try {
  		mqttClient = clientFactory.createMqttAsyncClient(serverURI, "testVeryLargeMessage");
  		MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
  		log.info("Assigning callback...");
  		mqttClient.setCallback(mqttV3Receiver);
  		
  		//keepAlive=30s
  		MqttConnectOptions options = new MqttConnectOptions();
  		options.setKeepAliveInterval(30);
  
  		IMqttToken connectToken = mqttClient.connect(options);
  		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
  		connectToken.waitForCompletion();
  
  		String topic = "testLargeMsg/Topic";
  		//10MB
  		int largeSize = 20 * (1 << 20);
  		byte[] message = new byte[largeSize];
  
  		java.util.Arrays.fill(message, (byte) 's');
  
  		IMqttToken subToken = mqttClient.subscribe(topic, 0);
  		log.info("Subscribing to..." + topic);
  		subToken.waitForCompletion();
  
  		IMqttToken pubToken = mqttClient.publish(topic, message, 0, false, null, null);
  		log.info("Publishing to..." + topic);
  		pubToken.waitForCompletion();
  		log.info("Published");
  
  		boolean ok = mqttV3Receiver.validateReceipt(topic, 0, message);
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
  				IMqttToken disconnectToken = mqttClient.disconnect(null, null);
  				log.info("Disconnecting...");
  				disconnectToken.waitForCompletion();
  				mqttClient.close();
  				log.info("Closed");
  			}
  		}
  		catch (Exception exception) {
  			log.log(Level.SEVERE, "caught exception:", exception);
  		}
  	}
  
  	log.exiting(className, methodName);
  }
  
  /**
   * For bug - https://bugs.eclipse.org/bugs/show_bug.cgi?id=414783
   * Test the behavior of the connection timeout when connecting to a non MQTT server.
   * i.e. ssh port 22
   */
  @Test
  public void testConnectTimeout() throws Exception {
	  final String methodName = Utility.getMethodName();
	  LoggingUtilities.banner(log, cclass, methodName);
	  log.entering(className, methodName);

	  IMqttAsyncClient mqttClient = null;
	  // Change the URI to a none MQTT server
	  URI uri = new URI("tcp://iot.eclipse.org:22");
	  IMqttToken connectToken = null;
	  try {
		  mqttClient = clientFactory.createMqttAsyncClient(uri, methodName);
		  log.info("Connecting...(serverURI:" + uri + ", ClientId:" + methodName);
		  connectToken = mqttClient.connect(new MqttConnectOptions());
		  connectToken.waitForCompletion(5000);
		  Assert.fail("Should throw an timeout exception.");
	  }
	  catch (Exception exception) {
		  log.log(Level.INFO, "Connect action failed at expected.");
		  Assert.assertTrue(exception instanceof MqttException);
		  Assert.assertEquals(MqttException.REASON_CODE_CLIENT_TIMEOUT, ((MqttException) exception).getReasonCode());
	  }
	  finally {
		  if (mqttClient != null) {
			  log.info("Close..." + mqttClient);
			  mqttClient.disconnectForcibly(5000, 5000);
		  }
	  }

	  //reuse the client instance to reconnect
	  try {
		  connectToken = mqttClient.connect(new MqttConnectOptions());
		  log.info("Connecting again...(serverURI:" + uri + ", ClientId:" + methodName);
		  connectToken.waitForCompletion(5000);
	  }
	  catch (Exception exception) {
		  log.log(Level.INFO, "Connect action failed at expected.");
		  Assert.assertTrue(exception instanceof MqttException);
		  Assert.assertEquals(
				  (MqttException.REASON_CODE_CLIENT_TIMEOUT == ((MqttException) exception).getReasonCode() ||
				   MqttException.REASON_CODE_CONNECT_IN_PROGRESS == ((MqttException) exception).getReasonCode())
				  , true);
	  }
	  finally {
		  if (mqttClient != null) {
			  log.info("Close..." + mqttClient);
			  mqttClient.disconnectForcibly(5000, 5000);
			  mqttClient.close();
		  }
	  }

	  Assert.assertFalse(mqttClient.isConnected());

	  log.exiting(className, methodName);
  }
}
