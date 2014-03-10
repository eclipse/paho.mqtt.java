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
package org.eclipse.paho.mqtt.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.paho.mqtt.ui.Constants.ImageKeys;
import org.eclipse.paho.mqtt.ui.Paho;
import org.eclipse.paho.mqtt.ui.core.event.Events;
import org.eclipse.paho.mqtt.ui.core.event.Selector;
import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.paho.mqtt.ui.util.Images;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * 
 * @author Bin Zhang
 * 
 */
public class NewConnectionAction extends Action {
	public static final String ID = "org.eclipse.paho.ui.actions.newConnection"; //$NON-NLS-1$

	/**
	 * @param text
	 * @param window
	 */
	public NewConnectionAction(IWorkbenchWindow window) {
		super(Messages.LABEL_NEW_CONNECTION);
		// The id is used to refer to the action in a menu or toolbar
		setId(ID);

		// Associate the action with a pre-defined command, to allow key bindings.
		setActionDefinitionId(ID);

		setImageDescriptor(Images.getDescriptor(ImageKeys.IMG_ADD));
	}

	public void run() {
		// send new connection event to request connection creation on nav tree
		Paho.getEventService().sendEvent(Events.of(Selector.ofNewConnection()));
	}
}