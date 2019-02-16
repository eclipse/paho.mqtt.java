/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp.
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
 *    James Sutton - MQTT V5 support
 */

package org.eclipse.paho.mqttv5.client.vertx;

import org.eclipse.paho.mqttv5.client.vertx.util.Debug;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.MqttSecurityException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

public interface IMqttAsyncClient {

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
	IMqttToken connect(Object userContext, MqttActionListener callback) throws MqttException, MqttSecurityException;

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
	IMqttToken connect() throws MqttException, MqttSecurityException;

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
	IMqttToken connect(MqttConnectionOptions options) throws MqttException, MqttSecurityException;

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
	IMqttToken connect(MqttConnectionOptions options, Object userContext, MqttActionListener callback)
			throws MqttException, MqttSecurityException;

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
	 * @see #disconnect(long, Object, MqttActionListener, int, MqttProperties)
	 *
	 */
	IMqttToken disconnect(Object userContext, MqttActionListener callback) throws MqttException;

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
	 * @see #disconnect(long, Object, MqttActionListener, int, MqttProperties)
	 */
	IMqttToken disconnect() throws MqttException;

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
	 * @see #disconnect(long, Object, MqttActionListener, int, MqttProperties)
	 */
	IMqttToken disconnect(long quiesceTimeout) throws MqttException;

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
	 * from the server. If the cleanStart flag was set to false and is set to
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
	 * @param disconnectProperties
	 *            The {@link MqttProperties} to be sent.
	 * @return token used to track and wait for the connect to complete. The token
	 *         will be passed to any callback that has been set.
	 * @throws MqttException
	 *             for problems encountered while disconnecting
	 */
	IMqttToken disconnect(long quiesceTimeout, Object userContext, MqttActionListener callback, int reasonCode,
			MqttProperties disconnectProperties) throws MqttException;

	/**
	 * Determines if this client is currently connected to the server.
	 *
	 * @return <code>true</code> if connected, <code>false</code> otherwise.
	 */
	boolean isConnected();

	/**
	 * Returns the client ID used by this client.
	 * <p>
	 * All clients connected to the same server or server farm must have a unique
	 * ID.
	 * </p>
	 *
	 * @return the client ID used by this client.
	 */
	String getClientId();

	void setClientId(String clientId);

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
	String getServerURI();

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
	String getCurrentServerURI();

	/*
	 * (non-Javadoc) Check and send a ping if needed. <p>By default, client sends
	 * PingReq to server to keep the connection to server. For some platforms which
	 * cannot use this mechanism, such as Android, developer needs to handle the
	 * ping request manually with this method. </p>
	 *
	 * @throws MqttException for other errors encountered while publishing the
	 * message.
	 */
	IMqttToken checkPing(Object userContext, MqttActionListener callback) throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener,
	 *      MqttProperties) throws MqttException
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
	IMqttToken subscribe(String topicFilter, int qos, Object userContext, MqttActionListener callback)
			throws MqttException;
	
	IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, MqttActionListener callback)
			throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener,
	 *      MqttProperties)
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
	IMqttToken subscribe(String topicFilter, int qos) throws MqttException;
	
	IMqttToken subscribe(String[] topicFilters, int[] qos) throws MqttException;

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener,
	 *      MqttProperties)
	 *
	 * @param subscriptions
	 *            one or more {@link MqttSubscription} defining the subscription to
	 *            be made.
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	IMqttToken subscribe(MqttSubscription[] subscriptions) throws MqttException;

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
	 * If (@link MqttConnectOptions#setCleanStart(boolean)} was set to true when
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
	 * If (@link MqttConnectOptions#setCleanStart(boolean)} was set to false when
	 * connecting to the server then the subscription remains in place until either:
	 * </p>
	 * <ul>
	 * <li>An unsubscribe method is called to unsubscribe the topic</li>
	 * <li>The next time the client connects with cleanStart set to true</li>
	 * </ul>
	 * <p>
	 * With cleanStart set to false the MQTT server will store messages on behalf
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
	 * @param subscriptionProperties
	 *            The {@link MqttProperties} to be sent.
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 * @throws IllegalArgumentException
	 *             if the two supplied arrays are not the same size.
	 */
	IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			MqttProperties subscriptionProperties) throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener,
	 *      MqttProperties)
	 *
	 * @param mqttSubscription
	 *            a {@link MqttSubscription} defining the subscription to be made.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param callback
	 *            optional listener that will be notified when subscribe has
	 *            completed
	 * @param messageListener
	 *            a callback to handle incoming messages
	 * @param subscriptionProperties
	 *            The {@link MqttProperties} to be sent.
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	IMqttToken subscribe(MqttSubscription mqttSubscription, Object userContext, MqttActionListener callback,
			IMqttMessageListener messageListener, MqttProperties subscriptionProperties) throws MqttException;

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener,
	 *      MqttProperties)
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
	IMqttToken subscribe(MqttSubscription subscription, IMqttMessageListener messageListener) throws MqttException;

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener,
	 *      MqttProperties)
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
	IMqttToken subscribe(MqttSubscription[] subscriptions, IMqttMessageListener messageListener) throws MqttException;

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener,
	 *      MqttProperties)
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
	 *            one or more callbacks to handle incoming messages.
	 * @param subscriptionProperties
	 *            The {@link MqttProperties} to be sent.
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			IMqttMessageListener[] messageListeners, MqttProperties subscriptionProperties) throws MqttException;

	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 *
	 * @see #subscribe(MqttSubscription[], Object, MqttActionListener,
	 *      MqttProperties)
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
	 * @param messageListener
	 *            a callback to handle incoming messages.
	 * @param subscriptionProperties
	 *            The {@link MqttProperties} to be sent.
	 * @return token used to track and wait for the subscribe to complete. The token
	 *         will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error registering the subscription.
	 */
	IMqttToken subscribe(MqttSubscription[] subscriptions, Object userContext, MqttActionListener callback,
			IMqttMessageListener messageListener, MqttProperties subscriptionProperties) throws MqttException;

	/**
	 * Requests the server unsubscribe the client from a topics.
	 *
	 * @see #unsubscribe(String[], Object, MqttActionListener, MqttProperties)
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
	IMqttToken unsubscribe(String topicFilter, Object userContext, MqttActionListener callback) throws MqttException;

	/**
	 * Requests the server unsubscribe the client from a topic.
	 *
	 * @see #unsubscribe(String[], Object, MqttActionListener, MqttProperties)
	 * @param topicFilter
	 *            the topic to unsubscribe from. It must match a topicFilter
	 *            specified on an earlier subscribe.
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error unregistering the subscription.
	 */
	IMqttToken unsubscribe(String topicFilter) throws MqttException;

	/**
	 * Requests the server unsubscribe the client from one or more topics.
	 *
	 * @see #unsubscribe(String[], Object, MqttActionListener, MqttProperties)
	 *
	 * @param topicFilters
	 *            one or more topics to unsubscribe from. Each topicFilter must
	 *            match one specified on an earlier subscribe. *
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error unregistering the subscription.
	 */
	IMqttToken unsubscribe(String[] topicFilters) throws MqttException;

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
	 * @param unsubscribeProperties
	 *            The {@link MqttProperties} to be sent.
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws MqttException
	 *             if there was an error unregistering the subscription.
	 */
	IMqttToken unsubscribe(String[] topicFilters, Object userContext, MqttActionListener callback,
			MqttProperties unsubscribeProperties) throws MqttException;

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
	void setCallback(MqttCallback callback);

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
	void setManualAcks(boolean manualAcks);

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
	void messageArrivedComplete(int messageId, int qos) throws MqttException;

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
	 * If a client connects with cleanStart true then there will be no delivery
	 * tokens as the cleanStart option deletes all earlier state. For state to be
	 * remembered the client must connect with cleanStart set to false
	 * </P>
	 *
	 * @return zero or more delivery tokens
	 */
	IMqttDeliveryToken[] getPendingDeliveryTokens();

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
	IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, Object userContext,
			MqttActionListener callback) throws MqttException, MqttPersistenceException;

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
	IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained)
			throws MqttException, MqttPersistenceException;

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
	IMqttDeliveryToken publish(String topic, MqttMessage message) throws MqttException, MqttPersistenceException;

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
	 * MqttConnectOptions#setCleanStart(boolean)} set to false
	 * <li>The connection is re-established with (@link
	 * MqttConnectOptions#setCleanStart(boolean)} set to false
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
	 *            completed to the requested quality of service.
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
	IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, MqttActionListener callback)
			throws MqttException, MqttPersistenceException;

	/**
	 * An AUTH Packet is sent from Client to Server or Server to Client as part of
	 * an extended authentication exchange, such as challenge / response
	 * authentication. It is a protocol error for the Client or Server to send an
	 * AUTH packet if the CONNECT packet did not contain the same Authentication
	 * Method.
	 * 
	 * @param reasonCode
	 *            The Reason code, can be Success (0), Continue authentication (24)
	 *            or Re-authenticate (25).
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null if
	 *            not required.
	 * @param properties
	 *            The {@link MqttProperties} to be sent, containing the
	 *            Authentication Method, Authentication Data and any required User
	 *            Defined Properties.
	 * @return token used to track and wait for the authentication to complete.
	 * @throws MqttException if an exception occurs whilst sending the authenticate packet.
	 */
	IMqttToken authenticate(int reasonCode, Object userContext, MqttProperties properties) throws MqttException;

	/**
	 * User triggered attempt to reconnect
	 *
	 * @throws MqttException
	 *             if there is an issue with reconnecting
	 */
	void reconnect() throws MqttException;

	/**
	 * Sets the DisconnectedBufferOptions for this client
	 *
	 * @param bufferOpts
	 *            the {@link DisconnectedBufferOptions}
	 */
	void setBufferOpts(DisconnectedBufferOptions bufferOpts);

	/**
	 * Returns the number of messages in the Disconnected Message Buffer
	 *
	 * @return Count of messages in the buffer
	 */
	int getBufferedMessageCount();

	/**
	 * Returns a message from the Disconnected Message Buffer
	 *
	 * @param bufferIndex
	 *            the index of the message to be retrieved.
	 * @return the message located at the bufferIndex
	 */
	MqttWireMessage getBufferedMessage(int bufferIndex);

	/**
	 * Deletes a message from the Disconnected Message Buffer
	 *
	 * @param bufferIndex
	 *            the index of the message to be deleted.
	 */
	void deleteBufferedMessage(int bufferIndex);

	/**
	 * Returns the current number of outgoing in-flight messages being sent by the
	 * client. Note that this number cannot be guaranteed to be 100% accurate as
	 * some messages may have been sent or queued in the time taken for this method
	 * to return.
	 *
	 * @return the current number of in-flight messages.
	 */
	int getInFlightMessageCount();

	/**
	 * Close the client Releases all resource associated with the client. After the
	 * client has been closed it cannot be reused. For instance attempts to connect
	 * will fail.
	 *
	 * @throws MqttException
	 *             if the client is not disconnected.
	 */
	void close() throws MqttException;

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
	void close(boolean force) throws MqttException;

	/**
	 * Return a debug object that can be used to help solve problems.
	 *
	 * @return the {@link Debug} object
	 */
	Debug getDebug();

	IMqttToken subscribe(MqttSubscription subscription) throws MqttException;

}