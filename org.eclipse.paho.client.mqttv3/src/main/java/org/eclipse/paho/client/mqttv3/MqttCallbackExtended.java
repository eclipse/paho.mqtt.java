/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 * Contributors:
 *    James Sutton - Initial Contribution for Automatic Reconnect & Offline Buffering
 */
package org.eclipse.paho.client.mqttv3;

/**
 * Extension of {@link MqttCallback} to allow new callbacks
 * without breaking the API for existing applications.
 * Classes implementing this interface can be registered on
 * both types of client: {@link IMqttClient#setCallback(MqttCallback)}
 * and {@link IMqttAsyncClient#setCallback(MqttCallback)}
 */
public interface MqttCallbackExtended extends MqttCallback {
	
	/**
	 * Called when the connection to the server is completed successfully.
	 * @param reconnect If true, the connection was the result of automatic reconnect.
	 * @param serverURI The server URI that the connection was made to.
	 */
    void connectComplete(boolean reconnect, String serverURI);

}
