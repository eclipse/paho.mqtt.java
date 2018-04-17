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
 * EventService to decouple the component using event exchange
 * 
 * @author Bin Zhang
 */
public interface IEventService {

	/**
	 * Register a event handler that only receive event when matches the selector
	 * 
	 * @param selector the event selector
	 * @param handler the event handler
	 * @return registration unregister it if the handler no longer needed
	 */
	<T> IRegistration registerHandler(Selector selector, IEventHandler<T> handler);

	/**
	 * Send a event asynchronously
	 * 
	 * @param event
	 */
	<T> void sendEvent(Event<T> event);
}
