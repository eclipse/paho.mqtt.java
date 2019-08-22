/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp and others.
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
 *    Ian Craggs - fix duplicate message id (Bug 466853)
 *    Ian Craggs - ack control (bug 472172)
 *    James Sutton - Ping Callback (bug 473928)
 *    Ian Craggs - fix for NPE bug 470718
 *    James Sutton - Automatic Reconnect & Offline Buffering
 *    Jens Reimann - Fix issue #370
 *    James Sutton - Mqttv5 - Outgoing Topic Aliases
 */
package org.eclipse.paho.mqttv5.client.internal;

import java.io.EOFException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.MqttPingSender;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistable;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.packet.MqttAck;
import org.eclipse.paho.mqttv5.common.packet.MqttAuth;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttConnect;
import org.eclipse.paho.mqttv5.common.packet.MqttPingReq;
import org.eclipse.paho.mqttv5.common.packet.MqttPingResp;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttPubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttPubComp;
import org.eclipse.paho.mqttv5.common.packet.MqttPubRec;
import org.eclipse.paho.mqttv5.common.packet.MqttPubRel;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

/**
 * The core of the client, which holds the state information for pending and
 * in-flight messages.
 * 
 * Messages that have been accepted for delivery are moved between several
 * objects while being delivered.
 * 
 * 1) When the client is not running messages are stored in a persistent store
 * that implements the MqttClientPersistent Interface. The default is
 * MqttDefaultFilePersistencew which stores messages safely across failures and
 * system restarts. If no persistence is specified there is a fall back to
 * MemoryPersistence which will maintain the messages while the Mqtt client is
 * instantiated.
 * 
 * 2) When the client or specifically ClientState is instantiated the messages
 * are read from the persistent store into: - outboundqos2 hashtable if a QoS 2
 * PUBLISH or PUBREL - outboundqos1 hashtable if a QoS 1 PUBLISH (see
 * restoreState)
 * 
 * 3) On Connect, copy messages from the outbound hashtables to the
 * pendingMessages or pendingFlows vector in messageid order. - Initial message
 * publish goes onto the pendingmessages buffer. - PUBREL goes onto the
 * pendingflows buffer (see restoreInflightMessages)
 * 
 * 4) Sender thread reads messages from the pendingflows and pendingmessages
 * buffer one at a time. The message is removed from the pendingbuffer but
 * remains on the outbound* hashtable. The hashtable is the place where the full
 * set of outstanding messages are stored in memory. (Persistence is only used
 * at start up)
 * 
 * 5) Receiver thread - receives wire messages: - if QoS 1 then remove from
 * persistence and outboundqos1 - if QoS 2 PUBREC send PUBREL. Updating the
 * outboundqos2 entry with the PUBREL and update persistence. - if QoS 2 PUBCOMP
 * remove from persistence and outboundqos2
 * 
 * Notes: because of the multithreaded nature of the client it is vital that any
 * changes to this class take concurrency into account. For instance as soon as
 * a flow / message is put on the wire it is possible for the receiving thread
 * to receive the ack and to be processing the response before the sending side
 * has finished processing. For instance a connect may be sent, the conack
 * received before the connect notify send has been processed!
 * 
 */
public class ClientState implements MqttState {
	private static final String CLASS_NAME = ClientState.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);
	private static final String PERSISTENCE_SENT_PREFIX = "s-";
	private static final String PERSISTENCE_SENT_BUFFERED_PREFIX = "sb-";
	private static final String PERSISTENCE_CONFIRMED_PREFIX = "sc-";
	private static final String PERSISTENCE_RECEIVED_PREFIX = "r-";

	private static final int MIN_MSG_ID = 1; // Lowest possible MQTT message ID to use
	private static final int MAX_MSG_ID = 65535; // Highest possible MQTT message ID to use
	private int nextMsgId = MIN_MSG_ID - 1; // The next available message ID to use
	private ConcurrentHashMap<Integer, Integer> inUseMsgIds; // Used to store a set of in-use message IDs

	volatile private Vector<MqttWireMessage> pendingMessages;
	volatile private Vector<MqttWireMessage> pendingFlows;

	private CommsTokenStore tokenStore;
	private ClientComms clientComms = null;
	private CommsCallback callback = null;
	//private long keepAlive;
	private boolean cleanStart;
	private MqttClientPersistence persistence;

	private int actualInFlight = 0;
	private int inFlightPubRels = 0;

	private final Object queueLock = new Object();
	private final Object quiesceLock = new Object();
	private boolean quiescing = false;

	private long lastOutboundActivity = 0;
	private long lastInboundActivity = 0;
	private long lastPing = 0;
	private MqttWireMessage pingCommand;
	private final Object pingOutstandingLock = new Object();
	private int pingOutstanding = 0;

	private boolean connected = false;

	private ConcurrentHashMap<Integer, MqttWireMessage> outboundQoS2 = null;
	private ConcurrentHashMap<Integer, MqttWireMessage> outboundQoS1 = null;
	private ConcurrentHashMap<Integer, MqttWireMessage> outboundQoS0 = null;
	private ConcurrentHashMap<Integer, MqttWireMessage> inboundQoS2 = null;

	private MqttPingSender pingSender = null;

	// Topic Alias Maps
	private Hashtable<String, Integer> outgoingTopicAliases;
	private Hashtable<Integer, String> incomingTopicAliases;

	private MqttConnectionState mqttConnection;

	protected ClientState(MqttClientPersistence persistence, CommsTokenStore tokenStore, CommsCallback callback,
			ClientComms clientComms, MqttPingSender pingSender, MqttConnectionState mqttConnection)
			throws MqttException {

		log.setResourceName(clientComms.getClient().getClientId());
		log.finer(CLASS_NAME, "<Init>", "");

		inUseMsgIds = new ConcurrentHashMap<>();
		pendingFlows = new Vector<MqttWireMessage>();
		pendingMessages = new Vector<MqttWireMessage>(mqttConnection.getReceiveMaximum());
		outboundQoS2 = new ConcurrentHashMap<>();
		outboundQoS1 = new ConcurrentHashMap<>();
		outboundQoS0 = new ConcurrentHashMap<>();
		inboundQoS2 = new ConcurrentHashMap<>();
		pingCommand = new MqttPingReq();
		inFlightPubRels = 0;
		actualInFlight = 0;
		this.outgoingTopicAliases = new Hashtable<String, Integer>();
		this.incomingTopicAliases = new Hashtable<Integer, String>();

		this.persistence = persistence;
		this.callback = callback;
		this.tokenStore = tokenStore;
		this.clientComms = clientComms;
		this.pingSender = pingSender;
		this.mqttConnection = mqttConnection;

		restoreState();
	}

	protected void setCleanStart(boolean cleanStart) {
		this.cleanStart = cleanStart;
	}

	protected boolean getCleanStart() {
		return this.cleanStart;
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

	private String getReceivedPersistenceKey(int messageId) {
		return PERSISTENCE_RECEIVED_PREFIX + messageId;
	}

	private String getSendBufferedPersistenceKey(MqttWireMessage message) {
		return PERSISTENCE_SENT_BUFFERED_PREFIX + message.getMessageId();
	}

	protected void clearState() throws MqttException {
		final String methodName = "clearState";
		// @TRACE 603=clearState
		log.fine(CLASS_NAME, methodName, ">");

		persistence.clear();
		inUseMsgIds.clear();
		pendingMessages.clear();
		pendingFlows.clear();
		outboundQoS2.clear();
		outboundQoS1.clear();
		outboundQoS0.clear();
		inboundQoS2.clear();
		tokenStore.clear();
		outgoingTopicAliases.clear();
		incomingTopicAliases.clear();
	}

	protected void clearConnectionState() throws MqttException {
		final String methodName = "clearConnectionState";
		// @TRACE=665=Clearing Connection State (Topic Aliases)
		log.fine(CLASS_NAME, methodName, "665");
		outgoingTopicAliases.clear();
		incomingTopicAliases.clear();

	}

	private MqttWireMessage restoreMessage(String key, MqttPersistable persistable) throws MqttException {
		final String methodName = "restoreMessage";
		MqttWireMessage message = null;

		try {
			message = MqttWireMessage.createWireMessage(persistable);
		} catch (MqttException ex) {
			// @TRACE 602=key={0} exception
			log.fine(CLASS_NAME, methodName, "602", new Object[] { key }, ex);
			if (ex.getCause() instanceof EOFException) {
				// Premature end-of-file means that the message is corrupted
				if (key != null) {
					persistence.remove(key);
				}
			} else {
				throw ex;
			}
		}
		// @TRACE 601=key={0} message={1}
		log.fine(CLASS_NAME, methodName, "601", new Object[] { key, message });
		return message;
	}

	/**
	 * Inserts a new message to the list, ensuring that list is ordered from lowest
	 * to highest in terms of the message id's.
	 * 
	 * @param list
	 *            the list to insert the message into
	 * @param newMsg
	 *            the message to insert into the list
	 */
	private void insertInOrder(Vector<MqttWireMessage> list, MqttWireMessage newMsg) {
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
	 * Produces a new list with the messages properly ordered according to their
	 * message id's.
	 * 
	 * @param list
	 *            the list containing the messages to produce a new reordered list
	 *            for - this will not be modified or replaced, i.e., be read-only to
	 *            this method
	 * @return a new reordered list
	 */
	private Vector<MqttWireMessage> reOrder(Vector<MqttWireMessage> list) {

		// here up the new list
		Vector<MqttWireMessage> newList = new Vector<MqttWireMessage>();

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

		// we need to check that the gap after highest msg id to the lowest msg id is
		// not beaten
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
	 * 
	 * @throws MqttException
	 *             if an exception occurs whilst restoring state
	 */
	protected void restoreState() throws MqttException {
		final String methodName = "restoreState";
		Enumeration<String> messageKeys = persistence.keys();
		MqttPersistable persistable;
		String key;
		int highestMsgId = nextMsgId;
		Vector<String> orphanedPubRels = new Vector<String>();
		// @TRACE 600=>
		log.fine(CLASS_NAME, methodName, "600");

		while (messageKeys.hasMoreElements()) {
			key = (String) messageKeys.nextElement();
			persistable = persistence.get(key);
			MqttWireMessage message = restoreMessage(key, persistable);
			if (message != null) {
				if (key.startsWith(PERSISTENCE_RECEIVED_PREFIX)) {
					// @TRACE 604=inbound QoS 2 publish key={0} message={1}
					log.fine(CLASS_NAME, methodName, "604", new Object[] { key, message });

					// The inbound messages that we have persisted will be QoS 2
					inboundQoS2.put(Integer.valueOf(message.getMessageId()), message);
				} else if (key.startsWith(PERSISTENCE_SENT_PREFIX)) {
					MqttPublish sendMessage = (MqttPublish) message;
					highestMsgId = Math.max(sendMessage.getMessageId(), highestMsgId);
					if (persistence.containsKey(getSendConfirmPersistenceKey(sendMessage))) {
						MqttPersistable persistedConfirm = persistence.get(getSendConfirmPersistenceKey(sendMessage));
						// QoS 2, and CONFIRM has already been sent...
						// NO DUP flag is allowed for 3.1.1 spec while it's not clear for 3.1 spec
						// So we just remove DUP
						MqttPubRel confirmMessage = (MqttPubRel) restoreMessage(key, persistedConfirm);
						if (confirmMessage != null) {
							// confirmMessage.setDuplicate(true); // REMOVED
							// @TRACE 605=outbound QoS 2 pubrel key={0} message={1}
							log.fine(CLASS_NAME, methodName, "605", new Object[] { key, message });

							outboundQoS2.put(Integer.valueOf(confirmMessage.getMessageId()), confirmMessage);
						} else {
							// @TRACE 606=outbound QoS 2 completed key={0} message={1}
							log.fine(CLASS_NAME, methodName, "606", new Object[] { key, message });
						}
					} else {
						// QoS 1 or 2, with no CONFIRM sent...
						// Put the SEND to the list of pending messages, ensuring message ID ordering...
						sendMessage.setDuplicate(true);
						if (sendMessage.getMessage().getQos() == 2) {
							// @TRACE 607=outbound QoS 2 publish key={0} message={1}
							log.fine(CLASS_NAME, methodName, "607", new Object[] { key, message });

							outboundQoS2.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
						} else {
							// @TRACE 608=outbound QoS 1 publish key={0} message={1}
							log.fine(CLASS_NAME, methodName, "608", new Object[] { key, message });

							outboundQoS1.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
						}
					}
					MqttDeliveryToken tok = tokenStore.restoreToken(sendMessage);
					tok.internalTok.setClient(clientComms.getClient());
					inUseMsgIds.put(Integer.valueOf(sendMessage.getMessageId()),
							Integer.valueOf(sendMessage.getMessageId()));
				} else if (key.startsWith(PERSISTENCE_SENT_BUFFERED_PREFIX)) {

					// Buffered outgoing messages that have not yet been sent at all
					MqttPublish sendMessage = (MqttPublish) message;
					highestMsgId = Math.max(sendMessage.getMessageId(), highestMsgId);
					if (sendMessage.getMessage().getQos() == 2) {
						// @TRACE 607=outbound QoS 2 publish key={0} message={1}
						log.fine(CLASS_NAME, methodName, "607", new Object[] { key, message });
						outboundQoS2.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
					} else if (sendMessage.getMessage().getQos() == 1) {
						// @TRACE 608=outbound QoS 1 publish key={0} message={1}
						log.fine(CLASS_NAME, methodName, "608", new Object[] { key, message });

						outboundQoS1.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);

					} else {
						// @TRACE 511=outbound QoS 0 publish key={0} message={1}
						log.fine(CLASS_NAME, methodName, "511", new Object[] { key, message });
						outboundQoS0.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
						// Because there is no Puback, we have to trust that this is enough to send the
						// message
						persistence.remove(key);

					}

					MqttDeliveryToken tok = tokenStore.restoreToken(sendMessage);
					tok.internalTok.setClient(clientComms.getClient());
					inUseMsgIds.put(Integer.valueOf(sendMessage.getMessageId()),
							Integer.valueOf(sendMessage.getMessageId()));

				} else if (key.startsWith(PERSISTENCE_CONFIRMED_PREFIX)) {
					MqttPubRel pubRelMessage = (MqttPubRel) message;
					if (!persistence.containsKey(getSendPersistenceKey(pubRelMessage))) {
						orphanedPubRels.addElement(key);
					}
				}
			}
		}

		messageKeys = orphanedPubRels.elements();
		while (messageKeys.hasMoreElements()) {
			key = (String) messageKeys.nextElement();
			// @TRACE 609=removing orphaned pubrel key={0}
			log.fine(CLASS_NAME, methodName, "609", new Object[] { key });

			persistence.remove(key);
		}

		nextMsgId = highestMsgId;
	}

	private void restoreInflightMessages() {
		final String methodName = "restoreInflightMessages";
		pendingMessages = new Vector<MqttWireMessage>(this.mqttConnection.getReceiveMaximum());
		pendingFlows = new Vector<MqttWireMessage>();

		Enumeration<Integer> keys = outboundQoS2.keys();
		while (keys.hasMoreElements()) {
			Integer key = keys.nextElement();
			MqttWireMessage msg = (MqttWireMessage) outboundQoS2.get(key);
			if (msg instanceof MqttPublish) {
				// @TRACE 610=QoS 2 publish key={0}
				log.fine(CLASS_NAME, methodName, "610", new Object[] { key });
				// set DUP flag only for PUBLISH, but NOT for PUBREL (spec 3.1.1)
				msg.setDuplicate(true);
				insertInOrder(pendingMessages, (MqttPublish) msg);
			} else if (msg instanceof MqttPubRel) {
				// @TRACE 611=QoS 2 pubrel key={0}
				log.fine(CLASS_NAME, methodName, "611", new Object[] { key });

				insertInOrder(pendingFlows, (MqttPubRel) msg);
			}
		}
		keys = outboundQoS1.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			MqttPublish msg = (MqttPublish) outboundQoS1.get(key);
			msg.setDuplicate(true);
			// @TRACE 612=QoS 1 publish key={0}
			log.fine(CLASS_NAME, methodName, "612", new Object[] { key });

			insertInOrder(pendingMessages, msg);
		}
		keys = outboundQoS0.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			MqttPublish msg = (MqttPublish) outboundQoS0.get(key);
			// @TRACE 512=QoS 0 publish key={0}
			log.fine(CLASS_NAME, methodName, "512", new Object[] { key });
			insertInOrder(pendingMessages, msg);

		}

		this.pendingFlows = reOrder(pendingFlows);
		this.pendingMessages = reOrder(pendingMessages);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#send(org.eclipse.paho.
	 * client.mqttv3.internal.wire.MqttWireMessage,
	 * org.eclipse.paho.mqttv5.client.MqttToken)
	 */
	@Override
	public void send(MqttWireMessage message, MqttToken token) throws MqttException {
		final String methodName = "send";
		// Set Message ID if required
		if (message.isMessageIdRequired() && (message.getMessageId() == 0)) {
			message.setMessageId(getNextMessageId());
		}
		// Set Topic Alias if required
		if (message instanceof MqttPublish && ((MqttPublish) message).getTopicName() != null 
				&& this.mqttConnection.getOutgoingTopicAliasMaximum() > 0) {
			String topic = ((MqttPublish) message).getTopicName();
			if (outgoingTopicAliases.containsKey(topic)) {
				// Existing Topic Alias, Assign it and remove the topic string
				((MqttPublish) message).getProperties().setTopicAlias(outgoingTopicAliases.get(topic));
				((MqttPublish) message).setTopicName(null);
			} else {
				int nextOutgoingTopicAlias = this.mqttConnection.getNextOutgoingTopicAlias();
				if (nextOutgoingTopicAlias <= this.mqttConnection.getOutgoingTopicAliasMaximum()) {
					// Create a new Topic Alias and increment the counter
					((MqttPublish) message).getProperties().setTopicAlias(nextOutgoingTopicAlias);
					outgoingTopicAliases.put(((MqttPublish) message).getTopicName(), nextOutgoingTopicAlias);
				}
			}
		}

		if (token != null) {
			try {
				token.internalTok.setMessageID(message.getMessageId());
			} catch (Exception e) {
			}
		}

		if (message instanceof MqttPublish) {
			synchronized (queueLock) {
				if (actualInFlight >= this.mqttConnection.getReceiveMaximum()) {
					// @TRACE 613= sending {0} msgs at max inflight window
					log.fine(CLASS_NAME, methodName, "613", new Object[] { Integer.valueOf(actualInFlight) });

					throw new MqttException(MqttClientException.REASON_CODE_MAX_INFLIGHT);
				}

				MqttMessage innerMessage = ((MqttPublish) message).getMessage();
				// @TRACE 628=pending publish key={0} qos={1} message={2}
				log.fine(CLASS_NAME, methodName, "628", new Object[] { Integer.valueOf(message.getMessageId()),
						Integer.valueOf(innerMessage.getQos()), message });

				switch (innerMessage.getQos()) {
				case 2:
					outboundQoS2.put(Integer.valueOf(message.getMessageId()), message);
					persistence.put(getSendPersistenceKey(message), (MqttPublish) message);
					break;
				case 1:
					outboundQoS1.put(Integer.valueOf(message.getMessageId()), message);
					persistence.put(getSendPersistenceKey(message), (MqttPublish) message);
					break;
				}
				tokenStore.saveToken(token, message);
				pendingMessages.addElement(message);
				queueLock.notifyAll();
			}
		} else {
			// @TRACE 615=pending send key={0} message {1}
			log.fine(CLASS_NAME, methodName, "615", new Object[] { Integer.valueOf(message.getMessageId()), message });

			if (message instanceof MqttConnect) {
				synchronized (queueLock) {
					// Add the connect action at the head of the pending queue ensuring it jumps
					// ahead of any of other pending actions.
					tokenStore.saveToken(token, message);
					pendingFlows.insertElementAt(message, 0);
					queueLock.notifyAll();
				}
			} else {
				if (message instanceof MqttPingReq) {
					this.pingCommand = message;
				} else if (message instanceof MqttPubRel) {
					outboundQoS2.put(Integer.valueOf(message.getMessageId()), message);
					persistence.put(getSendConfirmPersistenceKey(message), (MqttPubRel) message);
				} else if (message instanceof MqttPubComp) {
					persistence.remove(getReceivedPersistenceKey(message));
				}

				synchronized (queueLock) {
					if (!(message instanceof MqttAck)) {
						tokenStore.saveToken(token, message);
					}
					pendingFlows.addElement(message);
					queueLock.notifyAll();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.internal.MqttState#persistBufferedMessage(org.
	 * eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage)
	 */
	@Override
	public void persistBufferedMessage(MqttWireMessage message) {
		final String methodName = "persistBufferedMessage";
		String key = getSendBufferedPersistenceKey(message);

		// Because the client will have disconnected, we will want to re-open
		// persistence
		try {
			message.setMessageId(getNextMessageId());
			key = getSendBufferedPersistenceKey(message);
			try {
				persistence.put(key, (MqttPublish) message);
			} catch (MqttPersistenceException mpe) {
				// @TRACE 515=Could not Persist, attempting to Re-Open Persistence Store
				log.fine(CLASS_NAME, methodName, "515");
				persistence.open(this.clientComms.getClient().getClientId());
				persistence.put(key, (MqttPublish) message);
			}
			// @TRACE 513=Persisted Buffered Message key={0}
			log.fine(CLASS_NAME, methodName, "513", new Object[] { key });
		} catch (MqttException ex) {
			// @TRACE 514=Failed to persist buffered message key={0}
			log.warning(CLASS_NAME, methodName, "513", new Object[] { key });
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.internal.MqttState#unPersistBufferedMessage(
	 * org.eclipse.paho.mqttv5.client.internal.wire.MqttWireMessage)
	 */
	@Override
	public void unPersistBufferedMessage(MqttWireMessage message) {
		final String methodName = "unPersistBufferedMessage";
		try {
			// @TRACE 517=Un-Persisting Buffered message key={0}
			log.fine(CLASS_NAME, methodName, "517", new Object[] { message.getKey() });
			persistence.remove(getSendBufferedPersistenceKey(message));
		} catch (MqttPersistenceException mpe) {
			// @TRACE 518=Failed to Un-Persist Buffered message key={0}
			log.fine(CLASS_NAME, methodName, "518", new Object[] { message.getKey() });
		}

	}

	/**
	 * This removes the MqttSend message from the outbound queue and persistence.
	 * 
	 * @param message
	 *            the {@link MqttPublish} message to be removed
	 * @throws MqttPersistenceException
	 *             if an exception occurs whilst removing the message
	 */
	protected void undo(MqttPublish message) throws MqttPersistenceException {
		final String methodName = "undo";
		synchronized (queueLock) {
			// @TRACE 618=key={0} QoS={1}
			log.fine(CLASS_NAME, methodName, "618", new Object[] { Integer.valueOf(message.getMessageId()),
					Integer.valueOf(message.getMessage().getQos()) });

			if (message.getMessage().getQos() == 1) {
				outboundQoS1.remove(Integer.valueOf(message.getMessageId()));
			} else {
				outboundQoS2.remove(Integer.valueOf(message.getMessageId()));
			}
			pendingMessages.removeElement(message);
			persistence.remove(getSendPersistenceKey(message));
			tokenStore.removeToken(message);
			if (message.getMessage().getQos() > 0) {
				// Free this message Id so it can be used again
				releaseMessageId(message.getMessageId());
				// Set the messageId to 0 so if it's ever retried, it will get a new messageId
				message.setMessageId(0);
			}

			checkQuiesceLock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#checkForActivity(org.
	 * eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	@Override
	public MqttToken checkForActivity(MqttActionListener pingCallback) throws MqttException {
		final String methodName = "checkForActivity";
		// @TRACE 616=checkForActivity entered
		log.fine(CLASS_NAME, methodName, "616", new Object[] {});

		synchronized (quiesceLock) {
			// ref bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=440698
			// No ping while quiescing
			if (quiescing) {
				return null;
			}
		}

		MqttToken token = null;
		
		long nextPingTime, keepAlive = TimeUnit.MILLISECONDS.toNanos(this.mqttConnection.getKeepAlive());

		if (connected && this.mqttConnection.getKeepAlive() > 0) {
			long time = System.nanoTime();
			// Reduce schedule frequency since System.currentTimeMillis is no accurate, add
			// a buffer (This might not be needed since we moved to nanoTime)
			// It is 1/10 in minimum keepalive unit.
			int delta = 100000;

			// ref bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=446663
			synchronized (pingOutstandingLock) {

				// Is the broker connection lost because the broker did not reply to my ping?
				if (pingOutstanding > 0 && (time - lastInboundActivity >= keepAlive + delta)) {
					// lastInboundActivity will be updated once receiving is done.
					// Add a delta, since the timer and System.currentTimeMillis() is not accurate.
					// (This might not be needed since we moved to nanoTime)
					// A ping is outstanding but no packet has been received in KA so connection is
					// deemed broken
					// @TRACE 619=Timed out as no activity, keepAlive={0} lastOutboundActivity={1}
					// lastInboundActivity={2} time={3} lastPing={4}
					log.severe(CLASS_NAME, methodName, "619",
							new Object[] { Long.valueOf(keepAlive), Long.valueOf(lastOutboundActivity),
									Long.valueOf(lastInboundActivity), Long.valueOf(time), Long.valueOf(lastPing) });

					// A ping has already been sent. At this point, assume that the
					// broker has hung and the TCP layer hasn't noticed.
					throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_TIMEOUT);
				}

				// Is the broker connection lost because I could not get any successful write
				// for 2 keepAlive intervals?
				if (pingOutstanding == 0 && (time - lastOutboundActivity >= 2 * keepAlive)) {

					// I am probably blocked on a write operations as I should have been able to
					// write at least a ping message
					log.severe(CLASS_NAME, methodName, "642",
							new Object[] { Long.valueOf(keepAlive), Long.valueOf(lastOutboundActivity),
									Long.valueOf(lastInboundActivity), Long.valueOf(time), Long.valueOf(lastPing) });

					// A ping has not been sent but I am not progressing on the current write
					// operation.
					// At this point, assume that the broker has hung and the TCP layer hasn't
					// noticed.
					throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_WRITE_TIMEOUT);
				}

				// 1. Is a ping required by the client to verify whether the broker is down?
				// Condition: ((pingOutstanding == 0 && (time - lastInboundActivity >= keepAlive
				// + delta)))
				// In this case only one ping is sent. If not confirmed, client will assume a
				// lost connection to the broker.
				// 2. Is a ping required by the broker to keep the client alive?
				// Condition: (time - lastOutboundActivity >= keepAlive - delta)
				// In this case more than one ping outstanding may be necessary.
				// This would be the case when receiving a large message;
				// the broker needs to keep receiving a regular ping even if the ping response
				// are queued after the long message
				// If lacking to do so, the broker will consider my connection lost and cut my
				// socket.
				if ((pingOutstanding == 0 && (time - lastInboundActivity >= keepAlive - delta))
						|| (time - lastOutboundActivity >= keepAlive - delta)) {

					// @TRACE 620=ping needed. keepAlive={0} lastOutboundActivity={1}
					// lastInboundActivity={2}
					log.fine(CLASS_NAME, methodName, "620", new Object[] { Long.valueOf(keepAlive),
							Long.valueOf(lastOutboundActivity), Long.valueOf(lastInboundActivity) });

					// pingOutstanding++; // it will be set after the ping has been written on the
					// wire
					// lastPing = time; // it will be set after the ping has been written on the
					// wire
					token = new MqttToken(clientComms.getClient().getClientId());
					if (pingCallback != null) {
						token.setActionCallback(pingCallback);
					}
					tokenStore.saveToken(token, pingCommand);
					pendingFlows.insertElementAt(pingCommand, 0);

					nextPingTime = keepAlive;

					// Wake sender thread since it may be in wait state (in ClientState.get())
					notifyQueueLock();
				} else {
					log.fine(CLASS_NAME, methodName, "634", null);
					nextPingTime = Math.max(1, keepAlive - (time - lastOutboundActivity));
				}
			}
			// @TRACE 624=Schedule next ping at {0}
			log.fine(CLASS_NAME, methodName, "624", new Object[] { Long.valueOf(nextPingTime) });
			pingSender.schedule(TimeUnit.NANOSECONDS.toMillis(nextPingTime));
		}

		return token;
	}

	/**
	 * This returns the next piece of work, ie message, for the CommsSender to send
	 * over the network. Calls to this method block until either: - there is a
	 * message to be sent - the keepAlive interval is exceeded, which triggers a
	 * ping message to be returned - {@link ClientState#disconnected(MqttException)}
	 * is called
	 * 
	 * @return the next message to send, or null if the client is disconnected
	 * @throws MqttException
	 *             if an exception occurs whilst returning the next piece of work
	 */
	protected MqttWireMessage get() throws MqttException {
		final String methodName = "get";
		MqttWireMessage result = null;

		synchronized (queueLock) {
			while (result == null) {

				// If there is no work wait until there is work.
				// If the inflight window is full and no flows are pending wait until space is
				// freed.
				// In both cases queueLock will be notified.
				if ((pendingMessages.isEmpty() && pendingFlows.isEmpty())
						|| (pendingFlows.isEmpty() && actualInFlight >= this.mqttConnection.getReceiveMaximum())) {
					try {
						// @TRACE 644=wait for new work or for space in the inflight window
						log.fine(CLASS_NAME, methodName, "644");

						queueLock.wait();

						// @TRACE 647=new work or ping arrived
						log.fine(CLASS_NAME, methodName, "647");
					} catch (InterruptedException e) {
					}
				}

				// Handle the case where not connected. This should only be the case if:
				// - in the process of disconnecting / shutting down
				// - in the process of connecting
				if (pendingFlows == null || (!connected && (pendingFlows.isEmpty()
						|| !((MqttWireMessage) pendingFlows.elementAt(0) instanceof MqttConnect)))) {
					// @TRACE 621=no outstanding flows and not connected
					log.fine(CLASS_NAME, methodName, "621");

					return null;
				}

				// Check if there is a need to send a ping to keep the session alive.
				// Note this check is done before processing messages. If not done first
				// an app that only publishes QoS 0 messages will prevent keepalive processing
				// from functioning.
				// checkForActivity(); //Use pinger, don't check here

				// Now process any queued flows or messages
				if (!pendingFlows.isEmpty()) {
					// Process the first "flow" in the queue
					result = (MqttWireMessage) pendingFlows.remove(0);
					if (result instanceof MqttPubRel) {
						inFlightPubRels++;

						// @TRACE 617=+1 inflightpubrels={0}
						log.fine(CLASS_NAME, methodName, "617", new Object[] { Integer.valueOf(inFlightPubRels) });
					}

					checkQuiesceLock();
				} else if (!pendingMessages.isEmpty()) {

					// If the inflight window is full then messages are not
					// processed until the inflight window has space.
					if (actualInFlight < this.mqttConnection.getReceiveMaximum()) {
						// The in flight window is not full so process the
						// first message in the queue
						result = (MqttWireMessage) pendingMessages.elementAt(0);
						pendingMessages.removeElementAt(0);
						actualInFlight++;

						// @TRACE 623=+1 actualInFlight={0}
						log.fine(CLASS_NAME, methodName, "623", new Object[] { Integer.valueOf(actualInFlight) });
					} else {
						// @TRACE 622=inflight window full
						log.fine(CLASS_NAME, methodName, "622");
					}
				}
			} // end while
		} // synchronized
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#notifySentBytes(int)
	 */
	@Override
	public void notifySentBytes(int sentBytesCount) {
		final String methodName = "notifySentBytes";
		if (sentBytesCount > 0) {
			this.lastOutboundActivity = System.nanoTime();
		}
		// @TRACE 643=sent bytes count={0}
		log.fine(CLASS_NAME, methodName, "643", new Object[] { Integer.valueOf(sentBytesCount) });
	}

	/**
	 * Called by the CommsSender when a message has been sent
	 * 
	 * @param message
	 *            the {@link MqttWireMessage} to notify
	 */
	protected void notifySent(MqttWireMessage message) {
		final String methodName = "notifySent";

		this.lastOutboundActivity = System.nanoTime();
		// @TRACE 625=key={0}
		log.fine(CLASS_NAME, methodName, "625", new Object[] { message.getKey() });

		MqttToken token = tokenStore.getToken(message);
		if (token == null) return;
		token.internalTok.notifySent();
		if (message instanceof MqttPingReq) {
			synchronized (pingOutstandingLock) {
				long time = System.nanoTime();
				synchronized (pingOutstandingLock) {
					lastPing = time;
					pingOutstanding++;
				}
				// @TRACE 635=ping sent. pingOutstanding: {0}
				log.fine(CLASS_NAME, methodName, "635", new Object[] { Integer.valueOf(pingOutstanding) });
			}
		} else if (message instanceof MqttPublish) {
			if (((MqttPublish) message).getMessage().getQos() == 0) {
				// once a QoS 0 message is sent we can clean up its records straight away as
				// we won't be hearing about it again
				token.internalTok.markComplete(null, null);
				callback.asyncOperationComplete(token);
				decrementInFlight();
				releaseMessageId(message.getMessageId());
				tokenStore.removeToken(message);
				checkQuiesceLock();
			}
		}
	}

	private void decrementInFlight() {
		final String methodName = "decrementInFlight";
		synchronized (queueLock) {
			actualInFlight--;
			// @TRACE 646=-1 actualInFlight={0}
			log.fine(CLASS_NAME, methodName, "646", new Object[] { Integer.valueOf(actualInFlight) });

			if (!checkQuiesceLock()) {
				queueLock.notifyAll();
			}
		}
	}

	protected boolean checkQuiesceLock() {
		final String methodName = "checkQuiesceLock";
		// if (quiescing && actualInFlight == 0 && pendingFlows.size() == 0 &&
		// inFlightPubRels == 0 && callback.isQuiesced()) {
		int tokC = tokenStore.count();
		if (quiescing && tokC == 0 && pendingFlows.size() == 0 && callback.isQuiesced()) {
			// @TRACE 626=quiescing={0} actualInFlight={1} pendingFlows={2}
			// inFlightPubRels={3} callbackQuiesce={4} tokens={5}
			log.fine(CLASS_NAME, methodName, "626",
					new Object[] { Boolean.valueOf(quiescing), Integer.valueOf(actualInFlight),
							Integer.valueOf(pendingFlows.size()), Integer.valueOf(inFlightPubRels),
							Boolean.valueOf(callback.isQuiesced()), Integer.valueOf(tokC) });
			synchronized (quiesceLock) {
				quiesceLock.notifyAll();
			}
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.internal.MqttState#notifyReceivedBytes(int)
	 */
	@Override
	public void notifyReceivedBytes(int receivedBytesCount) {
		final String methodName = "notifyReceivedBytes";
		if (receivedBytesCount > 0) {
			this.lastInboundActivity = System.nanoTime();
		}
		// @TRACE 630=received bytes count={0}
		log.fine(CLASS_NAME, methodName, "630", new Object[] { Integer.valueOf(receivedBytesCount) });
	}

	/**
	 * Called by the CommsReceiver when an ack has arrived.
	 * 
	 * @param ack
	 *            The {@link MqttAck} that has arrived
	 * @throws MqttException
	 *             if an exception occurs when sending / notifying
	 */
	protected void notifyReceivedAck(MqttAck ack) throws MqttException {
		final String methodName = "notifyReceivedAck";
		this.lastInboundActivity = System.nanoTime();

		// @TRACE 627=received key={0} message={1}
		log.fine(CLASS_NAME, methodName, "627", new Object[] { Integer.valueOf(ack.getMessageId()), ack });

		MqttToken token = tokenStore.getToken(ack);
		MqttException mex = null;

		if (token == null) {
			// @TRACE 662=no message found for ack id={0}
			log.fine(CLASS_NAME, methodName, "662", new Object[] { Integer.valueOf(ack.getMessageId()) });
		} else if (ack instanceof MqttPubRec) {
			if (((MqttPubRec) ack).getReasonCodes()[0] > MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR) {
				// @TRACE 664=[MQTT-4.3.3-4] - A Reason code greater than 0x80 (128) was
				// received in an incoming PUBREC id={0} rc={1} message={2}, halting QoS 2 flow.
				log.severe(CLASS_NAME, methodName, "664",
						new Object[] { ack.getMessageId(), ack.getReasonCodes()[0], ack.toString() });
				throw new MqttException(((MqttPubRec) ack).getReasonCodes()[0]);
			}

			// Update the token with the reason codes
			updateResult(ack, token, mex);

			// Complete the QoS 2 flow. Unlike all other
			// flows, QoS is a 2 phase flow. The second phase sends a
			// PUBREL - the operation is not complete until a PUBCOMP
			// is received
			// Currently this client has no need of the properties, so this is left empty.
			MqttPubRel rel = new MqttPubRel(MqttReturnCode.RETURN_CODE_SUCCESS, ack.getMessageId(),
					new MqttProperties());
			this.send(rel, token);
		} else if (ack instanceof MqttPubAck || ack instanceof MqttPubComp) {

			// QoS 1 & 2 notify users of result before removing from
			// persistence
			notifyResult(ack, token, mex);
			// Do not remove publish / delivery token at this stage
			// do this when the persistence is removed later
		} else if (ack instanceof MqttPingResp) {

			synchronized (pingOutstandingLock) {
				pingOutstanding = Math.max(0, pingOutstanding - 1);
				notifyResult(ack, token, mex);
				if (pingOutstanding == 0) {
					tokenStore.removeToken(ack);
				}
			}
			// @TRACE 636=ping response received. pingOutstanding: {0}
			log.fine(CLASS_NAME, methodName, "636", new Object[] { Integer.valueOf(pingOutstanding) });
		} else if (ack instanceof MqttConnAck) {

			int rc = ((MqttConnAck) ack).getReturnCode();
			if (rc == 0) {
				synchronized (queueLock) {
					if (cleanStart) {
						clearState();
						// Add the connect token back in so that users can be
						// notified when connect completes.
						tokenStore.saveToken(token, ack);
					}
					inFlightPubRels = 0;
					actualInFlight = 0;
					restoreInflightMessages();
					connected();
				}
			} else {

				mex = ExceptionHelper.createMqttException(rc);
				throw mex;
			}

			clientComms.connectComplete((MqttConnAck) ack, mex);
			notifyResult(ack, token, mex);
			tokenStore.removeToken(ack);

			// Notify the sender thread that there maybe work for it to do now
			synchronized (queueLock) {
				queueLock.notifyAll();
			}
		} else {
			notifyResult(ack, token, mex);
			releaseMessageId(ack.getMessageId());
			tokenStore.removeToken(ack);
		}

		checkQuiesceLock();
	}

	/**
	 * Called by the CommsReceiver when an Ack has been received but cannot be
	 * matched to a token. This method will generate the appropriate response with
	 * an error code.
	 * 
	 * @param ack
	 *            - The Orphaned Ack
	 * @throws MqttException
	 *             if an exception occurs whilst handling orphaned Acks
	 */
	protected void handleOrphanedAcks(MqttAck ack) throws MqttException {
		final String methodName = "handleOrphanedAcks";
		// @TRACE 666=Orphaned Ack key={0} message={1}
		log.fine(CLASS_NAME, methodName, "666", new Object[] { Integer.valueOf(ack.getMessageId()), ack });

		if (ack instanceof MqttPubAck) {
			// MqttPubAck - This would be the end of a QoS 1 flow, so message can be ignored
		} else if (ack instanceof MqttPubRec) {
			// MqttPubRec - Send an MqttPubRel with the appropriate Reason Code
			MqttProperties pubRelProperties = new MqttProperties();
			if (this.mqttConnection.isSendReasonMessages()) {
				String reasonString = String.format("Message identifier [%d] was not found. Discontinuing QoS 2 flow.",
						ack.getMessageId());
				pubRelProperties.setReasonString(reasonString);
			}
			MqttPubRel rel = new MqttPubRel(MqttReturnCode.RETURN_CODE_PACKET_ID_NOT_FOUND, ack.getMessageId(),
					new MqttProperties());
			this.send(rel, null);
		} else if (ack instanceof MqttPubComp) {
			// MqttPubComp
		}
	}

	/**
	 * Called by {@link ClientState#notifyReceivedMsg} when a PUBREL message is
	 * received.
	 * 
	 * @param pubRel
	 *            The in-bound PUBREL
	 * @throws MqttException
	 *             When an exception occurs whilst handling the PUBREL
	 */
	protected void handleInboundPubRel(MqttPubRel pubRel) throws MqttException {
		final String methodName = "handleInboundPubRel";
		if (pubRel.getReasonCodes()[0] > MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR) {
			// @TRACE 667=MqttPubRel was received with an error code: key={0} message={1},
			// Reason Code={2}
			log.severe(CLASS_NAME, methodName, "667",
					new Object[] { pubRel.getMessageId(), pubRel.toString(), pubRel.getReasonCodes()[0] });
			throw new MqttException(pubRel.getReasonCodes()[0]);
		} else {
			// Currently this client has no need of the properties, so this is left empty.
			MqttPubComp pubComp = new MqttPubComp(MqttReturnCode.RETURN_CODE_SUCCESS, pubRel.getMessageId(),
					new MqttProperties());
			// @TRACE 668=Creating MqttPubComp: {0}
			log.info(CLASS_NAME, methodName, "668", new Object[] { pubComp.toString() });
			this.send(pubComp, null);
		}
	}

	/**
	 * Called by the CommsReceiver when a message has been received. Handles inbound
	 * messages and other flows such as PUBREL.
	 * 
	 * @param message
	 *            The {@link MqttWireMessage} that has been received
	 * @throws MqttException
	 *             when an exception occurs whilst notifying
	 */
	protected void notifyReceivedMsg(MqttWireMessage message) throws MqttException {
		final String methodName = "notifyReceivedMsg";
		this.lastInboundActivity = System.nanoTime();

		// @TRACE 651=received key={0} message={1}
		log.fine(CLASS_NAME, methodName, "651", new Object[] { Integer.valueOf(message.getMessageId()), message });

		if (!quiescing) {
			if (message instanceof MqttPublish) {
				MqttPublish send = (MqttPublish) message;

				// Do we have an incoming topic Alias?
				if (send.getProperties().getTopicAlias() != null) {
					int incomingTopicAlias = send.getProperties().getTopicAlias();

					// Are incoming Topic Aliases enabled / is it a valid Alias?
					if (incomingTopicAlias > this.mqttConnection.getIncomingTopicAliasMax()
							|| incomingTopicAlias == 0) {
						// @TRACE 653=Invalid Topic Alias: topicAliasMax={0}, publishTopicAlias={1}
						log.severe(CLASS_NAME, methodName, "653",
								new Object[] { Integer.valueOf(this.mqttConnection.getIncomingTopicAliasMax()),
										Integer.valueOf(incomingTopicAlias) });
						if (callback != null) {
							callback.mqttErrorOccurred(new MqttException(MqttException.REASON_CODE_INVALID_TOPIC_ALAS));
						}
						throw new MqttException(MqttClientException.REASON_CODE_INVALID_TOPIC_ALAS);

					}

					// Is this alias being sent with a topic string?
					if (send.getTopicName() != null) {
						// @TRACE 652=Setting Incoming New Topic Alias alias={0}, topicName={1}
						log.fine(CLASS_NAME, methodName, "652", new Object[] {
								Integer.valueOf(send.getProperties().getTopicAlias()), send.getTopicName() });
						incomingTopicAliases.put(send.getProperties().getTopicAlias(), send.getTopicName());
					} else {
						// No Topic String, so must be in incomingTopicAliases.
						if (incomingTopicAliases.contains(incomingTopicAlias)) {
							send.setTopicName(incomingTopicAliases.get(incomingTopicAlias));
						} else {
							// @TRACE 654=Unknown Topic Alias: Incoming Alias={1}
							log.severe(CLASS_NAME, methodName, "654",
									new Object[] { Integer.valueOf(send.getProperties().getTopicAlias()) });
							throw new MqttException(MqttClientException.REASON_CODE_UNKNOWN_TOPIC_ALIAS);
						}
					}
				}

				switch (send.getMessage().getQos()) {
				case 0:
				case 1:
					if (callback != null) {
						callback.messageArrived(send);
					}
					break;
				case 2:
					persistence.put(getReceivedPersistenceKey(message), (MqttPublish) message);
					inboundQoS2.put(Integer.valueOf(send.getMessageId()), send);
					if (callback != null) {
						callback.messageArrived(send);
					}
					// Currently this client has no need of the properties, so this is left empty.
					this.send(new MqttPubRec(MqttReturnCode.RETURN_CODE_SUCCESS, send.getMessageId(),
							new MqttProperties()), null);
					break;

				default:
					// should NOT reach here
				}
			} else if (message instanceof MqttPubRel) {
				handleInboundPubRel((MqttPubRel) message);
			} else if (message instanceof MqttAuth) {
				MqttAuth authMsg = (MqttAuth) message;
				callback.authMessageReceived(authMsg);
			}
		}
	}

	/**
	 * Called when waiters and callbacks have processed the message. For messages
	 * where delivery is complete the message can be removed from persistence and
	 * counters adjusted accordingly. Also tidy up by removing token from store...
	 * 
	 * @param token
	 *            The {@link MqttToken} that will be used to notify
	 * @throws MqttException
	 *             if an exception occurs during notification
	 */
	protected void notifyComplete(MqttToken token) throws MqttException {

		final String methodName = "notifyComplete";

		MqttWireMessage message = token.internalTok.getWireMessage();

		if (message != null && message instanceof MqttAck) {

			// @TRACE 629=received key={0} token={1} message={2}
			log.fine(CLASS_NAME, methodName, "629",
					new Object[] { Integer.valueOf(message.getMessageId()), token, message });

			MqttAck ack = (MqttAck) message;

			if (ack instanceof MqttPubAck) {

				// QoS 1 - user notified now remove from persistence...
				persistence.remove(getSendPersistenceKey(message));
				persistence.remove(getSendBufferedPersistenceKey(message));
				outboundQoS1.remove(Integer.valueOf(ack.getMessageId()));
				decrementInFlight();
				releaseMessageId(message.getMessageId());
				tokenStore.removeToken(message);
				// @TRACE 650=removed Qos 1 publish. key={0}
				log.fine(CLASS_NAME, methodName, "650", new Object[] { Integer.valueOf(ack.getMessageId()) });
			} else if (ack instanceof MqttPubComp) {
				// QoS 2 - user notified now remove from persistence...
				persistence.remove(getSendPersistenceKey(message));
				persistence.remove(getSendConfirmPersistenceKey(message));
				persistence.remove(getSendBufferedPersistenceKey(message));
				outboundQoS2.remove(Integer.valueOf(ack.getMessageId()));

				inFlightPubRels--;
				decrementInFlight();
				releaseMessageId(message.getMessageId());
				tokenStore.removeToken(message);

				// @TRACE 645=removed QoS 2 publish/pubrel. key={0}, -1 inFlightPubRels={1}
				log.fine(CLASS_NAME, methodName, "645",
						new Object[] { Integer.valueOf(ack.getMessageId()), Integer.valueOf(inFlightPubRels) });
			}

			checkQuiesceLock();
		}
	}

	/**
	 * Updates a token with the latest reason codes, currently only used for PubRec
	 * messages.
	 * 
	 * @param ack
	 *            - The message that we are using for the update
	 * @param token
	 *            - The Token we are updating
	 * @param ex
	 *            - if there was a problem store the exception in the token.
	 */
	protected void updateResult(MqttWireMessage ack, MqttToken token, MqttException ex) {
		token.internalTok.update(ack, ex);
	}

	protected void notifyResult(MqttWireMessage ack, MqttToken token, MqttException ex) {
		final String methodName = "notifyResult";
		// unblock any threads waiting on the token
		token.internalTok.markComplete(ack, ex);
		token.internalTok.notifyComplete();

		// Let the user know an async operation has completed and then remove the token
		if (ack != null && ack instanceof MqttAck && !(ack instanceof MqttPubRec)) {
			// @TRACE 648=key{0}, msg={1}, excep={2}
			log.fine(CLASS_NAME, methodName, "648", new Object[] { token.internalTok.getKey(), ack, ex });
			callback.asyncOperationComplete(token);
		}
		// There are cases where there is no ack as the operation failed before
		// an ack was received
		if (ack == null) {
			// @TRACE 649=key={0},excep={1}
			log.fine(CLASS_NAME, methodName, "649", new Object[] { token.internalTok.getKey(), ex });
			callback.asyncOperationComplete(token);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#connected()
	 */
	@Override
	public void connected() {
		final String methodName = "connected";
		// @TRACE 631=connected
		log.fine(CLASS_NAME, methodName, "631");
		this.connected = true;

		pingSender.start(); // Start ping thread when client connected to server.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#resolveOldTokens(org.
	 * eclipse.paho.client.mqttv3.MqttException)
	 */
	@Override
	public Vector<MqttToken> resolveOldTokens(MqttException reason) {
		final String methodName = "resolveOldTokens";
		// @TRACE 632=reason {0}
		log.fine(CLASS_NAME, methodName, "632", new Object[] { reason });

		// If any outstanding let the user know the reason why it is still
		// outstanding by putting the reason shutdown is occurring into the
		// token.
		MqttException shutReason = reason;
		if (reason == null) {
			shutReason = new MqttException(MqttClientException.REASON_CODE_CLIENT_DISCONNECTING);
		}

		// Set the token up so it is ready to be notified after disconnect
		// processing has completed. Do not
		// remove the token from the store if it is a delivery token, it is
		// valid after a reconnect.
		Vector<MqttToken> outT = tokenStore.getOutstandingTokens();
		Enumeration<MqttToken> outTE = outT.elements();
		while (outTE.hasMoreElements()) {
			MqttToken tok = (MqttToken) outTE.nextElement();
			synchronized (tok) {
				if (!tok.isComplete() && !tok.internalTok.isCompletePending() && tok.getException() == null) {
					tok.internalTok.setException(shutReason);
				}
			}
			if (!(tok instanceof MqttDeliveryToken)) {
				// If not a delivery token it is not valid on
				// restart so remove
				tokenStore.removeToken(tok.internalTok.getKey());
			}
		}
		return outT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.internal.MqttState#disconnected(org.eclipse.
	 * paho.client.mqttv3.MqttException)
	 */
	@Override
	public void disconnected(MqttException reason) {
		final String methodName = "disconnected";
		// @TRACE 633=disconnected
		log.fine(CLASS_NAME, methodName, "633", new Object[] { reason });

		this.connected = false;

		try {
			if (cleanStart) {
				clearState();
			}

			clearConnectionState();

			pendingMessages.clear();
			pendingFlows.clear();
			synchronized (pingOutstandingLock) {
				// Reset pingOutstanding to allow reconnects to assume no previous ping.
				pingOutstanding = 0;
			}
		} catch (MqttException e) {
			// Ignore as we have disconnected at this point
		}
	}

	/**
	 * Releases a message ID back into the pool of available message IDs. If the
	 * supplied message ID is not in use, then nothing will happen.
	 * 
	 * @param msgId
	 *            A message ID that can be freed up for re-use.
	 */
	private synchronized void releaseMessageId(int msgId) {
		inUseMsgIds.remove(Integer.valueOf(msgId));
	}

	/**
	 * Get the next MQTT message ID that is not already in use, and marks it as now
	 * being in use.
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
			if (nextMsgId > MAX_MSG_ID) {
				nextMsgId = MIN_MSG_ID;
			}
			if (nextMsgId == startingMessageId) {
				loopCount++;
				if (loopCount == 2) {
					throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE);
				}
			}
		} while (inUseMsgIds.containsKey(Integer.valueOf(nextMsgId)));
		Integer id = Integer.valueOf(nextMsgId);
		inUseMsgIds.put(id, id);
		return nextMsgId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#quiesce(long)
	 */
	@Override
	public void quiesce(long timeout) {
		final String methodName = "quiesce";
		// If the timeout is greater than zero t
		if (timeout > 0) {
			// @TRACE 637=timeout={0}
			log.fine(CLASS_NAME, methodName, "637", new Object[] { Long.valueOf(timeout) });
			synchronized (queueLock) {
				this.quiescing = true;
			}
			// We don't want to handle any new inbound messages
			callback.quiesce();
			notifyQueueLock();

			synchronized (quiesceLock) {
				try {
					// If token count is not zero there is outbound work to process and
					// if pending flows is not zero there is outstanding work to complete and
					// if call back is not quiseced there it needs to complete.
					int tokc = tokenStore.count();
					if (tokc > 0 || pendingFlows.size() > 0 || !callback.isQuiesced()) {
						// @TRACE 639=wait for outstanding: actualInFlight={0} pendingFlows={1}
						// inFlightPubRels={2} tokens={3}
						log.fine(CLASS_NAME, methodName, "639",
								new Object[] { Integer.valueOf(actualInFlight), Integer.valueOf(pendingFlows.size()),
										Integer.valueOf(inFlightPubRels), Integer.valueOf(tokc) });

						// wait for outstanding in flight messages to complete and
						// any pending flows to complete
						quiesceLock.wait(timeout);
					}
				} catch (InterruptedException ex) {
					// Don't care, as we're shutting down anyway
				}
			}

			// Quiesce time up or inflight messages delivered. Ensure pending delivery
			// vectors are cleared ready for disconnect to be sent as the final flow.
			synchronized (queueLock) {
				pendingMessages.clear();
				pendingFlows.clear();
				quiescing = false;
				actualInFlight = 0;
			}
			// @TRACE 640=finished
			log.fine(CLASS_NAME, methodName, "640");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#notifyQueueLock()
	 */
	@Override
	public void notifyQueueLock() {
		final String methodName = "notifyQueueLock";
		synchronized (queueLock) {
			// @TRACE 638=notifying queueLock holders
			log.fine(CLASS_NAME, methodName, "638");
			queueLock.notifyAll();
		}
	}

	protected void deliveryComplete(MqttPublish message) throws MqttPersistenceException {
		final String methodName = "deliveryComplete";

		// @TRACE 641=remove publish from persistence. key={0}
		log.fine(CLASS_NAME, methodName, "641", new Object[] { Integer.valueOf(message.getMessageId()) });

		persistence.remove(getReceivedPersistenceKey(message));
		inboundQoS2.remove(Integer.valueOf(message.getMessageId()));
	}

	protected void deliveryComplete(int messageId) throws MqttPersistenceException {
		final String methodName = "deliveryComplete";

		// @TRACE 641=remove publish from persistence. key={0}
		log.fine(CLASS_NAME, methodName, "641", new Object[] { Integer.valueOf(messageId) });

		persistence.remove(getReceivedPersistenceKey(messageId));
		inboundQoS2.remove(Integer.valueOf(messageId));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#getActualInFlight()
	 */
	@Override
	public int getActualInFlight() {
		return actualInFlight;
	}
	
	public Long getOutgoingMaximumPacketSize() {
		return this.mqttConnection.getIncomingMaximumPacketSize();
	}
	
	public Long getIncomingMaximumPacketSize() {
		return this.mqttConnection.getOutgoingMaximumPacketSize();
	}

	/**
	 * Tidy up - ensure that tokens are released as they are maintained over a
	 * disconnect / connect cycle.
	 */
	protected void close() {
		inUseMsgIds.clear();
		if (pendingMessages != null) {
			pendingMessages.clear();
		}
		pendingFlows.clear();
		outboundQoS2.clear();
		outboundQoS1.clear();
		outboundQoS0.clear();
		inboundQoS2.clear();
		tokenStore.clear();
		inUseMsgIds = null;
		pendingMessages = null;
		pendingFlows = null;
		outboundQoS2 = null;
		outboundQoS1 = null;
		outboundQoS0 = null;
		inboundQoS2 = null;
		tokenStore = null;
		callback = null;
		clientComms = null;
		persistence = null;
		pingCommand = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.internal.MqttState#getDebug()
	 */
	@Override
	public Properties getDebug() {
		Properties props = new Properties();
		props.put("In use msgids", inUseMsgIds);
		props.put("pendingMessages", pendingMessages);
		props.put("pendingFlows", pendingFlows);
		props.put("serverReceiveMaximum", Integer.valueOf(this.mqttConnection.getReceiveMaximum()));
		props.put("nextMsgID", Integer.valueOf(nextMsgId));
		props.put("actualInFlight", Integer.valueOf(actualInFlight));
		props.put("inFlightPubRels", Integer.valueOf(inFlightPubRels));
		props.put("quiescing", Boolean.valueOf(quiescing));
		props.put("pingoutstanding", Integer.valueOf(pingOutstanding));
		props.put("lastOutboundActivity", Long.valueOf(lastOutboundActivity));
		props.put("lastInboundActivity", Long.valueOf(lastInboundActivity));
		props.put("outboundQoS2", outboundQoS2);
		props.put("outboundQoS1", outboundQoS1);
		props.put("outboundQoS0", outboundQoS0);
		props.put("inboundQoS2", inboundQoS2);
		props.put("tokens", tokenStore);
		return props;
	}
}
