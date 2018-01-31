/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.util.CountingInputStream;

public class MqttUnsubscribe extends MqttWireMessage{
	
	private static final Byte[] validProperties = { MqttProperties.USER_DEFINED_PAIR_IDENTIFIER };

	
	// Fields
	private String[] topics;
	private MqttProperties properties;
	

	
	public  MqttUnsubscribe(byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE);
		this.properties = new MqttProperties(validProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream inputStream = new DataInputStream(counter);
		msgId = inputStream.readUnsignedShort();
		
		this.properties.decodeProperties(inputStream);
		
		ArrayList<String> topicList = new ArrayList<>();
		// Whilst we are reading data
		while(counter.getCounter() < data.length){
			topicList.add( MqttDataTypes.decodeUTF8(inputStream));
		}
		topics = topicList.toArray(new String[topicList.size()]);
		
	
		
		inputStream.close();
	}
	
	public MqttUnsubscribe(String[] topics, MqttProperties properties){
		super(MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE);
		this.topics = topics;
		if (properties != null) {
			this.properties = properties;
		} else {
			this.properties = new MqttProperties();
		}
		this.properties.setValidProperties(validProperties);
	}
	
	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Encode the Message ID
			outputStream.writeShort(msgId);


			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();
			outputStream.write(identifierValueFieldsByteArray);
			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}
	
	@Override
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);
			
			for(String topic : topics){
				MqttDataTypes.encodeUTF8(outputStream, topic);
			}
			
			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe){
			throw new MqttException(ioe);
		}
	}
	

	@Override
	protected byte getMessageInfo() {
		return (byte)( 2 | (this.duplicate ? 8 : 0));
	}

	public String[] getTopics() {
		return topics;
	}

	public void setTopics(String[] topics) {
		this.topics = topics;
	}
	
	@Override
	public MqttProperties getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "MqttUnsubscribe [topics=" + Arrays.toString(topics) + ", properties=" + properties + "]";
	}
	
	

	
}
