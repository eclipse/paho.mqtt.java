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
package org.eclipse.paho.mqtt.ui.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.paho.mqtt.ui.Constants.ImageKeys;
import org.eclipse.paho.mqtt.ui.Paho;
import org.eclipse.paho.mqtt.ui.PahoException;
import org.eclipse.paho.mqtt.ui.core.event.Event;
import org.eclipse.paho.mqtt.ui.core.event.IEventHandler;
import org.eclipse.paho.mqtt.ui.core.event.IEventService;
import org.eclipse.paho.mqtt.ui.core.event.IRegistration;
import org.eclipse.paho.mqtt.ui.core.event.Selector;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.core.model.Pair;
import org.eclipse.paho.mqtt.ui.util.Images;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

/**
 * Connection Editor Part
 * 
 * @author Bin Zhang
 */
public class ConnectionEditorPart extends EditorPart {
	public static final String ID = "org.eclipse.paho.mqtt.ui.views.ConnectionEditorPart"; //$NON-NLS-1$
	//
	private Connection connection;
	private IEventService eventService;
	private IRegistration registration;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof ConnectionEditorInput)) {
			throw new PahoException("Invalid editorInput!");//$NON-NLS-1$
		}

		setSite(site);
		setInput(input);
		setPartName(input.getName());
		setTitleImage(Images.get(ImageKeys.IMG_CONNECTION));

		// get connection
		connection = (Connection) input.getAdapter(Connection.class);

		// register handler
		eventService = Paho.getEventService();
		registration = eventService.registerHandler(Selector.ofRenameConnection(connection),
				new IEventHandler<Pair<String, String>>() {
					@Override
					public void handleEvent(Event<Pair<String, String>> e) {
						// <oldName, newName>
						final Pair<String, String> tuple = e.getData();

						// set to newName
						setPartName(tuple.getRight());
						// firePropertyChange(IEditorPart.PROP_TITLE);
					}
				});
	}

	/**
	 * Create contents of the editor part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		FillLayout layout = new FillLayout(SWT.HORIZONTAL);
		layout.marginHeight = 10;
		top.setLayout(layout);

		// TabFolder
		CTabFolder tabFolder = new CTabFolder(top, SWT.BORDER);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(
				SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		GridLayout tabLayout = new GridLayout();
		tabLayout.marginHeight = 10;
		tabLayout.marginWidth = 10;
		tabLayout.verticalSpacing = 20;
		tabLayout.numColumns = 1;
		tabFolder.setLayout(tabLayout);

		new MQTTTab(tabFolder, SWT.NONE, connection, eventService);

		// Tab Options
		new OptionsTab(tabFolder, SWT.NONE, connection);

		// select the first tab by default
		tabFolder.setSelection(0);
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Do the Save operation
	}

	@Override
	public void doSaveAs() {
		// Do the Save As operation
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void dispose() {
		if (registration != null) {
			registration.unregister();
		}

		// automatically disconnect it when editor is closed
		Paho.getConnectionManager().disconnect(connection);

		super.dispose();
	}

}
