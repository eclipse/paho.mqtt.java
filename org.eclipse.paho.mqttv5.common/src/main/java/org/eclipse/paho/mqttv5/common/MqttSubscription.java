/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corp.
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
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common;

public class MqttSubscription {

	private boolean mutable = true;
	private String topic;
	private int qos = 1;
	private boolean noLocal = false;
	private boolean retainAsPublished = false;
	private int retainHandling = 0;
	private int messageId;
	
	
	
	/**
	 * Constructs a subscription with the specified topic with
	 * all other values set to defaults.
	 * 
	 * The defaults are:
	 * <ul>
	 * 	<li>Subscription QoS is set to 1</li>
	 * 	<li>Messages published to this topic by the same client will also be received.</li>
	 * 	<li>Messages received by this subscription will keep the retain flag (if it is set).</li>
	 * 	<li>Retained messages on this topic will be delivered once the subscription has been made.</li>
	 * </ul>
	 * @param topic The Topic
	 */
	public MqttSubscription(String topic){
		setTopic(topic);
	}
	
	public MqttSubscription(String topic, int qos) {
		setTopic(topic);
		setQos(qos);
	}
	
	/**
	 * Utility method to validate the supplied QoS value.
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @param qos The QoS level to validate.
	 */
	public static void validateQos(int qos) {
		if ((qos < 0) || (qos > 2)) {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Utility method to validate the supplied Retain handling value.
	 * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
	 * @param retainHandling the retain value to validate.
	 */
	public static void validateRetainHandling(int retainHandling) {
		if ((retainHandling < 0) || (retainHandling > 2)) {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Sets the mutability of this object (whether or not its values can be
	 * changed.
	 * @param mutable <code>true</code> if the values can be changed,
	 * <code>false</code> to prevent them from being changed.
	 */
	protected void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	protected void checkMutable() throws IllegalStateException {
		if (!mutable) {
			throw new IllegalStateException();
		}
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		checkMutable();
		if(topic == null){
			throw new NullPointerException();
		}
		this.topic = topic;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		checkMutable();
		validateQos(qos);
		this.qos = qos;
	}

	public boolean isNoLocal() {
		return noLocal;
	}

	public void setNoLocal(boolean noLocal) {
		checkMutable();
		this.noLocal = noLocal;
	}

	public boolean isRetainAsPublished() {
		return retainAsPublished;
	}

	public void setRetainAsPublished(boolean retainAsPublished) {
		checkMutable();
		this.retainAsPublished = retainAsPublished;
	}

	public int getRetainHandling() {
		return retainHandling;
	}

	public void setRetainHandling(int retainHandling) {
		checkMutable();
		validateRetainHandling(retainHandling);
		this.retainHandling = retainHandling;
	}

	@Override
	public String toString() {
		return "MqttSubscription [mutable=" + mutable + ", topic=" + topic + ", qos=" + qos + ", noLocal=" + noLocal
				+ ", retainAsPublished=" + retainAsPublished + ", retainHandling=" + retainHandling + "]";
	}
	
	/**
	 * This is only to be used internally to provide the MQTT id of a message
	 * received from the server.  Has no effect when publishing messages.
	 * @param messageId The Message Identifier
	 */
	public void setId(int messageId) {
		this.messageId = messageId;
	}

	/**
	 * Returns the MQTT id of the message.  This is only applicable to messages
	 * received from the server.
	 * @return the MQTT id of the message
	 */
	public int getId() {
		return this.messageId;
	}
	
	
}
