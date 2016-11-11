/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.wire;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that counts the bytes read from it.
 */
public class CountingInputStream extends InputStream {
	private InputStream in;
	private int counter;

	/**
	 * Constructs a new <code>CountingInputStream</code> wrapping the supplied
	 * input stream.
	 * @param in The {@link InputStream}
	 */
	public CountingInputStream(InputStream in) {
		this.in = in;
		this.counter = 0;
	}
	
	public int read() throws IOException {
		int i = in.read();
		if (i != -1) {
			counter++;
		}
		return i;
	}

	/**
	 * @return  the number of bytes read since the last reset.
	 */
	public int getCounter() {
		return counter;
	}
	
	/**
	 * Resets the counter to zero.
	 */
	public void resetCounter() {
		counter = 0;
	}
}
