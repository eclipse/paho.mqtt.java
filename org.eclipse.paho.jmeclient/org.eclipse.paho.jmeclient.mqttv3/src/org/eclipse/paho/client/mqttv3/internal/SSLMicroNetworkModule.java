/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SecurityInfo;
import javax.microedition.io.SocketConnection;
import javax.microedition.io.SecureConnection;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;


/**
 * A network module for connecting over SSL from Java ME (CLDC profile).
 */
public class SSLMicroNetworkModule implements NetworkModule {
	private String uri;
	private SecureConnection connection;
	private InputStream in;
	private OutputStream out;
	final static String className = SSLMicroNetworkModule.class.getName();
	Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,className);
	
	/**
	 * Constructs a new SSLMicroNetworkModule using the specified host and
	 * port.
	 * 
	 * @param host the host name to connect to
	 * @param port the port to connect to
	 * @param secure whether or not to use SSL.
	 */
	public SSLMicroNetworkModule(String host, int port) {
		this.uri = "ssl://" + host + ":" + port;
	}
	
	/**
	 * Starts the module, by creating a TCP socket to the server.
	 */
	public void start() throws IOException, MqttException {
		final String methodName = "start";
		try {
			log.fine(className,methodName, "252", new Object[] {uri});
			connection = (SecureConnection) Connector.open(uri);
			connection.setSocketOption(SocketConnection.DELAY, 0);  // Do not use Nagle's algorithm
			in = connection.openInputStream();
			out = connection.openOutputStream();
		}
		catch (IOException ex) {
			System.out.println(ex.getMessage());
			//@TRACE 250=Failed to create TCP socket
			log.fine(className,methodName,"250",null,ex);
			ex.printStackTrace();
			throw new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR, ex);
		}
	}
	
	public InputStream getInputStream() {
		return in;
	}

	public OutputStream getOutputStream() {
		return out;
	}

	/**
	 * Stops the module, by closing the TCP socket.
	 */
	public void stop() throws IOException {
		in.close();
		out.close();
		connection.close();
	}
}
