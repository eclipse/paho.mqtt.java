/*******************************************************************************
 * Copyright (c) 2002, 2013 IBM Corp.
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
 * Description: MQTT connect options dialog box
 *
 * Contributors:
 *    Ian Craggs - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.paho.sample.utility;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;

/** This class constructs the user interface dialog allowing the input of additional 
  * connect options as well as some usability options such as displaying
  * a history of events and enabling persistence in the protocol.
  */
public class ConnOpts extends JPanel implements ActionListener {
	private static final String CLIENT_ID = "MQTT_Utility";
	private static final String KEEP_ALIVE = "30";
	private static final String RETRY_INT = "10";
	private static final String DEF_PERSIST_DIR = ".";
	private MQTTFrame mqisdpMgr = null;
	private JButton    trace;
    private JCheckBox  cleanSession;
    private JCheckBox  persistEnable;
    private JTextField clientId;
    private JTextField keepAlive;
    private JTextField retryInterval;
    private JTextField lwtTopic;
    private JTextField persistDir;
    private JTextArea  lwtData;
    private JCheckBox  lwtRetain;
    private JComboBox  lwtQoS;
	private Integer[]  qos = { Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2) };
    
	/**
	 * Constructor for ConnOpts. The constructor builds all the GUI objects required and creates the
	 * dialog in hidden mode ready to be made visible when required.
	 *
	 * @param mgr The object that manages the MQIsdp connection
	 * @param props The properties object created from the config file on disk
	 */
	public ConnOpts( MQTTFrame mgr, Properties props ) {

        mqisdpMgr = mgr;
        
        // Get the container for this dialog and set the size and layout manager		
        Container connOptions = this;
        connOptions.setLayout( new BorderLayout() );

        // Create widgets to place on the GUI
        // Clean session check box
        cleanSession = new JCheckBox();
        cleanSession.setSelected(true);
        
        // Text field to hold the client id
        clientId = new JTextField( props.getProperty( "ClientId", CLIENT_ID ), 15 );
        clientId.setMaximumSize( MQTTFrame.TEXT_FIELD_DIMENSION );

        // Should persistence be used for the WMQtt connection?
        persistEnable = new JCheckBox();
        Boolean b = new Boolean( props.getProperty( "Persistence", String.valueOf(false) ) );
        persistEnable.setSelected( b.booleanValue() );
        persistEnable.setToolTipText("Persist publications to ensure delivery");
                
        // Default button
        // Add an actionlistener and tooltip to the button
        
        JButton reset = new JButton("Default");
        reset.addActionListener(this);
        reset.setToolTipText( "Options will be set to default" );
        
        // Create components to go in the NORTH panel of the dialog
        // Top part
        JPanel top = new JPanel();
        top.setBorder( new EtchedBorder() );
        top.setLayout( new GridLayout(6,1) );

        // Create a title in BOLD with a trace button
        JLabel optsLabel = new JLabel("Session Options:    ", SwingConstants.LEFT);
        Font f = optsLabel.getFont();
        optsLabel.setFont( new Font( f.getName(), Font.BOLD, f.getSize() + 1 ) );
        trace = new JButton("Trace(Start)");
        trace.addActionListener( this );
        
        JPanel optsLabelAndTrace = new JPanel();
        optsLabelAndTrace.setLayout( new FlowLayout( FlowLayout.LEFT) );
        optsLabelAndTrace.add( optsLabel );
        optsLabelAndTrace.add( trace );
        

        // Create a Client Identifier field
        JPanel cid = new JPanel();
        cid.setLayout( new FlowLayout( FlowLayout.LEFT) );

        cid.add( new JLabel("Client Identifier:", SwingConstants.LEFT) );
        cid.add(clientId);
        
        // Create clean session and keep alive options
        JPanel clnSess = new JPanel();
        clnSess.setLayout( new FlowLayout( FlowLayout.LEFT ) );
        clnSess.add( new JLabel("Clean Session:", SwingConstants.LEFT) );
        clnSess.add( cleanSession );
                
        // Create retry interval and keep alive options
        JPanel timeouts = new JPanel();
        timeouts.setLayout( new FlowLayout( FlowLayout.LEFT ) );

        timeouts.add( new JLabel( "Keep Alive:", SwingConstants.LEFT ) );
        keepAlive = new JTextField(4);
        keepAlive.setText(KEEP_ALIVE);
        timeouts.add( keepAlive );

        timeouts.add( new JLabel( " Retry Interval:", SwingConstants.LEFT ) );
        retryInterval = new JTextField(4);
        retryInterval.setText(RETRY_INT);
        timeouts.add( retryInterval );
        timeouts.add( new JLabel( "seconds", SwingConstants.LEFT ) );
         
        // Create a log file name field
        JPanel log = new JPanel();
        log.setLayout( new FlowLayout( FlowLayout.LEFT) );

        log.add( new JLabel("Use persistence:", SwingConstants.LEFT) );
        log.add(persistEnable);
        persistDir = new JTextField( props.getProperty("PersistenceDir", DEF_PERSIST_DIR), 15);
        persistDir.setToolTipText("Enter the root directory for the log");
        log.add( new JLabel(" Directory:", SwingConstants.LEFT ) );
        log.add( persistDir );
        
        
        // Now add all the components to the top part of the options panel
        top.add( optsLabelAndTrace );
        top.add( cid );
        top.add( clnSess );
        top.add( timeouts );
        top.add( log );

        // Create components required for Last Will and Testament
        // CENTER pane - Last Will and Testament options
        // The CENTER pane of the dialog will contain a BorderLayout with the components laid out as follows:
        //       NORTH - title label, topic and options
        //       CENTER - LW&T message data
        JPanel middle = new JPanel();
        middle.setBorder( new EtchedBorder() );
        middle.setLayout( new BorderLayout() );

        // Create a LW&T topic field        
		lwtTopic = new JTextField( 15 );
        lwtTopic.setMaximumSize( MQTTFrame.TEXT_FIELD_DIMENSION );
        lwtTopic.setMargin( MQTTFrame.TEXT_MARGINS );
						
		// Create a text area for LW&T topic data				
		lwtData = new JTextArea(3,30);
		lwtData.setBorder( new BevelBorder( BevelBorder.LOWERED ) );
        lwtData.setMargin( MQTTFrame.TEXT_MARGINS );
						
		// Create a check box for LW&T retained topic option
        lwtRetain = new JCheckBox();
        lwtRetain.setSelected( false );
        
        // Create a dropdown box to hold the Last Will & Testament QoS options.
        lwtQoS = new JComboBox( qos );
        lwtQoS.setSelectedIndex( 0 );
		lwtQoS.setMaximumSize( MQTTFrame.DROP_DOWN_DIMENSION );

        // Create a title in BOLD
        JLabel lwtLabel = new JLabel("Last Will and Testament Settings:", SwingConstants.LEFT);
        Font fnt = lwtLabel.getFont();
        lwtLabel.setFont( new Font( fnt.getName(), Font.BOLD, fnt.getSize() + 1 ) );

        // Create a panel and add the LW&T topic and retained option to it
        JPanel lwtTopicOpts = new JPanel();
        lwtTopicOpts.setLayout( new BoxLayout( lwtTopicOpts, BoxLayout.X_AXIS) );

        lwtTopicOpts.add( new JLabel("Topic:") );
        lwtTopicOpts.add( lwtTopic );
        lwtTopicOpts.add( new JLabel(" QoS:") );
        lwtTopicOpts.add( lwtQoS );
        lwtTopicOpts.add( new JLabel(" Retained:") );
        lwtTopicOpts.add( lwtRetain );

        // Now add all the components for the LW&T NORTH panel to one panel
        JPanel lwtNorth = new JPanel();
        lwtNorth.setLayout( new GridLayout(2,1) );
        lwtNorth.add( lwtLabel );
        lwtNorth.add( lwtTopicOpts );
        
        // Finally construct the LW&T panel
        middle.add( lwtNorth, BorderLayout.NORTH );
        middle.add( new JScrollPane(lwtData), BorderLayout.CENTER );

        // Create button components
        // SOUTH pane - Close and reset buttons
        JPanel bottom = new JPanel();
        bottom.setBorder( new EtchedBorder() );
        bottom.add( reset );

        // Now put the complete connection options panel together
        connOptions.add(top, BorderLayout.NORTH );
        connOptions.add(middle, BorderLayout.CENTER );
        connOptions.add(bottom, BorderLayout.SOUTH );

	}

    /**
     * @return Is the clean session check box selected?
     */	
	public boolean isCleanSessionSelected() {
		return cleanSession.isSelected();
	}	
	
	/**
	 * @return the client id specified in the text field
	 */
	public String getClientID() {
		return clientId.getText();
	}	

    /**
     * @return Is the use persistence check box selected?
     */	
    public boolean isPersistenceSelected() {
    	return persistEnable.isSelected();
    }
    
	/**
	 * @return the persistence directory specified in the text field
	 */
    public String getPersistenceDirectory() {
    	return persistDir.getText();
    }

    /**
     * @return This method validates and returns the keep alive interval
     * specified in the text field.
     */
    public short getKeepAlive() {
    	Integer i;
    	try {
    		i = new Integer( keepAlive.getText() );
    	} catch ( NumberFormatException e) {
    		// Invalid entry set - use the default
    		keepAlive.setText(KEEP_ALIVE);
    		return (short)30;
    	}		
    	return i.shortValue();
    }

    /**
     * @return This method validates and returns the retry interval
     * specified in the text field.
     */
    public int getRetryInterval() {
    	Integer i;
    	try {
    		i = new Integer( retryInterval.getText() );
    	} catch ( NumberFormatException e) {
    		// Invalid entry set - use the default
    		retryInterval.setText(RETRY_INT);
    		return (int)10;
    	}		
    	return i.intValue();
    }

    /**
     * @return Is the Last Will and Testament retain flag check box selected?
     */	
	public boolean isLWTRetainSelected() {
		return lwtRetain.isSelected();
	}	

    /**
     * @return Is the Last Will and Testament topic set to something other than the empty string?
     */	
	public boolean isLWTTopicSet() {
		// See if the set text equals "" and return the inverse
		return !(lwtTopic.getText().equals(""));
	}	
	
	/**
	 * @return the Last Will and Testament data specified in the text area
	 */
	public String getLWTData() {
		return lwtData.getText();
	}	

	/**
	 * @return the Last Will and Testament topic specified in the text field
	 */
    public String getLWTTopic() {
    	return lwtTopic.getText();
    }
    	
	/**
	 * @return the Last Will and Testament Quality of Service specified in the drop down.
	 */
    public int getLWTQoS() {
    	return lwtQoS.getSelectedIndex();
    }

    /**
     *  ActionListener interface<BR>
     * Listen out for the following button events:
     * <UL><LI>Close button pressed - action: close the dialog</LI>
     *     <LI>Trace(Start) button pressed - action: Start trace and toggle the button to being the stop button</LI>
     *     <LI>Trace(Stop) button pressed - action: Stop the trace and toggle the button to being the start button</LI>
     *     <LI>Default button pressed - action: Reset all fields in the dialog to their default values</LI></UL>
     */
	public void actionPerformed(ActionEvent e) {
		if ( e.getActionCommand().equals("Trace(Start)") ) {    
   			try {
     			mqisdpMgr.startTrace();
     			trace.setText("Trace(Stop)");
   			} catch ( Exception ex ) {
   			}	
   		} else if ( e.getActionCommand().equals("Trace(Stop)") ) {    
   			mqisdpMgr.stopTrace();
   			trace.setText("Trace(Start)");
   		} else {
   			// Reset button has been pressed
   			// Reset all the fields to their default values
   			cleanSession.setSelected(true);
   			clientId.setText( CLIENT_ID );
   			keepAlive.setText(KEEP_ALIVE);
   			lwtTopic.setText("");
   			lwtData.setText("");
   			lwtRetain.setSelected(false);
   			lwtQoS.setSelectedIndex(0);
   			persistEnable.setSelected( false );
   			persistDir.setText( DEF_PERSIST_DIR );
   		}	
	}	
	
}
