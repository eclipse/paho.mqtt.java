//-----------------------------------------------------------------------------
// Source File Name: WmqttGauge.java
//
// Description: Gauge that has been extended so that it can automatically
//              progress itself whilst completion of an operation is awaited.
//              
// Licensed Materials - Property of IBM
//
// 5648-C63
// (C) Copyright IBM Corp. 2013 All Rights Reserved.
//
// US Government Users Restricted Rights - Use, duplication or
// disclosure restricted by GSA ADP Schedule Contract with
// IBM Corp.
//
// Version %Z% %W% %I% %E% %U%
//
//-----------------------------------------------------------------------------

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
