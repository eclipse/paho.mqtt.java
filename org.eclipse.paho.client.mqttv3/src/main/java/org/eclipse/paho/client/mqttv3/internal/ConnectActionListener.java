/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttToken;

/**
 * <p>This class handles the connection of the AsyncClient to one of the available URLs.</p>  
 * <p>The URLs are supplied as either the singleton when the client is created, or as a list in the connect options.</p> 
 * <p>This class uses its own onSuccess and onFailure callbacks in preference to the user supplied callbacks.</p>
 * <p>An attempt is made to connect to each URL in the list until either a connection attempt succeeds or all the URLs have been tried</p> 
 * <p>If a connection succeeds then the users token is notified and the users onSuccess callback is called.</p>
 * <p>If a connection fails then another URL in the list is attempted, otherwise the users token is notified 
 * and the users onFailure callback is called</p>
 */
public class ConnectActionListener implements IMqttActionListener {

  private MqttClientPersistence persistence;
  private MqttAsyncClient client;
  private ClientComms comms;
  private MqttConnectOptions options;
  private MqttToken userToken;
  private Object userContext;
  private IMqttActionListener userCallback;

  /**
   * @param persistence
   * @param client 
   * @param comms
   * @param options 
   * @param userToken  
   * @param userContext
   * @param userCallback
   */
  public ConnectActionListener(
      MqttAsyncClient client,
      MqttClientPersistence persistence,
      ClientComms comms,
      MqttConnectOptions options,
      MqttToken userToken,
      Object userContext,
      IMqttActionListener userCallback) {
    this.persistence = persistence;
    this.client = client;
    this.comms = comms;
    this.options = options;
    this.userToken = userToken;
    this.userContext = userContext;
    this.userCallback = userCallback;
  }

  /**
   * If the connect succeeded then call the users onSuccess callback
   * 
   * @param token 
   */
  public void onSuccess(IMqttToken token) {

    userToken.internalTok.markComplete(null, null);
    userToken.internalTok.notifyComplete();

    if (userCallback != null) {
      userToken.setUserContext(userContext);
      userCallback.onSuccess(userToken);
    }
  }

  /**
   * The connect failed, so try the next URI on the list.
   * If there are no more URIs, then fail the overall connect. 
   * 
   * @param token 
   * @param exception 
   */
  public void onFailure(IMqttToken token, Throwable exception) {

    int numberOfURIs = comms.getNetworkModules().length;
    int index = 1 + comms.getNetworkModuleIndex();

    if (index < numberOfURIs) {
      comms.setNetworkModuleIndex(index);
      try {
        connect();
      }
      catch (MqttPersistenceException e) {
        onFailure(token, e); // try the next URI in the list
      }
    }
    else {
      MqttException ex;
      if (exception instanceof MqttException) {
        ex = (MqttException) exception;
      }
      else {
        ex = new MqttException(exception);
      }
      userToken.internalTok.markComplete(null, ex);
      userToken.internalTok.notifyComplete();

      if (userCallback != null) {
        userToken.setUserContext(userContext);
        userCallback.onFailure(userToken, exception);
      }
    }
  }

  /**
   * The connect failed, so try the next URI on the list.
   * If there are no more URIs, then fail the overall connect. 
   * @throws MqttPersistenceException 
   */
  public void connect() throws MqttPersistenceException {
    MqttToken token = new MqttToken(client.getClientId());
    token.setActionCallback(this);
    token.setUserContext(this);

    persistence.open(client.getClientId(), client.getServerURI());

    if (options.isCleanSession()) {
      persistence.clear();
    }

    try {
      comms.connect(options, token);
    }
    catch (MqttException e) {
      onFailure(token, e);
    }
  }

}
