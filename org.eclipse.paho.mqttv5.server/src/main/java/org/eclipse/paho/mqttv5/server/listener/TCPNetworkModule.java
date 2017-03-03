/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.server.listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.common.MqttException;

public class TCPNetworkModule implements NetworkModule {
	
	private static final Logger log = Logger.getLogger(TCPNetworkModule.class.getName());
	
	private Socket socket;
	
	/**
	 * Constructs a new TCPNetworkModule using the specified socket.
	 * @param socket the {@link Socket} to be used to set up the connection.
	 */
	public TCPNetworkModule(Socket socket ) {
		this.socket = socket;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	public void stop() throws IOException {
		if(socket != null){
			socket.shutdownOutput();
			socket.close();
		}
		
	}

}
