/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.paho.client.mqttv3.MqttException;


public interface NetworkModule {
	void start() throws IOException, MqttException;
	
	InputStream getInputStream() throws IOException;
	
	OutputStream getOutputStream() throws IOException;
	
	void stop() throws IOException;
}
