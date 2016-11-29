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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.server.config.ListenerConfiguration;

public class MqttListenerThread implements Runnable, Listener{
	
	private static final Logger LOG = Logger.getLogger(MqttListenerThread.class.getName());

	
	private boolean running = false;
	private Object lifecycle = new Object();
	private Thread listenerThread = null;
	
	private int port;
	private String name;
	
	public MqttListenerThread(ListenerConfiguration listenerConfiguration){
		this.port = listenerConfiguration.getPort();
		this.name = listenerConfiguration.getName();

	}
	
	

	@Override
	public void run() {
		LOG.info("MqttListener Thread Starting, name: " + this.name + ", on port: " +  this.port);
		try {
			ServerSocket listener = new ServerSocket(this.port);
			while(running){
				new MqttListener(listener.accept()).start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}



	@Override
	public void start() {
		synchronized (lifecycle) {
			if(!running){
				running = true;
				listenerThread = new Thread(this, this.name);
				listenerThread.start();
			}
		}
	}

	
	@Override
	public void stop() {
		synchronized (lifecycle) {
			LOG.fine("Stopping MqttListener Thread...");
			if(running){
				running = false;
				if(!Thread.currentThread().equals(listenerThread)) {
					try {
						while(listenerThread.isAlive()){
							// First notify the listener to finish
							
							// Wait for the thread to finish
							listenerThread.join(100);
						}
					} catch(InterruptedException ex) {
						LOG.info(ex.getLocalizedMessage());
					}
				}
			}
			listenerThread = null;
			LOG.fine("Thread stopped..");
		}
		
	}
}
