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

/**
 * Enables an application to communicate with an MQTT server using blocking methods.
 * <p>
 * This interface allows applications to utilize all features of the MQTT version 3.1
 * specification including:</p>
 * <ul>
 * <li>connect
 * <li>publish
 * <li>subscribe
 * <li>unsubscribe
 * <li>disconnect
 * </ul>
 * <p>
 * There are two styles of MQTT client, this one and {@link IMqttAsyncClient}.</p>
 * <ul>
 * <li>IMqttClient provides a set of methods that block and return control to the application
 * program once the MQTT action has completed.</li>
 * <li>IMqttAsyncClient provides a set of non-blocking methods that return control to the
 * invoking application after initial validation of parameters and state. The main processing is
 * performed in the background so as not to block the application programs thread. This non
 * blocking approach is handy when the application wants to carry on processing while the
 * MQTT action takes place. For instance connecting to an MQTT server can take time, using
 * the non-blocking connect method allows an application to display a busy indicator while the
 * connect action is occurring. Non-blocking methods are particularly useful in event-oriented
 * programs and graphical programs where issuing methods that take time to complete on the the
 * main or GUI thread can cause problems.</li>
 * </ul>
 * <p>
 * The non-blocking client can also be used in a blocking form by turning a non-blocking
 * method into a blocking invocation using the following pattern:</p>
 *     <pre>
 *     IMqttToken token;
 *     token = asyncClient.method(parms).waitForCompletion();
 *     </pre>
 * <p>
 * Using the non-blocking client allows an application to use a mixture of blocking and
 * non-blocking styles. Using the blocking client only allows an application to use one
 * style. The blocking client provides compatibility with earlier versions
 * of the MQTT client.</p>
 */
public interface IMqttClient { //extends IMqttAsyncClient {
	/**
	 * Connects to an MQTT server using the default options.
	 * <p>The default options are specified in {@link MqttConnectOptions} class.
	 * </p>
	 *
	 * @throws MqttSecurityException when the server rejects the connect for security
	 * reasons
	 * @throws MqttException  for non security related problems
	 * @see #connect(MqttConnectOptions)
	 */
  public void connect() throws MqttSecurityException, MqttException;

	/**
	 * Connects to an MQTT server using the specified options.
	 * <p>The server to connect to is specified on the constructor.
	 * It is recommended to call {@link #setCallback(MqttCallback)} prior to
	 * connecting in order that messages destined for the client can be accepted
	 * as soon as the client is connected.
	 * </p>
	 * <p>This is a blocking method that returns once connect completes</p>
	 *
	 * @param options a set of connection parameters that override the defaults.
	 * @throws MqttSecurityException when the server rejects the connect for security
	 * reasons
	 * @throws MqttException  for non security related problems including communication errors
	 */
  public void connect(MqttConnectOptions options) throws MqttSecurityException, MqttException;
  
	/**
	 * Connects to an MQTT server using the specified options.
	 * <p>The server to connect to is specified on the constructor.
	 * It is recommended to call {@link #setCallback(MqttCallback)} prior to
	 * connecting in order that messages destined for the client can be accepted
	 * as soon as the client is connected.
	 * </p>
	 * <p>This is a blocking method that returns once connect completes</p>
	 *
	 * @param options a set of connection parameters that override the defaults.
	 * @return the MqttToken used for the call.  Can be used to obtain the session present flag
	 * @throws MqttSecurityException when the server rejects the connect for security
	 * reasons
	 * @throws MqttException  for non security related problems including communication errors
	 */
public IMqttToken connectWithResult(MqttConnectOptions options) throws MqttSecurityException, MqttException;

	/**
	 * Disconnects from the server.
	 * <p>An attempt is made to quiesce the client allowing outstanding
	 * work to complete before disconnecting. It will wait
	 * for a maximum of 30 seconds for work to quiesce before disconnecting.
	 * This method must not be called from inside {@link MqttCallback} methods.
	 * </p>
	 *
	 * <p>This is a blocking method that returns once disconnect completes</p>
	 *
	 * @throws MqttException if a problem is encountered while disconnecting
	 */
  public void disconnect() throws MqttException;

	/**
	 * Disconnects from the server.
	 * <p>
	 * The client will wait for all {@link MqttCallback} methods to
	 * complete. It will then wait for up to the quiesce timeout to allow for
	 * work which has already been initiated to complete - for example, it will
	 * wait for the QoS 2 flows from earlier publications to complete. When work has
	 * completed or after the quiesce timeout, the client will disconnect from
	 * the server. If the cleanSession flag was set to false and is set to false the
	 * next time a connection is made QoS 1 and 2 messages that
	 * were not previously delivered will be delivered.</p>
	 *
	 * <p>This is a blocking method that returns once disconnect completes</p>
	 *
	 * @param quiesceTimeout the amount of time in milliseconds to allow for
	 * existing work to finish before disconnecting.  A value of zero or less
	 * means the client will not quiesce.
	 * @throws MqttException if a problem is encountered while disconnecting
	 */
  public void disconnect(long quiesceTimeout) throws MqttException;
  
  /**
	 * Disconnects from the server forcibly to reset all the states. Could be useful when disconnect attempt failed.
	 * <p>
	 * Because the client is able to establish the TCP/IP connection to a none MQTT server and it will certainly fail to
	 * send the disconnect packet. It will wait for a maximum of 30 seconds for work to quiesce before disconnecting and
	 * wait for a maximum of 10 seconds for sending the disconnect packet to server.
	 * 
	 * @throws MqttException if any unexpected error
	 * @since 0.4.1
	 */
	public void disconnectForcibly() throws MqttException;
	
	/**
	 * Disconnects from the server forcibly to reset all the states. Could be useful when disconnect attempt failed.
	 * <p>
	 * Because the client is able to establish the TCP/IP connection to a none MQTT server and it will certainly fail to
	 * send the disconnect packet. It will wait for a maximum of 30 seconds for work to quiesce before disconnecting.
	 * 
	 * @param disconnectTimeout the amount of time in milliseconds to allow send disconnect packet to server.
	 * @throws MqttException if any unexpected error
	 * @since 0.4.1
	 */
	public void disconnectForcibly(long disconnectTimeout) throws MqttException;
	
	/**
	 * Disconnects from the server forcibly to reset all the states. Could be useful when disconnect attempt failed.
	 * <p>
	 * Because the client is able to establish the TCP/IP connection to a none MQTT server and it will certainly fail to
	 * send the disconnect packet.
	 * 
	 * @param quiesceTimeout the amount of time in milliseconds to allow for existing work to finish before
	 * disconnecting. A value of zero or less means the client will not quiesce.
	 * @param disconnectTimeout the amount of time in milliseconds to allow send disconnect packet to server.
	 * @throws MqttException if any unexpected error
	 * @since 0.4.1
	 */
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards using a QoS of 1.
	 *
	 * @see #subscribe(String[], int[])
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @throws MqttException if there was an error registering the subscription.
	 * @throws MqttSecurityException if the client is not authorized to register the subscription
	 */
  public void subscribe(String topicFilter) throws MqttException, MqttSecurityException;

	/**
	 * Subscribes to a one or more topics, which may include wildcards using a QoS of 1.
	 *
	 * @see #subscribe(String[], int[])
	 *
	 * @param topicFilters the topic to subscribe to, which can include wildcards.
	 * @throws MqttException if there was an error registering the subscription.
	 */
  public void subscribe(String[] topicFilters) throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(String[], int[])
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @throws MqttException if there was an error registering the subscription.
	 */
  public void subscribe(String topicFilter, int qos) throws MqttException;

	/**
	 * Subscribes to multiple topics, each of which may include wildcards.
	 * <p>The {@link #setCallback(MqttCallback)} method
	 * should be called before this method, otherwise any received messages
	 * will be discarded.
	 * </p>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true
	 * when when connecting to the server then the subscription remains in place
	 * until either:
	 * </p>
	 * <ul>
	 * <li>The client disconnects</li>
	 * <li>An unsubscribe method is called to un-subscribe the topic</li>
	 * </ul>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false
	 * when when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * <ul>
	 * <li>An unsubscribe method is called to unsubscribe the topic</li>
	 * <li>The client connects with cleanSession set to true</li>
	 * </ul>
	 * <p>
	 * With cleanSession set to false the MQTT server will store messages on
	 * behalf of the client when the client is not connected. The next time the
	 * client connects with the <b>same client ID</b> the server will
	 * deliver the stored messages to the client.
	 * </p>
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
	 * 	any number of levels within a topic. For example, if you subscribe to
	 *  <span><span class="filepath">finance/stock/ibm/#</span></span>, you receive
	 * 	messages on these topics:</p>
	 * <ul>
	 * <li><pre>finance/stock/ibm</pre></li>
	 * <li><pre>finance/stock/ibm/closingprice</pre></li>
	 * <li><pre>finance/stock/ibm/currentprice</pre></li>
	 * </ul>
	 *  
	 *  <p>The multi-level wildcard
	 *  can represent zero or more levels. Therefore, <em>finance/#</em> can also match
	 * 	the singular <em>finance</em>, where <em>#</em> represents zero levels. The topic
	 * 	level separator is meaningless in this context, because there are no levels
	 * 	to separate.</p>
	 *
	 * 	<p>The <span>multi-level</span> wildcard can
	 * 	be specified only on its own or next to the topic level separator character.
	 * 	Therefore, <em>#</em> and <em>finance/#</em> are both valid, but <em>finance#</em> is
	 * 	not valid. <span>The multi-level wildcard must be the last character
	 *  used within the topic tree. For example, <em>finance/#</em> is valid but
	 *  <em>finance/#/closingprice</em> is 	not valid.</span></p></dd>
	 *
	 * 	<dt>Single-level wildcard</dt>
	 * 	<dd><p>The plus sign (+) is a wildcard character that matches only one topic
	 * 	level. For example, <em>finance/stock/+</em> matches
	 * <em>finance/stock/ibm</em> and <em>finance/stock/xyz</em>,
	 * 	but not <em>finance/stock/ibm/closingprice</em>. Also, because the single-level
	 * 	wildcard matches only a single level, <em>finance/+</em> does not match <em>finance</em>.</p>
	 *
	 * 	<p>Use
	 * 	the single-level wildcard at any level in the topic tree, and in conjunction
	 * 	with the multilevel wildcard. Specify the single-level wildcard next to the
	 * 	topic level separator, except when it is specified on its own. Therefore,
	 *  <em>+</em> and <em>finance/+</em> are both valid, but <em>finance+</em> is
	 *  not valid. <span>The single-level wildcard can be used at the end of the
	 *  topic tree or within the topic tree.
	 * 	For example, <em>finance/+</em> and <em>finance/+/ibm</em> are both valid.</span></p>
	 * 	</dd>
	 * </dl>
	 *
	 * <p>This is a blocking method that returns once subscribe completes</p>
	 *
	 * @param topicFilters one or more topics to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service to subscribe each topic at.Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @throws MqttException if there was an error registering the subscription.
	 * @throws IllegalArgumentException if the two supplied arrays are not the same size.
	 */
  public void subscribe(String[] topicFilters, int[] qos) throws MqttException;
  
	/**
	 * Subscribe to a topic, which may include wildcards using a QoS of 1.
	 *
	 * @see #subscribe(String[], int[])
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param messageListener a callback to handle incoming messages
	 * @throws MqttException if there was an error registering the subscription.
	 * @throws MqttSecurityException if the client is not authorized to register the subscription
	 */
public void subscribe(String topicFilter, IMqttMessageListener messageListener) throws MqttException, MqttSecurityException;

	/**
	 * Subscribes to a one or more topics, which may include wildcards using a QoS of 1.
	 *
	 * @see #subscribe(String[], int[])
	 *
	 * @param topicFilters the topic to subscribe to, which can include wildcards.
	 * @param messageListeners one or more callbacks to handle incoming messages
	 * @throws MqttException if there was an error registering the subscription.
	 */
public void subscribe(String[] topicFilters, IMqttMessageListener[] messageListeners) throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(String[], int[])
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param messageListener a callback to handle incoming messages
	 * @throws MqttException if there was an error registering the subscription.
	 */
public void subscribe(String topicFilter, int qos, IMqttMessageListener messageListener) throws MqttException;

	/**
	 * Subscribes to multiple topics, each of which may include wildcards.
	 * <p>The {@link #setCallback(MqttCallback)} method
	 * should be called before this method, otherwise any received messages
	 * will be discarded.
	 * </p>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true
	 * when when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * <ul>
	 * <li>The client disconnects</li>
	 * <li>An unsubscribe method is called to un-subscribe the topic</li>
	 * </ul>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false
	 * when when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * <ul>
	 * <li>An unsubscribe method is called to unsubscribe the topic</li>
	 * <li>The client connects with cleanSession set to true</li>
	 * </ul>
	 * <p>
	 * With cleanSession set to false the MQTT server will store messages on
	 * behalf of the client when the client is not connected. The next time the
	 * client connects with the <b>same client ID</b> the server will
	 * deliver the stored messages to the client.
	 * </p>
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
	 * 	any number of levels within a topic. For example, if you subscribe to
	 *  <span><span class="filepath">finance/stock/ibm/#</span></span>, you receive
	 * 	messages on these topics:</p>
	 * <ul>
	 * <li><pre>finance/stock/ibm</pre></li>
	 * <li><pre>finance/stock/ibm/closingprice</pre></li>
	 * <li><pre>finance/stock/ibm/currentprice</pre></li>
	 * </ul>
	 *  <p>The multi-level wildcard
	 *  can represent zero or more levels. Therefore, <em>finance/#</em> can also match
	 * 	the singular <em>finance</em>, where <em>#</em> represents zero levels. The topic
	 * 	level separator is meaningless in this context, because there are no levels
	 * 	to separate.</p>
	 *
	 * 	<p>The <span>multi-level</span> wildcard can
	 * 	be specified only on its own or next to the topic level separator character.
	 * 	Therefore, <em>#</em> and <em>finance/#</em> are both valid, but <em>finance#</em> is
	 * 	not valid. <span>The multi-level wildcard must be the last character
	 *  used within the topic tree. For example, <em>finance/#</em> is valid but
	 *  <em>finance/#/closingprice</em> is 	not valid.</span></p></dd>
	 *
	 * 	<dt>Single-level wildcard</dt>
	 * 	<dd><p>The plus sign (+) is a wildcard character that matches only one topic
	 * 	level. For example, <em>finance/stock/+</em> matches
	 * <em>finance/stock/ibm</em> and <em>finance/stock/xyz</em>,
	 * 	but not <em>finance/stock/ibm/closingprice</em>. Also, because the single-level
	 * 	wildcard matches only a single level, <em>finance/+</em> does not match <em>finance</em>.</p>
	 *
	 * 	<p>Use
	 * 	the single-level wildcard at any level in the topic tree, and in conjunction
	 * 	with the multilevel wildcard. Specify the single-level wildcard next to the
	 * 	topic level separator, except when it is specified on its own. Therefore,
	 *  <em>+</em> and <em>finance/+</em> are both valid, but <em>finance+</em> is
	 *  not valid. <span>The single-level wildcard can be used at the end of the
	 *  topic tree or within the topic tree.
	 * 	For example, <em>finance/+</em> and <em>finance/+/ibm</em> are both valid.</span></p>
	 * 	</dd>
	 * </dl>
	 *
	 * <p>This is a blocking method that returns once subscribe completes</p>
	 *
	 * @param topicFilters one or more topics to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service to subscribe each topic at.Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param messageListeners one or more callbacks to handle incoming messages
	 * @throws MqttException if there was an error registering the subscription.
	 * @throws IllegalArgumentException if the two supplied arrays are not the same size.
	 */
	public void subscribe(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards using a QoS of 1.
	 *
	 * @see #subscribeWithResponse(String[], int[])
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @return token used to track the subscribe after it has completed.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttToken subscribeWithResponse(String topicFilter) throws MqttException;
	
	/**
	 * Subscribe to a topic, which may include wildcards using a QoS of 1.
	 *
	 * @see #subscribeWithResponse(String[], int[])
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param messageListener a callback to handle incoming messages
	 * @return token used to track the subscribe after it has completed.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttToken subscribeWithResponse(String topicFilter, IMqttMessageListener messageListener) throws MqttException;

	
	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribeWithResponse(String[], int[])
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @return token used to track the subscribe after it has completed.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttToken subscribeWithResponse(String topicFilter, int qos) throws MqttException;
	
	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribeWithResponse(String[], int[])
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param messageListener a callback to handle incoming messages
	 * @return token used to track the subscribe after it has completed.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttToken subscribeWithResponse(String topicFilter, int qos, IMqttMessageListener messageListener) throws MqttException;
	
	/**
	 * Subscribes to a one or more topics, which may include wildcards using a QoS of 1.
	 *
	 * @see #subscribeWithResponse(String[], int[])
	 *
	 * @param topicFilters the topic to subscribe to, which can include wildcards.
	 * @return token used to track the subscribe after it has completed.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttToken subscribeWithResponse(String[] topicFilters) throws MqttException;
	
	/**
	 * Subscribes to a one or more topics, which may include wildcards using a QoS of 1.
	 *
	 * @see #subscribeWithResponse(String[], int[])
	 *
	 * @param topicFilters the topic to subscribe to, which can include wildcards.
	 * @param messageListeners one or more callbacks to handle incoming messages
	 * @return token used to track the subscribe after it has completed.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttToken subscribeWithResponse(String[] topicFilters, IMqttMessageListener[] messageListeners) throws MqttException;
	
	/**
	 * Subscribes to multiple topics, each of which may include wildcards.
	 * <p>The {@link #setCallback(MqttCallback)} method
	 * should be called before this method, otherwise any received messages
	 * will be discarded.
	 * </p>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true
	 * when when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * <ul>
	 * <li>The client disconnects</li>
	 * <li>An unsubscribe method is called to un-subscribe the topic</li>
	 * </ul>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false
	 * when when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * <ul>
	 * <li>An unsubscribe method is called to unsubscribe the topic</li>
	 * <li>The client connects with cleanSession set to true</li>
	 * </ul>
	 * <p>
	 * With cleanSession set to false the MQTT server will store messages on
	 * behalf of the client when the client is not connected. The next time the
	 * client connects with the <b>same client ID</b> the server will
	 * deliver the stored messages to the client.
	 * </p>
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
	 * 	any number of levels within a topic. For example, if you subscribe to
	 *  <span><span class="filepath">finance/stock/ibm/#</span></span>, you receive
	 * 	messages on these topics:</p>
	 * <ul>
	 * <li><pre>finance/stock/ibm</pre></li>
	 * <li><pre>finance/stock/ibm/closingprice</pre></li>
	 * <li><pre>finance/stock/ibm/currentprice</pre></li>
	 * </ul>
	 *  <p>The multi-level wildcard
	 *  can represent zero or more levels. Therefore, <em>finance/#</em> can also match
	 * 	the singular <em>finance</em>, where <em>#</em> represents zero levels. The topic
	 * 	level separator is meaningless in this context, because there are no levels
	 * 	to separate.</p>
	 *
	 * 	<p>The <span>multi-level</span> wildcard can
	 * 	be specified only on its own or next to the topic level separator character.
	 * 	Therefore, <em>#</em> and <em>finance/#</em> are both valid, but <em>finance#</em> is
	 * 	not valid. <span>The multi-level wildcard must be the last character
	 *  used within the topic tree. For example, <em>finance/#</em> is valid but
	 *  <em>finance/#/closingprice</em> is 	not valid.</span></p></dd>
	 *
	 * 	<dt>Single-level wildcard</dt>
	 * 	<dd><p>The plus sign (+) is a wildcard character that matches only one topic
	 * 	level. For example, <em>finance/stock/+</em> matches
	 * <em>finance/stock/ibm</em> and <em>finance/stock/xyz</em>,
	 * 	but not <em>finance/stock/ibm/closingprice</em>. Also, because the single-level
	 * 	wildcard matches only a single level, <em>finance/+</em> does not match <em>finance</em>.</p>
	 *
	 * 	<p>Use
	 * 	the single-level wildcard at any level in the topic tree, and in conjunction
	 * 	with the multilevel wildcard. Specify the single-level wildcard next to the
	 * 	topic level separator, except when it is specified on its own. Therefore,
	 *  <em>+</em> and <em>finance/+</em> are both valid, but <em>finance+</em> is
	 *  not valid. <span>The single-level wildcard can be used at the end of the
	 *  topic tree or within the topic tree.
	 * 	For example, <em>finance/+</em> and <em>finance/+/ibm</em> are both valid.</span></p>
	 * 	</dd>
	 * </dl>
	 *
	 * <p>This is a blocking method that returns once subscribe completes</p>
	 *
	 * @param topicFilters one or more topics to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service to subscribe each topic at.Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @throws MqttException if there was an error registering the subscription.
	 * @return token used to track the subscribe after it has completed.
	 * @throws IllegalArgumentException if the two supplied arrays are not the same size.
	 */
	public IMqttToken subscribeWithResponse(String[] topicFilters, int[] qos) throws MqttException;

	/**
	 * Subscribes to multiple topics, each of which may include wildcards.
	 * <p>The {@link #setCallback(MqttCallback)} method
	 * should be called before this method, otherwise any received messages
	 * will be discarded.
	 * </p>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true
	 * when when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * <ul>
	 * <li>The client disconnects</li>
	 * <li>An unsubscribe method is called to un-subscribe the topic</li>
	 * </ul>
	 * 
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false
	 * when when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * <ul>
	 * <li>An unsubscribe method is called to unsubscribe the topic</li>
	 * <li>The client connects with cleanSession set to true</li>
	 * </ul>
	 * <p>
	 * With cleanSession set to false the MQTT server will store messages on
	 * behalf of the client when the client is not connected. The next time the
	 * client connects with the <b>same client ID</b> the server will
	 * deliver the stored messages to the client.
	 * </p>
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
	 * 	any number of levels within a topic. For example, if you subscribe to
	 *  <span><span class="filepath">finance/stock/ibm/#</span></span>, you receive
	 * 	messages on these topics:</p>
	 * <ul>
	 * <li><pre>finance/stock/ibm</pre></li>
	 * <li><pre>finance/stock/ibm/closingprice</pre></li>
	 * <li><pre>finance/stock/ibm/currentprice</pre></li>
	 * </ul>
	 *  <p>The multi-level wildcard
	 *  can represent zero or more levels. Therefore, <em>finance/#</em> can also match
	 * 	the singular <em>finance</em>, where <em>#</em> represents zero levels. The topic
	 * 	level separator is meaningless in this context, because there are no levels
	 * 	to separate.</p>
	 *
	 * 	<p>The <span>multi-level</span> wildcard can
	 * 	be specified only on its own or next to the topic level separator character.
	 * 	Therefore, <em>#</em> and <em>finance/#</em> are both valid, but <em>finance#</em> is
	 * 	not valid. <span>The multi-level wildcard must be the last character
	 *  used within the topic tree. For example, <em>finance/#</em> is valid but
	 *  <em>finance/#/closingprice</em> is 	not valid.</span></p></dd>
	 *
	 * 	<dt>Single-level wildcard</dt>
	 * 	<dd><p>The plus sign (+) is a wildcard character that matches only one topic
	 * 	level. For example, <em>finance/stock/+</em> matches
	 * <em>finance/stock/ibm</em> and <em>finance/stock/xyz</em>,
	 * 	but not <em>finance/stock/ibm/closingprice</em>. Also, because the single-level
	 * 	wildcard matches only a single level, <em>finance/+</em> does not match <em>finance</em>.</p>
	 *
	 * 	<p>Use
	 * 	the single-level wildcard at any level in the topic tree, and in conjunction
	 * 	with the multilevel wildcard. Specify the single-level wildcard next to the
	 * 	topic level separator, except when it is specified on its own. Therefore,
	 *  <em>+</em> and <em>finance/+</em> are both valid, but <em>finance+</em> is
	 *  not valid. <span>The single-level wildcard can be used at the end of the
	 *  topic tree or within the topic tree.
	 * 	For example, <em>finance/+</em> and <em>finance/+/ibm</em> are both valid.</span></p>
	 * 	</dd>
	 * </dl>

	 *
	 * <p>This is a blocking method that returns once subscribe completes</p>
	 *
	 * @param topicFilters one or more topics to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service to subscribe each topic at.Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param messageListeners one or more callbacks to handle incoming messages
	 * @throws MqttException if there was an error registering the subscription.
	 * @return token used to track the subscribe after it has completed.
	 * @throws IllegalArgumentException if the two supplied arrays are not the same size.
	 */
	public IMqttToken subscribeWithResponse(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) throws MqttException;
	
	/**
	 * Requests the server unsubscribe the client from a topic.
	 *
	 * @see #unsubscribe(String[])
	 * @param topicFilter the topic to unsubscribe from. It must match a topicFilter
	 * specified on the subscribe.
	 * @throws MqttException if there was an error unregistering the subscription.
	 */
  public void unsubscribe(String topicFilter) throws MqttException;

	/**
	 * Requests the server unsubscribe the client from one or more topics.
	 * <p>
	 * Unsubcribing is the opposite of subscribing. When the server receives the
	 * unsubscribe request it looks to see if it can find a subscription for the
	 * client and then removes it. After this point the server will send no more
	 * messages to the client for this subscription.
	 * </p>
	 * <p>The topic(s) specified on the unsubscribe must match the topic(s)
	 * specified in the original subscribe request for the subscribe to succeed
	 * </p>
	 *
	 * <p>This is a blocking method that returns once unsubscribe completes</p>
	 *
	 * @param topicFilters one or more topics to unsubscribe from. Each topicFilter
	 * must match one specified on a subscribe
	 * @throws MqttException if there was an error unregistering the subscription.
	 */
  public void unsubscribe(String[] topicFilters) throws MqttException;


	/**
	 * Publishes a message to a topic on the server and return once it is delivered.
	 * <p>This is a convenience method, which will
	 * create a new {@link MqttMessage} object with a byte array payload and the
	 * specified QoS, and then publish it.  All other values in the
	 * message will be set to the defaults.
	 * </p>
	 *
	 * @param topic  to deliver the message to, for example "finance/stock/ibm".
	 * @param payload the byte array to use as the payload
	 * @param qos the Quality of Service to deliver the message at.  Valid values are 0, 1 or 2.
	 * @param retained whether or not this message should be retained by the server.
	 * @throws MqttPersistenceException when a problem with storing the message
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @throws MqttException for other errors encountered while publishing the message.
	 * For instance client not connected.
	 * @see #publish(String, MqttMessage)
	 * @see MqttMessage#setQos(int)
	 * @see MqttMessage#setRetained(boolean)
	 */
	public void publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException, MqttPersistenceException;

	/**
	 * Publishes a message to a topic on the server.
	 * <p>
	 * Delivers a message to the server at the requested quality of service and returns control
	 * once the message has been delivered. In the event the connection fails or the client
	 * stops, any messages that are in the process of being delivered will be delivered once
	 * a connection is re-established to the server on condition that:</p>
	 * <ul>
	 * <li>The connection is re-established with the same clientID</li>
	 * <li>The original connection was made with (@link MqttConnectOptions#setCleanSession(boolean)}
	 * set to false</li>
	 * <li>The connection is re-established with (@link MqttConnectOptions#setCleanSession(boolean)}
	 * set to false</li>
	 * </ul>
	 * <p>In the event that the connection breaks or the client stops it is still possible to determine
	 * when the delivery of the message completes. Prior to re-establishing the connection to the server:</p>
	 * <ul>
	 * <li>Register a {@link #setCallback(MqttCallback)} callback on the client and the delivery complete
	 * callback will be notified once a delivery of a message completes
	 * <li>or call {@link #getPendingDeliveryTokens()} which will return a token for each message that
	 * is in-flight.  The token can be used to wait for delivery to complete.
	 * </ul>
	 *
	 * <p>When building an application,
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
	 * 	<li>Do not include the null character (Unicode<pre> \x0000</pre>) in
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
	 * 
	 *
	 * <p>This is a blocking method that returns once publish completes</p>	 *
	 *
	 * @param topic  to deliver the message to, for example "finance/stock/ibm".
	 * @param message to delivery to the server
 	 * @throws MqttPersistenceException when a problem with storing the message
	 * @throws MqttException for other errors encountered while publishing the message.
	 * For instance client not connected.
	 */
	public void publish(String topic, MqttMessage message) throws MqttException, MqttPersistenceException;

	/**
	 * Sets the callback listener to use for events that happen asynchronously.
	 * <p>There are a number of events that listener will be notified about. These include:</p>
	 * <ul>
	 * <li>A new message has arrived and is ready to be processed</li>
	 * <li>The connection to the server has been lost</li>
	 * <li>Delivery of a message to the server has completed.</li>
	 * </ul>
	 * <p>Other events that track the progress of an individual operation such
	 * as connect and subscribe can be tracked using the {@link MqttToken} passed to the
	 * operation<p>
	 * @see MqttCallback
	 * @param callback the class to callback when for events related to the client
	 */
	public void setCallback(MqttCallback callback);

	/**
	 * Get a topic object which can be used to publish messages.
	 * <p>An alternative method that should be used in preference to this one when publishing a message is:</p>
	 * <ul>
	 * <li>{@link MqttClient#publish(String, MqttMessage)} to publish a message in a blocking manner
	 * <li>or use publish methods on the non-blocking client like {@link IMqttAsyncClient#publish(String, MqttMessage, Object, IMqttActionListener)}
	 * </ul>
	 * <p>When building an application,
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
	 * 	<li>Do not include the null character (Unicode<pre> \x0000</pre>) in
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
	 * </ul>
	 *
	 * @param topic the topic to use, for example "finance/stock/ibm".
	 * @return an MqttTopic object, which can be used to publish messages to
	 * the topic.
	 * @throws IllegalArgumentException if the topic contains a '+' or '#'
	 * wildcard character.
	 */
	public MqttTopic getTopic(String topic);

	/**
	 * Determines if this client is currently connected to the server.
	 *
	 * @return <code>true</code> if connected, <code>false</code> otherwise.
	 */
	public boolean isConnected();

	/**
	 * Returns the client ID used by this client.
	 * <p>All clients connected to the
	 * same server or server farm must have a unique ID.
	 * </p>
	 *
	 * @return the client ID used by this client.
	 */
	public String getClientId();

	/**
	 * Returns the address of the server used by this client, as a URI.
	 * <p>The format is the same as specified on the constructor.
	 * </p>
	 *
	 * @return the server's address, as a URI String.
	 * @see MqttAsyncClient#MqttAsyncClient(String, String)
	 */
	public String getServerURI();

	/**
	 * Returns the delivery tokens for any outstanding publish operations.
	 * <p>If a client has been restarted and there are messages that were in the
	 * process of being delivered when the client stopped this method will
	 * return a token for each message enabling the delivery to be tracked
	 * Alternately the {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)}
	 * callback can be used to track the delivery of outstanding messages.
	 * </p>
	 * <p>If a client connects with cleanSession true then there will be no
	 * delivery tokens as the cleanSession option deletes all earlier state.
	 * For state to be remembered the client must connect with cleanSession
	 * set to false</P>
	 * @return zero or more delivery tokens
	 */
	public IMqttDeliveryToken[] getPendingDeliveryTokens();
	
	/**
	 * If manualAcks is set to true, then on completion of the messageArrived callback
	 * the MQTT acknowledgements are not sent.  You must call messageArrivedComplete
	 * to send those acknowledgements.  This allows finer control over when the acks are
	 * sent.  The default behaviour, when manualAcks is false, is to send the MQTT
	 * acknowledgements automatically at the successful completion of the messageArrived
	 * callback method.
	 * @param manualAcks if set to true, MQTT acknowledgements are not sent.
	 */
	public void setManualAcks(boolean manualAcks);
	
	/**
	 * Indicate that the application has completed processing the message with id messageId.
	 * This will cause the MQTT acknowledgement to be sent to the server.
	 * @param messageId the MQTT message id to be acknowledged
	 * @param qos the MQTT QoS of the message to be acknowledged
	 * @throws MqttException if there was a problem sending the acknowledgement
	 */
	public void messageArrivedComplete(int messageId, int qos) throws MqttException;

	/**
	 * Close the client
	 * Releases all resource associated with the client. After the client has
	 * been closed it cannot be reused. For instance attempts to connect will fail.
	 * @throws MqttException  if the client is not disconnected.
	 */
	public void close() throws MqttException;
}
