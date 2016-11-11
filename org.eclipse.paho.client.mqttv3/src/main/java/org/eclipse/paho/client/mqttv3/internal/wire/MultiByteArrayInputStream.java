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

public class MultiByteArrayInputStream extends InputStream {

	private byte[] bytesA;
	private int offsetA;
	private int lengthA;
	private byte[] bytesB;
	private int offsetB;
	private int lengthB;
	
	private int pos = 0;
	
	public MultiByteArrayInputStream(byte[] bytesA, int offsetA, int lengthA, byte[] bytesB, int offsetB, int lengthB) {
		this.bytesA = bytesA;
		this.bytesB = bytesB;
		this.offsetA = offsetA;
		this.offsetB = offsetB;
		this.lengthA = lengthA;
		this.lengthB = lengthB;
	}
	public int read() throws IOException {
		int result = -1;
		if (pos<lengthA) {
			result = bytesA[offsetA+pos];
		} else if (pos<lengthA+lengthB) {
			result = bytesB[offsetB+pos-lengthA];
		} else {
			return -1;
		}
		if (result < 0) {
			result += 256;
		}
		pos++;
		return result;
	}

}
