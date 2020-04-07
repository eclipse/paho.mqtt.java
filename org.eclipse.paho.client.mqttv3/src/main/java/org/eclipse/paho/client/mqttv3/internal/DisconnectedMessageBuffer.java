/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    James Sutton - Initial Contribution for Automatic Reconnect & Offline Buffering
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.util.ArrayList;

import org.eclipse.paho.client.mqttv3.BufferedMessage;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

public class DisconnectedMessageBuffer implements Runnable {

	private final String CLASS_NAME = DisconnectedMessageBuffer.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);
	private DisconnectedBufferOptions bufferOpts;
	private ArrayList<BufferedMessage> buffer;
	private final Object bufLock = new Object(); // Used to synchronise the buffer
	private IDisconnectedBufferCallback callback;
        private IDiscardedBufferMessageCallback messageDiscardedCallBack;

	public DisconnectedMessageBuffer(DisconnectedBufferOptions options) {
		this.bufferOpts = options;
		buffer = new ArrayList<BufferedMessage>();
	}

	/**
	 * This will add a new message to the offline buffer, if the buffer is full and
	 * deleteOldestMessages is enabled then the 0th item in the buffer will be
	 * deleted and the new message will be added. If it is not enabled then an
	 * MqttException will be thrown.
	 * 
	 * @param message
	 *            the {@link MqttWireMessage} that will be buffered
	 * @param token
	 *            the associated {@link MqttToken}
	 * @throws MqttException
	 *             if the Buffer is full
	 */
	public void putMessage(MqttWireMessage message, MqttToken token) throws MqttException {
		if (token != null) {
			message.setToken(token);
			token.internalTok.setMessageID(message.getMessageId());
		}
		
		BufferedMessage bufferedMessage = new BufferedMessage(message, token);
		synchronized (bufLock) {
			if (buffer.size() < bufferOpts.getBufferSize()) {
				buffer.add(bufferedMessage);
			} else if (bufferOpts.isDeleteOldestMessages() == true) {
				if(messageDiscardedCallBack != null){
					BufferedMessage discardedMessage = (BufferedMessage) buffer.get(0);
					messageDiscardedCallBack.messageDiscarded(discardedMessage.getMessage());
				}
				buffer.remove(0);
				buffer.add(bufferedMessage);
			} else {
				throw new MqttException(MqttException.REASON_CODE_DISCONNECTED_BUFFER_FULL);
			}
		}
	}

	/**
	 * Retrieves a message from the buffer at the given index.
	 * 
	 * @param messageIndex
	 *            the index of the message to be retrieved in the buffer
	 * @return the {@link BufferedMessage}
	 */
	public BufferedMessage getMessage(int messageIndex) {
		synchronized (bufLock) {
			return ((BufferedMessage) buffer.get(messageIndex));
		}
	}

	/**
	 * Removes a message from the buffer
	 * 
	 * @param messageIndex
	 *            the index of the message to be deleted in the buffer
	 */
	public void deleteMessage(int messageIndex) {
		synchronized (bufLock) {
			buffer.remove(messageIndex);
		}
	}

	/**
	 * Returns the number of messages currently in the buffer
	 * 
	 * @return The count of messages in the buffer
	 */
	public int getMessageCount() {
		synchronized (bufLock) {
			return buffer.size();
		}
	}

	private int mycount = 0;
	/**
	 * Flushes the buffer of messages into an open connection
	 */
	public void run() {
		final String methodName = "run";
		// @TRACE 516=Restoring all buffered messages.
		log.fine(CLASS_NAME, methodName, "516");
		while (getMessageCount() > 0) {
			try {
				BufferedMessage bufferedMessage = getMessage(0);
				callback.publishBufferedMessage(bufferedMessage);
				// Publish was successful, remove message from buffer.
				deleteMessage(0);
			} catch (MqttException ex) {
				if (ex.getReasonCode() == MqttException.REASON_CODE_MAX_INFLIGHT) {
					// If we get the max_inflight condition, try again after a short
					// interval to allow more messages to be completely sent.
					try { Thread.sleep(100); } catch (Exception e) {}
				} else {
					// Error occurred attempting to publish buffered message likely because the
					// client is not connected
					// @TRACE 519=Error occurred attempting to publish buffered message due to disconnect. Exception: {0}:{1}.
					log.severe(CLASS_NAME, methodName, "519", new Object[] { ex.getReasonCode(), ex.getMessage() });
					break;
				}
			}

		}
	}

	public void setPublishCallback(IDisconnectedBufferCallback callback) {
		this.callback = callback;
	}

	public boolean isPersistBuffer() {
		return bufferOpts.isPersistBuffer();
	}

        public void setMessageDiscardedCallBack(IDiscardedBufferMessageCallback callback) {
		this.messageDiscardedCallBack = callback;
	}

}
