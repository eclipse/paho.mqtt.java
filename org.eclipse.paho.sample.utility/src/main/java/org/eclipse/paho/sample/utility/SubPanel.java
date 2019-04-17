/********************************************************************************
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
 * Description: Controls and lays out the subscription panel on the main window
 *
 * Contributors:
 *    Ian Harwood, Ian Craggs - initial API and implementation and/or initial documentation
 ********************************************************************************/

package org.eclipse.paho.sample.utility;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;

/**
 * This class creates a panel that contains all the components to do with
 * subscribing for and receiving publications.
 */
public class SubPanel implements ActionListener {
	private JPanel subPanel;
	private JComboBox topic;
	private JTextField receivedTopic;
	private JTextField receivedQoS;
	private JCheckBox  receivedRetain;
	private JTextArea receivedData;
	private MQTTFrame mqttMgr = null;
	private Integer[]  qos = { Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2) };
	private JComboBox qosList;
	private boolean hexDisplay = true;
    private JLabel subLabel = null;
    private JButton hexButton = null;
    private JButton fileButton;
    private JButton subButton;
    private JButton unsubButton;
    private static final String PANEL_TITLE = " Subscribe To Topics";
	private File fileChooserCurrentDir = null;
	private byte[] fileContent = null;
    
    /**
     * The constructor for the subscription panel
     * @param theSubPanel The JPanel component into which all other components are placed
     * @param aMqttMgr The MQIsdpFrame object through which WMQtt communication is done.
     */        
	public SubPanel( JPanel theSubPanel, MQTTFrame aMqttMgr ) {
		subPanel = theSubPanel;
        subPanel.setBorder( new EtchedBorder() );

        mqttMgr = aMqttMgr;
        
        init();
	}	
	
    /**
     * The init method builds all the required components and adds them to the
     * subscription panel. Different layout managers are used throughout to ensure that the components
     * resize correctly as the window resizes.
     */	
	public void init() {
		subPanel.setLayout( new BorderLayout() );

        // Create field to specify topics to subscribe to / unsubscribe from
        // Set a maximum size for it to stop it sizing vertically
		topic = new JComboBox();
		topic.setEditable( true );
        topic.setMaximumSize(MQTTFrame.TEXT_FIELD_DIMENSION);
        topic.setMaximumRowCount(5);

        // Create fields to display the topic, QoS and retained flag for which data has been received
        // Set a maximum size for it to stop it sizing vertically
		receivedTopic = new JTextField( 15 );
        receivedTopic.setMaximumSize(MQTTFrame.TEXT_FIELD_DIMENSION);
		receivedTopic.setEditable( false );
		receivedTopic.setBackground( Color.lightGray );
        receivedTopic.setMargin( MQTTFrame.TEXT_MARGINS );
        
        receivedQoS = new JTextField( 3 );
        receivedQoS.setMaximumSize( MQTTFrame.DROP_DOWN_DIMENSION );
        receivedQoS.setEditable( false );
        receivedQoS.setBackground( Color.lightGray );
        receivedQoS.setMargin( MQTTFrame.TEXT_MARGINS );

        receivedRetain = new JCheckBox();
        receivedRetain.setMaximumSize( MQTTFrame.DROP_DOWN_DIMENSION );
        receivedRetain.setEnabled( false );
        receivedRetain.setMargin( MQTTFrame.TEXT_MARGINS );
        receivedRetain.setSelected( false );

        // Create a text area to display received data in		
		receivedData = new JTextArea(3,30);	
		receivedData.setBorder( new BevelBorder( BevelBorder.LOWERED ) );
		receivedData.setEditable( false );
		receivedData.setBackground( Color.lightGray );
        receivedData.setMargin( MQTTFrame.TEXT_MARGINS );

		// Create a JComboBox to display the delivery QoS for a subscription
        qosList = new JComboBox( qos );
		qosList.setSelectedIndex( 0 );
		qosList.setMaximumSize( MQTTFrame.DROP_DOWN_DIMENSION );
		qosList.setToolTipText("The QoS at which publications will be delivered");

        // Create the components to go in the NORTH panel
        // Create a horizontal box layout in which to display the topic sent and its label
        JPanel sendTopicPanel = new JPanel();
        sendTopicPanel.setLayout( new BoxLayout( sendTopicPanel, BoxLayout.X_AXIS) );
        sendTopicPanel.add( new JLabel(" Subscribe Topic:") );
        sendTopicPanel.add( topic );
        sendTopicPanel.add( new JLabel(" Request QoS:") );
        sendTopicPanel.add( qosList );
        // Keep the qoslist from being right up against the edge of the window by adding some spaces
        sendTopicPanel.add( new JLabel("  ") );

        // Create a horizontal box layout in which to display the topic rcvd and its label
        JPanel rcvdTopicPanel = new JPanel();
        rcvdTopicPanel.setLayout( new BoxLayout( rcvdTopicPanel, BoxLayout.X_AXIS ) );
        rcvdTopicPanel.add( new JLabel(" Received Topic:") );
        rcvdTopicPanel.add( receivedTopic );
        rcvdTopicPanel.add( new JLabel(" QoS:") );
        rcvdTopicPanel.add( receivedQoS );
        rcvdTopicPanel.add( new JLabel(" Retained:") );
        rcvdTopicPanel.add( receivedRetain );
        
        // Add some white space in to stop the receivedTopic field being sized to the whole width of the GUI
        // The white space occupies the same space as the Request QoS JLabel, so the sent and rcvd topic fields line up reasonably well
        // rcvdTopicPanel.add( new JLabel("                                       ") );

        // Now to incorporate a title into the panel we place the two topic fields in a vertical
        // layout with the title "Subscribe To Topics"
        JPanel titleAndTopics = new JPanel();
        titleAndTopics.setLayout( new GridLayout( 3, 1 ) );
        
        // Add a title in BOLD        
        subLabel = new JLabel( PANEL_TITLE + " - text display");
        Font f = subLabel.getFont();
        subLabel.setFont( new Font( f.getName(), Font.BOLD, f.getSize() + 1 ) );
        
        // Add all the fields that will make up the NORTH panel
        titleAndTopics.add( subLabel );
        titleAndTopics.add( sendTopicPanel );
        titleAndTopics.add( rcvdTopicPanel );
        
        // Create the components to go in the EAST panel
        // Add the buttons to a FlowLayout to stop them resizing
		JPanel subButtonsLayout = new JPanel();
        JPanel buttons = new JPanel();
        buttons.setLayout( new GridLayout(4,1) );
        
        hexDisplay = false;
        hexButton = new JButton( "Hex" );
        hexButton.addActionListener( this );

		fileButton = new JButton( "Save..." );
		fileButton.setEnabled(true);
		fileButton.addActionListener( this );

    	subButton = new JButton( "Subscribe" );
    	subButton.setEnabled(false);
		subButton.addActionListener( this );

   	    unsubButton = new JButton( "Unsubscribe" );
    	unsubButton.setEnabled(false);
		unsubButton.addActionListener( this );
        
        buttons.add( subButton );
        buttons.add( unsubButton );
        buttons.add( fileButton );
        buttons.add( hexButton );

		subButtonsLayout.add( buttons );

        // Add the Title and Topic fields to the NORTH panel
        // Add the received publications text area to the CENTER
        // Add the Subscribe and Unsubscribe buttons to the EAST       
        subPanel.add( titleAndTopics, BorderLayout.NORTH );
        subPanel.add( new JScrollPane(receivedData), BorderLayout.CENTER );
        subPanel.add( subButtonsLayout, BorderLayout.EAST );

   	}	
		
    /**
     * For any requests to add a topic to the publish drop down box
     * use the updateComboBoxList method in class MQIsdpFrame to do the job.
     */
    public boolean updateTopicList( String topicName ) {
        return mqttMgr.updateComboBoxList( topic, topicName );
    }

    /**
     * ActionListener interface<BR>
     * Listen out for the Subscribe button, Unsubscribe button, Save button or the Hex/Text button being pressed
     * <BR>Subscribing / Unsubscribing for data involves:<BR>
     * <UL><LI>Updating the drop down boxes with the topic if necessary
     * <LI>Subscribing / Unsubscribing for the data
     * </UL>
     * <BR>Processing the Save button involves poping up a file dialog, opening the file, writing the data and closing the file.
     * <BR>Processing the Hex/Text button presses involves converting the display between a text character representation
     * and hexadecimal character representation.
     */	    		  
    public void actionPerformed( ActionEvent e ) {
    	String topicName = (String)topic.getSelectedItem();
    	
    	if ( updateTopicList( topicName ) ) {
    		// If we needed to update this list, then update the publisher panel's topic list
    		mqttMgr.updatePublishTopicList( topicName );
    	}
    	
    	if ( e.getActionCommand().equals( "Subscribe" ) ) {
            // Subscribe
            mqttMgr.subscription( topicName, qosList.getSelectedIndex(), true );
        } else if ( e.getActionCommand().equals("Unsubscribe") ){
            // Unsubscribe
            mqttMgr.subscription( topicName, 0, false );
    	} else if ( e.getActionCommand().equals("Save...") ) {
    		JFileChooser selectFile = new JFileChooser( fileChooserCurrentDir );
    		selectFile.setMultiSelectionEnabled( false );
    		if ( selectFile.showSaveDialog( subPanel ) == JFileChooser.APPROVE_OPTION ) {
    			fileChooserCurrentDir = selectFile.getCurrentDirectory();
    			File theFile = selectFile.getSelectedFile();

				FileOutputStream output = null;
    			try { 
	    			output = new FileOutputStream( theFile );
	    			output.write( fileContent );
    			} catch( FileNotFoundException fnfe ) {
    				JOptionPane.showMessageDialog( subPanel, fnfe.getMessage(), "File Save Error", JOptionPane.ERROR_MESSAGE );
    			} catch( IOException ioe ) {
    				JOptionPane.showMessageDialog( subPanel, ioe.getMessage(), "File Save Error", JOptionPane.ERROR_MESSAGE );
    			}	
    			
    			// Now close the file if we can
    			try {
    				if ( output != null ) { 
		    			output.close();
    				}	
    			} catch( IOException ioe ) {
    			}		
    		}	
        } else {
    		if ( hexDisplay == false ) {
        		toHexString();
    		} else {
        		toCharString();
        	}	
    	}	
    }    
    
    /**
     * This method is passed a received publication. It then:
     * <UL><LI>Switches the display to text if was in hex display mode
     * <LI>Updates the display with the new data
     * <LI>Switches the display back to hex if was originally in hex display mode
     * <LI>Writes a log entry to the history window
     * </UL>
     */
    public void updateReceivedData( String topic, byte[] data, int QoS, boolean retained ) {
    	// Remember the display state (hex or text) of the subscribe JTextArea
    	boolean writeHex = hexDisplay;
    	
    	// Switch to a character display before adding text
    	if ( hexDisplay == true ) {
        	toCharString();
    	}	
    	
    	receivedTopic.setText( topic );
    	receivedQoS.setText( Integer.toString(QoS) );
    	receivedRetain.setSelected( retained );
     	receivedData.setText( new String(data) );
     	
     	// Store the data content in a buffer incase in needs to be written to a file
     	// If the data is binary reading it back from the receivedData field is not good enough
     	fileContent = data.clone();
     	
     	
     	// If the display was originally in hex then switch back to hex
     	if ( writeHex ) {
     		toHexString();
     	}	
     	
     	// When writing the data to the log get it from the receivedData text area, so that it is in the correct format - Hex or Text
   	    synchronized(mqttMgr) { // Grab the log synchronisation lock
         	mqttMgr.writeLogln( "  --> PUBLISH received, TOPIC:" + topic + ", QoS:" + QoS + ", Retained:" + retained );
        	mqttMgr.writeLog( "                        DATA:" );
    	    if ( writeHex ) {
        		// Prefix hex data with 0x
            	mqttMgr.writeLog( "0x" );
    	    }	
        	mqttMgr.writeLogln( receivedData.getText() );
   	    }	
     	
    }	
    
    /**
     * This method reads in the data from the received publication text area as text characters and converts them into hex characters (i.e. every character read is represented as two hex characters). It is used when the button
     * saying 'Hex' is pressed indicating that the text data in the data area needs to be converted to a hex representation.<BR>
     * The text string read in from the data area is converted into an array of bytes. The integer value of each byte is then converted into a hex string representation and appended to the output string buffer. Once the entire input string
     * has been processed the output string is written back to the text area.
     */
    private void toHexString() {
    	String subText = receivedData.getText();
    	StringBuffer hexText = new StringBuffer();
    	
    	byte[] subBytes = subText.getBytes();

    	for( int i=0; i<subBytes.length; i++ ) {
    		int byteValue = subBytes[i];

            // Change the byte value from a signed to unsigned value
            // e.g. A byte of value 0xAA is treated as -86 and displayed incorrectly as 0xFFFFFFAA
            // Adding 256 to this value changes it to 170 which is displayed correctly as 0xAA
    		if (byteValue < 0) {
    			byteValue += 256;
    		}

			if (byteValue < 16) {
				hexText.append("0").append(Integer.toHexString(byteValue));
			} else {
				hexText.append(Integer.toHexString(byteValue));
			}
		}
    	
        hexDisplay = true;
        subLabel.setText( PANEL_TITLE + " - hexadecimal display" );
        mqttMgr.setTitleText("");
        hexButton.setText("Text");
    	receivedData.setText( hexText.toString() );
    }	
    
    /**
     * This method reads in the data from the received publications text area as hex and converts it into a text string (i.e. every two characters read are treated as hex and represent one char). It is used when the button
     * saying 'Text' is pressed indicating that the hex data in the data area needs to be converted to character data.<BR>
     * Error conditions checked for include an odd number of hex characters and invalid base 16 characters (not in [0..9],[A..F]).<BR>
     * The hex input string is converted into a text character array then a new text string is generated from the character array and set as the string in the publication data area.
     */
    private void toCharString() {
    	String hexText = receivedData.getText();
    	
    	if ( hexText.length() % 2 != 0 ) {
    		System.out.println( "Hex length" + hexText.length() );
    		mqttMgr.setTitleText( "Odd number of hex characters!" );
    	} else {
    		try {
                byte[] charArray = new byte[hexText.length()/2];    
        		for( int i=0; i<charArray.length; i++ ) {
                    // Take each pair of bytes from the input and convert to a character			
                    // Use the Integer parseInt method to take a 2 byte string in base 16 and turn it into an integer.
    	    		charArray[i] = (byte)Integer.parseInt( hexText.substring( i*2,(i*2)+2), 16 );
    		    }	

                hexDisplay = false;
                subLabel.setText( PANEL_TITLE + " - text display" );
    			mqttMgr.setTitleText( "" );
                hexButton.setText("Hex");
    		    receivedData.setText( new String(charArray) );
    		} catch( NumberFormatException nfe ) {

    			mqttMgr.setTitleText( "Invalid hexadecimal data!" );
    		}	    
    	}	
    }	
    
    /**
     * This enables or disables the subscribe and unsubscribe buttons depending on the value of the boolean.
     * @param b Button enabled if true, otherwise disabled.
     */
    public void enableButtons( boolean b ) {
   		subButton.setEnabled( b );
   		unsubButton.setEnabled( b );
    }	
}
