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

import java.io.EOFException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnack;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingReq;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingResp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubComp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRec;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRel;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttUnsubscribe;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;


/**
 * The core of the client, which holds the state information for pending and
 * in-flight messages.
 */
public class ClientState {
	private static final String PERSISTENCE_SENT_PREFIX = "s-";
	private static final String PERSISTENCE_CONFIRMED_PREFIX = "sc-";
	private static final String PERSISTENCE_RECEIVED_PREFIX = "r-";
	/** Lowest possible MQTT message ID to use */
	private static final int MIN_MSG_ID = 1;
	/** Highest possible MQTT message ID to use */
	private static final int MAX_MSG_ID = 65535;
	
	/** The next available message ID */
	private int nextMsgId = MIN_MSG_ID - 1;
	
	/** Used to store a set of in-use message IDs */
	private Hashtable inUseMsgIds;

	private Vector pendingMessages;
	private Vector pendingFlows;
	
//	private Vector restoredPendingMessages;
//	private Vector restoredPendingFlows;
	
	private CommsTokenStore tokenStore;
	
	private long keepAlive;

	private boolean cleanSession;
	
	private int maxInflight = 10;
	
	private MqttClientPersistence persistence;
	
	private int actualInFlight = 0;
	private int inFlightPubRels = 0;
	
	private Object queueLock = new Object();
	private Object quiesceLock = new Object();
	private boolean quiescing = false;
	
	private long lastOutboundActivity = 0;
	private long lastInboundActivity = 0;
	
	private boolean connected = false;
	private boolean sentConnect = false;
	private boolean connectFailed = false;
	
	private CommsCallback callback = null;
	
	private Hashtable outboundQoS2 = null;
	private Hashtable outboundQoS1 = null;
	private Hashtable inboundQoS2 = null;
	
	private MqttWireMessage pingCommand;
	
	private boolean pingOutstanding = false;

	private Trace trace;
	
	/** A count of the threads waiting in token.waitUntilSent.
	 * @see #disconnected(MqttException, boolean)
	 * @see #incrementWaitingTokens()
	 * @see #decrementWaitingTokens() 
	 */ 
	private int waitingTokens = 0;
	private Object waitingTokensLock = new Object();

	protected ClientState(Trace trace, MqttClientPersistence persistence, CommsTokenStore tokenStore, CommsCallback callback) throws MqttException {
		this.trace = trace;
		inUseMsgIds = new Hashtable();
		pendingMessages = new Vector(this.maxInflight);
		pendingFlows = new Vector();
		outboundQoS2 = new Hashtable();
		outboundQoS1 = new Hashtable();
		inboundQoS2 = new Hashtable();
		pingCommand = new MqttPingReq();
		inFlightPubRels = 0;
		actualInFlight = 0;
		
		this.persistence = persistence;
		this.callback = callback;
		this.tokenStore = tokenStore;
		restoreState();
	}

	protected void setKeepAliveSecs(long keepAliveSecs) {
		this.keepAlive = keepAliveSecs*1000;
	}
	protected void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}
	
	private String getSendPersistenceKey(MqttWireMessage message) {
		return PERSISTENCE_SENT_PREFIX + message.getMessageId();
	}
	
	private String getSendConfirmPersistenceKey(MqttWireMessage message) {
		return PERSISTENCE_CONFIRMED_PREFIX + message.getMessageId();
	}
	
	private String getReceivedPersistenceKey(MqttWireMessage message) {
		return PERSISTENCE_RECEIVED_PREFIX + message.getMessageId();
	}
	
	protected void clearState() throws MqttException {
		//@TRACE 603=clearState
		trace.trace(Trace.FINE,603);

		persistence.clear();
		inUseMsgIds.clear();
		pendingMessages.clear();
		pendingFlows.clear();
		outboundQoS2.clear();
		outboundQoS1.clear();
		inboundQoS2.clear();
		tokenStore.clear();
	}
	
	private MqttWireMessage restoreMessage(String key, MqttPersistable persistable) throws MqttException {
		MqttWireMessage message = null;

		try {
			message = MqttWireMessage.createWireMessage(persistable);
		}
		catch (MqttException ex) {
			//@TRACE 602=restoreMessage key={0} exception
			trace.trace(Trace.FINE,602,new Object[]{key},ex);
			if (ex.getCause() instanceof EOFException) {
				// Premature end-of-file means that the message is corrupted
				if (key != null) {
					persistence.remove(key);
				}
			}
			else {
				throw ex;
			}
		}
		//@TRACE 601=restoreMessage key={0} message={1}
		trace.trace(Trace.FINE,601, new Object[]{key,message});
		return message;
	}

	/**
	 * Inserts a new message to the list, ensuring that list is ordered from lowest to highest in terms of the message id's.
	 * @param list the list to insert the message into
	 * @param newMsg the message to insert into the list
	 */
	private void insertInOrder(Vector list, MqttWireMessage newMsg) {
		int newMsgId = newMsg.getMessageId();
		for (int i = 0; i < list.size(); i++) {
			MqttWireMessage otherMsg = (MqttWireMessage) list.elementAt(i);
			int otherMsgId = otherMsg.getMessageId();
			if (otherMsgId > newMsgId) {
				list.insertElementAt(newMsg, i);
				return;
			}
		}
		list.addElement(newMsg);
	}

	/**
	 * Produces a new list with the messages properly ordered according to their message id's.
	 * @param list the list containing the messages to produce a new reordered list for - this will not be modified or replaced, i.e., be read-only to this method
	 * @return a new reordered list
	 */
	private Vector reOrder(Vector list) {

		// here up the new list
		Vector newList = new Vector();

		if (list.size() == 0) {
			return newList; // nothing to reorder
		}
		
		int previousMsgId = 0;
		int largestGap = 0;
		int largestGapMsgIdPosInList = 0;
		for (int i = 0; i < list.size(); i++) {
			int currentMsgId = ((MqttWireMessage) list.elementAt(i)).getMessageId();
			if (currentMsgId - previousMsgId > largestGap) {
				largestGap = currentMsgId - previousMsgId;
				largestGapMsgIdPosInList = i;
			}
			previousMsgId = currentMsgId;
		}
		int lowestMsgId = ((MqttWireMessage) list.elementAt(0)).getMessageId();
		int highestMsgId = previousMsgId; // last in the sorted list
		
		// we need to check that the gap after highest msg id to the lowest msg id is not beaten
		if (MAX_MSG_ID - highestMsgId + lowestMsgId > largestGap) {
			largestGapMsgIdPosInList = 0;
		}
		
		// starting message has been located, let's start from this point on

		for (int i = largestGapMsgIdPosInList; i < list.size(); i++) {
			newList.addElement(list.elementAt(i));
		}
	
		// and any wrapping back to the beginning
		for (int i = 0; i < largestGapMsgIdPosInList; i++) {
			newList.addElement(list.elementAt(i));
		}
	
		return newList;
		
	}
	
	/**
	 * Restores the state information from persistence.
	 */
	protected void restoreState() throws MqttException {
		Enumeration messageKeys = persistence.keys();
		MqttPersistable persistable;
		String key;
		int highestMsgId = nextMsgId;
		Vector orphanedPubRels = new Vector();
		//@TRACE 600=restoreState
		trace.trace(Trace.FINE,600);
		while (messageKeys.hasMoreElements()) {
			key = (String) messageKeys.nextElement();
			persistable = persistence.get(key);
			MqttWireMessage message = restoreMessage(key, persistable);
			if (message != null) {
				if (key.startsWith(PERSISTENCE_RECEIVED_PREFIX)) {
					//@TRACE 604=restoreState: inbound QoS 2 publish key={0} message={1}
					trace.trace(Trace.FINE,604, new Object[]{key,message});

					// The inbound messages that we have persisted will be QoS 2 
					inboundQoS2.put(new Integer(message.getMessageId()),message);
				}
				else if (key.startsWith(PERSISTENCE_SENT_PREFIX)) {
					MqttPublish sendMessage = (MqttPublish) message;
					highestMsgId = Math.max(sendMessage.getMessageId(), highestMsgId);
					if (persistence.containsKey(getSendConfirmPersistenceKey(sendMessage))) {
						MqttPersistable persistedConfirm = persistence.get(getSendConfirmPersistenceKey(sendMessage));
						// QoS 2, and CONFIRM has already been sent...
						MqttPubRel confirmMessage = (MqttPubRel) restoreMessage(key, persistedConfirm);
						if (confirmMessage != null) {
							//@TRACE 605=restoreState: outbound QoS 2 pubrel key={0} message={1}
							trace.trace(Trace.FINE,605, new Object[]{key,message});
							outboundQoS2.put(new Integer(confirmMessage.getMessageId()), confirmMessage);
						} else {
							//@TRACE 606=restoreState: outbound QoS 2 completed key={0} message={1}
							trace.trace(Trace.FINE,606, new Object[]{key,message});
						}
					}
					else {
						// QoS 1 or 2, with no CONFIRM sent...
						// Put the SEND to the list of pending messages, ensuring message ID ordering...
						if (((MqttPublish)sendMessage).getMessage().getQos() == 2) {
							//@TRACE 607=restoreState: outbound QoS 2 publish key={0} message={1}
							trace.trace(Trace.FINE,607, new Object[]{key,message});
							outboundQoS2.put(new Integer(sendMessage.getMessageId()),sendMessage);
						} else {
							//@TRACE 608=restoreState: outbound QoS 1 publish key={0} message={1}
							trace.trace(Trace.FINE,608, new Object[]{key,message});
							outboundQoS1.put(new Integer(sendMessage.getMessageId()),sendMessage);
						}
					}
					tokenStore.restoreToken(sendMessage);
					inUseMsgIds.put(new Integer(sendMessage.getMessageId()),new Integer(sendMessage.getMessageId()));
				}
				else if (key.startsWith(PERSISTENCE_CONFIRMED_PREFIX)) {
					MqttPubRel pubRelMessage = (MqttPubRel) message;
					if (!persistence.containsKey(getSendPersistenceKey(pubRelMessage))) {
						orphanedPubRels.addElement(key);
					}
				}
			}
		}

		messageKeys = orphanedPubRels.elements();
		while(messageKeys.hasMoreElements()) {
			key = (String) messageKeys.nextElement();
			//@TRACE 609=restoreState: removing orphaned pubrel key={0}
			trace.trace(Trace.FINE,609, new Object[]{key});
			persistence.remove(key);
		}
		
		nextMsgId = highestMsgId;
	}
	
	private void restoreInflightMessages() {
		pendingMessages = new Vector(this.maxInflight);
		pendingFlows = new Vector();

		Enumeration keys = outboundQoS2.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object msg = outboundQoS2.get(key);
			if (msg instanceof MqttPublish) {
				//@TRACE 610=restoreInflightMessages: QoS 2 publish key={0}
				trace.trace(Trace.FINE,610, new Object[]{key});
				insertInOrder(pendingMessages, (MqttPublish)msg);
			} else if (msg instanceof MqttPubRel) {
				//@TRACE 611=restoreInflightMessages: QoS 2 pubrel key={0}
				trace.trace(Trace.FINE,611, new Object[]{key});
				insertInOrder(pendingFlows, (MqttPubRel)msg);
			}
		}
		keys = outboundQoS1.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			MqttPublish msg = (MqttPublish)outboundQoS1.get(key);
			trace.trace(Trace.FINE,612, new Object[]{key});
			insertInOrder(pendingMessages, (MqttPublish)msg);
		}
		
		this.pendingFlows = reOrder(pendingFlows);
		this.pendingMessages = reOrder(pendingMessages);
	}
	
	/**
	 * Submits a message for delivery. This method will block until there is
	 * room in the inFlightWindow for the message. The message is put into
	 * persistence before returning.
	 * 
	 * @param message
	 *            the message to send
	 * @return the delivery token that can be used to track delivery of the
	 *         message
	 * @throws MqttException
	 */
	public MqttDeliveryTokenImpl send(MqttWireMessage message) throws MqttException {
		MqttDeliveryTokenImpl token = null;
		if (message instanceof MqttConnect) {
			sentConnect = false;
			connectFailed = false;
		}
		if (message.isMessageIdRequired() && (message.getMessageId() == 0)) {
			message.setMessageId(getNextMessageId());
		}
		if (message instanceof MqttPublish) {
			synchronized (queueLock) {
				if (quiescing) {
					if (trace.isOn()) {
						//@TRACE 613=send: cannot send whilst quiescing. message={0}
						trace.trace(Trace.FINE,613, new Object[]{message});
					}
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
				}
				MqttMessage innerMessage = ((MqttPublish) message).getMessage();
				if (trace.isOn()) {
					//@TRACE 612=send: publish id={0} qos={1} message={2}
					trace.trace(Trace.FINE,612, new Object[]{new Integer(message.getMessageId()), new Integer(innerMessage.getQos()), message});
				}
				switch(innerMessage.getQos()) {
				case 2:
					outboundQoS2.put(new Integer(message.getMessageId()), message);
					persistence.put(getSendPersistenceKey(message), (MqttPublish) message);
					break;
				case 1:
					outboundQoS1.put(new Integer(message.getMessageId()), message);
					persistence.put(getSendPersistenceKey(message), (MqttPublish) message);
					break;
				}
				pendingMessages.addElement(message);
				token = tokenStore.saveToken(message);

				queueLock.notifyAll();
			}
		} else if (message instanceof MqttConnect) {
			synchronized (queueLock) {
				pendingFlows.insertElementAt(message,0);
				token = tokenStore.saveToken(message);

				queueLock.notifyAll();
			}
		} else {
			if (quiescing && ((message instanceof MqttSubscribe) || (message instanceof MqttUnsubscribe))) {
				if (trace.isOn()) {
					//@TRACE 614=send: cannot send whilst quiescing. message={0}
					trace.trace(Trace.FINE,614, new Object[]{message});
				}
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
			}
			
			if (message instanceof MqttPingReq) {
				this.pingCommand = message;
			}
			else if (message instanceof MqttPubRel) {
				if (trace.isOn()) {
					//@TRACE 615=send: put pubrel to persistence. id={0}
					trace.trace(Trace.FINE,615, new Object[]{new Integer(message.getMessageId())});
				}
				outboundQoS2.put(new Integer(message.getMessageId()), message);
				persistence.put(getSendConfirmPersistenceKey(message), (MqttPubRel) message);
			}
			else if (message instanceof MqttPubComp)  {
				if (trace.isOn()) {
					//@TRACE 616=send: remove received publish from persistence. id={0}
					trace.trace(Trace.FINE,616, new Object[]{new Integer(message.getMessageId())});
				}
				persistence.remove(getReceivedPersistenceKey(message));
			}
			synchronized (queueLock) {
				pendingFlows.addElement(message);
				if ( !(message instanceof MqttAck )) {
					token = tokenStore.saveToken(message);
				}
				if (message instanceof MqttPubRel) {
					inFlightPubRels++;
					if (trace.isOn()) {
						//@TRACE 617=send: inFlightPubRels={0}
						trace.trace(Trace.FINE,617, new Object[]{new Integer(inFlightPubRels)});
					}
				}
				queueLock.notifyAll();
			}
		}

		return token;
	}
	
	/**
	 * This removes the MqttSend message from the outbound queue and persistence.
	 * @param message
	 * @throws MqttPersistenceException
	 */
	protected void undo(MqttPublish message) throws MqttPersistenceException {
		synchronized (queueLock) {
			if (trace.isOn()) {
				//@TRACE 618=undo: QoS={0} id={1}
				trace.trace(Trace.FINE,618, new Object[]{new Integer(message.getMessage().getQos()),new Integer(message.getMessageId())});
			}
			if (message.getMessage().getQos() == 1) {
				outboundQoS1.remove(new Integer(message.getMessageId()));
			} else {
				outboundQoS2.remove(new Integer(message.getMessageId()));
			}
			pendingMessages.removeElement(message);
			persistence.remove(getSendPersistenceKey(message));
			tokenStore.removeToken(message);
		}
	}
	
	/**
	 * Check whether there has been any activity in the last
	 * keep alive period.
	 */
	private MqttWireMessage checkForActivity() throws MqttException {
		MqttWireMessage result = null;
		if (System.currentTimeMillis() - lastOutboundActivity >= this.keepAlive ||
				System.currentTimeMillis() - lastInboundActivity >= this.keepAlive) {
			// Timed Out, send a ping
			if (pingOutstanding) {
				if (trace.isOn()) {
					//@TRACE 619=checkForActivity: timed-out last ping. keepAlive={0} lastOutboundActivity={1} lastInboundActivity={2}
					trace.trace(Trace.FINE,619, new Object[]{new Long(this.keepAlive),new Long(lastOutboundActivity),new Long(lastInboundActivity)});
				}
				// A ping has already been sent. At this point, assume that the
				// broker has hung and the TCP layer hasn't noticed.
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
			} else {
				if (trace.isOn()) {
					//@TRACE 620=checkForActivity: sending ping. keepAlive={0} lastOutboundActivity={1} lastInboundActivity={2}
					trace.trace(Trace.FINE,620, new Object[]{new Long(this.keepAlive),new Long(lastOutboundActivity),new Long(lastInboundActivity)});
				}
				pingOutstanding = true;
				result = pingCommand;
				tokenStore.saveToken(result);
			}
		}
		return result;
	}
	
	/**
	 * This returns the next piece of work, ie message, for the CommsSender
	 * to send over the network.
	 * Calls to this method block until either:
	 *  - there is a message to be sent
	 *  - the keepAlive interval is exceeded, which triggers a ping message
	 *    to be returned
	 *  - {@link #disconnected(MqttException, boolean)} is called
	 * @return the next message to send, or null if the client is disconnected
	 */
	protected MqttWireMessage get() throws MqttException {
		MqttWireMessage result = null;
		synchronized (queueLock) {
			if (sentConnect && connectFailed && !connected) {
				//@TRACE 648=get: not connected
				trace.trace(Trace.FINE,648);
				return null;
			}
			while (result == null) {
				if (pendingMessages.isEmpty() && pendingFlows.isEmpty()) {
					try {
						//@TRACE 644=get: wait on queueLock.
						trace.trace(Trace.FINE,644);
						queueLock.wait(this.keepAlive);
					} catch (InterruptedException e) {
					}
				}
				if (pendingFlows.isEmpty() || !((MqttWireMessage)pendingFlows.elementAt(0) instanceof MqttConnect)) {
					if (!connected) {
						//@TRACE 621=get: no outstanding flows and not connected
						trace.trace(Trace.FINE,621);
						return null;
					}
				}
				if (pendingMessages.isEmpty() && pendingFlows.isEmpty()) {
					result = checkForActivity();
				} else if (!pendingFlows.isEmpty()) {
					result = (MqttWireMessage)pendingFlows.elementAt(0);
					pendingFlows.removeElementAt(0);
					checkQuiesceLock();
				} else if (!pendingMessages.isEmpty()) {
					if (actualInFlight == this.maxInflight) {
						//@TRACE 622=get: wait on queueLock
						trace.trace(Trace.FINE,622);
						try {
							queueLock.wait(this.keepAlive);
						} catch (InterruptedException e) {
						}
						if (!connected) {
							//@TRACE 647=get: not connected
							trace.trace(Trace.FINE,647);
							return null;
						}
					}
					if (actualInFlight < this.maxInflight) {
						result = (MqttWireMessage)pendingMessages.elementAt(0);
						pendingMessages.removeElementAt(0);
						if (result == null) {
							result = checkForActivity();
						}
						else {
							actualInFlight++;
							if (trace.isOn()) {
								//@TRACE 623=get: actualInFlight={0}
								trace.trace(Trace.FINE,623,new Object[]{new Integer(actualInFlight)});
							}
						}
					}
				}
			}
		}
		if (trace.isOn()) {
			int msgId = 0;
			if (result != null) {
				msgId = result.getMessageId();
			}
			//@TRACE 624=SEND: message={0} id={1}
			trace.trace(Trace.FINE,624,new Object[]{result, new Integer(msgId)});
		}
		if (result instanceof MqttConnect) {
			sentConnect = true;
		}
		return result;
	}
	
	public void setKeepAliveInterval(long interval) {
		this.keepAlive = interval;
	}
	
	/**
	 * Called by the CommsSender when a message has been sent
	 * @param message
	 */
	protected void notifySent(MqttWireMessage message) {
		this.lastOutboundActivity = System.currentTimeMillis();
		if (trace.isOn()) {
			//@TRACE 625=notifySent: message={0}
			trace.trace(Trace.FINE,625,new Object[]{message});
		}

		MqttDeliveryTokenImpl token = tokenStore.getToken(message);
		token.notifySent();
		if (message instanceof MqttPublish) {
			if (((MqttPublish)message).getMessage().getQos() == 0) {
				// once a QOS 0 message is sent we can clean up its records straight away as
				// we won't be hearing about it again
				token.notifyReceived(null);
				tokenStore.removeToken(message);
				callback.deliveryComplete(token);
				decrementInFlight();
				releaseMessageId(message.getMessageId());
			}
		}
		// No ack expected for MqttDisconnect in v3 - so remove the token from the store
		if (message instanceof MqttDisconnect) {
			tokenStore.removeToken(message);
		}
	}

	private void decrementInFlight() {
		synchronized (queueLock) {
			actualInFlight--;
			if (trace.isOn()) {
				//@TRACE 646=decrementInFlight: actualInFlight={0}
				trace.trace(Trace.FINE,646,new Object[]{new Integer(actualInFlight)});
			}
			if (!checkQuiesceLock()) {
				queueLock.notifyAll();
			}
		}
	}
	
	private boolean checkQuiesceLock() {
		if (trace.isOn()) {
			//@TRACE 626=checkQuiesceLock: quiescing={0} actualInFlight={1} pendingFlows={2} inFlightPubRels={3}
			trace.trace(Trace.FINE,626,new Object[]{new Boolean(quiescing), new Integer(actualInFlight), new Integer(pendingFlows.size()), new Integer(inFlightPubRels)});
		}
		if (quiescing && actualInFlight == 0 && pendingFlows.size() == 0 && inFlightPubRels == 0) {
			synchronized (quiesceLock) {
				quiesceLock.notifyAll();
			}
			return true;
		}
		return false;
	}
	/**
	 * Called by the CommsReceiver when a new message has arrived.
	 * @param message
	 * @throws MqttException
	 */
	protected void notifyReceived(MqttWireMessage message) throws MqttException {
		this.lastInboundActivity = System.currentTimeMillis();

		if (trace.isOn()) {
			//@TRACE 627=RCVD: message={0} id={1}
			trace.trace(Trace.FINE,627,new Object[]{message, new Integer(message.getMessageId())});
		}
		
		if (message instanceof MqttAck) {
			MqttAck ack = (MqttAck) message;
			MqttDeliveryTokenImpl token = tokenStore.getToken(message);

			if ((ack instanceof MqttPubRec) &&
				outboundQoS2.containsKey(new Integer(ack.getMessageId()))) {
				// QoS 2
				MqttPubRel rel = new MqttPubRel((MqttPubRec) ack);
				this.send(rel);
			} else {
				if (ack instanceof MqttPubAck) {
					// QoS 1
					if (trace.isOn()) {
						//@TRACE 628=notifyReceived: removing QoS 1 publish. id={0}
						trace.trace(Trace.FINE,628,new Object[]{new Integer(ack.getMessageId())});
					}
					persistence.remove(getSendPersistenceKey(message));
					outboundQoS1.remove(new Integer(ack.getMessageId()));
				}
				else if (ack instanceof MqttPubComp) {
					outboundQoS2.remove(new Integer(ack.getMessageId()));
					persistence.remove(getSendPersistenceKey(message));
					persistence.remove(getSendConfirmPersistenceKey(message));
					inFlightPubRels--;
					if (trace.isOn()) {
						//@TRACE 645=notifyReceived: removing QoS 2 publish/pubrel. id={0} inFlightPubRels={1}
						trace.trace(Trace.FINE,645,new Object[]{new Integer(ack.getMessageId()), new Integer(inFlightPubRels)});
					}
				}
				releaseMessageId(message.getMessageId());
				if ((ack instanceof MqttPubAck) ||
					(ack instanceof MqttPubRec) || 
					(ack instanceof MqttPubComp)) {
					decrementInFlight();
				}
				if (ack instanceof MqttPingResp) {
					//@TRACE 629=notifyReceived: ping response
					trace.trace(Trace.FINE,629);
					pingOutstanding = false;
				}
				else if (message instanceof MqttConnack) {
					if (((MqttConnack)message).getReturnCode() == 0) {
						if (cleanSession) {
							clearState();
						}
						inFlightPubRels = 0;
						actualInFlight = 0;
						restoreInflightMessages();
						connected();
					} else {
						connectFailed = true;
					}
					// Notify the sender thread that there maybe work for it to do now
					synchronized (queueLock) {
						queueLock.notifyAll();
					}
				}
				tokenStore.responseReceived((MqttAck)message);
				if ((ack instanceof MqttPubAck) ||
						(ack instanceof MqttPubComp)) {
					callback.deliveryComplete(token);
				}
				checkQuiesceLock();
			}
		}
		// Only handle incoming PUBLISH/PUBREL messages if we're not already shutting down...
		else if (!quiescing) {
			if (message instanceof MqttPublish) {
				MqttPublish send = (MqttPublish) message;
				switch(send.getMessage().getQos()) {
				case 0:
				case 1:
					if (callback != null) {
						callback.messageArrived(send);
					}
					break;
				case 2:
					if (trace.isOn()) {
						//@TRACE 630=notifyReceived: adding QoS 2 publish to persistence id={0}
						trace.trace(Trace.FINE,630, new Object[]{new Integer(send.getMessageId())});
					}
					persistence.put(getReceivedPersistenceKey(message), (MqttPublish) message);
					inboundQoS2.put(new Integer(send.getMessageId()),send);
					this.send(new MqttPubRec(send));
				}
			}
			else if (message instanceof MqttPubRel) {
				MqttPublish sendMsg = (MqttPublish)inboundQoS2.get(new Integer(message.getMessageId()));
				if (sendMsg!= null) {
					if (callback != null) {
						callback.messageArrived(sendMsg);
					}
				} else {
					// Original publish has already been delivered.
					MqttPubComp pubComp = new MqttPubComp(message.getMessageId());
					this.send(pubComp);
				}
			}
		}
	}
	
	/**
	 * Called when the client has successfully connected to the broker
	 */
	public void connected() {
		//@TRACE 631=connected
		trace.trace(Trace.FINE,631);
		this.connected = true;
	}
	
	/**
	 * Called when the client is in the process of disconnecting
	 * from the broker.
	 * @param reason The root cause of the disconnection, or null if it is a clean disconnect
	 */
	public void disconnecting(MqttException reason) {
		//@TRACE 632=disconnecting
		trace.trace(Trace.FINE,632,null,reason);
		synchronized (queueLock) {
			queueLock.notifyAll();
		}
		tokenStore.noMoreResponses(reason);
	}
	
	/**
	 * Called when the client has been disconnected from the broker.
	 * @param reason The root cause of the disconnection, or null if it is a clean disconnect
	 */
	public void disconnected(MqttException reason) {
		//@TRACE 633=disconnected
		trace.trace(Trace.FINE,633,null,reason);
		this.connected = false;
		//tokenStore.noMoreResponses(reason);
		synchronized (queueLock) {
			queueLock.notifyAll();
		}
		try {
			if (cleanSession) {
				clearState();
			}
			pendingMessages.clear();
			pendingFlows.clear();
			// Reset pingOutstanding to allow reconnects to assume no previous ping.
		    pingOutstanding = false;
		    
			// Wait until there are no threads in token.waitUntilSent calls.
			// This allows them to undo their work, if needed, before we
			// close persistence.
			synchronized (waitingTokensLock) {
				if (trace.isOn()) {
					//@TRACE 634=disconnected: waitingTokens={0}
					trace.trace(Trace.FINE,634,new Object[]{new Integer(waitingTokens)});
				}
				while (waitingTokens > 0) {
					try {
						waitingTokensLock.wait();
					} catch (InterruptedException e) {
					}
				}
			}
			//@TRACE 635=disconnected: Close persistence
			trace.trace(Trace.FINE,635);
			persistence.close();
		} catch (MqttException e) {
			// Ignore as we have disconnected at this point
		}
	}
	
	/**
	 * Releases a message ID back into the pool of available message IDs.
	 * If the supplied message ID is not in use, then nothing will happen.
	 * 
	 * @param msgId A message ID that can be freed up for re-use.
	 */
	private synchronized void releaseMessageId(int msgId) {
		inUseMsgIds.remove(new Integer(msgId));
	}

	/**
	 * Get the next MQTT message ID that is not already in use, and marks
	 * it as now being in use.
	 * 
	 * @return the next MQTT message ID to use
	 */
	private synchronized int getNextMessageId() throws MqttException {
		int startingMessageId = nextMsgId;
		// Allow two complete passes of the message ID range. This gives
		// any asynchronous releases a chance to occur
		int loopCount = 0;
	    do {
	        nextMsgId++;
	        if ( nextMsgId > MAX_MSG_ID ) {
	            nextMsgId = MIN_MSG_ID;
	        }
	        if (nextMsgId == startingMessageId) {
	        	loopCount++;
	        	if (loopCount == 2) {
	        		throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE);
	        	}
	        }
	    } while( inUseMsgIds.containsKey( new Integer(nextMsgId) ) );
	    Integer id = new Integer(nextMsgId);
	    inUseMsgIds.put(id, id);
	    return nextMsgId;
	}
	
	/**
	 * Cleans up the supplied queue, notifying any tokens waiting for the
	 * messages on the queue.
	 */
	private void cleanUpQueue(Vector queue) {
		//@TRACE 636=cleanUpQueue
		trace.trace(Trace.FINE,636);

		Enumeration e = queue.elements();
		MqttWireMessage message;
		MqttDeliveryTokenImpl token;
		MqttException ex = ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
		Integer messageId;
		while (e.hasMoreElements()) {
			message = (MqttWireMessage) e.nextElement();
			token = this.tokenStore.getToken(message);
			messageId = new Integer(message.getMessageId());
			// It may be QoS 2, so prevent the CONFIRM from being sent.
			if (outboundQoS2.containsKey(messageId)) {
				outboundQoS2.remove(messageId);
			}
			// Outbound acks do not have tokens in the store
			if (token != null) {
				token.notifyException(ex);
				tokenStore.removeToken(message);
			}
			queue.removeElement(message);
		}
	}
	
	/**
	 * Quiesce the client state, preventing any new messages getting sent,
	 * and preventing the callback on any newly received messages.
	 * After the timeout expires, delete any pending messages except for
	 * outbound ACKs, and wait for those ACKs to complete.
	 */
	public void quiesce(long timeout) {
		//@TRACE 637=quiesce: timeout={0}
		trace.trace(Trace.FINE,637,new Object[]{new Long(timeout)});
		if (timeout > 0) {
			synchronized (queueLock) {
				this.quiescing = true;
			}
			// We don't want to handle any more inbound messages
			callback.quiesce();
			synchronized (queueLock) {
				//@TRACE 638=quiesce: notifying queueLock
				trace.trace(Trace.FINE,638);
				queueLock.notifyAll();
			}

			synchronized (quiesceLock) {
				try {
					if ((actualInFlight>0) || pendingFlows.size() > 0 || inFlightPubRels > 0) {
						if (trace.isOn()) {
							//@TRACE 639=quiesce: waiting. actualInFlight={0} pendingFlows={1} inFlightPubRels={2}
							trace.trace(Trace.FINE,639, new Object[]{new Integer(actualInFlight), new Integer(pendingFlows.size()), new Integer(inFlightPubRels)});
						}
						// wait for outstanding in flight messages to complete and
						// any pending flows to complete
						quiesceLock.wait(timeout);
						//@TRACE 640=quiesce: done waiting
						trace.trace(Trace.FINE,640);
					}
				}
				catch (InterruptedException ex) {
					// Don't care, as we're shutting down anyway
				}
			}
			
			synchronized (queueLock) {
				cleanUpQueue(pendingMessages);
				cleanUpQueue(pendingFlows);
				quiescing = false;
				actualInFlight = 0;
			}
		}
	}

	protected void deliveryComplete(MqttPublish message) throws MqttPersistenceException {
		if (trace.isOn()) {
			//@TRACE 641=deliveryComplete: remove publish from persistence. id={0}
			trace.trace(Trace.FINE,641, new Object[]{new Integer(message.getMessageId())});
		}
		persistence.remove(getReceivedPersistenceKey(message));
		inboundQoS2.remove(new Integer(message.getMessageId()));
	}
	
	/**
	 * Increments the count of threads waiting in token.waitUntilSent calls
	 */
	protected void incrementWaitingTokens() {
		synchronized (waitingTokensLock) {
			waitingTokens++;;
			if (trace.isOn()) {
				//@TRACE 642=incrementWaitingTokens: waitingTokens={0}
				trace.trace(Trace.FINE,642, new Object[]{new Integer(waitingTokens)});
			}
		}
	}

	/**
	 * Decrements the count of threads waiting in token.waitUntilSent calls. If
	 * the count hits 0, notify any thread waiting on the lock.
	 * @see #disconnected(MqttException, boolean)
	 */
	protected void decrementWaitingTokens() {
		synchronized (waitingTokensLock) {
			waitingTokens--;
			if (trace.isOn()) {
				//@TRACE 643=decrementWaitingTokens: waitingTokens={0}
				trace.trace(Trace.FINE,643, new Object[]{new Integer(waitingTokens)});
			}
			if (waitingTokens == 0) {
				waitingTokensLock.notifyAll();
			}
		}
	}
}
