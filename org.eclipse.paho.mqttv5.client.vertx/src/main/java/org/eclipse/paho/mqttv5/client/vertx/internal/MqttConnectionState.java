package org.eclipse.paho.mqttv5.client.vertx.internal;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used as a store for client information that should be preserved
 * for a single connection. 
 * Properties returned in subsequent connect packets will override existing properties
 * here as well.
 *
 * Connection variables that this class holds:
 *
 * <ul>
 * <li>Receive Maximum</li>
 * <li>Maximum QoS</li>
 * <li>Retain Available</li>
 * <li>Maximum Packet Size</li>
 * <li>Outgoing Topic Alias Maximum</li>
 * <li>Incoming Topic Alias Maximum</li>
 * <li>Wildcard Subscriptions Available</li>
 * <li>Subscription Identifiers Available</li>
 * <li>Shared Subscriptions Available</li>
 * <li>Send Reason Messages</li>
 * </ul>
 */
public class MqttConnectionState {

	// ******* Connection properties ******//
	private Integer receiveMaximum = 65535;
	private Integer maximumQoS = 2;
	private Boolean retainAvailable = true;
	private Long outgoingMaximumPacketSize = null;
	private Long incomingMaximumPacketSize = null;
	private Integer outgoingTopicAliasMaximum = 0;
	private Integer incomingTopicAliasMax = 0;
	private Boolean wildcardSubscriptionsAvailable = true;
	private Boolean subscriptionIdentifiersAvailable = true;
	private Boolean sharedSubscriptionsAvailable = true;
	private boolean sendReasonMessages = false;
	private long keepAlive = 60;

	// ******* Counters ******//
	private AtomicInteger nextOutgoingTopicAlias = new AtomicInteger(1);
	
	// Topic Alias Maps
	private Hashtable<String, Integer> outgoingTopicAliases;
	private Hashtable<Integer, String> incomingTopicAliases;


	/**
	 * Clears the session and resets. This would be called when the connection has
	 * been lost and cleanStart = True.
	 */
	public void clearConnectionState() {
		nextOutgoingTopicAlias.set(1);
	}


	public Integer getReceiveMaximum() {
		if (receiveMaximum == null) {
			return 65535;
		}
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

	public Long getOutgoingMaximumPacketSize() {
		return outgoingMaximumPacketSize;
	}

	public void setOutgoingMaximumPacketSize(Long maximumPacketSize) {
		this.outgoingMaximumPacketSize = maximumPacketSize;
	}
	
	public Long getIncomingMaximumPacketSize() {
		return incomingMaximumPacketSize;
	}


	public void setIncomingMaximumPacketSize(Long incomingMaximumPacketSize) {
		this.incomingMaximumPacketSize = incomingMaximumPacketSize;
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
	
	public Integer getNextOutgoingTopicAlias() {
		return nextOutgoingTopicAlias.getAndIncrement();
	}


	public Integer getIncomingTopicAliasMax() {
		return incomingTopicAliasMax;
	}


	public void setIncomingTopicAliasMax(Integer incomingTopicAliasMax) {
		this.incomingTopicAliasMax = incomingTopicAliasMax;
	}


	public boolean isSendReasonMessages() {
		return sendReasonMessages;
	}


	public void setSendReasonMessages(boolean enableReasonMessages) {
		this.sendReasonMessages = enableReasonMessages;
	}


	public long getKeepAlive() {
		return keepAlive;
	}


	public void setKeepAliveSeconds(long keepAlive) {
		this.keepAlive = keepAlive * 1000;
	}


	
}
