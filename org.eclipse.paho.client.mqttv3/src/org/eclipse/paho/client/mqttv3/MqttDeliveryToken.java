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

/**
 * Provides a mechanism for tracking the delivery of messages.
 * 
 * Calls to {@link MqttTopic#publish(MqttMessage)}
 * return after the message has been sent, but do not wait for acknowledgements
 * to be received.  This allows the 
 * client to have multiple messages in-flight at one time.
 * 
 * In order to track the delivery of a message, those methods
 * return an instance of this class which can be used in one of two ways:
 * <ul>
 * <li>calling {@link #waitForCompletion()} to block the current thread 
 *    until delivery has completed</li>
 * <li>matching this object to the one passed to the delivery callback methods
 *    of {@link MqttCallback}.</li>
 * </ul>
 */
public interface MqttDeliveryToken {
	
	/**
	 * Blocks the current thread until the message this is the token
	 * for completes delivery.
	 * @throws MqttException if there was a problem completing delivery of the message.
	 */
	public void waitForCompletion() throws MqttException, MqttSecurityException;
	
	/**
	 * Blocks the current thread until the message this is the token
	 * for completes delivery.
	 * This will only block for specified timeout period. If the delivery has not
	 * completed before the timeout occurs, this method will return - the 
	 * {@link #isComplete()} method can be used to determine if the delivery is
	 * complete, or if the wait has timed out.
	 * @param timeout the maximum amount of time to wait for, in milliseconds.
	 * @throws MqttException if there was a problem completing delivery of the message
	 */
	public void waitForCompletion(long timeout) throws MqttException, MqttSecurityException;
	
	/**
	 * Returns whether or not the delivery has finished.  Note that a token will
	 * be marked as complete even if the delivery failed.
	 */
	public boolean isComplete();
	
	/**
	 * Returns the message associated with this token, or <code>null</code> if the message has
	 * already been successfully sent.
	 * @return the message associated with this token
	 * @throws MqttException if there was a problem completing retrieving the message
	 */
	public MqttMessage getMessage() throws MqttException;
}
