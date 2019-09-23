/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corp.
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
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttOutputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;


public class CommsSender implements Runnable {
	private static final String CLASS_NAME = CommsSender.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	//Sends MQTT packets to the server on its own thread
	private enum State {STOPPED, RUNNING, STARTING}

    private State current_state = State.STOPPED;
	private State target_state = State.STOPPED;
	private final Object lifecycle = new Object();
	private Thread 	sendThread		= null;
	private String threadName;
	private Future<?> senderFuture;
	
	private ClientState clientState = null;
	private MqttOutputStream out;
	private ClientComms clientComms = null;
	private CommsTokenStore tokenStore = null;


	public CommsSender(ClientComms clientComms, ClientState clientState, CommsTokenStore tokenStore, OutputStream out) {
		this.out = new MqttOutputStream(clientState, out);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
		log.setResourceName(clientComms.getClient().getClientId());
	}

	/**
	 * Starts up the Sender thread.
	 * @param threadName the threadname
	 * @param executorService used to execute the thread
	 */
	public void start(String threadName, ExecutorService executorService) {
		this.threadName = threadName;
		synchronized (lifecycle) {
			if (current_state == State.STOPPED && target_state == State.STOPPED) {
				target_state = State.RUNNING;
				if (executorService == null) {
					new Thread(this).start();
				} else {
					senderFuture = executorService.submit(this);
				}
			}
		}
		while (!isRunning()) {
			try { Thread.sleep(100); } catch (Exception e) { }
		}
	}

	/**
	 * Stops the Sender's thread.  This call will block.
	 */
	public void stop() {
		final String methodName = "stop";
		
		if (!isRunning()) {
			return;
		}
			
		synchronized (lifecycle) {
			if (senderFuture != null) {
				senderFuture.cancel(true);
			}
			//@TRACE 800=stopping sender
			log.fine(CLASS_NAME,methodName,"800");
			if (isRunning()) {
				target_state = State.STOPPED;
				clientState.notifyQueueLock();
			}
		}
		while (isRunning()) {
			try { Thread.sleep(100); } catch (Exception e) { }
			clientState.notifyQueueLock();
		}
		//@TRACE 801=stopped
		log.fine(CLASS_NAME,methodName,"801");
	}

	public void run() {
		sendThread = Thread.currentThread();
		sendThread.setName(threadName);
		final String methodName = "run";
		MqttWireMessage message = null;
		
		synchronized (lifecycle) {
			current_state = State.RUNNING;
		}

		try {
			State my_target;
			synchronized (lifecycle) {
				my_target = target_state;
			}
			while (my_target == State.RUNNING && (out != null)) {
				try {
					message = clientState.get();
					if (message != null) {
						//@TRACE 802=network send key={0} msg={1}
						log.fine(CLASS_NAME,methodName,"802", new Object[] {message.getKey(),message});

						if (message instanceof MqttAck) {
							out.write(message);
							out.flush();
						} else {
							MqttToken token = message.getToken();
							if (token == null) {
								token = tokenStore.getToken(message);
							}
							// While quiescing the tokenstore can be cleared so need
							// to check for null for the case where clear occurs
							// while trying to send a message.
							if (token != null) {
								synchronized (token) {
									out.write(message);
									try {
										out.flush();
									} catch (IOException ex) {
										// The flush has been seen to fail on disconnect of a SSL socket
										// as disconnect is in progress this should not be treated as an error
										if (!(message instanceof MqttDisconnect)) {
											throw ex;
										}
									}
									clientState.notifySent(message);
								}
							}
						}
					} else { // null message
						//@TRACE 803=get message returned null, stopping}
						log.fine(CLASS_NAME,methodName,"803");
						synchronized (lifecycle) {
							target_state = State.STOPPED;
						}
					}
				} catch (MqttException me) {
					handleRunException(message, me);
				} catch (Exception ex) {
					handleRunException(message, ex);
				}
				synchronized (lifecycle) {
					my_target = target_state;
				}
			} // end while
		} finally {
			synchronized (lifecycle) {
				current_state = State.STOPPED;
				sendThread = null;
			}
		}

		//@TRACE 805=<
		log.fine(CLASS_NAME, methodName,"805");
	}

	private void handleRunException(MqttWireMessage message, Exception ex) {
		final String methodName = "handleRunException";
		//@TRACE 804=exception
		log.fine(CLASS_NAME,methodName,"804",null, ex);
		MqttException mex;
		if ( !(ex instanceof MqttException)) {
			mex = new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ex);
		} else {
			mex = (MqttException)ex;
		}
		synchronized (lifecycle) {
			target_state = State.STOPPED;
		}
		clientComms.shutdownConnection(null, mex);
	}

	public boolean isRunning() {
		boolean result;
		synchronized (lifecycle) {
			result = (current_state == State.RUNNING && target_state == State.RUNNING);
		}
		return result;
	}
}
