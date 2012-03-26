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

import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;

public class Trace {

	public static final byte FINE   = 0x01;
	public static final byte FINER  = 0x02;
	public static final byte FINEST = 0x03;
	
	private static TraceDestination destination;
	private static short count = 0;
	
	public synchronized static Trace getTrace(String resource) {
		if (destination == null) {
			if (ExceptionHelper.isClassAvailable("java.io.File")) {
				try {
					// Hide this class reference behind reflection so that the class does not need to
					// be present when compiled on midp
					destination = (TraceDestination)Class.forName("org.eclipse.paho.client.mqttv3.internal.trace.TraceFileDestination").newInstance();
				} catch (Exception e) {
				}
			}
		}
		Trace trace = new Trace(count,resource);
		count++;
		return trace;
	}
	
	private short source;
	private String resource;
	private boolean on;
	
	private Trace(short source, String resource) {
		this.source = source;
		this.resource = resource;
		this.on = Trace.destination!=null && Trace.destination.isEnabled(this.resource);
	}
	
	public boolean isOn() {
		return on;
	}
	
	public void traceEntry(byte level, int id) {
		if (on) {
			Trace.destination.write(new TracePoint(this.source,TracePoint.ENTRY,level,id,null,null));
		}
	}
	public void traceExit(byte level, int id) {
		if (on) {
			Trace.destination.write(new TracePoint(this.source,TracePoint.EXIT,level,id,null,null));
		}
	}
	public void traceBreak(byte level, int id) {
		if (on) {
			Trace.destination.write(new TracePoint(this.source,TracePoint.BREAK,level,id,null,null));
		}
	}
	public void traceCatch(byte level, int id, Throwable throwable) {
		if (on) {
			Trace.destination.write(new TracePoint(this.source,TracePoint.CATCH,level,id,throwable,null));
		}
	}
	public void trace(byte level, int id) {
		if (on) {
			Trace.destination.write(new TracePoint(this.source,TracePoint.OTHER,level,id,null,null));
		}
	}
	public void trace(byte level, int id, Object[] inserts) {
		if (on) {
			Trace.destination.write(new TracePoint(this.source,TracePoint.OTHER,level,id,null,inserts));
		}
	}
	public void trace(byte level, int id, Object[] inserts, Throwable throwable) {
		if (on) {
			Trace.destination.write(new TracePoint(this.source,TracePoint.OTHER,level,id,throwable,inserts));
		}
	}
}
