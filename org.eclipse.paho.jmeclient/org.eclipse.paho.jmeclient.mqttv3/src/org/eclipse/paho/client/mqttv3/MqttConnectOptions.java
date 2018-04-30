/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 */
package org.eclipse.paho.client.mqttv3;

import java.util.Hashtable;

import org.eclipse.paho.client.mqttv3.util.Debug;

/**
 * Holds options that control how the client connects to a server.
 */
public class MqttConnectOptions {
	/**
	 * The default keep alive interval in seconds if one is not specified
	 */
	public static final int KEEP_ALIVE_INTERVAL_DEFAULT = 60;
	/**
	 * The default connection timeout in seconds if one is not specified
	 */
	public static final int CONNECTION_TIMEOUT_DEFAULT = 30;
	/**
	 * The default clean session setting if one is not specified
	 */
	public static final boolean CLEAN_SESSION_DEFAULT = true;
	
	protected static final int URI_TYPE_TCP = 0;
	protected static final int URI_TYPE_SSL = 1;
	protected static final int URI_TYPE_LOCAL = 2;
  
	private int keepAliveInterval = KEEP_ALIVE_INTERVAL_DEFAULT;
	private String willDestination = null;
	private MqttMessage willMessage = null;
	private String userName;
	private char[] password;
	//private MqttSocketFactory socketFactory; 
	private Hashtable sslClientProps = null; 
	private boolean cleanSession = CLEAN_SESSION_DEFAULT;
	private int connectionTimeout = CONNECTION_TIMEOUT_DEFAULT;
	private String[] serverURIs;

	/**
	 * Constructs a new <code>MqttConnectOptions</code> object using the 
	 * default values.
	 * 
	 * The defaults are:
	 * <ul>
	 * <li>The keepalive interval is 60 seconds</li>
	 * <li>Clean Session is true</li>
	 * <li>The message delivery retry interval is 15 seconds</li>
	 * <li>The connection timeout period is 30 seconds</li> 
	 * <li>No Will message is set</li>
	 * <li>A standard SocketFactory is used</li>
	 * </ul>
	 * More information about these values can be found in the setter methods. 
	 */
	public MqttConnectOptions() {
	}
	
	/**
	 * Returns the password to use for the connection.
	 * @return the password to use for the connection.
	 */
	public char[] getPassword() {
		return password;
	}

	/**
	 * Sets the password to use for the connection.
	 */
	public void setPassword(char[] password) {
		this.password = password;
	}

	/**
	 * Returns the user name to use for the connection.
	 * @return the user name to use for the connection.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Sets the user name to use for the connection.
	 * @throws IllegalArgumentException if the user name is blank or only
	 * contains whitespace characters.
	 */
	public void setUserName(String userName) {
		if ((userName != null) && (userName.trim().equals(""))) {
			throw new IllegalArgumentException();
		}
		this.userName = userName;
	}
	
	/**
	 * Sets the "Last Will and Testament" (LWT) for the connection.
	 * In the event that this client unexpectedly loses its connection to the 
	 * server, the server will publish a message to itself using the supplied
	 * details.
	 * 
	 * @param topic the topic to publish to.
	 * @param payload the byte payload for the message.
	 * @param qos the quality of service to publish the message at (0, 1 or 2).
	 * @param retained whether or not the message should be retained.
	 */
	public void setWill(MqttTopic topic, byte[] payload, int qos, boolean retained) {
		String topicS = topic.getName();
		validateWill(topicS, payload);
		this.setWill(topicS, new MqttMessage(payload), qos, retained);
	}
	
	/**
	 * Sets the "Last Will and Testament" (LWT) for the connection.
	 * In the event that this client unexpectedly loses its connection to the 
	 * server, the server will publish a message to itself using the supplied
	 * details.
	 * 
	 * @param topic the topic to publish to.
	 * @param payload the byte payload for the message.
	 * @param qos the quality of service to publish the message at (0, 1 or 2).
	 * @param retained whether or not the message should be retained.
	 */
	public void setWill(String topic, byte[] payload, int qos, boolean retained) {
		validateWill(topic, payload);
		this.setWill(topic, new MqttMessage(payload), qos, retained);
	}
	
	
	/**
	 * Validates the will fields.
	 */
	private void validateWill(String dest, Object payload) {
		if ((dest == null) || (payload == null)) {
			throw new IllegalArgumentException();
		}
		MqttAsyncClient.validateTopic(dest);
	}

	/**
	 * Sets up the will information, based on the supplied parameters.
	 */
	protected void setWill(String topic, MqttMessage msg, int qos, boolean retained) {
		willDestination = topic;
		willMessage = msg;
		willMessage.setQos(qos);
		willMessage.setRetained(retained);
		// Prevent any more changes to the will message
		willMessage.setMutable(false);
	}
	
	/**
	 * Returns the "keep alive" interval.
	 * @see #setKeepAliveInterval(int)
	 * @return the keep alive interval.
	 */
	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	/**
	 * Sets the "keep alive" interval.
	 * This value, measured in seconds, defines the maximum time interval 
	 * between messages sent or received. It enables the client to 
	 * detect if the server is no longer available, without 
	 * having to wait for the TCP/IP timeout. The client will ensure
	 * that at least one message travels across the network within each
	 * keep alive period.  In the absence of a data-related message during 
	 * the time period, the client sends a very small "ping" message, which
	 * the server will acknowledge.
	 * A value of 0 disables keepalive processing in the client. 
	 * <p>The default value is 60 seconds</p>
	 * 
	 * @param keepAliveInterval the interval, measured in seconds, must be >= 0.
	 */
	public void setKeepAliveInterval(int keepAliveInterval)throws IllegalArgumentException {
		if (keepAliveInterval <0 ) {
			throw new IllegalArgumentException();
		}
		this.keepAliveInterval = keepAliveInterval;
	}
	
	/**
	 * Returns the connection timeout value.
	 * @see #setConnectionTimeout(int)
	 * @return the connection timeout value.
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	
	/**
	 * Sets the connection timeout value.
	 * This value, measured in seconds, defines the maximum time interval
	 * the client will wait for the network connection to the MQTT server to be established. 
	 * The default timeout is 30 seconds.
	 * A value of 0 disables timeout processing meaning the client will wait until the
	 * network connection is made successfully or fails. 
	 * @param connectionTimeout the timeout value, measured in seconds. It must be >0;
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		if (connectionTimeout <0 ) {
			throw new IllegalArgumentException();
		}
		this.connectionTimeout = connectionTimeout;
	}
	
	/**
	 * Returns the socket factory that will be used when connecting, or
	 * <code>null</code> if one has not been set.
	 */
	//public MqttSocketFactory getSocketFactory() {
	//	return socketFactory;
	//}
	
	/**
	 * Sets the <code>SocketFactory</code> to use.  This allows an application
	 * to apply its own policies around the creation of network sockets.  If
	 * using an SSL connection, an <code>SSLSocketFactory</code> can be used
	 * to supply application-specific security settings.
	 * @param socketFactory the factory to use.
	 */
	//public void setSocketFactory(MqttSocketFactory socketFactory) {
	//	this.socketFactory = socketFactory;
	//}

	/**
	 * Returns the topic to be used for last will and testament (LWT).
	 * @return the MqttTopic to use, or <code>null</code> if LWT is not set.
	 * @see #setWill(MqttTopic, byte[], int, boolean)
	 */
	public String getWillDestination() {
		return willDestination;
	}

	/**
	 * Returns the message to be sent as last will and testament (LWT).
	 * The returned object is "read only".  Calling any "setter" methods on 
	 * the returned object will result in an 
	 * <code>IllegalStateException</code> being thrown.
	 * @return the message to use, or <code>null</code> if LWT is not set.
	 */
	public MqttMessage getWillMessage() {
		return willMessage;
	}
	
	/**
	 * Returns the SSL properties for the connection.
	 * @return the properties for the SSL connection
	 */
	public Hashtable getSSLProperties() {
		return sslClientProps;
	}

	/**
	 * Sets the SSL properties for the connection.  Note that these
	 * properties are only valid if an implementation of the Java
	 * Secure Socket Extensions (JSSE) is available.  These properties are
	 * <em>not</em> used if a SocketFactory has been set using
	 * {@link #setSocketFactory(SocketFactory)}.
	 * The following properties can be used:</p>
	 * <dl>
	 * <dt>com.ibm.ssl.protocol</dt>
   	 * <dd>One of: SSL, SSLv3, TLS, TLSv1, SSL_TLS.</dd>
	 * <dt>com.ibm.ssl.contextProvider
   	 * <dd>Underlying JSSE provider.  For example "IBMJSSE2" or "SunJSSE"</dd>
	 * 
	 * <dt>com.ibm.ssl.keyStore</dt>
   	 * <dd>The name of the file that contains the KeyStore object that you 
   	 * want the KeyManager to use. For example /mydir/etc/key.p12</dd>
	 * 
	 * <dt>com.ibm.ssl.keyStorePassword</dt>
   	 * <dd>The password for the KeyStore object that you want the KeyManager to use.  
   	 * The password can either be in plain-text, 
   	 * or may be obfuscated using the static method:
     * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>.
   	 * This obfuscates the password using a simple and insecure XOR and Base64 
   	 * encoding mechanism. Note that this is only a simple scrambler to 
   	 * obfuscate clear-text passwords.</dd>
	 * 
	 * <dt>com.ibm.ssl.keyStoreType</dt>
   	 * <dd>Type of key store, for example "PKCS12", "JKS", or "JCEKS".</dd>
	 * 
	 * <dt>com.ibm.ssl.keyStoreProvider</dt>
   	 * <dd>Key store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
	 * 
	 * <dt>com.ibm.ssl.trustStore</dt>
   	 * <dd>The name of the file that contains the KeyStore object that you 
   	 * want the TrustManager to use.</dd> 
	 * 
	 * <dt>com.ibm.ssl.trustStorePassword</dt>
   	 * <dd>The password for the TrustStore object that you want the 
   	 * TrustManager to use.  The password can either be in plain-text, 
   	 * or may be obfuscated using the static method:
     * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>.
   	 * This obfuscates the password using a simple and insecure XOR and Base64 
   	 * encoding mechanism. Note that this is only a simple scrambler to 
   	 * obfuscate clear-text passwords.</dd>
	 * 
	 * <dt>com.ibm.ssl.trustStoreType</dt>
   	 * <dd>The type of KeyStore object that you want the default TrustManager to use.  
   	 * Same possible values as "keyStoreType".</dd>
	 * 
	 * <dt>com.ibm.ssl.trustStoreProvider</dt>
   	 * <dd>Trust store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
	 * 
	 * <dt>com.ibm.ssl.enabledCipherSuites</dt>
	 * <dd>A list of which ciphers are enabled.  Values are dependent on the provider, 
	 * for example: SSL_RSA_WITH_AES_128_CBC_SHA;SSL_RSA_WITH_3DES_EDE_CBC_SHA.</dd>
	 * 
	 * <dt>com.ibm.ssl.keyManager</dt>
	 * <dd>Sets the algorithm that will be used to instantiate a KeyManagerFactory object 
	 * instead of using the default algorithm available in the platform. Example values: 
	 * "IbmX509" or "IBMJ9X509".
	 * </dd>
	 * 
	 * <dt>com.ibm.ssl.trustManager</dt>
	 * <dd>Sets the algorithm that will be used to instantiate a TrustManagerFactory object 
	 * instead of using the default algorithm available in the platform. Example values: 
	 * "PKIX" or "IBMJ9X509".
	 * </dd>
	 * </dl>
	 */
	public void setSSLProperties(Hashtable props) {
		this.sslClientProps = props;
	}
	
	/**
	 * Returns whether the server should remember state for the client across reconnects.
	 * @return the clean session flag
	 */
	public boolean isCleanSession() {
		return this.cleanSession;
	}
	
	/**
	 * Sets whether the server should remember state for the client across reconnects.
	 * This includes subscriptions and the state of any in-flight messages.
 	 */
	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	/**
	 * @return the serverURIs
	 */
	public String[] getServerURIs() {
		return serverURIs;
	}

	/**
	 * @param array of serverURIs
	 */
	public void setServerURIs(String[] array) {
		for (int i = 0; i < array.length; i++) {
			validateURI(array[i]);
		}
		this.serverURIs = array;
	}

	/**
	 * Validate a URI 
	 * @param srvURI
	 * @return the URI type
	 */
	protected static int validateURI(String srvURI) {
		if (srvURI.startsWith("tcp://")) {
			return URI_TYPE_TCP;
		}
		else if (srvURI.startsWith("ssl://")) {
			return URI_TYPE_SSL;
		}
		else if (srvURI.startsWith("local://")) {
			return URI_TYPE_LOCAL;
		}
		else {
			throw new IllegalArgumentException(srvURI);
		}
	}
	
	public Hashtable getDebug() {
		Hashtable hash = new Hashtable();
		hash.put("CleanSession", new Boolean(isCleanSession()));
		hash.put("ConTimeout", new Integer(getConnectionTimeout()));
		hash.put("KeepAliveInterval", new Integer(getKeepAliveInterval()));
		hash.put("UserName", (getUserName()==null)?"null":getUserName());
		hash.put("WillDestination", (getWillDestination()==null)?"null":getWillDestination());
		//if (getSocketFactory()==null) {
		//	hash.put("SocketFactory", "null");	
		//} else {
		//	hash.put("SocketFactory", getSocketFactory());
		//}
		if (getSSLProperties()==null) {
			hash.put("SSLProperties", "null");
		} else {
			hash.put("SSLProperties", getSSLProperties());
		}
		return hash;
	}
	
	public String toString() {
		return Debug.dumpProperties(getDebug(), "Connection options");
	}
}
