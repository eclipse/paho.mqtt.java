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
 * Properties returned in subsequent connect packets will override existing properties
 * here as well.
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
	
	// ******* Connection properties ******//
	private int receiveMaximum = 65535;
	private int maximumQoS = 2;
	private boolean retainAvailable = true;
	private int maximumPacketSize = -1;
	private int topicAliasMaximum = 0;
	private boolean wildcardSubscriptionsAvailable = true;
	private boolean subscriptionIdentifiersAvailable = true;
	private boolean sharedSubscriptionsAvailable = true;
	
	// ******* Session Specific Properties and counters ******//
	private AtomicInteger nextSubscriptionIdentifier = new AtomicInteger(1);
	private String clientId;

	

	/**
	 * Clears the session and resets. This would be called when the connection has
	 * been lost and cleanSession = True.
	 */
	public void clearSession() {
		nextSubscriptionIdentifier.set(1);
	}
	
	
	public int getReceiveMaximum() {
		return receiveMaximum;
	}

	public void setReceiveMaximum(int receiveMaximum) {
		this.receiveMaximum = receiveMaximum;
	}

	public int getMaximumQoS() {
		return maximumQoS;
	}

	public void setMaximumQoS(int maximumQoS) {
		this.maximumQoS = maximumQoS;
	}

	public boolean isRetainAvailable() {
		return retainAvailable;
	}

	public void setRetainAvailable(boolean retainAvailable) {
		this.retainAvailable = retainAvailable;
	}

	public int getMaximumPacketSize() {
		return maximumPacketSize;
	}

	public void setMaximumPacketSize(int maximumPacketSize) {
		this.maximumPacketSize = maximumPacketSize;
	}

	public int getTopicAliasMaximum() {
		return topicAliasMaximum;
	}

	public void setTopicAliasMaximum(int topicAliasMaximum) {
		this.topicAliasMaximum = topicAliasMaximum;
	}

	public boolean isWildcardSubscriptionsAvailable() {
		return wildcardSubscriptionsAvailable;
	}

	public void setWildcardSubscriptionsAvailable(boolean wildcardSubscriptionsAvailable) {
		this.wildcardSubscriptionsAvailable = wildcardSubscriptionsAvailable;
	}

	public boolean isSubscriptionIdentifiersAvailable() {
		return subscriptionIdentifiersAvailable;
	}

	public void setSubscriptionIdentifiersAvailable(boolean subscriptionIdentifiersAvailable) {
		this.subscriptionIdentifiersAvailable = subscriptionIdentifiersAvailable;
	}

	public boolean isSharedSubscriptionsAvailable() {
		return sharedSubscriptionsAvailable;
	}

	public void setSharedSubscriptionsAvailable(boolean sharedSubscriptionsAvailable) {
		this.sharedSubscriptionsAvailable = sharedSubscriptionsAvailable;
	}
	
	public int getNextSubscriptionIdentifier() {
		return nextSubscriptionIdentifier.getAndIncrement();
	}


	public String getClientId() {
		return clientId;
	}


	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

}
