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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;

/**
 * Widgets helper
 * 
 * @author Bin Zhang
 */
public final class Widgets {

	/**
	 * Enable or disable controls
	 * 
	 * @param controls
	 */
	public static void enable(boolean enabled, Control... controls) {
		if (controls != null) {
			for (Control control : controls) {
				control.setEnabled(enabled);
			}
		}
	}

	/**
	 * Recursively enable or disable all the child controls of a composite
	 * 
	 * @param composite
	 * @param enabled
	 */
	public static void enable(Composite composite, boolean enabled) {
		for (Control control : composite.getChildren()) {

			if (Composite.class.isInstance(control)) {
				enable((Composite) control, enabled);
			}

			// ignore for label
			if (Label.class.isInstance(control)) {
				continue;
			}

			control.setEnabled(enabled);
		}
		composite.setEnabled(enabled);
	}

	/**
	 * Build a context menu for a control
	 * 
	 * @param control
	 * @param actions
	 * @return created menu
	 */
	public static Menu buildContextMenu(Control control, final Action... actions) {
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				if (actions != null) {
					for (Action action : actions) {
						manager.add(action);
					}
				}
			}
		});
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);
		return menu;
	}
}
