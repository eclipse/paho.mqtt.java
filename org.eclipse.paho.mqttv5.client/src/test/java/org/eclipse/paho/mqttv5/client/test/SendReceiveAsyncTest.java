/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corp.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.common.test.categories.ExternalTest;
import org.eclipse.paho.common.test.categories.MQTTV5Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.MqttV5Receiver;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Category({OnlineTest.class, MQTTV5Test.class})
@RunWith(Parameterized.class)
public class SendReceiveAsyncTest {

  static final Class<?> cclass = SendReceiveAsyncTest.class;
  static final String className = cclass.getName();
  static final Logger log = Logger.getLogger(className);

  private URI serverURI;
  private static MqttClientFactoryPaho clientFactory;
  private static String topicPrefix;
  
	@Parameters
	public static Collection<Object[]> data() throws Exception {
		
		return Arrays.asList(new Object[][] {     
          { TestProperties.getServerURI() }, { TestProperties.getWebSocketServerURI() }  
    });
		
	}
	
	public SendReceiveAsyncTest(URI serverURI) {
		this.serverURI = serverURI;
	}


  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    try {
      String methodName = Utility.getMethodName();
      LoggingUtilities.banner(log, cclass, methodName);

      clientFactory = new MqttClientFactoryPaho();
      clientFactory.open();
      topicPrefix = "SendReceiveAsyncTest-" + UUID.randomUUID().toString() + "-";

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

      connectToken = mqttClient.connect();
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      disconnectToken = mqttClient.disconnect();
      log.info("Disconnecting...");
      disconnectToken.waitForCompletion();
      log.info("Disconnect complete.  Connecting.");

      connectToken = mqttClient.connect();
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      disconnectToken = mqttClient.disconnect();
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
      IMqttToken pubToken = null;
      IMqttToken disconnectToken = null;

      connectToken = mqttClient.connect(null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      disconnectToken = mqttClient.disconnect(null, null);
      log.info("Disconnecting...");
      disconnectToken.waitForCompletion();

      MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV5Receiver);

      MqttConnectionOptions mqttConnectOptions = new MqttConnectionOptions();
      mqttConnectOptions.setCleanStart(false);

      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: false");
      connectToken.waitForCompletion();

      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};
      int[] topicQos = {0};
      subToken = mqttClient.subscribe(new MqttSubscription(topicNames[0], topicQos[0]));
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      byte[] payload = ("Message payload " + className + "." + methodName).getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();

      boolean ok = mqttV5Receiver.validateReceipt(topicNames[0], 0, payload);
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
      IMqttToken pubToken;

      MqttV5Receiver mqttReceiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttReceiver);

      connectToken = mqttClient.connect(null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      int largeSize = 1000;
      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};
      int[] topicQos = {0};
      byte[] message = new byte[largeSize];

      java.util.Arrays.fill(message, (byte) 's');

      subToken = mqttClient.subscribe(new MqttSubscription(topicNames[0], topicQos[0]));
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      unsubToken = mqttClient.unsubscribe(topicNames);
      log.info("Unsubscribing from..." + topicNames[0]);
      unsubToken.waitForCompletion();

      subToken = mqttClient.subscribe(new MqttSubscription(topicNames[0], topicQos[0]));
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();

      boolean ok = mqttReceiver.validateReceipt(topicNames[0], 0,
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
    IMqttToken pubToken;
    IMqttToken disconnectToken;

    try {
      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};
      int[] topicQos = {0};

      for (int i = 0; i < mqttPublisher.length; i++) {
        mqttPublisher[i] = clientFactory.createMqttAsyncClient(serverURI, "MultiPub" + i);
        connectToken = mqttPublisher[i].connect(null, null);
        log.info("Connecting...(serverURI:" + serverURI + ", ClientId: MultiPub" + i);
        connectToken.waitForCompletion();
      } // for...

      MqttV5Receiver[] mqttReceiver = new MqttV5Receiver[mqttSubscriber.length];
      for (int i = 0; i < mqttSubscriber.length; i++) {
        mqttSubscriber[i] = clientFactory.createMqttAsyncClient(serverURI, "MultiSubscriber" + i);
        mqttReceiver[i] = new MqttV5Receiver(mqttSubscriber[i].getClientId(), LoggingUtilities.getPrintStream());
        log.info("Assigning callback...");
        mqttSubscriber[i].setCallback(mqttReceiver[i]);
        connectToken = mqttSubscriber[i].connect(null, null);
        log.info("Connecting...(serverURI:" + serverURI + ", ClientId: MultiSubscriber" + i);
        connectToken.waitForCompletion();
        subToken = mqttSubscriber[i].subscribe(new MqttSubscription(topicNames[0], topicQos[0]));
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
            boolean ok = mqttReceiver[i].validateReceipt(
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
    IMqttToken pubToken;
    IMqttToken disconnectToken;

    try {
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
      MqttV5Receiver mqttReceiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttReceiver);

      MqttConnectionOptions mqttConnectOptions = new MqttConnectionOptions();
      // Clean start: true - The broker cleans up all client state, including subscriptions, when the client is disconnected.
      // Clean start: false - The broker remembers all client state, including subscriptions, when the client is disconnected.
      //                      Matching publications will get queued in the broker whilst the client is disconnected.
      // For Mqtt V3 cleanSession=false, implies new subscriptions are durable.
      mqttConnectOptions.setCleanStart(false);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: false");
      connectToken.waitForCompletion();

      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};
      int[] topicQos = {0};
      subToken = mqttClient.subscribe(new MqttSubscription(topicNames[0], topicQos[0]));
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      byte[] payload = ("Message payload " + className + "." + methodName + " First").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();
      boolean ok = mqttReceiver.validateReceipt(topicNames[0], 0,
          payload);
      if (!ok) {
        Assert.fail("Receive failed");
      }

      // Disconnect and reconnect to make sure the subscription and all queued messages are cleared.
      log.info("Disconnecting...");
      disconnectToken = mqttClient.disconnect();
      disconnectToken.waitForCompletion();
      log.info("Close");
      mqttClient.close();

      // Send a message from another client, to our durable subscription.
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName + "Other");
      mqttReceiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttReceiver);

      mqttConnectOptions = new MqttConnectionOptions();
      mqttConnectOptions.setCleanStart(true);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + "Other, cleanSession: true");
      connectToken.waitForCompletion();

      // Receive the publication so that we can be sure the first client has also received it.
      // Otherwise the first client may reconnect with its clean session before the message has arrived.
      subToken = mqttClient.subscribe(new MqttSubscription(topicNames[0], topicQos[0]));
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();
      payload = ("Message payload " + className + "." + methodName + " Other client").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();
      ok = mqttReceiver.validateReceipt(topicNames[0], 0, payload);
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
      mqttReceiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttReceiver);
      mqttConnectOptions = new MqttConnectionOptions();
      mqttConnectOptions.setCleanStart(true);
      connectToken = mqttClient.connect(mqttConnectOptions, null, null);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName + ", cleanSession: true");
      connectToken.waitForCompletion();
      MqttV5Receiver.ReceivedMessage receivedMessage = mqttReceiver.receiveNext(100);
      if (receivedMessage != null) {
        Assert.fail("Receive messaqe:" + new String(receivedMessage.message.getPayload()));
      }

      // Also check that subscription is cancelled.
      payload = ("Message payload " + className + "." + methodName + " Cancelled Subscription").getBytes();
      pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null);
      log.info("Publishing to..." + topicNames[0]);
      pubToken.waitForCompletion();

      receivedMessage = mqttReceiver.receiveNext(100);
      if (receivedMessage != null) {
        log.fine("Message I shouldn't have: " + new String(receivedMessage.message.getPayload()));
        Assert.fail("Receive message:" + new String(receivedMessage.message.getPayload()));
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      try {
        if (mqttClient != null) {
        	  try {
        		  disconnectToken = mqttClient.disconnect();
        		  log.info("Disconnecting...");
        		  disconnectToken.waitForCompletion();
        	  } catch (Exception e) {
        		  
        	  }
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
  		MqttV5Receiver mqttReceiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
  		log.info("Assigning callback...");
  		mqttClient.setCallback(mqttReceiver);
  		
  		//keepAlive=30s
  		MqttConnectionOptions options = new MqttConnectionOptions();
  		options.setKeepAliveInterval(30);
  
  		IMqttToken connectToken = mqttClient.connect(options);
  		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
  		connectToken.waitForCompletion();
  
  		String topic = topicPrefix + "testLargeMsg/Topic";
  		//10MB
  		int largeSize = 20 * (1 << 20);
  		byte[] message = new byte[largeSize];
  
  		java.util.Arrays.fill(message, (byte) 's');
  
  		IMqttToken subToken = mqttClient.subscribe(new MqttSubscription(topic, 0));
  		log.info("Subscribing to..." + topic);
  		subToken.waitForCompletion();
  
  		IMqttToken pubToken = mqttClient.publish(topic, message, 0, false, null, null);
  		log.info("Publishing to..." + topic);
  		pubToken.waitForCompletion();
  		log.info("Published");
  
  		boolean ok = mqttReceiver.validateReceipt(topic, 0, message);
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
  @Ignore
  @Category(ExternalTest.class)
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
		  connectToken = mqttClient.connect(new MqttConnectionOptions());
		  connectToken.waitForCompletion(5000);
		  Assert.fail("Should throw a timeout exception.");
	  }
	  catch (Exception exception) {
		  log.log(Level.INFO, "Connect action failed at expected.");
		  Assert.assertTrue(exception instanceof MqttException);
		  Assert.assertEquals(MqttException.REASON_CODE_MALFORMED_PACKET, ((MqttException) exception).getReasonCode());
	  }
	  finally {
		  if (mqttClient != null) {
			  log.info("Close..." + mqttClient);
			  try {
				  mqttClient.close();
			  } catch (Exception e) {
				  
			  }
		  }
	  }

	  //reuse the client instance to reconnect
	  try {
		  connectToken = mqttClient.connect(new MqttConnectionOptions());
		  log.info("Connecting again...(serverURI:" + uri + ", ClientId:" + methodName);
		  connectToken.waitForCompletion(5000);
	  }
	  catch (Exception exception) {
		  log.log(Level.INFO, "Connect action failed at expected.");
		  //Assert.assertTrue(exception instanceof MqttException);
		  Assert.assertEquals(
				  (MqttClientException.REASON_CODE_CLIENT_CLOSED == ((MqttException) exception).getReasonCode() ||
				   MqttClientException.REASON_CODE_CONNECT_IN_PROGRESS == ((MqttException) exception).getReasonCode())
				  , true);
	  }
	  finally {
		  if (mqttClient != null) {
			  log.info("Close..." + mqttClient);
			  try {
			  mqttClient.disconnect(5000);
			  } catch (Exception e) {
				  
			  }
			  mqttClient.close();
		  }
	  }

	  Assert.assertFalse(mqttClient.isConnected());

	  log.exiting(className, methodName);
  }
  
  /**
   * Test tokens for QoS 0 being 'lost'
   */
  @Test
  public void testQoS0Tokens() {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);
    
    int tokenCount = 1000;  // how many QoS 0 tokens shall we track?

    IMqttAsyncClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
      IMqttToken connectToken;
      IMqttToken subToken;
      IMqttToken[] pubTokens = new IMqttToken[tokenCount];

      MqttV5Receiver mqttReceiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttReceiver);

      MqttConnectionOptions opts = new MqttConnectionOptions();
      connectToken = mqttClient.connect(opts);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      connectToken.waitForCompletion();

      String[] topicNames = new String[]{topicPrefix + methodName + "/Topic"};

      subToken = mqttClient.subscribe(new MqttSubscription(topicNames[0], 2));
      log.info("Subscribing to..." + topicNames[0]);
      subToken.waitForCompletion();

      for (int i = 0; i < tokenCount; ++i) {
    	    try {
    	    		pubTokens[i] = mqttClient.publish(topicNames[0], "message".getBytes(), 0, false);
    	    } catch (Exception e) {
    	    		e.printStackTrace();
    	    }
      }
      log.info(tokenCount + " messages sent");
      int errors = 0;
      for (int i = 0; i < tokenCount; ++i) {
    	    try {
    	  	  pubTokens[i].waitForCompletion(10);
    	    } catch (Exception e) {
    	    	  log.log(Level.INFO, "Token no not complete:" + i);
    	    	  errors += 1;
    	    }
      }
      log.info("Number of waits incomplete "+errors);
      Assert.assertEquals(0, errors);
      
      while (mqttReceiver.receivedMessageCount() < tokenCount) {
  	    log.info("Expected "+tokenCount+" received "+mqttReceiver.receivedMessageCount());
    	  	Thread.sleep(10);
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Exception:" + methodName + " exception="+ exception);
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
}
