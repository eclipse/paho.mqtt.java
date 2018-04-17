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
 * Description: 3D LED
 *              The LED can optionally flash (The run method of
 *              this object must be started in a separate thread).
 *
 * Contributors:
 *    Ian Harwood, Ian Craggs - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.paho.sample.utility;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Panel;
 
/**
 * This class manipulates pixels to display an LED with a glint effect on it.
 * The colour may be selected for the LED and it may be set to flash if required.
 */ 
public class LED extends Panel implements Runnable {

	// define the polyline for the outer border
	private static final int xOutline[] = {2,3,4,8,9,10,10,9,9,8, 7, 3,2,2,1,1,2};
	private static final int yOutline[] = {2,2,1,1,2, 3, 7,8,9,9,10,10,9,8,7,4,3};
	private static final int nOutline = xOutline.length;

	private static final int xHighlight[] = {6,7,7,8,8};
	private static final int yHighlight[] = {8,8,7,7,6};
	private static final int nHighlight = xHighlight.length;

	// define the polyline which is the grey extra highlight
	// when the LED is in its off state
	private static final int xOfflight [] = {3,7,8,9,9,8,8,7,6,5,4};
	private static final int yOfflight [] = {9,9,8,7,3,4,5,6,7,8,8};
	private static final int nOfflight = xOfflight.length;

	private static final Color OfflightColour = new Color(91,91,91);

    // Interval between flashing
	private static final int delay = 500;

	private boolean running = true;
	

    /**
     * An inner class which holds the different colours required for
     * creating the glint and shadow effects for the LED.
     */
	static class Colours
	{
		public Color mainColour;	// main colour
		public Color outerColour;	// darker outline colour
		public Color highlightColour;	// highlight colour
		// default shadow colour for illuminated LED
		public Color shadowColour = new Color(47,0,0);
		// glint colour
		public static final Color glintColour = new Color(255,248,255);	

		Colours(Color main, Color outer, Color highlight)
		{
			mainColour = main;
			outerColour = outer;
			highlightColour = highlight;
		}		
		Colours(Color main, Color outer, Color highlight, Color shadow)
		{
			mainColour = main;
			outerColour = outer;
			highlightColour = highlight;
			shadowColour = shadow;
		}
		
	}


    // Some predefined colours - amber, red and green.
	private static final Colours amberColours = new Colours (
										new Color(240,168,0),
										new Color(95,64,0), 
										new Color(240,224,112));
	
	private static final Colours redColours = new Colours (
										new Color(240,0,0),
										new Color(63,0,0),
										new Color(255,104,111));
	
	private static final Colours greenColours = new Colours (
										new Color(0,176,0),
										new Color(0,48,0),
										new Color(79,248,80));

	// this one uses the 4-way initialiser, including the shadow colour
	private static final Colours offColours = new Colours (
										new Color(25,25,25),
										new Color(25,25,25),
										new Color(166,166,166),
										new Color(17,17,17));

	
	// this is the current state of the LED
	private Colours state;
	// and this is whether or not it is flashing
	private boolean flashing;
	// this is whether or not it is currently off, regardless of the state colour
	private boolean off;
										
/**
 * LED constructor
 * Set the LED to a state of off and set the preferred size
 */
public LED () {
	setOff();
	setSize( 20, 20 );    
}
/**
 * LED constructor - This simply calls the AWT panel constructor.
 * @param layout java.awt.LayoutManager
 */
public LED(java.awt.LayoutManager layout) {
	super(layout);
}
/**
 * Override the Panel paint method with this one.
 */
public void paint(Graphics g) {

	// if the LED is currently off, use the off colours to draw with, otherwise, use the
	// colours as defined in the state object
	Colours draw;
	
	if (off)
		draw = offColours;
	else
		draw = state;

	// draw the main blob of main colour
	g.setColor(draw.mainColour);
	g.fillRect(2,2,8,8);
	
	// draw the polyline of slightly darker main colour that goes round the outside

	g.setColor(draw.outerColour);
	g.drawPolyline(xOutline,yOutline,nOutline);
	
	// draw the drop shadow and the "corners"
	
	g.setColor(draw.shadowColour);
	g.drawLine(6,11,11,11);
	g.drawLine(8,10,11,10);
	g.drawLine(10,9,12,9);
	g.drawLine(10,8,11,8);
	g.drawLine(11,6,11,7);

	g.drawLine(9,12,9,12);
	g.drawLine(3,1,3,1);
	g.drawLine(8,1,8,1);
	g.drawLine(10,3,10,3);
	g.drawLine(3,10,3,10);
	g.drawLine(1,8,1,8);
	g.drawLine(1,3,1,3);
	
	
	// now do the highlight

	g.setColor(draw.highlightColour);
	g.drawLine(3,3,4,4);
	g.drawPolyline(xHighlight,yHighlight,nHighlight);

	// and finally the glint

	g.setColor(Colours.glintColour);
	g.drawLine(3,4,4,3);

	// if the LED is currently "off", also add the extra lowlight

	if (off)
	{
		g.setColor(OfflightColour);
		g.drawPolyline(xOfflight,yOfflight,nOfflight);
	}

}

/**
 * For the LED to flash this run method must be started in a thread
 * by the user of this class. If flashing is not required then this run method
 * does not need to be executed.<P>
 * When the LED is set to flash this method changes the state from off to on and vice-versa using
 * a predefined interval. When the LED is not required to flash and this thread is executing then it waits efficiently
 * until it is woken up by the {@link LED#setFlash setFlash()} method.
 */
public void run() {

	while (running)	{
		try	{
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
		// if we're flashing, flip the state
		// if it's off, turn it on, and vice versa
		if (flashing)
		{
			off = !off;
			repaint();
		} else {
			// Suspend the thread when flashing is not required
			synchronized(this) {
				try {
    				this.wait();
				} catch(Exception e) {
					// Don't care if an interrupt occurs
				}		
			}	
		}	
	}
}
/**
 * Set the LED colour to Amber
 */
public void setAmber() {

	state = amberColours;
	off = false;
	repaint();
}
/**
 * Set the LED colour to a user specified RGB
 *
 * @param r red value
 * @param g green value
 * @param b blue value
 */
public void setColor(int r, int g, int b) {

	
	// arbitrary colour LED
	Color mainColour = new Color(r,g,b);
	Color outerColour = mainColour.darker().darker().darker();
	Color highlightColour = new Color(148,148,148);

	state = new Colours (mainColour, outerColour, highlightColour);
	off = false;
	
	repaint();
}

/**
 * Query the flashing variable to determine if the LED is 
 * currently in a state of flashing or not.
 *
 * @return true if flashing
 */
public boolean isFlashing() {
	return flashing;
}	
/**
 * Flip the state of the flashing variable from true to false or vice-versa
 * This method notifies the run method that it should start flashing the LED
 */
public void setFlash() {

	flashing = !flashing;
	
	// Notify the flash thread that a state change has occurred
	synchronized(this) {
		this.notify();
	}	
	
	repaint();
}
/**
 * Set the LED colour to Green
 */
public void setGreen() {

	state = greenColours;
	off = false;
	repaint();
}
/**
 * Set the LED state to off
 */
public void setOff() {

	off = true;
	flashing = false;
	repaint();
}
/**
 * Set the LED colour to Red
 */
public void setRed() {

	state = redColours;
	off = false;
	repaint();
}
}
