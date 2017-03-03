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
package org.eclipse.paho.mqttv5.common;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

public class MqttOutputStream extends OutputStream{
	private static final Logger log = Logger.getLogger(MqttOutputStream.class.getName());
	
	private MqttState mqttState = null;
	private BufferedOutputStream outputStream;
	
	public MqttOutputStream(MqttState mqttState, OutputStream outputStream) {
		this.mqttState = mqttState;
		this.outputStream = new BufferedOutputStream(outputStream);
	}
	
	@Override
	public void close() throws IOException {
		outputStream.close();
	}
	
	@Override
	public void flush() throws IOException{
		outputStream.flush();
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		outputStream.write(b);
		mqttState.notifySentBytes(b.length);
	}
	
	@Override
	public void write(int b) throws IOException {
		outputStream.write(b);
	}
	
	/**
	 * Writes an {@link MqttWireMessage} to the stream.
	 * @param message The {@link MqttWireMessage} to send.
	 * @throws IOException if an exception is thrown when writing to the output stream.
	 * @throws MqttException if an exception is thrown when getting the header of the payload.
	 */
	public void write(MqttWireMessage message) throws IOException, MqttException {
		byte[] bytes = message.getHeader();
		byte[] payload = message.getPayload();
		outputStream.write(bytes, 0, bytes.length);
		mqttState.notifySentBytes(bytes.length);
		
		int offset = 0;
		int chunkSize = 1024;
		while(offset < payload.length){
			int length = Math.min(chunkSize, payload.length - offset);
			outputStream.write(payload,  offset, length);
			offset += chunkSize;
			mqttState.notifySentBytes(length);
		}
	}

}
