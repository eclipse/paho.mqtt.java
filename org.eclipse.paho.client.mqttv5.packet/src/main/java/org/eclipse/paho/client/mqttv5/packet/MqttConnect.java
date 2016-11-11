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
 * 	  Dave Locke - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.client.mqttv5.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv5.util.MqttException;
/**
 * Example Connect Packet Example
 * 
 * +----------+-----------------------+-----+-----+-----+-----+-----+-----+-----+-----+
 * 
 * | Byte No. |      Description      |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0  |
 * 
 * +----------+-----------------------+-----+-----+-----+-----+-----+
 * 
 */
public class MqttConnect extends MqttWireMessage {
	
	public static final String KEY = "Con";
	
	private String clientId;
	private boolean cleanSession;
	private MqttMessage willMessage;
	private String userName;
	private char[] password;
	private int keepAliveInterval;
	private String willDestination;
	private int mqttVersion;
	

	
	/**
	 * Constructor for an on the wire MQTT connect message
	 * 
	 * @param info
	 * @param data
	 * @throws IOException
	 * @throws MqttException
	 */
	public MqttConnect(byte info, byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
		
		String protocol_name = decodeUTF8(dis);
		int protocol_version = dis.readByte();
		byte connect_flags = dis.readByte();
		keepAliveInterval = dis.readUnsignedShort();
		clientId = decodeUTF8(dis);
		dis.close();
	}
	
	public MqttConnect(String clientId, int mqttVersion, boolean cleanSession, int keepAliveInterval, String userName, char[] password, MqttMessage willMessage, String willDestination) {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.clientId = clientId;
		this.cleanSession = cleanSession;
		this.keepAliveInterval = keepAliveInterval;
		this.userName = userName;
		this.password = password;
		this.willMessage = willMessage;
		this.willDestination = willDestination;
		this.mqttVersion = mqttVersion;
	}
	
	@Override
	public String toString(){
		String rc = super.toString();
		rc += " clientId: " + clientId + " keepAliveInterval: " + keepAliveInterval;
		return rc;
	}
	
	@Override
	protected byte getMessageInfo() {
		return (byte) 0;
	}
	
	public boolean isCleanSession(){
		return cleanSession;
	}
	
	
	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			
			// Encode the Protocol Name
			encodeUTF8(dos, "MQTT");
			
			// Encode the MQTT Version
			dos.write(mqttVersion);
			
			byte connectFlags = 0;
			
			if(cleanSession){
				connectFlags |= 0x02;
			}
			
			if(willMessage != null){
				connectFlags |= 0x04;
				connectFlags |= (willMessage.getQos()<<3);
				if(willMessage.isRetained()){
					connectFlags |= 0x20;
				}
			}
			
			if(userName != null){
				connectFlags |= 0x80;
				if(password != null){
					connectFlags |= 0x40;
				}
			}
			
			dos.write(connectFlags);
			dos.writeShort(keepAliveInterval);
			dos.flush();
			return baos.toByteArray();
		} catch(IOException ioe){
			throw new MqttException(ioe);
		}
	}
	
	@Override
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			encodeUTF8(dos, clientId);
			
			if(willMessage != null){
				encodeUTF8(dos, willDestination);
				dos.writeShort(willMessage.getPayload().length);
				dos.write(willMessage.getPayload());
			}
			
			if(userName != null){
				encodeUTF8(dos, userName);
				if(password != null){
					encodeUTF8(dos, new String(password));
				}
			}
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe){
			throw new MqttException(ioe);
		}
	}
	
	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	@Override
	public boolean isMessageIdRequired(){
		return false;
	}
	
	@Override
	public String getKey() {
		return KEY;
	}
	
	
	


}
