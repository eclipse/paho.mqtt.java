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
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.MqttV3Receiver;
import org.eclipse.paho.client.mqttv3.test.utilities.MqttV3Receiver.ReceivedMessage;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LiveTakeOverTest {

  private static final Class<?> cclass = LiveTakeOverTest.class;
  private static final String className = cclass.getName();
  private static final Logger log = Logger.getLogger(className);

  private static URI serverURI;
  private static MqttClientFactoryPaho clientFactory;

  static enum FirstClientState {
    INITIAL,
    READY,
    RUNNING,
    FINISHED,
    ERROR
  }

  private static String ClientId = "TakeOverClient";
  private static String FirstSubTopicString = "FirstClient/Topic";

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
   * Test that a client actively doing work can be taken over
   * @throws Exception 
   */
  @Test
  public void testLiveTakeOver() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

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

      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      mqttClient.setCallback(mqttV3Receiver);
      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setCleanSession(false);
      mqttConnectOptions.setWill("WillTopic", "payload".getBytes(), 2, true);
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
        oldMsg = mqttV3Receiver.receiveNext(1000);
      }
      while (oldMsg != null);

      log.fine("Now check we have grabbed his subscription by publishing..");
      //Now check we have grabbed his subscription by publishing..
      byte[] payload = ("Message payload from second client " + getClass().getName() + "." + methodName).getBytes();
      MqttTopic mqttTopic = mqttClient.getTopic(FirstSubTopicString);
      log.info("Publishing to..." + FirstSubTopicString);
      mqttTopic.publish(payload, 1, false);
      log.info("Publish sent, checking for receipt...");

      boolean ok = mqttV3Receiver.validateReceipt(FirstSubTopicString, 1, payload);
      if (!ok) {
        throw new Exception("Receive failed");
      }
    }
    catch (Exception exception) {
      log.throwing(className, methodName, exception);
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
        log.throwing(className, methodName, exception);
        throw exception;
      }
    }

    log.exiting(className, methodName);
  }

  class FirstClient implements Runnable {

    private FirstClientState state = FirstClientState.INITIAL;
    public Object stateLock = new Object();
    IMqttClient mqttClient = null;
    MqttV3Receiver mqttV3Receiver = null;

    void waitForState(FirstClientState desiredState) throws InterruptedException {
      final String methodName = "waitForState";
      synchronized (stateLock) {
        while ((state != desiredState) && (state != FirstClientState.ERROR)) {
          try {
            stateLock.wait();
          }
          catch (InterruptedException exception) {
            log.throwing(className, methodName, exception);
            throw exception;
          }
        }

        if (state == FirstClientState.ERROR) {
          Assert.fail("Firstclient entered an ERROR state");
        }
      }
      log.exiting(className, methodName);
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
        mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
        mqttV3Receiver.setReportConnectionLoss(false);
        mqttClient.setCallback(mqttV3Receiver);
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setWill("WillTopic", "payload".getBytes(), 2, true);
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
          log.info("Publishing to..." + FirstSubTopicString);
          mqttTopic.publish(payload, 1, false);

        }
        catch (Exception exception) {
          log.fine("Caught exception:" + exception);
          // Don't fail - we are going to get an exception as we disconnected during takeOver
          // Its likely the publish rate is too high i.e. inflight window is full
        }
      }
    }

    public void run() {
      String methodName = Utility.getMethodName();
      LoggingUtilities.banner(log, cclass, methodName);
      log.entering(className, methodName);

      connectAndSub();
      try {
        setState(FirstClientState.READY);
        waitForState(FirstClientState.RUNNING);
        repeatedlyPub();
        log.info("FirstClient exiting...");
        log.exiting(className, methodName);

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
