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

/**
 * Event object helper
 * 
 * @author Bin Zhang
 */
public final class Events {

	/**
	 * Create a event with given selector without any data
	 * 
	 * @param selector
	 * @return event
	 */
	public static final <T> Event<T> of(Selector selector) {
		return of(selector, null);
	}

	/**
	 * Create a event with given selector and data
	 * 
	 * @param selector
	 * @param data
	 * @return event
	 */
	public static final <T> Event<T> of(Selector selector, T data) {
		return new Event<T>(selector, data);
	}

	private Events() {
		// prevented from constructing objects
	}
}
