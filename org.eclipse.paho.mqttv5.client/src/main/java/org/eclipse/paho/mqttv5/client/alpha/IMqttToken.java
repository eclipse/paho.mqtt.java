/**************************************************************************
 * Copyright (c) 2009, 2012 IBM Corp.
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
 *    Ian Craggs - MQTT 3.1.1 support
 */
package org.eclipse.paho.mqttv5.client.alpha;

import org.eclipse.paho.mqttv5.client.alpha.result.IMqttResult;
import org.osgi.util.promise.Promise;

/**
 * Provides a mechanism for tracking the completion of an asynchronous task.
 *
 * <p>When using the asynchronous/non-blocking MQTT programming interface all
 * methods/operations that take any time (and in particular those that involve
 * any network operation) return control to the caller immediately. The operation
 * then proceeds to run in the background so as not to block the invoking thread.
 * An IMqttToken is used to track the state of the operation. An application can use the
 * token to wait for an operation to complete. A token is passed to callbacks
 * once the operation completes and provides context linking it to the original
 * request. A token is associated with a single operation.</p>
 * <p>
 * An action is in progress until either:</p>
 * <ul>
 * <li>isComplete() returns true or</li>
 * <li>getException() is not null.</li>
 * </ul>
 * @param <T> TODO
 * @param <C> TODO
 */
public interface IMqttToken<T extends IMqttResult<C>, C> {
	
	public Promise<T> getPromise();

	/**
	 * Returns the MQTT client that is responsible for processing the asynchronous
	 * action
	 * @return the client
	 */
	public IMqttCommonClient getClient();


	/**
	 * Retrieve the context associated with an action.
	 * <p>Allows the ActionListener associated with an action to retrieve any context
	 * that was associated with the action when the action was invoked. If not
	 * context was provided null is returned. </p>

	 * @return Object context associated with an action or null if there is none.
	 */
	public Object getUserContext();

	/**
	 * Returns the message ID of the message that is associated with the token.
	 * A message id of zero will be returned for tokens associated with
	 * connect, disconnect and ping operations as there can only ever
	 * be one of these outstanding at a time. For other operations
	 * the MQTT message id flowed over the network.
	 * @return the message ID of the message that is associated with the token
	 */
	public int getMessageId();
}
