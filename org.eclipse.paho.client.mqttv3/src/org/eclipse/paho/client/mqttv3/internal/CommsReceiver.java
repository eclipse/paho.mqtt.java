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

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnack;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttInputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;


/**
 * Receives MQTT packets from the server.
 */
public class CommsReceiver implements Runnable {
	private boolean running = false;
	private Object lifecycle = new Object();
	private ClientState clientState = null;
	private ClientComms clientComms = null;
	private MqttInputStream in;
	private CommsTokenStore tokenStore = null;
	private boolean disconnecting = false;
	private Trace trace;
	
	public CommsReceiver(Trace trace, ClientComms clientComms, ClientState clientState, CommsTokenStore tokenStore, InputStream in) {
		this.in = new MqttInputStream(in);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
		this.trace = trace;
	}
	
	/**
	 * Starts up the Receiver's thread.
	 */
	public void start() {
		if (running == false) {
			running = true;
			new Thread(this, "MQTT Client Comms Receiver").start();
		}
	}

	/**
	 * Stops the Receiver's thread.  This call will block.
	 */
	public void stop() throws IOException {
		synchronized (lifecycle) {
			//@TRACE 850=stopping receiver
			trace.trace(Trace.FINE,850);
			if (running) {
				running = false;
				try {
					//@TRACE 851=stop: wait on lifecycle
					trace.trace(Trace.FINE,851);
					// Wait for the thread to finish.
					lifecycle.wait();
				}
				catch (InterruptedException ex) {
				}
			}
		}
	}
	
	/**
	 * Run loop to receive messages from the server.
	 */
	public void run() {
		while (running && (in != null)) {
			try {
				//@TRACE 852=run: read message
				trace.trace(Trace.FINE,852);
				MqttWireMessage message = in.readMqttWireMessage();
				if (message instanceof MqttAck) {
					MqttDeliveryToken token = tokenStore.getToken(message);
					if (token!=null) {
						synchronized (token) {
							clientState.notifyReceived(message);
							if (message instanceof MqttConnack && ((MqttConnack)message).getReturnCode() != 0) {
								synchronized (lifecycle) {
									running = false;
								}
							}
						}
					} else {
						clientState.notifyReceived(message);
					}
				}
				else {
					clientState.notifyReceived(message);
				}
			}
			catch (MqttException ex) {
				running = false;
				clientComms.shutdownConnection(ex);
			} 
			catch (IOException ioe) {
				//@TRACE 853=run: IOException
				trace.trace(Trace.FINE,853,null,ioe);

				running = false;
				// An EOFException could be raised if the broker processes the 
				// DISCONNECT before we receive the ACK for it. As such,
				// only shutdown the connection if we're not already shutting down.
				if (!disconnecting) {
					clientComms.shutdownConnection(new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ioe));
				} else {
					clientComms.shutdownConnection(null);
				}

			}
		}
		synchronized (lifecycle) {
			//@TRACE 854=run: notify lifecycle
			trace.trace(Trace.FINE,854);
			lifecycle.notifyAll();
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void setDisconnecting(boolean disconnecting) {
		//@TRACE 855=setDisconnecting disconnecting={0}
		trace.trace(Trace.FINE,855, new Object[]{new Boolean(disconnecting)});
		this.disconnecting = disconnecting;
	}

}
