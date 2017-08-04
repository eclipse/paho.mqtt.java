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

import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.util.Strings;

/**
 * Represents a topic destination, used for publish/subscribe messaging.
 */
public class MqttTopic {

	/**
	 * The forward slash (/) is used to separate each level within a topic tree
	 * and provide a hierarchical structure to the topic space. The use of the
	 * topic level separator is significant when the two wildcard characters are
	 * encountered in topics specified by subscribers.
	 */
	public static final String TOPIC_LEVEL_SEPARATOR = "/";

	/**
	 * Multi-level wildcard The number sign (#) is a wildcard character that
	 * matches any number of levels within a topic.
	 */
	public static final String MULTI_LEVEL_WILDCARD = "#";

	/**
	 * Single-level wildcard The plus sign (+) is a wildcard character that
	 * matches only one topic level.
	 */
	public static final String SINGLE_LEVEL_WILDCARD = "+";

	/**
	 * Multi-level wildcard pattern(/#)
	 */
	public static final String MULTI_LEVEL_WILDCARD_PATTERN = TOPIC_LEVEL_SEPARATOR + MULTI_LEVEL_WILDCARD;

	/**
	 * Topic wildcards (#+)
	 */
	public static final String TOPIC_WILDCARDS = MULTI_LEVEL_WILDCARD + SINGLE_LEVEL_WILDCARD;

	//topic name and topic filter length range defined in the spec
	private static final int MIN_TOPIC_LEN = 1;
	private static final int MAX_TOPIC_LEN = 65535;
	private static final char NUL = '\u0000';

	private ClientComms comms;
	private String name;

	/**
	 * @param name The Name of the topic
	 * @param comms The {@link ClientComms}
	 */
	public MqttTopic(String name, ClientComms comms) {
		this.comms = comms;
		this.name = name;
	}

	/**
	 * Publishes a message on the topic.  This is a convenience method, which will
	 * create a new {@link MqttMessage} object with a byte array payload and the
	 * specified QoS, and then publish it.  All other values in the
	 * message will be set to the defaults.

	 * @param payload the byte array to use as the payload
	 * @param qos the Quality of Service.  Valid values are 0, 1 or 2.
	 * @param retained whether or not this message should be retained by the server.
	 * @return {@link MqttDeliveryToken}
	 * @throws MqttException If an error occurs publishing the message
	 * @throws MqttPersistenceException If an error occurs persisting the message
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @see #publish(MqttMessage)
	 * @see MqttMessage#setQos(int)
	 * @see MqttMessage#setRetained(boolean)
	 */
	public MqttDeliveryToken publish(byte[] payload, int qos, boolean retained) throws MqttException, MqttPersistenceException {
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		return this.publish(message);
	}

	/**
	 * Publishes the specified message to this topic, but does not wait for delivery
	 * of the message to complete. The returned {@link MqttDeliveryToken token} can be used
	 * to track the delivery status of the message.  Once this method has
	 * returned cleanly, the message has been accepted for publication by the
	 * client. Message delivery will be completed in the background when a connection
	 * is available.
	 *
	 * @param message the message to publish
	 * @return an MqttDeliveryToken for tracking the delivery of the message
	 * @throws MqttException if an error occurs publishing the message
	 * @throws MqttPersistenceException  if an error occurs persisting the message
	 */
	public MqttDeliveryToken publish(MqttMessage message) throws MqttException, MqttPersistenceException {
		MqttDeliveryToken token = new MqttDeliveryToken(comms.getClient().getClientId());
		token.setMessage(message);
		comms.sendNoWait(createPublish(message), token);
		token.internalTok.waitUntilSent();
		return token;
	}

	/**
	 * Returns the name of the queue or topic.
	 *
	 * @return the name of this destination.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Create a PUBLISH packet from the specified message.
	 */
	private MqttPublish createPublish(MqttMessage message) {
		return new MqttPublish(this.getName(), message);
	}

	/**
	 * Returns a string representation of this topic.
	 * @return a string representation of this topic.
	 */
	public String toString() {
		return getName();
	}

	/**
	 * Validate the topic name or topic filter
	 *
	 * @param topicString topic name or filter
	 * @param wildcardAllowed true if validate topic filter, false otherwise
	 * @throws IllegalArgumentException if the topic is invalid
	 */
	public static void validate(String topicString, boolean wildcardAllowed)
			throws  IllegalArgumentException{
		int topicLen = 0;
		try {
			topicLen = topicString.getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e.getMessage());
		}

		// Spec: length check
		// - All Topic Names and Topic Filters MUST be at least one character
		// long
		// - Topic Names and Topic Filters are UTF-8 encoded strings, they MUST
		// NOT encode to more than 65535 bytes
		if (topicLen < MIN_TOPIC_LEN || topicLen > MAX_TOPIC_LEN) {
			throw new IllegalArgumentException(String.format("Invalid topic length, should be in range[%d, %d]!", 
					new Object[] { new Integer(MIN_TOPIC_LEN), new Integer(MAX_TOPIC_LEN) }));
		}

		// *******************************************************************************
		// 1) This is a topic filter string that can contain wildcard characters
		// *******************************************************************************
		if (wildcardAllowed) {
			// Only # or +
			if (Strings.equalsAny(topicString, new String[] { MULTI_LEVEL_WILDCARD, SINGLE_LEVEL_WILDCARD })) {
				return;
			}

			// 1) Check multi-level wildcard
			// Rule:
			// The multi-level wildcard can be specified only on its own or next
			// to the topic level separator character.

			// - Can only contains one multi-level wildcard character
			// - The multi-level wildcard must be the last character used within
			// the topic tree
			if (Strings.countMatches(topicString, MULTI_LEVEL_WILDCARD) > 1
					|| (topicString.contains(MULTI_LEVEL_WILDCARD) && !topicString
							.endsWith(MULTI_LEVEL_WILDCARD_PATTERN))) {
				throw new IllegalArgumentException(
						"Invalid usage of multi-level wildcard in topic string: "
								+ topicString);
			}

			// 2) Check single-level wildcard
			// Rule:
			// The single-level wildcard can be used at any level in the topic
			// tree, and in conjunction with the
			// multilevel wildcard. It must be used next to the topic level
			// separator, except when it is specified on
			// its own.
			validateSingleLevelWildcard(topicString);

			return;
		}

		// *******************************************************************************
		// 2) This is a topic name string that MUST NOT contains any wildcard characters
		// *******************************************************************************
		if (Strings.containsAny(topicString, TOPIC_WILDCARDS)) {
			throw new IllegalArgumentException(
					"The topic name MUST NOT contain any wildcard characters (#+)");
		}
	}

    private static void validateSingleLevelWildcard(String topicString) {
        char singleLevelWildcardChar = SINGLE_LEVEL_WILDCARD.charAt(0);
        char topicLevelSeparatorChar = TOPIC_LEVEL_SEPARATOR.charAt(0);

        char[] chars = topicString.toCharArray();
        int length = chars.length;
        char prev = NUL, next = NUL;
        for (int i = 0; i < length; i++) {
            prev = (i - 1 >= 0) ? chars[i - 1] : NUL;
            next = (i + 1 < length) ? chars[i + 1] : NUL;

            if (chars[i] == singleLevelWildcardChar) {
                // prev and next can be only '/' or none
                if (prev != topicLevelSeparatorChar && prev != NUL || next != topicLevelSeparatorChar && next != NUL) {
                    throw new IllegalArgumentException(String.format( 
                            "Invalid usage of single-level wildcard in topic string '%s'!",
                            new Object[] { topicString }));
                   
                }
            }
        }
    }

	/**
	 * Check the supplied topic name and filter match
	 *
	 * @param topicFilter topic filter: wildcards allowed
	 * @param topicName topic name: wildcards not allowed
	 * @return true if the topic matches the filter
	 * @throws IllegalArgumentException if the topic name or filter is invalid
	 */
	public static boolean isMatched(String topicFilter, String topicName)
	                    throws IllegalArgumentException {
	    int curn = 0,
	        curf = 0;
	    int curn_end = topicName.length();
	    int curf_end = topicFilter.length();

	    MqttTopic.validate(topicFilter, true);
	    MqttTopic.validate(topicName, false);

	    if (topicFilter.equals(topicName)) {
	    	return true;
	    }

	    while (curf < curf_end && curn < curn_end)
	    {
	        if (topicName.charAt(curn) == '/' && topicFilter.charAt(curf) != '/')
	            break;
	        if (topicFilter.charAt(curf) != '+' && topicFilter.charAt(curf) != '#' &&
	        		topicFilter.charAt(curf) != topicName.charAt(curn))
	            break;
	        if (topicFilter.charAt(curf) == '+')
	        {   // skip until we meet the next separator, or end of string
	            int nextpos = curn + 1;
	            while (nextpos < curn_end && topicName.charAt(nextpos) != '/')
	                nextpos = ++curn + 1;
	        }
	        else if (topicFilter.charAt(curf) == '#')
	            curn = curn_end - 1;    // skip until end of string
	        curf++;
	        curn++;
	    };

	    return (curn == curn_end) && (curf == curf_end);
	}

}
