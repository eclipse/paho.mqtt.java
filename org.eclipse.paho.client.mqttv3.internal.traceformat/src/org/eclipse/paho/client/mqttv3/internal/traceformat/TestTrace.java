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
package org.eclipse.paho.client.mqttv3.internal.traceformat;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class TestTrace {
	public static void main(String[] args) {
		try {
			MqttClient client = new MqttClient("tcp://localhost:1883","foobar");
			client.connect();
			client.subscribe("foo");
			for (int i=0;i<5;i++) {
				client.getTopic("bar").publish(new MqttMessage("hi".getBytes()));
			}
			client.disconnect();
		} catch(Exception e) {
			e.printStackTrace();
		}
			/*
			System.out.println(System.getProperty("user.dir"));
			long start = System.currentTimeMillis();
			Trace trace = Trace.getTrace("foo");
			for (int i=0;i<100;i++) {
				trace.trace(Trace.FINE,123);
				trace.trace(Trace.FINE,456,new Object[] {"a",new Date(),System.class});
				trace.traceEntry(Trace.FINE,678);
				trace.traceExit(Trace.FINE,789);
				trace.trace(Trace.FINE,123);
				trace.trace(Trace.FINE,123);
				//trace.traceCatch(Trace.FINE,345, new Exception());
				//trace.trace(Trace.FINE,543,null,new Exception());
			}
			long stop = System.currentTimeMillis();
			System.out.println(stop-start);
			*/
		try {
			TracePointExtractor.main(new String[] {"-d","/home/nol/workspace/org.eclipse.paho.client.mqttv3/mqtt_client_v3/","-o","/home/nol/workspace/org.eclipse.paho.client.mqttv3/trace.properties"});
			TraceFormatter f = new TraceFormatter("mqtt-0.trc","/tmp/trace.html");
			f.format();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
