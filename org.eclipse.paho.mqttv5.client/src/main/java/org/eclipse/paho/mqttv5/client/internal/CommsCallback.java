/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 *    Ian Craggs - per subscription message handlers (bug 466579)
 *    Ian Craggs - ack control (bug 472172)
 *    James Sutton - Automatic Reconnect & Offline Buffering    
 */
package org.eclipse.paho.mqttv5.client.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.MqttTopic;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttAuth;
import org.eclipse.paho.mqttv5.common.packet.MqttDisconnect;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttPubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttPubComp;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

/**
 * Bridge between Receiver and the external API. This class gets called by
 * Receiver, and then converts the comms-centric MQTT message objects into ones
 * understood by the external API.
 */
public class CommsCallback implements Runnable {
	private static final String CLASS_NAME = CommsCallback.class.getName();
	private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private static final int INBOUND_QUEUE_SIZE = 10;
	private MqttCallback mqttCallback;
	private MqttCallback reconnectInternalCallback;
	private HashMap<Integer, IMqttMessageListener> callbackMap; // Map of message handler callbacks to internal IDs
	private HashMap<String, Integer> callbackTopicMap; // Map of Topic Strings to internal callback Ids
	private HashMap<Integer, Integer> subscriptionIdMap; // Map of Subscription Ids to callback Ids
	private AtomicInteger messageHandlerId = new AtomicInteger(0);
	private ClientComms clientComms;
	private ArrayList<MqttPublish> messageQueue;
	private ArrayList<MqttToken> completeQueue;
	private boolean running = false;
	private boolean quiescing = false;
	private Object lifecycle = new Object();
	private Thread callbackThread;
	private Object workAvailable = new Object();
	private Object spaceAvailable = new Object();
	private ClientState clientState;
	private boolean manualAcks = false;
	private String threadName;
	private final Semaphore runningSemaphore = new Semaphore(1);
	private Future<?> callbackFuture;

	CommsCallback(ClientComms clientComms) {
		this.clientComms = clientComms;
		this.messageQueue = new ArrayList<>(INBOUND_QUEUE_SIZE);
		this.completeQueue = new ArrayList<>(INBOUND_QUEUE_SIZE);
		this.callbackMap = new HashMap<>();
		this.callbackTopicMap = new HashMap<>();
		this.subscriptionIdMap = new HashMap<>();
		log.setResourceName(clientComms.getClient().getClientId());
	}

	public void setClientState(ClientState clientState) {
		this.clientState = clientState;
	}

	/**
	 * Starts up the Callback thread.
	 * 
	 * @param threadName
	 *            The name of the thread
	 * @param executorService
	 *            the {@link ExecutorService}
	 */
	public void start(String threadName, ExecutorService executorService) {
		this.threadName = threadName;
		synchronized (lifecycle) {
			if (!running) {
				// Preparatory work before starting the background thread.
				// For safety ensure any old events are cleared.
				messageQueue.clear();
				completeQueue.clear();

				running = true;
				quiescing = false;
				callbackFuture = executorService.submit(this);
			}
		}
	}

	/**
	 * Stops the callback thread. This call will block until stop has completed.
	 */
	public void stop() {
		final String methodName = "stop";
		synchronized (lifecycle) {
			if (callbackFuture != null) {
				callbackFuture.cancel(true);
			}
			if (running) {
				// @TRACE 700=stopping
				log.fine(CLASS_NAME, methodName, "700");
				running = false;
				if (!Thread.currentThread().equals(callbackThread)) {
					try {
						synchronized (workAvailable) {
							// @TRACE 701=notify workAvailable and wait for run
							// to finish
							log.fine(CLASS_NAME, methodName, "701");
							workAvailable.notifyAll();
						}
						// Wait for the thread to finish.
						runningSemaphore.acquire();
					} catch (InterruptedException ex) {
					} finally {
						runningSemaphore.release();
					}
				}
			}
			callbackThread = null;
			// @TRACE 703=stopped
			log.fine(CLASS_NAME, methodName, "703");
		}
	}

	public void setCallback(MqttCallback mqttCallback) {
		this.mqttCallback = mqttCallback;
	}

	public void setReconnectCallback(MqttCallback callback) {
		this.reconnectInternalCallback = callback;
	}

	public void setManualAcks(boolean manualAcks) {
		this.manualAcks = manualAcks;
	}

	public void run() {
		final String methodName = "run";
		callbackThread = Thread.currentThread();
		callbackThread.setName(threadName);

		try {
			runningSemaphore.acquire();
		} catch (InterruptedException e) {
			running = false;
			return;
		}

		while (running) {
			try {
				// If no work is currently available, then wait until there is some...
				try {
					synchronized (workAvailable) {
						if (running && messageQueue.isEmpty() && completeQueue.isEmpty()) {
							// @TRACE 704=wait for workAvailable
							log.fine(CLASS_NAME, methodName, "704");
							workAvailable.wait();
						}
					}
				} catch (InterruptedException e) {
				}

				if (running) {
					// Check for deliveryComplete callbacks...
					MqttToken token = null;
					synchronized (completeQueue) {
						if (!completeQueue.isEmpty()) {
							// First call the delivery arrived callback if needed
							token = completeQueue.get(0);
							completeQueue.remove(0);
						}
					}
					if (null != token) {
						handleActionComplete(token);
					}

					// Check for messageArrived callbacks...
					MqttPublish message = null;
					synchronized (messageQueue) {
						if (!messageQueue.isEmpty()) {
							// Note, there is a window on connect where a publish
							// could arrive before we've
							// finished the connect logic.
							message = messageQueue.get(0);
							messageQueue.remove(0);
						}
					}
					if (null != message) {
						handleMessage(message);
					}
				}

				if (quiescing) {
					clientState.checkQuiesceLock();
				}

			} catch (Throwable ex) {
				// Users code could throw an Error or Exception e.g. in the case
				// of class NoClassDefFoundError
				// @TRACE 714=callback threw exception
				log.fine(CLASS_NAME, methodName, "714", null, ex);
				running = false;
				clientComms.shutdownConnection(null, new MqttException(ex), null);
			} finally {
				runningSemaphore.release();
				synchronized (spaceAvailable) {
					// Notify the spaceAvailable lock, to say that there's now
					// some space on the queue...

					// @TRACE 706=notify spaceAvailable
					log.fine(CLASS_NAME, methodName, "706");
					spaceAvailable.notifyAll();
				}
			}
		}
	}

	private void handleActionComplete(MqttToken token) throws MqttException {
		final String methodName = "handleActionComplete";
		synchronized (token) {
			// @TRACE 705=callback and notify for key={0}
			log.fine(CLASS_NAME, methodName, "705", new Object[] { token.internalTok.getKey() });
			if (token.isComplete()) {
				// Finish by doing any post processing such as delete
				// from persistent store but only do so if the action
				// is complete
				clientState.notifyComplete(token);
			}

			// Unblock any waiters and if pending complete now set completed
			token.internalTok.notifyComplete();

			if (!token.internalTok.isNotified()) {
				// If a callback is registered and delivery has finished
				// call delivery complete callback.
				if (mqttCallback != null && token instanceof MqttDeliveryToken && token.isComplete()) {
					mqttCallback.deliveryComplete((MqttDeliveryToken) token);
				}
				// Now call async action completion callbacks
				fireActionEvent(token);
			}

			// Set notified so we don't tell the user again about this action.
			if (token.isComplete()) {
				if (token instanceof MqttDeliveryToken || token.getActionCallback() instanceof MqttActionListener) {
					token.internalTok.setNotified(true);
				}
			}

		}
	}

	/**
	 * This method is called when the connection to the server is lost. If there is
	 * no cause then it was a clean disconnect. The connectionLost callback will be
	 * invoked if registered and run on the thread that requested shutdown e.g.
	 * receiver or sender thread. If the request was a user initiated disconnect
	 * then the disconnect token will be notified.
	 * 
	 * @param cause
	 *            the reason behind the loss of connection.
	 * @param message
	 *            The {@link MqttDisconnect} packet sent by the server
	 */
	public void connectionLost(MqttException cause, MqttDisconnect message) {
		final String methodName = "connectionLost";
		// If there was a problem and a client callback has been set inform
		// the connection lost listener of the problem.
		try {
			if (mqttCallback != null && message != null) {

				// @TRACE 722=Server initiated disconnect, connection closed. Disconnect={0}
				log.fine(CLASS_NAME, methodName, "722", new Object[] { message.toString() });
				MqttDisconnectResponse disconnectResponse = new MqttDisconnectResponse(message.getReturnCode(),
						message.getProperties().getReasonString(), (ArrayList<UserProperty>) message.getProperties().getUserProperties(),
						message.getProperties().getServerReference());
				mqttCallback.disconnected(disconnectResponse);
			} else if (mqttCallback != null && cause != null) {
				// @TRACE 708=call connectionLost
				log.fine(CLASS_NAME, methodName, "708", new Object[] { cause });
				MqttDisconnectResponse disconnectResponse = new MqttDisconnectResponse(cause);
				mqttCallback.disconnected(disconnectResponse);
			}
			if (reconnectInternalCallback != null && cause != null) {
				MqttDisconnectResponse disconnectResponse = new MqttDisconnectResponse(cause);

				reconnectInternalCallback.disconnected(disconnectResponse);
			}
		} catch (Throwable t) {
			// Just log the fact that a throwable has caught connection lost
			// is called during shutdown processing so no need to do anything else
			// @TRACE 720=exception from connectionLost {0}
			log.fine(CLASS_NAME, methodName, "720", new Object[] { t });
		}
	}

	/**
	 * An action has completed - if a completion listener has been set on the token
	 * then invoke it with the outcome of the action.
	 * 
	 * @param token
	 *            The {@link MqttToken} that has completed
	 */
	public void fireActionEvent(MqttToken token) {
		final String methodName = "fireActionEvent";

		if (token != null) {
			MqttActionListener asyncCB = token.getActionCallback();
			if (asyncCB != null) {
				if (token.getException() == null) {
					// @TRACE 716=call onSuccess key={0}
					log.fine(CLASS_NAME, methodName, "716", new Object[] { token.internalTok.getKey() });
					asyncCB.onSuccess(token);
				} else {
					// @TRACE 717=call onFailure key {0}
					log.fine(CLASS_NAME, methodName, "716", new Object[] { token.internalTok.getKey() });
					asyncCB.onFailure(token, token.getException());
				}
			}
		}
	}

	/**
	 * This method is called when a message arrives on a topic. Messages are only
	 * added to the queue for inbound messages if the client is not quiescing.
	 * 
	 * @param sendMessage
	 *            the MQTT SEND message.
	 */
	public void messageArrived(MqttPublish sendMessage) {
		final String methodName = "messageArrived";
		if (mqttCallback != null || callbackMap.size() > 0) {
			// If we already have enough messages queued up in memory, wait
			// until some more queue space becomes available. This helps
			// the client protect itself from getting flooded by messages
			// from the server.
			synchronized (spaceAvailable) {
				while (running && !quiescing && messageQueue.size() >= INBOUND_QUEUE_SIZE) {
					try {
						// @TRACE 709=wait for spaceAvailable
						log.fine(CLASS_NAME, methodName, "709");
						spaceAvailable.wait(200);
					} catch (InterruptedException ex) {
					}
				}
			}
			if (!quiescing) {
				messageQueue.add(sendMessage);
				// Notify the CommsCallback thread that there's work to do...
				synchronized (workAvailable) {
					// @TRACE 710=new msg avail, notify workAvailable
					log.fine(CLASS_NAME, methodName, "710");
					workAvailable.notifyAll();
				}
			}
		}
	}
	
	/**
	 * This method is called when an Auth Message is received.
	 * @param authMessage The {@link MqttAuth} message.
	 */
	public void authMessageReceived(MqttAuth authMessage) {
		if(mqttCallback != null) {
			mqttCallback.authPacketArrived(authMessage.getReturnCode(), authMessage.getProperties());
		}
	}

	/**
	 * This method is called when a non-critical MQTT error has occurred in the
	 * client that the application should choose how to deal with.
	 * 
	 * @param exception
	 *            The exception that was thrown containing the cause for
	 *            disconnection.
	 */
	public void mqttErrorOccured(MqttException exception) {
		final String methodName = "mqttErrorOccured";
		log.warning(CLASS_NAME, methodName, "721", new Object[] { exception.getMessage() });
		if (mqttCallback != null) {
			mqttCallback.mqttErrorOccured(exception);
		}
	}

	/**
	 * Let the call back thread quiesce. Prevent new inbound messages being added to
	 * the process queue and let existing work quiesce. (until the thread is told to
	 * shutdown).
	 */
	public void quiesce() {
		final String methodName = "quiesce";
		this.quiescing = true;
		synchronized (spaceAvailable) {
			// @TRACE 711=quiesce notify spaceAvailable
			log.fine(CLASS_NAME, methodName, "711");
			// Unblock anything waiting for space...
			spaceAvailable.notifyAll();
		}
	}

	public boolean isQuiesced() {
		return (quiescing && completeQueue.isEmpty() && messageQueue.isEmpty());
	}

	private void handleMessage(MqttPublish publishMessage) throws Exception {
		final String methodName = "handleMessage";
		// If quisecing process any pending messages.

		String destName = publishMessage.getTopicName();

		// @TRACE 713=call messageArrived key={0} topic={1}
		log.fine(CLASS_NAME, methodName, "713", new Object[] { new Integer(publishMessage.getMessageId()), destName });
		deliverMessage(destName, publishMessage.getMessageId(), publishMessage.getMessage());

		if (!this.manualAcks) {
			if (publishMessage.getMessage().getQos() == 1) {
				this.clientComms.internalSend(
						new MqttPubAck(MqttReturnCode.RETURN_CODE_SUCCESS, publishMessage.getMessageId(), new MqttProperties()),
						new MqttToken(clientComms.getClient().getClientId()));
			} else if (publishMessage.getMessage().getQos() == 2) {
				this.clientComms.deliveryComplete(publishMessage);
				MqttPubComp pubComp = new MqttPubComp(MqttReturnCode.RETURN_CODE_SUCCESS,
						publishMessage.getMessageId(), new MqttProperties());
				this.clientComms.internalSend(pubComp, new MqttToken(clientComms.getClient().getClientId()));
			}
		}
	}

	public void messageArrivedComplete(int messageId, int qos) throws MqttException {
		if (qos == 1) {
			this.clientComms.internalSend(new MqttPubAck(MqttReturnCode.RETURN_CODE_SUCCESS, messageId, new MqttProperties()),
					new MqttToken(clientComms.getClient().getClientId()));
		} else if (qos == 2) {
			this.clientComms.deliveryComplete(messageId);
			MqttPubComp pubComp = new MqttPubComp(MqttReturnCode.RETURN_CODE_SUCCESS, messageId, new MqttProperties());
			this.clientComms.internalSend(pubComp, new MqttToken(clientComms.getClient().getClientId()));
		}
	}

	public void asyncOperationComplete(MqttToken token) {
		final String methodName = "asyncOperationComplete";

		if (running) {
			// invoke callbacks on callback thread
			completeQueue.add(token);
			synchronized (workAvailable) {
				// @TRACE 715=new workAvailable. key={0}
				log.fine(CLASS_NAME, methodName, "715", new Object[] { token.internalTok.getKey() });
				workAvailable.notifyAll();
			}
		} else {
			// invoke async callback on invokers thread
			try {
				handleActionComplete(token);
			} catch (MqttException ex) {
				// Users code could throw an Error or Exception e.g. in the case
				// of class NoClassDefFoundError
				// @TRACE 719=callback threw ex:
				log.fine(CLASS_NAME, methodName, "719", null, ex);

				// Shutdown likely already in progress but no harm to confirm
				clientComms.shutdownConnection(null, new MqttException(ex), null);
			}

		}
	}

	/**
	 * Returns the thread used by this callback.
	 * 
	 * @return The {@link Thread}
	 */
	protected Thread getThread() {
		return callbackThread;
	}

	public void setMessageListener(Integer subscriptionId, String topicFilter, IMqttMessageListener messageListener) {
		int internalId = messageHandlerId.incrementAndGet();
		this.callbackMap.put(internalId, messageListener);
		this.callbackTopicMap.put(topicFilter, internalId);

		if (subscriptionId != null) {
			this.subscriptionIdMap.put(subscriptionId, internalId);
		}
	}

	/**
	 * Removes a Message Listener by Topic. If the Topic is null or incorrect, this
	 * function will return without making any changes. It will also attempt to find
	 * any subscription IDs linked to the same message listener and will remove them
	 * too.
	 * 
	 * @param topicFilter
	 *            the topic filter that identifies the Message listener to remove.
	 */
	public void removeMessageListener(String topicFilter) {
		Integer callbackId = this.callbackTopicMap.get(topicFilter);
		this.callbackMap.remove(callbackId);
		this.callbackTopicMap.remove(topicFilter);

		// Reverse lookup the subscription ID if it exists to remove that as well
		for (Map.Entry<Integer, Integer> entry : this.subscriptionIdMap.entrySet()) {
			if (entry.getValue().equals(callbackId)) {
				this.subscriptionIdMap.remove(entry.getKey());
			}
		}
	}

	/**
	 * Removes a Message Listener by subscription ID. If the Subscription Identifier
	 * is null or incorrect, this function will return without making any changes.
	 * It will also attempt to find any Topic Strings linked to the same message
	 * listener and will remove them too.
	 * 
	 * @param subscriptionId
	 *            the subscription ID that identifies the Message listener to
	 *            remove.
	 */
	public void removeMessageListener(Integer subscriptionId) {
		Integer callbackId = this.subscriptionIdMap.get(subscriptionId);
		this.subscriptionIdMap.remove(callbackId);
		this.callbackMap.remove(callbackId);

		// Reverse lookup the topic if it exists to remove that as well
		for (Map.Entry<String, Integer> entry : this.callbackTopicMap.entrySet()) {
			if (entry.getValue().equals(callbackId)) {
				this.callbackTopicMap.remove(entry.getKey());
			}
		}
	}

	public void removeMessageListeners() {
		this.callbackMap.clear();
		this.subscriptionIdMap.clear();
		this.callbackTopicMap.clear();
	}

	protected boolean deliverMessage(String topicName, int messageId, MqttMessage aMessage) throws Exception {
		boolean delivered = false;

		if (aMessage.getProperties().getSubscriptionIdentifiers().isEmpty()) {
			// No Subscription IDs, use topic filter matching
			for (Map.Entry<String, Integer> entry : this.callbackTopicMap.entrySet()) {
				if (MqttTopic.isMatched(entry.getKey(), topicName)) {
					aMessage.setId(messageId);
					this.callbackMap.get(entry.getValue()).messageArrived(topicName, aMessage);
					delivered = true;
				}
			}

		} else {
			// We have Subscription IDs
			for (Integer subId : aMessage.getProperties().getSubscriptionIdentifiers()) {
				if (this.subscriptionIdMap.containsKey(subId)) {
					Integer callbackId = this.subscriptionIdMap.get(subId);
					aMessage.setId(messageId);
					this.callbackMap.get(callbackId).messageArrived(topicName, aMessage);
					delivered = true;
				}
			}
		}

		/*
		 * if the message hasn't been delivered to a per subscription handler, give it
		 * to the default handler
		 */
		if (mqttCallback != null && !delivered) {
			aMessage.setId(messageId);
			mqttCallback.messageArrived(topicName, aMessage);
			delivered = true;
		}

		return delivered;
	}

	public boolean doesSubscriptionIdentifierExist(int subscriptionIdentifier) {
		return (this.subscriptionIdMap.containsKey(subscriptionIdentifier));
	}

}
