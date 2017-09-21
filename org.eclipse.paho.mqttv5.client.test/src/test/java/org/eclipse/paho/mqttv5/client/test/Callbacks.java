package org.eclipse.paho.mqttv5.client.test;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttMessage;

public class Callbacks implements MqttCallback, IMqttMessageListener{

	private ArrayList<MqttMessage> messages;
	private ArrayList<Integer> publishedMessageIds;
	private ArrayList<Integer> subscriptionIds;
	private ArrayList<Integer> unsubscribeIds;
	private ArrayList<MqttDisconnectResponse> disconnects;

	private Logger log;

	public Callbacks(Logger log) {
		this.log = log;
		this.messages = new ArrayList<MqttMessage>();
		this.publishedMessageIds = new ArrayList<Integer>();
		this.subscriptionIds = new ArrayList<Integer>();
		this.unsubscribeIds = new ArrayList<Integer>();
		this.disconnects = new ArrayList<MqttDisconnectResponse>();
	}

	public void clear() {
		this.messages = new ArrayList<MqttMessage>();
		this.publishedMessageIds = new ArrayList<Integer>();
		this.subscriptionIds = new ArrayList<Integer>();
		this.unsubscribeIds = new ArrayList<Integer>();
		this.disconnects = new ArrayList<MqttDisconnectResponse>();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.info(String.format("Message Arrived: %s", message.toString()));
		this.messages.add(message);

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		log.info(String.format("Published: %s", token.getMessageId()));
		this.publishedMessageIds.add(token.getMessageId());
	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		log.info(String.format("Disconnected %s", disconnectResponse.toString()));
		this.disconnects.add(disconnectResponse);
	}

	public ArrayList<MqttMessage> getMessages() {
		return messages;
	}

	public ArrayList<Integer> getPublishedMessageIds() {
		return publishedMessageIds;
	}

	public ArrayList<Integer> getSubscriptionIds() {
		return subscriptionIds;
	}

	public ArrayList<Integer> getUnsubscribeIds() {
		return unsubscribeIds;
	}

	public ArrayList<MqttDisconnectResponse> getDisconnects() {
		return disconnects;
	}
	
	

}
