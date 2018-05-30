/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.paho.jmeclient.mqttv3.sampleMIDP;

import javax.microedition.lcdui.Gauge;


public class WmqttGauge extends Gauge implements Runnable {
	private Object   runLock = new Object();
	private boolean runLockNotified = false;
	
	private final static int MAX_VALUE = 100;
	private int curValue = 0;
	
	public WmqttGauge() {
       this( "" );
	}
	
	public WmqttGauge( String label ) {
       super( label, false, MAX_VALUE, 0 );
	}	

	/**
	 * Runnable interface
	 */	
	public void run() {
		synchronized( runLock ) {
			while ( !runLockNotified ) {
				curValue += 5;
				if ( curValue > MAX_VALUE ) {
					curValue = 0;
				}	
				setValue( curValue );
				try {
					runLock.wait( 100 );
				} catch ( InterruptedException ie ) {
				}	
			}
			runLockNotified = false;		
		}	
	}		

	/**
	 * Start the gauge scrolling
	 */
	public void start() {
       new Thread( this ).start();
	}
			
	/**
	 * Stop the gauge scrolling
	 */
	public void stop() {
		synchronized( runLock ) {
			runLockNotified = true;
			runLock.notify();
		}	
	}	

}
