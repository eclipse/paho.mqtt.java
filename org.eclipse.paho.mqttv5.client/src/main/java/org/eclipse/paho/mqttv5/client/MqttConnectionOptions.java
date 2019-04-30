/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corp.
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
 *    James Sutton - Automatic Reconnect & Offline Buffering
 *    James Sutton - MQTT v5
 */
package org.eclipse.paho.mqttv5.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;

//import org.eclipse.paho.mqttv5.client.internal.NetworkModuleService;
//import org.eclipse.paho.mqttv5.client.vertx.util.Debug;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;

/**
 * Holds the set of options that control how the client connects to a server.
 * 
 * Constructs a new {@link MqttConnectionOptions} object using the default
 * values.
 *
 * The defaults are:
 * <ul>
 * <li>The keep alive interval is 60 seconds</li>
 * <li>Clean Session is true</li>
 * <li>The message delivery retry interval is 15 seconds</li>
 * <li>The connection timeout period is 30 seconds</li>
 * <li>No Will message is set</li>
 * <li>Automatic Reconnect is enabled, starting at 1 second and capped at two
 * minutes</li>
 * <li>A standard SocketFactory is used</li>
 * </ul>
 * More information about these values can be found in the setter methods.
 */
public class MqttConnectionOptions {

	private static final String CLIENT_ID_PREFIX = "paho";

	// Connection Behaviour Properties
	private String[] serverURIs = null; // List of Servers to connect to in order
	private boolean automaticReconnect = false; // Automatic Reconnect
	private int automaticReconnectMinDelay = 1; // Time to wait before first automatic reconnection attempt in seconds.
	private int automaticReconnectMaxDelay = 120; // Max time to wait for automatic reconnection attempts in seconds.
	private boolean useSubscriptionIdentifiers = true; // Whether to automatically assign subscription identifiers.
	private int keepAliveInterval = 60; // Keep Alive Interval
	private int connectionTimeout = 30; // Connection timeout in seconds
	private boolean httpsHostnameVerificationEnabled = true;
	private int maxReconnectDelay = 128000;
	private boolean sendReasonMessages = false;

	public MqttProperties getConnectionProperties() {
		MqttProperties connectionProperties = new MqttProperties();
		connectionProperties.setSessionExpiryInterval(sessionExpiryInterval);
		connectionProperties.setReceiveMaximum(receiveMaximum);
		connectionProperties.setMaximumPacketSize(maximumPacketSize);
		connectionProperties.setTopicAliasMaximum(topicAliasMaximum);
		connectionProperties.setRequestResponseInfo(requestResponseInfo);
		connectionProperties.setRequestProblemInfo(requestProblemInfo);
		connectionProperties.setUserProperties(userProperties);
		connectionProperties.setAuthenticationMethod(authMethod);
		connectionProperties.setAuthenticationData(authData);
		return connectionProperties;
	}

	public MqttProperties getWillMessageProperties() {
		return willMessageProperties;
	}

	public void setWillMessageProperties(MqttProperties willMessageProperties) {
		this.willMessageProperties = willMessageProperties;
	}

	MqttProperties willMessageProperties = new MqttProperties();

	// Connection packet properties
	private int mqttVersion = 5; // MQTT Version 5
	private boolean cleanStart = true; // Clean Session
	private String willDestination = null; // Will Topic
	private MqttMessage willMessage = null; // Will Message
	private String userName; // Username
	private byte[] password; // Password
	private Long sessionExpiryInterval = null; // The Session expiry Interval in seconds, null is the default of
												// never.
	private Integer receiveMaximum = null; // The Receive Maximum, null defaults to 65,535, cannot be 0.
	private Long maximumPacketSize = null; // The Maximum packet size, null defaults to no limit.
	private Integer topicAliasMaximum = null; // The Topic Alias Maximum, null defaults to 0.
	private Boolean requestResponseInfo = null; // Request Response Information, null defaults to false.
	private Boolean requestProblemInfo = null; // Request Problem Information, null defaults to true.
	private List<UserProperty> userProperties = null; // User Defined Properties.
	private String authMethod = null; // Authentication Method, If null, Extended Authentication is not performed.
	private byte[] authData = null; // Authentication Data.

	// TLS Properties
	private SocketFactory socketFactory; // SocketFactory to be used to connect
	private Properties sslClientProps = null; // SSL Client Properties
	private HostnameVerifier sslHostnameVerifier = null; // SSL Hostname Verifier
	private Map<String, String> customWebSocketHeaders;

	// Client Operation Parameters
	private int executorServiceTimeout = 1; // How long to wait in seconds when terminating the executor service.

	/**
	 * Returns the MQTT version.
	 * 
	 * @return the MQTT version.
	 */
	public int getMqttVersion() {
		return mqttVersion;
	}

	/**
	 * Returns the user name to use for the connection.
	 * 
	 * @return the user name to use for the connection.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Sets the user name to use for the connection.
	 * 
	 * @param userName
	 *            The Username as a String
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Returns the password to use for the connection.
	 * 
	 * @return the password to use for the connection.
	 */
	public byte[] getPassword() {
		return password;
	}

	/**
	 * Sets the password to use for the connection.
	 * 
	 * @param password
	 *            A Char Array of the password
	 */
	public void setPassword(byte[] password) {
		this.password = password;
	}

	/**
	 * Returns the topic to be used for last will and testament (LWT).
	 * 
	 * @return the MqttTopic to use, or <code>null</code> if LWT is not set.
	 * @see #setWill(String, MqttMessage)
	 */
	public String getWillDestination() {
		return willDestination;
	}

	/**
	 * Returns the message to be sent as last will and testament (LWT). The returned
	 * object is "read only". Calling any "setter" methods on the returned object
	 * will result in an <code>IllegalStateException</code> being thrown.
	 * 
	 * @return the message to use, or <code>null</code> if LWT is not set.
	 */
	public MqttMessage getWillMessage() {
		return willMessage;
	}

	/**
	 * Sets the "Last Will and Testament" (LWT) for the connection. In the event
	 * that this client unexpectedly looses it's connection to the server, the
	 * server will publish a message to itself using the supplied details.
	 * 
	 * @param topic
	 *            the topic to publish to.
	 * @param message
	 *            the {@link MqttMessage} to send
	 */
	public void setWill(String topic, MqttMessage message) {
		if (topic == null || message == null || message.getPayload() == null) {
			throw new IllegalArgumentException();
		}
		MqttTopicValidator.validate(topic, false, true); // Wildcards are not allowed
		this.willDestination = topic;
		this.willMessage = message;
		// Prevent any more changes to the will message
		this.willMessage.setMutable(false);
	}

	/**
	 * Returns whether the client and server should remember state for the client
	 * across reconnects.
	 * 
	 * @return the clean session flag
	 */
	public boolean isCleanStart() {
		return this.cleanStart;
	}

	/**
	 * Sets whether the client and server should remember state across restarts and
	 * reconnects. If set to false, the server will retain the session state until
	 * either:
	 * <ul>
	 * <li>A new connection is made with the client and with the cleanStart flag set
	 * to true.</li>
	 * <li>The Session expiry interval is exceeded after the network connection is
	 * closed, see {@link MqttConnectionOptions#setSessionExpiryInterval}</li>
	 * </ul>
	 * 
	 * If set to true, the server will immediately drop any existing session state
	 * for the given client and will initiate a new session.
	 * 
	 * In order to implement QoS 1 and QoS 2 protocol flows the Client and Server
	 * need to associate state with the Client Identifier, this is referred to as
	 * the Session State. The Server also stores the subscriptions as part of the
	 * Session State.
	 * 
	 * The session can continue across a sequence of Network Connections. It lasts
	 * as long as the latest Network Connection plus the Session Expiry Interval.
	 * 
	 * The Session State in the Client consists of:
	 * 
	 * <ul>
	 * <li>QoS 1 and QoS 2 messages which have been sent to the Server, but have not
	 * been completely acknowledged.</li>
	 * <li>QoS 2 messages which have been received from the Server, but have not
	 * been completely acknowledged.</li>
	 * </ul>
	 * 
	 * The Session State in the Server consists of:
	 * <ul>
	 * <li>The existence of a Session, even if the rest of the Session State is
	 * empty.</li>
	 * <li>The Clients subscriptions, including any Subscription Identifiers.</li>
	 * <li>QoS 1 and QoS 2 messages which have been sent to the Client, but have not
	 * been completely acknowledged.</li>
	 * <li>QoS 1 and QoS 2 messages pending transmission to the Client and
	 * OPTIONALLY QoS 0 messages pending transmission to the Client.</li>
	 * <li>QoS 2 messages which have been received from the Client, but have not
	 * been completely acknowledged.The Will Message and the Will Delay
	 * Interval</li>
	 * <li>If the Session is currently not connected, the time at which the Session
	 * will end and Session State will be discarded.</li>
	 * </ul>
	 * 
	 * Retained messages do not form part of the Session State in the Server, they
	 * are not deleted as a result of a Session ending.
	 * 
	 * 
	 * 
	 * @param cleanStart
	 *            Set to True to enable cleanSession
	 */
	public void setCleanStart(boolean cleanStart) {
		this.cleanStart = cleanStart;
	}

	/**
	 * Returns the "keep alive" interval.
	 * 
	 * @see #setKeepAliveInterval(int)
	 * @return the keep alive interval.
	 */
	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	/**
	 * Sets the "keep alive" interval. This value, measured in seconds, defines the
	 * maximum time interval between messages sent or received. It enables the
	 * client to detect if the server is no longer available, without having to wait
	 * for the TCP/IP timeout. The client will ensure that at least one message
	 * travels across the network within each keep alive period. In the absence of a
	 * data-related message during the time period, the client sends a very small
	 * "ping" message, which the server will acknowledge. A value of 0 disables
	 * keepalive processing in the client.
	 * <p>
	 * The default value is 60 seconds
	 * </p>
	 *
	 * @param keepAliveInterval
	 *            the interval, measured in seconds, must be &gt;= 0.
	 * @throws IllegalArgumentException
	 *             if the keepAliveInterval was invalid
	 */
	public void setKeepAliveInterval(int keepAliveInterval) {
		if (keepAliveInterval < 0) {
			throw new IllegalArgumentException();
		}
		this.keepAliveInterval = keepAliveInterval;
	}

	/**
	 * Returns the connection timeout value.
	 * 
	 * @see #setConnectionTimeout(int)
	 * @return the connection timeout value.
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Sets the connection timeout value. This value, measured in seconds, defines
	 * the maximum time interval the client will wait for the network connection to
	 * the MQTT server to be established. The default timeout is 30 seconds. A value
	 * of 0 disables timeout processing meaning the client will wait until the
	 * network connection is made successfully or fails.
	 * 
	 * @param connectionTimeout
	 *            the timeout value, measured in seconds. It must be &gt;0;
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		if (connectionTimeout < 0) {
			throw new IllegalArgumentException();
		}
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Get the maximum time (in millis) to wait between reconnects
	 * 
	 * @return Get the maximum time (in millis) to wait between reconnects
	 */
	public int getMaxReconnectDelay() {
		return maxReconnectDelay;
	}

	/**
	 * Set the maximum time to wait between reconnects
	 * 
	 * @param maxReconnectDelay
	 *            the duration (in millis)
	 */
	public void setMaxReconnectDelay(int maxReconnectDelay) {
		this.maxReconnectDelay = maxReconnectDelay;
	}

	/**
	 * Return a list of serverURIs the client may connect to
	 * 
	 * @return the serverURIs or null if not set
	 */
	public String[] getServerURIs() {
		return serverURIs;
	}

	/**
	 * Set a list of one or more serverURIs the client may connect to.
	 * <p>
	 * Each <code>serverURI</code> specifies the address of a server that the client
	 * may connect to. Two types of connection are supported <code>tcp://</code> for
	 * a TCP connection and <code>ssl://</code> for a TCP connection secured by
	 * SSL/TLS. For example:
	 * <ul>
	 * <li><code>tcp://localhost:1883</code></li>
	 * <li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * If the port is not specified, it will default to 1883 for
	 * <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * <p>
	 * If serverURIs is set then it overrides the serverURI parameter passed in on
	 * the constructor of the MQTT client.
	 * <p>
	 * When an attempt to connect is initiated the client will start with the first
	 * serverURI in the list and work through the list until a connection is
	 * established with a server. If a connection cannot be made to any of the
	 * servers then the connect attempt fails.
	 * <p>
	 * Specifying a list of servers that a client may connect to has several uses:
	 * <ol>
	 * <li>High Availability and reliable message delivery
	 * <p>
	 * Some MQTT servers support a high availability feature where two or more
	 * "equal" MQTT servers share state. An MQTT client can connect to any of the
	 * "equal" servers and be assured that messages are reliably delivered and
	 * durable subscriptions are maintained no matter which server the client
	 * connects to.
	 * </p>
	 * <p>
	 * The cleanStart flag must be set to false if durable subscriptions and/or
	 * reliable message delivery is required.
	 * </p>
	 * </li>
	 * <li>Hunt List
	 * <p>
	 * A set of servers may be specified that are not "equal" (as in the high
	 * availability option). As no state is shared across the servers reliable
	 * message delivery and durable subscriptions are not valid. The cleanStart flag
	 * must be set to true if the hunt list mode is used
	 * </p>
	 * </li>
	 * </ol>
	 * 
	 * @param serverURIs
	 *            to be used by the client
	 */
	public void setServerURIs(String[] serverURIs) {
		for (String serverURI : serverURIs) {
			//NetworkModuleService.validateURI(serverURI);
		}
		this.serverURIs = serverURIs.clone();
	}

	/**
	 * Returns whether the client will automatically attempt to reconnect to the
	 * server if the connection is lost
	 * 
	 * @return the automatic reconnection flag.
	 */
	public boolean isAutomaticReconnect() {
		return automaticReconnect;
	}

	/**
	 * Sets whether the client will automatically attempt to reconnect to the server
	 * if the connection is lost.
	 * <ul>
	 * <li>If set to false, the client will not attempt to automatically reconnect
	 * to the server in the event that the connection is lost.</li>
	 * <li>If set to true, in the event that the connection is lost, the client will
	 * attempt to reconnect to the server. It will initially wait 1 second before it
	 * attempts to reconnect, for every failed reconnect attempt, the delay will
	 * double until it is at 2 minutes at which point the delay will stay at 2
	 * minutes.</li>
	 * </ul>
	 * 
	 * You can change the Minimum and Maximum delays by using
	 * {@link #setAutomaticReconnectDelay(int, int)}
	 * 
	 * This Defaults to true
	 * 
	 * @param automaticReconnect
	 *            If set to True, Automatic Reconnect will be enabled
	 */
	public void setAutomaticReconnect(boolean automaticReconnect) {
		this.automaticReconnect = automaticReconnect;
	}

	/**
	 * Sets the Minimum and Maximum delays used when attempting to automatically
	 * reconnect.
	 * 
	 * @param minDelay
	 *            the minimum delay to wait before attempting to reconnect in
	 *            seconds, defaults to 1 second.
	 * @param maxDelay
	 *            the maximum delay to wait before attempting to reconnect in
	 *            seconds, defaults to 120 seconds.
	 */
	public void setAutomaticReconnectDelay(int minDelay, int maxDelay) {
		this.automaticReconnectMinDelay = minDelay;
		this.automaticReconnectMaxDelay = maxDelay;
	}

	/**
	 * Returns the minimum number of seconds to wait before attempting to
	 * automatically reconnect.
	 * 
	 * @return the automatic reconnect minimum delay in seconds.
	 */
	public int getAutomaticReconnectMinDelay() {
		return automaticReconnectMinDelay;
	}

	/**
	 * Returns the maximum number of seconds to wait before attempting to
	 * automatically reconnect.
	 * 
	 * @return the automatic reconnect maximum delay in seconds.
	 */
	public int getAutomaticReconnectMaxDelay() {
		return automaticReconnectMaxDelay;
	}

	/**
	 * Returns the Session Expiry Interval. If <code>null</code>, this means the
	 * session will not expire.
	 * 
	 * @return the Session Expiry Interval in seconds.
	 */
	public Long getSessionExpiryInterval() {
		return sessionExpiryInterval;
	}

	/**
	 * Sets the Session Expiry Interval. This value, measured in seconds, defines
	 * the maximum time that the broker will maintain the session for once the
	 * client disconnects. Clients should only connect with a long Session Expiry
	 * interval if they intend to connect to the server at some later point in time.
	 * 
	 * <ul>
	 * <li>By default this value is null and so will not be sent, in this case, the
	 * session will not expire.</li>
	 * <li>If a 0 is sent, the session will end immediately once the Network
	 * Connection is closed.</li>
	 * </ul>
	 * 
	 * When the client has determined that it has no longer any use for the session,
	 * it should disconnect with a Session Expiry Interval set to 0.
	 * 
	 * @param sessionExpiryInterval
	 *            The Session Expiry Interval in seconds.
	 */
	public void setSessionExpiryInterval(Long sessionExpiryInterval) {
		this.sessionExpiryInterval = sessionExpiryInterval;
	}

	/**
	 * Returns the Receive Maximum value. If <code>null</code>, it will default to
	 * 65,535.
	 * 
	 * @return the Receive Maximum
	 */
	public Integer getReceiveMaximum() {
		return receiveMaximum;
	}

	/**
	 * Sets the Receive Maximum. This value represents the limit of QoS 1 and QoS 2
	 * publications that the client is willing to process concurrently. There is no
	 * mechanism to limit the number of QoS 0 publications that the Server might try
	 * to send.
	 * <ul>
	 * <li>If set to <code>null</code> then this value defaults to 65,535.</li>
	 * <li>If set, the minimum value for this property is 1.</li>
	 * <li>The maximum value for this property is 65,535.</li>
	 * </ul>
	 * 
	 * @param receiveMaximum
	 *            the Receive Maximum.
	 */
	public void setReceiveMaximum(Integer receiveMaximum) {
		if (receiveMaximum != null && (receiveMaximum == 0 || receiveMaximum > 65535)) {
			throw new IllegalArgumentException();
		}
		this.receiveMaximum = receiveMaximum;
	}

	/**
	 * Returns the Maximum Packet Size. If <code>null</code>, no limit is imposed.
	 * 
	 * @return the Maximum Packet Size in bytes.
	 */
	public Long getMaximumPacketSize() {
		return maximumPacketSize;
	}

	/**
	 * Sets the Maximum Packet Size. This value represents the Maximum Packet Size
	 * the client is willing to accept.
	 * 
	 * <ul>
	 * <li>If set to <code>null</code> then no limit is imposed beyond the
	 * limitations of the protocol.</li>
	 * <li>If set, the minimum value for this property is 1.</li>
	 * <li>The maximum value for this property is 2,684,354,656.</li>
	 * </ul>
	 * 
	 * @param maximumPacketSize
	 *            The Maximum packet size.
	 */
	public void setMaximumPacketSize(Long maximumPacketSize) {
		this.maximumPacketSize = maximumPacketSize;
	}

	/**
	 * Returns the Topic Alias Maximum. If <code>null</code>, the default value is
	 * 0.
	 * 
	 * @return the Topic Alias Maximum.
	 */
	public Integer getTopicAliasMaximum() {
		return topicAliasMaximum;
	}

	/**
	 * Sets the Topic Alias Maximum. This value if present represents the highest
	 * value that the Client will accept as a Topic Alias sent by the Server.
	 * 
	 * <ul>
	 * <li>If set to <code>null</code>, then it will default to to 0.</li>
	 * <li>If set to 0, the Client will not accept any Topic Aliases</li>
	 * <li>The Maximum value for this property is 65535.</li>
	 * </ul>
	 * 
	 * @param topicAliasMaximum
	 *            the Topic Alias Maximum
	 */
	public void setTopicAliasMaximum(Integer topicAliasMaximum) {
		if (topicAliasMaximum != null && topicAliasMaximum > 65535) {
			throw new IllegalArgumentException();
		}
		this.topicAliasMaximum = topicAliasMaximum;
	}

	/**
	 * Returns the Request Response Info flag. If <code>null</code>, the default
	 * value is false.
	 * 
	 * @return The Request Response Info Flag.
	 */
	public Boolean getRequestResponseInfo() {
		return requestResponseInfo;
	}

	/**
	 * Sets the Request Response Info Flag.
	 * <ul>
	 * <li>If set to <code>null</code>, then it will default to false.</li>
	 * <li>If set to false, the server will not return any response information in
	 * the CONNACK.</li>
	 * <li>If set to true, the server MAY return response information in the
	 * CONNACK.</li>
	 * </ul>
	 * 
	 * @param requestResponseInfo
	 *            The Request Response Info Flag.
	 */
	public void setRequestResponseInfo(boolean requestResponseInfo) {
		this.requestResponseInfo = requestResponseInfo;
	}

	/**
	 * Returns the Request Problem Info flag. If <code>null</code>, the default
	 * value is true.
	 * 
	 * @return the Request Problem Info flag.
	 */
	public Boolean getRequestProblemInfo() {
		return requestProblemInfo;
	}

	/**
	 * Sets the Request Problem Info flag.
	 * <ul>
	 * <li>If set to <code>null</code>, then it will default to true.</li>
	 * <li>If set to false, the server MAY return a Reason String or User Properties
	 * on a CONNACK or DISCONNECT, but must not send a Reason String or User
	 * Properties on any packet other than PUBLISH, CONNACK or DISCONNECT.</li>
	 * <li>If set to true, the server MAY return a Reason String or User Properties
	 * on any packet where it is allowed.</li>
	 * </ul>
	 * 
	 * @param requestProblemInfo
	 *            The Flag to request problem information.
	 */
	public void setRequestProblemInfo(boolean requestProblemInfo) {
		this.requestProblemInfo = requestProblemInfo;
	}

	/**
	 * Returns the User Properties.
	 * 
	 * @return the User Properties.
	 */
	public List<UserProperty> getUserProperties() {
		return userProperties;
	}

	/**
	 * Sets the User Properties. A User Property is a UTF-8 String Pair, the same
	 * name is allowed to appear more than once.
	 * 
	 * @param userProperties
	 *            User Properties
	 */
	public void setUserProperties(List<UserProperty> userProperties) {
		this.userProperties = userProperties;
	}

	/**
	 * Returns the Authentication Method. If <code>null</code>, extended
	 * authentication is not performed.
	 * 
	 * @return the Authentication Method.
	 */
	public String getAuthMethod() {
		return authMethod;
	}

	/**
	 * Sets the Authentication Method. If set, this value contains the name of the
	 * authentication method to be used for extended authentication.
	 * 
	 * If <code>null</code>, extended authentication is not performed.
	 * 
	 * @param authMethod
	 *            The Authentication Method.
	 */
	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	/**
	 * Returns the Authentication Data.
	 * 
	 * @return the Authentication Data.
	 */
	public byte[] getAuthData() {
		return authData;
	}

	/**
	 * Sets the Authentication Data. If set, this byte array contains the extended
	 * authentication data, defined by the Authenticated Method. It is a protocol
	 * error to include Authentication Data if there is no Authentication Method.
	 * 
	 * @param authData
	 *            The Authentication Data
	 */
	public void setAuthData(byte[] authData) {
		this.authData = authData;
	}

	/**
	 * Returns the socket factory that will be used when connecting, or
	 * <code>null</code> if one has not been set.
	 * 
	 * @return The Socket Factory
	 */
	public SocketFactory getSocketFactory() {
		return socketFactory;
	}

	/**
	 * Sets the <code>SocketFactory</code> to use. This allows an application to
	 * apply its own policies around the creation of network sockets. If using an
	 * SSL connection, an <code>SSLSocketFactory</code> can be used to supply
	 * application-specific security settings.
	 * 
	 * @param socketFactory
	 *            the factory to use.
	 */
	public void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	/**
	 * Returns the SSL properties for the connection.
	 * 
	 * @return the properties for the SSL connection
	 */
	public Properties getSSLProperties() {
		return sslClientProps;
	}

	/**
	 * Sets the SSL properties for the connection.
	 * <p>
	 * Note that these properties are only valid if an implementation of the Java
	 * Secure Socket Extensions (JSSE) is available. These properties are
	 * <em>not</em> used if a SocketFactory has been set using
	 * {@link #setSocketFactory(SocketFactory)}. The following properties can be
	 * used:
	 * </p>
	 * <dl>
	 * <dt>com.ibm.ssl.protocol</dt>
	 * <dd>One of: SSL, SSLv3, TLS, TLSv1, SSL_TLS.</dd>
	 * <dt>com.ibm.ssl.contextProvider
	 * <dd>Underlying JSSE provider. For example "IBMJSSE2" or "SunJSSE"</dd>
	 *
	 * <dt>com.ibm.ssl.keyStore</dt>
	 * <dd>The name of the file that contains the KeyStore object that you want the
	 * KeyManager to use. For example /mydir/etc/key.p12</dd>
	 *
	 * <dt>com.ibm.ssl.keyStorePassword</dt>
	 * <dd>The password for the KeyStore object that you want the KeyManager to use.
	 * The password can either be in plain-text, or may be obfuscated using the
	 * static method:
	 * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>. This
	 * obfuscates the password using a simple and insecure XOR and Base64 encoding
	 * mechanism. Note that this is only a simple scrambler to obfuscate clear-text
	 * passwords.</dd>
	 *
	 * <dt>com.ibm.ssl.keyStoreType</dt>
	 * <dd>Type of key store, for example "PKCS12", "JKS", or "JCEKS".</dd>
	 *
	 * <dt>com.ibm.ssl.keyStoreProvider</dt>
	 * <dd>Key store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
	 *
	 * <dt>com.ibm.ssl.trustStore</dt>
	 * <dd>The name of the file that contains the KeyStore object that you want the
	 * TrustManager to use.</dd>
	 *
	 * <dt>com.ibm.ssl.trustStorePassword</dt>
	 * <dd>The password for the TrustStore object that you want the TrustManager to
	 * use. The password can either be in plain-text, or may be obfuscated using the
	 * static method:
	 * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>. This
	 * obfuscates the password using a simple and insecure XOR and Base64 encoding
	 * mechanism. Note that this is only a simple scrambler to obfuscate clear-text
	 * passwords.</dd>
	 *
	 * <dt>com.ibm.ssl.trustStoreType</dt>
	 * <dd>The type of KeyStore object that you want the default TrustManager to
	 * use. Same possible values as "keyStoreType".</dd>
	 *
	 * <dt>com.ibm.ssl.trustStoreProvider</dt>
	 * <dd>Trust store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
	 *
	 * <dt>com.ibm.ssl.enabledCipherSuites</dt>
	 * <dd>A list of which ciphers are enabled. Values are dependent on the
	 * provider, for example:
	 * SSL_RSA_WITH_AES_128_CBC_SHA;SSL_RSA_WITH_3DES_EDE_CBC_SHA.</dd>
	 *
	 * <dt>com.ibm.ssl.keyManager</dt>
	 * <dd>Sets the algorithm that will be used to instantiate a KeyManagerFactory
	 * object instead of using the default algorithm available in the platform.
	 * Example values: "IbmX509" or "IBMJ9X509".</dd>
	 *
	 * <dt>com.ibm.ssl.trustManager</dt>
	 * <dd>Sets the algorithm that will be used to instantiate a TrustManagerFactory
	 * object instead of using the default algorithm available in the platform.
	 * Example values: "PKIX" or "IBMJ9X509".</dd>
	 * </dl>
	 * 
	 * @param props
	 *            The SSL {@link Properties}
	 */
	public void setSSLProperties(Properties props) {
		this.sslClientProps = props;
	}

	/**
	 * Returns the HostnameVerifier for the SSL connection.
	 * 
	 * @return the HostnameVerifier for the SSL connection
	 */
	public HostnameVerifier getSSLHostnameVerifier() {
		return sslHostnameVerifier;
	}

	/**
	 * Sets the HostnameVerifier for the SSL connection. Note that it will be used
	 * after handshake on a connection and you should do actions by yourserlf when
	 * hostname is verified error.
	 * <p>
	 * There is no default HostnameVerifier
	 * </p>
	 * 
	 * @param hostnameVerifier
	 *            the {@link HostnameVerifier}
	 */
	public void setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
		this.sslHostnameVerifier = hostnameVerifier;
	}

	/**
	 * Returns whether to automatically assign subscription identifiers when
	 * subscribing to a topic.
	 * 
	 * @return if automatic assignment of subscription identifiers is enabled.
	 */
	public boolean useSubscriptionIdentifiers() {
		return useSubscriptionIdentifiers;
	}

	/**
	 * Sets whether to automatically assign subscription identifiers when
	 * subscribing to a topic. This will mean that if a subscription has a callback
	 * associated with it then it is guaranteed to be called when an incoming
	 * message has the correct subscription ID embedded. If disabled, then the
	 * client will do best effort topic matching with all callbacks, however this
	 * might result in an incorrect callback being called if there are multiple
	 * subscriptions to topics using a combination of wildcards.
	 * 
	 * @param useSubscriptionIdentifiers
	 *            Whether to enable automatic assignment of subscription
	 *            identifiers.
	 */
	public void setUseSubscriptionIdentifiers(boolean useSubscriptionIdentifiers) {
		this.useSubscriptionIdentifiers = useSubscriptionIdentifiers;
	}

	public boolean isHttpsHostnameVerificationEnabled() {
		return httpsHostnameVerificationEnabled;
	}

	public void setHttpsHostnameVerificationEnabled(boolean httpsHostnameVerificationEnabled) {
		this.httpsHostnameVerificationEnabled = httpsHostnameVerificationEnabled;
	}

	/**
	 * @return The Debug Properties
	 */
	public Properties getDebug() {
		final String strNull = "null";
		Properties p = new Properties();
		p.put("MqttVersion", getMqttVersion());
		p.put("CleanStart", Boolean.valueOf(isCleanStart()));
		p.put("ConTimeout", getConnectionTimeout());
		p.put("KeepAliveInterval", getKeepAliveInterval());
		p.put("UserName", (getUserName() == null) ? strNull : getUserName());
		p.put("WillDestination", (getWillDestination() == null) ? strNull : getWillDestination());
		if (getSocketFactory() == null) {
			p.put("SocketFactory", strNull);
		} else {
			p.put("SocketFactory", getSocketFactory());
		}
		if (getSSLProperties() == null) {
			p.put("SSLProperties", strNull);
		} else {
			p.put("SSLProperties", getSSLProperties());
		}
		return p;
	}

	/**
	 * Sets the Custom WebSocket Headers for the WebSocket Connection.
	 *
	 * @param headers
	 *            The custom websocket headers {@link Properties}
	 */
	public void setCustomWebSocketHeaders(Map<String, String> headers) {
		this.customWebSocketHeaders = Collections.unmodifiableMap(headers);
	}

	public Map<String, String> getCustomWebSocketHeaders() {
		return customWebSocketHeaders;
	}

	public String toString() {
		return null; //Debug.dumpProperties(getDebug(), "Connection options");
	}

	public boolean isSendReasonMessages() {
		return sendReasonMessages;
	}

	public void setSendReasonMessages(boolean sendReasonMessages) {
		this.sendReasonMessages = sendReasonMessages;
	}

	public int getExecutorServiceTimeout() {
		return executorServiceTimeout;
	}

	/**
	 * Set the time in seconds that the executor service should wait when
	 * terminating before forcefully terminating. It is not recommended to change
	 * this value unless you are absolutely sure that you need to.
	 * 
	 * @param executorServiceTimeout the time in seconds to wait when shutting down.Ï
	 */
	public void setExecutorServiceTimeout(int executorServiceTimeout) {
		this.executorServiceTimeout = executorServiceTimeout;
	}
}
