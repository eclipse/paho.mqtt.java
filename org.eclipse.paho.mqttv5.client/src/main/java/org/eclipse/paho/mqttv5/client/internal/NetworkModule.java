/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.mqttv5.client.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.paho.mqttv5.common.MqttException;


public interface NetworkModule {
	void start() throws IOException, MqttException;
	
	InputStream getInputStream() throws IOException;
	
	OutputStream getOutputStream() throws IOException;
	
	void stop() throws IOException;

	String getServerURI();
}
