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
import org.eclipse.paho.client.mqttv3.internal.DestinationProvider;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.internal.LocalNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.internal.SSLNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.TCPNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.comms.MqttDirectException;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttUnsubscribe;


/**
 * Lightweight client for talking to a server via the MQTT version 3 
 * protocol.  The client allows an application to use publish/subscribe 
 * messaging.
 */
public class MqttClient implements DestinationProvider {
	private static final int URI_TYPE_TCP = 0;
	private static final int URI_TYPE_SSL = 1;
	private static final int URI_TYPE_LOCAL = 2;
	
	private String clientId;
	private String serverURI;
	private int serverURIType;
	private ClientComms comms;
	private Hashtable topics;
	private MqttClientPersistence persistence;
	
	private Trace trace;
	
	/**
	 * Creates an MqttClient to connect to the specified address, using the
	 * specified client identifier.  The address
	 * should be a URI, using a scheme of either "tcp://" for a TCP connection
	 * or "ssl://" for a TCP connection secured by SSL/TLS. For example:
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * <p>
	 * If the port is not specified, it will
	 * default to 1883 for "tcp://" URIs, and 8883 for "ssl://" URIs.
	 * </p>
	 * <p>
	 * The client identifier should be unique across all clients connecting to the same
	 * server. A convenience method is provided to generate a random client id that
	 * should satisfy this criteria - {@link #generateClientId()}. As the client identifier
	 * is used by the server to identify a client when it reconnects, the client must use the
	 * same identifier between connections if durable subscriptions are to be used.
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
	 * <p>A default instance of {@link MqttDefaultFilePersistence} is used by
	 * the client. To specify a different persistence implementation, or to turn
	 * off persistence, use the {@link #MqttClient(String, String, MqttClientPersistence)} constructor.
	 *  
	 * @param serverURI the address to connect to, specified as a URI
	 * @param clientId the client ID to use
	 * @throws IllegalArgumentException if the URI does not start with
	 * "tcp://", "ssl://" or "local://".
	 * @throws IllegalArgumentException if the clientId is null or is greater than 23 characters in length
	 * @throws MqttException if any other problem was encountered 
	 */
	public MqttClient(String serverURI, String clientId) throws MqttException {
		this(serverURI,clientId, new MqttDefaultFilePersistence());
	}
	
	/**
	 * Creates an MqttClient to connect to the specified address, using the
	 * specified client identifer and persistence implementation.  The address
	 * should be a URI, using a scheme of either "tcp://" for a TCP connection
	 * or "ssl://" for a TCP connection secured by SSL/TLS. For example:
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * 	<li><code>local://FirstBroker</code></li>
	 * </ul>
	 * <p>
	 * If the port is not specified, it will
	 * default to 1883 for "tcp://" URIs, and 8883 for "ssl://" URIs.
	 * </p>
	 * <p>
	 * The client identifier should be unique across all clients connecting to the same
	 * server. A convenience method is provided to generate a random client id that
	 * should satisfy this criteria - {@link #generateClientId()}. As the client identifier
	 * is used by the server to identify a client when it reconnects, the client must use the
	 * same identifier between connections if durable subscriptions are to be used.
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
	 * The persistence mechanism is used to enable reliable messaging.
	 * For qualities of server (QoS) 1 or 2 to work, messages must be persisted
	 * to disk by both the client and the server.  If this is not done, then
	 * a failure in the client or server will result in lost messages.   It
	 * is the application's responsibility to provide an implementation of the
	 * {@link MqttClientPersistence} interface, which the client can use to
	 * persist messages.  If the application is only sending QoS 0 messages, 
	 * then this is not needed.
	 * 
	 * <p>An implementation of file-based persistence is provided in the 
	 * class {@link MqttDefaultFilePersistence}.
	 * If no persistence is needed, it can be explicitly set to <code>null</code>.</p>
	 * 
	 * @param serverURI the address to connect to, specified as a URI
	 * @param clientId the client ID to use
 	 * @param persistence the persistence mechanism to use.
	 * @throws IllegalArgumentException if the URI does not start with
	 * "tcp://", "ssl://" or "local://".
	 * @throws IllegalArgumentException if the clientId is null or is greater than 23 characters in length
	 * @throws MqttException if any other problem was encountered 
	 */
	public MqttClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
		if (clientId == null || clientId.length() == 0 || clientId.length() > 23) {
			throw new IllegalArgumentException();
		}
		this.trace = Trace.getTrace(clientId);
		
		this.serverURI = serverURI;
		this.serverURIType = validateURI(serverURI);
		this.clientId = clientId;
		
		this.persistence = persistence;
		if (this.persistence == null) {
			this.persistence = new MemoryPersistence();
		}
		
		// @TRACE 101=New Client Instance ClientID={0} ServerURI={1} PersistenceType={2}  	
		trace.trace(Trace.FINE,101,new Object[]{clientId,serverURI,persistence});

		this.persistence.open(clientId, serverURI);
		this.comms = new ClientComms(this, this.persistence, trace);
		this.persistence.close();
		this.topics = new Hashtable();

	}
	
	private int validateURI(String serverURI) {
		if (serverURI.startsWith("tcp://")) {
			return URI_TYPE_TCP;
		}
		else if (serverURI.startsWith("ssl://")) {
			return URI_TYPE_SSL;
		}
		else if (serverURI.startsWith("local://")) {
			return URI_TYPE_LOCAL;
		}
		else {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Factory method to create the correct network module, based on the
	 * supplied address URI.
	 * 
	 * @param address the URI for the server.
	 * @return a network module appropriate to the specified address.
	 */
	protected NetworkModule createNetworkModule(String address, MqttConnectOptions options) throws MqttException {
		NetworkModule netModule;
		String shortAddress;
		String host;
		int port;
		SocketFactory factory = options.getSocketFactory();
		switch (serverURIType) {
		case URI_TYPE_TCP:
			shortAddress = address.substring(6);
			host = getHostName(shortAddress);
			port = getPort(shortAddress, 1883);
			if (factory == null) {
				factory = SocketFactory.getDefault();
				options.setSocketFactory(factory);
			}
			else if (factory instanceof SSLSocketFactory) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}
			netModule = new TCPNetworkModule(trace, factory, host, port);
			break;
		case URI_TYPE_SSL:
			shortAddress = address.substring(6);
			host = getHostName(shortAddress);
			port = getPort(shortAddress, 8883);
			SSLSocketFactoryFactory factoryFactory = null;
			if (factory == null) {
				try {
					factoryFactory = new SSLSocketFactoryFactory();
					Properties sslClientProps = options.getSSLProperties();
					if (null != sslClientProps)
						factoryFactory.initialize(sslClientProps, null);
					factory = factoryFactory.createSocketFactory(null);
				}
				catch (MqttDirectException ex) {
					/*One of cases when you will reach here is when the truststore is created in DeviceEE 
						  and loaded in J2SE. This will lead to a java.io.IOException: Invalid keystore format
						  which is encapsulated in the MQTT exception and thrown.
					 */
					throw ExceptionHelper.createMqttException(ex.getCause());
				}
			}
			else if ((factory instanceof SSLSocketFactory) == false) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}

			// Create the network module...
			netModule = new SSLNetworkModule(trace, (SSLSocketFactory) factory, host, port);
			((SSLNetworkModule)netModule).setSSLhandshakeTimeout(options.getConnectionTimeout());
			// Ciphers suites need to be set, if they are available
			if (factoryFactory != null) {
				String[] enabledCiphers = factoryFactory.getEnabledCipherSuites(null);
				if (enabledCiphers != null) {
					((SSLNetworkModule) netModule).setEnabledCiphers(enabledCiphers);
				}
			}
			break;
		case URI_TYPE_LOCAL:
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
	
	/**
	 * Connects to a server using the default options.
	 * It is recommended to call {@link #setCallback(MqttCallback)} prior to
	 * connecting.
	 */
	public void connect() throws MqttSecurityException, MqttException {
		this.connect(new MqttConnectOptions());
	}
	
	/**
	 * Connects to a server using the specified options.
	 * It is recommended to call {@link #setCallback(MqttCallback)} prior to
	 * connecting.
	 */
	public void connect(MqttConnectOptions options) throws MqttSecurityException, MqttException {
		if (this.isConnected()) {
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_ALREADY_CONNECTED);
		}

		// @TRACE 103=Connect cleanSession={0} connectionTimeout={1} TimekeepAlive={2} userName={3} password={4} will={5}
		if (trace.isOn()) {
			trace.trace(Trace.FINE,103,
					new Object[]{ 
					new Boolean(options.isCleanSession()),
					new Integer(options.getConnectionTimeout()),
					new Integer(options.getKeepAliveInterval()),
					options.getUserName(),
					((null == options.getPassword())?"[null]":"[notnull]"),
					((null == options.getWillMessage())?"[null]":"[notnull]")});
		}
		comms.setNetworkModule(createNetworkModule(serverURI, options));

		this.persistence.open(clientId, serverURI);

		if (options.isCleanSession()) {
			persistence.clear();
		}
		comms.connect(
				new MqttConnect(clientId,
						options.isCleanSession(),
						options.getKeepAliveInterval(),
						options.getUserName(),
						options.getPassword(),
						options.getWillMessage(),
						options.getWillDestination()),
						options.getConnectionTimeout(),
						options.getKeepAliveInterval(),
						options.isCleanSession()
		);
	}

	/**
	 * Disconnects from the server, which quiesces for up to a
	 * maximum of thirty seconds, to allow the client to finish any work it 
	 * currently has.
	 * 
	 *  @see #disconnect(long)
	 */
	public void disconnect() throws MqttException {
		this.disconnect(30000);
	}
	
	/**
	 * Disconnects from the server.
	 * This method must not be called from inside {@link MqttCallback} methods.
	 * <p>
	 * Firstly, the client will wait for all {@link MqttCallback} methods to 
	 * complete.  It will then quiesce for the specified time, to allow for
	 * work which has already been accepted to complete - for example, it will
	 * wait for the QoS 2 flows from earlier publications to complete.  After
	 * the quiesce timeout, the client will disconnect from the server.  When
	 * the client is next connected, any QoS 1 or 2 messages which have not 
	 * completed will be retried.</p>
	 * 
	 * @param quiesceTimeout the amount of time in milliseconds to allow for existing work to finish
	 * before disconnecting.  A value of zero or less means the client will
	 * not quiesce.
	 */
	public void disconnect(long quiesceTimeout) throws MqttException {
		// @TRACE 104=Disconnect quiesceTimeout={0}
		trace.trace(Trace.FINE,104,new Object[]{ new Long(quiesceTimeout) });
		MqttDisconnect disconnect = new MqttDisconnect();
		try {
			comms.disconnect(disconnect, quiesceTimeout);
		}
		catch (MqttException ex) {
			//@TRACE 105=Disconnect exception
			trace.trace(Trace.FINE,105,null,ex);
			throw ex;
		}
	}

	/**
	 * Determines if this client is currently connected to the
	 * server.
	 * 
	 * @return <code>true</code> if connected, <code>false</code> otherwise.
	 */
	public boolean isConnected() {
		return comms.isConnected();
	}
	
	/**
	 * Returns the client ID used by this client.
	 * 
	 * @return the client ID used by this client.
	 */
	public String getClientId() {
		return clientId;
	}
	
	/**
	 * Returns the address of the server used by this client, as a URI.
	 * The format is the same as specified on the constructor.
	 * 
	 * @return the server's address, as a URI String.
	 * @see #MqttClient(String, String)
	 */
	public String getServerURI() {
		return serverURI;
	}
	
	/**
	 * Gets a topic object which can be used to publish messages.
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
	public MqttTopic getTopic(String topic) {
		if ((topic.indexOf('#') == -1) && (topic.indexOf('+') == -1)) {
			MqttTopic result = (MqttTopic)topics.get(topic);
			if (result == null) {
				result = new MqttTopic(topic, comms);
				topics.put(topic,result);
			}
			return result;
		}
		else {
			throw new IllegalArgumentException();
		}
	}
	
	/** 
	 * Subscribes to a topic, which may include wildcards, using the default
	 * options.  The {@link #setCallback(MqttCallback)} method should be called
	 * before this method, otherwise any received messages will be discarded.
	 * 
	 * @see #subscribe(String[], int[])
	 */
	public void subscribe(String topicFilter) throws MqttException, MqttSecurityException {
		this.subscribe(new String[] {topicFilter}, new int[] {1});
	}
	
	/** 
	 * Subscribes to multiple topics, each of which may include wildcards, 
	 * using the default options.  The {@link #setCallback(MqttCallback)} method should be called
	 * before this method, otherwise any received messages will be discarded.
	 * 
	 * @see #subscribe(String[], int[])
	 */
	public void subscribe(String[] topicFilters) throws MqttException, MqttSecurityException {
		int[] qos = new int[topicFilters.length];
		for (int i=0; i<qos.length; i++) {
			qos[i] = 1; 
		}
		this.subscribe(topicFilters, qos);
	}
	
	/**
	 * Subscribes to a topic, which may include wildcards, using the specified
	 * options.  The {@link #setCallback(MqttCallback)} method should be called
	 * before this method, otherwise any received messages will be discarded.
	 * 
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the quality of service at which to subscribe. 
	 * @see #subscribe(String[], int[])
	 */
	public void subscribe(String topicFilter, int qos) throws MqttException, MqttSecurityException {
		this.subscribe(new String[] {topicFilter}, new int[] {qos});
	}

	/**
	 * Subscribes to multiple topics, each of which may include wildcards, 
	 * using the specified options.  The {@link #setCallback(MqttCallback)} method should be called
	 * before this method, otherwise any received messages will be discarded.
	 * 
	 * <p>The "topic filter" string used when subscribing
	 * may contain special characters, which allow you to subscribe to multiple topics
	 * at once.</p>
	 * <p>The topic level separator is used to introduce structure into the topic, and
	 * can therefore be specified within the topic for that purpose.  The multi-level
	 * wildcard and single-level wildcard can be used for subscriptions, but they
	 * cannot be used within a topic by the publisher of a message.
	 * <dl>
	 * 	<dt>Topic level separator</dt>
	 * 	<dd>The forward slash (/) is used to separate each level within
	 * 	a topic tree and provide a hierarchical structure to the topic space. The
	 * 	use of the topic level separator is significant when the two wildcard characters
	 * 	are encountered in topics specified by subscribers.</dd>
	 * 
	 * 	<dt>Multi-level wildcard</dt>
	 * 	<dd><p>The number sign (#) is a wildcard character that matches
	 * 	any number of levels within a topic. For example, if you subscribe to <span><span class="filepath">finance/stock/ibm/#</span></span>, you receive
	 * 	messages on these topics:<pre>   finance/stock/ibm<br />   finance/stock/ibm/closingprice<br />   finance/stock/ibm/currentprice</pre>
	 * </p>
	 * <p>The multi-level wildcard
	 * can represent zero or more levels. Therefore, <em>finance/#</em> can also match
	 * 	the singular <em>finance</em>, where <em>#</em> represents zero levels. The topic
	 * 	level separator is meaningless in this context, because there are no levels
	 * 	to separate.</p>
	 * 
	 * 	<p>The <span>multi-level</span> wildcard can
	 * 	be specified only on its own or next to the topic level separator character.
	 * 	Therefore, <em>#</em> and <em>finance/#</em> are both valid, but <em>finance#</em> is
	 * 	not valid. <span>The multi-level wildcard must be the last character
	 *  used within the topic tree. For example, <em>finance/#</em> is valid but <em>finance/#/closingprice</em> is
	 * 	not valid.</span></p></dd>
	 * 
	 * 	<dt>Single-level wildcard</dt>
	 * 	<dd><p>The plus sign (+) is a wildcard character that matches only one topic
	 * 	level. For example, <em>finance/stock/+</em> matches <em>finance/stock/ibm</em> and <em>finance/stock/xyz</em>,
	 * 	but not <em>finance/stock/ibm/closingprice</em>. Also, because the single-level
	 * 	wildcard matches only a single level, <em>finance/+</em> does not match <em>finance</em>.</p>
	 * 	
	 * 	<p>Use
	 * 	the single-level wildcard at any level in the topic tree, and in conjunction
	 * 	with the multilevel wildcard. Specify the single-level wildcard next to the
	 * 	topic level separator, except when it is specified on its own. Therefore, <em>+</em> and <em>finance/+</em> are
	 * 	both valid, but <em>finance+</em> is not valid. <span>The single-level
	 * 	wildcard can be used at the end of the topic tree or within the topic tree.
	 * 	For example, <em>finance/+</em> and <em>finance/+/ibm</em> are both valid.</span></p>
	 * 	</dd>
	 * </dl>
	 * </p>
	 * 
	 * @param topicFilters the topics to subscribe to, which can include wildcards.
	 * @param qos the qualities of service levels at which to subscribe. 
	 * @throws MqttException if there was an error registering the subscription.
	 * @throws IllegalArgumentException if the two supplied arrays are not the same size.
	 */
	public void subscribe(String[] topicFilters, int[] qos) throws MqttException, MqttSecurityException {
		if (topicFilters.length != qos.length) {
			throw new IllegalArgumentException();
		}
		if (trace.isOn()) {
			String subs = "";
			for (int i=0;i<topicFilters.length;i++) {
				if (i>0) {
					subs+=", ";
				}
				subs+=topicFilters[i]+":"+qos[i];
			}
			//@TRACE 106=Subscribe topic={0}
			trace.trace(Trace.FINE,106,new Object[]{subs});
		}
		MqttSubscribe register = new MqttSubscribe(topicFilters, qos);
		comms.sendAndWait(register);
	}

	/**
	 * Unsubscribes from a topic.
	 * 
	 * @param topicFilter the topic to unsubscribe from.
	 */
	public void unsubscribe(String topicFilter) throws MqttException {
		unsubscribe(new String[] {topicFilter});
	}
	
	/**
	 * Unsubscribes from multiple topics.
	 * 
	 * @param topicFilters the topics to unsubscribe from.
	 */
	public void unsubscribe(String[] topicFilters) throws MqttException {
		if (trace.isOn()) {
			String subs = "";
			for (int i=0;i<topicFilters.length;i++) {
				if (i>0) {
					subs+=", ";
				}
				subs+=topicFilters[i];
			}
			//@TRACE 107=Unsubscribe topic={0}
			trace.trace(Trace.FINE,107,new Object[]{subs});
		}
		MqttUnsubscribe unregister = new MqttUnsubscribe(topicFilters);
		comms.sendAndWait(unregister);
	}
	
	/**
	 * Sets the callback listener to use for asynchronously received
	 * messages.
	 * The 
	 * {@link MqttCallback#messageArrived(MqttTopic, MqttMessage)}
	 * method will be called back whenever a message arrives.
	 * 
	 * @param callback the class to callback when a message arrives.
	 */
	public void setCallback(MqttCallback callback) throws MqttException {
		comms.setCallback(callback);
	}
	
	/**
	 * Returns a randomly generated client identifier based on the current user's login
	 * name and the system time.
	 * <p>When cleanSession is set to false, an application should ensure it uses the 
	 * same client identifier when it reconnects to the server to resume state and maintain
	 *  assured message delivery.</p>
	 * @return a generated client identifier
	 * @see MqttConnectOptions#setCleanSession(boolean)
	 */
	public static String generateClientId() {
		return (System.getProperty("user.name") + "." + System.currentTimeMillis());
	}
	
	/**
	 * Returns the delivery tokens for any outstanding publish operations.
	 */
	public MqttDeliveryToken[] getPendingDeliveryTokens() {
		return comms.getPendingDeliveryTokens();
	}
}
