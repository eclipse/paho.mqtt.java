/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corp.
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
 *    Ian Craggs - MQTT 3.1.1 support
 *    Ian Craggs - per subscription message handlers (bug 466579)
 *    Ian Craggs - ack control (bug 472172)
 */
package org.eclipse.paho.client.mqttv3;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.util.Debug;

/**
 * Lightweight client for talking to an MQTT server using methods that block
 * until an operation completes.
 *
 * <p>This class implements the blocking {@link IMqttClient} client interface where all
 * actions block until they have completed (or timed out).
 * This implementation is compatible with all Java SE runtimes from 1.7 and up.
 * </p>
 * <p>An application can connect to an MQTT server using:</p>
 * <ul>
 * <li>A plain TCP socket
 * <li>An secure SSL/TLS socket
 * </ul>
 *
 * <p>To enable messages to be delivered even across network and client restarts
 * messages need to be safely stored until the message has been delivered at the requested
 * quality of service. A pluggable persistence mechanism is provided to store the messages.
 * </p>
 * <p>By default {@link MqttDefaultFilePersistence} is used to store messages to a file.
 * If persistence is set to null then messages are stored in memory and hence can  be lost
 * if the client, Java runtime or device shuts down.
 * </p>
 * <p>If connecting with {@link MqttConnectOptions#setCleanSession(boolean)} set to true it
 * is safe to use memory persistence as all state it cleared when a client disconnects. If
 * connecting with cleanSession set to false, to provide reliable message delivery
 * then a persistent message store should be used such as the default one. </p>
 * <p>The message store interface is pluggable. Different stores can be used by implementing
 * the {@link MqttClientPersistence} interface and passing it to the clients constructor.
 * </p>
 *
 * @see IMqttClient
 */
public class MqttClient implements IMqttClient { //), DestinationProvider {
	//private static final String CLASS_NAME = MqttClient.class.getName();
	//private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,CLASS_NAME);

	protected MqttAsyncClient aClient = null;  // Delegate implementation to MqttAsyncClient
	protected long timeToWait = -1;				// How long each method should wait for action to complete

	/**
	 * Create an MqttClient that can be used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively
	 * a list containing one or more servers can be specified using the
	 * {@link MqttConnectOptions#setServerURIs(String[]) setServerURIs} method
	 * on MqttConnectOptions.
	 *
	 * <p>The <code>serverURI</code> parameter is typically used with the
	 * the <code>clientId</code> parameter to form a key. The key
	 * is used to store and reference messages while they are being delivered.
	 * Hence the serverURI specified on the constructor must still be specified even if a list
	 * of servers is specified on an MqttConnectOptions object.
	 * The serverURI on the constructor must remain the same across
	 * restarts of the client for delivery of messages to be maintained from a given
	 * client to a given server or set of servers.
	 *
	 * <p>The address of the server to connect to is specified as a URI. Two types of
	 * connection are supported <code>tcp://</code> for a TCP connection and
	 * <code>ssl://</code> for a TCP connection secured by SSL/TLS.
	 * For example:</p>
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * <p>
	 * If the port is not specified, it will
	 * default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 65535 characters.
	 * It must be unique across all clients connecting to the same
	 * server. The clientId is used by the server to store data related to the client,
	 * hence it is important that the clientId remain the same when connecting to a server
	 * if durable subscriptions or reliable messaging are required.
	 * <p>A convenience method is provided to generate a random client id that
	 * should satisfy this criteria - {@link #generateClientId()}. As the client identifier
	 * is used by the server to identify a client when it reconnects, the client must use the
	 * same identifier between connections if durable subscriptions or reliable
	 * delivery of messages is required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the
	 * client will use in the following order:
	 * </p>
	 * <ul>
	 * 	<li><strong>Supplying an <code>SSLSocketFactory</code></strong> - applications can
	 * use {@link MqttConnectOptions#setSocketFactory(SocketFactory)} to supply
	 * a factory with the appropriate SSL settings.</li>
	 * 	<li><strong>SSL Properties</strong> - applications can supply SSL settings as a
	 * simple Java Properties using {@link MqttConnectOptions#setSSLProperties(Properties)}.</li>
	 * 	<li><strong>Use JVM settings</strong> - There are a number of standard
	 * Java system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>In Java ME, the platform settings are used for SSL connections.</p>
	 *
	 * <p>An instance of the default persistence mechanism {@link MqttDefaultFilePersistence}
	 * is used by the client. To specify a different persistence mechanism or to turn
	 * off persistence, use the {@link #MqttClient(String, String, MqttClientPersistence)}
	 * constructor.
	 *
	 * @param serverURI the address of the server to connect to, specified as a URI. Can be overridden using
	 * {@link MqttConnectOptions#setServerURIs(String[])}
	 * @param clientId a client identifier that is unique on the server being connected to
	 * @throws IllegalArgumentException if the URI does not start with
	 * "tcp://", "ssl://" or "local://".
	 * @throws IllegalArgumentException if the clientId is null or is greater than 65535 characters in length
	 * @throws MqttException if any other problem was encountered
	 */
	public MqttClient(String serverURI, String clientId) throws MqttException {
		this(serverURI,clientId, new MqttDefaultFilePersistence());
	}

	/**
	 * Create an MqttClient that can be used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively
	 * a list containing one or more servers can be specified using the
	 * {@link MqttConnectOptions#setServerURIs(String[]) setServerURIs} method
	 * on MqttConnectOptions.
	 *
	 * <p>The <code>serverURI</code> parameter is typically used with the
	 * the <code>clientId</code> parameter to form a key. The key
	 * is used to store and reference messages while they are being delivered.
	 * Hence the serverURI specified on the constructor must still be specified even if a list
	 * of servers is specified on an MqttConnectOptions object.
	 * The serverURI on the constructor must remain the same across
	 * restarts of the client for delivery of messages to be maintained from a given
	 * client to a given server or set of servers.
	 *
	 * <p>The address of the server to connect to is specified as a URI. Two types of
	 * connection are supported <code>tcp://</code> for a TCP connection and
	 * <code>ssl://</code> for a TCP connection secured by SSL/TLS.
	 * For example:</p>
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * <p>
	 * If the port is not specified, it will
	 * default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 65535 characters.
	 * It must be unique across all clients connecting to the same
	 * server. The clientId is used by the server to store data related to the client,
	 * hence it is important that the clientId remain the same when connecting to a server
	 * if durable subscriptions or reliable messaging are required.
	 * <p>A convenience method is provided to generate a random client id that
	 * should satisfy this criteria - {@link #generateClientId()}. As the client identifier
	 * is used by the server to identify a client when it reconnects, the client must use the
	 * same identifier between connections if durable subscriptions or reliable
	 * delivery of messages is required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the
	 * client will use in the following order:
	 * </p>
	 * <ul>
	 * 	<li><strong>Supplying an <code>SSLSocketFactory</code></strong> - applications can
	 * use {@link MqttConnectOptions#setSocketFactory(SocketFactory)} to supply
	 * a factory with the appropriate SSL settings.</li>
	 * 	<li><strong>SSL Properties</strong> - applications can supply SSL settings as a
	 * simple Java Properties using {@link MqttConnectOptions#setSSLProperties(Properties)}.</li>
	 * 	<li><strong>Use JVM settings</strong> - There are a number of standard
	 * Java system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>In Java ME, the platform settings are used for SSL connections.</p>
	 * <p>
	 * A persistence mechanism is used to enable reliable messaging.
	 * For messages sent at qualities of service (QoS) 1 or 2 to be reliably delivered,
	 * messages must be stored (on both the client and server) until the delivery of the message
	 * is complete. If messages are not safely stored when being delivered then
	 * a failure in the client or server can result in lost messages. A pluggable
	 * persistence mechanism is supported via the {@link MqttClientPersistence}
	 * interface. An implementer of this interface that safely stores messages
	 * must be specified in order for delivery of messages to be reliable. In
	 * addition {@link MqttConnectOptions#setCleanSession(boolean)} must be set
	 * to false. In the event that only QoS 0 messages are sent or received or
	 * cleanSession is set to true then a safe store is not needed.
	 * </p>
	 * <p>An implementation of file-based persistence is provided in
	 * class {@link MqttDefaultFilePersistence} which will work in all Java SE based
	 * systems. If no persistence is needed, the persistence parameter
	 * can be explicitly set to <code>null</code>.</p>
	 *
	 * @param serverURI the address of the server to connect to, specified as a URI. Can be overridden using
	 * {@link MqttConnectOptions#setServerURIs(String[])}
	 * @param clientId a client identifier that is unique on the server being connected to
 	 * @param persistence the persistence class to use to store in-flight message. If null then the
 	 * default persistence mechanism is used
	 * @throws IllegalArgumentException if the URI does not start with
	 * "tcp://", "ssl://" or "local://"
	 * @throws IllegalArgumentException if the clientId is null or is greater than 65535 characters in length
	 * @throws MqttException if any other problem was encountered
	 */
	public MqttClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
		aClient = new MqttAsyncClient(serverURI, clientId, persistence);
	}

	/**
	 * Create an MqttClient that can be used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively
	 * a list containing one or more servers can be specified using the
	 * {@link MqttConnectOptions#setServerURIs(String[]) setServerURIs} method
	 * on MqttConnectOptions.
	 *
	 * <p>The <code>serverURI</code> parameter is typically used with the
	 * the <code>clientId</code> parameter to form a key. The key
	 * is used to store and reference messages while they are being delivered.
	 * Hence the serverURI specified on the constructor must still be specified even if a list
	 * of servers is specified on an MqttConnectOptions object.
	 * The serverURI on the constructor must remain the same across
	 * restarts of the client for delivery of messages to be maintained from a given
	 * client to a given server or set of servers.
	 *
	 * <p>The address of the server to connect to is specified as a URI. Two types of
	 * connection are supported <code>tcp://</code> for a TCP connection and
	 * <code>ssl://</code> for a TCP connection secured by SSL/TLS.
	 * For example:
	 * </p>
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * <p>If the port is not specified, it will
	 * default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 65535 characters.
	 * It must be unique across all clients connecting to the same
	 * server. The clientId is used by the server to store data related to the client,
	 * hence it is important that the clientId remain the same when connecting to a server
	 * if durable subscriptions or reliable messaging are required.
	 * <p>A convenience method is provided to generate a random client id that
	 * should satisfy this criteria - {@link #generateClientId()}. As the client identifier
	 * is used by the server to identify a client when it reconnects, the client must use the
	 * same identifier between connections if durable subscriptions or reliable
	 * delivery of messages is required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the
	 * client will use in the following order:
	 * </p>
	 * <ul>
	 * 	<li><strong>Supplying an <code>SSLSocketFactory</code></strong> - applications can
	 * use {@link MqttConnectOptions#setSocketFactory(SocketFactory)} to supply
	 * a factory with the appropriate SSL settings.</li>
	 * 	<li><strong>SSL Properties</strong> - applications can supply SSL settings as a
	 * simple Java Properties using {@link MqttConnectOptions#setSSLProperties(Properties)}.</li>
	 * 	<li><strong>Use JVM settings</strong> - There are a number of standard
	 * Java system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>In Java ME, the platform settings are used for SSL connections.</p>
	 * <p>
	 * A persistence mechanism is used to enable reliable messaging.
	 * For messages sent at qualities of service (QoS) 1 or 2 to be reliably delivered,
	 * messages must be stored (on both the client and server) until the delivery of the message
	 * is complete. If messages are not safely stored when being delivered then
	 * a failure in the client or server can result in lost messages. A pluggable
	 * persistence mechanism is supported via the {@link MqttClientPersistence}
	 * interface. An implementer of this interface that safely stores messages
	 * must be specified in order for delivery of messages to be reliable. In
	 * addition {@link MqttConnectOptions#setCleanSession(boolean)} must be set
	 * to false. In the event that only QoS 0 messages are sent or received or
	 * cleanSession is set to true then a safe store is not needed.
	 * </p>
	 * <p>An implementation of file-based persistence is provided in
	 * class {@link MqttDefaultFilePersistence} which will work in all Java SE based
	 * systems. If no persistence is needed, the persistence parameter
	 * can be explicitly set to <code>null</code>.</p>
	 *
	 * @param serverURI the address of the server to connect to, specified as a URI. Can be overridden using
	 * {@link MqttConnectOptions#setServerURIs(String[])}
	 * @param clientId a client identifier that is unique on the server being connected to
	 * @param persistence the persistence class to use to store in-flight message. If null then the
	 * default persistence mechanism is used
	 * @param executorService used for managing threads. If null then a newFixedThreadPool is used.
	 * @throws IllegalArgumentException if the URI does not start with
	 * "tcp://", "ssl://" or "local://"
	 * @throws IllegalArgumentException if the clientId is null or is greater than 65535 characters in length
	 * @throws MqttException if any other problem was encountered
	 */
	public MqttClient(String serverURI, String clientId, MqttClientPersistence persistence, ScheduledExecutorService executorService) throws MqttException {
		aClient = new MqttAsyncClient(serverURI, clientId, persistence, new ScheduledExecutorPingSender(executorService), executorService);
	}

	/*
	 * @see IMqttClient#connect()
	 */
	public void connect() throws MqttSecurityException, MqttException {
		this.connect(new MqttConnectOptions());
	}

	/*
	 * @see IMqttClient#connect(MqttConnectOptions)
	 */
	public void connect(MqttConnectOptions options) throws MqttSecurityException, MqttException {
		aClient.connect(options, null, null).waitForCompletion(getTimeToWait());
	}

	/*
	 * @see IMqttClient#connect(MqttConnectOptions)
	 */
	public IMqttToken connectWithResult(MqttConnectOptions options) throws MqttSecurityException, MqttException {
		IMqttToken tok = aClient.connect(options, null, null);
		tok.waitForCompletion(getTimeToWait());
		return tok;
	}

	/*
	 * @see IMqttClient#disconnect()
	 */
	public void disconnect() throws MqttException {
		aClient.disconnect().waitForCompletion();
	}

	/*
	 * @see IMqttClient#disconnect(long)
	 */
	public void disconnect(long quiesceTimeout) throws MqttException {
		aClient.disconnect(quiesceTimeout, null, null).waitForCompletion();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnectForcibly()
	 */
	public void disconnectForcibly() throws MqttException {
		aClient.disconnectForcibly();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnectForcibly(long)
	 */
	public void disconnectForcibly(long disconnectTimeout) throws MqttException {
		aClient.disconnectForcibly(disconnectTimeout);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnectForcibly(long, long)
	 */
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
		aClient.disconnectForcibly(quiesceTimeout, disconnectTimeout);
	}

	/**
	 * Disconnects from the server forcibly to reset all the states. Could be useful when disconnect attempt failed.
	 * <p>
	 * Because the client is able to establish the TCP/IP connection to a none MQTT server and it will certainly fail to
	 * send the disconnect packet.
	 *
	 * @param quiesceTimeout the amount of time in milliseconds to allow for existing work to finish before
	 * disconnecting. A value of zero or less means the client will not quiesce.
	 * @param disconnectTimeout the amount of time in milliseconds to allow send disconnect packet to server.
	 * @param sendDisconnectPacket if true, will send the disconnect packet to the server
	 * @throws MqttException if any unexpected error
	 */
    public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout, boolean sendDisconnectPacket) throws MqttException {
    	aClient.disconnectForcibly(quiesceTimeout, disconnectTimeout, sendDisconnectPacket);
    }

	/*
	 * @see IMqttClient#subscribe(String)
	 */
	public void subscribe(String topicFilter) throws MqttException {
		this.subscribe(new String[] {topicFilter}, new int[] {1});
	}

	/*
	 * @see IMqttClient#subscribe(String[])
	 */
	public void subscribe(String[] topicFilters) throws MqttException {
		int[] qos = new int[topicFilters.length];
		for (int i=0; i<qos.length; i++) {
			qos[i] = 1;
		}
		this.subscribe(topicFilters, qos);
	}

	/*
	 * @see IMqttClient#subscribe(String, int)
	 */
	public void subscribe(String topicFilter, int qos) throws MqttException {
		this.subscribe(new String[] {topicFilter}, new int[] {qos});
	}

	/*
	 * @see IMqttClient#subscribe(String[], int[])
	 */
	public void subscribe(String[] topicFilters, int[] qos) throws MqttException {
		IMqttToken tok = aClient.subscribe(topicFilters, qos, null, null);
		tok.waitForCompletion(getTimeToWait());
		int[] grantedQos = tok.getGrantedQos();
		for (int i = 0; i < grantedQos.length; ++i) {
			qos[i] = grantedQos[i];
		}
		if (grantedQos.length == 1 && qos[0] == 0x80) {
			throw new MqttException(MqttException.REASON_CODE_SUBSCRIBE_FAILED);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#subscribe(java.lang.String, int, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public void subscribe(String topicFilter, IMqttMessageListener messageListener) throws MqttException {
		this.subscribe(new String[] {topicFilter}, new int[] {1}, new IMqttMessageListener[] {messageListener});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#subscribe(java.lang.String, int, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public void subscribe(String[] topicFilters, IMqttMessageListener[] messageListeners) throws MqttException {
		int[] qos = new int[topicFilters.length];
		for (int i=0; i<qos.length; i++) {
			qos[i] = 1;
		}
		this.subscribe(topicFilters, qos, messageListeners);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#subscribe(java.lang.String, int)
	 */
	public void subscribe(String topicFilter, int qos, IMqttMessageListener messageListener) throws MqttException {
		this.subscribe(new String[] {topicFilter}, new int[] {qos}, new IMqttMessageListener[] {messageListener});
	}


	public void subscribe(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) throws MqttException {
		this.subscribe(topicFilters, qos);

		// add message handlers to the list for this client
		for (int i = 0; i < topicFilters.length; ++i) {
			aClient.comms.setMessageListener(topicFilters[i], messageListeners[i]);
		}
	}

	/*
	 * @see IMqttClient#subscribeWithResponse(String)
	 */
	public IMqttToken subscribeWithResponse(String topicFilter) throws MqttException {
		return this.subscribeWithResponse(new String[] {topicFilter}, new int[] {1});
	}

	/*
	 * @see IMqttClient#subscribeWithResponse(String, IMqttMessageListener)
	 */
	public IMqttToken subscribeWithResponse(String topicFilter, IMqttMessageListener messageListener) throws MqttException {
		return this.subscribeWithResponse(new String[] {topicFilter}, new int[] {1}, new IMqttMessageListener[] {messageListener});
	}

	/*
	 * @see IMqttClient#subscribeWithResponse(String, int)
	 */
	public IMqttToken subscribeWithResponse(String topicFilter, int qos) throws MqttException {
		return this.subscribeWithResponse(new String[] {topicFilter}, new int[] {qos});
	}

	/*
	 * @see IMqttClient#subscribeWithResponse(String, int, IMqttMessageListener)
	 */
	public IMqttToken subscribeWithResponse(String topicFilter, int qos, IMqttMessageListener messageListener)
			throws MqttException {
		return this.subscribeWithResponse(new String[] {topicFilter}, new int[] {qos}, new IMqttMessageListener[] {messageListener});
	}

	/*
	 * @see IMqttClient#subscribeWithResponse(String[])
	 */
	public IMqttToken subscribeWithResponse(String[] topicFilters) throws MqttException {
		int[] qos = new int[topicFilters.length];
		for (int i=0; i<qos.length; i++) {
			qos[i] = 1;
		}
		return this.subscribeWithResponse(topicFilters, qos);
	}

	/*
	 * @see IMqttClient#subscribeWithResponse(String[], IMqttMessageListener[])
	 */
	public IMqttToken subscribeWithResponse(String[] topicFilters, IMqttMessageListener[] messageListeners)
			throws MqttException {
		int[] qos = new int[topicFilters.length];
		for (int i=0; i<qos.length; i++) {
			qos[i] = 1;
		}
		return this.subscribeWithResponse(topicFilters, qos, messageListeners);
	}

	/*
	 * @see IMqttClient#subscribeWithResponse(String[], int[])
	 */
	public IMqttToken subscribeWithResponse(String[] topicFilters, int[] qos) throws MqttException {
		IMqttToken tok = aClient.subscribe(topicFilters, qos, null, null);
		tok.waitForCompletion(getTimeToWait());
		return tok;
	}

	/*
	 * @see IMqttClient#subscribeWithResponse(String[], int[], IMqttMessageListener[])
	 */
	public IMqttToken subscribeWithResponse(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners)
			throws MqttException {
		IMqttToken tok = this.subscribeWithResponse(topicFilters, qos);

		// add message handlers to the list for this client
		for (int i = 0; i < topicFilters.length; ++i) {
			aClient.comms.setMessageListener(topicFilters[i], messageListeners[i]);
		}
		return tok;
	}

	/*
	 * @see IMqttClient#unsubscribe(String)
	 */
	public void unsubscribe(String topicFilter) throws MqttException {
		unsubscribe(new String[] {topicFilter});
	}

	/*
	 * @see IMqttClient#unsubscribe(String[])
	 */
	public void unsubscribe(String[] topicFilters) throws MqttException {
		// message handlers removed in the async client unsubscribe below
		aClient.unsubscribe(topicFilters, null,null).waitForCompletion(getTimeToWait());
	}

	/*
	 * @see IMqttClient#publishBlock(String, byte[], int, boolean)
	 */
	public void publish(String topic, byte[] payload,int qos, boolean retained) throws MqttException,
			MqttPersistenceException {
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		this.publish(topic, message);
	}

	/*
	 * @see IMqttClient#publishBlock(String, MqttMessage)
	 */
	public void publish(String topic, MqttMessage message) throws MqttException,
			MqttPersistenceException {
		aClient.publish(topic, message, null, null).waitForCompletion(getTimeToWait());
	}

	/**
	 * Set the maximum time to wait for an action to complete.
	 * <p>Set the maximum time to wait for an action to complete before
	 * returning control to the invoking application. Control is returned
	 * when:</p>
	 * <ul>
	 * <li>the action completes</li>
	 * <li>or when the timeout if exceeded</li>
	 * <li>or when the client is disconnect/shutdown</li>
	 * </ul>
	 * <p>
	 * The default value is -1 which means the action will not timeout.
	 * In the event of a timeout the action carries on running in the
	 * background until it completes. The timeout is used on methods that
	 * block while the action is in progress.
	 * </p>
	 * @param timeToWaitInMillis before the action times out. A value or 0 or -1 will wait until
	 * the action finishes and not timeout.
	 * @throws IllegalArgumentException if timeToWaitInMillis is invalid
	 */
	public void setTimeToWait(long timeToWaitInMillis) throws IllegalArgumentException{
		if (timeToWaitInMillis < -1) {
			throw new IllegalArgumentException();
		}
		this.timeToWait = timeToWaitInMillis;
	}

	/**
	 * Return the maximum time to wait for an action to complete.
	 * @return the time to wait
	 * @see MqttClient#setTimeToWait(long)
	 */
	public long getTimeToWait() {
		return this.timeToWait;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#close()
	 */
	public void close() throws MqttException {
		aClient.close(false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#close()
	 */
	public void close(boolean force) throws MqttException {
		aClient.close(force);
	}
	
	

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#getClientId()
	 */
	public String getClientId() {
		return aClient.getClientId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#getPendingDeliveryTokens()
	 */
	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return aClient.getPendingDeliveryTokens();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#getServerURI()
	 */
	public String getServerURI() {
		return aClient.getServerURI();
	}

	/**
	 * Returns the currently connected Server URI
	 * Implemented due to: https://bugs.eclipse.org/bugs/show_bug.cgi?id=481097
	 *
	 * Where getServerURI only returns the URI that was provided in
	 * MqttAsyncClient's constructor, getCurrentServerURI returns the URI of the
	 * Server that the client is currently connected to. This would be different in scenarios
	 * where multiple server URIs have been provided to the MqttConnectOptions.
	 *
	 * @return the currently connected server URI
	 */
	public String getCurrentServerURI(){
		return aClient.getCurrentServerURI();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#getTopic(java.lang.String)
	 */
	public MqttTopic getTopic(String topic) {
		return aClient.getTopic(topic);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#isConnected()
	 */
	public boolean isConnected() {
		return aClient.isConnected();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#setCallback(org.eclipse.paho.client.mqttv3.MqttCallback)
	 */
	public void setCallback(MqttCallback callback) {
		aClient.setCallback(callback);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttClient#setCallback(org.eclipse.paho.client.mqttv3.MqttCallback)
	 */
	public void setManualAcks(boolean manualAcks) {
		aClient.setManualAcks(manualAcks);
	}

	public void messageArrivedComplete(int messageId, int qos) throws MqttException {
		aClient.messageArrivedComplete(messageId, qos);
	}

	/**
	 * Returns a randomly generated client identifier based on the current user's login
	 * name and the system time.
	 * <p>When cleanSession is set to false, an application must ensure it uses the
	 * same client identifier when it reconnects to the server to resume state and maintain
	 * assured message delivery.</p>
	 * @return a generated client identifier
	 * @see MqttConnectOptions#setCleanSession(boolean)
	 */
	public static String generateClientId() {
		return MqttAsyncClient.generateClientId();
	}

	/**
	 * Will attempt to reconnect to the server after the client has lost connection.
	 * @throws MqttException if an error occurs attempting to reconnect
	 */
	public void reconnect() throws MqttException {
		aClient.reconnect();
	}

	/**
	 * Return a debug object that can be used to help solve problems.
	 * @return the {@link Debug} Object.
	 */
	public Debug getDebug() {
		return (aClient.getDebug());
	}

}
