package org.eclipse.paho.mqttv5.client.internal;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used as a store for client information that should be preserved
 * for a single session. If the client connects with Clean start = false, and
 * then looses connection and reconnects, then this information will be
 * preserved and used in the subsequent connections. Currently there are no
 * plans to persist this information, however that could be added at a later
 * date.
 * 
 * Session variables that this class holds:
 * 
 * <ul>
 * <li>Next Subscription Identifier - Used when automatic subscription
 * Identifier assignment is enabled</li>
 * 
 * </ul>
 *
 */
public class MqttSession {
	private AtomicInteger nextSubscriptionIdentifier = new AtomicInteger(1);

	public int getNextSubscriptionIdentifier() {
		return nextSubscriptionIdentifier.getAndIncrement();
	}

	/**
	 * Clears the session and resets. This would be called when the connection has
	 * been lost and cleanSession = True.
	 */
	public void clearSession() {
		nextSubscriptionIdentifier.set(1);

	}

}
