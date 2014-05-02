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

package org.eclipse.paho.client.mqttv3.test.client;

import java.net.URI;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;


/**
 *
 */
public class MqttClientFactoryPaho {

  /**
   * @param serverURI 
   * @param clientId 
   * @return MqttClient
   * @throws Exception 
   */
  public IMqttClient createMqttClient(URI serverURI, String clientId) throws Exception {
    return new MqttClientPaho(serverURI.toString(), clientId);
  }

  /**
   * @param serverURI 
   * @param clientId 
   * @param persistence 
   * @return MqttClient 
   * @throws Exception 
   */
  public IMqttClient createMqttClient(URI serverURI, String clientId, MqttClientPersistence persistence) throws Exception {
    return new MqttClientPaho(serverURI.toString(), clientId, persistence);
  }

  /**
   * @param serverURI 
   * @param clientId 
   * @return client
   * @throws Exception 
   */
  public IMqttAsyncClient createMqttAsyncClient(URI serverURI, String clientId) throws Exception {
    return new MqttAsyncClientPaho(serverURI.toString(), clientId);
  }

  /**
   * @param serverURI 
   * @param clientId 
   * @param persistence 
   * @return client
   * @throws Exception 
   */
  public IMqttAsyncClient createMqttAsyncClient(URI serverURI, String clientId, MqttClientPersistence persistence) throws Exception {
    return new MqttAsyncClientPaho(serverURI.toString(), clientId, persistence);
  }

  /**
   * 
   */
  public void open() {
    // empty
  }

  /**
   * 
   */
  public void close() {
    // empty
  }

  /**
   * 
   */
  public void disconnect() {
    // empty
  }

  /**
   * @return flag indicating if this client supports High Availability
   */
  public boolean isHighAvalabilitySupported() {
    return true;
  }

}
