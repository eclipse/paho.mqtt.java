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
package org.eclipse.paho.mqtt.ui;

import org.eclipse.paho.mqtt.ui.views.NavigationView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IViewLayout;

/**
 * 
 * @author Bin Zhang
 * 
 */
public class Perspective implements IPerspectiveFactory {
	/**
	 * The ID of the perspective as specified in the extension.
	 */
	public static final String ID = "org.eclipse.paho.ui.perspective"; //$NON-NLS-1$

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(true);

		IFolderLayout folderLeft = layout.createFolder("left", IPageLayout.LEFT, .2f, editorArea); //$NON-NLS-1$
		folderLeft.addView(NavigationView.ID);

		// Make it not moveable and closeable
		IViewLayout navView = layout.getViewLayout(NavigationView.ID);
		navView.setCloseable(false);
		navView.setMoveable(false);
	}
}
