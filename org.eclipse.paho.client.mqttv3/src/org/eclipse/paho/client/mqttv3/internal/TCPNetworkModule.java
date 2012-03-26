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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;


/**
 * A network module for connecting over TCP. 
 */
public class TCPNetworkModule implements NetworkModule {
	protected Socket socket;
	private SocketFactory factory;
	private String host;
	private int port;
	protected Trace trace;
	
	/**
	 * Constructs a new TCPNetworkModule using the specified host and
	 * port.  The supplied SocketFactory is used to supply the network
	 * socket.
	 */
	public TCPNetworkModule(Trace trace, SocketFactory factory, String host, int port) {
		this.factory = factory;
		this.host = host;
		this.port = port;
		this.trace = trace;
	}

	/**
	 * Starts the module, by creating a TCP socket to the server.
	 */
	public void start() throws IOException, MqttException {
		try {
//			InetAddress localAddr = InetAddress.getLocalHost();
//			socket = factory.createSocket(host, port, localAddr, 0);
			socket = factory.createSocket(host, port);
			socket.setTcpNoDelay(true);	// TCP_NODELAY on, which means we do not use Nagle's algorithm
		}
		catch (ConnectException ex) {
			//@TRACE 250=Failed to create TCP socket
			trace.trace(Trace.FINE,250,null,ex);
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR);
		}
	}

	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}
	
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	/**
	 * Stops the module, by closing the TCP socket.
	 */
	public void stop() throws IOException {
		if (socket != null) {
			socket.close();
		}
	}
}
