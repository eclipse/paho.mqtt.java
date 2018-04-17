/*******************************************************************************
 * Copyright (c) 2012 Eurotech Inc. All rights reserved.
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
 *    Chad Kienle
 */

package org.eclipse.paho.client.eclipse.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

/**
 * An MQTT client view.  The top half provides three tabs for Connecting to an MQTT broker, publishing messages,
 * and subscribing on topics.  The bottom half of the view contains a log of all MQTT connections, publications, and
 * subscription events.
 */
public class MqttClientView extends ViewPart implements MqttCallback {

	// The display and parent composite
    private Display display;
	private Composite parent;
	
	// MQTT connection parameters with defaults
	private static MqttClient mqttClient		= null;
	private static MqttConnectOptions connOpts	= null;
	private static String connectAddress 		= ""; //$NON-NLS-1$
	private static int connectPort 				= 1883;
	private static String clientId 				= ""; //$NON-NLS-1$
	private static short keepAlive 				= 30;
	private static boolean cleanStart 			= true;
	private static boolean firstConnect 		= true;
	private static Boolean connected 			= new Boolean(false);
	private static String tmpMsg 				= null;
	private static String willTopic 			= ""; //$NON-NLS-1$
	private static String willMessage 			= ""; //$NON-NLS-1$
	private static int willQos 					= 0;
	private static boolean willRetain 			= false;
	private static String username 				= ""; //$NON-NLS-1$
	private static String password 				= ""; //$NON-NLS-1$
	
	// Current publish parameters with defaults
	private String publishTopic = null;
	private int publishQos 		= 0;
	private boolean retain 		= false;
	private byte [] payload		= null;
	private boolean useWill 	= false;
	
	// Current subscribe topic and QoS with defaults
	private String subscribeTopic 	= null;
	private int subscribeQos 		= 0;
	
	// The text boxes to store the values of the MQTT parameters for connecting/publishing/subscribing
	private Text subscribeTopicValue;
	private Text publishTopicValue;
	private Text publishPayloadValue;
	private Text publishFileName;
	private Text brokerAddressValue;
	private Text brokerPortValue;
	private Text clientIdValue;
	private Text keepAliveValue;
	private Text messageLog;
	private Text willTopicValue;
	private Text willMessageValue;
	private Text usernameValue;
	private Text passwordValue;
	
	// The buttons needed to store the state of MQTT parameters for connecting/publishing/subscribing
	private Button willCheckBox;
	private Button willRetainCheckBox;
	private Button cleanStartCheckBox;
	private Button connectButton;
	private Button disconnectButton;
	private Button publishRetainCheckBox;
	
	// The Groups associate with each tab
	private Group connectionGroup;
	private Group publishGroup;
	private Group subscribeGroup;
	
	// Quality of Service drop down selection menus
	private Combo subscribeQosDrop;
	private Combo publishQosDrop;
	private Combo willQosDrop;

	/**
	 *  Constructor
	 */
	public MqttClientView() {
		super();
	}

	/**
	 *  Sets the focus
	 */
	public void setFocus() {
	}

	/**
	 *  Create the control
	 */
	public void createPartControl(Composite parent) {
		// Connection Group
		this.parent = parent;
		display = parent.getDisplay();
		FillLayout masterLayout = new FillLayout();
		parent.setLayout(masterLayout);
		
		// Create composite with rows
		SashForm sashform = new SashForm(parent, SWT.VERTICAL);
		
		final TabFolder tabFolder = new TabFolder(sashform, SWT.V_SCROLL | SWT.H_SCROLL);

	    // Create each tab and set its text, tool tip text, image, and control
		// Connection tab
	    TabItem one = new TabItem(tabFolder, SWT.NONE);
	    one.setText(Messages.MqttClientView_6);
	    one.setToolTipText(Messages.MqttClientView_7);
	    one.setControl(getConnectionControl(tabFolder));

	    // Publish tab
	    TabItem two = new TabItem(tabFolder, SWT.NONE);
	    two.setText(Messages.MqttClientView_8);
	    two.setToolTipText(Messages.MqttClientView_9);
	    two.setControl(getPublishControl(tabFolder));

	    // Subscribe tab
	    TabItem three = new TabItem(tabFolder, SWT.NONE);
	    three.setText(Messages.MqttClientView_10);
	    three.setToolTipText(Messages.MqttClientView_11);
	    three.setControl(getSubscribeControl(tabFolder));
		
		messageLog = new Text(sashform, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
	    messageLog.setFont(new Font(display, new FontData("Courier New", 10, SWT.NORMAL))); //$NON-NLS-1$

		sashform.setWeights(new int[]{2,1});
	}

	// Listener for LWT enable/disable events
	private SelectionListener willListener = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			useWill = willCheckBox.getSelection();
			willTopicValue.setEnabled(useWill);
			willMessageValue.setEnabled(useWill);
			willQosDrop.setEnabled(useWill);
			willRetainCheckBox.setEnabled(useWill);
		}
	};
	
	// Listener for widget selection events
	private SelectionListener selectListener = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			cleanStart = cleanStartCheckBox.getSelection();
			updateInfo();
		}
	};
	
	// Listener for connect events
	private SelectionListener connectListener = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {}
		public void widgetSelected(SelectionEvent e) {
			if(e.getSource() == connectButton) {
				connect();
			} else if(e.getSource() == disconnectButton) {
				disconnect();
			}
		}
	};
	
	// Listener for publish (text) events
	private SelectionListener publishListener = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {}
		public void widgetSelected(SelectionEvent e) {
			publishTopic = publishTopicValue.getText();
			publishQos = Integer.parseInt(publishQosDrop.getText());
			retain = publishRetainCheckBox.getSelection();
			payload = publishPayloadValue.getText().getBytes();
			publish();
		}
	};
	
	// Listener for publish (file) events
	private SelectionListener publishFileListener = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {}
		public void widgetSelected(SelectionEvent e) {
			publishTopic = publishTopicValue.getText();
			publishQos = Integer.parseInt(publishQosDrop.getText());
			retain = publishRetainCheckBox.getSelection();
			String filename = publishFileName.getText();
			try {
				payload = getBytesFromFile(new File(filename));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			publish();
		}
	};
	
	// Listener for subscribe events
	private SelectionListener subscribeListener = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {}
		public void widgetSelected(SelectionEvent e) {
			subscribeTopic = subscribeTopicValue.getText();
			subscribeQos = Integer.parseInt(subscribeQosDrop.getText());
			subscribe();
			publishTopicValue.setText(subscribeTopic);
		}
	};
	
	// Listener for unsubscribe events
	private SelectionListener unsubscribeListener = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {}
		public void widgetSelected(SelectionEvent e) {
			subscribeTopic = subscribeTopicValue.getText();
			unsubscribe();
		}
	};

	/**
	 * A parameter's value has changed, update info
	 */
	public void valueChanged(Text text) {
		updateInfo();
	}
	
	/**
	 * Broker address value has changed, update info
	 */
	private void addressValueChanged(Text text) {
		updateInfo();
		firstConnect = true;
	}
	
	/**
	 * Updates the connection information
	 */
	private void updateInfo() {
		connectAddress = brokerAddressValue.getText();
		willTopic = willTopicValue.getText();
		willMessage = willMessageValue.getText();
		clientId = clientIdValue.getText();
		willQos = Integer.parseInt(willQosDrop.getText());
		willRetain = willRetainCheckBox.getSelection();
		username = usernameValue.getText();
		password = passwordValue.getText();
		try {
			connectPort = Integer.parseInt(brokerPortValue.getText());
		} catch (NumberFormatException e) {
		}
		try {
			keepAlive = Short.parseShort(keepAliveValue.getText());
		} catch (NumberFormatException e) {
		}
	}
	
	/**
	 * Connects to the broker
	 */
	private void connect() {
		// Check if the client is currently connected
		if (!connected) {
			// Update connection information
			updateInfo();
			// Build connection string
			String connectString = "tcp://" + connectAddress + ":" + connectPort; //$NON-NLS-1$ //$NON-NLS-2$
			if(clientId == null || clientId.length() < 1) {
				out(getDate() + Messages.MqttClientView_15 + connectString + Messages.MqttClientView_16);
				return;
			}
			// Instantiate client
			try {
				if (firstConnect) {
					mqttClient = new MqttClient(connectString, clientId);
					mqttClient.setCallback(this);
					firstConnect = false;
				}
			} catch (MqttException e) {
				out(getDate() + Messages.MqttClientView_17 + e.getMessage());
				e.printStackTrace();
			}
			
			// Set connection options
			connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(cleanStart);
			connOpts.setConnectionTimeout(30);
			connOpts.setKeepAliveInterval(keepAlive);
			if (username.length() > 0 && password.length() > 0) {
				connOpts.setPassword(password.toCharArray());
				connOpts.setUserName(username);
			}
			if (useWill) {
				if(willTopic == null || willTopic.equals("")) { //$NON-NLS-1$
					out(Messages.MqttClientView_19);
					return;
				}
				connOpts.setWill(mqttClient.getTopic(willTopic), willMessage.getBytes(), willQos, willRetain);
			}
			// Attempt to connect
			try {
				out(getDate() + Messages.MqttClientView_20 + connectString);
				mqttClient.connect(connOpts);
			
				connected = true;
				out(getDate() + Messages.MqttClientView_21 + clientId);
			} catch (MqttException e) {
				out(getDate() + Messages.MqttClientView_22 + e.getMessage());
				e.printStackTrace();
			}
		} else {
			out(Messages.MqttClientView_23);
		}
	}
	
	/**
	 * Disconnects from the broker
	 */
	private void disconnect() {
		try {
			if(connected) {
				mqttClient.disconnect();
				out(getDate() + Messages.MqttClientView_24);
			} else {
				out(getDate() + Messages.MqttClientView_25);
			}
		} catch (MqttException e) {
			out(getDate() + Messages.MqttClientView_26 + e.getMessage());
		}
		connected = false;
	}
	
	/**
	 * Publishes a message
	 */
	private void publish() {
		
		if(mqttClient != null) {
			if(publishTopic == null || publishTopic.equals("")) { //$NON-NLS-1$
				out(Messages.MqttClientView_28);
				return;
			}
			try {
				MqttTopic topic = mqttClient.getTopic(publishTopic);
				topic.publish(payload, publishQos, retain);
				out(getDate() + Messages.MqttClientView_29);
				out(Messages.MqttClientView_30 + publishTopic + "\"\n" +  //$NON-NLS-2$
				    Messages.MqttClientView_32 + publishQos +  "\n" + //$NON-NLS-2$
				    Messages.MqttClientView_34 + retain + "\n" + //$NON-NLS-2$
				    Messages.MqttClientView_36 + new String(payload) + "\""); //$NON-NLS-2$
			} catch (MqttPersistenceException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (MqttException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				out(Messages.MqttClientView_12);
			}
		}
	}
	
	/**
	 * Subscribes on a topic
	 */
	private void subscribe() {
		if(mqttClient != null) {
			try {
				String [] topicArray = {subscribeTopic};
				int [] qosArray = {subscribeQos};
				mqttClient.subscribe(topicArray, qosArray);
				out(getDate() + Messages.MqttClientView_38);
				out(Messages.MqttClientView_39 + subscribeTopic + "\"\n" +  //$NON-NLS-2$
				    Messages.MqttClientView_41 + subscribeQos);
			} catch (MqttPersistenceException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Unsubscribes from a topic
	 */
	private void unsubscribe() {
		if(mqttClient != null) {
			try {
				String [] topicArray = {subscribeTopic};
				mqttClient.unsubscribe(topicArray);
				out(getDate() + Messages.MqttClientView_42);
				out(Messages.MqttClientView_43 + subscribeTopic + "\""); //$NON-NLS-2$
			} catch (MqttPersistenceException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Logs a message that the broker connection has been lost and attempts to reconnect.
	 */
	public void connectionLost(Throwable cause) {
		connected = false;
		syncOut(getDate() + Messages.MqttClientView_45);
		String connectString = connectAddress + ":" + connectPort; //$NON-NLS-1$
		syncOut(getDate() + Messages.MqttClientView_47 + connectString);
		try {
			mqttClient.connect(connOpts);
		} catch (Exception e) {
			syncOut(getDate() + Messages.MqttClientView_48);
			syncOut(getDate() + Messages.MqttClientView_49);
		}
		connected = true;
		syncOut(getDate() + Messages.MqttClientView_50 + clientId);
	}
	
	@Override
	/**
	 * Logs a message that has arrived from the broker
	 */
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		syncOut(getDate() + Messages.MqttClientView_51);
		syncOut(Messages.MqttClientView_52 + topic + "\""); //$NON-NLS-2$
		syncOut(Messages.MqttClientView_54 + new String(message.getPayload()) + "\""); //$NON-NLS-2$
	}

	/**
	 * Logs that a publish has completed (an acknowledgement has been received from the broker
	 */
	public void deliveryComplete(MqttDeliveryToken token) {
		syncOut(getDate() + Messages.MqttClientView_56);
	}
	
	/**
	 * Synchronously writes a message to the log.
	 */
	private void syncOut(String msg) {
		tmpMsg = msg;
		parent.getDisplay().syncExec(new Runnable() {
			public void run(){
				out(tmpMsg);
			}
		});
	}
	
	/**
	 * Returns a hex string representation of the byte array
	 */
	public static String getHexString(byte[] b) {
		  String result = ""; //$NON-NLS-1$
		  for (int i=0; i < b.length; i++) {
		    result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		  }
		  return result;
	}
	
	/**
	 * Writes a message to the log
	 */
	private void out(String message) {
		messageLog.append(message + "\n"); //$NON-NLS-1$
	}
	
	/**
	 * Return the current date as a formatted string.
	 */
	private String getDate() {
		System.currentTimeMillis();
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy/MM/dd hh:mm:ss.SS"); //$NON-NLS-1$
		Date currentTime_1 = new Date();
		String dateString = formatter.format(currentTime_1);
		return "[" + dateString + "] "; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Creates the connection composite and populates it with all the widgets needed to establish an MQTT connection
	 */
	private Control getConnectionControl(TabFolder tabFolder) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new FillLayout(SWT.VERTICAL));

		// Connection group
		connectionGroup = new Group(composite, SWT.NONE);
	    connectionGroup.setLayout(new GridLayout(2, false));
	    connectionGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
	    connectionGroup.setText(Messages.MqttClientView_62);
		
	    // Broker address
		Label brokerAddressLabel = new Label(connectionGroup, SWT.NULL);
		brokerAddressLabel.setText(Messages.MqttClientView_63);
		brokerAddressValue = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		brokerAddressValue.setLayoutData(new GridData(120,13));
		brokerAddressValue.setText(connectAddress);
		brokerAddressValue.setToolTipText(Messages.MqttClientView_64);
		
		// Broker port
		Label brokerPortLabel = new Label(connectionGroup, SWT.NULL);
		brokerPortLabel.setText(   Messages.MqttClientView_65);
		brokerPortValue = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		brokerPortValue.setLayoutData(new GridData(30,13));
		brokerPortValue.setText(Integer.toString(connectPort));
		brokerPortValue.setToolTipText(Messages.MqttClientView_66);
		
		// Client ID
		Label clientIdLabel = new Label(connectionGroup, SWT.NULL);
		clientIdLabel.setText(     Messages.MqttClientView_67);
		clientIdValue = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		clientIdValue.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		clientIdValue.setText(clientId);
		clientIdValue.setToolTipText(Messages.MqttClientView_68);

		// Username
		Label usernameLabel = new Label(connectionGroup, SWT.NULL);
		usernameLabel.setText(     Messages.MqttClientView_69);
		usernameValue = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		usernameValue.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		usernameValue.setText(username);
		usernameValue.setToolTipText(Messages.MqttClientView_70);
		
		// Password
		Label passwordLabel = new Label(connectionGroup, SWT.NULL);
		passwordLabel.setText(     Messages.MqttClientView_71);
		passwordValue = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		passwordValue.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		passwordValue.setText(password);
		passwordValue.setToolTipText(Messages.MqttClientView_72);
		
		// Keep alive value in seconds
		Label keepAliveLabel = new Label(connectionGroup, SWT.NULL);
		keepAliveLabel.setText(    Messages.MqttClientView_73);
		keepAliveValue = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		keepAliveValue.setLayoutData(new GridData(30,13));
		keepAliveValue.setText(Short.toString(keepAlive));
		keepAliveValue.setToolTipText(Messages.MqttClientView_74);

		// Clean start
		// we should use text referring to "Clean Session" for consistency
		Label cleanStartLabel = new Label(connectionGroup, SWT.NULL);
		cleanStartLabel.setText(Messages.MqttClientView_75);
		cleanStartCheckBox = new Button(connectionGroup, SWT.CHECK);
		cleanStartCheckBox.setSelection(cleanStart);
		cleanStartCheckBox.setToolTipText(Messages.MqttClientView_76);
		
		// LWT checkbox
		Label useWillLabel = new Label(connectionGroup, SWT.NULL);
		useWillLabel.setText(Messages.MqttClientView_77);
		willCheckBox = new Button(connectionGroup, SWT.CHECK);
		willCheckBox.setSelection(false);
		willCheckBox.setToolTipText(Messages.MqttClientView_78);
		
		// LWT topic
		Label willTopicLabel = new Label(connectionGroup, SWT.NULL);
		willTopicLabel.setText(Messages.MqttClientView_79);
		willTopicValue = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		willTopicValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		willTopicValue.setText(willTopic);
		willTopicValue.setEnabled(useWill);
		willTopicValue.setToolTipText(Messages.MqttClientView_80);
		
		// LWT message
		Label willMessageLabel = new Label(connectionGroup, SWT.NULL);
		willMessageLabel.setText(Messages.MqttClientView_81);
		willMessageValue = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		willMessageValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		willMessageValue.setText(willMessage);
		willMessageValue.setEnabled(useWill);
		willMessageValue.setToolTipText(Messages.MqttClientView_82);
		
		// LWT quality of service
		Label willQosLabel = new Label(connectionGroup, SWT.NULL);
		willQosLabel.setText(Messages.MqttClientView_83);
		willQosDrop = new Combo(connectionGroup, SWT.DROP_DOWN | SWT.BORDER);
		willQosDrop.add("0"); //$NON-NLS-1$
		willQosDrop.add("1"); //$NON-NLS-1$
		willQosDrop.add("2"); //$NON-NLS-1$
		willQosDrop.select(0);
		
		// LWT retained flag
		Label willRetainedLabel = new Label(connectionGroup, SWT.NULL);
		willRetainedLabel.setText(Messages.MqttClientView_87);
		willRetainCheckBox = new Button(connectionGroup, SWT.CHECK);
		willRetainCheckBox.setSelection(false);
		willRetainCheckBox.setToolTipText(Messages.MqttClientView_88);
		
		// Connect button
		connectButton = new Button(connectionGroup, SWT.PUSH);
		connectButton.setLayoutData(new GridData(ClientConstants.BUTTON_WIDTH, ClientConstants.BUTTON_HEIGHT));
		connectButton.setText(Messages.MqttClientView_89);
		connectButton.setToolTipText(Messages.MqttClientView_90);
		
		// Disconnect button
		disconnectButton = new Button(connectionGroup, SWT.PUSH);
		disconnectButton.setLayoutData(new GridData(ClientConstants.BUTTON_WIDTH, ClientConstants.BUTTON_HEIGHT));
		disconnectButton.setText(Messages.MqttClientView_91);
		disconnectButton.setToolTipText(Messages.MqttClientView_92);
		
		// A modify listener for modifications to textual parameters
		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				valueChanged((Text) e.widget);
			}
		};
		
		// A modify listener for modification to the broker address
		ModifyListener addressListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				addressValueChanged((Text) e.widget);
			}
		};
		
		// Set listeners
		brokerAddressValue.addModifyListener(addressListener);
		brokerPortValue.addModifyListener(addressListener);
		clientIdValue.addModifyListener(listener);
		usernameValue.addModifyListener(listener);
		passwordValue.addModifyListener(listener);
		keepAliveValue.addModifyListener(listener);
		cleanStartCheckBox.addSelectionListener(selectListener);
		connectButton.addSelectionListener(connectListener);
		disconnectButton.addSelectionListener(connectListener);
		willCheckBox.addSelectionListener(willListener);
		willTopicValue.addModifyListener(listener);
		willMessageValue.addModifyListener(listener);
		
		return composite;
	}

	/**
	 * Creates the publish composite and populates it with all the widgets needed to make an MQTT publish.
	 * TODO: add tooltips for input fields and controls
	 */
	private Control getPublishControl(TabFolder tabFolder) {
		@SuppressWarnings("unused")
		Label tmpNullLabel;
		
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new FillLayout(SWT.VERTICAL));
		
		// Publish Group
		publishGroup = new Group(composite, SWT.NONE);
		publishGroup.setLayout(new GridLayout(3, false));
		publishGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		publishGroup.setText(Messages.MqttClientView_93);
		
		// Publish topic
		Label publishTopicLabel = new Label(publishGroup, SWT.NULL);
		publishTopicLabel.setText(Messages.MqttClientView_94);
		publishTopicValue = new Text(publishGroup, SWT.SINGLE | SWT.BORDER);
		publishTopicValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		publishTopicValue.setSize(110, 1);
		
		tmpNullLabel = new Label(publishGroup, SWT.NULL);
		
		// Publish quality of service
		Label publishQosLabel = new Label(publishGroup, SWT.NULL);
		publishQosLabel.setText(Messages.MqttClientView_95);
		publishQosDrop = new Combo(publishGroup, SWT.DROP_DOWN | SWT.BORDER);
		publishQosDrop.add("0"); //$NON-NLS-1$
		publishQosDrop.add("1"); //$NON-NLS-1$
		publishQosDrop.add("2"); //$NON-NLS-1$
		publishQosDrop.select(0);
		
		tmpNullLabel = new Label(publishGroup, SWT.NULL);
		
		// Retained flag
		Label publishRetainedLabel = new Label(publishGroup, SWT.NULL);
		publishRetainedLabel.setText(Messages.MqttClientView_96);
		publishRetainCheckBox = new Button(publishGroup, SWT.CHECK);
		publishRetainCheckBox.setSelection(false);
		publishRetainCheckBox.setToolTipText(Messages.MqttClientView_97);
		
		tmpNullLabel = new Label(publishGroup, SWT.NULL);
		
		// Publish payload (text)
		Label publishPayloadLabel = new Label(publishGroup, SWT.NULL);
		publishPayloadLabel.setText(Messages.MqttClientView_99);
		publishPayloadValue = new Text(publishGroup, SWT.SINGLE | SWT.BORDER);
		publishPayloadValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		publishPayloadValue.setSize(110, 1);
		
		tmpNullLabel = new Label(publishGroup, SWT.NULL);
		tmpNullLabel = new Label(publishGroup, SWT.NULL);
		
		Button publishPayloadButton = new Button(publishGroup, SWT.PUSH);
		publishPayloadButton.setLayoutData(new GridData(ClientConstants.BUTTON_WIDTH, ClientConstants.BUTTON_HEIGHT));
		publishPayloadButton.setText(Messages.MqttClientView_100);
		
		tmpNullLabel = new Label(publishGroup, SWT.NULL);
		
		// Publish payload (file)
		Label publishFileLabel = new Label(publishGroup, SWT.NULL);
		publishFileLabel.setText(Messages.MqttClientView_101);
		publishFileName = new Text(publishGroup, SWT.SINGLE | SWT.BORDER);
		publishFileName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		publishFileName.setSize(100, 1);

		// Browse button for selecting a filename
		Button browseButton = new Button(publishGroup, SWT.PUSH);
		browseButton.setText(Messages.MqttClientView_102);
		
		tmpNullLabel = new Label(publishGroup, SWT.NULL);

		// Publish file button
		Button publishFileButton = new Button(publishGroup, SWT.PUSH);
		publishFileButton.setLayoutData(new GridData(ClientConstants.BUTTON_WIDTH, ClientConstants.BUTTON_HEIGHT));
		publishFileButton.setText(Messages.MqttClientView_103);

		// Set selection listeners
		publishPayloadButton.addSelectionListener(publishListener);
		publishFileButton.addSelectionListener(publishFileListener);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(new Shell(), SWT.NULL);
				String path = dialog.open();
				if (path != null) {
					File file = new File(path);
					if (file.isFile())
						displayFiles(new String[] { file.toString()});
					else
						displayFiles(file.list());

				}
			}
		});
		
		return composite;
	}

	private void displayFiles(String[] files) {
		for (int i = 0; files != null && i < files.length; i++) {
			publishFileName.setText(files[i]);
			publishFileName.setEditable(true);
		}
	}

	/**
	 * Creates the subscribe composite and populates it with all the widgets needed to make an MQTT subscription.
	 * TODO: add tooltips for input fields and controls
	 */
	private Control getSubscribeControl(TabFolder tabFolder) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new FillLayout(SWT.VERTICAL));
		
		// Subscribe Group
		subscribeGroup = new Group(composite, SWT.NONE);
		subscribeGroup.setLayout(new GridLayout(2, false));
		subscribeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		subscribeGroup.setText(Messages.MqttClientView_104);
		
		// Subscribe topic
		Label subscribeTopicLabel = new Label(subscribeGroup, SWT.NULL);
		subscribeTopicLabel.setText(Messages.MqttClientView_105);
		subscribeTopicValue = new Text(subscribeGroup, SWT.SINGLE | SWT.BORDER);
		subscribeTopicValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		subscribeTopicValue.setSize(80, 1);
		
		// Subscribe quality of service
		Label subscribeQosLabel = new Label(subscribeGroup, SWT.NULL);
		subscribeQosLabel.setText(Messages.MqttClientView_106);
		subscribeQosDrop = new Combo(subscribeGroup, SWT.DROP_DOWN | SWT.BORDER);
		subscribeQosDrop.add("0"); //$NON-NLS-1$
		subscribeQosDrop.add("1"); //$NON-NLS-1$
		subscribeQosDrop.add("2"); //$NON-NLS-1$
		subscribeQosDrop.select(0);
		
		// Subscribe button
		Button subscribeButton = new Button(subscribeGroup, SWT.PUSH);
		subscribeButton.setLayoutData(new GridData(ClientConstants.BUTTON_WIDTH, ClientConstants.BUTTON_HEIGHT));
		subscribeButton.setText(Messages.MqttClientView_110);
		
		// Unsubscribe button
		Button unsubscribeButton = new Button(subscribeGroup, SWT.PUSH);
		unsubscribeButton.setLayoutData(new GridData(ClientConstants.BUTTON_WIDTH, ClientConstants.BUTTON_HEIGHT));
		unsubscribeButton.setText(Messages.MqttClientView_111);
		
		// Set selection listeners
		subscribeButton.addSelectionListener(subscribeListener);
		unsubscribeButton.addSelectionListener(unsubscribeListener);
		
		return composite;
	}
	
	/**
	 * Return the bytes from a file
	 */
	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);
        long length = file.length();
        byte[] bytes = new byte[(int)length];
    
        // Read in file
        int offset = 0;
        int numRead = 0;
        while ((offset < bytes.length) && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
        	offset += numRead;
        }
    
        // Close stream
        is.close();
        
        return bytes;
    }

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub
		
	}
}
