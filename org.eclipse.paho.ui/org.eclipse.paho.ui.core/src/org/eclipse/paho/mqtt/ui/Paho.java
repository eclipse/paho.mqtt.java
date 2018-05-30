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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.mqtt.ui.core.IConnectionManager;
import org.eclipse.paho.mqtt.ui.core.event.IEventService;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.util.Files;
import org.eclipse.paho.mqtt.ui.util.Strings;
import org.eclipse.paho.mqtt.ui.views.ConnectionEditorInput;
import org.eclipse.paho.mqtt.ui.views.ConnectionEditorPart;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * @author Bin Zhang
 */
@SuppressWarnings("unchecked")
public final class Paho {
	// the UI persistent state data dir
	private static File dataDir;

	// init
	static {
		// Note: the paho ui jar should be located at: eclipse/plugins/xxxx.jar
		// so the data dir will be located at: eclipse/<data_dir>
		dataDir = new File(Files.getJarPath().getParentFile().getParent(), Constants.DATA_DIR);

		//
		String customDataDirStr = System.getProperty(Constants.PROP_DATA_DIR);
		if (!Strings.isEmpty(customDataDirStr)) {
			File customDataDir = new File(customDataDirStr);
			if (!customDataDir.exists()) {
				if (customDataDir.mkdirs()) {
					dataDir = customDataDir;
				}
			}
			else {
				if (customDataDir.isDirectory() && customDataDir.canWrite()) {
					dataDir = customDataDir;
				}
			}
		}

		if (!dataDir.exists()) {
			dataDir.mkdirs();
		}
	}

	/**
	 * Save connection data
	 * 
	 * @param connection
	 */
	public static void saveConnection(Connection connection) {
		try {
			Files.writeObject(connection, new File(dataDir, connection.getId() + Constants.DATA_FILE_EXTENSION));
		}
		catch (IOException e) {
			throw new PahoException(e);
		}
	}

	/**
	 * Delete connection data
	 * 
	 * @param connection
	 */
	public static void deleteConnection(Connection connection) {
		new File(dataDir, connection.getId() + Constants.DATA_FILE_EXTENSION).delete();

	}

	/**
	 * Load all saved connections data
	 */
	public static List<Connection> loadConnections() {
		List<Connection> connections = new ArrayList<Connection>();

		//
		File[] files = dataDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(Constants.DATA_FILE_EXTENSION);
			}
		});

		for (File file : files) {
			try {
				connections.add((Connection) Files.readObject(file));
			}
			catch (Exception e) {
				// ignore it, may be invalid files are there
				e.printStackTrace();
			}
		}

		return connections;
	}

	/**
	 * Return IConnectionManager
	 */
	public static IConnectionManager getConnectionManager() {
		return getService(IConnectionManager.class);
	}

	/**
	 * Return IEventService
	 */
	public static IEventService getEventService() {
		return getService(IEventService.class);
	}

	/**
	 * @param editor
	 * @param save
	 */
	public static boolean closeConnectionEditor(IEditorPart editor, boolean save) {
		return getActivePage().closeEditor(editor, save);
	}

	/**
	 * @param connection
	 */
	public static IEditorPart openConnectionEditor(Connection connection) {
		try {
			return getActivePage().openEditor(new ConnectionEditorInput(connection), ConnectionEditorPart.ID);
		}
		catch (PartInitException e) {
			throw new PahoException(e);
		}
	}

	/**
	 * @param clazz
	 * @return service instance
	 */
	private static <T> T getService(Class<T> clazz) {
		return (T) PlatformUI.getWorkbench().getService(clazz);
	}

	/**
	 * Returns IWorkbenchPage
	 */
	private static IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
}
