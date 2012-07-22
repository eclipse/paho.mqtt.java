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
import java.io.OutputStream;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttOutputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;


public class CommsSender implements Runnable {
	/**
	 * Receives MQTT packets from the server.
	 */
	private boolean running = false;
	private Object lifecycle = new Object();
	private ClientState clientState = null;
	private MqttOutputStream out;
	private ClientComms clientComms = null;
	private CommsTokenStore tokenStore = null;
	private Trace trace;
	
	public CommsSender(Trace trace, ClientComms clientComms, ClientState clientState, CommsTokenStore tokenStore, OutputStream out) {
		this.trace = trace;
		this.out = new MqttOutputStream(out);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
	}
	
	/**
	 * Starts up the Sender thread.
	 */
	public void start() {
		if (running == false) {
			running = true;
			new Thread(this, "MQTT Client Comms Sender").start();
		}
	}

	/**
	 * Stops the Sender's thread.  This call will block.
	 */
	public void stop() throws IOException {
		synchronized (lifecycle) {
			//@TRACE 800=stopping sender
			trace.trace(Trace.FINE,800);
			if (running) {
				running = false;
				try {
					//@TRACE 801=stop: wait on lifecycle
					trace.trace(Trace.FINE,801);
					// Wait for the thread to finish.
					lifecycle.wait();
				}
				catch (InterruptedException ex) {
				}
			}
		}
	}
	
	public void run() {
		MqttWireMessage message = null;
		while (running && (out != null)) {
			try {
				//@TRACE 802=run: get message
				trace.trace(Trace.FINE,802);
				message = clientState.get();
				if (message != null) {
					if (message instanceof MqttAck) {
						out.write(message);
						out.flush();
					}
					else {
						MqttDeliveryToken token = tokenStore.getToken(message);
						synchronized (token) {
							out.write(message);
							out.flush();
							clientState.notifySent(message);
						}
					}
					
					if (message instanceof MqttDisconnect) {
						synchronized (lifecycle) {
							//@TRACE 803=run: sent disconnect
							trace.trace(Trace.FINE,803);
							running = false;
						}
					}
				} else {
					synchronized (lifecycle) {
						running = false;
					}
				}
			} catch (MqttException me) {
				synchronized (lifecycle) {
					running = false;
				}
				clientComms.shutdownConnection(me);
			} catch (Exception ioe) {
				//@TRACE 804=run: exception
				trace.trace(Trace.FINE,804,null,ioe);
				if (message != null && message instanceof MqttDisconnect) {
					// An IO exception whilst sending the disconnect will
					// cause the application thread to stay blocked
					// on the token if we don't pretend it has successfully
					// been sent.
					clientState.notifySent(message);
				}
				running = false;
				clientComms.shutdownConnection(new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ioe));
			}
		}
		synchronized (lifecycle) {
			//@TRACE 805=run: notify lifecycle
			trace.trace(Trace.FINE,805);
			lifecycle.notifyAll();
		}
	}
	
	public boolean isRunning() {
		return running;
	}
}
