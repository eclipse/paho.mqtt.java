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
package org.eclipse.paho.client.mqttv3;

import java.util.Hashtable;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.internal.ConnectActionListener;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.internal.LocalNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.internal.SSLNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.TCPNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttUnsubscribe;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.util.Debug;

/**
 * Lightweight client for talking to an MQTT server using non-blocking methods
 * that allow an operation to run in the background.
 *
 * <p>This class implements the non-blocking {@link IMqttAsyncClient} client interface
 * allowing applications to initiate MQTT actions and then carry on working while the
 * MQTT action completes on a background thread.
 * This implementation is compatible with all Java SE runtimes from 1.4.2 and up.
 * </p>
 * <p>An application can connect to an MQTT server using:
 * <ul>
 * <li>A plain TCP socket
 * <li>A secure SSL/TLS socket
 * </ul>
 * </p>
 * <p>To enable messages to be delivered even across network and client restarts
 * messages need to be safely stored until the message has been delivered at the requested
 * quality of service. A pluggable persistence mechanism is provided to store the messages.
 * </p>
 * <p>By default {@link MqttDefaultFilePersistence} is used to store messages to a file.
 * If persistence is set to null then messages are stored in memory and hence can be lost
 * if the client, Java runtime or device shuts down.
 * </p>
 * <p>If connecting with {@link MqttConnectOptions#setCleanSession(boolean)} set to true it
 * is safe to use memory persistence as all state is cleared when a client disconnects. If
 * connecting with cleanSession set to false in order to provide reliable message delivery
 * then a persistent message store such as the default one should be used.
 * </p>
 * <p>The message store interface is pluggable. Different stores can be used by implementing
 * the {@link MqttClientPersistence} interface and passing it to the clients constructor.
 * </p>
 *
 * @see IMqttAsyncClient
 */
public class MqttAsyncClient implements IMqttAsyncClient { // DestinationProvider {

	private String clientId;
	private String serverURI;
	protected ClientComms comms;
	private Hashtable topics;
	private MqttClientPersistence persistence;

	final static String className = MqttAsyncClient.class.getName();
	public Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,className);

	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
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
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * If the port is not specified, it will
	 * default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 23 characters.
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
	 * off persistence, use the {@link #MqttAsyncClient(String, String, MqttClientPersistence)}
	 * constructor.
	 *
	 * @param serverURI the address of the server to connect to, specified as a URI. Can be overridden using
	 * {@link MqttConnectOptions#setServerURIs(String[])}
	 * @param clientId a client identifier that is unique on the server being connected to
	 * @throws IllegalArgumentException if the URI does not start with
	 * "tcp://", "ssl://" or "local://".
	 * @throws IllegalArgumentException if the clientId is null or is greater than 23 characters in length
	 * @throws MqttException if any other problem was encountered
	 */
	public MqttAsyncClient(String serverURI, String clientId) throws MqttException {
		this(serverURI,clientId, new MqttDefaultFilePersistence());
	}

	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
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
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * If the port is not specified, it will
	 * default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 23 characters.
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
	 * @throws IllegalArgumentException if the clientId is null or is greater than 23 characters in length
	 * @throws MqttException if any other problem was encountered
	 */
	public MqttAsyncClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
		final String methodName = "MqttAsyncClient";

		log.setResourceName(clientId);

		if (clientId == null || clientId.length() == 0) {
			throw new IllegalArgumentException("Null or zero length clientId");
		}
		// Count characters, surrogate pairs count as one character.
		int clientIdLength = 0;
		for (int i = 0; i < clientId.length() - 1; i++) {
			if (Character_isHighSurrogate(clientId.charAt(i)))
				i++;
			clientIdLength++;
		}
		if ( clientIdLength > 23) {
			throw new IllegalArgumentException("ClientId longer than 23 characters");
		}

		MqttConnectOptions.validateURI(serverURI);

		this.serverURI = serverURI;
		this.clientId = clientId;

		this.persistence = persistence;
		if (this.persistence == null) {
			this.persistence = new MemoryPersistence();
		}

		// @TRACE 101=<init> ClientID={0} ServerURI={1} PersistenceType={2}
		log.fine(className,methodName,"101",new Object[]{clientId,serverURI,persistence});

		this.persistence.open(clientId, serverURI);
		this.comms = new ClientComms(this, this.persistence);
		this.persistence.close();
		this.topics = new Hashtable();

	}

	/**
	 * @param ch
	 * @return returns 'true' if the character is a high-surrogate code unit
	 */
	protected static boolean Character_isHighSurrogate(char ch) {
		char MIN_HIGH_SURROGATE = '\uD800';
		char MAX_HIGH_SURROGATE = '\uDBFF';
		return(ch >= MIN_HIGH_SURROGATE) && (ch <= MAX_HIGH_SURROGATE);
	}

	/**
	 * Factory method to create an array of network modules, one for
	 * each of the supplied URIs
	 *
	 * @param address the URI for the server.
	 * @return a network module appropriate to the specified address.
	 */

	// may need an array of these network modules

	protected NetworkModule[] createNetworkModules(String address, MqttConnectOptions options) throws MqttException, MqttSecurityException {
		final String methodName = "createNetworkModules";
		// @TRACE 116=URI={0}
		log.fine(className, methodName, "116", new Object[]{address});

		NetworkModule[] networkModules = null;
		String[] serverURIs = options.getServerURIs();
		String[] array = null;
		if (serverURIs == null) {
			array = new String[]{address};
		} else if (serverURIs.length == 0) {
			array = new String[]{address};
		} else {
			array = serverURIs;
		}

		networkModules = new NetworkModule[array.length];
		for (int i = 0; i < array.length; i++) {
			networkModules[i] = createNetworkModule(array[i], options);
		}

		log.fine(className, methodName, "108");
		return networkModules;
	}

	/**
	 * Factory method to create the correct network module, based on the
	 * supplied address URI.
	 *
	 * @param address the URI for the server.
	 * @param Connect options
	 * @return a network module appropriate to the specified address.
	 */
	private NetworkModule createNetworkModule(String address, MqttConnectOptions options) throws MqttException, MqttSecurityException {
		final String methodName = "createNetworkModule";
		// @TRACE 115=URI={0}
		log.fine(className,methodName, "115", new Object[] {address});

		NetworkModule netModule;
		String shortAddress;
		String host;
		int port;
		SocketFactory factory = options.getSocketFactory();

		int serverURIType = MqttConnectOptions.validateURI(address);

		switch (serverURIType) {
		case MqttConnectOptions.URI_TYPE_TCP :
			shortAddress = address.substring(6);
			host = getHostName(shortAddress);
			port = getPort(shortAddress, 1883);
			if (factory == null) {
				factory = SocketFactory.getDefault();
			}
			else if (factory instanceof SSLSocketFactory) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}
			netModule = new TCPNetworkModule(factory, host, port, clientId);
			((TCPNetworkModule)netModule).setConnectTimeout(options.getConnectionTimeout());
			break;
		case MqttConnectOptions.URI_TYPE_SSL:
			shortAddress = address.substring(6);
			host = getHostName(shortAddress);
			port = getPort(shortAddress, 8883);
			SSLSocketFactoryFactory factoryFactory = null;
			if (factory == null) {
//				try {
					factoryFactory = new SSLSocketFactoryFactory();
					Properties sslClientProps = options.getSSLProperties();
					if (null != sslClientProps)
						factoryFactory.initialize(sslClientProps, null);
					factory = factoryFactory.createSocketFactory(null);
//				}
//				catch (MqttDirectException ex) {
//					throw ExceptionHelper.createMqttException(ex.getCause());
//				}
			}
			else if ((factory instanceof SSLSocketFactory) == false) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}

			// Create the network module...
			netModule = new SSLNetworkModule((SSLSocketFactory) factory, host, port, clientId);
			((SSLNetworkModule)netModule).setSSLhandshakeTimeout(options.getConnectionTimeout());
			// Ciphers suites need to be set, if they are available
			if (factoryFactory != null) {
				String[] enabledCiphers = factoryFactory.getEnabledCipherSuites(null);
				if (enabledCiphers != null) {
					((SSLNetworkModule) netModule).setEnabledCiphers(enabledCiphers);
				}
			}
			break;
		case MqttConnectOptions.URI_TYPE_LOCAL :
			netModule = new LocalNetworkModule(address.substring(8));
			break;
		default:
			// This shouldn't happen, as long as validateURI() has been called.
			netModule = null;
		}
		return netModule;
	}

	private int getPort(String uri, int defaultPort) {
		int port;
		int portIndex = uri.lastIndexOf(':');
		if (portIndex == -1) {
			port = defaultPort;
		}
		else {
			port = Integer.valueOf(uri.substring(portIndex + 1)).intValue();
		}
		return port;
	}

	private String getHostName(String uri) {
		int schemeIndex = uri.lastIndexOf('/');
		int portIndex = uri.lastIndexOf(':');
		if (portIndex == -1) {
			portIndex = uri.length();
		}
		return uri.substring(schemeIndex + 1, portIndex);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken connect(Object userContext, IMqttActionListener callback)
			throws MqttException, MqttSecurityException {
		return this.connect(new MqttConnectOptions(), userContext, callback);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect()
	 */
	public IMqttToken connect() throws MqttException, MqttSecurityException {
		return this.connect(null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(org.eclipse.paho.client.mqttv3.MqttConnectOptions)
	 */
	public IMqttToken connect(MqttConnectOptions options) throws MqttException, MqttSecurityException {
		return this.connect(options, null,null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(org.eclipse.paho.client.mqttv3.MqttConnectOptions, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback)
			throws MqttException, MqttSecurityException {
		final String methodName = "connect";
		if (comms.isConnected()) {
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
		}
		if (comms.isConnecting()) {
			throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
		}
		if (comms.isDisconnecting()) {
			throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
		}
		if (comms.isClosed()) {
			throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
		}

		// @TRACE 103=cleanSession={0} connectionTimeout={1} TimekeepAlive={2} userName={3} password={4} will={5} userContext={6} callback={7}
		log.fine(className,methodName, "103",
				new Object[]{
				new Boolean(options.isCleanSession()),
				new Integer(options.getConnectionTimeout()),
				new Integer(options.getKeepAliveInterval()),
				options.getUserName(),
				((null == options.getPassword())?"[null]":"[notnull]"),
				((null == options.getWillMessage())?"[null]":"[notnull]"),
				userContext,
				callback });
		comms.setNetworkModules(createNetworkModules(serverURI, options));

		// Insert our own callback to iterate through the URIs till the connect succeeds
		MqttToken userToken = new MqttToken(getClientId());
		ConnectActionListener connectActionListener = new ConnectActionListener(this, persistence, comms, options, userToken, userContext, callback);
		userToken.setActionCallback(connectActionListener);
		userToken.setUserContext(this);

		comms.setNetworkModuleIndex(0);
		connectActionListener.connect();

		return userToken;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken disconnect( Object userContext, IMqttActionListener callback) throws MqttException {
		return this.disconnect(30000, userContext, callback);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect()
	 */
	public IMqttToken disconnect() throws MqttException {
		return this.disconnect(null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(long)
	 */
	public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
		return this.disconnect(quiesceTimeout, null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(long, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback) throws MqttException {
		final String methodName = "disconnect";
		// @TRACE 104=> quiesceTimeout={0} userContext={1} callback={2}
		log.fine(className,methodName, "104",new Object[]{ new Long(quiesceTimeout), userContext, callback});

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);

		MqttDisconnect disconnect = new MqttDisconnect();
		try {
			comms.disconnect(disconnect, quiesceTimeout, token);
		} catch (MqttException ex) {
			//@TRACE 105=< exception
			log.fine(className,methodName,"105",null,ex);
			throw ex;
		}
		//@TRACE 108=<
		log.fine(className,methodName,"108");

		return token;
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#isConnected()
	 */
	public boolean isConnected() {
		return comms.isConnected();
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#getClientId()
	 */
	public String getClientId() {
		return clientId;
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#getServerURI()
	 */
	public String getServerURI() {
		return serverURI;
	}

	/**
	 * Get a topic object which can be used to publish messages.
	 * <p>There are two alternative methods that should be used in preference to this one when publishing a message:
	 * <ul>
	 * <li>{@link MqttAsyncClient#publish(String, MqttMessage, MqttDeliveryToken)} to publish a message in a non-blocking manner or
	 * <li>{@link MqttClient#publishBlock(String, MqttMessage, MqttDeliveryToken)} to publish a message in a blocking manner
	 * </ul>
	 * </p>
	 * <p>When you build an application,
	 * the design of the topic tree should take into account the following principles
	 * of topic name syntax and semantics:</p>
	 *
	 * <ul>
	 * 	<li>A topic must be at least one character long.</li>
	 * 	<li>Topic names are case sensitive.  For example, <em>ACCOUNTS</em> and <em>Accounts</em> are
	 * 	two different topics.</li>
	 * 	<li>Topic names can include the space character.  For example, <em>Accounts
	 * 	payable</em> is a valid topic.</li>
	 * 	<li>A leading "/" creates a distinct topic.  For example, <em>/finance</em> is
	 * 	different from <em>finance</em>. <em>/finance</em> matches "+/+" and "/+", but
	 * 	not "+".</li>
	 * 	<li>Do not include the null character (Unicode<samp class="codeph"> \x0000</samp>) in
	 * 	any topic.</li>
	 * </ul>
	 *
	 * <p>The following principles apply to the construction and content of a topic
	 * tree:</p>
	 *
	 * <ul>
	 * 	<li>The length is limited to 64k but within that there are no limits to the
	 * 	number of levels in a topic tree.</li>
	 * 	<li>There can be any number of root nodes; that is, there can be any number
	 * 	of topic trees.</li>
	 * 	</ul>
	 * </p>
	 *
	 * @param topic the topic to use, for example "finance/stock/ibm".
	 * @return an MqttTopic object, which can be used to publish messages to
	 * the topic.
	 * @throws IllegalArgumentException if the topic contains a '+' or '#'
	 * wildcard character.
	 */
	protected MqttTopic getTopic(String topic) {
		validateTopic(topic);

		MqttTopic result = (MqttTopic)topics.get(topic);
		if (result == null) {
			result = new MqttTopic(topic, comms);
			topics.put(topic,result);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(java.lang.String, int, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback) throws MqttException {
		return this.subscribe(new String[] {topicFilter}, new int[] {qos}, userContext, callback);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(java.lang.String, int)
	 */
	public IMqttToken subscribe(String topicFilter, int qos) throws MqttException {
		return this.subscribe(new String[] {topicFilter}, new int[] {qos}, null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(java.lang.String[], int[])
	 */
	public IMqttToken subscribe(String[] topicFilters, int[] qos) throws MqttException {
		return this.subscribe(topicFilters, qos, null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(java.lang.String[], int[], java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback) throws MqttException {
		final String methodName = "subscribe";

		if (topicFilters.length != qos.length) {
			throw new IllegalArgumentException();
		}
		String subs = "";
		for (int i=0;i<topicFilters.length;i++) {
			if (i>0) {
				subs+=", ";
			}
			subs+=topicFilters[i]+":"+qos[i];
		}
		//@TRACE 106=Subscribe topic={0} userContext={1} callback={2}
		log.fine(className,methodName,"106",new Object[]{subs, userContext, callback});

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.internalTok.setTopics(topicFilters);

		MqttSubscribe register = new MqttSubscribe(topicFilters, qos);

		comms.sendNoWait(register, token);
		//@TRACE 109=<
		log.fine(className,methodName,"109");

		return token;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(java.lang.String, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken unsubscribe(String topicFilter,  Object userContext, IMqttActionListener callback) throws MqttException {
		return unsubscribe(new String[] {topicFilter}, userContext, callback);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(java.lang.String)
	 */
	public IMqttToken unsubscribe(String topicFilter) throws MqttException {
		return unsubscribe(new String[] {topicFilter}, null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(java.lang.String[])
	 */
	public IMqttToken unsubscribe(String[] topicFilters) throws MqttException {
		return unsubscribe(topicFilters, null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(java.lang.String[], java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken unsubscribe(String[] topicFilters, Object userContext, IMqttActionListener callback) throws MqttException {
		final String methodName = "unsubscribe";
		String subs = "";
		for (int i=0;i<topicFilters.length;i++) {
			if (i>0) {
				subs+=", ";
			}
			subs+=topicFilters[i];
		}
		//@TRACE 107=Unsubscribe topic={0} userContext={1} callback={2}
		log.fine(className, methodName,"107",new Object[]{subs, userContext, callback});

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.internalTok.setTopics(topicFilters);

		MqttUnsubscribe unregister = new MqttUnsubscribe(topicFilters);

		comms.sendNoWait(unregister, token);
		//@TRACE 110=<
		log.fine(className,methodName,"110");

		return token;
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#setCallback(MqttCallback)
	 */
	public void setCallback(MqttCallback callback) {
		comms.setCallback(callback);
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
		return (System.getProperty("user.name") + "." + System.currentTimeMillis());
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#getPendingDeliveryTokens()
	 */
	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return comms.getPendingDeliveryTokens();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(java.lang.String, byte[], int, boolean, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
			boolean retained, Object userContext, IMqttActionListener callback) throws MqttException,
			MqttPersistenceException {
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		return this.publish(topic, message, userContext, callback);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(java.lang.String, byte[], int, boolean)
	 */
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
			boolean retained) throws MqttException, MqttPersistenceException {
		return this.publish(topic, payload, qos, retained, null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage)
	 */
	public IMqttDeliveryToken publish(String topic, MqttMessage message) throws MqttException, 	MqttPersistenceException {
		return this.publish(topic, message, null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, IMqttActionListener callback) throws MqttException,
			MqttPersistenceException {
		final String methodName = "publish";
		//@TRACE 111=< topic={0} message={1}userContext={1} callback={2}
		log.fine(className,methodName,"111", new Object[] {topic, userContext, callback});

		validateTopic(topic);

		MqttDeliveryToken token = new MqttDeliveryToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.setMessage(message);
		token.internalTok.setTopics(new String[] {topic});

		MqttPublish pubMsg = new MqttPublish(topic, message);
		comms.sendNoWait(pubMsg, token);

		//@TRACE 112=<
		log.fine(className,methodName,"112");

		return token;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#close()
	 */
	public void close() throws MqttException {
		final String methodName = "close";
		//@TRACE 113=<
		log.fine(className,methodName,"113");
		comms.close();
		//@TRACE 114=>
		log.fine(className,methodName,"114");

	}

	/**
	 * Return a debug object that can be used to help solve problems.
	 */
	public Debug getDebug() {
		return new Debug(clientId,comms);
	}

	/**
	 * Checks a topic is valid when publishing a message.
	 * <p>Checks the topic does not contain a wild card character.</p>
	 * @param topic to validate
	 * @throws IllegalArgumentException if the topic is not valid
	 */
	static public void validateTopic(String topic) {
		if ((topic.indexOf('#') == -1) && (topic.indexOf('+') == -1)) {
			return;
		}
		// The topic string does not comply with topic string rules.
		throw new IllegalArgumentException();
	}
}
