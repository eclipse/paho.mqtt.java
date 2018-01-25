/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corp.
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

package org.eclipse.paho.mqttv5.client.alpha;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttConnectionResult;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttResult;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.MqttSecurityException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

/**
 * Enables an application to communicate with an MQTT server using non-blocking methods.
 * <p>
 * It provides applications a simple programming interface to all features of the MQTT version 5
 * specification including:
 * </p>
 * <ul>
 * <li>connect
 * <li>publish
 * <li>subscribe
 * <li>unsubscribe
 * <li>disconnect
 * </ul>
 * <p>
 * There are two styles of MQTT client, this one and {@link IMqttClient}.</p>
 * <ul>
 * <li>IMqttAsyncClient provides a set of non-blocking methods that return control to the
 * invoking application after initial validation of parameters and state. The main processing is
 * performed in the background so as not to block the application program's thread. This non-
 * blocking approach is handy when the application needs to carry on processing while the
 * MQTT action takes place. For instance connecting to an MQTT server can take time, using
 * the non-blocking connect method allows an application to display a busy indicator while the
 * connect action takes place in the background. Non blocking methods are particularly useful
 * in event oriented programs and graphical programs where invoking methods that take time
 * to complete on the the main or GUI thread can cause problems. The non-blocking interface
 * can also be used in blocking form.</li>
 * <li>IMqttClient provides a set of methods that block and return control to the application
 * program once the MQTT action has completed. It is a thin layer that sits on top of the
 * IMqttAsyncClient implementation and is provided mainly for compatibility with earlier
 * versions of the MQTT client. In most circumstances it is recommended to use IMqttAsyncClient
 * based clients which allow an application to mix both non-blocking and blocking calls. </li>
 * </ul>
 * <p>
 * An application is not restricted to using one style if an IMqttAsyncClient based client is used
 * as both blocking and non-blocking methods can be used in the same application. If an IMqttClient
 * based client is used then only blocking methods are available to the application.
 * For more details on the blocking client see {@link IMqttClient}</p>
 *
 * <p>There are two forms of non-blocking method:
 * <ol>
 *   <li>
 *     <p>In this form a callback is registered with the promise. The callback will be
 *     notified when the action succeeds or fails. The callback is invoked on the thread
 *     managed by the MQTT client so it is important that processing is minimised in the
 *     callback. If not the operation of the MQTT client will be inhibited. For example
 *     to be notified (called back) when a connect completes:</p>
 *     <pre>
 *     	IMqttToken conToken;
 *	    conToken = asyncClient.connect()
 *          .then(p -&gt; {
 *                  log("Connected");
 *                  return p;
 *              }, p -&gt; {
 *                  log ("connect failed" + exception);
 *              });
 *      </pre>
 *	    <p>An optional context object can be passed into the method which will then be made
 *      available in the callback. The context is stored by the MQTT client in the token
 *      which is then returned to the invoker. The context is also provided to the callback methods
 *      where the context can then be accessed.
 *     </p>
 *   </li>
 *   <li>
 *     <pre>
 *     IMqttToken token = asyncClient.method(parms)
 *     </pre>
 *     <p>In this form the method returns a token that can be used to track the
 *     progress of the action (method). The method provides a getPromise()
 *     method that represents the state of the action. Once the action is
 *     completed the getValue() method on the promise can be used to get the
 *     result, but more normally one of the Promise's callback methods will
 *     be used to find out if the action completed successfully or not. For example:
 * 	   </p>
 *     <pre>
 *      IMqttToken conToken;
 *   	conToken = asyncClient.client.connect(conToken);
 *     ... do some work...
 *   	conToken.getValue();
 *     </pre>
 *   </li>
 *
 *   <li>
 *     <pre>
 *     IMqttToken token method(parms, Object userContext, IMqttActionListener callback)
 *     </pre>
 *   </li>
 * </ol>
 *   <p>To understand when the delivery of a message is complete either of the two methods above
 *   can be used to either wait on or be notified when the publish completes.</p>
 *
 */
public interface IMqttAsyncClient extends IMqttCommonClient {
	/**
	 * Connects to an MQTT server using the default options.
	 * <p>The default options are specified in {@link MqttConnectionOptions} class.
	 * </p>
	 *
	 * @throws MqttSecurityException  for security related problems
	 * @throws MqttException  for non security related problems
	 * @return token used to track and wait for the connect to complete.
	 * @see #connect(MqttConnectionOptions, Object)
	 */
	public IMqttToken<IMqttConnectionResult<Void>, Void> connect() throws MqttException, MqttSecurityException;

	/**
	 * Connects to an MQTT server using the provided connect options.
	 * <p>The connection will be established using the options specified in the
	 * {@link MqttConnectionOptions} parameter.
	 * </p>
	 *
	 * @param options a set of connection parameters that override the defaults.
	 * @throws MqttSecurityException  for security related problems
	 * @throws MqttException  for non security related problems
	 * @return token used to track and wait for the connect to complete.
	 * @see #connect(MqttConnectionOptions, Object)
	 */
	public IMqttToken<IMqttConnectionResult<Void>, Void> connect(MqttConnectionOptions options) throws MqttException, MqttSecurityException ;
	/**
	 * Connects to an MQTT server using the default options.
	 * <p>The default options are specified in {@link MqttConnectionOptions} class.
	 * </p>
	 * @param <C> The OSGI Promise
	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @throws MqttSecurityException  for security related problems
	 * @throws MqttException  for non security related problems
	 * @return token used to track and wait for the connect to complete.
	 * @see #connect(MqttConnectionOptions, Object)
	 */
	public <C> IMqttToken<IMqttConnectionResult<C>, C> connect(C userContext) throws MqttException, MqttSecurityException;


	/**
	 * Connects to an MQTT server using the specified options.
	 * <p>The server to connect to is specified on the constructor.
	 * </p>
	 * <p>The method returns control before the connect completes. Completion can
	 * be tracked by:
	 * </p>
	 * <ul>
	 * <li>Registering a callback with the Promise returned by the token {@link IMqttToken#getPromise()} or</li>
	 * <li>Waiting on the Promise returned by the token {@link IMqttToken#getPromise()}</li>
	 * </ul>
	 *
	 * @param <C> The OSGI Promise
	 * @param options a set of connection parameters that override the defaults.
	 * @param userContext optional object for used to pass context to the callback. Use
	 * null if not required.
	 * @return token used to track and wait for the connect to complete.
	 * @throws MqttSecurityException  for security related problems
	 * @throws MqttException  for non security related problems including communication errors
	 */
	public <C> IMqttToken<IMqttConnectionResult<C>, C> connect(MqttConnectionOptions options, C userContext) throws MqttException, MqttSecurityException;

	/**
	 * Disconnects from the server.
	 * <p>An attempt is made to quiesce the client allowing outstanding
	 * work to complete before disconnecting. It will wait
	 * for a maximum of 30 seconds for work to quiesce before disconnecting.
 	 * This method must not be called from inside callback methods.
 	 * </p>
 	 *
	 * @return token used to track and wait for disconnect to complete
	 * @throws MqttException for problems encountered while disconnecting
	 * @see #disconnect(long, Object, MqttProperties)
	 */
	public IMqttToken<IMqttResult<Void>, Void> disconnect( ) throws MqttException;

	/**
	 * Disconnects from the server.
	 * <p>An attempt is made to quiesce the client allowing outstanding
	 * work to complete before disconnecting. It will wait
	 * for a maximum of the specified quiesce time  for work to complete before disconnecting.
 	 * This method must not be called from inside callback methods.
 	 * </p>
	 * @param quiesceTimeout the amount of time in milliseconds to allow for
	 * existing work to finish before disconnecting.  A value of zero or less
	 * means the client will not quiesce.
	 * @return token used to track and wait for disconnect to complete.
	 * @throws MqttException for problems encountered while disconnecting
	 * @see #disconnect(long, Object, MqttProperties)
	 */
	public IMqttToken<IMqttResult<Void>, Void> disconnect(long quiesceTimeout) throws MqttException;

	/**
	 * Disconnects from the server.
	 * <p>An attempt is made to quiesce the client allowing outstanding
	 * work to complete before disconnecting. It will wait
	 * for a maximum of 30 seconds for work to quiesce before disconnecting.
 	 * This method must not be called from inside callback methods.
 	 * </p>
 	 * @param <C> The OSGI Promise
 	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @return token used to track and wait for the disconnect to complete.
	 * @throws MqttException for problems encountered while disconnecting
	 * @see #disconnect(long, Object, MqttProperties)

	 */
	public <C> IMqttToken<IMqttResult<C>, C> disconnect( C userContext) throws MqttException;

	/**
	 * Disconnects from the server.
	 * <p>
	 * The client will wait for callback methods to
	 * complete. It will then wait for up to the quiesce timeout to allow for
	 * work which has already been initiated to complete. For instance when a QoS 2
	 * message has started flowing to the server but the QoS 2 flow has not completed.It
	 * prevents new messages being accepted and does not send any messages that have
	 * been accepted but not yet started delivery across the network to the server. When
	 * work has completed or after the quiesce timeout, the client will disconnect from
	 * the server. If the cleanSession flag was set to false and is set to false the
	 * next time a connection is made QoS 1 and 2 messages that
	 * were not previously delivered will be delivered.</p>
	 * <p>This method must not be called from inside callback methods.</p>
	 * <p>The method returns control before the disconnect completes. Completion can
	 * be tracked by:
	 * </p>
	 * <ul>
	 * <li>Registering a callback with the Promise returned by the token {@link IMqttToken#getPromise()} or</li>
	 * <li>Waiting on the Promise returned by the token {@link IMqttToken#getPromise()}</li>
	 * </ul>
	 *
	 * @param <C> The OSGI Promise
	 * @param quiesceTimeout the amount of time in milliseconds to allow for
	 * existing work to finish before disconnecting.  A value of zero or less
	 * means the client will not quiesce.
 	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @param disconnectProperties optional collection of properties to be sent in the disconnect packet to the server
	 * @return token used to track and wait for the connect to complete. The token
	 * will be passed to any callback that has been set.
	 * @throws MqttException for problems encountered while disconnecting
	 */
	public <C> IMqttToken<IMqttResult<C>, C> disconnect(long quiesceTimeout, C userContext, MqttProperties disconnectProperties) throws MqttException;
	
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
	 * Publishes a message to a topic on the server.
	 * <p>A convenience method, which will
	 * create a new {@link MqttMessage} object with a byte array payload and the
	 * specified QoS, and then publish it.
	 * </p>
	 *
	 * @param topic to deliver the message to, for example "finance/stock/ibm".
	 * @param payload the byte array to use as the payload
	 * @param qos the Quality of Service to deliver the message at. Valid values are 0, 1 or 2.
	 * @param retained whether or not this message should be retained by the server.
	 * @return token used to track and wait for the publish to complete. 
	 * @throws MqttPersistenceException when a problem occurs storing the message
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @throws MqttException for other errors encountered while publishing the message.
	 * For instance if too many messages are being processed.
	 * @see #publish(String, IMqttMessage, Object)
	 * @see MqttMessage#setQos(int)
	 * @see MqttMessage#setRetained(boolean)
	 */
	public IMqttDeliveryToken<Void> publish(String topic, byte[] payload, int qos,
			boolean retained ) throws MqttException, MqttPersistenceException;

	/**
	 * Publishes a message to a topic on the server.
 	 * <p>A convenience method, which will
	 * create a new {@link MqttMessage} object with a byte array payload and the
	 * specified QoS, and then publish it.
	 * </p>
	 *
	 * @param <C> The OSGI Promise
	 * @param topic  to deliver the message to, for example "finance/stock/ibm".
	 * @param payload the byte array to use as the payload
	 * @param qos the Quality of Service to deliver the message at.  Valid values are 0, 1 or 2.
	 * @param retained whether or not this message should be retained by the server.
	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @return token used to track and wait for the publish to complete. 
	 * @throws MqttPersistenceException when a problem occurs storing the message
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @throws MqttException for other errors encountered while publishing the message.
	 * For instance client not connected.
	 * @see #publish(String, IMqttMessage, Object)
	 * @see MqttMessage#setQos(int)
	 * @see MqttMessage#setRetained(boolean)
	 */
	public <C> IMqttDeliveryToken<C> publish(String topic, byte[] payload, int qos,
			boolean retained, C userContext ) throws MqttException, MqttPersistenceException;

	/**
	 * Publishes a message to a topic on the server.
	 * Takes an {@link MqttMessage} message and delivers it to the server at the
	 * requested quality of service.
	 *
	 * @param topic  to deliver the message to, for example "finance/stock/ibm".
	 * @param message to deliver to the server
	 * @return token used to track and wait for the publish to complete. The token
	 * will be passed to any callback that has been set.
	 * @throws MqttPersistenceException when a problem occurs storing the message
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @throws MqttException for other errors encountered while publishing the message.
	 * For instance client not connected.
	 * @see #publish(String, IMqttMessage, Object)
	 */
	public IMqttDeliveryToken<Void> publish(String topic, IMqttMessage message ) throws MqttException, MqttPersistenceException;

	/**
	 * Publishes a message to a topic on the server.
	 * <p>
	 * Once this method has returned cleanly, the message has been accepted for publication by the
	 * client and will be delivered on a background thread.
	 * In the event the connection fails or the client stops. Messages will be delivered to the
	 * requested quality of service once the connection is re-established to the server on condition that:
	 * </p>
	 * <ul>
	 * <li>The connection is re-established with the same clientID
	 * <li>The original connection was made with (@link MqttConnectOptions#setCleanSession(boolean)}
	 * set to false
	 * <li>The connection is re-established with (@link MqttConnectOptions#setCleanSession(boolean)}
	 * set to false
	 * <li>Depending when the failure occurs QoS 0 messages may not be delivered.
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
	 * 	<li>Do not include the null character (Unicode <pre>\x0000</pre>) in
	 * 	any topic.</li>
	 * </ul>
	 *
	 * <p>The following principles apply to the construction and content of a topic
	 * tree:</p>
	 * <ul>
	 * 	<li>The length is limited to 64k but within that there are no limits to the
	 * 	number of levels in a topic tree.</li>
	 * 	<li>There can be any number of root nodes; that is, there can be any number
	 * 	of topic trees.</li>
	 * 	</ul>
	 * 
	 * <p>The method returns control before the publish completes. Completion can
	 * be tracked by:
	 * </p>
	 * <ul>
	 * <li>Registering a callback with the Promise returned by the token {@link IMqttToken#getPromise()} or</li>
	 * <li>Waiting on the Promise returned by the token {@link IMqttToken#getPromise()}</li>
	 * </ul>
	 *
	 * @param <C> The OSGI Promise
	 * @param topic  to deliver the message to, for example "finance/stock/ibm".
	 * @param message to deliver to the server
	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @return token used to track and wait for the publish to complete.
 	 * @throws MqttPersistenceException when a problem occurs storing the message
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @throws MqttException for other errors encountered while publishing the message.
	 * For instance client not connected.
	 * @see MqttMessage
	 */
	public <C> IMqttDeliveryToken<C> publish(String topic, IMqttMessage message,
			C userContext) throws MqttException, MqttPersistenceException;

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(String, int, Object)
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttSubscriptionToken<Void> subscribe(String topicFilter, int qos) throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(String, int, Object) throws MqttException
	 *
	 * @param <C> The OSGI Promise
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public <C> IMqttSubscriptionToken<C> subscribe(String topicFilter, int qos, C userContext)
	throws MqttException;

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.</p>
	 *
	 * @see #subscribe(String, int, Object)
	 *
	 * @param subscriptions An array of {@link MqttSubscription} objects containing the details for a number of subscriptions.
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttSubscriptionToken<Void> subscribe(MqttSubscription[] subscriptions) throws MqttException;

	/**
	 * Subscribes to multiple topics, each of which may include wildcards.
 	 * <p>Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.</p>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true
	 * when when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * 
	 * <ul>
	 * <li>The client disconnects</li>
	 * <li>The stream returned by {@link IMqttSubscriptionToken#getStream()} is closed</li>
	 * <li>Any part of the stream pipeline returns negative backpressure to close the stream</li>
	 * </ul>
	 * 
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false
	 * when connecting to the server then the subscription remains in place
	 * until either:</p>
	 * <ul>
	 * <li>The stream returned by {@link IMqttSubscriptionToken#getStream()} is closed</li>
	 * <li>Any part of the stream pipeline returns negative backpressure to close the stream</li>
	 * <li>The next time the client connects with cleanSession set to true</li>
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
	 *  <ul>
	 *  <li>finance/stock/ibm</li>
	 *  <li>finance/stock/ibm/closingprice</li>
	 *  <li>finance/stock/ibm/currentprice</li>
	 *  </ul>
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
	 * <p>The method returns control without beginning the subscribe operation. The
	 * subscribe operation only begins when a terminal operation is called on the
	 * stream associated with the subscription. Completion can
	 * be tracked by:</p>
	 * <ul>
	 * <li>Registering a callback with the Promise returned by the token {@link IMqttToken#getPromise()} or</li>
	 * <li>Waiting on the Promise returned by the token {@link IMqttToken#getPromise()}</li>
	 * </ul>
	 *
	 * @param <C> The OSGI Promise
	 * @param subscriptions An array of {@link MqttSubscription} objects containing the details for a number of subscriptions.
	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @param subscribeProperties optional {@link MqttProperties} object to send with the message.
	 * @return token used to track and wait for the subscribe to complete. 
	 * @throws MqttException if there was an error registering the subscription.
	 * @throws IllegalArgumentException if the two supplied arrays are not the same size.
	 */
	public <C> IMqttSubscriptionToken<C> subscribe(MqttSubscription[] subscriptions, C userContext, MqttProperties subscribeProperties)
			throws MqttException;
	
	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(String, int, Object)
	 *
	 * @param <C> The OSGI Promise
	 * @param subscription A {@link MqttSubscription}  containing the details for a subscription.
	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public <C> IMqttSubscriptionToken<C> subscribe(MqttSubscription subscription, C userContext) throws MqttException;


	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(String, int, Object)
	 *
	 * @param subscription A {@link MqttSubscription}  containing the details for a subscription.
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 * @throws MqttException if there was an error registering the subscription.
	 */
	public IMqttSubscriptionToken<Void> subscribe(MqttSubscription subscription) throws MqttException;

}
