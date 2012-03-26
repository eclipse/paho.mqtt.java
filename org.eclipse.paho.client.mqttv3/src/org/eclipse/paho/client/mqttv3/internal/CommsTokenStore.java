/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnack;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingReq;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingResp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRel;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;


/**
 * Provides a "token" based system for handling the
 * threading.  When a message is sent, a token is derived from that message
 * and saved using the {@link #saveToken(MqttWireMessage)} method.  The sending
 * method then calls {@link Object#wait()} on that token.  The {@link CommsReceiver}
 * class, on another thread, reads responses back from the network.  It uses 
 * the response to find the relevant token, which it can then 
 * {@link Object#notify()}.
 */
public class CommsTokenStore {
	/** Maps message-specific data (usually message IDs) to tokens */
	private Hashtable tokens;
	private MqttDeliveryTokenImpl pingToken;
	private MqttDeliveryTokenImpl connectToken;
	private MqttDeliveryTokenImpl disconnectToken;
	
	private MqttException noMoreResponsesException = null;
	private boolean noMoreResponses = false;
	
	private Trace trace;

	public CommsTokenStore(Trace trace) {
		this.tokens = new Hashtable();
		this.trace = trace;
		pingToken = new MqttDeliveryTokenImpl(trace);
		connectToken = new MqttDeliveryTokenImpl(trace);
		disconnectToken = new MqttDeliveryTokenImpl(trace);
	}

	public MqttDeliveryTokenImpl getToken(MqttWireMessage message) {
		Object key;
		if (message instanceof MqttAck) {
			return getTokenForAck((MqttAck)message);
		}
		else if (message instanceof MqttPingReq) {
			key = pingToken;
		}
		else if (message instanceof MqttConnect) {
			key = connectToken;
		}
		else if (message instanceof MqttDisconnect) {
			key = disconnectToken;
		}
		else {
			key = new Integer(message.getMessageId());
		}
		return (MqttDeliveryTokenImpl)tokens.get(key);
	}
	
	private MqttDeliveryTokenImpl getTokenForAck(MqttWireMessage message) {
		MqttDeliveryTokenImpl token;
		if (message instanceof MqttPingResp) {
			token = pingToken;
		}
		else if (message instanceof MqttConnack) {
			token = connectToken;
		}
		else {
			token = (MqttDeliveryTokenImpl)tokens.get(new Integer(message.getMessageId()));
		}
		return token;
	}
	
	public MqttDeliveryTokenImpl removeToken(MqttWireMessage message) {
		Object key;
		if (message instanceof MqttConnack) {
			key = connectToken;
		} else if (message instanceof MqttDisconnect) {
			key = disconnectToken;
		} else {
			key = new Integer(message.getMessageId());
		}

		if (trace.isOn()) {
			//@TRACE 301=removeToken message={0} key={1}
			trace.trace(Trace.FINE,301,new Object[]{message,key});
		}

		return (MqttDeliveryTokenImpl) tokens.remove(key);
	}
	
	/**
	 * Restores a token after a client restart.  This method could be called
	 * for a SEND of CONFIRM, but either way, the original SEND is what's 
	 * needed to re-build the token.
	 */
	protected MqttDeliveryTokenImpl restoreToken(MqttPublish message) {
		MqttDeliveryTokenImpl token;
		Object key = new Integer(message.getMessageId());
		if (this.tokens.containsKey(key)) {
			token = (MqttDeliveryTokenImpl)this.tokens.get(key);
			if (trace.isOn()) {
				//@TRACE 302=restoreToken existing message={0} key={1} token={2}
				trace.trace(Trace.FINE,302,new Object[]{message,key,token});
			}
		} else {
			token = new MqttDeliveryTokenImpl(trace, message);
			this.tokens.put(key, token);
			if (trace.isOn()) {
				//@TRACE 303=restoreToken creating new message={0} key={1} token={2}
				trace.trace(Trace.FINE,303,new Object[]{message,key,token});
			}
		}
		return token;
	}
	
	protected MqttDeliveryTokenImpl saveToken(MqttWireMessage message) {
		Object key;
		MqttDeliveryTokenImpl token;
		if (message instanceof MqttPingReq) {
			token = pingToken;
			key = token;
		}
		else if (message instanceof MqttConnect) {
			noMoreResponses = false;
			noMoreResponsesException = null;
			connectToken = new MqttDeliveryTokenImpl(trace);
			token = connectToken;
			key = token;
		}
		else if (message instanceof MqttDisconnect) {
			disconnectToken = new MqttDeliveryTokenImpl(trace);
			token = disconnectToken;
			key = token;
		}
		else if (message instanceof MqttPubRel) {
			// TODO: This could be brittle, as the key might not always be a message ID
			key = new Integer(message.getMessageId());
			token = getToken(message);
		}
		else if (message instanceof MqttPublish) {
			key = new Integer(message.getMessageId());
			token = new MqttDeliveryTokenImpl(trace, (MqttPublish) message);
		} 
		else {
			key = new Integer(message.getMessageId());
			token = new MqttDeliveryTokenImpl(trace);
		}
		if (trace.isOn()) {
			//@TRACE 300=saveToken message={0} key={1} token={2}
			trace.trace(Trace.FINE,300,new Object[]{message,key,token.toString()});
		}
		this.tokens.put(key, token);
		if (noMoreResponses) {
			token.notifyException(noMoreResponsesException);
		}
		return token;
	}
	
	/**
	 * Called by the Receiver's thread to indicate the the specified
	 * response has been received.  The MQTTAck object contains the 
	 * details of what is being responded to.
	 */
	protected void responseReceived(MqttAck ack) {
		MqttDeliveryTokenImpl token = getTokenForAck(ack);
		removeToken(ack);
		
		if (token != null) {
			token.notifyReceived(ack);
		}
		//Else token == null - the only way the token will ever be null is if the
		//server has sent a message the client knows nothing about - which the server
		//should never do (unless it's broken somehow).
		//As there's no ability to log in the client and we can't notify the app using
		//the client in any other way, the only other choice is to swallow the problem.
	}
	
	/**
	 * Called by the Receiver's thread to indicate that no more responses are
	 * expected, due to a shutdown of the receiver;
	 */
	protected void noMoreResponses(MqttException reason) {
		noMoreResponses = true;
		noMoreResponsesException = reason;
		Enumeration enumeration = tokens.elements();
		Object token;
		//@TRACE 304=noMoreResponses
		trace.trace(Trace.FINE,304,null,reason);

		while (enumeration.hasMoreElements()) {
			token = enumeration.nextElement();
			if (token != null) {
				synchronized (token) {
					((MqttDeliveryTokenImpl)token).notifyException(reason);
				}
			}
		}
	}
	
	public MqttDeliveryToken[] getOutstandingTokens() {
		Vector list = new Vector();
		Enumeration enumeration = tokens.elements();
		MqttDeliveryToken token;
		while(enumeration.hasMoreElements()) {
			token = (MqttDeliveryToken)enumeration.nextElement();
			if (token != null) {
				if (!(token.equals(pingToken) ||
						token.equals(connectToken) ||
						token.equals(disconnectToken))) {
					list.addElement(token);
				}
			}
		}
		MqttDeliveryToken[] result = new MqttDeliveryToken[list.size()];
		for(int i=0;i<list.size();i++) {
			result[i] = (MqttDeliveryToken)list.elementAt(i);
		}
		return result;
	}
	
	/**
	 * Empties the token store without notifying any of the tokens.
	 * This should only be called when the client has disconnected
	 * cleanSession.
	 */
	public void clear() {
		//@TRACE 305=clear
		trace.trace(Trace.FINE,305);
		tokens.clear();
	}
}
