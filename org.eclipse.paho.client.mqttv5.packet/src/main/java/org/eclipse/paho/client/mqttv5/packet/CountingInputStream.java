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

import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends InputStream{
	private InputStream inputStream;
	private int counter;
	
	
	/** Constructs a new <code>CountingInputStream</code> wrapping the supplied
	 * input stream.
	 */
	public CountingInputStream(InputStream inputStream){
		this.inputStream = inputStream;
		this.counter = 0;
	}
	
	public int read() throws IOException {
		int i = inputStream.read();
		if (i != -1){
			counter++;
		}
		return i;
	}
	
	/**
	 * Returns the number of bytes read since last reset
	 */
	public int getCounter() {
		return counter;
	}
	/**
	 * Resets the counter to zero
	 */
	public void resetCounter() {
		counter = 0;
	}
	
}
