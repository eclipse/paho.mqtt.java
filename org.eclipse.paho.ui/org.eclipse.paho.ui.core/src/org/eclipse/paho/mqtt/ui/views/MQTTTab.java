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

import static org.eclipse.paho.mqtt.ui.Constants.BUTTON_WIDTH;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.paho.mqtt.ui.Constants.ImageKeys;
import org.eclipse.paho.mqtt.ui.PahoException;
import org.eclipse.paho.mqtt.ui.core.DataBindings;
import org.eclipse.paho.mqtt.ui.core.DataBindings.DataBinding;
import org.eclipse.paho.mqtt.ui.core.DataBindings.Validators;
import org.eclipse.paho.mqtt.ui.core.event.Event;
import org.eclipse.paho.mqtt.ui.core.event.Events;
import org.eclipse.paho.mqtt.ui.core.event.IEventHandler;
import org.eclipse.paho.mqtt.ui.core.event.IEventService;
import org.eclipse.paho.mqtt.ui.core.event.Registrations;
import org.eclipse.paho.mqtt.ui.core.event.Selector;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.core.model.Pair;
import org.eclipse.paho.mqtt.ui.core.model.PublishMessage;
import org.eclipse.paho.mqtt.ui.core.model.QoS;
import org.eclipse.paho.mqtt.ui.core.model.Topic;
import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.paho.mqtt.ui.support.table.TableViewerBuilder;
import org.eclipse.paho.mqtt.ui.util.Colors;
import org.eclipse.paho.mqtt.ui.util.Files;
import org.eclipse.paho.mqtt.ui.util.Widgets;
import org.eclipse.paho.mqtt.ui.util.Files.Content;
import org.eclipse.paho.mqtt.ui.util.Images;
import org.eclipse.paho.mqtt.ui.util.Strings;
import org.eclipse.paho.mqtt.ui.views.IViewUpdater.ConnectionStatus;
import org.eclipse.paho.mqtt.ui.views.editor.TopicQosCellEditor;
import org.eclipse.paho.mqtt.ui.views.editor.TopicStringCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Tab MQTT
 * 
 * @author Bin Zhang
 */
public class MQTTTab extends CTabItem {
	private final Connection connection;
	private IEventService eventService;

	/**
	 * @param parent
	 * @param style
	 * @param connection
	 * @param eventService
	 */
	public MQTTTab(CTabFolder parent, int style, Connection connection, IEventService eventService) {
		super(parent, style);
		this.connection = connection;
		this.eventService = eventService;

		// set title
		setText(Messages.MQTT_TAB_TITLE);
		setImage(Images.get(ImageKeys.IMG_MQTT));

		// create UI
		createControl(parent);

		// register event handlers
		final Registrations registrations = new Registrations()
				.addRegistration(
						eventService.registerHandler(Selector.ofConnected(connection), new IEventHandler<Void>() {
							@Override
							public void handleEvent(Event<Void> e) {
								connectionViewUpdater.update(ConnectionStatus.Connected, null);
							}
						}))
				.addRegistration(
						eventService.registerHandler(Selector.ofConnectFailed(connection),
								new IEventHandler<Exception>() {
									@Override
									public void handleEvent(Event<Exception> e) {
										connectionViewUpdater.update(ConnectionStatus.Failed, e.getData());
									}
								}))
				.addRegistration(
						eventService.registerHandler(Selector.ofConnectionLost(connection),
								new IEventHandler<Exception>() {
									@Override
									public void handleEvent(Event<Exception> e) {
										connectionViewUpdater.update(ConnectionStatus.Disconnected, e.getData());
									}
								}))
				.addRegistration(
						eventService.registerHandler(Selector.ofDisconnected(connection), new IEventHandler<Void>() {
							@Override
							public void handleEvent(Event<Void> e) {
								connectionViewUpdater.update(ConnectionStatus.Disconnected, null);
							}
						}));

		// unregister when widget disposed
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				registrations.unregister();
			}
		});
	}

	/**
	 * @param tabFolder
	 */
	private void createControl(CTabFolder tabFolder) {
		// splitter
		SashForm sashForm = new SashForm(tabFolder, SWT.HORIZONTAL);
		sashForm.setLayout(new FillLayout());
		sashForm.setOrientation(SWT.HORIZONTAL);

		// ******************************************************
		// Part 1) Tab MQTT - Composite container
		// ******************************************************
		Composite mqttContainer = new Composite(sashForm, SWT.BORDER);
		GridLayout mqttLayout = new GridLayout();
		mqttLayout.numColumns = 1;
		mqttLayout.marginTop = 0;
		mqttLayout.marginBottom = 0;
		mqttLayout.verticalSpacing = 20;
		mqttContainer.setLayout(mqttLayout);

		// Connection group
		createConnectionGroup(mqttContainer);

		// Subscription group
		createSubscriptionGroup(mqttContainer);

		// Subscription group
		createPublicationGroup(mqttContainer);

		// ******************************************************
		// Part 2) Sub TabFolder (history & last message)
		// ******************************************************
		CTabFolder subTabFolder = new CTabFolder(sashForm, SWT.BORDER);
		subTabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(
				SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		GridLayout tabLayout = new GridLayout();
		tabLayout.marginHeight = 10;
		tabLayout.marginWidth = 10;
		tabLayout.verticalSpacing = 20;
		tabLayout.numColumns = 1;
		subTabFolder.setLayout(tabLayout);

		// Tab History
		new HistoryTab(subTabFolder, SWT.NONE, connection, eventService);

		// Tab Last Message
		new LastMessageTab(subTabFolder, SWT.NONE, connection, eventService);

		// select the first tab by default
		subTabFolder.setSelection(0);

		// should set weights after controls are attached to sashForm
		sashForm.setWeights(new int[] { 40, 60 });
		//
		setControl(sashForm);
	}

	// *********************************************************************************
	// Group: Connection
	// *********************************************************************************
	private ViewUpdater<ConnectionStatus> connectionViewUpdater;

	private Group createConnectionGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(Messages.MQTT_TAB_GROUP_CONN);
		group.setLayout(new FormLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// **********************************************
		// Row: Server URI
		// **********************************************
		// Server URI - label
		Label serverUriLabel = new Label(group, SWT.NONE);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		serverUriLabel.setLayoutData(fd);
		serverUriLabel.setText(Messages.MQTT_TAB_GROUP_CONN_SERVERURI);

		// Server URI - input
		final Text serverUriText = new Text(group, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(serverUriLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(serverUriLabel, 10);
		// percent (100%) + offset (-4)
		fd.right = new FormAttachment(100, -4);
		serverUriText.setLayoutData(fd);

		// **********************************************
		// Row: Client ID
		// **********************************************
		// Client ID - label
		Label clientIdLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(serverUriLabel, 10);
		fd.left = new FormAttachment(0, 10);
		clientIdLabel.setLayoutData(fd);
		clientIdLabel.setText(Messages.MQTT_TAB_GROUP_CONN_CLIENTID);

		// Client ID - input
		final Text clientIdText = new Text(group, SWT.BORDER);
		fd = new FormData();
		fd.right = new FormAttachment(serverUriText, 0, SWT.RIGHT);
		fd.top = new FormAttachment(clientIdLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(serverUriText, 0, SWT.LEFT);
		clientIdText.setLayoutData(fd);

		// **********************************************
		// Row: Status
		// **********************************************
		// Status - label
		Label statusLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(clientIdLabel, 10);
		fd.left = new FormAttachment(0, 10);
		statusLabel.setLayoutData(fd);
		statusLabel.setText(Messages.MQTT_TAB_GROUP_CONN_STATUS);

		// Status - text
		final Text statusText = new Text(group, SWT.BORDER);
		statusText.setEditable(false);
		statusText.setText(Messages.MQTT_TAB_GROUP_CONN_STATUS_DISCONNECTED);
		fd = new FormData();
		fd.right = new FormAttachment(serverUriText, 0, SWT.RIGHT);
		fd.top = new FormAttachment(statusLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(serverUriText, 0, SWT.LEFT);
		statusText.setLayoutData(fd);

		// **********************************************
		// Row: Connect/Disconnect button
		// **********************************************
		Composite connBtnComp = new Composite(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(statusText, 10);
		fd.left = new FormAttachment(statusText, 0, SWT.LEFT);
		fd.right = new FormAttachment(statusText, 0, SWT.RIGHT);
		connBtnComp.setLayoutData(fd);

		// layout
		RowLayout connBtnCompLayout = new RowLayout();
		// all widgets are the same size
		connBtnCompLayout.pack = false;
		// widgets are spread across the available space
		connBtnCompLayout.justify = true;
		connBtnCompLayout.type = SWT.HORIZONTAL;
		connBtnCompLayout.spacing = 0;
		connBtnComp.setLayout(connBtnCompLayout);

		//
		Composite btnContainer = new Composite(connBtnComp, SWT.NONE);
		GridLayout connBtnContainerLayout = new GridLayout();
		connBtnContainerLayout.numColumns = 2;
		connBtnContainerLayout.horizontalSpacing = 10;
		btnContainer.setLayout(connBtnContainerLayout);

		// connect - btn
		final Button btnConnect = new Button(btnContainer, SWT.NONE);
		btnConnect.setText(Messages.MQTT_TAB_GROUP_CONN_BTN_CONNECT);
		GridData gd = new GridData();
		gd.widthHint = BUTTON_WIDTH;
		btnConnect.setLayoutData(gd);
		btnConnect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// disable controls ASAP
				Widgets.enable(false, btnConnect, serverUriText, clientIdText);

				eventService.sendEvent(Events.of(Selector.ofConnect(), connection));
			}
		});

		// disconnect - btn
		final Button btnDisconnect = new Button(btnContainer, SWT.NONE);
		btnDisconnect.setText(Messages.MQTT_TAB_GROUP_CONN_BTN_DISCONNECT);
		gd = new GridData();
		gd.widthHint = BUTTON_WIDTH;
		btnDisconnect.setLayoutData(gd);
		btnDisconnect.setEnabled(false);
		btnDisconnect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						// disable btn ASAP
						btnDisconnect.setEnabled(false);

						eventService.sendEvent(Events.of(Selector.ofDisconnect(), connection));
					}
				});
			}
		});

		// UI updater
		connectionViewUpdater = new ViewUpdater<ConnectionStatus>() {
			@Override
			public void update(final ConnectionStatus status, final Exception ex) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						boolean connected = status == ConnectionStatus.Connected;

						// update text input
						serverUriText.setEnabled(!connected);
						clientIdText.setEnabled(!connected);

						// update button
						// enable connect if disconnected or failed
						btnConnect.setEnabled(!connected);
						btnDisconnect.setEnabled(connected);

						// update connection status
						switch (status) {
						case Connected:
							statusText.setText(Messages.MQTT_TAB_GROUP_CONN_STATUS_CONNECTED);
							statusText.setForeground(Colors.getColor("007A19")); //$NON-NLS-1$
							break;

						case Disconnected:
							statusText.setText(Messages.MQTT_TAB_GROUP_CONN_STATUS_DISCONNECTED);
							statusText.setForeground(Colors.getColor(SWT.COLOR_BLACK));
							break;

						case Failed:
							String msg = ex.getLocalizedMessage();
							if (msg == null) {
								StringWriter stackTrace = new StringWriter();
								ex.printStackTrace(new PrintWriter(stackTrace));
								msg = stackTrace.toString();
							}

							statusText.setText(Messages.MQTT_TAB_GROUP_CONN_STATUS_FAILED + " - " + msg); //$NON-NLS-1$
							statusText.setForeground(Colors.getColor("E82C0C")); //$NON-NLS-1$
							break;
						}

						// update pub/sub UI
						subscriptionViewUpdater.update(connected);
						publicationViewUpdater.update(connected);
					}
				});
			}
		};

		// **********************************************************************************
		// Connection Group Data Binding
		// **********************************************************************************
		DataBinding dataBinding = DataBindings.createDataBinding();
		dataBinding.bindText(serverUriText, connection, Connection.PROP_SERVERURI, Validators.serverUri);
		dataBinding.bindText(clientIdText, connection, Connection.PROP_CLIENTID, Validators.clientId);
		dataBinding.onMergedValueChange(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				Status status = (Status) event.getObservableValue().getValue();
				// if (status.isMultiStatus()) {
				// MultiStatus multiStatus = (MultiStatus) status;
				// System.out.println("multi"+multiStatus);
				// }

				// Enable connect btn only if serverURI and clientID are valid
				btnConnect.setEnabled(status.isOK());
			}
		});

		// dispose dataBinding when UI is disposed
		addDisposeListener(dataBinding);

		return group;
	}

	// *********************************************************************************
	// Group: Subscription
	// *********************************************************************************
	// subscription table
	private CheckboxTableViewer viewer;
	private ViewUpdater<Boolean> subscriptionViewUpdater;
	private ViewUpdater<Boolean> subscribeButtonsUpdater;

	private Group createSubscriptionGroup(Composite container) {
		// XXX topic model - need to save it?
		final List<Topic> topics = new ArrayList<Topic>();

		//
		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.MQTT_TAB_GROUP_SUB);
		GridLayout subgrpLayout = new GridLayout();
		subgrpLayout.numColumns = 1;
		subgrpLayout.marginTop = 0;
		subgrpLayout.marginBottom = 0;
		subgrpLayout.verticalSpacing = 0;
		group.setLayout(subgrpLayout);
		GridData subgd = new GridData(GridData.FILL_BOTH);
		// subgd.heightHint = 100;
		group.setLayoutData(subgd);

		// Toolbar
		ToolBar toolBar = new ToolBar(group, SWT.NONE);
		// add
		ToolItem itemAdd = new ToolItem(toolBar, SWT.FLAT);
		itemAdd.setImage(Images.get(ImageKeys.IMG_ADD));
		itemAdd.setDisabledImage(Images.get(ImageKeys.IMG_ADD_GRAY));
		itemAdd.setToolTipText(Messages.MQTT_TAB_GROUP_SUB_ADD_BTN_TOOLTIP);
		itemAdd.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						Topic t = new Topic("test", QoS.AT_MOST_ONCE); //$NON-NLS-1$
						((List<Topic>) viewer.getInput()).add(t);
						viewer.refresh();

						viewer.setChecked(t, true);

						// select the added topic
						viewer.setSelection(new StructuredSelection(t));

						// begin edit the added topic string cell
						viewer.editElement(t, 1);

						// update sub/unsub buttons
						subscribeButtonsUpdater.update(true);
					}
				});
			}
		});

		// delete
		final ToolItem itemRemove = new ToolItem(toolBar, SWT.FLAT);
		itemRemove.setImage(Images.get(ImageKeys.IMG_REMOVE));
		itemRemove.setDisabledImage(Images.get(ImageKeys.IMG_REMOVE_GRAY));
		itemRemove.setToolTipText(Messages.MQTT_TAB_GROUP_SUB_RM_BTN_TOOLTIP);
		itemRemove.setEnabled(false);
		itemRemove.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						StructuredSelection selection = (StructuredSelection) viewer.getSelection();
						List<Topic> input = ((List<Topic>) viewer.getInput());
						int index = -1;
						for (Topic t : (List<Topic>) selection.toList()) {
							if (index == -1) {
								index = input.indexOf(t);
							}
							input.remove(t);
						}

						viewer.refresh();

						// select previous index
						if (!input.isEmpty()) {
							index = index - 1 >= 0 ? index - 1 : 0;
							viewer.setSelection(new StructuredSelection(input.get(index)));
						}

						// update sub/unsub buttons
						subscribeButtonsUpdater.update(viewer.getCheckedElements().length > 0);
					}
				});
			}
		});

		// clear
		ToolItem itemClear = new ToolItem(toolBar, SWT.FLAT);
		itemClear.setImage(Images.get(ImageKeys.IMG_CLEAR));
		// itemClear.setDisabledImage(Images.get("clear_gray"));
		itemClear.setToolTipText(Messages.MQTT_TAB_GROUP_SUB_CLEAR_BTN_TOOLTIP);
		itemClear.setEnabled(true);
		itemClear.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						((List<Topic>) viewer.getInput()).clear();
						viewer.refresh();

						// update sub/unsub buttons
						subscribeButtonsUpdater.update(false);
					}
				});
			}
		});

		// Topic Table view
		Composite subComp = new Composite(group, SWT.NONE);
		subComp.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Note: using V_SCROLL | NO_SCROLL to force disable H_SCROLL
		TableViewerBuilder builder = new TableViewerBuilder(subComp, SWT.MULTI | SWT.V_SCROLL | SWT.NO_SCROLL
				| SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK)
				.selectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								itemRemove.setEnabled(!viewer.getSelection().isEmpty());
							}
						});
					}
				}).checkStateListener(new ICheckStateListener() {
					@Override
					public void checkStateChanged(CheckStateChangedEvent event) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								subscribeButtonsUpdater.update(viewer.getCheckedElements().length > 0);
							}
						});
					}
				}).makeEditable().input(topics);

		viewer = builder.buildCheckable();

		// build columns
		// checkbox column
		builder.columnBuilder("", SWT.LEFT).pixelWidth(50).emptyCellLabelProvider().build();

		// Topic column
		builder.columnBuilder(Messages.MQTT_TAB_GROUP_SUB_TOPIC, SWT.LEFT).percentWidth(60)
				.cellLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						Topic t = (Topic) element;
						return t.getTopicString();
					}

					@Override
					public String getToolTipText(Object element) {
						return Messages.TOOLTIP_DBCLICK_TO_EDIT;
					}
				}).editingSupport(new TopicStringCellEditor(viewer)).build();

		// QoS column
		builder.columnBuilder(Messages.MQTT_TAB_GROUP_SUB_QOS, SWT.CENTER).percentWidth(40)
				.editingSupport(new TopicQosCellEditor(viewer)).cellLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						Topic t = (Topic) element;
						return t.getQos().getLabel();
					}

					@Override
					public String getToolTipText(Object element) {
						return Messages.TOOLTIP_DBCLICK_TO_EDIT;
					}
				}).build();

		// **********************************************
		// Row: Sub/Unsub button
		// **********************************************
		Composite subBtnContainerForm = new Composite(group, SWT.NONE);
		subBtnContainerForm.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		subBtnContainerForm.setLayout(new FormLayout());

		// layout
		Composite subBtnContainerParent = new Composite(subBtnContainerForm, SWT.NONE);
		FormData fd = new FormData();
		// 80: manually checked value
		fd.left = new FormAttachment(0, 80);
		fd.top = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, 0);
		subBtnContainerParent.setLayoutData(fd);

		RowLayout subBtnLayout = new RowLayout();
		// all widgets are the same size
		subBtnLayout.pack = false;
		// widgets are spread across the available space
		subBtnLayout.justify = true;
		subBtnLayout.type = SWT.HORIZONTAL;
		subBtnLayout.spacing = 0;
		subBtnContainerParent.setLayout(subBtnLayout);

		Composite subBtnContainer = new Composite(subBtnContainerParent, SWT.NONE);
		GridLayout subBtnContainerLayout = new GridLayout();
		subBtnContainerLayout.numColumns = 2;
		subBtnContainerLayout.horizontalSpacing = 10;
		subBtnContainer.setLayout(subBtnContainerLayout);

		// sub - btn
		final Button btnSub = new Button(subBtnContainer, SWT.NONE);
		btnSub.setText(Messages.MQTT_TAB_GROUP_SUB_BTN_SUB);
		GridData btnSubGD = new GridData();
		btnSubGD.widthHint = BUTTON_WIDTH;
		btnSub.setLayoutData(btnSubGD);
		btnSub.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<Topic> topics = new ArrayList<Topic>();
				for (Object o : viewer.getCheckedElements()) {
					topics.add((Topic) o);
				}
				eventService.sendEvent(Events.of(Selector.ofSubscribe(), Pair.of(connection, topics)));
			}
		});

		// unsub - btn
		final Button btnUnsub = new Button(subBtnContainer, SWT.NONE);
		btnUnsub.setText(Messages.MQTT_TAB_GROUP_SUB_BTN_UNSUB);
		GridData btnUnsubGD = new GridData();
		btnUnsubGD.widthHint = BUTTON_WIDTH;
		btnUnsub.setLayoutData(btnUnsubGD);
		btnUnsub.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<Topic> topics = new ArrayList<Topic>();
				for (Object o : viewer.getCheckedElements()) {
					topics.add((Topic) o);
				}
				eventService.sendEvent(Events.of(Selector.ofUnsubscribe(), Pair.of(connection, topics)));
			}
		});

		// view updater
		subscriptionViewUpdater = new ViewUpdater<Boolean>() {
			@Override
			public void update(Boolean connected) {
				Widgets.enable(group, connected);
				subscribeButtonsUpdater.update(connected && viewer.getCheckedElements().length > 0);
			}
		};

		subscribeButtonsUpdater = new ViewUpdater<Boolean>() {
			@Override
			public void update(Boolean enabled) {
				btnSub.setEnabled(enabled);
				btnUnsub.setEnabled(enabled);
			}
		};

		// disabled for init state
		Widgets.enable(group, false);

		return group;
	}

	// *********************************************************************************
	// Group: Publication
	// *********************************************************************************
	private ViewUpdater<Boolean> publicationViewUpdater;
	private static final String[] FD_FILE_FILTER_EXTENSIONS = new String[] { "*.txt", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$

	private Group createPublicationGroup(Composite container) {
		// publish message model
		final PublishMessage message = new PublishMessage();

		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.MQTT_TAB_GROUP_PUB);
		group.setLayout(new FormLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// **********************************************
		// Row: Topic + QoS + Retain
		// **********************************************
		// Topic - label
		Label topicLabel = new Label(group, SWT.NONE);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		topicLabel.setLayoutData(fd);
		topicLabel.setText(Messages.MQTT_TAB_GROUP_PUB_TOPIC);

		// Topic - input
		Text topicText = new Text(group, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(topicLabel, 10, SWT.CENTER);
		// XXX should be computed via 'Server URI' text?
		fd.left = new FormAttachment(topicLabel, 40);
		fd.right = new FormAttachment(100, -4);
		topicText.setLayoutData(fd);

		// QoS - label
		Label qosLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(topicLabel, 10);
		fd.left = new FormAttachment(0, 10);
		qosLabel.setLayoutData(fd);
		qosLabel.setText(Messages.MQTT_TAB_GROUP_PUB_QOS);

		// QoS - selection
		CCombo combo = new CCombo(group, SWT.BORDER);
		List<String> qosList = new ArrayList<String>();
		for (QoS qos : QoS.values()) {
			qosList.add(qos.getLabel());
		}
		combo.setItems(qosList.toArray(new String[0]));
		combo.setEditable(false);
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						CCombo combo = (CCombo) e.widget;
						// String qos = combo.getItem(combo.getSelectionIndex());
						message.setQos(QoS.valueOf(combo.getSelectionIndex()));
					}
				});
			}
		});
		combo.select(0);
		fd = new FormData();
		fd.top = new FormAttachment(qosLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(topicText, 0, SWT.LEFT);
		combo.setLayoutData(fd);

		// Retained
		Button retained = new Button(group, SWT.CHECK);
		retained.setText(Messages.MQTT_TAB_GROUP_PUB_RETAINED);
		fd = new FormData();
		fd.top = new FormAttachment(qosLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(combo, 10);
		retained.setLayoutData(fd);

		// Hex
		final Button hex = new Button(group, SWT.CHECK);
		hex.setText(Messages.MQTT_TAB_GROUP_PUB_HEX);
		fd = new FormData();
		fd.top = new FormAttachment(qosLabel, 0, SWT.CENTER);
		fd.right = new FormAttachment(100, -4);
		hex.setLayoutData(fd);

		// **********************************************
		// Row: Message - label
		// **********************************************
		// Topic - label
		Label messageLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(topicLabel, 60);
		fd.left = new FormAttachment(0, 10);
		messageLabel.setLayoutData(fd);
		messageLabel.setText(Messages.MQTT_TAB_GROUP_PUB_MSG);

		// Message - input
		final Text messageText = new Text(group, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		fd = new FormData();
		fd.top = new FormAttachment(messageLabel, 10, SWT.CENTER);
		fd.left = new FormAttachment(topicText, 0, SWT.LEFT);
		fd.right = new FormAttachment(topicText, 0, SWT.RIGHT);
		fd.width = 300;
		fd.height = 60;
		messageText.setLayoutData(fd);

		// XXX handle input when in HEX mode
		hex.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						Button button = (Button) e.widget;
						if (button.getSelection()) {
							messageText.setText(Strings.toHex(messageText.getText()));
							messageText.setEditable(false);
						}
						else {
							messageText.setText(Strings.hexToString(messageText.getText()));
							messageText.setEditable(true);
						}
					}
				});
			}
		});

		// **********************************************
		// Row: Pub button
		// **********************************************
		Composite compPubBtn = new Composite(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(messageText, 10);
		fd.left = new FormAttachment(messageText, 0, SWT.LEFT);
		fd.right = new FormAttachment(messageText, 0, SWT.RIGHT);
		compPubBtn.setLayoutData(fd);

		RowLayout pubBtnLayout = new RowLayout();
		// all widgets are the same size
		pubBtnLayout.pack = false;
		// widgets are spread across the available space
		pubBtnLayout.justify = true;
		pubBtnLayout.type = SWT.HORIZONTAL;
		pubBtnLayout.spacing = 0;
		compPubBtn.setLayout(pubBtnLayout);

		Composite pubBtnContainer = new Composite(compPubBtn, SWT.NONE);
		GridLayout pubBtnContainerLayout = new GridLayout();
		pubBtnContainerLayout.numColumns = 2;
		pubBtnContainerLayout.horizontalSpacing = 10;
		pubBtnContainer.setLayout(pubBtnContainerLayout);
		// File - btn
		Button btnFile = new Button(pubBtnContainer, SWT.NONE);
		btnFile.setText(Messages.MQTT_TAB_GROUP_PUB_FILE);
		GridData gd = new GridData();
		gd.widthHint = BUTTON_WIDTH;
		btnFile.setLayoutData(gd);
		btnFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						FileDialog fd = new FileDialog(getControl().getShell(), SWT.OPEN);
						fd.setText(Messages.MQTT_TAB_GROUP_PUB_FD_OPENFILE);
						fd.setFilterExtensions(FD_FILE_FILTER_EXTENSIONS);
						String file = fd.open();
						if (file != null) {
							try {
								Content content = Files.read(new File(file));
								messageText.setText(content.getData());
								// if (content.isBinary()) {
								// hex.setSelection(true);
								// hex.setEnabled(false);
								// messageText.setEditable(false);
								// }
								// else {
								// hex.setSelection(false);
								// hex.setEnabled(true);
								// messageText.setEditable(true);
								// }
							}
							catch (IOException ioe) {
								ioe.printStackTrace();
								throw new PahoException(ioe);
							}
						}
					}
				});
			}
		});

		// Pub - btn
		final Button btnPub = new Button(pubBtnContainer, SWT.NONE);
		btnPub.setText(Messages.MQTT_TAB_GROUP_PUB_PUBLISH);
		gd = new GridData();
		gd.widthHint = BUTTON_WIDTH;
		btnPub.setLayoutData(gd);
		btnPub.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				eventService.sendEvent(Events.of(Selector.ofPublish(), Pair.of(connection, message)));
			}
		});

		// ***************************************************************************************
		// Publication Group Data Binding
		// ***************************************************************************************
		DataBinding dataBinding = DataBindings.createDataBinding();
		dataBinding.bindText(topicText, message, PublishMessage.PROP_TOPIC, Validators.publishTopic);
		dataBinding.bindTextAsBytes(messageText, message, PublishMessage.PROP_PAYLOAD);
		dataBinding.bindSelection(retained, message, PublishMessage.PROP_RETAIN);
		dataBinding.onMergedValueChange(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				Status status = (Status) event.getObservableValue().getValue();
				if (status.isOK()) {
					btnPub.setEnabled(true);
				}
				else {
					btnPub.setEnabled(false);
				}
			}
		});

		// dispose dataBinding when UI is disposed
		addDisposeListener(dataBinding);

		// view updater
		publicationViewUpdater = new ViewUpdater<Boolean>() {
			@Override
			public void update(Boolean connected) {
				Widgets.enable(group, connected);
			}
		};

		Widgets.enable(group, false);

		return group;
	}

}
