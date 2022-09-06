/* Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Tests providing how to use PasswdHandler to change password when client connecct.
 */
public class DynamicPwdTest {

  static final Class<?> cclass = DynamicPwdTest.class;
  private static final String className = cclass.getName();
  private static final Logger log = Logger.getLogger(className);

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
      LoggingUtilities.banner(log, cclass, methodName);

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


  @Test
  public void testDynamicPwd() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);

    PasswdHandler.Factory.setPasswdHandler(new TestPasswdHandler());

    IMqttAsyncClient client = clientFactory.createMqttAsyncClient(serverURI, "client-1");

    MqttConnectOptions options = new MqttConnectOptions();
    options.setAutomaticReconnect(true);
    options.setUserName("foo");
    options.setPassword("123456".toCharArray());
    options.setConnectionTimeout(2);
    client.connect(options);

    Thread.sleep(1000);

    try {
      client.disconnect(0).waitForCompletion();
    } finally {
      client.close();
    }

  }

  public class TestPasswdHandler implements PasswdHandler {

    @Override
    public String handler(NetworkModule networkModule) {
      String url = networkModule.getServerURI();
      String msg = "url:"+url + ",timestamp:" + System.currentTimeMillis();
      log.info(msg);
      return msg;
    }
  }
}
