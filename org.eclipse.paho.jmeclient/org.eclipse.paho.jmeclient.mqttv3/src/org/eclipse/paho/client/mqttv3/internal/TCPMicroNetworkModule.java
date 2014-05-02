/* @start_prolog@
 * Version: %Z% %W% %I% %E% %U%  
 * ============================================================================
 *   <copyright 
 *   notice="oco-source" 
 *   pids="5724-H72," 
 *   years="2010,2012" 
 *   crc="2914692611" > 
 *   IBM Confidential 
 *    
 *   OCO Source Materials 
 *    
 *   5724-H72, 
 *    
 *   (C) Copyright IBM Corp. 2010, 2012 
 *    
 *   The source code for the program is not published 
 *   or otherwise divested of its trade secrets, 
 *   irrespective of what has been deposited with the 
 *   U.S. Copyright Office. 
 *   </copyright> 
 * ============================================================================
 * @end_prolog@
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;


/**
 * A network module for connecting over TCP from Java ME (CLDC profile).
 */
public class TCPMicroNetworkModule implements NetworkModule {
	private String uri;
	private SocketConnection connection;
	private InputStream in;
	private OutputStream out;
	final static String className = SSLMicroNetworkModule.class.getName();
	Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,className);
	
	/**
	 * Constructs a new MicroTCPNetworkModule using the specified host and
	 * port.
	 * 
	 * @param host the host name to connect to
	 * @param port the port to connect to
	 * @param secure whether or not to use SSL.
	 */
	public TCPMicroNetworkModule(String host, int port) {
		this.uri = "socket://" + host + ":" + port;
	}
	
	/**
	 * Starts the module, by creating a TCP socket to the server.
	 */
	public void start() throws IOException, MqttException {
		final String methodName = "start";
		try {
			log.fine(className,methodName, "252", new Object[] {uri});
			connection = (SocketConnection) Connector.open(uri);
			connection.setSocketOption(SocketConnection.DELAY, 0);  // Do not use Nagle's algorithm
			in = connection.openInputStream();
			out = connection.openOutputStream();
		}
		catch (IOException ex) {
			//@TRACE 250=Failed to create TCP socket
			log.fine(className,methodName,"250",null,ex);
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
