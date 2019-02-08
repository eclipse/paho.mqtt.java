package org.eclipse.paho.mqttv5.client.vertx.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Hashtable;

import org.eclipse.paho.mqttv5.client.vertx.MqttClientException;
import org.eclipse.paho.mqttv5.client.vertx.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.vertx.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.vertx.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.vertx.MqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttConnect;
import org.eclipse.paho.mqttv5.common.packet.MqttDataTypes;
import org.eclipse.paho.mqttv5.common.packet.MqttDisconnect;
import org.eclipse.paho.mqttv5.common.packet.MqttPingReq;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttPubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttPubComp;
import org.eclipse.paho.mqttv5.common.packet.MqttPubRec;
import org.eclipse.paho.mqttv5.common.packet.MqttPubRel;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttSubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttSubscribe;
import org.eclipse.paho.mqttv5.common.packet.MqttUnsubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttUnsubscribe;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.eclipse.paho.mqttv5.common.packet.util.VariableByteInteger;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

public class ClientInternal {
	
	private MqttAsyncClient client = null;
	
	private static Object vertxLock = new Object(); // Simple lock
	private static Vertx vertx = null;
	private io.vertx.core.net.NetClient netclient = null;
	public NetSocket socket = null;
	
	private MqttConnectionOptions connOpts;
	private boolean connected = false;
	public Hashtable<Integer, MqttToken> out_tokens = new Hashtable<Integer, MqttToken>();
	public Hashtable<Integer, MqttToken> out_hash_tokens = new Hashtable<Integer, MqttToken>();
	
	
	// Variables that exist within the life of an MQTT session
	private MqttSessionState sessionstate;
	
	private ToDoQueue todoQueue;
	
	public ClientInternal(MqttAsyncClient client, MqttClientPersistence persistence) {
		this.client = client;
		// There is only one vert.x instance which we need to create
		synchronized (vertxLock) {
			if (vertx == null) {
				vertx = Vertx.vertx();
			}
		}
		NetClientOptions options = new NetClientOptions().setLogActivity(true);
		options.setIdleTimeout(100);
		options.setConnectTimeout(100);
		netclient = vertx.createNetClient(options);
		sessionstate = new MqttSessionState(persistence);
		
		todoQueue = new ToDoQueue(this, vertx, persistence);
	}
	
	private void handleData(Buffer buffer, MqttToken connectToken) {
		MqttWireMessage msg = getPacket(buffer);
		
		while (msg != null) {
			handlePacket(msg, connectToken);
			msg = getPacket(null);
		}
	}
	
	public static VariableByteInteger readVariableByteInteger(Buffer in) throws IOException {
		byte digit;
		int value = 0;
		int multiplier = 1;
		int count = 0;

		do {
			digit = in.getByte(count + 1);
			count++;
			value += ((digit & 0x7F) * multiplier);
			multiplier *= 128;
		} while ((digit & 0x80) != 0);

		if (value < 0 || value > MqttDataTypes.VARIABLE_BYTE_INT_MAX) {
			throw new IOException("This property must be a number between 0 and " + 
					MqttDataTypes.VARIABLE_BYTE_INT_MAX
					+ ". Read value was: " + value);
		}
		return new VariableByteInteger(value, count);
	}
	
	Buffer tempBuffer = Buffer.buffer();
	VariableByteInteger remlen = null;
	int packet_len = 0;
	
	public MqttWireMessage getPacket(Buffer buffer) {
		MqttWireMessage msg = null;
		try {
			if (tempBuffer.length() == 0) {
				if (buffer == null) {
					return null; // no more MQTT packets in the data
				}
				remlen = readVariableByteInteger(buffer);
				packet_len = remlen.getValue() + remlen.getEncodedLength() + 1;
				if (packet_len <= buffer.length()) { // we have at least 1 complete packet
					msg = MqttWireMessage.createWireMessage(buffer.getBytes(0, packet_len));
					// put any unused data into the temporary buffer
					if (buffer.length() > packet_len) {
						tempBuffer.appendBuffer(buffer, packet_len, buffer.length() - packet_len);
						remlen = null; // just in case there aren't enough bytes for the VBI
						remlen = readVariableByteInteger(tempBuffer);
						packet_len = remlen.getValue() + remlen.getEncodedLength() + 1;
					}
				} else {
					// incomplete packet
					tempBuffer.appendBuffer(buffer);
					return null;
				}
			} else {
				if (buffer != null) {
					tempBuffer.appendBuffer(buffer);
				}
				if (remlen == null) {
					remlen = readVariableByteInteger(tempBuffer);
					packet_len = remlen.getValue() + remlen.getEncodedLength() + 1;
				}
				if (tempBuffer.length() >= packet_len) {
					msg = MqttWireMessage.createWireMessage(tempBuffer.getBytes(0, packet_len));
					if (tempBuffer.length() > packet_len) {
						// leave unused data in the temporary buffer
						tempBuffer = tempBuffer.getBuffer(packet_len, tempBuffer.length());
						remlen = null; // just in case there aren't enough bytes for the VBI
						remlen = readVariableByteInteger(tempBuffer);
						packet_len = remlen.getValue() + remlen.getEncodedLength() + 1;
					} else {
						tempBuffer = Buffer.buffer();
					}
				} else {
					return null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return msg;
	}
		
	private void handlePacket(MqttWireMessage msg, MqttToken connectToken) {
		try
		{		
			System.out.println("DEBUG - msg received "+msg.toString());
			if (msg instanceof MqttConnAck) {
				connectToken.setResponse(msg);
				connected = true;
				connectToken.setComplete();	
				String assigned_clientid = connectToken.getResponse().getProperties().getAssignedClientIdentifier();	
				if (assigned_clientid != null) {
					client.setClientId(assigned_clientid);
				}
				try {
				long kid = vertx.setPeriodic(connOpts.getKeepAliveInterval() * 1000, id -> {
					keepAlive();
				});
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (msg instanceof MqttSubAck || msg instanceof MqttPubAck
					|| msg instanceof MqttUnsubAck || msg instanceof MqttPubComp) {
				MqttToken acktoken = out_tokens.get(new Integer(msg.getMessageId()));
				if (acktoken != null) {
					if (msg instanceof MqttPubComp) {
						int[] a = acktoken.getReasonCodes();
						int[] b = msg.getReasonCodes();					
						int[] c = new int[a.length + b.length];
						System.arraycopy(a, 0, c, 0, a.length);
						System.arraycopy(b, 0, c, a.length, b.length);				
						acktoken.setReasonCodes(c);
					}
					acktoken.setResponse(msg);
					out_tokens.remove(new Integer(msg.getMessageId()));
					acktoken.setComplete();
				}
			} else if (msg instanceof MqttPublish) {
				if (client.getCallback() != null) {
					client.getCallback().messageArrived(((MqttPublish) msg).getTopicName(), 
							((MqttPublish) msg).getMessage());
				}
			} else if (msg instanceof MqttPubRec) {
				MqttToken acktoken = out_tokens.get(new Integer(msg.getMessageId()));
				MqttPubRel pubrel = new MqttPubRel(MqttReturnCode.RETURN_CODE_SUCCESS, 
						msg.getMessageId(),
						msg.getProperties());
				acktoken.setReasonCodes(msg.getReasonCodes());
				socket.write(Buffer.buffer(pubrel.serialize())); // check write successful
			}
		} catch (Exception e) {
			//internal_connect(options, userToken, serverURIs, index + 1);
		}
	}

	private void keepAlive() {
		System.out.println("keepalive");
		
		/* This is the simplest and incorrect implementation - 
		 * it sends a ping at the keep alive interval whether or not there has been existing traffic.
		 */
		MqttPingReq pingreq = new MqttPingReq();
		try {
			socket.write(Buffer.buffer(pingreq.serialize()),
				res1 -> {
					if (!res1.succeeded()) {
						
					}
				});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public void connect(MqttConnectionOptions options, MqttToken userToken, 
			String[] serverURIs, final int index) {
	
		connOpts = options;
		
		if (index >= serverURIs.length) {
			// connect failed
			System.out.println("connect failed");
			userToken.setComplete();
			return;
		}
		
		URI uri = null;
		try {
			uri = new URI(serverURIs[index]);
		} catch (Exception e) {
			e.printStackTrace();
			connect(options, userToken, serverURIs, index + 1);
			return;
		}
		
		System.out.println("Connecting to "+uri.toString());

		try {
			netclient.connect(uri.getPort(), uri.getHost(), res -> {
			if (res.succeeded()) {
				socket = res.result();
				socket.handler(buffer -> {
					handleData(buffer, userToken);
				});
				socket.closeHandler(v -> {
					System.out.println("The socket has been closed "+v);
					connected = false;
				});
				socket.exceptionHandler(throwable -> {
					System.out.println("The socket has an exception "+throwable.getMessage());
				});
				System.out.println("Cleansession: "+options.isCleanStart());
				MqttConnect connect = new MqttConnect(client.getClientId(), 
						options.getMqttVersion(),
						options.isCleanStart(),
						options.getKeepAliveInterval(),
						null, // properties
						new MqttProperties());  // will properties
				try {
					socket.write(Buffer.buffer(connect.serialize()),
						res1 -> {
							if (!res1.succeeded()) {
								connect(options, userToken, serverURIs, index + 1);
							}
						});
				} catch (Exception e) {
					e.printStackTrace();
					connect(options, userToken, serverURIs, index + 1);
				}
			} else {
				System.out.println("TCP connect failed");
				connect(options, userToken, serverURIs, index + 1);
			}
		});
		} catch (Exception e) {
			e.printStackTrace();
			connect(options, userToken, serverURIs, index + 1);
		}
	}
	
	public void disconnect(int reasonCode, MqttProperties disconnectProperties, MqttToken token) {
		try {
			MqttDisconnect disconnect = new MqttDisconnect(reasonCode, disconnectProperties);
			socket.write(Buffer.buffer(disconnect.serialize()),
					res1 -> {
						if (res1.succeeded()) {
							// we still need to close the socket and indicate the disconnect
							// is finished if the packet write failed
							socket.close();
							socket = null;
							token.setComplete();
							connected = false;
						} else {
							System.out.println("write failed");
							// we still need to close the socket and indicate the disconnect
							// is finished if the packet write failed
							socket.close();
							socket = null;
							token.setComplete();
							connected = false;
						}
					});
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
	
	public void subscribe(MqttSubscription[] subscriptions, MqttProperties subscriptionProperties,
			MqttToken token) throws MqttException {
		MqttSubscribe subscribe = new MqttSubscribe(subscriptions, subscriptionProperties);
		subscribe.setMessageId(sessionstate.getNextMessageId());
		out_tokens.put(new Integer(subscribe.getMessageId()), token);
		todoQueue.add(subscribe, "", token);
	}
	
	public void unsubscribe(String[] topicFilters, MqttProperties unsubscribeProperties,
			MqttToken token) throws MqttException {

		MqttUnsubscribe unsubscribe = new MqttUnsubscribe(topicFilters, unsubscribeProperties);
		unsubscribe.setMessageId(sessionstate.getNextMessageId());
		out_tokens.put(new Integer(unsubscribe.getMessageId()), token);
		todoQueue.add(unsubscribe, "", token);
	}
	
	public void publish(String topic, MqttMessage message, MqttToken token) throws MqttException {
		
		// if we are not connected, and offline buffering is not enabled, then we return a failure
		if (!client.isConnected()) {
			throw new MqttException(MqttClientException.REASON_CODE_CLIENT_NOT_CONNECTED);
		}
		
		MqttPublish publish = new MqttPublish(topic, message, message.getProperties());
		if (message.getQos() > 0) {
			publish.setMessageId(sessionstate.getNextMessageId()); // getNextId
			out_tokens.put(new Integer(publish.getMessageId()), token);
		} else {
			// QoS 0 messages have no message id
			out_hash_tokens.put(new Integer(token.hashCode()), token);
		}

		// Add the info to the retry queue.  In the event of reconnecting, any outstanding publishes
		// will need to be resent.
		System.out.println("Session expiry " +connOpts.getConnectionProperties().getSessionExpiryInterval());
		Long sessionExpiry = connOpts.getConnectionProperties().getSessionExpiryInterval();
		if (sessionExpiry == null) {
			sessionExpiry = new Long(0L);
		}
		if (sessionExpiry >= 0L /*&& this.persistence != null*/) {
			System.out.println("Adding to retry queue");
			sessionstate.addRetryQueue(publish, token);
		}
		
		todoQueue.add(publish, "", token);
		
		//System.out.println("publish at "+message.getQos());
		/*socket.write(Buffer.buffer(publish.serialize()),
				res1 -> {
					if (res1.succeeded()) {
						System.out.println("published successfully");
						if (message.getQos() == 0) {
							token.setComplete();
						}
					} else {
						System.out.println("publish fail");
						// If the socket write fails, then we should remove it from persistence.
					}
				});*/
		
	}
	
	public MqttSessionState getSessionState() {
		return sessionstate;
	}
	
	public String getClientId() {
		return sessionstate.getClientId();
	}

	public void setClientId(String clientId) {
		sessionstate.setClientId(clientId);
	}


}
