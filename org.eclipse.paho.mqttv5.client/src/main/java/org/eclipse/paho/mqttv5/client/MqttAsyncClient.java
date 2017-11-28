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
 *    Ian Craggs - MQTT 3.1.1 support
 *    Ian Craggs - per subscription message handlers (bug 466579)
 *    Ian Craggs - ack control (bug 472172)
 *    James Sutton - Bug 459142 - WebSocket support for the Java client.
 *    James Sutton - Automatic Reconnect & Offline Buffering.
 */

package org.eclipse.paho.mqttv5.client;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.mqttv5.client.alpha.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.internal.ClientComms;
import org.eclipse.paho.mqttv5.client.internal.ConnectActionListener;
import org.eclipse.paho.mqttv5.client.internal.DisconnectedMessageBuffer;
import org.eclipse.paho.mqttv5.client.internal.MqttSession;
import org.eclipse.paho.mqttv5.client.internal.NetworkModule;
import org.eclipse.paho.mqttv5.client.internal.SSLNetworkModule;
import org.eclipse.paho.mqttv5.client.internal.TCPNetworkModule;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.client.security.SSLSocketFactoryFactory;
import org.eclipse.paho.mqttv5.client.util.Debug;
import org.eclipse.paho.mqttv5.client.websocket.WebSocketNetworkModule;
import org.eclipse.paho.mqttv5.client.websocket.WebSocketSecureNetworkModule;
import org.eclipse.paho.mqttv5.common.ExceptionHelper;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.MqttSecurityException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttDisconnect;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttSubscribe;
import org.eclipse.paho.mqttv5.common.packet.MqttUnsubscribe;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

/**
 * Lightweight client for talking to an MQTT server using non-blocking methods
 * that allow an operation to run in the background.
 *
 * <p>
 * This class implements the non-blocking {@link IMqttAsyncClient} client
 * interface allowing applications to initiate MQTT actions and then carry on
 * working while the MQTT action completes on a background thread. This
 * implementation is compatible with all Java SE runtimes from 1.7 and up.
 * </p>
 * <p>
 * An application can connect to an MQTT server using:
 * </p>
 * <ul>
 * <li>A plain TCP socket
 * <li>A secure SSL/TLS socket
 * </ul>
 *
 * <p>
 * To enable messages to be delivered even across network and client restarts
 * messages need to be safely stored until the message has been delivered at the
 * requested quality of service. A pluggable persistence mechanism is provided
 * to store the messages.
 * </p>
 * <p>
 * By default {@link MqttDefaultFilePersistence} is used to store messages to a
 * file. If persistence is set to null then messages are stored in memory and
 * hence can be lost if the client, Java runtime or device shuts down.
 * </p>
 * <p>
 * If connecting with {@link MqttConnectionOptions#setCleanSession(boolean)} set
 * to true it is safe to use memory persistence as all state is cleared when a
 * client disconnects. If connecting with cleanSession set to false in order to
 * provide reliable message delivery then a persistent message store such as the
 * default one should be used.
 * </p>
 * <p>
 * The message store interface is pluggable. Different stores can be used by
 * implementing the {@link MqttClientPersistence} interface and passing it to
 * the clients constructor.
 * </p>
 *
 * TODO - Class docs taken from IMqttAsyncClient, review for v5 Enables an
 * application to communicate with an MQTT server using non-blocking methods.
 * <p>
 * It provides applications a simple programming interface to all features of
 * the MQTT version 3.1 specification including:
 * </p>
 * <ul>
 * <li>connect
 * <li>publish
 * <li>subscribe
 * <li>unsubscribe
 * <li>disconnect
 * </ul>
 * <p>
 * There are two styles of MQTT client, this one and {@link IMqttClient}.
 * </p>
 * <ul>
 * <li>IMqttAsyncClient provides a set of non-blocking methods that return
 * control to the invoking application after initial validation of parameters
 * and state. The main processing is performed in the background so as not to
 * block the application program's thread. This non- blocking approach is handy
 * when the application needs to carry on processing while the MQTT action takes
 * place. For instance connecting to an MQTT server can take time, using the
 * non-blocking connect method allows an application to display a busy indicator
 * while the connect action takes place in the background. Non blocking methods
 * are particularly useful in event oriented programs and graphical programs
 * where invoking methods that take time to complete on the the main or GUI
 * thread can cause problems. The non-blocking interface can also be used in
 * blocking form.</li>
 * <li>IMqttClient provides a set of methods that block and return control to
 * the application program once the MQTT action has completed. It is a thin
 * layer that sits on top of the IMqttAsyncClient implementation and is provided
 * mainly for compatibility with earlier versions of the MQTT client. In most
 * circumstances it is recommended to use IMqttAsyncClient based clients which
 * allow an application to mix both non-blocking and blocking calls.</li>
 * </ul>
 * <p>
 * An application is not restricted to using one style if an IMqttAsyncClient
 * based client is used as both blocking and non-blocking methods can be used in
 * the same application. If an IMqttClient based client is used then only
 * blocking methods are available to the application. For more details on the
 * blocking client see {@link IMqttClient}
 * </p>
 *
 * <p>
 * There are two forms of non-blocking method:
 * <ol>
 * <li>
 *
 * <pre>
 *     IMqttToken token = asyncClient.method(parms)
 * </pre>
 * <p>
 * In this form the method returns a token that can be used to track the
 * progress of the action (method). The method provides a waitForCompletion()
 * method that once invoked will block until the action completes. Once
 * completed there are method on the token that can be used to check if the
 * action completed successfully or not. For example to wait until a connect
 * completes:
 * </p>
 *
 * <pre>
 *      IMqttToken conToken;
 *   	conToken = asyncClient.client.connect(conToken);
 *     ... do some work...
 *   	conToken.waitForCompletion();
 * </pre>
 * <p>
 * To turn a method into a blocking invocation the following form can be used:
 * </p>
 *
 * <pre>
 * IMqttToken token;
 * token = asyncClient.method(parms).waitForCompletion();
 * </pre>
 *
 * </li>
 *
 * <li>
 *
 * <pre>
 *     IMqttToken token method(parms, Object userContext, IMqttActionListener callback)
 * </pre>
 * <p>
 * In this form a callback is registered with the method. The callback will be
 * notified when the action succeeds or fails. The callback is invoked on the
 * thread managed by the MQTT client so it is important that processing is
 * minimised in the callback. If not the operation of the MQTT client will be
 * inhibited. For example to be notified (called back) when a connect completes:
 * </p>
 *
 * <pre>
 *     	IMqttToken conToken;
 *	    conToken = asyncClient.connect("some context",new new MqttAsyncActionListener() {
 *			public void onSuccess(IMqttToken asyncActionToken) {
 *				log("Connected");
 *			}
 *
 *			public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
 *				log ("connect failed" +exception);
 *			}
 *		  });
 * </pre>
 * <p>
 * An optional context object can be passed into the method which will then be
 * made available in the callback. The context is stored by the MQTT client) in
 * the token which is then returned to the invoker. The token is provided to the
 * callback methods where the context can then be accessed.
 * </p>
 * </li>
 * </ol>
 * <p>
 * To understand when the delivery of a message is complete either of the two
 * methods above can be used to either wait on or be notified when the publish
 * completes. An alternative is to use the
 * {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)} method which will
 * also be notified when a message has been delivered to the requested quality
 * of service.
 * </p>
 *
 * @see IMqttAsyncClient
 */
public class MqttAsyncClient implements MqttClientInterface {
	private static final String CLASS_NAME = MqttAsyncClient.class.getName();
	private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private static final long QUIESCE_TIMEOUT = 30000; // ms
	private static final long DISCONNECT_TIMEOUT = 10000; // ms
	private static final char MIN_HIGH_SURROGATE = '\uD800';
	private static final char MAX_HIGH_SURROGATE = '\uDBFF';
	//private String clientId;
	private String serverURI;
	protected ClientComms comms;
	private Hashtable<String, MqttTopic> topics;
	private MqttClientPersistence persistence;
	private MqttCallback mqttCallback;
	private MqttConnectionOptions connOpts;
	private Object userContext;
	private Timer reconnectTimer; // Automatic reconnect timer
	private static int reconnectDelay = 1000; // Reconnect delay, starts at 1
												// second
	private boolean reconnecting = false;
	private static Object clientLock = new Object(); // Simple lock
	private MqttSession mqttSession = new MqttSession();
	private ScheduledExecutorService executorService;
	private MqttPingSender pingSender;

	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively a
	 * list containing one or more servers can be specified using the
	 * {@link MqttConnectionOptions#setServerURIs(String[]) setServerURIs} method on
	 * MqttConnectOptions.
	 *
	 * <p>
	 * The <code>serverURI</code> parameter is typically used with the the
	 * <code>clientId</code> parameter to form a key. The key is used to store and
	 * reference messages while they are being delivered. Hence the serverURI
	 * specified on the constructor must still be specified even if a list of
	 * servers is specified on an MqttConnectOptions object. The serverURI on the
	 * constructor must remain the same across restarts of the client for delivery
	 * of messages to be maintained from a given client to a given server or set of
	 * servers.
	 *
	 * <p>
	 * The address of the server to connect to is specified as a URI. Two types of
	 * connection are supported <code>tcp://</code> for a TCP connection and
	 * <code>ssl://</code> for a TCP connection secured by SSL/TLS. For example:
	 * </p>
	 * <ul>
	 * <li><code>tcp://localhost:1883</code></li>
	 * <li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * <p>
	 * If the port is not specified, it will default to 1883 for
	 * <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that
	 * 65535 characters. It must be unique across all clients connecting to the same
	 * server. The clientId is used by the server to store data related to the
	 * client, hence it is important that the clientId remain the same when
	 * connecting to a server if durable subscriptions or reliable messaging are
	 * required.
	 * <p>
	 * A convenience method is provided to generate a random client id that should
	 * satisfy this criteria - {@link MqttConnectionOptions#generateClientId()}. As
	 * the client identifier is used by the server to identify a client when it
	 * reconnects, the client must use the same identifier between connections if
	 * durable subscriptions or reliable delivery of messages is required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the client
	 * will use in the following order:
	 * </p>
	 * <ul>
	 * <li><strong>Supplying an <code>SSLSocketFactory</code></strong> -
	 * applications can use
	 * {@link MqttConnectionOptions#setSocketFactory(SocketFactory)} to supply a
	 * factory with the appropriate SSL settings.</li>
	 * <li><strong>SSL Properties</strong> - applications can supply SSL settings as
	 * a simple Java Properties using
	 * {@link MqttConnectionOptions#setSSLProperties(Properties)}.</li>
	 * <li><strong>Use JVM settings</strong> - There are a number of standard Java
	 * system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>
	 * In Java ME, the platform settings are used for SSL connections.
	 * </p>
	 *
	 * <p>
	 * An instance of the default persistence mechanism
	 * {@link MqttDefaultFilePersistence} is used by the client. To specify a
	 * different persistence mechanism or to turn off persistence, use the
	 * {@link #MqttAsyncClient(String, String, MqttClientPersistence)} constructor.
	 *
	 * @param serverURI
	 *            the address of the server to connect to, specified as a URI. Can
	 *            be overridden using
	 *            {@link MqttConnectionOptions#setServerURIs(String[])}
	 * @param clientId
	 *            a client identifier that is unique on the server being connected
	 *            to
	 * @throws IllegalArgumentException
	 *             if the URI does not start with "tcp://", "ssl://" or "local://".
	 * @throws IllegalArgumentException
	 *             if the clientId is null or is greater than 65535 characters in
	 *             length
	 * @throws MqttException
	 *             if any other problem was encountered
	 */
	public MqttAsyncClient(String serverURI, String clientId) throws MqttException {
		this(serverURI, clientId, new MqttDefaultFilePersistence());
	}

	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively a
	 * list containing one or more servers can be specified using the
	 * {@link MqttConnectionOptions#setServerURIs(String[]) setServerURIs} method on
	 * MqttConnectOptions.
	 *
	 * <p>
	 * The <code>serverURI</code> parameter is typically used with the the
	 * <code>clientId</code> parameter to form a key. The key is used to store and
	 * reference messages while they are being delivered. Hence the serverURI
	 * specified on the constructor must still be specified even if a list of
	 * servers is specified on an MqttConnectOptions object. The serverURI on the
	 * constructor must remain the same across restarts of the client for delivery
	 * of messages to be maintained from a given client to a given server or set of
	 * servers.
	 *
	 * <p>
	 * The address of the server to connect to is specified as a URI. Two types of
	 * connection are supported <code>tcp://</code> for a TCP connection and
	 * <code>ssl://</code> for a TCP connection secured by SSL/TLS. For example:
	 * </p>
	 * <ul>
	 * <li><code>tcp://localhost:1883</code></li>
	 * <li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * <p>
	 * If the port is not specified, it will default to 1883 for
	 * <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that
	 * 65535 characters. It must be unique across all clients connecting to the same
	 * server. The clientId is used by the server to store data related to the
	 * client, hence it is important that the clientId remain the same when
	 * connecting to a server if durable subscriptions or reliable messaging are
	 * required.
	 * <p>
	 * A convenience method is provided to generate a random client id that should
	 * satisfy this criteria - {@link MqttConnectionOptions#generateClientId()}. As
	 * the client identifier is used by the server to identify a client when it
	 * reconnects, the client must use the same identifier between connections if
	 * durable subscriptions or reliable delivery of messages is required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the client
	 * will use in the following order:
	 * </p>
	 * <ul>
	 * <li><strong>Supplying an <code>SSLSocketFactory</code></strong> -
	 * applications can use
	 * {@link MqttConnectionOptions#setSocketFactory(SocketFactory)} to supply a
	 * factory with the appropriate SSL settings.</li>
	 * <li><strong>SSL Properties</strong> - applications can supply SSL settings as
	 * a simple Java Properties using
	 * {@link MqttConnectionOptions#setSSLProperties(Properties)}.</li>
	 * <li><strong>Use JVM settings</strong> - There are a number of standard Java
	 * system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>
	 * In Java ME, the platform settings are used for SSL connections.
	 * </p>
	 * <p>
	 * A persistence mechanism is used to enable reliable messaging. For messages
	 * sent at qualities of service (QoS) 1 or 2 to be reliably delivered, messages
	 * must be stored (on both the client and server) until the delivery of the
	 * message is complete. If messages are not safely stored when being delivered
	 * then a failure in the client or server can result in lost messages. A
	 * pluggable persistence mechanism is supported via the
	 * {@link MqttClientPersistence} interface. An implementer of this interface
	 * that safely stores messages must be specified in order for delivery of
	 * messages to be reliable. In addition
	 * {@link MqttConnectionOptions#setCleanSession(boolean)} must be set to false.
	 * In the event that only QoS 0 messages are sent or received or cleanSession is
	 * set to true then a safe store is not needed.
	 * </p>
	 * <p>
	 * An implementation of file-based persistence is provided in class
	 * {@link MqttDefaultFilePersistence} which will work in all Java SE based
	 * systems. If no persistence is needed, the persistence parameter can be
	 * explicitly set to <code>null</code>.
	 * </p>
	 *
	 * @param serverURI
	 *            the address of the server to connect to, specified as a URI. Can
	 *            be overridden using
	 *            {@link MqttConnectionOptions#setServerURIs(String[])}
	 * @param clientId
	 *            a client identifier that is unique on the server being connected
	 *            to
	 * @param persistence
	 *            the persistence class to use to store in-flight message. If null
	 *            then the default persistence mechanism is used
	 * @throws MqttException
	 *             if any other problem was encountered
	 */
	public MqttAsyncClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
		this(serverURI, clientId, persistence, null, null);
	}

	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively a
	 * list containing one or more servers can be specified using the
	 * {@link MqttConnectionOptions#setServerURIs(String[]) setServerURIs} method on
	 * MqttConnectOptions.
	 *
	 * <p>
	 * The <code>serverURI</code> parameter is typically used with the the
	 * <code>clientId</code> parameter to form a key. The key is used to store and
	 * reference messages while they are being delivered. Hence the serverURI
	 * specified on the constructor must still be specified even if a list of
	 * servers is specified on an MqttConnectOptions object. The serverURI on the
	 * constructor must remain the same across restarts of the client for delivery
	 * of messages to be maintained from a given client to a given server or set of
	 * servers.
	 *
	 * <p>
	 * The address of the server to connect to is specified as a URI. Two types of
	 * connection are supported <code>tcp://</code> for a TCP connection and
	 * <code>ssl://</code> for a TCP connection secured by SSL/TLS. For example:
	 * </p>
	 * <ul>
	 * <li><code>tcp://localhost:1883</code></li>
	 * <li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * <p>
	 * If the port is not specified, it will default to 1883 for
	 * <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that
	 * 65535 characters. It must be unique across all clients connecting to the same
	 * server. The clientId is used by the server to store data related to the
	 * client, hence it is important that the clientId remain the same when
	 * connecting to a server if durable subscriptions or reliable messaging are
	 * required.
	 * <p>
	 * A convenience method is provided to generate a random client id that should
	 * satisfy this criteria - {@link MqttConnectionOptions#generateClientId()}. As
	 * the client identifier is used by the server to identify a client when it
	 * reconnects, the client must use the same identifier between connections if
	 * durable subscriptions or reliable delivery of messages is required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the client
	 * will use in the following order:
	 * </p>
	 * <ul>
	 * <li><strong>Supplying an <code>SSLSocketFactory</code></strong> -
	 * applications can use
	 * {@link MqttConnectionOptions#setSocketFactory(SocketFactory)} to supply a
	 * factory with the appropriate SSL settings.</li>
	 * <li><strong>SSL Properties</strong> - applications can supply SSL settings as
	 * a simple Java Properties using
	 * {@link MqttConnectionOptions#setSSLProperties(Properties)}.</li>
	 * <li><strong>Use JVM settings</strong> - There are a number of standard Java
	 * system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>
	 * In Java ME, the platform settings are used for SSL connections.
	 * </p>
	 * <p>
	 * A persistence mechanism is used to enable reliable messaging. For messages
	 * sent at qualities of service (QoS) 1 or 2 to be reliably delivered, messages
	 * must be stored (on both the client and server) until the delivery of the
	 * message is complete. If messages are not safely stored when being delivered
	 * then a failure in the client or server can result in lost messages. A
	 * pluggable persistence mechanism is supported via the
	 * {@link MqttClientPersistence} interface. An implementer of this interface
	 * that safely stores messages must be specified in order for delivery of
	 * messages to be reliable. In addition
	 * {@link MqttConnectionOptions#setCleanSession(boolean)} must be set to false.
	 * In the event that only QoS 0 messages are sent or received or cleanSession is
	 * set to true then a safe store is not needed.
	 * </p>
	 * <p>
	 * An implementation of file-based persistence is provided in class
	 * {@link MqttDefaultFilePersistence} which will work in all Java SE based
	 * systems. If no persistence is needed, the persistence parameter can be
	 * explicitly set to <code>null</code>.
	 * </p>
	 *
	 * @param serverURI
	 *            the address of the server to connect to, specified as a URI. Can
	 *            be overridden using
	 *            {@link MqttConnectionOptions#setServerURIs(String[])}
	 * @param clientId
	 *            a client identifier that is unique on the server being connected
	 *            to
	 * @param persistence
	 *            the persistence class to use to store in-flight message. If null
	 *            then the default persistence mechanism is used
	 * @param pingSender
	 *            the {@link MqttPingSender} Implementation to handle timing and
	 *            sending Ping messages to the server.
	 * @param executorService
	 *            used for managing threads. If null then a newFixedThreadPool is
	 *            used.
	 * @throws IllegalArgumentException
	 *             if the URI does not start with "tcp://", "ssl://" or "local://"
	 * @throws IllegalArgumentException
	 *             if the clientId is null or is greater than 65535 characters in
	 *             length
	 * @throws MqttException
	 *             if any other problem was encountered
	 */
	public MqttAsyncClient(String serverURI, String clientId, MqttClientPersistence persistence,
			MqttPingSender pingSender, ScheduledExecutorService executorService) throws MqttException {
		final String methodName = "MqttAsyncClient";

		log.setResourceName(clientId);

		if (clientId != null) {
			// Count characters, surrogate pairs count as one character.
			int clientIdLength = 0;
			for (int i = 0; i < clientId.length() - 1; i++) {
				if (Character_isHighSurrogate(clientId.charAt(i)))
					i++;
				clientIdLength++;
			}
			if (clientIdLength > 65535) {
				throw new IllegalArgumentException("ClientId longer than 65535 characters");
			}
		} else {
			clientId = "";
		}



		MqttConnectionOptions.validateURI(serverURI);

		this.serverURI = serverURI;
		this.mqttSession.setClientId(clientId);

		this.persistence = persistence;
		if (this.persistence == null) {
			this.persistence = new MemoryPersistence();
		}

		this.executorService = executorService;
		if (this.executorService == null) {
			this.executorService = Executors.newScheduledThreadPool(10);
		}

		this.pingSender = pingSender;
		if (this.pingSender == null) {
			this.pingSender = new TimerPingSender(this.executorService);
		}

		// @TRACE 101=<init> ClientID={0} ServerURI={1} PersistenceType={2}
		log.fine(CLASS_NAME, methodName, "101", new Object[] { clientId, serverURI, persistence });

		this.persistence.open(clientId);
		this.comms = new ClientComms(this, this.persistence, this.pingSender, this.executorService, this.mqttSession);
		this.persistence.close();
		this.topics = new Hashtable<String, MqttTopic>();

	}

	/**
	 * @param ch
	 *            the character to check.
	 * @return returns 'true' if the character is a high-surrogate code unit
	 */
	protected static boolean Character_isHighSurrogate(char ch) {
		return (ch >= MIN_HIGH_SURROGATE) && (ch <= MAX_HIGH_SURROGATE);
	}

	/**
	 * Factory method to create an array of network modules, one for each of the
	 * supplied URIs
	 *
	 * @param address
	 *            the URI for the server.
	 * @param options
	 *            the {@link MqttConnectionOptions} for the connection.
	 * @return a network module appropriate to the specified address.
	 * @throws MqttException
	 *             if an exception occurs creating the network Modules
	 * @throws MqttSecurityException
	 *             if an issue occurs creating an SSL / TLS Socket
	 */
	protected NetworkModule[] createNetworkModules(String address, MqttConnectionOptions options)
			throws MqttException, MqttSecurityException {
		final String methodName = "createNetworkModules";
		// @TRACE 116=URI={0}
		log.fine(CLASS_NAME, methodName, "116", new Object[] { address });

		NetworkModule[] networkModules = null;
		String[] serverURIs = options.getServerURIs();
		String[] array = null;
		if (serverURIs == null) {
			array = new String[] { address };
		} else if (serverURIs.length == 0) {
			array = new String[] { address };
		} else {
			array = serverURIs;
		}

		networkModules = new NetworkModule[array.length];
		for (int i = 0; i < array.length; i++) {
			networkModules[i] = createNetworkModule(array[i], options);
		}

		log.fine(CLASS_NAME, methodName, "108");
		return networkModules;
	}

	/**
	 * Factory method to create the correct network module, based on the supplied
	 * address URI.
	 *
	 * @param address
	 *            the URI for the server.
	 * @param options
	 *            Connect options
	 * @return a network module appropriate to the specified address.
	 */
	private NetworkModule createNetworkModule(String address, MqttConnectionOptions options)
			throws MqttException, MqttSecurityException {
		final String methodName = "createNetworkModule";
		// @TRACE 115=URI={0}
		log.fine(CLASS_NAME, methodName, "115", new Object[] { address });

		NetworkModule netModule;
		SocketFactory factory = options.getSocketFactory();

		MqttConnectionOptions.UriType serverURIType = MqttConnectionOptions.validateURI(address);

		URI uri;
		try {
			uri = new URI(address);
			// If the returned uri contains no host and the address contains underscores,
			// then it's likely that Java did not parse the URI
			if (uri.getHost() == null && address.contains("_")) {
				try {
					final Field hostField = URI.class.getDeclaredField("host");
					hostField.setAccessible(true);
					// Get everything after the scheme://
					String shortAddress = address.substring(uri.getScheme().length() + 3);
					hostField.set(uri, getHostName(shortAddress));

				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					throw ExceptionHelper.createMqttException(e.getCause());
				}

			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Malformed URI: " + address + ", " + e.getMessage());
		}

		String host = uri.getHost();
		int port = uri.getPort(); // -1 if not defined

		switch (serverURIType) {
		case TCP:
			if (port == -1) {
				port = 1883;
			}
			if (factory == null) {
				factory = SocketFactory.getDefault();
			} else if (factory instanceof SSLSocketFactory) {
				throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}
			netModule = new TCPNetworkModule(factory, host, port, this.mqttSession.getClientId());
			((TCPNetworkModule) netModule).setConnectTimeout(options.getConnectionTimeout());
			break;
		case SSL:
			if (port == -1) {
				port = 8883;
			}
			SSLSocketFactoryFactory factoryFactory = null;
			if (factory == null) {
				// try {
				factoryFactory = new SSLSocketFactoryFactory();
				Properties sslClientProps = options.getSSLProperties();
				if (null != sslClientProps)
					factoryFactory.initialize(sslClientProps, null);
				factory = factoryFactory.createSocketFactory(null);
				// }
				// catch (MqttDirectException ex) {
				// throw ExceptionHelper.createMqttException(ex.getCause());
				// }
			} else if ((factory instanceof SSLSocketFactory) == false) {
				throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}

			// Create the network module...
			netModule = new SSLNetworkModule((SSLSocketFactory) factory, host, port, this.mqttSession.getClientId());
			((SSLNetworkModule) netModule).setSSLhandshakeTimeout(options.getConnectionTimeout());
			((SSLNetworkModule) netModule).setSSLHostnameVerifier(options.getSSLHostnameVerifier());
			// Ciphers suites need to be set, if they are available
			if (factoryFactory != null) {
				String[] enabledCiphers = factoryFactory.getEnabledCipherSuites(null);
				if (enabledCiphers != null) {
					((SSLNetworkModule) netModule).setEnabledCiphers(enabledCiphers);
				}
			}
			break;
		case WS:
			if (port == -1) {
				port = 80;
			}
			if (factory == null) {
				factory = SocketFactory.getDefault();
			} else if (factory instanceof SSLSocketFactory) {
				throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}
			netModule = new WebSocketNetworkModule(factory, address, host, port, this.mqttSession.getClientId());
			((WebSocketNetworkModule) netModule).setConnectTimeout(options.getConnectionTimeout());
			break;
		case WSS:
			if (port == -1) {
				port = 443;
			}
			SSLSocketFactoryFactory wSSFactoryFactory = null;
			if (factory == null) {
				wSSFactoryFactory = new SSLSocketFactoryFactory();
				Properties sslClientProps = options.getSSLProperties();
				if (null != sslClientProps)
					wSSFactoryFactory.initialize(sslClientProps, null);
				factory = wSSFactoryFactory.createSocketFactory(null);

			} else if ((factory instanceof SSLSocketFactory) == false) {
				throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}

			// Create the network module...
			netModule = new WebSocketSecureNetworkModule((SSLSocketFactory) factory, address, host, port, this.mqttSession.getClientId());
			((WebSocketSecureNetworkModule) netModule).setSSLhandshakeTimeout(options.getConnectionTimeout());
			// Ciphers suites need to be set, if they are available
			if (wSSFactoryFactory != null) {
				String[] enabledCiphers = wSSFactoryFactory.getEnabledCipherSuites(null);
				if (enabledCiphers != null) {
					((SSLNetworkModule) netModule).setEnabledCiphers(enabledCiphers);
				}
			}
			break;
		default:
			// This shouldn't happen, as long as validateURI() has been called.
			log.fine(CLASS_NAME, methodName, "119", new Object[] { address });
			netModule = null;
		}
		return netModule;
	}

	private String getHostName(String uri) {
		int portIndex = uri.indexOf(':');
		if (portIndex == -1) {
			portIndex = uri.indexOf('/');
		}
		if (portIndex == -1) {
			portIndex = uri.length();
		}
		return uri.substring(0, portIndex);
	}

	/**
	 * Connects to an MQTT server using the default options.
	 * <p>
	 * The default options are specified in {@link MqttConnectionOptions} class.
	 * </p>
	 *
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when the connect
	 *            completes. Use null if not required.
	 * @throws MqttSecurityException
	 *             for security related problems
	 * @throws MqttException
	 *             for non security related problems
	 * @return token used to track and wait for the connect to complete. The token
	 *         will be passed to any callback that has been set.
	 * @see #connect(MqttConnectionOptions, Object, MqttActionListener)
	 */
	public IMqttToken connect(Object userContext, MqttActionListener callback)
			throws MqttException, MqttSecurityException {
		return this.connect(new MqttConnectionOptions(), userContext, callback);
	}

	/**
	 * Connects to an MQTT server using the default options.
	 * <p>
	 * The default options are specified in {@link MqttConnectionOptions} class.
	 * </p>
	 *
	 * @throws MqttSecurityException
	 *             for security related problems
	 * @throws MqttException
	 *             for non security related problems
	 * @return token used to track and wait for the connect to complete. The token
	 *         will be passed to the callback methods if a callback is set.
	 * @see #connect(MqttConnectionOptions, Object, MqttActionListener)
	 */
	public IMqttToken connect() throws MqttException, MqttSecurityException {
		return this.connect(null, null);
	}

	/**
	 * Connects to an MQTT server using the provided connect options.
	 * <p>
	 * The connection will be established using the options specified in the
	 * {@link MqttConnectionOptions} parameter.
	 * </p>
	 *
	 * @param options
	 *            a set of connection parameters that override the defaults.
	 * @throws MqttSecurityException
	 *             for security related problems
	 * @throws MqttException
	 *             for non security related problems
	 * @return token used to track and wait for the connect to complete. The token
	 *         will be passed to any callback that has been set.
	 * @see #connect(MqttConnectionOptions, Object, MqttActionListener)
	 */
	public IMqttToken connect(MqttConnectionOptions options) throws MqttException, MqttSecurityException {
		return this.connect(options, null, null);
	}

	/**
	 * Connects to an MQTT server using the specified options.
	 * <p>
	 * The server to connect to is specified on the constructor. It is recommended
	 * to call {@link #setCallback(MqttCallback)} prior to connecting in order that
	 * messages destined for the client can be accepted as soon as the client is
	 * connected.
	 * </p>
	 * <p>
	 * The method returns control before the connect completes. Completion can be
	 * tracked by:
	 * </p>
	 * <ul>
	 * <li>Waiting on the returned token {@link IMqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link MqttActionListener}</li>
	 * </ul>
	 *
	 * @param options
	 *            a set of connection parameters that override the defaults.
	 * @param userContext
	 *            optional object for used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when the connect
	 *            completes. Use null if not required.
	 * @return token used to track and wait for the connect to complete. The token
	 *         will be passed to any callback that has been set.
	 * @throws MqttSecurityException
	 *             for security related problems
	 * @throws MqttException
	 *             for non security related problems including communication errors
	 */
	public IMqttToken connect(MqttConnectionOptions options, Object userContext, MqttActionListener callback)
			throws MqttException, MqttSecurityException {
		final String methodName = "connect";
		if (comms.isConnected()) {
			throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_CONNECTED);
		}
		if (comms.isConnecting()) {
			throw new MqttException(MqttClientException.REASON_CODE_CONNECT_IN_PROGRESS);
		}
		if (comms.isDisconnecting()) {
			throw new MqttException(MqttClientException.REASON_CODE_CLIENT_DISCONNECTING);
		}
		if (comms.isClosed()) {
			throw new MqttException(MqttClientException.REASON_CODE_CLIENT_CLOSED);
		}
		if (options == null) {
			options = new MqttConnectionOptions();
		}
		this.connOpts = options;
		this.userContext = userContext;
		final boolean automaticReconnect = options.isAutomaticReconnect();

		// @TRACE 103=cleanSession={0} connectionTimeout={1} TimekeepAlive={2}
		// userName={3} password={4} will={5} userContext={6} callback={7}
		log.fine(CLASS_NAME, methodName, "103",
				new Object[] { Boolean.valueOf(options.isCleanSession()), new Integer(options.getConnectionTimeout()),
						new Integer(options.getKeepAliveInterval()), options.getUserName(),
						((null == options.getPassword()) ? "[null]" : "[notnull]"),
						((null == options.getWillMessage()) ? "[null]" : "[notnull]"), userContext, callback });
		comms.setNetworkModules(createNetworkModules(serverURI, options));
		comms.setReconnectCallback(new MqttReconnectCallback(automaticReconnect));

		// Insert our own callback to iterate through the URIs till the connect
		// succeeds
		MqttToken userToken = new MqttToken(getClientId());
		ConnectActionListener connectActionListener = new ConnectActionListener(this, persistence, comms, options,
				userToken, userContext, callback, reconnecting, mqttSession);
		userToken.setActionCallback(connectActionListener);
		userToken.setUserContext(this);

		// If we are using the MqttCallbackExtended, set it on the
		// connectActionListener
		if (this.mqttCallback instanceof MqttCallbackExtended) {
			connectActionListener.setMqttCallbackExtended((MqttCallbackExtended) this.mqttCallback);
		}

		if(this.connOpts.isCleanSession()) {
			this.mqttSession.clearSession();
		}

		if(this.connOpts.isCleanSession()) {
			this.mqttSession.clearSession();
		}

		comms.setNetworkModuleIndex(0);
		connectActionListener.connect();

		return userToken;
	}

	/**
	 * Disconnects from the server.
	 * <p>
	 * An attempt is made to quiesce the client allowing outstanding work to
	 * complete before disconnecting. It will wait for a maximum of 30 seconds for
	 * work to quiesce before disconnecting. This method must not be called from
	 * inside {@link MqttCallback} methods.
	 * </p>
	 *
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when the disconnect
	 *            completes. Use null if not required.
	 * @return token used to track and wait for the disconnect to complete. The
	 *         token will be passed to any callback that has been set.
	 * @throws MqttException
	 *             for problems encountered while disconnecting
	 * @see #disconnect(long, Object, MqttActionListener, int, Integer, String,
	 *      ArrayList)
	 *
	 */
	public IMqttToken disconnect(Object userContext, MqttActionListener callback) throws MqttException {
		return this.disconnect(QUIESCE_TIMEOUT, userContext, callback, MqttReturnCode.RETURN_CODE_SUCCESS, null, null,
				null);
	}

	/**
	 * Disconnects from the server.
	 * <p>
	 * An attempt is made to quiesce the client allowing outstanding work to
	 * complete before disconnecting. It will wait for a maximum of 30 seconds for
	 * work to quiesce before disconnecting. This method must not be called from
	 * inside {@link MqttCallback} methods.
	 * </p>
	 *
	 * @return token used to track and wait for disconnect to complete. The token
	 *         will be passed to any callback that has been set.
	 * @throws MqttException
	 *             for problems encountered while disconnecting
	 * @see #disconnect(long, Object, MqttActionListener, int, Integer, String,
	 *      ArrayList)
	 */
	public IMqttToken disconnect() throws MqttException {
		return this.disconnect(null, null);
	}

	/**
	 * Disconnects from the server.
	 * <p>
	 * An attempt is made to quiesce the client allowing outstanding work to
	 * complete before disconnecting. It will wait for a maximum of the specified
	 * quiesce time for work to complete before disconnecting. This method must not
	 * be called from inside {@link MqttCallback} methods.
	 * </p>
	 *
	 * @param quiesceTimeout
	 *            the amount of time in milliseconds to allow for existing work to
	 *            finish before disconnecting. A value of zero or less means the
	 *            client will not quiesce.
	 * @return token used to track and wait for disconnect to complete. The token
	 *         will be passed to the callback methods if a callback is set.
	 * @throws MqttException
	 *             for problems encountered while disconnecting
	 * @see #disconnect(long, Object, MqttActionListener, int, Integer, String,
	 *      ArrayList)
	 */
	public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
		return this.disconnect(quiesceTimeout, null, null, MqttReturnCode.RETURN_CODE_SUCCESS, null, null, null);
	}

	/**
	 * Disconnects from the server.
	 * <p>
	 * The client will wait for {@link MqttCallback} methods to complete. It will
	 * then wait for up to the quiesce timeout to allow for work which has already
	 * been initiated to complete. For instance when a QoS 2 message has started
	 * flowing to the server but the QoS 2 flow has not completed.It prevents new
	 * messages being accepted and does not send any messages that have been
	 * accepted but not yet started delivery across the network to the server. When
	 * work has completed or after the quiesce timeout, the client will disconnect
	 * from the server. If the cleanSession flag was set to false and is set to
	 * false the next time a connection is made QoS 1 and 2 messages that were not
	 * previously delivered will be delivered.
	 * </p>
	 * <p>
	 * This method must not be called from inside {@link MqttCallback} methods.
	 * </p>
	 * <p>
	 * The method returns control before the disconnect completes. Completion can be
	 * tracked by:
	 * </p>
	 * <ul>
	 * <li>Waiting on the returned token {@link IMqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link MqttActionListener}</li>
	 * </ul>
	 *
	 * @param quiesceTimeout
	 *            the amount of time in milliseconds to allow for existing work to
	 *            finish before disconnecting. A value of zero or less means the
	 *            client will not quiesce.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when the disconnect
	 *            completes. Use null if not required.
	 * @param reasonCode
	 *            the disconnection reason code.
	 * @param sessionExpiryInterval
	 *            optional property to be sent in the disconnect packet to the
	 *            server. Use null if not required.
	 * @param reasonString
	 *            optional property to be sent in the disconnect packet to the
	 *            server. Use null if not required.
	 * @param userDefinedProperties
	 *            {@link ArrayList} of {@link UserProperty} to be sent to the
	 *            server. Use null if not required.
	 * @return token used to track and wait for the connect to complete. The token
	 *         will be passed to any callback that has been set.
	 * @throws MqttException
	 *             for problems encountered while disconnecting
	 */
	public IMqttToken disconnect(long quiesceTimeout, Object userContext, MqttActionListener callback, int reasonCode,
			Integer sessionExpiryInterval, String reasonString, ArrayList<UserProperty> userDefinedProperties)
			throws MqttException {
		final String methodName = "disconnect";
		// @TRACE 104=> quiesceTimeout={0} userContext={1} callback={2}
		log.fine(CLASS_NAME, methodName, "104", new Object[] { new Long(quiesceTimeout), userContext, callback });

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);

		MqttDisconnect disconnect = new MqttDisconnect(reasonCode);

		if (sessionExpiryInterval != null) {
			disconnect.setSessionExpiryInterval(sessionExpiryInterval);
		}

		if (reasonString != null) {
			disconnect.setReasonString(reasonString);
		}

		if (userDefinedProperties != null) {
			disconnect.setUserDefinedProperties(userDefinedProperties);
		}

		try {
			comms.disconnect(disconnect, quiesceTimeout, token);
		} catch (MqttException ex) {
			// @TRACE 105=< exception
			log.fine(CLASS_NAME, methodName, "105", null, ex);
			throw ex;
		}
		// @TRACE 108=<
		log.fine(CLASS_NAME, methodName, "108");

		return token;
	}

	/**
	 * Disconnects from the server forcibly to reset all the states. Could be useful
	 * when disconnect attempt failed.
	 * <p>
	 * Because the client is able to establish the TCP/IP connection to a none MQTT
	 * server and it will certainly fail to send the disconnect packet. It will wait
	 * for a maximum of 30 seconds for work to quiesce before disconnecting and wait
	 * for a maximum of 10 seconds for sending the disconnect packet to server.
	 *
	 * @throws MqttException
	 *             if any unexpected error
	 * @since 0.4.1
	 */
	public void disconnectForcibly() throws MqttException {
		disconnectForcibly(QUIESCE_TIMEOUT, DISCONNECT_TIMEOUT, MqttReturnCode.RETURN_CODE_SUCCESS, null, null, null);
	}

	/**
	 * Disconnects from the server forcibly to reset all the states. Could be useful
	 * when disconnect attempt failed.
	 * <p>
	 * Because the client is able to establish the TCP/IP connection to a none MQTT
	 * server and it will certainly fail to send the disconnect packet. It will wait
	 * for a maximum of 30 seconds for work to quiesce before disconnecting.
	 *
	 * @param disconnectTimeout
	 *            the amount of time in milliseconds to allow send disconnect packet
	 *            to server.
	 * @throws MqttException
	 *             if any unexpected error
	 * @since 0.4.1
	 */
	public void disconnectForcibly(long disconnectTimeout) throws MqttException {
		disconnectForcibly(QUIESCE_TIMEOUT, disconnectTimeout, MqttReturnCode.RETURN_CODE_SUCCESS, null, null, null);
	}

	/**
	 * Disconnects from the server forcibly to reset all the states. Could be useful
	 * when disconnect attempt failed.
	 * <p>
	 * Because the client is able to establish the TCP/IP connection to a none MQTT
	 * server and it will certainly fail to send the disconnect packet.
	 *
	 * @param quiesceTimeout
	 *            the amount of time in milliseconds to allow for existing work to
	 *            finish before disconnecting. A value of zero or less means the
	 *            client will not quiesce.
	 * @param disconnectTimeout
	 *            the amount of time in milliseconds to allow send disconnect packet
	 *            to server.
	 * @param reasonCode
	 *            the disconnection reason code.
	 * @param sessionExpiryInterval
	 *            optional property to be sent in the disconnect packet to the
	 *            server. Use null if not required.
	 * @param reasonString
	 *            optional property to be sent in the disconnect packet to the
	 *            server. Use null if not required.
	 * @param userDefinedProperties
	 *            {@link ArrayList} of {@link UserProperty} to be sent to the
	 *            server. Use null if not required.
	 * @throws MqttException
	 *             if any unexpected error
	 * @since 0.4.1
	 */
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout, int reasonCode,
			Integer sessionExpiryInterval, String reasonString, ArrayList<UserProperty> userDefinedProperties)
			throws MqttException {
		comms.disconnectForcibly(quiesceTimeout, disconnectTimeout, reasonCode, sessionExpiryInterval, reasonString,
				userDefinedProperties);
	}

	/**
	 * Disconnects from the server forcibly to reset all the states. Could be useful
	 * when disconnect attempt failed.
	 * <p>
	 * Because the client is able to establish the TCP/IP connection to a none MQTT
	 * server and it will certainly fail to send the disconnect packet.
	 *
	 * @param quiesceTimeout
	 *            the amount of time in milliseconds to allow for existing work to
	 *            finish before disconnecting. A value of zero or less means the
	 *            client will not quiesce.
	 * @param disconnectTimeout
	 *            the amount of time in milliseconds to allow send disconnect packet
	 *            to server.
	 * @param sendDisconnectPacket
	 *            if true, will send the disconnect packet to the server
	 * @throws MqttException
	 *             if any unexpected error
	 */
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout, boolean sendDisconnectPacket)
			throws MqttException {
		comms.disconnectForcibly(quiesceTimeout, disconnectTimeout, sendDisconnectPacket,
				MqttReturnCode.RETURN_CODE_SUCCESS, null, null, null);
	}

	/**
	 * Determines if this client is currently connected to the server.
	 *
	 * @return <code>true</code> if connected, <code>false</code> otherwise.
	 */
	public boolean isConnected() {
		return comms.isConnected();
	}

	/**
	 * Returns the client ID used by this client.
	 * <p>
	 * All clients connected to the same server or server farm must have a unique
	 * ID.
	 * </p>
	 *
	 * @return the client ID used by this client.
	 */
	public String getClientId() {
		return this.mqttSession.getClientId();
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Returns the address of the server used by this client.
	 * <p>
	 * The format of the returned String is the same as that used on the
	 * constructor.
	 * </p>
	 *
	 * @return the server's address, as a URI String.
	 * @see MqttAsyncClient#MqttAsyncClient(String, String)
	 */
	public String getServerURI() {
		return serverURI;
	}

	/**
	 * Returns the currently connected Server URI Implemented due to:
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=481097
	 *
	 * Where getServerURI only returns the URI that was provided in
	 * MqttAsyncClient's constructor, getCurrentServerURI returns the URI of the
	 * Server that the client is currently connected to. This would be different in
	 * scenarios where multiple server URIs have been provided to the
	 * MqttConnectOptions.
	 *
	 * @return the currently connected server URI
	 */
	public String getCurrentServerURI() {
		return comms.getNetworkModules()[comms.getNetworkModuleIndex()].getServerURI();
	}

	/**
	 * Get a topic object which can be used to publish messages.
	 * <p>
	 * There are two alternative methods that should be used in preference to this
	 * one when publishing a message:
	 * </p>
	 * <ul>
	 * <li>{@link MqttAsyncClient#publish(String, MqttMessage)} to publish a message
	 * in a non-blocking manner or</li>
	 * <li>{@link MqttLegacyBlockingClient#publish(String, MqttMessage)} to publish
	 * a message in a blocking manner</li>
	 * </ul>
	 * <p>
	 * When you build an application, the design of the topic tree should take into
	 * account the following principles of topic name syntax and semantics:
	 * </p>
	 *
	 * <ul>
	 * <li>A topic must be at least one character long.</li>
	 * <li>Topic names are case sensitive. For example, <em>ACCOUNTS</em> and
	 * <em>Accounts</em> are two different topics.</li>
	 * <li>Topic names can include the space character. For example, <em>Accounts
	 * payable</em> is a valid topic.</li>
	 * <li>A leading "/" creates a distinct topic. For example, <em>/finance</em> is
	 * different from <em>finance</em>. <em>/finance</em> matches "+/+" and "/+",
	 * but not "+".</li>
	 * <li>Do not include the null character (Unicode \x0000) in any topic.</li>
	 * </ul>
	 *
	 * <p>
	 * The following principles apply to the construction and content of a topic
	 * tree:
	 * </p>
	 *
	 * <ul>
	 * <li>The length is limited to 64k but within that there are no limits to the
	 * number of levels in a topic tree.</li>
	 * <li>There can be any number of root nodes; that is, there can be any number
	 * of topic trees.</li>
	 * </ul>
	 *
	 * @param topic
	 *            the topic to use, for example "finance/stock/ibm".
	 * @return an MqttTopic object, which can be used to publish messages to the
	 *         topic.
	 * @throws IllegalArgumentException
	 *             if the topic contains a '+' or '#' wildcard character.
	 */
	protected MqttTopic getTopic(String topic) {
		MqttTopic.validate(topic, false/* wildcards NOT allowed */);

		MqttTopic result = (MqttTopic) topics.get(topic);
		if (result == null) {
			result = new MqttTopic(topic, comms);
			topics.put(topic, result);
		}
		return result;
	}

	/*
	 * (non-Javadoc) Check and send a ping if needed. <p>By default, client sends
	 * PingReq to server to keep the connection to server. For some platforms which
	 * cannot use this mechanism, such as Android, developer needs to handle the
	 * ping request manually with this method. </p>
	 *
	 * @throws MqttException for other errors encountered while publishing the
	 * message.
	 */
	public IMqttToken checkPing(Object userContext, MqttActionListener callback) throws MqttException {
		final String methodName = "ping";
		MqttToken token;
		// @TRACE 117=>
		log.fine(CLASS_NAME, methodName, "117");

		token = comms.checkForActivity();
		// @TRACE 118=<
		log.fine(CLASS_NAME, methodName, "118");

		return token;
	}

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener) throws
	 *      MqttException
	 *
	 * @param topicFilter
	 *            the topic to subscribe to, which can include wildcards.
	 * @param qos
	 *            the maximum quality of service at which to subscribe. Messages
	 *            published at a lower quality of service will be received at the
	 *            published QoS. Messages published at a higher quality of service
	 *            will be received using the QoS specified on the subscribe.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when subscribe has
	 *            completed
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	public IMqttToken subscribe(String topicFilter, int qos, Object userContext, MqttActionListener callback)
			throws MqttException {
		return this.subscribe(new MqttSubscription[] { new MqttSubscription(topicFilter, qos) }, userContext, callback,
				0, null);
	}

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener)
	 *
	 * @param topicFilter
	 *            the topic to subscribe to, which can include wildcards.
	 * @param qos
	 *            the maximum quality of service at which to subscribe. Messages
	 *            published at a lower quality of service will be received at the
	 *            published QoS. Messages published at a higher quality of service
	 *            will be received using the QoS specified on the subscribe.
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	public IMqttToken subscribe(String topicFilter, int qos) throws MqttException {
		return this.subscribe(new MqttSubscription[] { new MqttSubscription(topicFilter, qos) }, null, null, 0, null);
	}

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener)
	 *
	 * @param subscriptions
	 *            one or more {@link MqttSubscription} defining the subscription to
	 *            be made.
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	public IMqttToken subscribe(MqttSubscription[] subscriptions) throws MqttException {
		return this.subscribe(subscriptions, null, null, 0, null);
	}

	/**
	 * Subscribes to multiple topics, each of which may include wildcards.
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 * <p>
	 * The {@link #setCallback(MqttCallback)} method should be called before this
	 * method, otherwise any received messages will be discarded.
	 * </p>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true when
	 * when connecting to the server then the subscription remains in place until
	 * either:
	 * </p>
	 *
	 * <ul>
	 * <li>The client disconnects</li>
	 * <li>An unsubscribe method is called to un-subscribe the topic</li>
	 * </ul>
	 *
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false when
	 * connecting to the server then the subscription remains in place until either:
	 * </p>
	 * <ul>
	 * <li>An unsubscribe method is called to unsubscribe the topic</li>
	 * <li>The next time the client connects with cleanSession set to true</li>
	 * </ul>
	 * <p>
	 * With cleanSession set to false the MQTT server will store messages on behalf
	 * of the client when the client is not connected. The next time the client
	 * connects with the <b>same client ID</b> the server will deliver the stored
	 * messages to the client.
	 * </p>
	 *
	 * <p>
	 * The "topic filter" string used when subscribing may contain special
	 * characters, which allow you to subscribe to multiple topics at once.
	 * </p>
	 * <p>
	 * The topic level separator is used to introduce structure into the topic, and
	 * can therefore be specified within the topic for that purpose. The multi-level
	 * wildcard and single-level wildcard can be used for subscriptions, but they
	 * cannot be used within a topic by the publisher of a message.
	 * <dl>
	 * <dt>Topic level separator</dt>
	 * <dd>The forward slash (/) is used to separate each level within a topic tree
	 * and provide a hierarchical structure to the topic space. The use of the topic
	 * level separator is significant when the two wildcard characters are
	 * encountered in topics specified by subscribers.</dd>
	 *
	 * <dt>Multi-level wildcard</dt>
	 * <dd>
	 * <p>
	 * The number sign (#) is a wildcard character that matches any number of levels
	 * within a topic. For example, if you subscribe to
	 * <span><span class="filepath">finance/stock/ibm/#</span></span>, you receive
	 * messages on these topics:
	 * </p>
	 * <ul>
	 * <li>finance/stock/ibm</li>
	 * <li>finance/stock/ibm/closingprice</li>
	 * <li>finance/stock/ibm/currentprice</li>
	 * </ul>
	 * <p>
	 * The multi-level wildcard can represent zero or more levels. Therefore,
	 * <em>finance/#</em> can also match the singular <em>finance</em>, where
	 * <em>#</em> represents zero levels. The topic level separator is meaningless
	 * in this context, because there are no levels to separate.
	 * </p>
	 *
	 * <p>
	 * The <span>multi-level</span> wildcard can be specified only on its own or
	 * next to the topic level separator character. Therefore, <em>#</em> and
	 * <em>finance/#</em> are both valid, but <em>finance#</em> is not valid.
	 * <span>The multi-level wildcard must be the last character used within the
	 * topic tree. For example, <em>finance/#</em> is valid but
	 * <em>finance/#/closingprice</em> is not valid.</span>
	 * </p>
	 * </dd>
	 *
	 * <dt>Single-level wildcard</dt>
	 * <dd>
	 * <p>
	 * The plus sign (+) is a wildcard character that matches only one topic level.
	 * For example, <em>finance/stock/+</em> matches <em>finance/stock/ibm</em> and
	 * <em>finance/stock/xyz</em>, but not <em>finance/stock/ibm/closingprice</em>.
	 * Also, because the single-level wildcard matches only a single level,
	 * <em>finance/+</em> does not match <em>finance</em>.
	 * </p>
	 *
	 * <p>
	 * Use the single-level wildcard at any level in the topic tree, and in
	 * conjunction with the multilevel wildcard. Specify the single-level wildcard
	 * next to the topic level separator, except when it is specified on its own.
	 * Therefore, <em>+</em> and <em>finance/+</em> are both valid, but
	 * <em>finance+</em> is not valid. <span>The single-level wildcard can be used
	 * at the end of the topic tree or within the topic tree. For example,
	 * <em>finance/+</em> and <em>finance/+/ibm</em> are both valid.</span>
	 * </p>
	 * </dd>
	 * </dl>
	 * <p>
	 * The method returns control before the subscribe completes. Completion can be
	 * tracked by:
	 * </p>
	 * <ul>
	 * <li>Waiting on the supplied token {@link MqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link MqttActionListener} to this method</li>
	 * </ul>
	 *
	 * @param subscriptions
	 *            one or more {@link MqttSubscription} defining the subscription to
	 *            be made.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when subscribe has
	 *            completed
	 * @param subscriptionIdentifier
	 *            optional integer representing the identifier for the subscription.
	 * @param userProperties
	 *            optional {@link List} of {@link UserProperty} objects containing
	 *            the User Defined Properties for the subscription.
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 * @throws IllegalArgumentException
	 *             if the two supplied arrays are not the same size.
	 */
	public IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			int subscriptionIdentifier, List<UserProperty> userProperties) throws MqttException {
		final String methodName = "subscribe";

		// remove any message handlers for individual topics
		for (int i = 0; i < subscriptions.length; ++i) {
			this.comms.removeMessageListener(subscriptions[i].getTopic());
		}

		// Only Generate Log string if we are logging at FINE level
		if (log.isLoggable(Logger.FINE)) {
			StringBuffer subs = new StringBuffer();
			for (int i = 0; i < subscriptions.length; i++) {
				if (i > 0) {
					subs.append(", ");
				}
				subs.append(subscriptions[i].toString());

				// Check if the topic filter is valid before subscribing
				MqttTopic.validate(subscriptions[i].getTopic(), true/* allow wildcards */);
			}
			// @TRACE 106=Subscribe topicFilter={0} userContext={1} callback={2}
			log.fine(CLASS_NAME, methodName, "106", new Object[] { subs.toString(), userContext, callback });
		}

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		// TODO - Somehow refactor this....
		// token.internalTok.setTopics(topicFilters);
		// TODO - Build up MQTT Subscriptions properly here

		MqttSubscribe register = new MqttSubscribe(subscriptions);
		if (subscriptionIdentifier != 0) {
			register.setSubscriptionIdentifier(subscriptionIdentifier);
		}
		if (userProperties != null) {
			register.setUserDefinedProperties(userProperties);
		}

		comms.sendNoWait(register, token);
		// @TRACE 109=<
		log.fine(CLASS_NAME, methodName, "109");

		return token;
	}

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener)
	 *
	 * @param subscription
	 *            a {@link MqttSubscription} defining the subscription to be made.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when subscribe has
	 *            completed
	 * @param messageListener
	 *            a callback to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	public IMqttToken subscribe(MqttSubscription mqttSubscription, Object userContext, MqttActionListener callback,
			IMqttMessageListener messageListener, int subscriptionIdentifier, List<UserProperty> userProperties)
			throws MqttException {

		return this.subscribe(new MqttSubscription[] { mqttSubscription }, userContext, callback,
				messageListener , subscriptionIdentifier, userProperties);
	}

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener)
	 *
	 * @param subscription
	 *            a {@link MqttSubscription} defining the subscription to be made.
	 * @param messageListener
	 *            a callback to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	public IMqttToken subscribe(MqttSubscription subscription, IMqttMessageListener messageListener)
			throws MqttException {
		return this.subscribe(new MqttSubscription[] { subscription }, null, null,
				 messageListener , 0, null);
	}

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener)
	 *
	 ** @param subscriptions
	 *            one or more {@link MqttSubscription} defining the subscription to
	 *            be made.
	 * @param messageListener
	 *            a callback to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	public IMqttToken subscribe(MqttSubscription[] subscriptions, IMqttMessageListener messageListener)
			throws MqttException {
		return this.subscribe(subscriptions, null, null, messageListener, 0, null);
	}

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener)
	 *
	 * @param subscriptions
	 *            one or more {@link MqttSubscription} defining the subscription to
	 *            be made.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when subscribe has
	 *            completed
	 * @param messageListeners
	 *            one or more callbacks to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	public IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			IMqttMessageListener[] messageListeners, List<UserProperty> userProperties)
			throws MqttException {

		IMqttToken token = this.subscribe(subscriptions, userContext, callback, 0, userProperties);

		// add message handlers to the list for this client
		for (int i = 0; i < subscriptions.length; ++i) {
			this.comms.setMessageListener(null, subscriptions[i].getTopic(), messageListeners[i]);
		}

		return token;
	}

		IMqttToken token = this.subscribe(subscriptions, userContext, callback, 0, userProperties);

		// add message handlers to the list for this client
		for (int i = 0; i < subscriptions.length; ++i) {
			this.comms.setMessageListener(null, subscriptions[i].getTopic(), messageListeners[i]);
		}

		return token;
	}

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener)
	 *
	 * @param subscriptions
	 *            one or more {@link MqttSubscription} defining the subscription to
	 *            be made.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when subscribe has
	 *            completed
	 * @param messageListeners
	 *            one or more callbacks to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	public IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			IMqttMessageListener messageListener, int subscriptionIdentifier, List<UserProperty> userProperties)
			throws MqttException {

		int subId = subscriptionIdentifier;


			// Automatic Subscription Identifier Assignment is enabled
			if(connOpts.useSubscriptionIdentifiers()) {

				// Application is overriding the subscription Identifier
				if(subId != 0) {
					// Check that we are not already using this ID, else throw Illegal Argument Exception
					if(this.comms.doesSubscriptionIdentifierExist(subId)) {
						throw new IllegalArgumentException("The Subscription Identifier " + subId + " already exists.");
					}


				} else {
					// Automatically assign new ID and link to callback.
					subId = this.mqttSession.getNextSubscriptionIdentifier();
				}
			}

		IMqttToken token = this.subscribe(subscriptions, userContext, callback, subId, userProperties);

		// add message handlers to the list for this client
		for (int i = 0; i < subscriptions.length; ++i) {
			this.comms.setMessageListener(subId, subscriptions[i].getTopic(), messageListener);
		}

		return token;
	}

	/**
	 * Requests the server unsubscribe the client from a topics.
	 *
	 * @see #unsubscribe(String[], Object, MqttActionListener)
	 *
	 * @param topicFilter
	 *            the topic to unsubscribe from. It must match a topicFilter
	 *            specified on an earlier subscribe.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when unsubscribe has
	 *            completed
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error unregistering the subscription.
	 */
	public IMqttToken unsubscribe(String topicFilter, Object userContext, MqttActionListener callback)
			throws MqttException {
		return unsubscribe(new String[] { topicFilter }, userContext, callback);
	}

	/**
	 * Requests the server unsubscribe the client from a topic.
	 *
	 * @see #unsubscribe(String[], Object, MqttActionListener)
	 * @param topicFilter
	 *            the topic to unsubscribe from. It must match a topicFilter
	 *            specified on an earlier subscribe.
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error unregistering the subscription.
	 */
	public IMqttToken unsubscribe(String topicFilter) throws MqttException {
		return unsubscribe(new String[] { topicFilter }, null, null);
	}

	/**
	 * Requests the server unsubscribe the client from one or more topics.
	 *
	 * @see #unsubscribe(String[], Object, MqttActionListener)
	 *
	 * @param topicFilters
	 *            one or more topics to unsubscribe from. Each topicFilter must
	 *            match one specified on an earlier subscribe. *
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error unregistering the subscription.
	 */
	public IMqttToken unsubscribe(String[] topicFilters) throws MqttException {
		return unsubscribe(topicFilters, null, null);
	}

	/**
	 * Requests the server unsubscribe the client from one or more topics.
	 * <p>
	 * Unsubcribing is the opposite of subscribing. When the server receives the
	 * unsubscribe request it looks to see if it can find a matching subscription
	 * for the client and then removes it. After this point the server will send no
	 * more messages to the client for this subscription.
	 * </p>
	 * <p>
	 * The topic(s) specified on the unsubscribe must match the topic(s) specified
	 * in the original subscribe request for the unsubscribe to succeed
	 * </p>
	 * <p>
	 * The method returns control before the unsubscribe completes. Completion can
	 * be tracked by:
	 * </p>
	 *
	 * <ul>
	 * <li>Waiting on the returned token {@link MqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link MqttActionListener} to this method</li>
	 * </ul>
	 *
	 * @param topicFilters
	 *            one or more topics to unsubscribe from. Each topicFilter must
	 *            match one specified on an earlier subscribe.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when unsubscribe has
	 *            completed
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error unregistering the subscription.
	 */
	public IMqttToken unsubscribe(String[] topicFilters, Object userContext, MqttActionListener callback)
			throws MqttException {
		final String methodName = "unsubscribe";

		// Only Generate Log string if we are logging at FINE level
		if (log.isLoggable(Logger.FINE)) {
			String subs = "";
			for (int i = 0; i < topicFilters.length; i++) {
				if (i > 0) {
					subs += ", ";
				}
				subs += topicFilters[i];
			}

			// @TRACE 107=Unsubscribe topic={0} userContext={1} callback={2}
			log.fine(CLASS_NAME, methodName, "107", new Object[] { subs, userContext, callback });
		}

		for (int i = 0; i < topicFilters.length; i++) {
			// Check if the topic filter is valid before unsubscribing
			// Although we already checked when subscribing, but invalid
			// topic filter is meanless for unsubscribing, just prohibit it
			// to reduce unnecessary control packet send to broker.
			MqttTopic.validate(topicFilters[i], true/* allow wildcards */);
		}

		// remove message handlers from the list for this client
		for (int i = 0; i < topicFilters.length; ++i) {
			this.comms.removeMessageListener(topicFilters[i]);
		}

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.internalTok.setTopics(topicFilters);

		MqttUnsubscribe unregister = new MqttUnsubscribe(topicFilters);

		comms.sendNoWait(unregister, token);
		// @TRACE 110=<
		log.fine(CLASS_NAME, methodName, "110");

		return token;
	}

	/**
	 * Sets a callback listener to use for events that happen asynchronously.
	 * <p>
	 * There are a number of events that the listener will be notified about. These
	 * include:
	 * </p>
	 * <ul>
	 * <li>A new message has arrived and is ready to be processed</li>
	 * <li>The connection to the server has been lost</li>
	 * <li>Delivery of a message to the server has completed</li>
	 * </ul>
	 * <p>
	 * Other events that track the progress of an individual operation such as
	 * connect and subscribe can be tracked using the {@link MqttToken} returned
	 * from each non-blocking method or using setting a {@link MqttActionListener}
	 * on the non-blocking method.
	 * <p>
	 *
	 * @see MqttCallback
	 * @param callback
	 *            which will be invoked for certain asynchronous events
	 */
	public void setCallback(MqttCallback callback) {
		this.mqttCallback = callback;
		comms.setCallback(callback);
	}

	/**
	 * If manualAcks is set to true, then on completion of the messageArrived
	 * callback the MQTT acknowledgements are not sent. You must call
	 * messageArrivedComplete to send those acknowledgements. This allows finer
	 * control over when the acks are sent. The default behaviour, when manualAcks
	 * is false, is to send the MQTT acknowledgements automatically at the
	 * successful completion of the messageArrived callback method.
	 *
	 * @param manualAcks
	 *            if set to true MQTT acknowledgements are not sent
	 */
	public void setManualAcks(boolean manualAcks) {
		comms.setManualAcks(manualAcks);
	}

	/**
	 * Indicate that the application has completed processing the message with id
	 * messageId. This will cause the MQTT acknowledgement to be sent to the server.
	 *
	 * @param messageId
	 *            the MQTT message id to be acknowledged
	 * @param qos
	 *            the MQTT QoS of the message to be acknowledged
	 * @throws MqttException
	 *             if there was a problem sending the acknowledgement
	 */
	public void messageArrivedComplete(int messageId, int qos) throws MqttException {
		comms.messageArrivedComplete(messageId, qos);
	}

	/**
	 * Returns the delivery tokens for any outstanding publish operations.
	 * <p>
	 * If a client has been restarted and there are messages that were in the
	 * process of being delivered when the client stopped this method returns a
	 * token for each in-flight message enabling the delivery to be tracked
	 * Alternately the {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)}
	 * callback can be used to track the delivery of outstanding messages.
	 * </p>
	 * <p>
	 * If a client connects with cleanSession true then there will be no delivery
	 * tokens as the cleanSession option deletes all earlier state. For state to be
	 * remembered the client must connect with cleanSession set to false
	 * </P>
	 *
	 * @return zero or more delivery tokens
	 */
	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return comms.getPendingDeliveryTokens();
	}

	/**
	 * Publishes a message to a topic on the server.
	 * <p>
	 * A convenience method, which will create a new {@link MqttMessage} object with
	 * a byte array payload and the specified QoS, and then publish it.
	 * </p>
	 *
	 * @param topic
	 *            to deliver the message to, for example "finance/stock/ibm".
	 * @param payload
	 *            the byte array to use as the payload
	 * @param qos
	 *            the Quality of Service to deliver the message at. Valid values are
	 *            0, 1 or 2.
	 * @param retained
	 *            whether or not this message should be retained by the server.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when message delivery hsa
	 *            completed to the requested quality of service
	 * @return token used to track and wait for the publish to complete. The token
	 *         will be passed to any callback that has been set.
	 * @throws MqttPersistenceException
	 *             when a problem occurs storing the message
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @throws MqttException
	 *             for other errors encountered while publishing the message. For
	 *             instance client not connected.
	 * @see #publish(String, MqttMessage, Object, MqttActionListener)
	 * @see MqttMessage#setQos(int)
	 * @see MqttMessage#setRetained(boolean)
	 */
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, Object userContext,
			MqttActionListener callback) throws MqttException, MqttPersistenceException {
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		return this.publish(topic, message, userContext, callback);
	}

	/**
	 * Publishes a message to a topic on the server.
	 * <p>
	 * A convenience method, which will create a new {@link MqttMessage} object with
	 * a byte array payload and the specified QoS, and then publish it.
	 * </p>
	 *
	 * @param topic
	 *            to deliver the message to, for example "finance/stock/ibm".
	 * @param payload
	 *            the byte array to use as the payload
	 * @param qos
	 *            the Quality of Service to deliver the message at. Valid values are
	 *            0, 1 or 2.
	 * @param retained
	 *            whether or not this message should be retained by the server.
	 * @return token used to track and wait for the publish to complete. The token
	 *         will be passed to any callback that has been set.
	 * @throws MqttPersistenceException
	 *             when a problem occurs storing the message
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @throws MqttException
	 *             for other errors encountered while publishing the message. For
	 *             instance if too many messages are being processed.
	 * @see #publish(String, MqttMessage, Object, MqttActionListener)
	 * @see MqttMessage#setQos(int)
	 * @see MqttMessage#setRetained(boolean)
	 */
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained)
			throws MqttException, MqttPersistenceException {
		return this.publish(topic, payload, qos, retained, null, null);
	}

	/**
	 * Publishes a message to a topic on the server. Takes an {@link MqttMessage}
	 * message and delivers it to the server at the requested quality of service.
	 *
	 * @param topic
	 *            to deliver the message to, for example "finance/stock/ibm".
	 * @param message
	 *            to deliver to the server
	 * @return token used to track and wait for the publish to complete. The token
	 *         will be passed to any callback that has been set.
	 * @throws MqttPersistenceException
	 *             when a problem occurs storing the message
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @throws MqttException
	 *             for other errors encountered while publishing the message. For
	 *             instance client not connected.
	 * @see #publish(String, MqttMessage, Object, MqttActionListener)
	 */
	public IMqttDeliveryToken publish(String topic, MqttMessage message)
			throws MqttException, MqttPersistenceException {
		return this.publish(topic, message, null, null);
	}

	/**
	 * Publishes a message to a topic on the server.
	 * <p>
	 * Once this method has returned cleanly, the message has been accepted for
	 * publication by the client and will be delivered on a background thread. In
	 * the event the connection fails or the client stops. Messages will be
	 * delivered to the requested quality of service once the connection is
	 * re-established to the server on condition that:
	 * </p>
	 * <ul>
	 * <li>The connection is re-established with the same clientID
	 * <li>The original connection was made with (@link
	 * MqttConnectOptions#setCleanSession(boolean)} set to false
	 * <li>The connection is re-established with (@link
	 * MqttConnectOptions#setCleanSession(boolean)} set to false
	 * <li>Depending when the failure occurs QoS 0 messages may not be delivered.
	 * </ul>
	 *
	 * <p>
	 * When building an application, the design of the topic tree should take into
	 * account the following principles of topic name syntax and semantics:
	 * </p>
	 *
	 * <ul>
	 * <li>A topic must be at least one character long.</li>
	 * <li>Topic names are case sensitive. For example, <em>ACCOUNTS</em> and
	 * <em>Accounts</em> are two different topics.</li>
	 * <li>Topic names can include the space character. For example, <em>Accounts
	 * payable</em> is a valid topic.</li>
	 * <li>A leading "/" creates a distinct topic. For example, <em>/finance</em> is
	 * different from <em>finance</em>. <em>/finance</em> matches "+/+" and "/+",
	 * but not "+".</li>
	 * <li>Do not include the null character (Unicode
	 *
	 * <pre>
	 * \x0000
	 * </pre>
	 *
	 * ) in any topic.</li>
	 * </ul>
	 *
	 * <p>
	 * The following principles apply to the construction and content of a topic
	 * tree:
	 * </p>
	 * <ul>
	 * <li>The length is limited to 64k but within that there are no limits to the
	 * number of levels in a topic tree.</li>
	 * <li>There can be any number of root nodes; that is, there can be any number
	 * of topic trees.</li>
	 * </ul>
	 *
	 * <p>
	 * The method returns control before the publish completes. Completion can be
	 * tracked by:
	 * </p>
	 * <ul>
	 * <li>Setting an {@link IMqttAsyncClient#setCallback(MqttCallback)} where the
	 * {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)} method will be
	 * called.</li>
	 * <li>Waiting on the returned token {@link MqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link MqttActionListener} to this method</li>
	 * </ul>
	 *
	 * @param topic
	 *            to deliver the message to, for example "finance/stock/ibm".
	 * @param message
	 *            to deliver to the server
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when message delivery has
	 *            completed to the requested quality of service
	 * @return token used to track and wait for the publish to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttPersistenceException
	 *             when a problem occurs storing the message
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @throws MqttException
	 *             for other errors encountered while publishing the message. For
	 *             instance client not connected.
	 * @see MqttMessage
	 */
	public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext,
			MqttActionListener callback) throws MqttException, MqttPersistenceException {
		final String methodName = "publish";
		// @TRACE 111=< topic={0} message={1}userContext={1} callback={2}
		log.fine(CLASS_NAME, methodName, "111", new Object[] { topic, userContext, callback });

		// Checks if a topic is valid when publishing a message.
		MqttTopic.validate(topic, false/* wildcards NOT allowed */);

		MqttDeliveryToken token = new MqttDeliveryToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.setMessage(message);
		token.internalTok.setTopics(new String[] { topic });

		MqttPublish pubMsg = new MqttPublish(topic, message);
		comms.sendNoWait(pubMsg, token);

		// @TRACE 112=<
		log.fine(CLASS_NAME, methodName, "112");

		return token;
	}

	/**
	 * User triggered attempt to reconnect
	 *
	 * @throws MqttException
	 *             if there is an issue with reconnecting
	 */
	public void reconnect() throws MqttException {
		final String methodName = "reconnect";
		// @Trace 500=Attempting to reconnect client: {0}
		log.fine(CLASS_NAME, methodName, "500", new Object[] { this.mqttSession.getClientId() });
		// Some checks to make sure that we're not attempting to reconnect an
		// already connected client
		if (comms.isConnected()) {
			throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_CLIENT_CONNECTED);
		}
		if (comms.isConnecting()) {
			throw new MqttException(MqttClientException.REASON_CODE_CONNECT_IN_PROGRESS);
		}
		if (comms.isDisconnecting()) {
			throw new MqttException(MqttClientException.REASON_CODE_CLIENT_DISCONNECTING);
		}
		if (comms.isClosed()) {
			throw new MqttException(MqttClientException.REASON_CODE_CLIENT_CLOSED);
		}
		// We don't want to spam the server
		stopReconnectCycle();

		attemptReconnect();
	}

	/**
	 * Attempts to reconnect the client to the server. If successful it will make
	 * sure that there are no further reconnects scheduled. However if the connect
	 * fails, the delay will double up to 128 seconds and will re-schedule the
	 * reconnect for after the delay.
	 *
	 * Any thrown exceptions are logged but not acted upon as it is assumed that
	 * they are being thrown due to the server being offline and so reconnect
	 * attempts will continue.
	 */
	private void attemptReconnect() {
		final String methodName = "attemptReconnect";
		// @Trace 500=Attempting to reconnect client: {0}
		log.fine(CLASS_NAME, methodName, "500", new Object[] { this.mqttSession.getClientId() });
		try {
			connect(this.connOpts, this.userContext, new MqttReconnectActionListener(methodName));
		} catch (MqttSecurityException ex) {
			// @TRACE 804=exception
			log.fine(CLASS_NAME, methodName, "804", null, ex);
		} catch (MqttException ex) {
			// @TRACE 804=exception
			log.fine(CLASS_NAME, methodName, "804", null, ex);
		}
	}

	private void startReconnectCycle() {
		String methodName = "startReconnectCycle";
		// @Trace 503=Start reconnect timer for client: {0}, delay: {1}
		log.fine(CLASS_NAME, methodName, "503", new Object[] { this.mqttSession.getClientId(), new Long(reconnectDelay) });
		reconnectTimer = new Timer("MQTT Reconnect: " + this.mqttSession.getClientId());
		reconnectTimer.schedule(new ReconnectTask(), reconnectDelay);
	}

	private void stopReconnectCycle() {
		String methodName = "stopReconnectCycle";
		// @Trace 504=Stop reconnect timer for client: {0}
		log.fine(CLASS_NAME, methodName, "504", new Object[] { this.mqttSession.getClientId() });
		synchronized (clientLock) {
			if (this.connOpts.isAutomaticReconnect()) {
				if (reconnectTimer != null) {
					reconnectTimer.cancel();
					reconnectTimer = null;
				}
				reconnectDelay = 1000; // Reset Delay Timer
			}
		}
	}

	private class ReconnectTask extends TimerTask {
		private static final String methodName = "ReconnectTask.run";

		public void run() {
			// @Trace 506=Triggering Automatic Reconnect attempt.
			log.fine(CLASS_NAME, methodName, "506");
			attemptReconnect();
		}
	}

	class MqttReconnectCallback implements MqttCallbackExtended {

		final boolean automaticReconnect;

		MqttReconnectCallback(boolean isAutomaticReconnect) {
			automaticReconnect = isAutomaticReconnect;
		}

		public void messageArrived(String topic, MqttMessage message) throws Exception {
		}

		public void deliveryComplete(IMqttDeliveryToken token) {
		}

		public void connectComplete(boolean reconnect, String serverURI) {
		}

		@Override
		public void disconnected(MqttDisconnectResponse disconnectResponse) {
			if (automaticReconnect) {
				// Automatic reconnect is set so make sure comms is in resting
				// state
				comms.setRestingState(true);
				reconnecting = true;
				startReconnectCycle();
			}
		}

		@Override
		public void mqttErrorOccured(MqttException exception) {
		}

	}

	class MqttReconnectActionListener implements MqttActionListener {

		final String methodName;

		MqttReconnectActionListener(String methodName) {
			this.methodName = methodName;
		}

		public void onSuccess(IMqttToken asyncActionToken) {
			// @Trace 501=Automatic Reconnect Successful: {0}
			log.fine(CLASS_NAME, methodName, "501", new Object[] { asyncActionToken.getClient().getClientId() });
			comms.setRestingState(false);
			stopReconnectCycle();
		}

		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			// @Trace 502=Automatic Reconnect failed, rescheduling: {0}
			log.fine(CLASS_NAME, methodName, "502", new Object[] { asyncActionToken.getClient().getClientId() });
			if (reconnectDelay < 128000) {
				reconnectDelay = reconnectDelay * 2;
			}
			rescheduleReconnectCycle(reconnectDelay);
		}

		private void rescheduleReconnectCycle(int delay) {
			String reschedulemethodName = methodName + ":rescheduleReconnectCycle";
			// @Trace 505=Rescheduling reconnect timer for client: {0}, delay:
			// {1}
			log.fine(CLASS_NAME, reschedulemethodName, "505",
					new Object[] { MqttAsyncClient.this.mqttSession.getClientId(), String.valueOf(reconnectDelay) });
			synchronized (clientLock) {
				if (MqttAsyncClient.this.connOpts.isAutomaticReconnect()) {
					if (reconnectTimer != null) {
						reconnectTimer.schedule(new ReconnectTask(), delay);
					} else {
						// The previous reconnect timer was cancelled
						reconnectDelay = delay;
						startReconnectCycle();
					}
				}
			}
		}

	}

	/**
	 * Sets the DisconnectedBufferOptions for this client
	 *
	 * @param bufferOpts
	 *            the {@link DisconnectedBufferOptions}
	 */
	public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
		this.comms.setDisconnectedMessageBuffer(new DisconnectedMessageBuffer(bufferOpts));
	}

	/**
	 * Returns the number of messages in the Disconnected Message Buffer
	 *
	 * @return Count of messages in the buffer
	 */
	public int getBufferedMessageCount() {
		return this.comms.getBufferedMessageCount();
	}

	/**
	 * Returns a message from the Disconnected Message Buffer
	 *
	 * @param bufferIndex
	 *            the index of the message to be retrieved.
	 * @return the message located at the bufferIndex
	 */
	public MqttMessage getBufferedMessage(int bufferIndex) {
		return this.comms.getBufferedMessage(bufferIndex);
	}

	/**
	 * Deletes a message from the Disconnected Message Buffer
	 *
	 * @param bufferIndex
	 *            the index of the message to be deleted.
	 */
	public void deleteBufferedMessage(int bufferIndex) {
		this.comms.deleteBufferedMessage(bufferIndex);
	}

	/**
	 * Returns the current number of outgoing in-flight messages being sent by the
	 * client. Note that this number cannot be guaranteed to be 100% accurate as
	 * some messages may have been sent or queued in the time taken for this method
	 * to return.
	 *
	 * @return the current number of in-flight messages.
	 */
	public int getInFlightMessageCount() {
		return this.comms.getActualInFlight();
	}

	/**
	 * Close the client Releases all resource associated with the client. After the
	 * client has been closed it cannot be reused. For instance attempts to connect
	 * will fail.
	 *
	 * @throws MqttException
	 *             if the client is not disconnected.
	 */
	public void close() throws MqttException {
		close(false);
	}

	/**
	 * Close the client Releases all resource associated with the client. After the
	 * client has been closed it cannot be reused. For instance attempts to connect
	 * will fail.
	 *
	 * @param force
	 *            - Will force the connection to close.
	 *
	 * @throws MqttException
	 *             if the client is not disconnected.
	 */
	public void close(boolean force) throws MqttException {
		final String methodName = "close";
		// @TRACE 113=<
		log.fine(CLASS_NAME, methodName, "113");
		comms.close(force);
		// @TRACE 114=>
		log.fine(CLASS_NAME, methodName, "114");

	}

	/**
	 * Return a debug object that can be used to help solve problems.
	 *
	 * @return the {@link Debug} object
	 */
	public Debug getDebug() {
		return new Debug(this.mqttSession.getClientId(), comms);
	}

}
