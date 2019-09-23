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
package org.eclipse.paho.mqttv5.client.internal;

import org.eclipse.paho.mqttv5.client.BufferedMessage;
import org.eclipse.paho.mqttv5.common.MqttException;

public interface IDisconnectedBufferCallback {
	
	void publishBufferedMessage(BufferedMessage bufferedMessage) throws MqttException;

}
