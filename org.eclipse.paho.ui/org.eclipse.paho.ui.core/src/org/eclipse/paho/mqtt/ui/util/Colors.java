/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
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
 * Contributors:
 *    Bin Zhang - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.paho.mqtt.ui.util;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * SWT Color helper
 * 
 * @author Bin Zhang
 */
public final class Colors {

	/**
	 * Maps RGB values to colors
	 */
	private static HashMap<RGB, Color> colorMap = new HashMap<RGB, Color>();

	/**
	 * Returns the system color matching the specific ID
	 * 
	 * @param systemColorID int The ID value for the color
	 * @return Color The system color matching the specific ID
	 */
	public static Color getColor(int systemColorID) {
		return Display.getCurrent().getSystemColor(systemColorID);
	}

	/**
	 * 
	 * @param hexColorCode if hexColorCode is not valid return RGB(0, 0, 0)
	 * @return Color
	 * @throws NumberFormatException if the hex code is invalid
	 */
	public static Color getColor(String hexColorCode) {
		if (hexColorCode.length() != 6)
			return getColor(new RGB(0, 0, 0));
		int r = Integer.parseInt(hexColorCode.substring(0, 2), 16);
		int g = Integer.parseInt(hexColorCode.substring(2, 4), 16);
		int b = Integer.parseInt(hexColorCode.substring(4, 6), 16);
		return getColor(new RGB(r, g, b));
	}

	/**
	 * Returns a color given its red, green and blue component values
	 * 
	 * @param r int The red component of the color
	 * @param g int The green component of the color
	 * @param b int The blue component of the color
	 * @return Color The color matching the given red, green and blue component values
	 */
	public static Color getColor(int r, int g, int b) {
		return getColor(new RGB(r, g, b));
	}

	/**
	 * Returns a color given its RGB value
	 * 
	 * @param rgb RGB The RGB value of the color
	 * @return Color The color matching the RGB value
	 */
	public static Color getColor(RGB rgb) {
		Color color = colorMap.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			colorMap.put(rgb, color);
		}
		return color;
	}

	/**
	 * Dispose of all the cached colors
	 */
	public static void disposeColors() {
		for (Iterator<Color> iter = colorMap.values().iterator(); iter.hasNext();) {
			iter.next().dispose();
		}
		colorMap.clear();
	}

}
