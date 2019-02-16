package org.eclipse.paho.mqttv5.client.vertx.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.paho.mqttv5.client.vertx.MqttClientException;
import org.eclipse.paho.mqttv5.client.vertx.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.vertx.MqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.packet.MqttPersistableWireMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttSubscribe;
import org.eclipse.paho.mqttv5.common.packet.MqttUnsubscribe;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;

/* A queue of outstanding work, which is persistent and uses the Vert.x bus 
 * 
 */

public class ToDoQueue {
	
	private static final String PERSISTENCE_SENT_BUFFERED_PREFIX = "sb-";
	
	private MqttClientPersistence persistence;
	private EventBus eb;
	private String ebtopic = "paho.mqtt.java.toDoQueue";
	private MessageConsumer<String> consumer;
	private ClientInternal internal;
	//private MessageCodec codec = new Codec();
	private ConnectionState connectionstate;
	private int queued = 0;
	private boolean remove1message = false;
	
	private int sequence_no = 0; // used when restoring messages, for ordering
	// from sequence number to message
	private HashMap<Integer, MqttWireMessage> messages = new HashMap<Integer, MqttWireMessage>();
	
	public ToDoQueue(ClientInternal internal, Vertx vertx, MqttClientPersistence persistence,
			ConnectionState connectionstate) {
		this.eb = vertx.eventBus();
		this.persistence = persistence;
		this.internal = internal;
		this.connectionstate = connectionstate;
		
		/*try {
			eb.registerCodec(codec);
		} catch (IllegalStateException e) {
			// already registered
		}*/
		ebtopic += "."+this.hashCode(); // ensure this topic is unique for this queue
		consumer = eb.consumer(ebtopic);
		setSize(internal.getClient().getBufferOpts().getBufferSize());
		consumer.pause();
		consumer.handler(message -> { handle(message); });
	}
	
	public void clear() {
		messages.clear();
		sequence_no = 0;
		queued = 0;
		//remove1message = false;
	}
	
	public int getQueued() {
		return queued;
	}
	
	public void setSize(int size) {
		// +1 because of the way we delete oldest messages, if that option is set
		consumer.setMaxBufferedMessages(size + 1);
	}
	
	public void close() {
		consumer.unregister(/*res -> {
				  if (res.succeeded()) {
					    System.out.println("The handler un-registration has reached all nodes");
					  } else {
					    System.out.println("Un-registration failed!");
					  }
					}*/);
	}
	
	protected void finalize( ) throws Throwable {
		close();
	}
	
	
	public void restore(Integer sequence_no, MqttToken token, MqttWireMessage message) throws MqttPersistenceException {
		DeliveryOptions options = new DeliveryOptions();
		Integer hashcode = new Integer(token.hashCode());
		options.addHeader("token hash", hashcode.toString());
		options.addHeader("sequence_no", sequence_no.toString());
		//options.setCodecName("MqttPersistableWireMessage");
		messages.put(sequence_no, message);
		eb.send(ebtopic, sequence_no.toString(), options);
	}
	
	public void add(MqttWireMessage message, MqttToken token) 
			throws MqttException {	
		System.out.println("--- adding "+message);
		if (queued >= consumer.getMaxBufferedMessages() - 1) { 
			if (!internal.getClient().getBufferOpts().isDeleteOldestMessages()) {
				throw new MqttException(MqttClientException.REASON_CODE_DISCONNECTED_BUFFER_FULL);
			} else {
				remove1message = true;
				removeOldest();
				resume();
			}
		}
		if (persistence != null && message instanceof MqttPersistableWireMessage) {
			System.out.println("--- persisting "+message);
			persistence.put(PERSISTENCE_SENT_BUFFERED_PREFIX + sequence_no, 
					(MqttPersistableWireMessage)message);
		}
		queued++;
		restore(new Integer(sequence_no), token, message);
		sequence_no++; // ready for next time
	}
	
	// Stop processing messages temporarily 
	public void pause() {
		System.out.println("--- pause "+internal.getClientId());
		consumer.pause();
	}
	
	// Start processing messages again
	public void resume() {
		System.out.println("+++ resume "+internal.getClientId());
		consumer.resume();
	}
	
	/*
	 * This is called each time a message is available on the todo queue
	 */
	public void handle(Message<String> message) {
		queued--;
		Integer seqno = new Integer(message.body());
		if (remove1message) {
			pause();
			remove1message = false;
		}
		if (!messages.containsKey(seqno)) {
			// it was already removed, by deleteMsg
			return;
		}
		MqttWireMessage mqttmessage = messages.remove(seqno);
		MultiMap metadata = message.headers();
		System.out.println("*** handle message "+internal.getClientId() + " "+mqttmessage);
		try {
			if (remove1message) {
			
			} else if (mqttmessage instanceof MqttPublish) {
				publish((MqttPublish)mqttmessage, new Integer(metadata.get("token hash")));
			} else if (mqttmessage instanceof MqttSubscribe || 
					mqttmessage instanceof MqttUnsubscribe) {
				write(mqttmessage);
			}	
			System.out.println("*** unpersisting "+persistence + " " + (message instanceof MqttPersistableWireMessage));
			if (persistence != null && mqttmessage instanceof MqttPersistableWireMessage) {
				System.out.println("*** unpersisting "+mqttmessage.getMessageId());
				persistence.remove(PERSISTENCE_SENT_BUFFERED_PREFIX + seqno);
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}

	}
	
	private void publish(MqttPublish publish, Integer hashcode) throws MqttException {
		
		// Add the info to the retry queue.  In the event of reconnecting, any outstanding publishes
		// will need to be resent.
		Long sessionExpiry = internal.getClient().getConnectOpts().getConnectionProperties().getSessionExpiryInterval();
		if (sessionExpiry == null) {
			sessionExpiry = new Long(0L);
		}
		if (sessionExpiry >= 0L /*&& this.persistence != null*/) {
			internal.getSessionState().addRetryQueue(publish, hashcode);
		}
		
		internal.socket.write(Buffer.buffer(publish.serialize()),
			res1 -> {
				if (res1.succeeded()) {
					connectionstate.registerOutboundActivity();
					if (publish.getQoS() == 0) {
						MqttToken token = internal.out_hash_tokens.remove(hashcode);
						token.setComplete();
					}
				} else {
					System.out.println("publish fail");
					// If the socket write fails, then we should remove it from persistence.
					pause();
				}
			});
	}
	
	private void write(MqttWireMessage subscribe) throws MqttException {
		internal.socket.write(Buffer.buffer(subscribe.serialize()),
				res1 -> {
					if (res1.succeeded()) {
						System.out.println("*** "+internal.getClientId() + " " + connectionstate);
						connectionstate.registerOutboundActivity();
					} else {
						System.out.println("subscribe fail");
					}
					
				});
	}
	
	
	public MqttWireMessage removeOldest() {
		LinkedList<Integer> keys = new LinkedList<Integer>(messages.keySet()); 
		Collections.sort(keys);
		MqttWireMessage result = messages.remove(keys.get(0));
		keys = new LinkedList<Integer>(messages.keySet()); 
		return result;
	}
	
	public MqttWireMessage removeMessage(int index) {
		// This doesn't remove it from the Vert.x queue, so it doesn't make space
		// for a new message if the buffer is full
		LinkedList<Integer> keys = new LinkedList<Integer>(messages.keySet()); 
		Collections.sort(keys);
		return messages.remove(keys.get(index));
	}
	
	public MqttWireMessage getMessage(int index) {
		LinkedList<Integer> keys = new LinkedList<Integer>(messages.keySet()); 
		Collections.sort(keys);
		return messages.get(keys.get(index));
	}
	
	
	class Codec implements MessageCodec<MqttPersistableWireMessage,MqttPersistableWireMessage> {

		@Override
		public MqttPersistableWireMessage decodeFromWire(int arg0, Buffer arg1) {	
			return (MqttPersistableWireMessage) internal.getPacket(arg1.slice(arg0, arg1.length()));
		}

		@Override
		public void encodeToWire(Buffer arg0, MqttPersistableWireMessage arg1) {
			try {
				arg0.appendBytes(arg1.serialize());
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}

		@Override
		public String name() {
			return "MqttPersistableWireMessage";
		}

		@Override
		public byte systemCodecID() {
			return -1;
		}

		@Override
		public MqttPersistableWireMessage transform(MqttPersistableWireMessage arg0) {
			return arg0;
		}
	}

}
