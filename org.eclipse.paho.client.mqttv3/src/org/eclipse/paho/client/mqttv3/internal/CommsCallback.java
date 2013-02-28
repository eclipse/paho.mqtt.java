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

import java.util.Vector;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubComp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

/**
 * Bridge between Receiver and the external API. This class gets called by
 * Receiver, and then converts the comms-centric MQTT message objects into ones
 * understood by the external API.
 */
public class CommsCallback implements Runnable {
	private static int INBOUND_QUEUE_SIZE = 10;
	private MqttCallback mqttCallback;
	private ClientComms clientComms;
	private Vector messageQueue;
	private Vector completeQueue;
	public boolean running = false;
	private boolean quiescing = false;
	private Object lifecycle = new Object();
	private Thread callbackThread;
	private Object workAvailable = new Object();
	private Object spaceAvailable = new Object();
	private ClientState clientState;

	final static String className = CommsCallback.class.getName();
	Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,	className);

	CommsCallback(ClientComms clientComms) {
		this.clientComms = clientComms;
		this.messageQueue = new Vector(INBOUND_QUEUE_SIZE);
		this.completeQueue = new Vector(INBOUND_QUEUE_SIZE);
		log.setResourceName(clientComms.getClient().getClientId());
	}

	public void setClientState(ClientState clientState) {
		this.clientState = clientState;
	}

	/**
	 * Starts up the Callback thread.
	 */
	public void start(String threadName) {
		synchronized (lifecycle) {
			if (running == false) {
				// Praparatory work before starting the background thread.
				// For safety ensure any old events are cleared.
				messageQueue.clear();
				completeQueue.clear();

				running = true;
				quiescing = false;
				callbackThread = new Thread(this, threadName);
				callbackThread.start();
			}
		}
	}

	/**
	 * Stops the callback thread. 
	 * This call will block until stop has completed.
	 */
	public void stop() {
		final String methodName = "stop";
		synchronized (lifecycle) {
			if (running) {
				// @TRACE 700=stopping
				log.fine(className, methodName, "700");
				running = false;
				if (!Thread.currentThread().equals(callbackThread)) {
					try {
						synchronized (workAvailable) {
							// @TRACE 701=notify workAvailable and wait for run
							// to finish
							log.fine(className, methodName, "701");
							workAvailable.notifyAll();
						}
						// Wait for the thread to finish.
						callbackThread.join();
					} catch (InterruptedException ex) {
					}
				}
			}
			callbackThread = null;
			// @TRACE 703=stopped
			log.fine(className, methodName, "703");
		}
	}

	public void setCallback(MqttCallback mqttCallback) {
		this.mqttCallback = mqttCallback;
	}

	public void run() {
		final String methodName = "run";
		while (running) {
			try {
				// If no work is currently available, then wait until there is some...
				try {
					synchronized (workAvailable) {
						if (running & messageQueue.isEmpty()
								&& completeQueue.isEmpty()) {
							// @TRACE 704=wait for workAvailable
							log.fine(className, methodName, "704");
							workAvailable.wait();
						}
					}
				} catch (InterruptedException e) {
				}

				if (running) {
					// Check for deliveryComplete callbacks...
					if (!completeQueue.isEmpty()) {
						// First call the delivery arrived callback if needed
						MqttToken token = (MqttToken) completeQueue.elementAt(0);
						handleActionComplete(token);
						completeQueue.removeElementAt(0);
					}
					
					// Check for messageArrived callbacks...
					if (!messageQueue.isEmpty()) {
						// Note, there is a window on connect where a publish
						// could arrive before we've
						// finished the connect logic.
						MqttPublish message = (MqttPublish) messageQueue
								.elementAt(0);

						handleMessage(message);
						messageQueue.removeElementAt(0);
					}
				}

				if (quiescing) {
					clientState.checkQuiesceLock();
				}

				synchronized (spaceAvailable) {
					// Notify the spaceAvailable lock, to say that there's now
					// some space on the queue...

					// @TRACE 706=notify spaceAvailable
					log.fine(className, methodName, "706");
					spaceAvailable.notifyAll();
				}
			} catch (Throwable ex) {
				// Users code could throw an Error or Exception e.g. in the case
				// of class NoClassDefFoundError
				// @TRACE 714=callback threw exception
				log.fine(className, methodName, "714", null, ex);
				running = false;
				clientComms.shutdownConnection(null, new MqttException(ex));
			}
		}
	}

	private void handleActionComplete(MqttToken token)
			throws MqttException {
		final String methodName = "handleActionComplete";
		synchronized (token) {
			// @TRACE 705=callback and notify for key={0}
			log.fine(className, methodName, "705",	new Object[] { token.internalTok.getKey() });
			
			// Unblock any waiters and if pending complete now set completed
			token.internalTok.notifyComplete();
			
 			if (!token.internalTok.isNotified()) {
 				// If a callback is registered and delivery has finished 
 				// call delivery complete callback. 
				if ( mqttCallback != null 
					&& token instanceof MqttDeliveryToken 
					&& token.isComplete()) {
						mqttCallback.deliveryComplete((MqttDeliveryToken) token);
				}
				// Now call async action completion callbacks
				fireActionEvent(token);
			}
			
			// Set notified so we don't tell the user again about this action.
			if ( token instanceof MqttDeliveryToken && token.isComplete()) {
				token.internalTok.setNotified(true);
			}

			if (token.isComplete()) {
				// Finish by doing any post processing such as delete 
				// from persistent store but only do so if the action
				// is complete
				clientState.notifyComplete(token);
			}
		}
	}

	/**
	 * This method is called when the connection to the server is lost. If there
	 * is no cause then it was a clean disconnect. The connectionLost callback
	 * will be invoked if registered and run on the thread that requested
	 * shutdown e.g. receiver or sender thread. If the request was a user
	 * initiated disconnect then the disconnect token will be notified.
	 * 
	 * @param cause  the reason behind the loss of connection.
	 */
	public void connectionLost(MqttException cause) {
		final String methodName = "connectionLost";
		// If there was a problem and a client callback has been set inform
		// the connection lost listener of the problem.
		try {
			if (mqttCallback != null && cause != null) {
				// @TRACE 708=call connectionLost
				log.fine(className, methodName, "708", new Object[] { cause });
				mqttCallback.connectionLost(cause);
			}
		} catch (java.lang.Throwable t) {
			// Just log the fact that a throwable has caught connection lost 
			// is called during shutdown processing so no need to do anything else
			// @TRACE 720=exception from connectionLost {0}
			log.fine(className, methodName, "720", new Object[] { t });
		}
	}

	/**
	 * An action has completed - if a completion listener has been set on the
	 * token then invoke it with the outcome of the action.
	 * 
	 * @param token
	 */
	public void fireActionEvent(MqttToken token) {
		final String methodName = "fireActionEvent";

		if (token != null) {
			IMqttActionListener asyncCB = token.getActionCallback();
			if (asyncCB != null) {
				if (token.getException() == null) {
					// @TRACE 716=call onSuccess key={0}
					log.fine(className, methodName, "716",
							new Object[] { token.internalTok.getKey() });
					asyncCB.onSuccess(token);
				} else {
					// @TRACE 717=call onFailure key {0}
					log.fine(className, methodName, "716",
							new Object[] { token.internalTok.getKey() });
					asyncCB.onFailure(token, token.getException());
				}
			}
		}
	}

	/**
	 * This method is called when a message arrives on a topic. Messages are
	 * only added to the queue for inbound messages if the client is not
	 * quiescing.
	 * 
	 * @param sendMessage
	 *            the MQTT SEND message.
	 */
	public void messageArrived(MqttPublish sendMessage) {
		final String methodName = "messageArrived";
		if (mqttCallback != null) {
			// If we already have enough messages queued up in memory, wait
			// until some more queue space becomes available. This helps 
			// the client protect itself from getting flooded by messages 
			// from the server.
			synchronized (spaceAvailable) {
				if (!quiescing && messageQueue.size() >= INBOUND_QUEUE_SIZE) {
					try {
						// @TRACE 709=wait for spaceAvailable
						log.fine(className, methodName, "709");
						spaceAvailable.wait();
					} catch (InterruptedException ex) {
					}
				}
			}
			if (!quiescing) {
				messageQueue.addElement(sendMessage);
				// Notify the CommsCallback thread that there's work to do...
				synchronized (workAvailable) {
					// @TRACE 710=new msg avail, notify workAvailable
					log.fine(className, methodName, "710");
					workAvailable.notifyAll();
				}
			}
		}
	}

	/**
	 * Let the call back thread quiesce. Prevent new inbound messages being
	 * added to the process queue and let existing work quiesce. (until the
	 * thread is told to shutdown).
	 */
	public void quiesce() {
		final String methodName = "quiesce";
		this.quiescing = true;
		synchronized (spaceAvailable) {
			// @TRACE 711=quiesce notify spaceAvailable
			log.fine(className, methodName, "711");
			// Unblock anything waiting for space...
			spaceAvailable.notifyAll();
		}
	}

	public boolean isQuiesced() {
		if (quiescing && completeQueue.size() == 0 && messageQueue.size() == 0) {
			return true;
		}
		return false;
	}

	private void handleMessage(MqttPublish publishMessage)
			throws MqttException, Exception {
		final String methodName = "handleMessage";
		// If quisecing process any pending messages. 
		if (mqttCallback != null) {
			String destName = publishMessage.getTopicName();

			// @TRACE 713=call messageArrived key={0} topic={1}
			log.fine(className, methodName, "713", new Object[] { 
					new Integer(publishMessage.getMessageId()), destName });
			mqttCallback.messageArrived(destName, publishMessage.getMessage());
			if (publishMessage.getMessage().getQos() == 1) {
				this.clientComms.internalSend(new MqttPubAck(publishMessage),
						new MqttToken(clientComms.getClient().getClientId()));
			} else if (publishMessage.getMessage().getQos() == 2) {
				this.clientComms.deliveryComplete(publishMessage);
				MqttPubComp pubComp = new MqttPubComp(publishMessage);
				this.clientComms.internalSend(pubComp, new MqttToken(clientComms.getClient().getClientId()));
			}
		}
	}

	public void asyncOperationComplete(MqttToken token) {
		final String methodName = "asyncOperationComplete";

		if (running) {
			// invoke callbacks on callback thread
			completeQueue.addElement(token);
			synchronized (workAvailable) {
				// @TRACE 715=new workAvailable. key={0}
				log.fine(className, methodName, "715", new Object[] { token.internalTok.getKey() });
				workAvailable.notifyAll();
			}
		} else {
			// invoke async callback on invokers thread
			try {
				handleActionComplete(token);
			} catch (Throwable ex) {
				// Users code could throw an Error or Exception e.g. in the case
				// of class NoClassDefFoundError
				// @TRACE 719=callback threw ex:
				log.fine(className, methodName, "719", null, ex);
				
				// Shutdown likely already in progress but no harm to confirm
				System.err.println("problem in asyncopcomplete "+ex);
				ex.printStackTrace();
				clientComms.shutdownConnection(null, new MqttException(ex));
			}

		}
	}

	/**
	 * Returns the thread used by this callback.
	 */
	protected Thread getThread() {
		return callbackThread;
	}
}
