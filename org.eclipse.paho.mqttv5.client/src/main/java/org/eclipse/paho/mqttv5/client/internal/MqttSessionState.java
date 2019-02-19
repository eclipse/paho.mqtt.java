package org.eclipse.paho.mqttv5.client.internal;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used as a store for client information that should be preserved
 * for a single MQTT Session. If the client is disconnected and reconnects with
 * clean start = true, then this object will be reset to it's initial state.
 *
 * Connection variables that this class holds:
 *
 * <ul>
 * <li>Client ID</li>
 * <li>Next Subscription Identifier - The next subscription Identifier available
 * to use.</li>
 * </ul>
 */
public class MqttSessionState {

	// ******* Session Specific Properties and counters ******//
	private AtomicInteger nextSubscriptionIdentifier = new AtomicInteger(1);
	private String clientId;

	public void clearSessionState() {
		nextSubscriptionIdentifier.set(1);
	}

	public Integer getNextSubscriptionIdentifier() {
		return nextSubscriptionIdentifier.getAndIncrement();
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
}
