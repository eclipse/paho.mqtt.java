package org.eclipse.paho.mqttv5.client.vertx.internal;

import java.io.EOFException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.mqttv5.client.vertx.logging.Logger;
import org.eclipse.paho.mqttv5.client.vertx.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.client.vertx.persist.PersistedBuffer;
import org.eclipse.paho.mqttv5.client.vertx.MqttClientException;
import org.eclipse.paho.mqttv5.client.vertx.MqttToken;
import org.eclipse.paho.mqttv5.client.vertx.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.vertx.MqttDeliveryToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistable;
import org.eclipse.paho.mqttv5.common.packet.MqttAck;
import org.eclipse.paho.mqttv5.common.packet.MqttConnect;
import org.eclipse.paho.mqttv5.common.packet.MqttPingReq;
import org.eclipse.paho.mqttv5.common.packet.MqttPubComp;
import org.eclipse.paho.mqttv5.common.packet.MqttPubRel;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttPersistableWireMessage;

public class ClientState {
	private static final String CLASS_NAME = ClientState.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);
	private static final String PERSISTENCE_SENT_PREFIX = "s-";
	private static final String PERSISTENCE_SENT_BUFFERED_PREFIX = "sb-";
	private static final String PERSISTENCE_CONFIRMED_PREFIX = "sc-";
	private static final String PERSISTENCE_RECEIVED_PREFIX = "r-";
	
	private MqttClientPersistence persistence;
	
	// We should use Vert.x comms for queues
	private PersistedBuffer outboundQoS2 = null;
	private PersistedBuffer outboundQoS1 = null;
	private PersistedBuffer outboundQoS0 = null;
	private PersistedBuffer inboundQoS2 = null;
	private PersistedBuffer retryQueue;
	
	private static final int MIN_MSG_ID = 1; // Lowest possible MQTT message ID to use
	private static final int MAX_MSG_ID = 65535; // Highest possible MQTT message ID to use
	private int nextMsgId = MIN_MSG_ID - 1; // The next available message ID to use
	private ConcurrentHashMap<Integer, Integer> inUseMsgIds; // Used to store a set of in-use message IDs
	
	// Variables that exist within the life of an MQTT session
	private MqttSessionState sessionstate;
	
	// Variables that exist within the life of an MQTT connection.
	private MqttConnectionState connectionstate = new MqttConnectionState(); 
	
	public ClientState() {
		inUseMsgIds = new ConcurrentHashMap<>();
		/*outboundQoS2 = new PersistedBuffer(persistence);
		outboundQoS1 = new PersistedBuffer(persistence);
		outboundQoS0 = new PersistedBuffer(persistence);
		inboundQoS2 = new PersistedBuffer(persistence);*/
		
		sessionstate = new MqttSessionState(persistence);
		retryQueue = new PersistedBuffer(persistence);
	}
	
	/**
	 * Restores the state information from persistence.
	 * 
	 * @throws MqttException
	 *             if an exception occurs whilst restoring state
	 */
	protected void restore() throws MqttException {
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
					inboundQoS2.restore(Integer.valueOf(message.getMessageId()), message);
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

							outboundQoS2.restore(Integer.valueOf(confirmMessage.getMessageId()), confirmMessage);
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

							outboundQoS2.restore(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
						} else {
							// @TRACE 608=outbound QoS 1 publish key={0} message={1}
							log.fine(CLASS_NAME, methodName, "608", new Object[] { key, message });

							outboundQoS1.restore(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
						}
					}
					//MqttDeliveryToken tok = tokenStore.restoreToken(sendMessage);
					//tok.internalTok.setClient(clientComms.getClient());
					inUseMsgIds.put(Integer.valueOf(sendMessage.getMessageId()),
							Integer.valueOf(sendMessage.getMessageId()));
				} else if (key.startsWith(PERSISTENCE_SENT_BUFFERED_PREFIX)) {
					// Buffered outgoing messages that have not yet been sent at all
					MqttPublish sendMessage = (MqttPublish) message;
					highestMsgId = Math.max(sendMessage.getMessageId(), highestMsgId);
					if (sendMessage.getMessage().getQos() == 2) {
						// @TRACE 607=outbound QoS 2 publish key={0} message={1}
						log.fine(CLASS_NAME, methodName, "607", new Object[] { key, message });
						outboundQoS2.restore(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
					} else if (sendMessage.getMessage().getQos() == 1) {
						// @TRACE 608=outbound QoS 1 publish key={0} message={1}
						log.fine(CLASS_NAME, methodName, "608", new Object[] { key, message });

						outboundQoS1.restore(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
					} else {
						// @TRACE 511=outbound QoS 0 publish key={0} message={1}
						log.fine(CLASS_NAME, methodName, "511", new Object[] { key, message });
						outboundQoS0.restore(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
						// Because there is no Puback, we have to trust that this is enough to send the message
						persistence.remove(key);
					}

					//MqttDeliveryToken tok = tokenStore.restoreToken(sendMessage);
					//tok.internalTok.setClient(clientComms.getClient());
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
	
	/* Queue up a message to send.
	 */
	public void queue(MqttPersistableWireMessage message) throws MqttException {
		final String methodName = "send";

		if (message instanceof MqttPublish) {

			MqttMessage innerMessage = ((MqttPublish) message).getMessage();
			// @TRACE 628=pending publish key={0} qos={1} message={2}
			log.fine(CLASS_NAME, methodName, "628", new Object[] { Integer.valueOf(message.getMessageId()),
					Integer.valueOf(innerMessage.getQos()), message });

			switch (innerMessage.getQos()) {
			case 2:
				outboundQoS2.add(Integer.valueOf(message.getMessageId()), message, PERSISTENCE_SENT_PREFIX);
				break;
			case 1:
				outboundQoS1.add(Integer.valueOf(message.getMessageId()), message, PERSISTENCE_SENT_PREFIX);
				break;
			}

		} else {
			// @TRACE 615=pending send key={0} message {1}
			log.fine(CLASS_NAME, methodName, "615", new Object[] { Integer.valueOf(message.getMessageId()), message });

			if (message instanceof MqttPubRel) {
				outboundQoS2.add(Integer.valueOf(message.getMessageId()), (MqttPubRel) message,
						PERSISTENCE_CONFIRMED_PREFIX);

				// persistence.put(getSendConfirmPersistenceKey(message), (MqttPubRel) message);
			} else if (message instanceof MqttPubComp) {
				persistence.remove(getReceivedPersistenceKey(message));
			}
		}
	}

}
