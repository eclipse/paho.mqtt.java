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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.paho.mqtt.ui.Constants;
import org.eclipse.paho.mqtt.ui.Constants.ImageKeys;
import org.eclipse.paho.mqtt.ui.core.DataBindings;
import org.eclipse.paho.mqtt.ui.core.DataBindings.Converters;
import org.eclipse.paho.mqtt.ui.core.DataBindings.DataBinding;
import org.eclipse.paho.mqtt.ui.core.DataBindings.DecoratedBinding;
import org.eclipse.paho.mqtt.ui.core.DataBindings.Validators;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.core.model.LWT;
import org.eclipse.paho.mqtt.ui.core.model.QoS;
import org.eclipse.paho.mqtt.ui.core.model.ServerURI;
import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.paho.mqtt.ui.support.table.TableViewerBuilder;
import org.eclipse.paho.mqtt.ui.util.Colors;
import org.eclipse.paho.mqtt.ui.util.Images;
import org.eclipse.paho.mqtt.ui.util.Strings;
import org.eclipse.paho.mqtt.ui.util.Widgets;
import org.eclipse.paho.mqtt.ui.views.editor.ServerUriCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.ToolTip;

/**
 * Options Tab
 * 
 * @author Bin Zhang
 */
public class OptionsTab extends CTabItem {
	private static final String[] KEYSTORE_FILE_EXT = new String[] { "*.jks", "*.p12", "*.pfx", "*.*" };
	private final Connection connection;

	/**
	 * @param parent
	 * @param style
	 * @param connection
	 */
	public OptionsTab(CTabFolder parent, int style, Connection connection) {
		super(parent, style);

		this.connection = connection;

		// set text & image
		setText(Messages.OPT_TAB_TITLE);
		setImage(Images.get(ImageKeys.IMG_OPTIONS));

		// create UI
		createControl(parent);
	}

	/**
	 * @param tabFolder
	 */
	private void createControl(CTabFolder tabFolder) {
		// Create Container
		ScrolledComposite scrolledContainer = new ScrolledComposite(tabFolder, SWT.BORDER | SWT.V_SCROLL);

		Composite container = new Composite(scrolledContainer, SWT.NONE);
		GridLayout glayout = new GridLayout();
		glayout.numColumns = 1;
		glayout.marginTop = 0;
		glayout.marginBottom = 0;
		glayout.marginRight = 4;
		glayout.verticalSpacing = 20;
		container.setLayout(glayout);

		// General
		createGeneralGroup(container);

		// SSL
		createSSLGroup(container);

		// HA
		createHAGroup(container);

		// LWT
		createLWTGroup(container);

		scrolledContainer.setContent(container);
		scrolledContainer.setExpandHorizontal(true);
		scrolledContainer.setExpandVertical(true);
		// scrolledContainer.setMinWidth( 1600 );
		scrolledContainer.setMinHeight(900);

		setControl(scrolledContainer);
	}

	// *********************************************************************************
	// Group: LWT
	// *********************************************************************************
	private ViewUpdater<Boolean> lwtViewUpdater;

	private void createLWTGroup(Composite container) {
		// data model
		final LWT lwt = connection.getOptions().getLwt();

		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.OPT_TAB_GROUP_LWT);

		// layout
		FormLayout layout = new FormLayout();
		layout.marginBottom = 10;
		group.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);

		// **********************************************
		// Row: Topic + QoS + Retain
		// **********************************************
		// Topic - label
		Label topicLabel = new Label(group, SWT.NONE);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		topicLabel.setLayoutData(fd);
		topicLabel.setText(Messages.OPT_TAB_GROUP_LWT_TOPIC);

		// Topic - input
		final Text topicText = new Text(group, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(topicLabel, 10, SWT.CENTER);
		fd.left = new FormAttachment(topicLabel, 40);
		fd.right = new FormAttachment(100, -4);
		topicText.setLayoutData(fd);

		// QoS - label
		Label qosLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(topicLabel, 10);
		fd.left = new FormAttachment(0, 10);
		qosLabel.setLayoutData(fd);
		qosLabel.setText(Messages.OPT_TAB_GROUP_LWT_QOS);

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
						// Note: need manually set (because NOT using databinding for combo)
						CCombo combo = (CCombo) e.widget;
						lwt.setQos(QoS.valueOf(combo.getSelectionIndex()));
					}
				});
			}
		});

		// Note: need manually set (because NOT using databinding for combo)
		combo.select(lwt.getQos().getValue());
		fd = new FormData();
		fd.top = new FormAttachment(qosLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(topicText, 0, SWT.LEFT);
		combo.setLayoutData(fd);

		// Retained
		Button retained = new Button(group, SWT.CHECK);
		retained.setText(Messages.OPT_TAB_GROUP_LWT_RETAINED);
		fd = new FormData();
		fd.top = new FormAttachment(qosLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(combo, 10);
		retained.setLayoutData(fd);

		// Hex
		final Button hex = new Button(group, SWT.CHECK);
		hex.setText(Messages.OPT_TAB_GROUP_LWT_HEX);
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
		messageLabel.setText(Messages.OPT_TAB_GROUP_LWT_MSG);

		// Message - input
		final Text messageText = new Text(group, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		fd = new FormData();
		fd.top = new FormAttachment(messageLabel, 10, SWT.CENTER);
		fd.left = new FormAttachment(topicText, 0, SWT.LEFT);
		fd.right = new FormAttachment(topicText, 0, SWT.RIGHT);
		// fd.width = 300;
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
		// DataBinding
		// **********************************************
		final DataBinding dataBinding = DataBindings.createDataBinding();
		dataBinding.bindTextAsBytes(messageText, connection, Connection.PROP_OPT_LWT_PAYLOAD);
		dataBinding.bindSelection(retained, connection, Connection.PROP_OPT_LWT_RETAIN);

		// LWT view updater
		lwtViewUpdater = new ViewUpdater<Boolean>() {
			final DecoratedBinding binding;
			{
				binding = dataBinding.bindText(topicText, connection, Connection.PROP_OPT_LWT_TOPIC,
						Validators.publishTopic).hideDecorations();
			}

			@Override
			public void update(Boolean selected) {
				Widgets.enable(group, selected);
				if (selected) {
					binding.showDecorations();
				}
				else {
					binding.hideDecorations();
				}
			}
		};
		// dispose dataBinding when UI is disposed
		addDisposeListener(dataBinding);

		// initial state
		Widgets.enable(group, connection.getOptions().isLwtEnabled());
	}

	// *********************************************************************************
	// Group: HA
	// *********************************************************************************
	private ViewUpdater<Boolean> haViewUpdater;
	// HA table viewer
	private TableViewer viewer;

	private void createHAGroup(Composite container) {
		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.OPT_TAB_GROUP_HA);

		// layout
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginTop = 0;
		layout.marginBottom = 10;
		layout.verticalSpacing = 0;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Toolbar
		ToolBar toolBar = new ToolBar(group, SWT.NONE);
		// add
		ToolItem itemAdd = new ToolItem(toolBar, SWT.FLAT);
		itemAdd.setImage(Images.get(ImageKeys.IMG_ADD));
		itemAdd.setDisabledImage(Images.get(ImageKeys.IMG_ADD_GRAY));
		itemAdd.setToolTipText(Messages.OPT_TAB_GROUP_HA_ADD_BTN_TOOLTIP);
		itemAdd.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						ServerURI uri = new ServerURI(Constants.TCP_SERVER_URI);
						((List<ServerURI>) viewer.getInput()).add(uri);
						viewer.refresh();

						// select the added uri
						viewer.setSelection(new StructuredSelection(uri));

						viewer.editElement(uri, 1);
					}
				});
			}
		});

		// remove
		final ToolItem itemRemove = new ToolItem(toolBar, SWT.FLAT);
		itemRemove.setImage(Images.get(ImageKeys.IMG_REMOVE));
		itemRemove.setDisabledImage(Images.get(ImageKeys.IMG_REMOVE_GRAY));
		itemRemove.setToolTipText(Messages.OPT_TAB_GROUP_HA_RM_BTN_TOOLTIP);
		itemRemove.setEnabled(false);
		itemRemove.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						StructuredSelection selection = (StructuredSelection) viewer.getSelection();
						List<ServerURI> input = ((List<ServerURI>) viewer.getInput());
						int index = -1;
						for (ServerURI uri : (List<ServerURI>) selection.toList()) {
							if (index == -1) {
								index = input.indexOf(uri);
							}
							input.remove(uri);
						}

						viewer.refresh();

						// select previous index
						if (!input.isEmpty()) {
							index = index - 1 >= 0 ? index - 1 : 0;
							viewer.setSelection(new StructuredSelection(input.get(index)));
						}
					}
				});
			}
		});

		// clear
		ToolItem itemClear = new ToolItem(toolBar, SWT.FLAT);
		itemClear.setImage(Images.get(ImageKeys.IMG_CLEAR));
		// itemClear.setDisabledImage(Images.get("clear_gray"));
		itemClear.setToolTipText(Messages.OPT_TAB_GROUP_HA_CLEAR_BTN_TOOLTIP);
		itemClear.setEnabled(true);
		itemClear.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						((List<ServerURI>) viewer.getInput()).clear();
						viewer.refresh();
					}
				});
			}
		});

		// Table viewer
		Composite composite = new Composite(group, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Note: using V_SCROLL | NO_SCROLL to force disable H_SCROLL
		TableViewerBuilder builder = new TableViewerBuilder(composite, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION
				| SWT.NO_SCROLL | SWT.BORDER).selectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						itemRemove.setEnabled(!viewer.getSelection().isEmpty());
					}
				});
			}
		}).makeEditable().input(connection.getOptions().getServerURIs());

		// build the table viewer
		viewer = builder.build();

		// table columns
		// First extra column to display error message
		builder.columnBuilder("", SWT.LEFT).pixelWidth(20).emptyCellLabelProvider().build();
		builder.columnBuilder(Messages.OPT_TAB_GROUP_HA_SERVER_URIS, SWT.LEFT).percentWidth(90)
				.cellLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return ((ServerURI) element).getValue();
					}

					@Override
					public String getToolTipText(Object element) {
						return Messages.TOOLTIP_DBCLICK_TO_EDIT;
					}
				}).editingSupport(new ServerUriCellEditor(viewer)).build();

		viewer.refresh();

		// initial state
		Widgets.enable(group, connection.getOptions().isHaEnabled());
		haViewUpdater = new ViewUpdater<Boolean>() {
			@Override
			public void update(Boolean selected) {
				Widgets.enable(group, selected);
				viewer.refresh();
			}
		};

	}

	// *********************************************************************************
	// Group: SSL
	// *********************************************************************************
	private ViewUpdater<Boolean> sslViewUpdater;

	private void createSSLGroup(Composite container) {
		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.OPT_TAB_GROUP_SSL);
		FormLayout layout = new FormLayout();
		layout.marginBottom = 10;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// 1)
		// **********************************************
		// Row: Keystore - Location
		// **********************************************
		// Keystore Location - Label
		Label keystoreLabel = new Label(group, SWT.NONE);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		keystoreLabel.setLayoutData(fd);
		keystoreLabel.setText(Messages.OPT_TAB_GROUP_SSL_KEY_STORE);

		// Keystore Location - Directory - Text
		final Text keystoreDirText = new Text(group, SWT.BORDER);
		keystoreDirText.setEditable(false);
		fd = new FormData();
		fd.top = new FormAttachment(keystoreLabel, 0, SWT.CENTER);
		// fd.left = new FormAttachment(keystoreLabel, 30);
		fd.left = new FormAttachment(0, 160);
		fd.width = 454;
		keystoreDirText.setLayoutData(fd);

		// Keystore Location- Browse - Button
		final Button keystoreBrowseButton = new Button(group, SWT.PUSH);
		keystoreBrowseButton.setText(Messages.LABEL_BROWSE);
		fd = new FormData();
		fd.top = new FormAttachment(keystoreDirText, 0, SWT.CENTER);
		fd.left = new FormAttachment(keystoreDirText, 10);
		keystoreBrowseButton.setLayoutData(fd);
		keystoreBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						FileDialog  fileDialog = new FileDialog (getControl().getShell());
						fileDialog.setText(Messages.OPT_TAB_GROUP_SSL_KEY_STORE_DLG);
						fileDialog.setFilterExtensions(KEYSTORE_FILE_EXT);
						String selectedFile = fileDialog.open();
						if (selectedFile != null) {
							keystoreDirText.setText(selectedFile);
						}
					}
				});
			}
		});

		// **********************************************
		// Row: Keystore - Password
		// **********************************************
		// Keystore - Password - Label
		Label keystorePwdLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(keystoreLabel, 10);
		fd.left = new FormAttachment(0, 10);
		keystorePwdLabel.setLayoutData(fd);
		keystorePwdLabel.setText(Messages.OPT_TAB_GROUP_SSL_KEY_STORE_PWD);

		// Keystore - Password - Text
		final Text keystorePwdText = new Text(group, SWT.BORDER | SWT.PASSWORD);
		fd = new FormData();
		fd.top = new FormAttachment(keystorePwdLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(keystoreDirText, 0, SWT.LEFT);
		fd.right = new FormAttachment(keystoreDirText, 0, SWT.RIGHT);
		keystorePwdText.setLayoutData(fd);

		// 2)
		// **********************************************
		// Row: Truststore - Location
		// **********************************************
		// Truststore Location - Label
		Label truststoreLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(keystorePwdLabel, 10);
		fd.left = new FormAttachment(0, 10);
		truststoreLabel.setLayoutData(fd);
		truststoreLabel.setText(Messages.OPT_TAB_GROUP_SSL_TRUST_STORE);

		// Truststore Location - Directory - Text
		final Text truststoreDirText = new Text(group, SWT.BORDER);
		truststoreDirText.setEditable(false);
		fd = new FormData();
		fd.top = new FormAttachment(truststoreLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(keystoreDirText, 0, SWT.LEFT);
		fd.right = new FormAttachment(keystoreDirText, 0, SWT.RIGHT);
		truststoreDirText.setLayoutData(fd);

		// Truststore Location- Browse - Button
		final Button truststoreBrowseButton = new Button(group, SWT.PUSH);
		truststoreBrowseButton.setText(Messages.LABEL_BROWSE);
		fd = new FormData();
		fd.top = new FormAttachment(truststoreDirText, 0, SWT.CENTER);
		fd.left = new FormAttachment(truststoreDirText, 10);
		truststoreBrowseButton.setLayoutData(fd);
		truststoreBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						FileDialog  fileDialog = new FileDialog (getControl().getShell());
						fileDialog.setText(Messages.OPT_TAB_GROUP_SSL_TRUST_STORE_DLG);
						fileDialog.setFilterExtensions(KEYSTORE_FILE_EXT);
						String selectedFile = fileDialog.open();
						if (selectedFile != null) {
							truststoreDirText.setText(selectedFile);
						}
					}
				});
			}
		});

		// **********************************************
		// Row: Truststore - Password
		// **********************************************
		// Truststore - Password - Label
		Label truststorePwdLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(truststoreLabel, 10);
		fd.left = new FormAttachment(0, 10);
		truststorePwdLabel.setLayoutData(fd);
		truststorePwdLabel.setText(Messages.OPT_TAB_GROUP_SSL_TRUST_STORE_PWD);

		// Truststore - Password - Text
		final Text truststorePwdText = new Text(group, SWT.BORDER | SWT.PASSWORD);
		fd = new FormData();
		fd.top = new FormAttachment(truststorePwdLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(keystoreDirText, 0, SWT.LEFT);
		fd.right = new FormAttachment(keystoreDirText, 0, SWT.RIGHT);
		truststorePwdText.setLayoutData(fd);

		// **********************************************
		// DataBinding
		// **********************************************
		final DataBinding dataBinding = DataBindings.createDataBinding();
		// SSL view updater
		sslViewUpdater = new ViewUpdater<Boolean>() {
			final DecoratedBinding keystoreBinding;
			final DecoratedBinding truststoreBinding;
			{
				// key store
				keystoreBinding = dataBinding.bindText(keystoreDirText, connection, Connection.PROP_OPT_SSL_KS,
						Validators.required).hideDecorations();

				// PWD is not required, no decoration is required
				dataBinding.bindText(keystorePwdText, connection, Connection.PROP_OPT_SSL_KSPWD,
						Converters.stringToChars);

				// trust store
				truststoreBinding = dataBinding.bindText(truststoreDirText, connection, Connection.PROP_OPT_SSL_TS,
						Validators.required).hideDecorations();

				// PWD is not required, no decoration is required
				dataBinding.bindText(truststorePwdText, connection, Connection.PROP_OPT_SSL_TSPWD,
						Converters.stringToChars);
			}

			@Override
			public void update(Boolean selected) {
				Widgets.enable(group, selected);
				if (selected) {
					keystoreBinding.showDecorations();
					truststoreBinding.showDecorations();
				}
				else {
					keystoreBinding.hideDecorations();
					truststoreBinding.hideDecorations();
				}
			}
		};

		// dispose dataBinding when UI is disposed
		addDisposeListener(dataBinding);

		// disable all when creation
		Widgets.enable(group, connection.getOptions().isSslEnabled());
	}

	// *********************************************************************************
	// Group: General
	// *********************************************************************************
	private ViewUpdater<Boolean> loginViewUpdater;
	private ViewUpdater<Boolean> persistenceViewUpdater;

	private void createGeneralGroup(Composite container) {
		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.OPT_TAB_GROUP_GENERAL);
		FormLayout layout = new FormLayout();
		layout.marginBottom = 10;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// **********************************************
		// Row: CleanSession
		// **********************************************
		// CleanSession - Label
		Label cleanSessionLabel = new Label(group, SWT.NONE);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		cleanSessionLabel.setLayoutData(fd);
		cleanSessionLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_CLEAN_SESSION);

		// CleanSession - Check
		final Button cleanSessionCheck = new Button(group, SWT.CHECK);
		fd = new FormData();
		fd.top = new FormAttachment(cleanSessionLabel, 0, SWT.CENTER);
		// fd.left = new FormAttachment(cleanSessionLabel, 60);
		fd.left = new FormAttachment(0, 160);
		cleanSessionCheck.setLayoutData(fd);

		// **********************************************
		// Row: SSL
		// **********************************************
		// SSL - Label
		Label sslLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(cleanSessionLabel, 10);
		fd.left = new FormAttachment(0, 10);
		sslLabel.setLayoutData(fd);
		sslLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_ENABLE_SSL);

		// SSL - Check
		final Button sslCheck = new Button(group, SWT.CHECK);
		fd = new FormData();
		fd.top = new FormAttachment(sslLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(cleanSessionCheck, 0, SWT.LEFT);
		sslCheck.setLayoutData(fd);
		sslCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						sslViewUpdater.update(sslCheck.getSelection());
					}
				});
			}
		});

		// **********************************************
		// Row: HA
		// **********************************************
		// HA - Label
		Label haLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(sslLabel, 10);
		fd.left = new FormAttachment(0, 10);
		haLabel.setLayoutData(fd);
		haLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_ENABLE_HA);

		// HA - Check
		final Button haCheck = new Button(group, SWT.CHECK);
		fd = new FormData();
		fd.top = new FormAttachment(haLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(cleanSessionCheck, 0, SWT.LEFT);
		haCheck.setLayoutData(fd);
		haCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						haViewUpdater.update(haCheck.getSelection());
					}
				});
			}
		});

		// **********************************************
		// Row: LWT
		// **********************************************
		// LWT - Label
		Label lwtLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(haLabel, 10);
		fd.left = new FormAttachment(0, 10);
		lwtLabel.setLayoutData(fd);
		lwtLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_ENABLE_LWT);

		// LWT - Check
		final Button lwtCheck = new Button(group, SWT.CHECK);
		fd = new FormData();
		fd.top = new FormAttachment(lwtLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(cleanSessionCheck, 0, SWT.LEFT);
		lwtCheck.setLayoutData(fd);
		lwtCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						lwtViewUpdater.update(lwtCheck.getSelection());
					}
				});
			}
		});

		// **********************************************
		// Row: Keep Alive Interval
		// **********************************************
		// Keep Alive - Label
		Label keepAliveLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(lwtLabel, 10);
		fd.left = new FormAttachment(0, 10);
		keepAliveLabel.setLayoutData(fd);
		keepAliveLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_KEEP_ALIVE);

		// Keep Alive - Spinner
		final Spinner keepAliveSpinner = new Spinner(group, SWT.BORDER);
		keepAliveSpinner.setMinimum(0);
		keepAliveSpinner.setMaximum(Integer.MAX_VALUE);
		keepAliveSpinner.setIncrement(5);
		keepAliveSpinner.setPageIncrement(30);
		trackSpinnerUpdates(keepAliveSpinner);

		fd = new FormData();
		fd.top = new FormAttachment(keepAliveLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(cleanSessionCheck, 0, SWT.LEFT);
		keepAliveSpinner.setLayoutData(fd);

		// Keep Alive Unit - Label
		Label keepAliveUnitLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(keepAliveLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(keepAliveSpinner, 10);
		keepAliveUnitLabel.setLayoutData(fd);
		keepAliveUnitLabel.setText(Messages.LABEL_SECONDS);

		// **********************************************
		// Row: Connection Timeout
		// **********************************************
		// Connection Timeout - Label
		Label connectionTimeoutLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(keepAliveLabel, 10);
		fd.left = new FormAttachment(0, 10);
		connectionTimeoutLabel.setLayoutData(fd);
		connectionTimeoutLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_CONNECTION_TIMEOUT);

		// Connection Timeout - Spinner
		final Spinner connectionTimeoutSpinner = new Spinner(group, SWT.BORDER);
		connectionTimeoutSpinner.setMinimum(0);
		connectionTimeoutSpinner.setMaximum(Integer.MAX_VALUE);
		connectionTimeoutSpinner.setIncrement(5);
		connectionTimeoutSpinner.setPageIncrement(30);
		trackSpinnerUpdates(connectionTimeoutSpinner);

		fd = new FormData();
		fd.top = new FormAttachment(connectionTimeoutLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(cleanSessionCheck, 0, SWT.LEFT);
		connectionTimeoutSpinner.setLayoutData(fd);

		// Connection Timeout Unit - Label
		Label connectionTimeoutUnitLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(connectionTimeoutLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(connectionTimeoutSpinner, 10);
		connectionTimeoutUnitLabel.setLayoutData(fd);
		connectionTimeoutUnitLabel.setText(Messages.LABEL_SECONDS);

		// **********************************************
		// Row: Use Login
		// **********************************************
		// Use Login - Label
		Label loginLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(connectionTimeoutLabel, 10);
		fd.left = new FormAttachment(0, 10);
		loginLabel.setLayoutData(fd);
		loginLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_ENABLE_LOGIN);

		// Use Login - Check
		final Button loginCheck = new Button(group, SWT.CHECK);
		fd = new FormData();
		fd.top = new FormAttachment(loginLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(cleanSessionCheck, 0, SWT.LEFT);
		loginCheck.setLayoutData(fd);
		loginCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						loginViewUpdater.update(loginCheck.getSelection());
					}
				});
			}
		});

		// Use Login - Username - Label
		Label usernameLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(loginLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(connectionTimeoutUnitLabel, 0, SWT.LEFT);
		usernameLabel.setLayoutData(fd);
		usernameLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_USERNAME);

		// Use Login - Username - Text
		final Text usernameText = new Text(group, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(usernameLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(usernameLabel, 10);
		fd.width = 120;
		usernameText.setLayoutData(fd);

		// Use Login - Password - Label
		Label passwordLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(loginLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(usernameText, 20);
		passwordLabel.setLayoutData(fd);
		passwordLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_PASSWORD);

		// Use Login - Password - Text
		final Text passwordText = new Text(group, SWT.BORDER | SWT.PASSWORD);
		fd = new FormData();
		fd.top = new FormAttachment(passwordLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(passwordLabel, 10);
		fd.width = 120;
		passwordText.setLayoutData(fd);

		// **********************************************
		// Row: Use Persistence
		// **********************************************
		// Use Persistence - Label
		Label persistLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(loginLabel, 10);
		fd.left = new FormAttachment(0, 10);
		persistLabel.setLayoutData(fd);
		persistLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_ENABLE_PERSISTENCE);

		// Use Persistence - Check
		final Button persistCheck = new Button(group, SWT.CHECK);
		fd = new FormData();
		fd.top = new FormAttachment(persistLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(cleanSessionCheck, 0, SWT.LEFT);
		persistCheck.setLayoutData(fd);
		persistCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						persistenceViewUpdater.update(persistCheck.getSelection());
					}
				});
			}
		});

		// Use Persistence - Directory - Label
		Label directoryLabel = new Label(group, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(persistLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(connectionTimeoutUnitLabel, 0, SWT.LEFT);
		directoryLabel.setLayoutData(fd);
		directoryLabel.setText(Messages.OPT_TAB_GROUP_GENERAL_PERSISTENCE_DIRECTORY);

		// Use Persistence - Directory - Text
		final Text directoryText = new Text(group, SWT.BORDER);
		directoryText.setEditable(false);
		fd = new FormData();
		fd.top = new FormAttachment(directoryLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(usernameText, 0, SWT.LEFT);
		fd.right = new FormAttachment(passwordText, 0, SWT.RIGHT);
		directoryText.setLayoutData(fd);

		// Use Persistence - Browse - Button
		final Button browseDirButton = new Button(group, SWT.PUSH);
		browseDirButton.setText(Messages.LABEL_BROWSE);
		fd = new FormData();
		fd.top = new FormAttachment(directoryText, 0, SWT.CENTER);
		fd.left = new FormAttachment(directoryText, 10);
		browseDirButton.setLayoutData(fd);
		browseDirButton.setEnabled(connection.getOptions().isPersistenceEnabled());
		browseDirButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						DirectoryDialog directoryDialog = new DirectoryDialog(getControl().getShell());
						directoryDialog.setMessage(Messages.OPT_TAB_GROUP_GENERAL_PERSISTENCE_DLG);
						String dir = directoryDialog.open();
						if (dir != null) {
							directoryText.setText(dir);
						}
					}
				});
			}
		});

		// **********************************************
		// DataBinding
		// **********************************************
		final DataBinding dataBinding = DataBindings.createDataBinding();

		// updater
		loginViewUpdater = new ViewUpdater<Boolean>() {
			final DecoratedBinding usrBinding;
			final DecoratedBinding pwdBinding;
			{
				// username is required
				usrBinding = dataBinding.bindText(usernameText, connection, Connection.PROP_OPT_LOGIN_USERNAME,
						Validators.required).hideDecorations();

				// password is not required
				pwdBinding = dataBinding.bindText(passwordText, connection, Connection.PROP_OPT_LOGIN_PASSWORD,
						Converters.stringToChars).hideDecorations();
			}

			@Override
			public void update(Boolean selected) {
				if (selected) {
					usrBinding.showDecorations();
					pwdBinding.showDecorations();
					Widgets.enable(true, usernameText, passwordText);
				}
				else {
					usrBinding.hideDecorations();
					pwdBinding.hideDecorations();
					Widgets.enable(false, usernameText, passwordText);
				}
			}
		};

		// Persistence view updater
		persistenceViewUpdater = new ViewUpdater<Boolean>() {
			final DecoratedBinding binding;
			{
				binding = dataBinding.bindText(directoryText, connection, Connection.PROP_OPT_PERSISTENCE_DIRECTORY,
						Validators.required).hideDecorations();
				directoryText.setText(System.getProperty("user.dir")); //$NON-NLS-1$
			}

			@Override
			public void update(Boolean selected) {
				browseDirButton.setEnabled(selected);
				if (selected) {
					binding.showDecorations();
				}
				else {
					binding.hideDecorations();
				}
			}
		};

		// checkbox
		dataBinding.bindSelection(cleanSessionCheck, connection, Connection.PROP_OPT_CLEAN_SESSION);
		dataBinding.bindSelection(sslCheck, connection, Connection.PROP_OPT_SSL_ENABLED);
		dataBinding.bindSelection(haCheck, connection, Connection.PROP_OPT_HA_ENABLED);
		dataBinding.bindSelection(lwtCheck, connection, Connection.PROP_OPT_LWT_ENABLED);
		dataBinding.bindSelection(loginCheck, connection, Connection.PROP_OPT_LOGIN_ENABLED);
		dataBinding.bindSelection(persistCheck, connection, Connection.PROP_OPT_PERSISTENCE_ENABLED);

		// spinner
		dataBinding.bindSelection(keepAliveSpinner, connection, Connection.PROP_OPT_KEEP_ALIVE_INTERVAL);
		dataBinding.bindSelection(connectionTimeoutSpinner, connection, Connection.PROP_OPT_CONNECTION_TIMEOUT);

		// dispose dataBinding when UI is disposed
		addDisposeListener(dataBinding);

		// initial state
		Widgets.enable(connection.getOptions().isLoginEnabled(), usernameText, passwordText);
	}

	/**
	 * @param spinner
	 */
	private void trackSpinnerUpdates(final Spinner spinner) {

		final ToolTip toolTip = new ToolTip(spinner.getParent().getShell(), SWT.BALLOON | SWT.ICON_ERROR);
		spinner.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						String message = null;
						try {
							int value = Integer.parseInt(spinner.getText());
							int maximum = spinner.getMaximum();
							int minimum = spinner.getMinimum();
							if (value > maximum) {
								message = Messages.bind(Messages.VALIDATION_NUM_GT_MAX, value, maximum);
							}
							else if (value < minimum) {
								message = Messages.bind(Messages.VALIDATION_NUM_LT_MIN, value, minimum);
							}
						}
						catch (Exception ex) {
							message = Messages.bind(Messages.VALIDATION_INVALID_NUM, spinner.getText());
						}

						// display tooltip if error
						if (message != null) {
							spinner.setForeground(Colors.getColor(SWT.COLOR_RED));
							Rectangle rect = spinner.getBounds();
							GC gc = new GC(spinner);
							Point pt = gc.textExtent(spinner.getText());
							gc.dispose();
							toolTip.setLocation(getControl().getDisplay().map(spinner.getParent(), null, rect.x + pt.x,
									rect.y + rect.height));
							toolTip.setMessage(message);
							toolTip.setVisible(true);
						}
						else {
							toolTip.setVisible(false);
							spinner.setForeground(null);
						}
					}
				});
			}
		});

		// reset to defaults if invalid inputs
		spinner.addFocusListener(new FocusAdapter() {
			final int DEFAULT = spinner.getSelection();

			@Override
			public void focusLost(FocusEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						// in case of this tab is already disposed
						// and the UI is updated async
						if (spinner.isDisposed()) {
							return;
						}

						int value = spinner.getSelection();
						if (value > spinner.getMaximum() || value < spinner.getMinimum()) {
							spinner.setSelection(DEFAULT);
						}
					}
				});
			}
		});
	}
}
