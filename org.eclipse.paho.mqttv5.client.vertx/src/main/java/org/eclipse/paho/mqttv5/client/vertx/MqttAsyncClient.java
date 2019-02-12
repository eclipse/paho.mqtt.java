/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corp.
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
 *    James Sutton - initial API and implementation and/or initial documentation
 */

package org.eclipse.paho.mqttv5.client.vertx;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.URI;

import org.eclipse.paho.mqttv5.client.vertx.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.vertx.MqttClientInterface;
import org.eclipse.paho.mqttv5.client.vertx.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.vertx.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.vertx.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.client.vertx.util.Debug;
import org.eclipse.paho.mqttv5.client.vertx.internal.ClientInternal;
import org.eclipse.paho.mqttv5.client.vertx.internal.MqttConnectionState;
import org.eclipse.paho.mqttv5.client.vertx.logging.Logger;
import org.eclipse.paho.mqttv5.client.vertx.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.common.ExceptionHelper;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.MqttSecurityException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttAuth;
import org.eclipse.paho.mqttv5.common.packet.MqttDataTypes;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;


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
 * If connecting with {@link MqttConnectionOptions#setCleanStart(boolean)} set
 * to true it is safe to use memory persistence as all state is cleared when a
 * client disconnects. If connecting with cleanStart set to false in order to
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
public class MqttAsyncClient implements MqttClientInterface, IMqttAsyncClient {
	private static final String CLASS_NAME = MqttAsyncClient.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private static final long QUIESCE_TIMEOUT = 30000; // ms
	private static final long DISCONNECT_TIMEOUT = 10000; // ms
	private static final char MIN_HIGH_SURROGATE = '\uD800';
	private static final char MAX_HIGH_SURROGATE = '\uDBFF';

	private Hashtable<String, MqttTopic> topics;
	private MqttCallback mqttCallback;

	private Object userContext;
	private Timer reconnectTimer; // Automatic reconnect timer
	private static int reconnectDelay = 1000; // Reconnect delay, starts at 1
												// second
	private URI serverURI;
	private MqttClientPersistence persistence;
	private MqttConnectionOptions connOpts;
	
	// Variables that exist within the life of an MQTT connection.
	private MqttConnectionState connectionstate; 
	
	private ClientInternal internal; 
	
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
	 * As the client identifier is used by the server to identify a client when it
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
	 * As the client identifier is used by the server to identify a client when it
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
	 * {@link MqttConnectionOptions#setCleanStart(boolean)} must be set to false. In
	 * the event that only QoS 0 messages are sent or received or cleanStart is set
	 * to true then a safe store is not needed.
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
	/*public MqttAsyncClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
		this(serverURI, clientId, persistence, null);
	}*/

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
	 * As the client identifier is used by the server to identify a client when it
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
	 * {@link MqttConnectionOptions#setCleanStart(boolean)} must be set to false. In
	 * the event that only QoS 0 messages are sent or received or cleanStart is set
	 * to true then a safe store is not needed.
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
	 *            used for managing threads. If null then a newScheduledThreadPool
	 *            is used.
	 * @throws IllegalArgumentException
	 *             if the URI does not start with "tcp://", "ssl://" or "local://"
	 * @throws IllegalArgumentException
	 *             if the clientId is null or is greater than 65535 characters in
	 *             length
	 * @throws MqttException
	 *             if any other problem was encountered
	 */
	public MqttAsyncClient(String serverURI, String clientId, MqttClientPersistence persistence) 
			throws MqttException {
		final String methodName = "MqttAsyncClient";

		log.setResourceName(clientId);

		if (clientId != null) {
			// Verify that the client ID is not too long
			// Encode it ourselves to check
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			MqttDataTypes.encodeUTF8(dos, clientId);
			// Remove the two size bytes.
			if (dos.size() - 2 > 65535) {
				throw new IllegalArgumentException("ClientId longer than 65535 characters");
			}

		} else {
			clientId = "";
		}

		try {
			this.serverURI = new URI(serverURI);
		} catch (Exception e) {
			throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
		}

		this.persistence = persistence;
		if (this.persistence == null) {
			this.persistence = new MemoryPersistence();
		}
		internal = new ClientInternal(this, persistence);
		internal.setClientId(clientId);
		connectionstate = internal.getConnectionState();

		// @TRACE 101=<init> ClientID={0} ServerURI={1} PersistenceType={2}
		log.fine(CLASS_NAME, methodName, "101", new Object[] { clientId, serverURI, persistence });

		if (persistence != null) {
			this.persistence.open(clientId);
		}

				
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

	/*private String getHostName(String uri) {
		int portIndex = uri.indexOf(':');
		if (portIndex == -1) {
			portIndex = uri.indexOf('/');
		}
		if (portIndex == -1) {
			portIndex = uri.length();
		}
		return uri.substring(0, portIndex);
	}*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#connect(java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener)
	 */
	@Override
	public IMqttToken connect(Object userContext, MqttActionListener callback)
			throws MqttException, MqttSecurityException {
		return this.connect(new MqttConnectionOptions(), userContext, callback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#connect()
	 */
	@Override
	public IMqttToken connect() throws MqttException, MqttSecurityException {
		return this.connect(null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#connect(org.eclipse.paho.
	 * mqttv5.client.MqttConnectionOptions)
	 */
	@Override
	public IMqttToken connect(MqttConnectionOptions options) throws MqttException, MqttSecurityException {
		return this.connect(options, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#connect(org.eclipse.paho.
	 * mqttv5.client.MqttConnectionOptions, java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener)
	 */
	@Override
	public IMqttToken connect(MqttConnectionOptions options, Object userContext, MqttActionListener callback)
			throws MqttException, MqttSecurityException {
		final String methodName = "connect";

		// @TRACE 103=cleanSession={0} connectionTimeout={1} TimekeepAlive={2}
		// userName={3} password={4} will={5} userContext={6} callback={7}
		log.fine(CLASS_NAME, methodName, "103",
				new Object[] { Boolean.valueOf(options.isCleanStart()), new Integer(options.getConnectionTimeout()),
						Integer.valueOf(options.getKeepAliveInterval()), options.getUserName(),
						((null == options.getPassword()) ? "[null]" : "[notnull]"),
						((null == options.getWillMessage()) ? "[null]" : "[notnull]"), userContext, callback });

		MqttToken connectToken = new MqttToken(getClientId());
		
		String[] serverURIs = options.getServerURIs();
		if (serverURIs == null) {
			serverURIs = new String[] {serverURI.toString()};
		}

		connOpts = options;
		internal.connect(options, connectToken, serverURIs, 0, null);
		
		return connectToken;
	}
	
	/*
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
	
	private MqttWireMessage getPacket(Buffer buffer) {
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
		*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#disconnect(java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener)
	 */
	@Override
	public IMqttToken disconnect(Object userContext, MqttActionListener callback) throws MqttException {
		return this.disconnect(QUIESCE_TIMEOUT, userContext, callback, MqttReturnCode.RETURN_CODE_SUCCESS,
				new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#disconnect()
	 */
	@Override
	public IMqttToken disconnect() throws MqttException {
		return this.disconnect(null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#disconnect(long)
	 */
	@Override
	public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
		return this.disconnect(quiesceTimeout, null, null, MqttReturnCode.RETURN_CODE_SUCCESS, new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#disconnect(long,
	 * java.lang.Object, org.eclipse.paho.mqttv5.client.MqttActionListener, int,
	 * org.eclipse.paho.mqttv5.common.packet.MqttProperties)
	 */
	@Override
	public IMqttToken disconnect(long quiesceTimeout, Object userContext, MqttActionListener callback, int reasonCode,
			MqttProperties disconnectProperties) throws MqttException {
		final String methodName = "disconnect";
		// @TRACE 104=> quiesceTimeout={0} userContext={1} callback={2}
		log.fine(CLASS_NAME, methodName, "104", new Object[] { new Long(quiesceTimeout), userContext, callback });

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		
		internal.disconnect(reasonCode, disconnectProperties, token);

		// @TRACE 108=<
		log.fine(CLASS_NAME, methodName, "108");
		return token;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IMqttAsyncClient#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return internal.isConnected();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#getClientId()
	 */
	@Override
	public String getClientId() {
		return internal.getClientId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#setClientId(java.lang.String)
	 */
	@Override
	public void setClientId(String clientId) {
		internal.setClientId(clientId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#getServerURI()
	 */
	@Override
	public String getServerURI() {
		return serverURI.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#getCurrentServerURI()
	 */
	@Override
	public String getCurrentServerURI() {
		return null; //comms.getNetworkModules()[comms.getNetworkModuleIndex()].getServerURI();
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
	 * <li>{@link MqttClient#publish(String, MqttMessage)} to publish
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
		MqttTopicValidator.validate(topic, false/* wildcards NOT allowed */, true);

		MqttTopic result = (MqttTopic) topics.get(topic);
		if (result == null) {
			result = new MqttTopic(topic, internal);
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
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#checkPing(java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener)
	 */
	@Override
	public IMqttToken checkPing(Object userContext, MqttActionListener callback) throws MqttException {
		final String methodName = "ping";
		MqttToken token = null;
		// @TRACE 117=>
		log.fine(CLASS_NAME, methodName, "117");

		//token = comms.checkForActivity(callback);
		// @TRACE 118=<
		log.fine(CLASS_NAME, methodName, "118");

		return token;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(java.lang.String,
	 * int, java.lang.Object, org.eclipse.paho.mqttv5.client.MqttActionListener)
	 */
	@Override
	public IMqttToken subscribe(String topicFilter, int qos, Object userContext, MqttActionListener callback)
			throws MqttException {
		return this.subscribe(new MqttSubscription[] { new MqttSubscription(topicFilter, qos) }, userContext, callback,
				new MqttProperties());
	}
	
	@Override
	public IMqttToken subscribe(String[] topicFilters, int[] qoss, Object userContext, MqttActionListener callback)
			throws MqttException {
		MqttSubscription[] subs = new MqttSubscription[topicFilters.length];
		for (int i = 0; i < topicFilters.length; ++ i) {
			subs[i] = new MqttSubscription(topicFilters[i], qoss[i]);
		}
		return this.subscribe(subs, userContext, callback, new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(java.lang.String,
	 * int)
	 */
	@Override
	public IMqttToken subscribe(String topicFilter, int qos) throws MqttException {
		return this.subscribe(new MqttSubscription[] { new MqttSubscription(topicFilter, qos) }, null, null,
				new MqttProperties());
	}
	
	@Override
	public IMqttToken subscribe(String[] topicFilters, int[] qoss) throws MqttException {
		return this.subscribe(topicFilters, qoss, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(java.lang.String,
	 * int)
	 */
	@Override
	public IMqttToken subscribe(MqttSubscription subscription) throws MqttException {
		return this.subscribe(new MqttSubscription[] { subscription }, null, null, new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(org.eclipse.paho.
	 * mqttv5.common.MqttSubscription[])
	 */
	@Override
	public IMqttToken subscribe(MqttSubscription[] subscriptions) throws MqttException {
		return this.subscribe(subscriptions, null, null, new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(org.eclipse.paho.
	 * mqttv5.common.MqttSubscription[], java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener,
	 * org.eclipse.paho.mqttv5.common.packet.MqttProperties)
	 */
	@Override
	public IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			MqttProperties subscriptionProperties) throws MqttException {

		// remove any message handlers for individual topics and validate Topics
		for (int i = 0; i < subscriptions.length; ++i) {
			//this.comms.removeMessageListener(subscriptions[i].getTopic());
			// Check if the topic filter is valid before subscribing
			MqttTopicValidator.validate(subscriptions[i].getTopic(),
					this.connectionstate.isWildcardSubscriptionsAvailable(),
					this.connectionstate.isSharedSubscriptionsAvailable());
		}
		
		return this.subscribeBase(subscriptions, userContext, callback, subscriptionProperties);
	}

	private IMqttToken subscribeBase(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
				MqttProperties subscriptionProperties) throws MqttException {
		final String methodName = "subscribe";
		
		// Only Generate Log string if we are logging at FINE level
		if (log.isLoggable(Logger.FINE)) {
			StringBuffer subs = new StringBuffer();
			for (int i = 0; i < subscriptions.length; i++) {
				if (i > 0) {
					subs.append(", ");
				}
				subs.append(subscriptions[i].toString());
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

		try {
			internal.subscribe(subscriptions, subscriptionProperties, token);
		} catch (MqttException e) {
			
		}

		// @TRACE 109=<
		log.fine(CLASS_NAME, methodName, "109");

		return token;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(org.eclipse.paho.
	 * mqttv5.common.MqttSubscription, java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener,
	 * org.eclipse.paho.mqttv5.client.IMqttMessageListener,
	 * org.eclipse.paho.mqttv5.common.packet.MqttProperties)
	 */
	@Override
	public IMqttToken subscribe(MqttSubscription mqttSubscription, Object userContext, MqttActionListener callback,
			IMqttMessageListener messageListener, MqttProperties subscriptionProperties) throws MqttException {

		return this.subscribe(new MqttSubscription[] { mqttSubscription }, userContext, callback, messageListener,
				subscriptionProperties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(org.eclipse.paho.
	 * mqttv5.common.MqttSubscription,
	 * org.eclipse.paho.mqttv5.client.IMqttMessageListener)
	 */
	@Override
	public IMqttToken subscribe(MqttSubscription subscription, IMqttMessageListener messageListener)
			throws MqttException {
		return this.subscribe(new MqttSubscription[] { subscription }, null, null, messageListener,
				new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(org.eclipse.paho.
	 * mqttv5.common.MqttSubscription[],
	 * org.eclipse.paho.mqttv5.client.IMqttMessageListener)
	 */
	@Override
	public IMqttToken subscribe(MqttSubscription[] subscriptions, IMqttMessageListener messageListener)
			throws MqttException {
		return this.subscribe(subscriptions, null, null, messageListener, new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(org.eclipse.paho.
	 * mqttv5.common.MqttSubscription[], java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener,
	 * org.eclipse.paho.mqttv5.client.IMqttMessageListener[],
	 * org.eclipse.paho.mqttv5.common.packet.MqttProperties)
	 */
	@Override
	public IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			IMqttMessageListener[] messageListeners, MqttProperties subscriptionProperties) throws MqttException {

		// add message handlers to the list for this client
		for (int i = 0; i < subscriptions.length; ++i) {
			MqttTopicValidator.validate(subscriptions[i].getTopic(),
					this.connectionstate.isWildcardSubscriptionsAvailable(),
					this.connectionstate.isSharedSubscriptionsAvailable());
			if (messageListeners == null || messageListeners[i] == null) {
				//this.comms.removeMessageListener(subscriptions[i].getTopic());
			} else {
				//this.comms.setMessageListener(null, subscriptions[i].getTopic(), messageListeners[i]);
			}
		}
		
		IMqttToken token = null;
		try 	{
			token = this.subscribeBase(subscriptions, userContext, callback, subscriptionProperties);
		} catch(Exception e) {
			// if the subscribe fails, then we have to remove the message handlers
			for (int i = 0; i < subscriptions.length; ++i) {
				//this.comms.removeMessageListener(subscriptions[i].getTopic());
			}
			throw e;
		}
		return token;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#subscribe(org.eclipse.paho.
	 * mqttv5.common.MqttSubscription[], java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener,
	 * org.eclipse.paho.mqttv5.client.IMqttMessageListener,
	 * org.eclipse.paho.mqttv5.common.packet.MqttProperties)
	 */
	@Override
	public IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			IMqttMessageListener messageListener, MqttProperties subscriptionProperties) throws MqttException {

		int subId = subscriptionProperties.getSubscriptionIdentifiers().get(0);

		// Automatic Subscription Identifier Assignment is enabled
		if (connOpts.useSubscriptionIdentifiers() && this.connectionstate.isSubscriptionIdentifiersAvailable()) {

			// Application is overriding the subscription Identifier
			if (subId != 0) {
				// Check that we are not already using this ID, else throw Illegal Argument
				// Exception
				/*if (this.comms.doesSubscriptionIdentifierExist(subId)) {
					throw new IllegalArgumentException(
							String.format("The Subscription Identifier %s already exists.", subId));
				}*/

			} else {
				// Automatically assign new ID and link to callback.
				subId = internal.getSessionState().getNextSubscriptionIdentifier();
			}
		}
		
		// add message handlers to the list for this client
		for (int i = 0; i < subscriptions.length; ++i) {
			MqttTopicValidator.validate(subscriptions[i].getTopic(),
					this.connectionstate.isWildcardSubscriptionsAvailable(),
					this.connectionstate.isSharedSubscriptionsAvailable());
			if (messageListener == null) {
				//this.comms.removeMessageListener(subscriptions[i].getTopic());
			} else {
				//this.comms.setMessageListener(subId, subscriptions[i].getTopic(), messageListener);
			}
		}

		IMqttToken token = null;
		try 	{
			token = this.subscribeBase(subscriptions, userContext, callback, subscriptionProperties);
		} catch(Exception e) {
			// if the subscribe fails, then we have to remove the message handlers
			for (int i = 0; i < subscriptions.length; ++i) {
				//this.comms.removeMessageListener(subscriptions[i].getTopic());
			}
			throw e;
		}
		return token;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#unsubscribe(java.lang.String,
	 * java.lang.Object, org.eclipse.paho.mqttv5.client.MqttActionListener)
	 */
	@Override
	public IMqttToken unsubscribe(String topicFilter, Object userContext, MqttActionListener callback)
			throws MqttException {
		return unsubscribe(new String[] { topicFilter }, userContext, callback, new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#unsubscribe(java.lang.String)
	 */
	@Override
	public IMqttToken unsubscribe(String topicFilter) throws MqttException {
		return unsubscribe(new String[] { topicFilter }, null, null, new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#unsubscribe(java.lang.String[
	 * ])
	 */
	@Override
	public IMqttToken unsubscribe(String[] topicFilters) throws MqttException {
		return unsubscribe(topicFilters, null, null, new MqttProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#unsubscribe(java.lang.String[
	 * ], java.lang.Object, org.eclipse.paho.mqttv5.client.MqttActionListener,
	 * org.eclipse.paho.mqttv5.common.packet.MqttProperties)
	 */
	@Override
	public IMqttToken unsubscribe(String[] topicFilters, Object userContext, MqttActionListener callback,
			MqttProperties unsubscribeProperties) throws MqttException {
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
			MqttTopicValidator.validate(topicFilters[i], true/* allow wildcards */, this.connectionstate.isSharedSubscriptionsAvailable());
		}

		// remove message handlers from the list for this client
		for (int i = 0; i < topicFilters.length; ++i) {
			//this.comms.removeMessageListener(topicFilters[i]);
		}

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		//token.internalTok.setTopics(topicFilters);

		internal.unsubscribe(topicFilters, unsubscribeProperties, token);

		// @TRACE 110=<
		log.fine(CLASS_NAME, methodName, "110");

		return token;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#setCallback(org.eclipse.paho.
	 * mqttv5.client.MqttCallback)
	 */
	@Override
	public void setCallback(MqttCallback callback) {
		this.mqttCallback = callback;
		//comms.setCallback(callback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#setManualAcks(boolean)
	 */
	@Override
	public void setManualAcks(boolean manualAcks) {
		//comms.setManualAcks(manualAcks);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#messageArrivedComplete(int,
	 * int)
	 */
	@Override
	public void messageArrivedComplete(int messageId, int qos) throws MqttException {
		//comms.messageArrivedComplete(messageId, qos);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#getPendingDeliveryTokens()
	 */
	@Override
	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return null; //comms.getPendingDeliveryTokens();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#publish(java.lang.String,
	 * byte[], int, boolean, java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, Object userContext,
			MqttActionListener callback) throws MqttException, MqttPersistenceException {
		MqttMessage message = new MqttMessage(payload);
		message.setProperties(new MqttProperties());
		message.setQos(qos);
		message.setRetained(retained);
		return this.publish(topic, message, userContext, callback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#publish(java.lang.String,
	 * byte[], int, boolean)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained)
			throws MqttException, MqttPersistenceException {
		return this.publish(topic, payload, qos, retained, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#publish(java.lang.String,
	 * org.eclipse.paho.mqttv5.common.MqttMessage)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, MqttMessage message)
			throws MqttException, MqttPersistenceException {
		return this.publish(topic, message, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#publish(java.lang.String,
	 * org.eclipse.paho.mqttv5.common.MqttMessage, java.lang.Object,
	 * org.eclipse.paho.mqttv5.client.MqttActionListener,
	 * org.eclipse.paho.mqttv5.common.packet.MqttProperties)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext,
			MqttActionListener callback) throws MqttException, MqttPersistenceException {
		final String methodName = "publish";
		// @TRACE 111=< topic={0} message={1}userContext={1} callback={2}
		log.fine(CLASS_NAME, methodName, "111", new Object[] { topic, userContext, callback });

		// Checks if a topic is valid when publishing a message.
		MqttTopicValidator.validate(topic, false/* wildcards NOT allowed */, true);

		MqttDeliveryToken token = new MqttDeliveryToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.setMessage(message);
		//token.internalTok.setTopics(new String[] { topic });

		internal.publish(topic, message, token);

		// @TRACE 112=<
		log.fine(CLASS_NAME, methodName, "112");
		return token;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#reconnect()
	 */
	@Override
	public void reconnect() throws MqttException {
		final String methodName = "reconnect";
		// @Trace 500=Attempting to reconnect client: {0}
		log.fine(CLASS_NAME, methodName, "500", new Object[] { internal.getClientId() });
		// Some checks to make sure that we're not attempting to reconnect an
		// already connected client
		/*if (comms.isConnected()) {
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

		attemptReconnect();*/
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
		log.fine(CLASS_NAME, methodName, "500", new Object[] { internal.getClientId() });
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
		log.fine(CLASS_NAME, methodName, "503",
				new Object[] { internal.getClientId(), new Long(reconnectDelay) });
		reconnectTimer = new Timer("MQTT Reconnect: " + internal.getClientId());
		reconnectTimer.schedule(new ReconnectTask(), reconnectDelay);
	}

	private void stopReconnectCycle() {
		String methodName = "stopReconnectCycle";
		// @Trace 504=Stop reconnect timer for client: {0}
		log.fine(CLASS_NAME, methodName, "504", new Object[] { internal.getClientId() });
		//synchronized (clientLock) {
			if (this.connOpts.isAutomaticReconnect()) {
				if (reconnectTimer != null) {
					reconnectTimer.cancel();
					reconnectTimer = null;
				}
				reconnectDelay = 1000; // Reset Delay Timer
			}
		//}
	}

	private class ReconnectTask extends TimerTask {
		private static final String methodName = "ReconnectTask.run";

		public void run() {
			// @Trace 506=Triggering Automatic Reconnect attempt.
			log.fine(CLASS_NAME, methodName, "506");
			attemptReconnect();
		}
	}

	class MqttReconnectCallback implements MqttCallback {

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

		public void disconnected(MqttDisconnectResponse disconnectResponse) {
			if (automaticReconnect) {
				//reconnecting = true;
				startReconnectCycle();
			}
		}

		public void mqttErrorOccurred(MqttException exception) {
		}

		public void authPacketArrived(int reasonCode, MqttProperties properties) {

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
			//comms.setRestingState(false);
			stopReconnectCycle();
		}

		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			// @Trace 502=Automatic Reconnect failed, rescheduling: {0}
			log.fine(CLASS_NAME, methodName, "502", new Object[] { asyncActionToken.getClient().getClientId() });
			if (reconnectDelay < connOpts.getMaxReconnectDelay()) {
				reconnectDelay = reconnectDelay * 2;
			}
			rescheduleReconnectCycle(reconnectDelay);
		}

		private void rescheduleReconnectCycle(int delay) {
			String reschedulemethodName = methodName + ":rescheduleReconnectCycle";
			// @Trace 505=Rescheduling reconnect timer for client: {0}, delay:
			// {1}
			log.fine(CLASS_NAME, reschedulemethodName, "505",
					new Object[] { internal.getClientId(), String.valueOf(reconnectDelay) });
			//synchronized (clientLock) {
				if (MqttAsyncClient.this.connOpts.isAutomaticReconnect()) {
					if (reconnectTimer != null) {
						reconnectTimer.schedule(new ReconnectTask(), delay);
					} else {
						// The previous reconnect timer was cancelled
						reconnectDelay = delay;
						startReconnectCycle();
					}
				}
			//}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#setBufferOpts(org.eclipse.
	 * paho.mqttv5.client.DisconnectedBufferOptions)
	 */
	@Override
	public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
		//this.comms.setDisconnectedMessageBuffer(new DisconnectedMessageBuffer(bufferOpts));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#getBufferedMessageCount()
	 */
	@Override
	public int getBufferedMessageCount() {
		return 0; //this.comms.getBufferedMessageCount();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#getBufferedMessage(int)
	 */
	@Override
	public MqttMessage getBufferedMessage(int bufferIndex) {
		return null; //this.comms.getBufferedMessage(bufferIndex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#deleteBufferedMessage(int)
	 */
	@Override
	public void deleteBufferedMessage(int bufferIndex) {
		//this.comms.deleteBufferedMessage(bufferIndex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.paho.mqttv5.client.IMqttAsyncClient#getInFlightMessageCount()
	 */
	@Override
	public int getInFlightMessageCount() {
		return 0; //this.comms.getActualInFlight();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#close()
	 */
	@Override
	public void close() throws MqttException {
		close(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#close(boolean)
	 */
	@Override
	public void close(boolean force) throws MqttException {
		final String methodName = "close";
		// @TRACE 113=<
		log.fine(CLASS_NAME, methodName, "113");
		//comms.close(force);
		// @TRACE 114=>
		log.fine(CLASS_NAME, methodName, "114");

	}
	
	public MqttCallback getCallback() {
		return this.mqttCallback;
	}
	
	/**
	 * Return a debug object that can be used to help solve problems.
	 * 
	 * @see org.eclipse.paho.mqttv5.client.IMqttAsyncClient#getDebug()
	 */
	@Override
	public Debug getDebug() {
		return null; //new Debug(this.mqttSession.getClientId(), comms);
	}

	@Override
	public IMqttToken authenticate(int reasonCode, Object userContext, MqttProperties properties) throws MqttException {
		MqttToken token = new MqttToken(getClientId());
		token.setUserContext(userContext);

		MqttAuth auth = new MqttAuth(reasonCode, properties);
		//comms.sendNoWait(auth, token);
		return null;
	}

}
