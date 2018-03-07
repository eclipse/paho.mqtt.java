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
	private Integer receiveMaximum = 65535;
	private Integer maximumQoS = 2;
	private Boolean retainAvailable = true;
	private Long maximumPacketSize = -1L;
	private Integer outgoingTopicAliasMaximum = 0;
	private Integer incomingTopicAliasMax = 0;
	private Boolean wildcardSubscriptionsAvailable = true;
	private Boolean subscriptionIdentifiersAvailable = true;
	private Boolean sharedSubscriptionsAvailable = true;

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


	public Integer getReceiveMaximum() {
		return receiveMaximum;
	}

	public void setReceiveMaximum(Integer receiveMaximum) {
		this.receiveMaximum = receiveMaximum;
	}

	public Integer getMaximumQoS() {
		return maximumQoS;
	}

	public void setMaximumQoS(Integer maximumQoS) {
		this.maximumQoS = maximumQoS;
	}

	public Boolean isRetainAvailable() {
		return retainAvailable;
	}

	public void setRetainAvailable(Boolean retainAvailable) {
		this.retainAvailable = retainAvailable;
	}

	public Long getMaximumPacketSize() {
		return maximumPacketSize;
	}

	public void setMaximumPacketSize(Long maximumPacketSize) {
		this.maximumPacketSize = maximumPacketSize;
	}

	public Integer getOutgoingTopicAliasMaximum() {
		return outgoingTopicAliasMaximum;
	}

	public void setOutgoingTopicAliasMaximum(Integer topicAliasMaximum) {
		this.outgoingTopicAliasMaximum = topicAliasMaximum;
	}

	public Boolean isWildcardSubscriptionsAvailable() {
		return wildcardSubscriptionsAvailable;
	}

	public void setWildcardSubscriptionsAvailable(Boolean wildcardSubscriptionsAvailable) {
		this.wildcardSubscriptionsAvailable = wildcardSubscriptionsAvailable;
	}

	public Boolean isSubscriptionIdentifiersAvailable() {
		return subscriptionIdentifiersAvailable;
	}

	public void setSubscriptionIdentifiersAvailable(Boolean subscriptionIdentifiersAvailable) {
		this.subscriptionIdentifiersAvailable = subscriptionIdentifiersAvailable;
	}

	public Boolean isSharedSubscriptionsAvailable() {
		return sharedSubscriptionsAvailable;
	}

	public void setSharedSubscriptionsAvailable(Boolean sharedSubscriptionsAvailable) {
		this.sharedSubscriptionsAvailable = sharedSubscriptionsAvailable;
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


	public Integer getIncomingTopicAliasMax() {
		return incomingTopicAliasMax;
	}


	public void setIncomingTopicAliasMax(Integer incomingTopicAliasMax) {
		this.incomingTopicAliasMax = incomingTopicAliasMax;
	}

}
