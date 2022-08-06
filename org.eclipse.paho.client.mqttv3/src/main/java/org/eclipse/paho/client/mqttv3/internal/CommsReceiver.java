/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp.
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
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttInputStream;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubComp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRec;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

import static org.eclipse.paho.client.mqttv3.internal.CommsSender.MAX_STOPPED_STATE_TO_STOP_THREAD;

/**
 * Receives MQTT packets from the server.
 */
public class CommsReceiver implements Runnable {
	private static final String CLASS_NAME = CommsReceiver.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private enum State {STOPPED, RUNNING, STARTING, RECEIVING}

	private State current_state = State.STOPPED;
	private State target_state = State.STOPPED;
	private final Object lifecycle = new Object();
	private String threadName;
	private Future<?> receiverFuture;

	private ClientState clientState = null;
	private ClientComms clientComms = null;
	private MqttInputStream in;
	private CommsTokenStore tokenStore = null;
	private Thread recThread	= null;

	public CommsReceiver(ClientComms clientComms, ClientState clientState,CommsTokenStore tokenStore, InputStream in) {
		this.in = new MqttInputStream(clientState, in);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
		log.setResourceName(clientComms.getClient().getClientId());
	}

	/**
	 * Starts up the Receiver's thread.
	 * @param threadName the thread name.
	 * @param executorService used to execute the thread, or null
	 */
	public void start(String threadName, ExecutorService executorService) {
		this.threadName = threadName;
		final String methodName = "start";
		//@TRACE 855=starting
		log.fine(CLASS_NAME,methodName, "855");
		synchronized (lifecycle) {
			if (current_state == State.STOPPED && target_state == State.STOPPED) {
				target_state = State.RUNNING;
				current_state = State.RUNNING;
				if (executorService == null) {
					receiverFuture = null;
					recThread = new Thread(this);
					recThread.start();
				} else {
					recThread = null;
					receiverFuture = executorService.submit(this);
				}
			}
		}

		AtomicInteger stoppedStateCounter = new AtomicInteger(0);
		while (!isRunning()) {
			try { Thread.sleep(100); } catch (Exception e) { }
			if (current_state == State.STOPPED) {
				if (stoppedStateCounter.incrementAndGet() > MAX_STOPPED_STATE_TO_STOP_THREAD) {
					break;
				}
			} else {
				stoppedStateCounter.set(0);
			}
		}
	}

	/**
	 * Stops the Receiver's thread.  This call will block.
	 */
	public void stop() {
		final String methodName = "stop";
		boolean isRunning;

		synchronized (lifecycle) {
			//@TRACE 850=stopping
			log.fine(CLASS_NAME,methodName, "850");
			isRunning = isRunning();
			if (isRunning) {
				target_state = State.STOPPED;
			}
		}
		// This and the clause above will prevent a thread from waiting for itself.
		if (isRunning) {
			if (receiverFuture != null) {
				try {
					receiverFuture.get();
				} catch (ExecutionException | InterruptedException e) {
				}
			} else {
				try {
					recThread.join();
				} catch (InterruptedException e) {
				}
			}
		}
		//@TRACE 851=stopped
		log.fine(CLASS_NAME,methodName,"851");
	}

	/**
	 * Run loop to receive messages from the server.
	 */
	public void run() {
		Thread.currentThread().setName(threadName);
		final String methodName = "run";
		MqttToken token = null;

		try {
			State my_target;
			synchronized (lifecycle) {
				my_target = target_state;
			}
			while (my_target == State.RUNNING && (in != null)) {
				try {
					//@TRACE 852=network read message
					log.fine(CLASS_NAME,methodName,"852");
					if (in.available() > 0) {
						synchronized (lifecycle) {
							current_state = State.RECEIVING;
						}
					}
					MqttWireMessage message = in.readMqttWireMessage();
					synchronized (lifecycle) {
						current_state = State.RUNNING;
					}

					// instanceof checks if message is null
					if (message instanceof MqttAck) {
						token = tokenStore.getToken(message);
						if (token!=null) {
							synchronized (token) {
								// Ensure the notify processing is done under a lock on the token
								// This ensures that the send processing can complete  before the
								// receive processing starts! ( request and ack and ack processing
								// can occur before request processing is complete if not!
								clientState.notifyReceivedAck((MqttAck)message);
							}
						} else if(message instanceof MqttPubRec || message instanceof MqttPubComp || message instanceof MqttPubAck) {
							//This is an ack for a message we no longer have a ticket for.
							//This probably means we already received this message and it's being send again
							//because of timeouts, crashes, disconnects, restarts etc.
							//It should be safe to ignore these unexpected messages.
							log.fine(CLASS_NAME, methodName, "857");
						} else {
							// It its an ack and there is no token then something is not right.
							// An ack should always have a token assoicated with it.
							throw new MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
						}
					} else {
						if (message != null) {
							// A new message has arrived
							clientState.notifyReceivedMsg(message);
						}  
                                                else {
                                                    // fix for bug 719
                                                    if (!clientComms.isConnected() && !clientComms.isConnecting()) {
                                                         throw new IOException("Connection is lost.");
                                                    }
                                                }
					}
				}
				catch (MqttException ex) {
					//@TRACE 856=Stopping, MQttException
					log.fine(CLASS_NAME,methodName,"856",null,ex);
					synchronized (lifecycle) {
						target_state = State.STOPPED;
					}
					// Token maybe null but that is handled in shutdown
					clientComms.shutdownConnection(token, ex);
				}
				catch (IOException ioe) {
					//@TRACE 853=Stopping due to IOException
					log.fine(CLASS_NAME,methodName,"853");
					if (target_state != State.STOPPED) {
						synchronized (lifecycle) {
							target_state = State.STOPPED;
						}
						// An EOFException could be raised if the broker processes the
						// DISCONNECT and ends the socket before we complete. As such,
						// only shutdown the connection if we're not already shutting down.
						if (!clientComms.isDisconnecting()) {
							clientComms.shutdownConnection(token, new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ioe));
						}
					}
				}
				finally {
					synchronized (lifecycle) {
						current_state = State.RUNNING;
					}
				}
				synchronized (lifecycle) {
					my_target = target_state;
				}
			} // end while
		} finally {
			synchronized (lifecycle) {
				current_state = State.STOPPED;
			}
		} // end try

		//@TRACE 854=<
		log.fine(CLASS_NAME,methodName,"854");
	}

	public boolean isRunning() {
		boolean result;
		synchronized (lifecycle) {
			result = ((current_state == State.RUNNING || current_state == State.RECEIVING) && 
					target_state == State.RUNNING);
		}
		return result;
	}

	/**
	 * Returns the receiving state.
	 *
	 * @return true if the receiver is receiving data, false otherwise.
	 */
	public boolean isReceiving() {
		boolean result;
		synchronized (lifecycle) {
			result = (current_state == State.RECEIVING);
		}
		return result;
	}
}
