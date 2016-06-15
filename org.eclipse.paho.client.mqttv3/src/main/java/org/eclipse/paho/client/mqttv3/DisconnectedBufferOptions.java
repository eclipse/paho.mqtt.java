/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 *    James Sutton - Initial Contribution for Automatic Reconnect & Offline Buffering
 */
package org.eclipse.paho.client.mqttv3;

/**
 * Holds the set of options that govern the behaviour
 * of Offline (or Disconnected) buffering of messages
 */
public class DisconnectedBufferOptions {
	
	/**
	 * The default size of the disconnected buffer
	 */
	public static final int DISCONNECTED_BUFFER_SIZE_DEFAULT = 5000;
	
	public static final boolean DISCONNECTED_BUFFER_ENABLED_DEFAULT = false;
	
	public static final boolean PERSIST_DISCONNECTED_BUFFER_DEFAULT = false;
	
	public static final boolean DELETE_OLDEST_MESSAGES_DEFAULT = false;
	
	private int bufferSize = DISCONNECTED_BUFFER_SIZE_DEFAULT;
	private boolean bufferEnabled = DISCONNECTED_BUFFER_ENABLED_DEFAULT;
	private boolean persistBuffer = PERSIST_DISCONNECTED_BUFFER_DEFAULT;
	private boolean deleteOldestMessages = DELETE_OLDEST_MESSAGES_DEFAULT;
	
	/**
	 * Constructs a new <code>DisconnectedBufferOptions</code> object using the
	 * default values.
	 *
	 * The defaults are:
	 * <ul>
	 * <li>The disconnected buffer is disabled</li>
	 * <li>The buffer holds 5000 messages</li>
	 * <li>The buffer is not persisted</li>
	 * <li>Once the buffer is full, old messages are not deleted</li>
	 * </ul>
	 * More information about these values can be found in the setter methods.
	 */
	public DisconnectedBufferOptions() {
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		if (bufferSize < 1) {
            throw new IllegalArgumentException();
        }
		this.bufferSize = bufferSize;
	}

	public boolean isBufferEnabled() {
		return bufferEnabled;
	}

	public void setBufferEnabled(boolean bufferEnabled) {
		this.bufferEnabled = bufferEnabled;
	}

	public boolean isPersistBuffer() {
		return persistBuffer;
	}

	public void setPersistBuffer(boolean persistBuffer) {
		this.persistBuffer = persistBuffer;
	}

	public boolean isDeleteOldestMessages() {
		return deleteOldestMessages;
	}

	public void setDeleteOldestMessages(boolean deleteOldestMessages) {
		this.deleteOldestMessages = deleteOldestMessages;
	}
}
