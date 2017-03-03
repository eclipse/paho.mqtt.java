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
package org.eclipse.paho.mqttv5.server.listener;

import java.net.Socket;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public class ServerComms {
	
	public ServerComms(NetSocket socket){
		
		socket.handler(buffer -> {
			System.out.println("I received some bytes: " + buffer);
			
			socket.write("This is some data!");
			
			
		});
		
	}

}
