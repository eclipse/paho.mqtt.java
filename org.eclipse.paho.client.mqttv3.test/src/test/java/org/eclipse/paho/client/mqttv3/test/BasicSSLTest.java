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

import java.io.File;
import java.net.URI;
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
import org.eclipse.paho.client.mqttv3.test.utilities.MqttV3Receiver;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * This test aims to run some basic SSL functionality tests of the MQTT client
 */

public class BasicSSLTest {

  static final Class<?> cclass = BasicSSLTest.class;
  private static final String className = cclass.getName();
  private static final Logger log = Logger.getLogger(className);

  private static URI serverURI;
  private static String serverHost;
  private static MqttClientFactoryPaho clientFactory;
  private static File keystorePath;
  private static int messageSize = 100000;

  /**
   * @throws Exception 
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    try {
      String methodName = Utility.getMethodName();
      LoggingUtilities.banner(log, cclass, methodName);

      serverURI = TestProperties.getServerURI();
      serverHost = serverURI.getHost();
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
   * An ssl connection with server cert authentication, simple pub/sub
   * @throws Exception
   */
  @Test
  public void testSSL() throws Exception {
    URI serverURI = new URI("ssl://" + serverHost + ":" + TestProperties.getServerSSLPort());
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback");
      mqttClient.setCallback(mqttV3Receiver);

      log.info("Setting SSL properties...");
      System.setProperty("javax.net.ssl.keyStore", TestProperties.getClientKeyStore());
      System.setProperty("javax.net.ssl.keyStorePassword", TestProperties.getClientKeyStorePassword());
      System.setProperty("javax.net.ssl.trustStore", TestProperties.getClientTrustStore());
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();

      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {2};
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);

      byte[] payload = ("Message payload " + getClass().getName() + "." + methodName).getBytes();
      MqttTopic mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(payload, 2, false);

      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payload);
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
        if ((mqttClient != null) && mqttClient.isConnected()) {
          log.info("Disconnecting...");
          mqttClient.disconnect();
        }
        if (mqttClient != null) {
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
   * An ssl connection with server cert authentication, small workload with multiple clients
   * @throws Exception
   */
  @Test
  public void testSSLWorkload() throws Exception {
    URI serverURI = new URI("ssl://" + serverHost + ":" + TestProperties.getServerSSLPort());
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient[] mqttPublisher = new IMqttClient[4];
    IMqttClient[] mqttSubscriber = new IMqttClient[20];
    try {
      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {0};

      MqttTopic[] mqttTopic = new MqttTopic[mqttPublisher.length];
      for (int i = 0; i < mqttPublisher.length; i++) {
        mqttPublisher[i] = clientFactory.createMqttClient(serverURI, "MultiPub" + i);

        log.info("Setting SSL properties...ClientId: MultiPub" + i);
        System.setProperty("javax.net.ssl.keyStore", TestProperties.getClientKeyStore());
        System.setProperty("javax.net.ssl.keyStorePassword", TestProperties.getClientKeyStorePassword());
        System.setProperty("javax.net.ssl.trustStore", TestProperties.getClientKeyStore());
        System.setProperty("javax.net.ssl.trustStorePassword", TestProperties.getClientKeyStorePassword());
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

        log.info("Setting SSL properties...ClientId: MultiSubscriber" + i);
        System.setProperty("javax.net.ssl.keyStore", TestProperties.getClientKeyStore());
        System.setProperty("javax.net.ssl.keyStorePassword", TestProperties.getClientKeyStorePassword());
        System.setProperty("javax.net.ssl.trustStore", TestProperties.getClientKeyStore());
        System.setProperty("javax.net.ssl.trustStorePassword", TestProperties.getClientKeyStorePassword());
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

      }
      catch (Exception exception) {
        log.log(Level.SEVERE, "caught exception:", exception);
      }
    }

    log.exiting(className, methodName);
  }

  /**
   * An ssl connection with server cert authentication, simple pub/sub of a large message
   * 'messageSize' defined at start of test, change it to meet your requirements
   * @throws Exception
   */
  @Test
  public void testSSLLargeMessage() throws Exception {
    URI serverURI = new URI("ssl://" + serverHost + ":" + TestProperties.getServerSSLPort());
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(serverURI, methodName);
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);

      log.info("Setting SSL properties...");
      System.setProperty("javax.net.ssl.keyStore", TestProperties.getClientKeyStore());
      System.setProperty("javax.net.ssl.keyStorePassword", TestProperties.getClientKeyStorePassword());
      System.setProperty("javax.net.ssl.trustStore", TestProperties.getClientKeyStore());
      System.setProperty("javax.net.ssl.trustStorePassword", TestProperties.getClientKeyStorePassword());
      log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
      mqttClient.connect();

      String[] topicNames = new String[]{methodName + "/Topic"};
      int[] topicQos = {2};
      log.info("Subscribing to..." + topicNames[0]);
      mqttClient.subscribe(topicNames, topicQos);

      // Create message of size 'messageSize'
      byte[] message = new byte[messageSize];
      java.util.Arrays.fill(message, (byte) 's');

      MqttTopic mqttTopic = mqttClient.getTopic(topicNames[0]);
      log.info("Publishing to..." + topicNames[0]);
      mqttTopic.publish(message, 2, false);
      boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, message);
      if (!ok) {
        Assert.fail("Receive failed");
      }
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      try {
        if ((mqttClient != null) && mqttClient.isConnected()) {
          log.info("Disconnecting...");
          mqttClient.disconnect();
        }
        if (mqttClient != null) {
          log.info("Close...");
          mqttClient.close();
        }
      }
      catch (Exception exception) {
        log.log(Level.SEVERE, "caught exception:", exception);
        throw exception;
      }
    }
    log.exiting(className, methodName);
  }

  /**
   * A non ssl connection to an ssl channel
   * @throws Exception
   */
  @Test
  public void testNonSSLtoSSLChannel() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(new URI("tcp://" + serverHost + ":" + TestProperties.getServerSSLPort()) , methodName);
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      mqttClient.setCallback(mqttV3Receiver);
      log.info("Assigning callback...");
      try {
        log.info("Connecting...Expect to fail");
        mqttClient.connect();
        Assert.fail("Non SSL Connection was allowed to SSL channel with Client Authentication");
      }
      catch (Exception e) {
        // Expected exception
      }

    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      try {
        if ((mqttClient != null) && mqttClient.isConnected()) {
          log.info("Disconnecting...");
          mqttClient.disconnect();
        }
        if (mqttClient != null) {
          log.info("Close...");
          mqttClient.close();
        }

      }
      catch (Exception exception) {
        log.log(Level.SEVERE, "caught exception:", exception);
        throw exception;
      }
    }
    log.exiting(className, methodName);
  }

  /**
   * Try ssl connection to channel without ssl
   * @throws Exception
   */
  @Test
  public void testSSLtoNonSSLChannel() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttClient mqttClient = null;
    try {
      mqttClient = clientFactory.createMqttClient(new URI("ssl://" + serverHost + ":18883"), methodName);
      MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, LoggingUtilities.getPrintStream());
      log.info("Assigning callback...");
      mqttClient.setCallback(mqttV3Receiver);

      log.info("Setting SSL properties...");
      System.setProperty("javax.net.ssl.keyStore", TestProperties.getClientKeyStore());
      System.setProperty("javax.net.ssl.keyStorePassword", TestProperties.getClientKeyStorePassword());
      System.setProperty("javax.net.ssl.trustStore", TestProperties.getClientKeyStore());
      System.setProperty("javax.net.ssl.trustStorePassword", TestProperties.getClientKeyStorePassword());
      try {
        log.info("Connecting...Expect to fail");
        mqttClient.connect();
        Assert.fail("SSL Connection was allowed to a channel without SSL");
      }
      catch (Exception e) {
        // Expected exception
      }

    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      try {
        if ((mqttClient != null) && mqttClient.isConnected()) {
          log.info("Disconnecting...");
          mqttClient.disconnect();
        }
        if (mqttClient != null) {
          log.info("Close...");
          mqttClient.close();
        }

      }
      catch (Exception exception) {
        log.log(Level.SEVERE, "caught exception:", exception);
        throw exception;
      }
    }
    log.exiting(className, methodName);
  }
}
