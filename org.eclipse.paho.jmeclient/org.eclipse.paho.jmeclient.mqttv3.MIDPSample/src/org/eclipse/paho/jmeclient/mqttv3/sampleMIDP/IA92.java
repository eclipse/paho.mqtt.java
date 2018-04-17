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

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;
import org.eclipse.paho.client.mqttv3.logging.MIDPLogging;

public class IA92 extends MIDlet implements CommandListener, Runnable, MqttCallback {
	private Displayable configPanel = null; 
	private Displayable gaugePanel  = null; 
	private TextBox     logPanel    = null;
	private Alert       errorPanel  = null;
	private Alert       infoPanel   = null;
	
	private WmqttGauge  gauge       = new WmqttGauge();

	private Command     connect     = new Command( "Connect", Command.OK    , 0 );
	private Command     cancel      = new Command( "Cancel" , Command.CANCEL, 0 );

	private RecordStore config = null;

	// WMQTT connection manager object
	private WmqttMgr wmqttConnMgr = null;

    // Indicies for extracting data from the connect form
    private static final int IDX_CLIENT_ID = 0;
    private static final int IDX_IP_ADDR   = 1;
    private static final int IDX_PORT_NUM  = 2;

	// States for the midlet
	private final static int DISCONNECTED  = 0;
	private final static int CONNECTING    = 1;
	private final static int DISCONNECTING = 2;
	private final static int PUBARRIVED    = 3;
		
	private int state = DISCONNECTED;

	// The name of the J2ME RecordStore into which parameters will be saved.
	private final static String RMS_NAME = "WMQTT";
	
	// Objects have access synchronised by pubArrivedLock
	private String newTopic = null;
	private byte[] newData  = null;
	
    /**
     * Zero argument constructor.
     * Initialisation is done in the startApp method
     */ 
    public IA92() {
        super();
    }

	/**
	 * @see MIDlet#startApp()
	 * startApp creates all the screens required. StartApp is called each time the
	 * application is resumed from a paused state, so this method and all methods
	 * called by it are written to cope with being called multiple times.
	 */
	protected void startApp() throws MIDletStateChangeException {
		
		// Set the logger for MIDP
		LoggerFactory.setLogger("org.eclipse.paho.client.mqttv3.logging.MIDPLogging");
		

		if ( config == null ) {
			// Open a RecordStore to hold TCP/IP configuration data
			try {
				config = RecordStore.openRecordStore( RMS_NAME, true );
			} catch ( RecordStoreException rse ) {
				// Run without the record store
				config = null;
			}
		}

		if ( errorPanel == null ) {
			errorPanel = new Alert( "Echo error!", "", null, AlertType.ERROR );
		}
		if ( infoPanel == null ) {
			infoPanel  = new Alert( "Echo info"   , "", null, AlertType.INFO  );
			infoPanel.setTimeout( 3000 );
		}
		createGaugePanel();
		createLogPanel();
		createConfigPanel();

	}

	/**
	 * @see MIDlet#pauseApp()
	 */
	protected void pauseApp() {
	}

	/**
	 * @see MIDlet#destroyApp(boolean)
	 */
	protected void destroyApp(boolean flag) throws MIDletStateChangeException {
		notifyDestroyed();
	}

	/**
	 * Runnable interface
	 * Whenever a significant piece of work needs to be done by hte MIDlet
	 * it sets the stat appropriately and starts a Thread to do the work.
	 * This keeps the GUI responsive.
	 */
	public void run() {
				 
		if ( state == CONNECTING ) {
			// Connect to the broker
			
			// Get the input parameters from the ui
			String clientId	= ((TextField)((Form)configPanel).get(IDX_CLIENT_ID)).getString();
			String broker	= ((TextField)((Form)configPanel).get(IDX_IP_ADDR)).getString();
			String portStr  = ((TextField)((Form)configPanel).get(IDX_PORT_NUM)).getString();
			
			int port = 1883;
			try {
				port = Integer.parseInt( portStr );
			} catch( NumberFormatException nfe ) {
				port = 1883;
			}		
			
			// Store the parameters in an RMS store
            try {
                // BIG Workaround. You should be able to set specific
                // records in the store. As a workaround create a new store each time.
                config.closeRecordStore();
                RecordStore.deleteRecordStore(RMS_NAME);
                config = RecordStore.openRecordStore( RMS_NAME, true );
            } catch ( RecordStoreException rse1 ) {
            	config = null;
            }

            if ( config != null ) {
                try {
                    config.addRecord( broker.getBytes()  , 0, broker.length()   );
                    config.addRecord( portStr.getBytes() , 0, portStr.length()  );
                    config.addRecord( clientId.getBytes(), 0, clientId.length() );
                } catch( RecordStoreException rse2 ) {
                }
            }

			// Connect to the broker
			boolean wmqttConnected = true;
			try {
				wmqttConnMgr = new WmqttMgr( clientId, broker, port );
				wmqttConnMgr.setCallback( this );
				wmqttConnMgr.connectToBroker();
			} catch ( MqttException e ) {
				wmqttConnected = false;
				String errMsg = "WMQTT connect failed\n";
				if ( e.getMessage() != null ) {
					errMsg += "\n" + e + "\n";
				}
				if ( e.getCause() != null ) {
					errMsg += "\n" + e.getCause();
				}
				errorPanel.setString( errMsg );
				Display.getDisplay(this).setCurrent( errorPanel, configPanel );
			}		

			// Subscribe for echo requests
			if ( wmqttConnected ) {
				try {
					if ( wmqttConnMgr.subscribe() ) {
						infoPanel.setString("Connected and subscribed!");
						Display.getDisplay(this).setCurrent( infoPanel, logPanel );
					} else {
						errorPanel.setString("Subscribe failed!");
						Display.getDisplay(this).setCurrent( errorPanel, configPanel );
					}
				} catch(Exception e ) {
					errorPanel.setString("Subscribe failed!\n" + e.getMessage() );
					Display.getDisplay(this).setCurrent( errorPanel, configPanel );
				}
			}	
					
			gauge.stop();
			
		} else if ( state == DISCONNECTING ) {
			// Unsubscribe and disconnect from the broker
			
			try {
				wmqttConnMgr.unsubscribe();
			} catch ( Exception e ) {
				// Unable to unsubscribe. If the application connected using the
				// WMQTT clean session option, then the subscription will be
				// automatically removed anyway.
			}		
			
			try {
				wmqttConnMgr.disconnectClient();
			
				wmqttConnMgr.destroyClient();
				wmqttConnMgr = null;
			
			} catch (MqttException e) {
				// Unable to disable client
			}
			
			Display.getDisplay(this).setCurrent( configPanel );

			gauge.stop();
			
		} else if ( state == PUBARRIVED ) {
			// Process a publishArrived event.			
			
			// Display the data on the device screen
			String message =  "Topic: " + newTopic + "\n\nData : " + new String( newData );
			logPanel.setString( message );
				
			// Echo the publication back on the response topic.
			try {
				wmqttConnMgr.publishResponse( newData );
				logPanel.setString(message + "\n \n" + "Response sent to Topic: " + wmqttConnMgr.getRespTopic());
			} catch ( MqttException mqe ) {
				errorPanel.setString("publishArrived: MqttException:" + mqe.getMessage() );
				Display.getDisplay(this).setCurrent( errorPanel, logPanel );
			} catch ( Exception e ) {
				errorPanel.setString("publishArrived: Exception:" + e.getMessage() );
				Display.getDisplay(this).setCurrent( errorPanel, logPanel );
			}
		}	
	}	
	
	/**
	 * CommandListener interface
	 * Handle events from the user when buttons on the GUI are pressed.
	 */
	public void commandAction( Command c, Displayable d ) {

		if ( d.equals( configPanel ) ) {
			if ( c.equals( connect ) ) {
				// Proceed
				gauge.setLabel( "Connecting..." );
				gauge.start();

				Display.getDisplay(this).setCurrent( gaugePanel );
				
				state = CONNECTING;
				new Thread( this ).start();
								
			} else {
				// Cancel
				try {
					destroyApp( true );
				} catch( Exception e ) {
				}		
			}		
		} else if ( d.equals( logPanel ) ) {
			// The only command we expect is cancel
			// Cancel - revert to configuration page
			gauge.setLabel( "Disconnecting..." );
			state = DISCONNECTING;

			gauge.start();
			Display.getDisplay(this).setCurrent( gaugePanel );
			new Thread( this ).start();
			
		}	
	}

	/**
	 * createConfigPanel
	 * Build the lcdui display. Add the necessary fields to
	 * a form and return the completed Displayable object.
	 */
    public void createConfigPanel() {
    	if ( configPanel == null ) {
			Item[] items = new Item[3];
			String ipAddress = "127.0.0.1";
			String portNum = "1883";
			String clientId = "MQTT_MIDLET";

			// Query the record store (if we have opened it) for any previously set
			// IP address, port number or client identifier
			if ( config != null ) {
				byte[] ip   = null;
				byte[] port = null;
				byte[] cid = null;
				try {
					ip   = config.getRecord(1);
					if ( ip != null ) {
						ipAddress =  new String(ip);
					}
					port = config.getRecord(2);
					if ( port != null ) {
						portNum =  new String(port);
					}
					cid = config.getRecord(3);
					if ( cid != null ) {
						clientId =  new String(cid);
					}
				} catch ( RecordStoreException rse ) {
					// Don't worry if something fails. The user can enter the information again
				}
			}

			// Build up the GUI objects
			TextField t1 = new TextField( "Client Id", clientId, 100, TextField.ANY );
			TextField t2 = new TextField( "IP address", ipAddress, 100, TextField.ANY );
			TextField t3 = new TextField( "IP port", portNum, 4, TextField.NUMERIC );

			items[IDX_CLIENT_ID] = (Item)t1;
			items[IDX_IP_ADDR] = (Item)t2;
			items[IDX_PORT_NUM] = (Item)t3;
			Form f = new Form( "Connection", items );

			f.addCommand( connect );
			f.addCommand( cancel );
        
			f.setCommandListener( this );

			configPanel = f;
			Display.getDisplay(this).setCurrent( configPanel );
    	}
    }

	/**
	 * Create a simple text area where publications are displayed as they arrive.
	 */
	public void createLogPanel() {
		if ( logPanel == null ) {
			TextBox t = new TextBox("Log:", null, 128, TextField.ANY );
			t.addCommand( cancel );
        
			t.setCommandListener(this);
			
			logPanel = t;
		}
	}
		
	/**
	 * createGaugePanel
	 * Build the lcdui display. Add a gauge to a form and
	 * return the completed Displayable object.
	 */
	public void createGaugePanel() {
		if ( gaugePanel == null ) {
			Item[] items = new Item[1];
			items[0] = gauge; 

			Form f = new Form( "Progress", items );
			
			gaugePanel = f;
		}
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(Throwable)
	 */
	public void connectionLost(Throwable cause) {
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#messageArrive(String, MqttMessage)
	 */
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		
		newTopic = topic;
		newData  = message.getPayload();
		
		state = PUBARRIVED;	
		new Thread(this).start();
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.deliveryComplete(String, MqttMessage)
	 */
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

}

