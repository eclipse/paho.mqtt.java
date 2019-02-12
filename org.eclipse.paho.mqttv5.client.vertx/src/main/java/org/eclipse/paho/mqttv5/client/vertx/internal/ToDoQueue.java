package org.eclipse.paho.mqttv5.client.vertx.internal;

import org.eclipse.paho.mqttv5.client.vertx.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.vertx.MqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
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
	
	private MqttClientPersistence persistence;
	private EventBus eb;
	private String ebtopic = "paho.mqtt.java.toDoQueue";
	private MessageConsumer<MqttPersistableWireMessage> consumer;
	private ClientInternal internal;
	private MessageCodec codec = new Codec();
	private MqttConnectionState connectionstate;
	
	public ToDoQueue(ClientInternal internal, Vertx vertx, MqttClientPersistence persistence,
			MqttConnectionState connectionstate) {
		this.eb = vertx.eventBus();
		this.persistence = persistence;
		this.internal = internal;
		this.connectionstate = connectionstate;
		
		try {
			eb.registerCodec(codec);
		} catch (IllegalStateException e) {
			// already registered
		}
		ebtopic += "."+this.hashCode(); // ensure this topic is unique for this queue
		consumer = eb.consumer(ebtopic);
		consumer.handler(message -> { handle(message); });		
	}
	
	protected void finalize( ) throws Throwable {
		consumer.unregister(/*res -> {
			  if (res.succeeded()) {
				    System.out.println("The handler un-registration has reached all nodes");
				  } else {
				    System.out.println("Un-registration failed!");
				  }
				}*/);
	}
	
	public void add(MqttWireMessage message, String persistence_key_prefix, MqttToken token) 
			throws MqttPersistenceException {
		if (persistence != null && message instanceof MqttPersistableWireMessage) {
			persistence.put(persistence_key_prefix + message.getMessageId(), 
					(MqttPersistableWireMessage)message);
		}
		DeliveryOptions options = new DeliveryOptions();
		options.addHeader("token hash", new Integer(token.hashCode()).toString());
		options.setCodecName("MqttPersistableWireMessage");
		eb.publish(ebtopic, message, options);
	}
	
	// Stop processing messages temporarily 
	public void pause() {
		consumer.pause();
	}
	
	// Start processing messages again
	public void resume() {
		consumer.resume();
	}
	
	/*
	 * This is called each time a message is available on the todo queue
	 */
	public void handle(Message<MqttPersistableWireMessage> message) {
		MqttWireMessage mqttmessage = message.body();
		MultiMap metadata = message.headers();
		System.out.println("handle message "+message.body());
		try {
			if (mqttmessage instanceof MqttPublish) {
				publish((MqttPublish)mqttmessage, new Integer(metadata.get("token hash")));
			} else if (mqttmessage instanceof MqttSubscribe || 
					mqttmessage instanceof MqttUnsubscribe) {
				write(mqttmessage);
			}	
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void publish(MqttPublish publish, Integer hashcode) throws MqttException {
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
				}
			});
	}
	
	private void write(MqttWireMessage subscribe) throws MqttException {
		internal.socket.write(Buffer.buffer(subscribe.serialize()),
				res1 -> {
					if (res1.succeeded()) {
						connectionstate.registerOutboundActivity();
					} else {
						System.out.println("subscribe fail");
					}
					
				});
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
