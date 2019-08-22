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
 * Description: Displays history such as publications received
 *
 * Contributors:
 *    Ian Harwood, Ian Craggs - initial API and implementation and/or initial documentation
 ********************************************************************************/
package org.eclipse.paho.sample.utility;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;

/**
 * MQTTHist is a simple dialog window which displays a history of messages received.
 */
public class MQTTHist extends JDialog implements ActionListener, Runnable {
    private ConnOpts connOptions;
    private final JTextArea histData;
    private JScrollPane scroller; // Remember the scroll pane object so that we can auto scroll
    private boolean running = true;
    private boolean logEnabled = true;
    private JButton close = null; // close button
    private JFrame owner = null;
    
	/**
	 * Constructor for MQTTHist.
	 * @param theOwner The frame owner
	 * @param connOptions the COnnOpts panel which started this dialog
	 */
	public MQTTHist( JFrame theOwner, ConnOpts connOptions ) {

		super( theOwner, "WMQTT Client History" );
		
		this.owner = theOwner;		
        this.connOptions = connOptions;
        
        // Get the container for this dialog and set the size and layout manager		
        Container histDialog = this.getContentPane();
        histDialog.setLayout( new BorderLayout() );
        
        // Clear button
        // Add an actionlistener and tooltip to the button
        JButton clear = new JButton("Clear");
        clear.addActionListener(this);
        clear.setToolTipText( "Clear history dialog" );

        // Enable/Disable button
        // Add an actionlistener and tooltip to the button
        close = new JButton("Close");
        close.addActionListener(this);
        close.setToolTipText( "Close history log" );

        // Create button components
        // SOUTH pane - Close and reset buttons
        JPanel bottom = new JPanel();
        bottom.setBorder( new EtchedBorder() );
        bottom.add( clear );
        bottom.add( close );

		// Create a text area for history data
		histData = new JTextArea(3,30);
		histData.setBorder( new BevelBorder( BevelBorder.LOWERED ) );
		histData.setEditable( false );
        histData.setMargin( MQTTFrame.TEXT_MARGINS );

        // Now put the complete panel together
        scroller = new JScrollPane(histData);
        histDialog.add(scroller, BorderLayout.CENTER );
        histDialog.add(bottom, BorderLayout.SOUTH );

        setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
        
        // Start up the run method of this object as a thread to manage
        // autoscrolling the log history window        
        new Thread(this).start();

	}

    /**
     * Size the window and positioning it relative to the
     * parent frame.
     */
	private void defaultDialogSize( JFrame owner ) {
        this.setBounds( owner.getX() + owner.getWidth(),
                         owner.getY(),
                         MQTTFrame.FRAME_WIDTH,
						 MQTTFrame.FRAME_HEIGHT );
	}	

	/**
     * Terminate the thread that autoscrolls the text.
     */	
	public void close() {
		running = false;
    	// Notify the autoscroll thread that it is time to exit
    	synchronized(histData) {
    		histData.notify();
    	}	
	}	

    /**
     * Append text to the log, repaint the window and notify the autoscroll thread to scroll
     * to the end of the text area.
     * @param logData A string of text to append to the log.
     */
    public synchronized void write( String logData ) {
    	if ( logEnabled ) {
    		histData.append( logData );

    		// Tell the ScrollPane to sort itself out in terms of resizing the scrollbars    	
    		histData.revalidate();
    	
    		// Notify the autoscroll thread to scroll the window
    		synchronized(histData) {
    			histData.notify();
    		}
    	}	
    }
            	
	/**
	 * ActionListener interface<BR>
	 * Listen out for close and clear button events.
	 * @param e The button event to react to.
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if ( e.getActionCommand().equals("Clear") ) {   			
            histData.setText("");
   		} else if ( e.getActionCommand().equals("Close") ) {
   			// Log is being enabled / disabled
   			disableHistory();
   		}
	}

    /**
     * This run method runs in a separate thread created by this class and
     * waits to be notified that it needs to scroll the window. If no data is being
     * written to the log then it can be scrolled up and down by the user
     * without being continuously autoscrolled to theby this thread.
     */	
	public void run() {
       	JScrollBar jsb = scroller.getVerticalScrollBar();
		while(running) {
			try {
    			synchronized(histData) {
	    			histData.wait();
		    	}	
		    	// Take a short nap after being notified to
		        // allow the swing components to revalidate themselves
                Thread.sleep(100);
			} catch(Exception e) {
				// Don't mind if an interrupt occurs
			}		
				
        	// Autoscroll the text area to the bottom
            jsb.setValue( jsb.getMaximum() );

		}	
	}	
	
	public void enableHistory() {
		defaultDialogSize( owner );
		logEnabled = true;
		setVisible( true );
	}	

	public void disableHistory() {
		logEnabled = false;
		setVisible( false );
	}	
}

