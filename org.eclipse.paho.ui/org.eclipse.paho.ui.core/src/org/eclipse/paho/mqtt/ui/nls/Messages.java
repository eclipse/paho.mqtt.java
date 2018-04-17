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
package org.eclipse.paho.mqtt.ui.nls;

import org.eclipse.osgi.util.NLS;

/**
 * NLS binding messages
 * 
 * @author Bin Zhang
 */
public final class Messages {
	// commons
	public static String QOS_AT_MOST_ONCE;
	public static String QOS_AT_LEAST_ONCE;
	public static String QOS_EXACTLY_ONCE;

	public static String LABEL_FILE;
	public static String LABEL_HELP;
	public static String LABEL_EXIT;
	public static String LABEL_ABOUT;
	public static String LABEL_BROWSE;
	public static String LABEL_SECONDS;
	public static String LABEL_CLEAR;
	public static String LABEL_DELETE;
	public static String LABEL_SAVE;
	public static String LABEL_CLOSE;
	public static String LABEL_INFO;
	public static String LABEL_ERROR;
	public static String LABEL_YES;
	public static String LABEL_NO;
	public static String LABEL_NEW_CONNECTION;

	public static String TOOLTIP_DBCLICK_TO_EDIT;

	// events
	public static String EVENT_CONNECTED;
	public static String EVENT_DISCONNECTED;
	public static String EVENT_PUBLISHED;
	public static String EVENT_SUBSCRIBED;
	public static String EVENT_UNSUBSCRIBED;
	public static String EVENT_RECEIVED;
	public static String EVENT_CONNECT_FAILED;
	public static String EVENT_CONNECTION_LOST;
	public static String EVENT_DISCONNECT_FAILED;
	public static String EVENT_PUBLISH_FAILED;
	public static String EVENT_SUBSCRIBE_FAILED;
	public static String EVENT_UNSUBSCRIBE_FAILED;

	// validation
	public static String VALIDATION_VALUE_REQUIRED;
	public static String VALIDATION_SERVER_URI_REQUIRED;
	public static String VALIDATION_INVALID_SERVER_URI;
	public static String VALIDATION_INVALID_SERVER_URI_MSG;
	public static String VALIDATION_INVALID_CLIENT_ID;
	public static String VALIDATION_INVALID_NUM;
	public static String VALIDATION_NUM_GT_MAX;
	public static String VALIDATION_NUM_LT_MIN;
	public static String VALIDATION_INVALID_TOPIC_LEN;
	public static String VALIDATION_TOPIC_MULTI_LEVEL_WILDCARD;
	public static String VALIDATION_TOPIC_SINGLE_LEVEL_WILDCARD;
	public static String VALIDATION_TOPIC_WILDCARDS_NOT_ALLOWED;

	// Nav tree
	public static String NAV_TREE_TITLE;
	public static String NAV_TREE_DELETE_CONFIRM_TITLE;
	public static String NAV_TREE_DELETE_CONFIRM_MESSAGE;

	// ************************
	// MQTT Tab
	// ************************
	public static String MQTT_TAB_TITLE;
	public static String MQTT_TAB_GROUP_CONN;
	public static String MQTT_TAB_GROUP_CONN_SERVERURI;
	public static String MQTT_TAB_GROUP_CONN_CLIENTID;
	public static String MQTT_TAB_GROUP_CONN_STATUS;
	public static String MQTT_TAB_GROUP_CONN_STATUS_CONNECTED;
	public static String MQTT_TAB_GROUP_CONN_STATUS_DISCONNECTED;
	public static String MQTT_TAB_GROUP_CONN_STATUS_FAILED;
	public static String MQTT_TAB_GROUP_CONN_BTN_CONNECT;
	public static String MQTT_TAB_GROUP_CONN_BTN_DISCONNECT;

	// MQTT Tab - group subscription
	public static String MQTT_TAB_GROUP_SUB;
	public static String MQTT_TAB_GROUP_SUB_ADD_BTN_TOOLTIP;
	public static String MQTT_TAB_GROUP_SUB_RM_BTN_TOOLTIP;
	public static String MQTT_TAB_GROUP_SUB_CLEAR_BTN_TOOLTIP;
	public static String MQTT_TAB_GROUP_SUB_TOPIC;
	public static String MQTT_TAB_GROUP_SUB_QOS;
	public static String MQTT_TAB_GROUP_SUB_BTN_SUB;
	public static String MQTT_TAB_GROUP_SUB_BTN_UNSUB;

	// MQTT Tab - group publication
	public static String MQTT_TAB_GROUP_PUB;
	public static String MQTT_TAB_GROUP_PUB_TOPIC;
	public static String MQTT_TAB_GROUP_PUB_QOS;
	public static String MQTT_TAB_GROUP_PUB_RETAINED;
	public static String MQTT_TAB_GROUP_PUB_HEX;
	public static String MQTT_TAB_GROUP_PUB_MSG;
	public static String MQTT_TAB_GROUP_PUB_FILE;
	public static String MQTT_TAB_GROUP_PUB_PUBLISH;
	public static String MQTT_TAB_GROUP_PUB_FD_OPENFILE;

	// ************************
	// Options Tab
	// ************************
	public static String OPT_TAB_TITLE;
	// LWT
	public static String OPT_TAB_GROUP_LWT;
	public static String OPT_TAB_GROUP_LWT_TOPIC;
	public static String OPT_TAB_GROUP_LWT_QOS;
	public static String OPT_TAB_GROUP_LWT_RETAINED;
	public static String OPT_TAB_GROUP_LWT_HEX;
	public static String OPT_TAB_GROUP_LWT_MSG;

	// HA
	public static String OPT_TAB_GROUP_HA;
	public static String OPT_TAB_GROUP_HA_ADD_BTN_TOOLTIP;
	public static String OPT_TAB_GROUP_HA_RM_BTN_TOOLTIP;
	public static String OPT_TAB_GROUP_HA_CLEAR_BTN_TOOLTIP;
	public static String OPT_TAB_GROUP_HA_SERVER_URIS;

	// SSL
	public static String OPT_TAB_GROUP_SSL;
	public static String OPT_TAB_GROUP_SSL_KEY_STORE;
	public static String OPT_TAB_GROUP_SSL_KEY_STORE_DLG;
	public static String OPT_TAB_GROUP_SSL_KEY_STORE_PWD;
	public static String OPT_TAB_GROUP_SSL_TRUST_STORE;
	public static String OPT_TAB_GROUP_SSL_TRUST_STORE_DLG;
	public static String OPT_TAB_GROUP_SSL_TRUST_STORE_PWD;

	// General
	public static String OPT_TAB_GROUP_GENERAL;
	public static String OPT_TAB_GROUP_GENERAL_CLEAN_SESSION;
	public static String OPT_TAB_GROUP_GENERAL_ENABLE_SSL;
	public static String OPT_TAB_GROUP_GENERAL_ENABLE_HA;
	public static String OPT_TAB_GROUP_GENERAL_ENABLE_LWT;
	public static String OPT_TAB_GROUP_GENERAL_KEEP_ALIVE;
	public static String OPT_TAB_GROUP_GENERAL_CONNECTION_TIMEOUT;
	public static String OPT_TAB_GROUP_GENERAL_ENABLE_LOGIN;
	public static String OPT_TAB_GROUP_GENERAL_USERNAME;
	public static String OPT_TAB_GROUP_GENERAL_PASSWORD;
	public static String OPT_TAB_GROUP_GENERAL_ENABLE_PERSISTENCE;
	public static String OPT_TAB_GROUP_GENERAL_PERSISTENCE_DIRECTORY;
	public static String OPT_TAB_GROUP_GENERAL_PERSISTENCE_DLG;

	// ************************
	// History Tab
	// ************************
	public static String HISTORY_TAB_TITLE;
	public static String HISTORY_TAB_EVENT;
	public static String HISTORY_TAB_TOPIC;
	public static String HISTORY_TAB_QOS;
	public static String HISTORY_TAB_MSG;
	public static String HISTORY_TAB_RETAINED;
	public static String HISTORY_TAB_TIME;

	// ************************
	// Last Message Tab
	// ************************
	public static String LASTMSG_TAB_TITLE;
	public static String LASTMSG_TAB_TOPIC;
	public static String LASTMSG_TAB_QOS;
	public static String LASTMSG_TAB_MSG;
	public static String LASTMSG_TAB_RETAINED;
	public static String LASTMSG_TAB_TIME;

	public static String DLG_MESSAGE_VIEWER_TITLE;
	public static String DLG_MESSAGE_VIEWER_MSG_SAVED;

	// init
	private static final String BUNDLE_NAME = "org.eclipse.paho.mqtt.ui.nls.messages"; //$NON-NLS-1$
	static {
		initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 * 
	 * @param message the message to be manipulated bindings
	 * @param bindings An array of objects to be inserted into the message
	 * @return the manipulated String
	 */
	public static String bind(String message, Object... bindings) {
		return NLS.bind(message, bindings);
	}

	/**
	 * Initialize the given class with the values from the specified message bundle.
	 * 
	 * @param bundleName - fully qualified path of the class name
	 * @param clazz - the class where the constants will exist
	 */
	private static void initializeMessages(String bundleName, Class<?> clazz) {
		NLS.initializeMessages(bundleName == null ? BUNDLE_NAME : bundleName, Messages.class);
	}

}