/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
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
 *    Bin Zhang - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.paho.mqtt.ui.core.event;

import org.eclipse.paho.mqtt.ui.core.model.Connection;

/**
 * Event selector used by {@code IEventService}
 * 
 * @author Bin Zhang
 */
public class Selector {
	// Wild card character for matching all sub topics
	private static final String ALL_SUB_TOPICS = "*"; //$NON-NLS-1$
	// Base name of all events
	private static final String TOPIC_BASE = "org/eclipse/paho/mqtt/event/"; //$NON-NLS-1$

	private static final String TOPIC_BASE_FORMAT = TOPIC_BASE + "%s"; //$NON-NLS-1$
	// client/<topic>
	private static final String CONNECTION_REQUEST_FORMAT = TOPIC_BASE + "client/%s"; //$NON-NLS-1$
	// client/<id>/<topic>
	private static final String CONNECTION_RESPONSE_FORMAT = TOPIC_BASE + "client/%s/%s"; //$NON-NLS-1$

	// topic & type
	private final String topic;
	private final Type type;

	// selectors

	// UI events
	public static Selector ofNewConnection() {
		return new Selector(format(TOPIC_BASE_FORMAT, Type.ADD_CONNECTION.name()), Type.ADD_CONNECTION);
	}

	public static Selector ofRenameConnection(Connection connection) {
		return connectionSelector(connection, Type.RENAME_CONNECTION);
	}

	// Requests
	/**
	 * Send all ofXxxed events
	 */
	public static Selector ofConnect() {
		return requestSelector(Type.CONNECT);
	}

	public static Selector ofDisconnect() {
		return requestSelector(Type.DISCONNECT);
	}

	public static Selector ofSubscribe() {
		return requestSelector(Type.SUBSCRIBE);
	}

	public static Selector ofUnsubscribe() {
		return requestSelector(Type.UNSUBSCRIBE);
	}

	public static Selector ofPublish() {
		return requestSelector(Type.PUBLISH);
	}

	private static Selector requestSelector(Type type) {
		return new Selector(format(CONNECTION_REQUEST_FORMAT, type == Type.ALL ? ALL_SUB_TOPICS : type.name()), type);
	}

	// Responses
	/**
	 * Receive all ofXxxed events
	 */
	public static Selector ofAllResponses(Connection connection) {
		return connectionSelector(connection, Type.ALL);
	}

	public static Selector ofDisconnected(Connection connection) {
		return connectionSelector(connection, Type.DISCONNECTED);
	}

	public static Selector ofDisconnectFailed(Connection connection) {
		return connectionSelector(connection, Type.DISCONNECT_FAILED);
	}

	public static Selector ofConnected(Connection connection) {
		return connectionSelector(connection, Type.CONNECTED);
	}

	public static Selector ofConnectionLost(Connection connection) {
		return connectionSelector(connection, Type.CONNECTION_LOST);
	}

	public static Selector ofConnectFailed(Connection connection) {
		return connectionSelector(connection, Type.CONNECT_FAILED);
	}

	public static Selector ofSubscribed(Connection connection) {
		return connectionSelector(connection, Type.SUBSCRIBED);
	}

	public static Selector ofSubscribeFailed(Connection connection) {
		return connectionSelector(connection, Type.SUBSCRIBE_FAILED);
	}

	public static Selector ofUnsubscribed(Connection connection) {
		return connectionSelector(connection, Type.UNSUBSCRIBED);
	}

	public static Selector ofUnsubscribeFailed(Connection connection) {
		return connectionSelector(connection, Type.UNSUBSCRIBE_FAILED);
	}

	public static Selector ofPublished(Connection connection) {
		return connectionSelector(connection, Type.PUBLISHED);
	}

	public static Selector ofPublishedFailed(Connection connection) {
		return connectionSelector(connection, Type.PUBLISH_FAILED);
	}

	public static Selector ofReceived(Connection connection) {
		return connectionSelector(connection, Type.RECEIVED);
	}

	/**
	 * @param connection
	 * @param type
	 * @return selector
	 */
	private static Selector connectionSelector(Connection connection, Type type) {
		return new Selector(format(CONNECTION_RESPONSE_FORMAT, connection.getId(), type == Type.ALL ? ALL_SUB_TOPICS
				: type.name()), type);
	}

	/**
	 * @param topic
	 * @param type
	 */
	private Selector(String topic, Type type) {
		this.topic = topic;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public String select() {
		return topic;
	}

	/**
	 * @param format
	 * @param args
	 */
	private static String format(String format, Object... args) {
		return String.format(format, args);
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("topic=").append(topic)
				.append(",").append("type=").append(type).append("]").toString();
	}

	/**
	 * Selector type
	 */
	public enum Type {

		// matches all
		ALL,

		// for ui
		ADD_CONNECTION, RENAME_CONNECTION,

		// requests
		CONNECT, DISCONNECT, PUBLISH, SUBSCRIBE, UNSUBSCRIBE,

		// responses
		CONNECTED, DISCONNECTED, PUBLISHED, SUBSCRIBED, UNSUBSCRIBED, RECEIVED, CONNECT_FAILED, CONNECTION_LOST,

		DISCONNECT_FAILED, PUBLISH_FAILED, SUBSCRIBE_FAILED, UNSUBSCRIBE_FAILED;
	}

}
