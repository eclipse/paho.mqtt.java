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
package org.eclipse.paho.mqtt.ui.core.model;

import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.paho.mqtt.ui.util.Strings;

/**
 * 
 * @author Bin Zhang
 * 
 */
public class Topic extends Bindable {
	private static final long serialVersionUID = 1L;

	// The forward slash (/) is used to separate each level within a topic tree and provide a hierarchical structure to
	// the topic space. The use of the topic level separator is significant when the two wildcard characters are
	// encountered in topics specified by subscribers.
	public static final String TOPIC_LEVEL_SEPARATOR = "/";

	// Multi-level wildcard
	// The number sign (#) is a wildcard character that matches any number of levels within a topic.
	public static final String MULTI_LEVEL_WILDCARD = "#";

	// Single-level wildcard
	// The plus sign (+) is a wildcard character that matches only one topic level.
	public static final String SINGLE_LEVEL_WILDCARD = "+";
	/* /# */
	public static final String MULTI_LEVEL_WILDCARD_PATTERN = TOPIC_LEVEL_SEPARATOR + MULTI_LEVEL_WILDCARD;
	/* /+ */
	public static final String SINGLE_LEVEL_WILDCARD_PATTERN = TOPIC_LEVEL_SEPARATOR + SINGLE_LEVEL_WILDCARD;
	/* #+ */
	public static final String TOPIC_WILDCARDS = MULTI_LEVEL_WILDCARD + SINGLE_LEVEL_WILDCARD;

	private static final int TOPIC_MIN_LEN = 1;
	private static final int TOPIC_MAX_LEN = 65536;
	private String topicString;
	private QoS qos;

	public Topic() {
	}

	/**
	 * @param topicString
	 * @param qos
	 */
	public Topic(String topicString, QoS qos) {
		this.topicString = topicString;
		this.qos = qos;
	}

	public String getTopicString() {
		return topicString;
	}

	public void setTopicString(String topicString) {
		this.topicString = topicString;
	}

	public QoS getQos() {
		return qos;
	}

	public void setQos(QoS qos) {
		this.qos = qos;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("topic=").append(topicString)
				.append(",").append("qos=").append(qos).append("]").toString();
	}

	/**
	 * @param topicString
	 * @param wildcardAllowed
	 * @throws IllegalArgumentException if invalid
	 */
	public static void validate(String topicString, boolean wildcardAllowed) {
		int topicLen = topicString.length();

		// length check
		satisfy(topicLen >= TOPIC_MIN_LEN || topicLen <= TOPIC_MAX_LEN,
				Messages.bind(Messages.VALIDATION_INVALID_TOPIC_LEN, TOPIC_MIN_LEN, TOPIC_MAX_LEN));

		// *******************************************************************************
		// 1) This is a subscription topic string that can contain wildcard characters
		// *******************************************************************************
		if (wildcardAllowed) {

			// Only # or +
			if (Strings.equalsAny(topicString, MULTI_LEVEL_WILDCARD, SINGLE_LEVEL_WILDCARD)) {
				return;
			}

			// 1) Check multi-level wildcard
			// Rule:
			// The multi-level wildcard can be specified only on its own or next to the topic level separator character.

			// Can only contains one multi-level wildcard character
			if (Strings.countMatches(topicString, MULTI_LEVEL_WILDCARD) > 1) {

				throw new IllegalArgumentException(Messages.bind(Messages.VALIDATION_TOPIC_MULTI_LEVEL_WILDCARD,
						topicString));
			}

			// The multi-level wildcard must be the last character used within the topic tree
			if (topicString.contains(MULTI_LEVEL_WILDCARD) && !topicString.endsWith(MULTI_LEVEL_WILDCARD_PATTERN)) {

				throw new IllegalArgumentException(Messages.bind(Messages.VALIDATION_TOPIC_MULTI_LEVEL_WILDCARD,
						topicString));
			}

			// 2) Check single-level wildcard
			// Rule:
			// The single-level wildcard can be used at any level in the topic tree, and in conjunction with the
			// multilevel wildcard. It must be used next to the topic level separator, except when it is specified on
			// its own.
			if (topicString.contains(SINGLE_LEVEL_WILDCARD) && topicString.indexOf(SINGLE_LEVEL_WILDCARD_PATTERN) == -1) {

				throw new IllegalArgumentException(Messages.bind(Messages.VALIDATION_TOPIC_SINGLE_LEVEL_WILDCARD,
						topicString));
			}

			return;
		}

		// *******************************************************************************
		// 2) This is a publish topic string that should not contains any wildcard characters
		// *******************************************************************************
		if (Strings.containsAny(topicString, TOPIC_WILDCARDS)) {

			throw new IllegalArgumentException(Messages.bind(Messages.VALIDATION_TOPIC_WILDCARDS_NOT_ALLOWED,
					TOPIC_WILDCARDS));
		}
	}

	/**
	 * Assert a boolean expression, throwing IllegalArgumentException if the test result is false.
	 * @param expression a boolean expression
	 * @param message the message to use if the assertion fails
	 */
	static void satisfy(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

}
