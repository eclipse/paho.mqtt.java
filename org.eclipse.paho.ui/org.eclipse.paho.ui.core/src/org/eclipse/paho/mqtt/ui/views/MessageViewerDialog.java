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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.paho.mqtt.ui.Constants;
import org.eclipse.paho.mqtt.ui.core.model.History;
import org.eclipse.paho.mqtt.ui.core.model.QoS;
import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.paho.mqtt.ui.util.Files;
import org.eclipse.paho.mqtt.ui.util.Strings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Message viewer
 * 
 * @author Bin Zhang
 */
public class MessageViewerDialog extends Dialog {
	private static final int SAVE_ID = IDialogConstants.CLIENT_ID + 1;
	private final History history;

	// These filter names are displayed to the user in the file dialog. Note
	// that
	// the inclusion of the actual extension in parentheses is optional, and
	// doesn't have any effect on which files are displayed.
	private static final String[] FILTER_NAMES = { "Message Files (*.msg)", "All Files (*.*)" }; //$NON-NLS-1$ //$NON-NLS-2$

	// These filter extensions are used to filter which files are displayed.
	private static final String[] FILTER_EXTS = { "*.msg", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * @param parentShell
	 * @param history
	 */
	public MessageViewerDialog(Shell parentShell, History history) {
		super(parentShell);
		this.history = history;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		FillLayout fill = new FillLayout();
		fill.marginHeight = 10;
		fill.marginWidth = 10;
		container.setLayout(fill);

		//
		Group group = new Group(container, SWT.NONE);
		// group.setText("Message");
		FormLayout layout = new FormLayout();
		layout.marginTop = 10;
		layout.marginBottom = 10;
		layout.marginLeft = 4;
		layout.marginRight = 4;
		group.setLayout(layout);

		// *******************************************
		// Event
		// *******************************************
		// Event - Label
		Label eventLabel = new Label(group, SWT.NONE);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		eventLabel.setLayoutData(fd);
		eventLabel.setText(Messages.HISTORY_TAB_EVENT);

		// Event - Text
		String event = history.getEvent();
		Text eventText = new Text(group, SWT.BORDER);
		eventText.setEditable(false);
		eventText.setText(event == null ? "" : event);
		fd = new FormData();
		fd.top = new FormAttachment(eventLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(eventLabel, 40);
		fd.right = new FormAttachment(100, -4);
		eventText.setLayoutData(fd);

		// *******************************************
		// Topic
		// *******************************************
		// Topic - Label
		String topic = history.getTopic();
		Label topicLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(eventLabel, 10);
		fd.left = new FormAttachment(0, 10);
		topicLabel.setLayoutData(fd);
		topicLabel.setText(Messages.HISTORY_TAB_TOPIC);

		// Topic - Text
		final Text topicText = new Text(group, SWT.BORDER);
		topicText.setText(topic == null ? "" : topic);
		topicText.setEditable(false);
		fd = new FormData();
		fd.right = new FormAttachment(eventText, 0, SWT.RIGHT);
		fd.top = new FormAttachment(topicLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(eventText, 0, SWT.LEFT);
		topicText.setLayoutData(fd);

		// *******************************************
		// Message
		// *******************************************
		// Message - Label
		String message = history.getMessage();
		Label messageLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(topicLabel, 50);
		fd.left = new FormAttachment(0, 10);
		messageLabel.setLayoutData(fd);
		messageLabel.setText(Messages.HISTORY_TAB_MSG);

		// Message - Text
		final Text messageText = new Text(group, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		messageText.setText(message == null ? "" : message);
		messageText.setEditable(false);
		fd = new FormData();
		fd.right = new FormAttachment(topicText, 0, SWT.RIGHT);
		fd.top = new FormAttachment(messageLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(topicText, 0, SWT.LEFT);
		fd.height = 100;
		messageText.setLayoutData(fd);

		// *******************************************
		// QoS
		// *******************************************
		// QoS - Label
		QoS qos = history.getQos();
		Label qosLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(messageText, 10);
		fd.left = new FormAttachment(0, 10);
		qosLabel.setLayoutData(fd);
		qosLabel.setText(Messages.HISTORY_TAB_QOS);

		// QoS - Text
		final Text qosText = new Text(group, SWT.BORDER);
		qosText.setText(qos == null ? "" : qos.getLabel());
		qosText.setEditable(false);
		fd = new FormData();
		fd.right = new FormAttachment(eventText, 0, SWT.RIGHT);
		fd.top = new FormAttachment(qosLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(eventText, 0, SWT.LEFT);
		qosText.setLayoutData(fd);

		// *******************************************
		// Retain
		// *******************************************
		// Retain - Label
		Boolean retained = history.getRetained();
		Label retainLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(qosText, 10);
		fd.left = new FormAttachment(0, 10);
		retainLabel.setLayoutData(fd);
		retainLabel.setText(Messages.HISTORY_TAB_RETAINED);

		// Retain - Text
		final Text retainText = new Text(group, SWT.BORDER);
		if (retained != null) {
			retainText.setText(retained ? Messages.LABEL_YES : Messages.LABEL_NO);
		}
		retainText.setEditable(false);
		fd = new FormData();
		fd.right = new FormAttachment(eventText, 0, SWT.RIGHT);
		fd.top = new FormAttachment(retainLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(eventText, 0, SWT.LEFT);
		retainText.setLayoutData(fd);

		// *******************************************
		// Time
		// *******************************************
		// Time - Label
		Date time = history.getTime();
		Label timeLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(retainText, 10);
		fd.left = new FormAttachment(0, 10);
		timeLabel.setLayoutData(fd);
		timeLabel.setText(Messages.HISTORY_TAB_TIME);

		// Time - Text
		final Text timeText = new Text(group, SWT.BORDER);
		timeText.setText(new SimpleDateFormat(Constants.DEFAULT_DATA_FORMAT).format(time));
		timeText.setEditable(false);
		fd = new FormData();
		fd.right = new FormAttachment(eventText, 0, SWT.RIGHT);
		fd.top = new FormAttachment(timeLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(eventText, 0, SWT.LEFT);
		timeText.setLayoutData(fd);

		return container;
	}

	// overriding this methods allows you to set the
	// title of the custom dialog
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.DLG_MESSAGE_VIEWER_TITLE);
		newShell.setImage(null);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		if (!Strings.isEmpty(history.getMessage())) {
			createButton(parent, SAVE_ID, Messages.LABEL_SAVE, false);
		}
		createButton(parent, IDialogConstants.OK_ID, Messages.LABEL_CLOSE, true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		switch (buttonId) {
		case SAVE_ID:
			// Only save the history: message as bytes, and only when the message is not empty or null
			FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
			fd.setFilterNames(FILTER_NAMES);
			fd.setFilterExtensions(FILTER_EXTS);
			String fileName = fd.open();
			if (fileName != null) {
				try {
					Files.writeBinary(new File(fileName), history.getMessage().getBytes());
					MessageDialog.openInformation(getShell(), Messages.LABEL_INFO,
							Messages.bind(Messages.DLG_MESSAGE_VIEWER_MSG_SAVED, fileName));
				}
				catch (Exception e) {
					MessageDialog.openError(getShell(), Messages.LABEL_ERROR,
							Messages.LABEL_ERROR + ": " + e.getLocalizedMessage());
				}
			}

			break;

		case IDialogConstants.OK_ID:
			okPressed();
			break;
		}
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 450);
	}
}
