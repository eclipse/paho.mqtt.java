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
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.client.MqttTopic;
import org.eclipse.paho.mqttv5.client.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.MqttV5Receiver;
import org.eclipse.paho.mqttv5.client.test.utilities.MqttV5Receiver.ReceivedMessage;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.common.test.categories.MQTTV3Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category({OnlineTest.class, MQTTV3Test.class})
public class LiveTakeOverTest {
  private static final Logger log = Logger.getLogger(LiveTakeOverTest.class.getName());

  private static URI serverURI;
  private static MqttClientFactoryPaho clientFactory;
  private static String topicPrefix;


  static enum FirstClientState {
    INITIAL,
    READY,
    RUNNING,
    FINISHED,
    ERROR
  }

  private static String ClientId = "TakeOverClient";
  private static String FirstSubTopicString;

  /**
   * @throws Exception 
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    try {
      String methodName = Utility.getMethodName();
      LoggingUtilities.banner(log, LiveTakeOverTest.class, methodName);

      serverURI = TestProperties.getServerURI();
      clientFactory = new MqttClientFactoryPaho();
      clientFactory.open();
      topicPrefix = "FirstClientState-" + UUID.randomUUID().toString() + "-";
      FirstSubTopicString = topicPrefix + "FirstClient/Topic";

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
    LoggingUtilities.banner(log, LiveTakeOverTest.class, methodName);

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
   * Test that a client actively doing work can be taken over
   * @throws Exception 
   */
  @Test(timeout=10000)
  public void testLiveTakeOver() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, LiveTakeOverTest.class, methodName);
    log.entering(LiveTakeOverTest.class.getName(), methodName);

    IMqttClient mqttClient = null;
    try {
      FirstClient firstClient = new FirstClient();
      Thread firstClientThread = new Thread(firstClient);
      log.info("Starting the firstClient thread");
      firstClientThread.start();
      log.info("firstClientThread Started");

      firstClient.waitForState(FirstClientState.READY);

      log.fine("telling the 1st client to go and let it publish for 2 seconds");
      //Tell the first client to go and let it publish for a couple of seconds
      firstClient.setState(FirstClientState.RUNNING);
      Thread.sleep(2000);

      log.fine("Client has been run for 2 seconds, now taking over connection");

      //Now lets take over the connection  
      // Create a second MQTT client connection with the same clientid. The 
      // server should spot this and kick the first client connection off. 
      // To do this from the same box the 2nd client needs to use either
      // a different form of persistent store or a different locaiton for 
      // the store to the first client. 
      // MqttClientPersistence persist = new MemoryPersistence();
      mqttClient = clientFactory.createMqttClient(serverURI, ClientId, null);

      MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
      mqttClient.setCallback(mqttV5Receiver);
      MqttConnectionOptions mqttConnectOptions = new MqttConnectionOptions();
      mqttConnectOptions.setCleanStart(false);
      MqttMessage message = new MqttMessage();
      message.setQos(2);
      message.setRetained(true);
      message.setPayload("payload".getBytes());
      mqttConnectOptions.setWill(topicPrefix+"WillTopic", message);
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + ClientId);
      mqttClient.connect(mqttConnectOptions);

      //We should have taken over the first Client's subscription...we may have some
      //of his publishes arrive.
      // NOTE: as a different persistence is used for the second client any inflight 
      // publications from the client will not be recovered / restarted. This will 
      // leave debris on the server.
      log.fine("We should have taken over the first Client's subscription...we may have some of his publishes arrive.");
      //Ignore his publishes that arrive...
      ReceivedMessage oldMsg;
      do {
        oldMsg = mqttV5Receiver.receiveNext(100);
      }
      while (oldMsg != null);

      log.fine("Now check we have grabbed his subscription by publishing..");
      //Now check we have grabbed his subscription by publishing..
      byte[] payload = ("Message payload from second client " + getClass().getName() + "." + methodName).getBytes();
      MqttTopic mqttTopic = mqttClient.getTopic(FirstSubTopicString);
      log.info("Publishing to..." + FirstSubTopicString);
      mqttTopic.publish(payload, 1, false);
      log.info("Publish sent, checking for receipt...");

      boolean ok = mqttV5Receiver.validateReceipt(FirstSubTopicString, 1, payload);
      if (!ok) {
        throw new Exception("Receive failed");
      }
    }
    catch (Exception exception) {
      log.throwing(LiveTakeOverTest.class.getName(), methodName, exception);
      throw exception;
    }
    finally {
      try {
        if (mqttClient != null) {
          mqttClient.disconnect();
          log.info("Disconnecting...");
          mqttClient.close();
          log.info("Close...");
        }
      }
      catch (Exception exception) {
        log.throwing(LiveTakeOverTest.class.getName(), methodName, exception);
        throw exception;
      }
    }

    log.exiting(LiveTakeOverTest.class.getName(), methodName);
  }

  class FirstClient implements Runnable {

    private FirstClientState state = FirstClientState.INITIAL;
    public Object stateLock = new Object();
    IMqttClient mqttClient = null;
    MqttV5Receiver mqttV5Receiver = null;

    void waitForState(FirstClientState desiredState) throws InterruptedException {
      final String methodName = "waitForState";
      synchronized (stateLock) {
        while ((state != desiredState) && (state != FirstClientState.ERROR)) {
          try {
            stateLock.wait();
          }
          catch (InterruptedException exception) {
            log.throwing(LiveTakeOverTest.class.getName(), methodName, exception);
            throw exception;
          }
        }

        if (state == FirstClientState.ERROR) {
          Assert.fail("Firstclient entered an ERROR state");
        }
      }
      log.exiting(LiveTakeOverTest.class.getName(), methodName);
    }

    void setState(FirstClientState newState) {
      synchronized (stateLock) {
        state = newState;
        stateLock.notifyAll();
      }
    }

    void connectAndSub() {
      String methodName = Utility.getMethodName();
      try {
        mqttClient = clientFactory.createMqttClient(serverURI, ClientId);
        mqttV5Receiver = new MqttV5Receiver(mqttClient.getClientId(), LoggingUtilities.getPrintStream());
        mqttV5Receiver.setReportConnectionLoss(false);
        mqttClient.setCallback(mqttV5Receiver);
        MqttConnectionOptions mqttConnectOptions = new MqttConnectionOptions();
        mqttConnectOptions.setCleanStart(false);
        mqttConnectOptions.setSessionExpiryInterval(9999L);
        MqttMessage message = new MqttMessage();
        message.setQos(2);
        message.setRetained(true);
        message.setPayload("payload".getBytes());
        mqttConnectOptions.setWill(topicPrefix + "WillTopic", message);
        log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + ClientId);
        mqttClient.connect(mqttConnectOptions);
        log.info("Subscribing to..." + FirstSubTopicString);
        mqttClient.subscribe(FirstSubTopicString, 2);
      }
      catch (Exception exception) {
        log.log(Level.SEVERE, "caugh exception:" + exception);
        setState(FirstClientState.ERROR);
        Assert.fail("Failed ConnectAndSub exception=" + exception);
      }
    }

    void repeatedlyPub() {
      String methodName = Utility.getMethodName();

      int i = 0;
      while (mqttClient.isConnected()) {
        try {
          if (i > 999999) {
            i = 0;
          }
          byte[] payload = ("Message payload " + getClass().getName() + ".publish" + (i++)).getBytes();
          MqttTopic mqttTopic = mqttClient.getTopic(FirstSubTopicString);
          log.fine("Publishing to..." + FirstSubTopicString);
          MqttToken token = mqttTopic.publish(payload, 1, false);
          token.waitForCompletion();
        }
        catch (Exception exception) {
          log.fine("Caught exception:" + exception);
          // Don't fail - we are going to get an exception as we disconnected during takeOver
          // Its likely the publish rate is too high i.e. inflight window is full
        }
      }
      log.info("Sent at least " + i + " messages.");
    }

    public void run() {
      String methodName = Utility.getMethodName();
      LoggingUtilities.banner(log, LiveTakeOverTest.class, methodName);
      log.entering(LiveTakeOverTest.class.getName(), methodName);

      connectAndSub();
      try {
        setState(FirstClientState.READY);
        waitForState(FirstClientState.RUNNING);
        repeatedlyPub();
        log.info("FirstClient exiting...");
        log.exiting(LiveTakeOverTest.class.getName(), methodName);

        mqttClient.close();

      }
      catch (InterruptedException exception) {
        setState(FirstClientState.ERROR);
        log.log(Level.SEVERE, "caught exception:", exception);
      }
      catch (MqttException exception) {
        setState(FirstClientState.ERROR);
        log.log(Level.SEVERE, "caught exception:", exception);
      }
    }
  }
}
