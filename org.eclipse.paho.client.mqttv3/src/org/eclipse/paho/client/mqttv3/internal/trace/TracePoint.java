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
package org.eclipse.paho.client.mqttv3.internal.trace;

public class TracePoint {
	public static final byte ENTRY = 0x01;
	public static final byte EXIT  = 0x02;
	public static final byte BREAK = 0x03;
	public static final byte CATCH = 0x04;
	public static final byte OTHER = 0x05;
	
	public short source;
	public long timestamp;
	public byte type;
	public short id;
	public byte level;
	public String threadName;
	public Throwable throwable;
	public String[] stacktrace;
	public Object[] inserts;
	
	public TracePoint() {
	}
	
	public TracePoint(short source, byte type, byte level, int id, Throwable throwable, Object[] inserts) {
		this.source = source;
		this.threadName = Thread.currentThread().getName();
		this.timestamp = System.currentTimeMillis();
		this.type = type;
		this.level = level;
		this.id = (short)id;
		this.throwable = throwable;
		this.inserts = inserts;
	}
}
