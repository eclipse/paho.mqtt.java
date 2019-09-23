/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.wire;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.ClientState;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;


/**
 * An <code>MqttInputStream</code> lets applications read instances of
 * <code>MqttWireMessage</code>. 
 */
public class MqttInputStream extends InputStream {
	private final String CLASS_NAME = MqttInputStream.class.getName();
	private final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private ClientState clientState = null;
	private DataInputStream in;	
	private ByteArrayOutputStream bais;
	private int remLen;
	private int packetLen;
	private byte[] packet;

	public MqttInputStream(ClientState clientState, InputStream in) {
		this.clientState = clientState;
		this.in = new DataInputStream(in);		
		this.bais = new ByteArrayOutputStream();
		this.remLen = -1;
	}
	
	public int read() throws IOException {
		return in.read();
	}
	
	public int available() throws IOException {
		return in.available();
	}
	
	public void close() throws IOException {
		in.close();
	}
	
	/**
	 * Reads an <code>MqttWireMessage</code> from the stream.
	 * If the message cannot be fully read within the socket read timeout,
	 * a null message is returned and the method can be called again until
	 * the message is fully read.
	 * @return The {@link MqttWireMessage}
	 * @throws IOException if an exception is thrown when reading from the stream
	 * @throws MqttException if the message is invalid 
	 */
	public MqttWireMessage readMqttWireMessage() throws IOException, MqttException {
		final String methodName ="readMqttWireMessage";
		
		MqttWireMessage message = null;
		try {
			// read header
			if (remLen < 0) {
				// Assume we can read the whole header at once.
				// The header is very small so it's likely we
				// are able to read it fully or not at all.
				// This keeps the parser lean since we don't
				// need to cope with a partial header.
				// Should we lose synch with the stream,
				// the keepalive mechanism would kick in
				// closing the connection.
				bais.reset();
				
				byte first = in.readByte();
				clientState.notifyReceivedBytes(1);

				byte type = (byte) ((first >>> 4) & 0x0F);
				if ((type < MqttWireMessage.MESSAGE_TYPE_CONNECT) ||
						(type > MqttWireMessage.MESSAGE_TYPE_DISCONNECT)) {
					// Invalid MQTT message type...
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_INVALID_MESSAGE);
				}
				remLen = MqttWireMessage.readMBI(in).getValue();
				bais.write(first);
				// bit silly, we decode it then encode it
				bais.write(MqttWireMessage.encodeMBI(remLen));
				packet = new byte[(int)(bais.size()+remLen)];
				packetLen = 0;
			}
			
			// read remaining packet
			if (remLen >= 0) {
				// the remaining packet can be read with timeouts
				readFully();

				// reset packet parsing state 
				remLen = -1;
				
				byte[] header = bais.toByteArray();
				System.arraycopy(header,0,packet,0, header.length);
				message = MqttWireMessage.createWireMessage(packet);
				// @TRACE 301= received {0} 
				log.fine(CLASS_NAME, methodName, "301",new Object[] {message});
			}
		} catch (SocketTimeoutException e) {
			// ignore socket read timeout
		}
		
		return message;
	}
	
    private void readFully() throws IOException {
    	int off = bais.size() + (int) packetLen;
    	int len = (int) (remLen - packetLen);
    	if (len < 0)
    		throw new IndexOutOfBoundsException();
    	int n = 0;
    	while (n < len) {
    		int count = -1;
    		try {
    			count = in.read(packet, off + n, len - n);
    		} catch (SocketTimeoutException e) {
    			// remember the packet read so far 
    			packetLen += n;
    			throw e;
    		}
    		

    		if (count < 0) {
    			throw new EOFException();
    		}
    		clientState.notifyReceivedBytes(count);
    		n += count;
    	}
    }
}
