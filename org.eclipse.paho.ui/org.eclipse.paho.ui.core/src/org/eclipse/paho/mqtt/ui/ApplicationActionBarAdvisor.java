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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.paho.mqtt.ui.actions.NewConnectionAction;
import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the actions added to a workbench window.
 * Each window will be populated with new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	// Actions - important to allocate these only in makeActions, and then use them
	// in the fill methods. This ensures that the actions aren't recreated
	// when fillActionBars is called with FILL_PROXY.
	private IWorkbenchAction exitAction;
	private IWorkbenchAction aboutAction;

	private Action newConnectionAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	protected void makeActions(final IWorkbenchWindow window) {

		// Creates the actions and registers them.
		// Registering is needed to ensure that key bindings work.
		// The corresponding commands keybindings are defined in the plugin.xml file.
		// Registering also provides automatic disposal of the actions when
		// the window is closed.

		exitAction = ActionFactory.QUIT.create(window);
		exitAction.setText(Messages.LABEL_EXIT);
		register(exitAction);

		aboutAction = ActionFactory.ABOUT.create(window);
		aboutAction.setText(Messages.LABEL_ABOUT);
		register(aboutAction);

		newConnectionAction = new NewConnectionAction(window);
		register(newConnectionAction);
	}

	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager("&" + Messages.LABEL_FILE, IWorkbenchActionConstants.M_FILE); //$NON-NLS-1$
		MenuManager helpMenu = new MenuManager("&" + Messages.LABEL_HELP, IWorkbenchActionConstants.M_HELP); //$NON-NLS-1$

		menuBar.add(fileMenu);
		// Add a group marker indicating where action set menus will appear.
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(helpMenu);

		// File
		fileMenu.add(newConnectionAction);
		fileMenu.add(new Separator());
		fileMenu.add(exitAction);

		// Help
		helpMenu.add(aboutAction);
	}

	protected void fillCoolBar(ICoolBarManager coolBar) {
		IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		coolBar.add(new ToolBarContributionItem(toolbar, "main")); //$NON-NLS-1$
		toolbar.add(newConnectionAction);
	}
}
