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
 *    James Sutton - Bug 459142 - WebSocket support for the Java client.
 */
package org.eclipse.paho.client.mqttv3.internal.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

public class WebSocketReceiver implements Runnable{

	private static final String CLASS_NAME = WebSocketReceiver.class.getName();
	private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private boolean running = false;
	private boolean stopping = false;
	private Object lifecycle = new Object();
	private InputStream input;
	private Thread receiverThread = null;
	private volatile boolean receiving;
	private PipedOutputStream pipedOutputStream;

	public WebSocketReceiver(InputStream input, PipedInputStream pipedInputStream) throws IOException{
		this.input = input;
		this.pipedOutputStream = new PipedOutputStream();
		pipedInputStream.connect(pipedOutputStream);
	}

	/**
	 * Starts up the WebSocketReceiver's thread
	 * @param threadName The name of the thread
	 */
	public void start(String threadName){
		final String methodName = "start";
		//@TRACE 855=starting
		log.fine(CLASS_NAME, methodName, "855");
		synchronized (lifecycle) {
			if(!running) {
				running = true;
				receiverThread = new Thread(this, threadName);
				receiverThread.start();
			}
		}
	}

	/**
	 * Stops this WebSocketReceiver's thread.
	 * This call will block.
	 */
	public void stop() {
		final String methodName = "stop";
		stopping = true;
        boolean closed = false;
		synchronized (lifecycle) {
			//@TRACE 850=stopping
			log.fine(CLASS_NAME,methodName, "850");
			if(running) {
				running = false;
				receiving = false;
                closed = true;
				closeOutputStream();

			}
		}
		if(closed && !Thread.currentThread().equals(receiverThread)) {
			try {
				// Wait for the thread to finish
		        //This must not happen in the synchronized block, otherwise we can deadlock ourselves!
				receiverThread.join();
			} catch (InterruptedException ex) {
				// Interrupted Exception
			}
		}
		receiverThread = null;
		//@TRACE 851=stopped
		log.fine(CLASS_NAME, methodName, "851");
	}

	public void run() {
		final String methodName = "run";

		while (running && (input != null)) {
			try {
				//@TRACE 852=network read message
				log.fine(CLASS_NAME, methodName, "852");
				receiving = input.available() > 0;
				WebSocketFrame incomingFrame = new WebSocketFrame(input);
				if(!incomingFrame.isCloseFlag()){
					for(int i = 0; i < incomingFrame.getPayload().length; i++){
						pipedOutputStream.write(incomingFrame.getPayload()[i]);
					}

					pipedOutputStream.flush();
				} else {
					if(!stopping){
						throw new IOException("Server sent a WebSocket Frame with the Stop OpCode");
					}
				}

				receiving = false;

			} catch (IOException ex) {
				// Exception occurred whilst reading the stream.
				this.stop();
			}
		}
	}

	private void closeOutputStream(){
		try {
			pipedOutputStream.close();
		} catch (IOException e) {
		}
	}


	public boolean isRunning() {
		return running;
	}

	/**
	 * Returns the receiving state.
	 *
	 * @return true if the receiver is receiving data, false otherwise.
	 */
	public boolean isReceiving(){
		return receiving;
	}

}
