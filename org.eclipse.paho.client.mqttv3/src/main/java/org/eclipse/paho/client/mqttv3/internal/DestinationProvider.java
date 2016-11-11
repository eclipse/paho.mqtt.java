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
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttTopic;

/**
 * This interface exists to act as a common type for
 * MqttClient and MqttMIDPClient so they can be passed to
 * ClientComms without either client class need to know
 * about the other.
 * Specifically, this allows the MIDP client to work
 * without the non-MIDP MqttClient/MqttConnectOptions
 * classes being present.
 */
public interface DestinationProvider {
	public MqttTopic getTopic(String topic);
}
