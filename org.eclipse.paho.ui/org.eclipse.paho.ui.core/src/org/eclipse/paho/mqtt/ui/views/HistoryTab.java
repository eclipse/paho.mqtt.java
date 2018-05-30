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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.paho.mqtt.ui.Constants;
import org.eclipse.paho.mqtt.ui.Constants.ImageKeys;
import org.eclipse.paho.mqtt.ui.core.event.Event;
import org.eclipse.paho.mqtt.ui.core.event.IEventHandler;
import org.eclipse.paho.mqtt.ui.core.event.IEventService;
import org.eclipse.paho.mqtt.ui.core.event.IRegistration;
import org.eclipse.paho.mqtt.ui.core.event.Selector;
import org.eclipse.paho.mqtt.ui.core.event.Selector.Type;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.core.model.History;
import org.eclipse.paho.mqtt.ui.core.model.PublishMessage;
import org.eclipse.paho.mqtt.ui.core.model.Topic;
import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.paho.mqtt.ui.support.provider.IValueFormatter;
import org.eclipse.paho.mqtt.ui.support.table.TableViewerBuilder;
import org.eclipse.paho.mqtt.ui.util.Images;
import org.eclipse.paho.mqtt.ui.util.Strings;
import org.eclipse.paho.mqtt.ui.util.Widgets;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * History Tab
 * 
 * @author Bin Zhang
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class HistoryTab extends CTabItem {
	private TableViewer viewer;

	/**
	 * @param parent
	 * @param style
	 * @param connection
	 * @param eventService
	 */
	public HistoryTab(CTabFolder parent, int style, Connection connection, IEventService eventService) {
		super(parent, style);

		setText(Messages.HISTORY_TAB_TITLE);
		setImage(Images.get(ImageKeys.IMG_HISTORY));

		createControl(parent);

		// register event handler
		final IRegistration reg = eventService.registerHandler(Selector.ofAllResponses(connection),
				new IEventHandler() {
					@Override
					public void handleEvent(Event e) {
						Type type = e.getSelector().getType();
						switch (type) {
						case CONNECTED:
							addHistory(new History(Messages.EVENT_CONNECTED));
							break;

						case DISCONNECTED:
							addHistory(new History(Messages.EVENT_DISCONNECTED));
							break;

						case SUBSCRIBED:
							List<Topic> topics = (List<Topic>) e.getData();
							for (Topic topic : topics) {
								addHistory(new History(Messages.EVENT_SUBSCRIBED, topic.getTopicString(), topic
										.getQos()));
							}
							break;

						case UNSUBSCRIBED:
							List<String> topicFilters = (List<String>) e.getData();
							for (String topic : topicFilters) {
								addHistory(new History(Messages.EVENT_UNSUBSCRIBED, topic, null));
							}
							break;

						case PUBLISHED:
							addHistory(Messages.EVENT_PUBLISHED, (PublishMessage) e.getData());
							break;

						case RECEIVED:
							addHistory(Messages.EVENT_RECEIVED, (PublishMessage) e.getData());
							break;

						// error events
						case CONNECT_FAILED:
							addErrorHistory(Messages.EVENT_CONNECT_FAILED, (Throwable) e.getData());
							break;

						case CONNECTION_LOST:
							addErrorHistory(Messages.EVENT_CONNECTION_LOST, (Throwable) e.getData());
							break;

						case DISCONNECT_FAILED:
							addErrorHistory(Messages.EVENT_DISCONNECT_FAILED, (Throwable) e.getData());
							break;

						case PUBLISH_FAILED:
							addErrorHistory(Messages.EVENT_PUBLISH_FAILED, (Throwable) e.getData());
							break;

						case SUBSCRIBE_FAILED:
							addErrorHistory(Messages.EVENT_SUBSCRIBE_FAILED, (Throwable) e.getData());
							break;

						case UNSUBSCRIBE_FAILED:
							addErrorHistory(Messages.EVENT_UNSUBSCRIBE_FAILED, (Throwable) e.getData());
							break;

						default:
							break;
						}
					}
				});

		// unregister when widget disposed
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				reg.unregister();
			}
		});
	}

	/**
	 * @param tabFolder
	 */
	private void createControl(CTabFolder tabFolder) {
		Composite container = new Composite(tabFolder, SWT.BORDER);
		FillLayout layout = new FillLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 4;
		container.setLayout(layout);
		setControl(container);

		// Table viewer
		Composite composite = new Composite(container, SWT.NONE);
		TableViewerBuilder builder = new TableViewerBuilder(composite).doubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						StructuredSelection selection = (StructuredSelection) viewer.getSelection();
						History history = (History) selection.getFirstElement();
						new MessageViewerDialog(getControl().getShell(), history).open();
					}
				});
			}
		}).modelClass(History.class).input(new ArrayList<History>());

		viewer = builder.build();

		builder.columnBuilder(Messages.HISTORY_TAB_EVENT, SWT.LEFT).percentWidth(4).property(History.PROP_EVENT)
				.build();

		// Topic column
		builder.columnBuilder(Messages.HISTORY_TAB_TOPIC, SWT.LEFT).percentWidth(4).property(History.PROP_TOPIC)
				.build();

		// Msg column
		builder.columnBuilder(Messages.HISTORY_TAB_MSG, SWT.LEFT).percentWidth(4).property(History.PROP_MSG).build();
		// QoS column
		builder.columnBuilder(Messages.HISTORY_TAB_QOS, SWT.CENTER).percentWidth(2).property(History.PROP_QOS)
				.format(new IValueFormatter<Enum<?>>() {
					@Override
					public String format(Enum<?> e) {
						return String.valueOf(e.ordinal());
					}
				}).build();
		// Retained column
		builder.columnBuilder(Messages.HISTORY_TAB_RETAINED, SWT.CENTER).percentWidth(2)
				.property(History.PROP_RETAINED).format(new IValueFormatter<Boolean>() {
					@Override
					public String format(Boolean b) {
						return b ? Messages.LABEL_YES : Messages.LABEL_NO;
					}
				}).build();
		// Time column
		builder.columnBuilder(Messages.HISTORY_TAB_TIME, SWT.CENTER).percentWidth(4).property(History.PROP_TIME)
				.format(new IValueFormatter<Date>() {
					@Override
					public String format(Date date) {
						return new SimpleDateFormat(Constants.DEFAULT_DATA_FORMAT).format(date);
					}
				}).build();

		// build context menu for the table
		Widgets.buildContextMenu(viewer.getControl(), new Action() {
			@Override
			public String getText() {
				return Messages.LABEL_CLEAR;
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				return Images.getDescriptor(ImageKeys.IMG_CLEAR);
			}

			@Override
			public void run() {
				clearHistories();
			}
		});
	}

	/**
	 * @param type
	 * @param ex
	 */
	private void addErrorHistory(String event, Throwable ex) {
		addHistory(new History(event, ex.getClass().getName() + ": " + ex.getLocalizedMessage()));
	}

	/**
	 * @param type
	 * @param publish
	 */
	private void addHistory(String event, PublishMessage publish) {
		byte[] payload = publish.getPayload();
		String message = payload == null ? "" : Strings.of(payload);
		addHistory(new History(event, publish.getTopic(), message, publish.getQos(), publish.isRetain()));
	}

	/**
	 * @param history
	 */
	private void addHistory(History history) {
		((List<History>) viewer.getInput()).add(history);
		refreshView();
	}

	/**
	 * Clear histories
	 */
	private void clearHistories() {
		List<History> histories = ((List<History>) viewer.getInput());
		if (!histories.isEmpty()) {
			histories.clear();
			refreshView();
		}
	}

	private void refreshView() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				viewer.refresh();
			}
		});
	}
}
