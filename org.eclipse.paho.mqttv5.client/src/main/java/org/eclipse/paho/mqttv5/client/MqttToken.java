/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
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
 * Contributions:
 *   Ian Craggs - MQTT 3.1.1 support
 */

package org.eclipse.paho.mqttv5.client;

import java.util.ArrayList;

import org.eclipse.paho.mqttv5.client.internal.Token;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttAuth;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttPubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttPubComp;
import org.eclipse.paho.mqttv5.common.packet.MqttPubRec;
import org.eclipse.paho.mqttv5.common.packet.MqttSubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttUnsubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

/**
 * Provides a mechanism for tracking the completion of an asynchronous action.
 * <p>
 * A token that implements the ImqttToken interface is returned from all
 * non-blocking method with the exception of publish.
 * </p>
 * 
 * @see IMqttToken
 */

public class MqttToken implements IMqttToken {
	/**
	 * A reference to the the class that provides most of the implementation of the
	 * MqttToken. MQTT application programs must not use the internal class.
	 */
	public Token internalTok = null;

	public MqttToken() {
	}

	public MqttToken(String logContext) {
		internalTok = new Token(logContext);
	}

	public MqttException getException() {
		return internalTok.getException();
	}

	public boolean isComplete() {
		return internalTok.isComplete();
	}

	public void setActionCallback(MqttActionListener listener) {
		internalTok.setActionCallback(listener);

	}
  
	public MqttActionListener getActionCallback() {
		return internalTok.getActionCallback();
	}

	public void waitForCompletion() throws MqttException {
		internalTok.waitForCompletion(-1);
	}

	public void waitForCompletion(long timeout) throws MqttException {
		internalTok.waitForCompletion(timeout);
	}

	public MqttClientInterface getClient() {
		return internalTok.getClient();
	}

	public String[] getTopics() {
		return internalTok.getTopics();
	}

	public Object getUserContext() {
		return internalTok.getUserContext();
	}

	public void setUserContext(Object userContext) {
		internalTok.setUserContext(userContext);
	}

	public int getMessageId() {
		return internalTok.getMessageID();
	}

	public int[] getGrantedQos() {
		return internalTok.getGrantedQos();
	}

	public boolean getSessionPresent() {
		return internalTok.getSessionPresent();
	}

	public MqttWireMessage getResponse() {
		return internalTok.getResponse();
	}

	public String getAssignedClientIdentifier() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getAssignedClientIdentifier();
		}
		return null;
	}

	public int getServerKeepAlive() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getServerKeepAlive();
		}
		return -1;
	}

	public String getAuthMethod() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getAuthMethod();
		} else if (internalTok.getWireMessage() instanceof MqttAuth) {
			return ((MqttAuth) internalTok.getWireMessage()).getAuthMethod();
		}
		return null;
	}

	public byte[] getAuthData() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getAuthData();
		} else if (internalTok.getWireMessage() instanceof MqttAuth) {
			return ((MqttAuth) internalTok.getWireMessage()).getAuthData();
		}
		return null;
	}

	public String getResponseInformation() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getResponseInfo();
		}
		return null;
	}

	public String getServerReference() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getServerReference();
		}
		return null;
	}

	public String getReasonString() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getReasonString();
		} else if (internalTok.getWireMessage() instanceof MqttPubAck) {
			return ((MqttPubAck) internalTok.getWireMessage()).getReasonString();
		} else if (internalTok.getWireMessage() instanceof MqttPubRec) {
			return ((MqttPubRec) internalTok.getWireMessage()).getReasonString();
		} else if (internalTok.getWireMessage() instanceof MqttPubComp) {
			return ((MqttPubComp) internalTok.getWireMessage()).getReasonString();
		} else if (internalTok.getWireMessage() instanceof MqttSubAck) {
			return ((MqttSubAck) internalTok.getWireMessage()).getReasonString();
		} else if (internalTok.getWireMessage() instanceof MqttUnsubAck) {
			return ((MqttUnsubAck) internalTok.getWireMessage()).getReasonString();
		} else if (internalTok.getWireMessage() instanceof MqttAuth) {
			return ((MqttAuth) internalTok.getWireMessage()).getReasonString();
		}
		return null;
	}

	public int getRecieveMaximum() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getReceiveMaximum();
		}
		return 65535;
	}

	public int getTopicAliasMaximum() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getTopicAliasMaximum();
		}
		return 0;
	}

	public int getMaximumQoS() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getMaximumQoS();
		}
		return 2;
	}

	public boolean isRetainAvailable() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).isRetainAvailableAdvertisement();
		}
		return true;
	}

	public ArrayList<UserProperty> getUserDefinedProperties() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getUserDefinedProperties();
		} else if (internalTok.getWireMessage() instanceof MqttPubAck) {
			return ((MqttPubAck) internalTok.getWireMessage()).getUserDefinedProperties();
		} else if (internalTok.getWireMessage() instanceof MqttPubRec) {
			return ((MqttPubRec) internalTok.getWireMessage()).getUserDefinedProperties();
		} else if (internalTok.getWireMessage() instanceof MqttPubComp) {
			return ((MqttPubComp) internalTok.getWireMessage()).getUserDefinedProperties();
		} else if (internalTok.getWireMessage() instanceof MqttSubAck) {
			return ((MqttSubAck) internalTok.getWireMessage()).getUserDefinedProperties();
		} else if (internalTok.getWireMessage() instanceof MqttUnsubAck) {
			return ((MqttUnsubAck) internalTok.getWireMessage()).getUserDefinedProperties();
		} else if (internalTok.getWireMessage() instanceof MqttAuth) {
			return ((MqttAuth) internalTok.getWireMessage()).getUserDefinedProperties();
		}
		return new ArrayList<UserProperty>();
	}

	public int getMaximumPacketSize() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).getMaximumPacketSize();
		}
		return -1;
	}

	public boolean isWildcardSubscriptionAvailable() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).isWildcardSubscriptionsAvailable();
		}
		return true;
	}

	public boolean isSubscriptionIdentifiersAvailable() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).isSubscriptionIdentifiersAvailable();
		}
		return true;
	}

	public boolean isSharedSubscriptionAvailable() {
		if (internalTok.getWireMessage() instanceof MqttConnAck) {
			return ((MqttConnAck) internalTok.getWireMessage()).isSharedSubscriptionAvailable();
		}
		return true;
	}

}
