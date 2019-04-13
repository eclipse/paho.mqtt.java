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
 * Description: Controls and lays out the publication panel on the main window
 *
 * Contributors:
 *    Ian Harwood, Ian Craggs - initial API and implementation and/or initial documentation
 ********************************************************************************/

package org.eclipse.paho.sample.utility;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This class creates a panel that contains all the components to do with
 * sending publications.<P>
 * <B>File input</B><BR>
 * When input is read from a file the input is stored in a separate buffer called fileContent
 * as the file contents may not be diplayable in the JTextArea (e.g. a binary file). When a file is read
 * the contents are written to the JTextArea, although it is the fileContent byte array that will get published,
 * not the window contents.<BR>
 * To know when the fileContent array is out of date (i.e the JTextArea data has been modified) a DocumentListener is
 * added to the JTextArea upon reading the file. The first change event from the JTextArea clears the fileContent array
 * and removes the DocumentListener, so that data is now read directly from the JTextArea again.
 */
public class PubPanel implements ActionListener, DocumentListener {
	private JPanel pubPanel;
	private JComboBox topic;
	private Integer[]  qos = { Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2) };
	private JComboBox qosList;
	private JButton pubButton;
    private JButton fileButton;
    private JButton hexButton;
    private boolean hexDisplay = false;
	private JTextArea pubData;
	private JCheckBox retained;
	private MQTTFrame mqttMgr = null;
	private JLabel pubLabel = null;
	private static final String PANEL_TITLE = " Publish Messages";
	private File fileChooserCurrentDir = null;
	private byte[] fileContent = null;
		
    /**
     * The constructor for the publication panel
     * @param thePubPanel The JPanel component into which all other components are placed
     * @param aMqttMgr The MQIsdpFrame object through which WMQtt communication is done.
     */        
	public PubPanel( JPanel thePubPanel, MQTTFrame aMqttMgr ) {
		pubPanel = thePubPanel;
        pubPanel.setBorder( new EtchedBorder() );

        mqttMgr = aMqttMgr;
        
        init();
        
	}	

    /**
     * The init method builds all the required components and adds them to the
     * publication panel. Different layout managers are used throughout to ensure that the components
     * resize correctly as the window resizes.
     */	
	public void init() {
		pubPanel.setLayout(new BorderLayout() );
        
		topic = new JComboBox();
		topic.setEditable( true );
        topic.setMaximumSize(MQTTFrame.TEXT_FIELD_DIMENSION);
        topic.setMaximumRowCount(5);
		
        qosList = new JComboBox( qos );
		qosList.setSelectedIndex( 0 );
		qosList.setMaximumSize( MQTTFrame.DROP_DOWN_DIMENSION );
				
		pubData = new JTextArea(3,30);
		pubData.setBorder( new BevelBorder( BevelBorder.LOWERED ) );
        pubData.setMargin( MQTTFrame.TEXT_MARGINS );
								
        retained = new JCheckBox();
        retained.setSelected( false );

        // Create the components to go in the NORTH panel
        // Create the title label for the pubPanel
        pubLabel = new JLabel( PANEL_TITLE  + " - text display" );
        Font f = pubLabel.getFont();
        pubLabel.setFont( new Font( f.getName(), Font.BOLD, f.getSize() + 1 ) );

        // To add a title to the panel place the Topic label, topic text field, qos combo box and retained check box in a horizontal layout
        // Then place this horizontal layout below the title "Publish Messages"
        JPanel topicBox = new JPanel();
        topicBox.setLayout( new BoxLayout( topicBox, BoxLayout.X_AXIS ) );
        topicBox.add( new JLabel(" Topic:") );
        topicBox.add( topic );
        topicBox.add( new JLabel(" QoS:") );
        topicBox.add( qosList );
        topicBox.add( new JLabel(" Retained:") );
        topicBox.add( retained );
        
        // Add everything to the panel that will be inserted into the NORTH of the pubPanel
        JPanel titleAndTopic = new JPanel();
        titleAndTopic.setLayout( new GridLayout(2,1) );
        titleAndTopic.add( pubLabel );
        titleAndTopic.add( topicBox );

        // Create the components to go in the EAST panel
        // Add the button to a FlowLayout to stop it resizing
        hexDisplay = false;
        hexButton = new JButton( "Hex" );
        hexButton.addActionListener( this );
        
		fileButton = new JButton( "File..." );
		fileButton.setEnabled(true);
		fileButton.addActionListener( this );

		pubButton = new JButton( "Publish" );
		pubButton.setEnabled(false);
		pubButton.addActionListener( this );

        JPanel buttons = new JPanel();
        buttons.setLayout( new GridLayout(3,1) );
        
        buttons.add( pubButton );
        buttons.add( fileButton );
        buttons.add( hexButton );
        		
		JPanel buttonLayout = new JPanel();
		buttonLayout.add( buttons );

        // Now add the title and topic options, data text area and the publish button to the pubPanel
        pubPanel.add( titleAndTopic, BorderLayout.NORTH );
        pubPanel.add( new JScrollPane(pubData), BorderLayout.CENTER );
        pubPanel.add( buttonLayout, BorderLayout.EAST );
	}	

    /**
     * For any requests to add a topic to the subscribe drop down box
     * use the updateComboBoxList method in class MQTTFrame to do the job.
     */
    public boolean updateTopicList( String topicName ) {
        return mqttMgr.updateComboBoxList( topic, topicName );
    }
    	    		
    /**
     * ActionListener interface<BR>
     * Listen out for the Publish button, the Hex/Text button being pressed or the File button being pressed
     * <BR>Publishing data involves:<BR>
     * <UL><LI>Converting the data to a character representation if necessary from hex
     * <LI>Updating the drop down boxes with the topic if necessary
     * <LI>Publishing the data
     * <LI>Converting the data back to a hex representation if necessary
     * <LI>Writing a log entry
     * </UL>
     * <BR>Processing the File button involves reading the file contents into a buffer ready for publishing. A DocumentListener
     * is added to the JTextArea pubData so that we know when the file content buffer becomes out of date.
     * <BR>Processing the Hex/Text button presses involves converting the display between a text character representation
     * and hexadecimal character representation.
     */	    		  
    public void actionPerformed( ActionEvent e ) {
    	// Remember the display state (hex or text) of the publish JTextArea
    	boolean hexData = hexDisplay;
    	boolean pubSuccess = false;
    	
    	if ( e.getActionCommand().equals("Publish") ) {
    		if ( hexDisplay == true ) {
    			// Convert the display to the characters we want to send
    			toCharString();
    		}	
        	String pubText = pubData.getText();
        	String topicName = (String)topic.getSelectedItem();
    	
    	    if ( updateTopicList( topicName ) ) {
    		    // If we needed to update this list, then update the subscriber panel's topic list
        		mqttMgr.updateSubscribeTopicList( topicName );
        	}
        	
        	Exception pubExp = null;
        	try {
        		// Publish the fileContent or the JTextArea contents. The file contents are the most
        		// current if fileContent is not null.
                mqttMgr.publish( topicName,
                				   (fileContent == null) ? pubText.getBytes() : fileContent,
                				   qosList.getSelectedIndex(),
                				   retained.isSelected() );
                pubSuccess = true;
        	} catch( Exception ex) {
        		// Publish failed
        		pubSuccess = false;
        		pubExp = ex;
        	}	    
            
         	// If the display was originally in hex then switch back to hex
        	if ( hexData ) {
     	    	toHexString();
         	}	
     	
    	    synchronized(mqttMgr) { // Grab the log synchronisation lock
         	    if ( pubSuccess ) {
                 	// When writing the data to the log get it from the receivedData text area, so that it is in the correct format - Hex or Text
                    mqttMgr.writeLogln( "  --> PUBLISH sent,     TOPIC:" + topicName + ", QoS:" + qosList.getSelectedIndex() + ", Retained:" + retained.isSelected() );
        	    } else {
                    mqttMgr.writeLogln( " *--> PUBLISH send FAILED, TOPIC:" + topicName + ", QoS:" + qosList.getSelectedIndex() + ", Retained:" + retained.isSelected() );
     	        }		

            	mqttMgr.writeLog(   "                        DATA:" );
        	    if ( hexData ) {
        		    // Prefix hex data with 0x
                	mqttMgr.writeLog( "0x" );
        	    }	
            	mqttMgr.writeLogln( pubData.getText() );
 
                if ( !pubSuccess ) {
                    mqttMgr.writeLogln( "                   EXCEPTION:" + pubExp.getMessage() );
     	        }		
    	    }    

    	} else if ( e.getActionCommand().equals("File...") ) {
    		JFileChooser selectFile = new JFileChooser( fileChooserCurrentDir );
    		selectFile.setMultiSelectionEnabled( false );
    		if ( selectFile.showOpenDialog( pubPanel ) == JFileChooser.APPROVE_OPTION ) {
    			fileChooserCurrentDir = selectFile.getCurrentDirectory();
    			File theFile = selectFile.getSelectedFile();

                fileContent = new byte[ (int)theFile.length() ];    			
                
    			try { 
	    			FileInputStream input = new FileInputStream( theFile );
	    			input.read( fileContent );

	                // Display the file content as string data and listen for changes to it occuring
    	            pubData.setText( new String(fileContent) );
        	        pubData.getDocument().addDocumentListener( this );
        	        
    			} catch( FileNotFoundException fnfe ) {
    				JOptionPane.showMessageDialog( pubPanel, fnfe.getMessage(), "File Open Error", JOptionPane.ERROR_MESSAGE );
    			} catch( IOException ioe ) {
    				JOptionPane.showMessageDialog( pubPanel, ioe.getMessage(), "File Open Error", JOptionPane.ERROR_MESSAGE );
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
     * This method reads in the data from the publish text area as text characters and converts them into hex characters (i.e. every character read is represented as two hex characters). It is used when the button
     * saying 'Hex' is pressed indicating that the text data in the data area needs to be converted to a hex representation.<BR>
     * The text string read in from the data area is converted into an array of bytes. The integer value of each byte is then converted into a hex string representation and append to the output string buffer. Once the entire input string
     * has been processed the output string is written back to the text area.
     */
    private void toHexString() {
    	String pubText = pubData.getText();
    	StringBuffer hexText = new StringBuffer();
    	
    	byte[] pubBytes = pubText.getBytes();

    	for( int i=0; i<pubBytes.length; i++ ) {
    		int byteValue = pubBytes[i];

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
        pubLabel.setText( PANEL_TITLE + " - hexadecimal display" );
        mqttMgr.setTitleText("");
        hexButton.setText("Text");
    	pubData.setText( hexText.toString() );
    }	
    
    /**
     * This method reads in the data from the publish text area as hex and converts it into a text string (i.e. every two characters read are treated as hex and represent one char). It is used when the button
     * saying 'Text' is pressed indicating that the hex data in the data area needs to be converted to character data.<BR>
     * Error conditions checked for include an odd number of hex characters and invalid base 16 characters (not in [0..9],[A..F]).<BR>
     * The hex input string is converted into a text character array then a new text string is generated from the character array and set as the string in the publication data area.
     */
    private void toCharString() {
    	String hexText = pubData.getText();
    	
    	if ( hexText.length() % 2 != 0 ) {
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
                pubLabel.setText( PANEL_TITLE + " - text display" );
                mqttMgr.setTitleText("");
                hexButton.setText("Hex");
    		    pubData.setText( new String(charArray) );
    		} catch( NumberFormatException nfe ) {
    			mqttMgr.setTitleText( "Invalid hexadecimal data!" );
    		}	    
    	}	
    }	

    /**
     * This enables or disables the publish button depending on the value of the boolean.
     * @param b Button enabled if true, otherwise disabled.
     */
    public void enableButtons( boolean b ) {
   		pubButton.setEnabled( b );
    }	

    // DocumentListener Interface
    /**
     * DocumentListener - changedUpdate
     * The JTextArea has been modified. Null the file contents
     * and stop listening for document changes.
     * A new listener will be added the next time content is read from a file.
     */
    public void changedUpdate( DocumentEvent de ) {
    	fileContent = null;
    	pubData.getDocument().removeDocumentListener( this );
    }	
    
    /**
     * DocumentListener - insertUpdate
     * Performs the same functionality as changedUpdate above.
     */
    public void insertUpdate( DocumentEvent de ) {
    	changedUpdate( de );
    }	

    /**
     * DocumentListener - removeUpdate
     * Performs the same functionality as changedUpdate above.
     */
    public void removeUpdate( DocumentEvent de ) {
    	changedUpdate( de );
    }	
}
