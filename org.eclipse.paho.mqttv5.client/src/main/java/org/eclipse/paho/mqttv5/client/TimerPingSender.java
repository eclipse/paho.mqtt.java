/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corp.
 * Copyright (c) 2017 BMW Car IT GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at
 *   https://www.eclipse.org/org/documents/edl-v10.php
 */

package org.eclipse.paho.mqttv5.client;

import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.mqttv5.client.internal.ClientComms;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;

/**
 * Default ping sender implementation
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
public class TimerPingSender implements MqttPingSender{
	private static final String CLASS_NAME = TimerPingSender.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private ClientComms comms;
	private Timer timer;
	private ScheduledExecutorService executorService = null;
	private ScheduledFuture<?> scheduledFuture;
	private String clientid;

	public TimerPingSender(ScheduledExecutorService executorService) {
		this.executorService = executorService;
	}

	public void init(ClientComms comms) {
		if (comms == null) {
			throw new IllegalArgumentException("ClientComms cannot be null.");
		}
		this.comms = comms;
		clientid = comms.getClient().getClientId();
		log.setResourceName(clientid);
	}

	public void start() {
		final String methodName = "start";

		//@Trace 659=start timer for client:{0}
		log.fine(CLASS_NAME, methodName, "659", new Object[]{ clientid });
		if (executorService == null) {
			timer = new Timer("MQTT Ping: " + clientid);
			//Check ping after first keep alive interval.
			timer.schedule(new PingTask(), comms.getKeepAlive());
		} else {
			//Check ping after first keep alive interval.
			schedule(comms.getKeepAlive());
		}
	}

	public void stop() {
		final String methodName = "stop";
		//@Trace 661=stop
		log.fine(CLASS_NAME, methodName, "661", null);
		if (executorService == null) {
			if (timer != null){
				timer.cancel();
			}
		} else {
			if (scheduledFuture != null) {
				scheduledFuture.cancel(true);
			}
		}
	}

	public void schedule(long delayInMilliseconds) {
		if (executorService == null) {
			timer.schedule(new PingTask(), delayInMilliseconds);	
		} else {
			scheduledFuture = executorService.schedule(new PingRunnable(), delayInMilliseconds, TimeUnit.MILLISECONDS);
		}
	}
	
	private class PingTask extends TimerTask {
		private static final String methodName = "PingTask.run";

		public void run() {
			Thread.currentThread().setName("MQTT Ping: " + clientid);
			//@Trace 660=Check schedule at {0}
			log.fine(CLASS_NAME, methodName, "660", new Object[]{ Long.valueOf(System.nanoTime()) });
			comms.checkForActivity();
		}
	}

	private class PingRunnable implements Runnable {
		private static final String methodName = "PingTask.run";

		public void run() {
			String originalThreadName = Thread.currentThread().getName();
			Thread.currentThread().setName("MQTT Ping: " + clientid);
			//@Trace 660=Check schedule at {0}
			log.fine(CLASS_NAME, methodName, "660", new Object[]{ Long.valueOf(System.nanoTime()) });
			comms.checkForActivity();
			Thread.currentThread().setName(originalThreadName);
		}
	}
}
