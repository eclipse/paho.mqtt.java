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
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.util.Vector;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubComp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;


/**
 * Bridge between Receiver and the external API.
 * This class gets called by Receiver, and then converts the comms-centric
 * MQTT message objects into ones understood by the external API.
 */
public class CommsCallback implements Runnable {
	private static int INBOUND_QUEUE_SIZE = 10;
	private MqttCallback mqttCallback;
	private ClientComms clientComms;
	private Vector messageQueue;
	private Vector completeQueue;
	private boolean running = false;
	private boolean quiescing = false;
	private Object lifecycle = new Object();
	private Thread callbackThread;
	private Object workAvailable = new Object();
	private Object spaceAvailable = new Object();
	private boolean invoking = false;
	private Trace trace;
	
	
	CommsCallback(Trace trace, ClientComms clientComms) {
		this.trace = trace;
		this.clientComms = clientComms;
		this.messageQueue = new Vector(INBOUND_QUEUE_SIZE);
		this.completeQueue = new Vector(INBOUND_QUEUE_SIZE);
	}
	
	/**
	 * Starts up the Sender thread.
	 */
	public void start() {
		if (running == false) {
			running = true;
			quiescing = false;
			callbackThread = new Thread(this, "MQTT Client Callback");
			callbackThread.start();
		}
	}

	/**
	 * Stops the Receiver's thread.  This call will block.
	 */
	public void stop() throws IOException {
		if (running) {
			// @TRACE 700=stop
			trace.trace(Trace.FINE,700);
			running = false;
			if (!Thread.currentThread().equals(callbackThread)) {
				try {
					synchronized (lifecycle) {
						synchronized (workAvailable) {
							// @TRACE 701=stop: notify workAvailable
							trace.trace(Trace.FINE,701);
							workAvailable.notifyAll();
						}
						// @TRACE 702=stop: wait lifecycle
						trace.trace(Trace.FINE,702);
						// Wait for the thread to finish.
						lifecycle.wait();
					}
				}
				catch (InterruptedException ex) {
				}
			}
			// @TRACE 703=stop complete
			trace.trace(Trace.FINE,703);
		}
	}
	
	public void setCallback(MqttCallback mqttCallback) {
		this.mqttCallback = mqttCallback;
	}
	
	public void run() {
		while(running) {
			// If no work is currently available, then wait until there is some...
			try {
				synchronized (workAvailable) {
					if (messageQueue.isEmpty() && completeQueue.isEmpty()) {
						// @TRACE 704=run: wait workAvailable
						trace.trace(Trace.FINE,704);
						workAvailable.wait();
					}
				}
			} catch (InterruptedException e) {
			}
			if (running) {
				// Check for deliveryComplete callbacks...
				if (!completeQueue.isEmpty()) {
					if (mqttCallback != null) {
						MqttDeliveryToken token = (MqttDeliveryToken) completeQueue.elementAt(0);
						completeQueue.removeElementAt(0);
						if (trace.isOn()) {
							// @TRACE 705=run: deliveryComplete token={0}
							trace.trace(Trace.FINE,705, new Object[]{token});
						}
						mqttCallback.deliveryComplete(token);
					}
				}
				// Check for messageArrived callbacks...
				if (!messageQueue.isEmpty()) {
					if (quiescing) {
						messageQueue.clear();
					} else {
						// Ensure we're really connected before calling back with the message.
						// There is a window on connect where a publish could arrive before we've
						// finished the connect logic, causing the message to be lost.
						if (clientComms.isConnected()) {
							invoking = true;
							MqttPublish message = (MqttPublish) messageQueue.elementAt(0);
							messageQueue.removeElementAt(0);
							handleMessage(message);
							invoking = false;
						}
					}
				} 
			}
			// Notify the spaceAvailable lock, to say that there's now some space
			// on the queue...
			synchronized (spaceAvailable) {
				// @TRACE 706=run: notify spaceAvailable
				trace.trace(Trace.FINE,706);
				spaceAvailable.notifyAll();
			}
		}
		/* the following line was added to fix defect 62163.  It is probable that the messageQueue.clear() in the code above is superfluous after
		 * adding this, but I wanted to make the minimum change at this point
		 */
		messageQueue.clear();
		synchronized (lifecycle) {
			// @TRACE 707=run: notify lifecycle
			trace.trace(Trace.FINE,707);
			lifecycle.notifyAll();
		}
	}
	
	/**
	 * This method is called when the connection to the server is lost.
	 * 
	 * @param cause the reason behind the loss of connection.
	 */
	public void connectionLost(Throwable cause) {
		if (mqttCallback != null) {
			// @TRACE 708=run: connectionLost
			trace.trace(Trace.FINE,708,null,cause);
			mqttCallback.connectionLost(cause);
		}
	}

	/**
	 * This method is called when a message arrives on a topic.
	 * 
	 * @param sendMessage the MQTT SEND message.
	 */
	public void messageArrived(MqttPublish sendMessage) {
		if (mqttCallback != null) {
			// If we already have enough messages queued up in memory, wait until
			// some more queue space becomes available.  This helps the client protect
			// itself from getting flooded by messages from the server.
			synchronized (spaceAvailable) {
				if (!quiescing && messageQueue.size() >= INBOUND_QUEUE_SIZE) {
					try {
						// @TRACE 709=messageArrived: wait spaceAvailable
						trace.trace(Trace.FINE,709);
						spaceAvailable.wait();
					}
					catch (InterruptedException ex) {
					}
				}
			}
			if (!quiescing) {
				messageQueue.addElement(sendMessage);
				// Notify the CommsCallback thread that there's work to do...
				synchronized (workAvailable) {
					// @TRACE 710=messageArrived: notify workAvailable
					trace.trace(Trace.FINE,710);
					workAvailable.notifyAll();
				}
			}
		}
	}
	
	public void quiesce() {
		this.quiescing = true;
		synchronized (spaceAvailable) {
			// @TRACE 711=quiesce: notify spaceAvailable
			trace.trace(Trace.FINE,711);
			// Unblock anything waiting for space...
			spaceAvailable.notifyAll();
		}
		synchronized (spaceAvailable) {
			if (invoking) {
				// Wait until the last message has finished processing...
				try {
					// @TRACE 712=quiesce: wait spaceAvailable
					trace.trace(Trace.FINE,712);
					spaceAvailable.wait();
				}
				catch (InterruptedException ex) {
				}
			}
		}
	}
	
	private void handleMessage(MqttPublish publishMessage) {
		// If disconnect() was called within a previous call to messageArrived,
		// we may just need to skip the processing of any messages we have in memory.
		if (clientComms.isConnected() && (mqttCallback != null)) {
			// Handle getting an MqttDestination object...
			String destName = publishMessage.getTopicName();
			MqttTopic destination = null;
			if (destName != null) {
				destination = clientComms.getTopic(destName);
			}
			
			try {
				if (trace.isOn()) {
					// @TRACE 713=handleMessage: messageArrived topic={0} id={1}
					trace.trace(Trace.FINE,713,new Object[]{destination.getName(),new Integer(publishMessage.getMessageId())});
				}
				mqttCallback.messageArrived(destination, publishMessage.getMessage());
				if (publishMessage.getMessage().getQos() == 1) {
					this.clientComms.sendNoWait(new MqttPubAck(publishMessage));
				} else if (publishMessage.getMessage().getQos() == 2) {
					this.clientComms.deliveryComplete(publishMessage);
					MqttPubComp pubComp = new MqttPubComp(publishMessage);
					this.clientComms.sendNoWait(pubComp);
				}
			}
			catch (Exception ex) {
				// @TRACE 714=handleMessage: messageArrived threw exception
				trace.trace(Trace.FINE,714,null, ex);
				clientComms.shutdownConnection(new MqttException(ex));
			}
		}
	}
	
	public void deliveryComplete(MqttDeliveryToken token) {
		if (mqttCallback != null) {
			completeQueue.addElement(token);
			synchronized (workAvailable) {
				if (trace.isOn()) {
					// @TRACE 715=delieveryComplete: notify workAvailable. token={0}
					trace.trace(Trace.FINE,715, new Object[]{token});
				}
				workAvailable.notifyAll();
			}
		}
	}
	
	/**
	 * Returns the thread used by this callback.
	 */
	protected Thread getThread() {
		return callbackThread;
	}
}
