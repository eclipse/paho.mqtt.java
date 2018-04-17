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
package org.eclipse.paho.mqtt.ui.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.paho.mqtt.ui.Activator;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

/**
 * SWT Image helper
 * 
 * @author Bin Zhang
 */
public final class Images {
	private static Set<String> keys;
	private static ImageRegistry register;
	private static final String ICONS_ENTRY = "icons"; //$NON-NLS-1$
	private static final String[] ICONS_EXTS = new String[] { ".gif", ".png", ".bmp", ".jpg" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/**
	 * @param key
	 */
	public static ImageDescriptor getDescriptor(String key) {
		ImageDescriptor image = register.getDescriptor(key);
		if (image == null) {
			image = getMissingImageDescriptor();
		}
		return image;
	}

	/**
	 * @param key
	 */
	public static Image get(String key) {
		Image image = register.get(key);
		if (image == null) {
			image = getMissingImageDescriptor().createImage();
		}
		return image;
	}

	/**
	 * Returns a set view of image keys
	 */
	public static Set<String> getImageKeys() {
		return Collections.unmodifiableSet(keys);
	}

	/**
	 * Returns absolute path in the FS
	 * 
	 * @param path
	 */
	public static Image getImageFromURL(String path) {
		try {
			return ImageDescriptor.createFromURL(new File(path).toURI().toURL()).createImage();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			// ignore
		}
		return getMissingImageDescriptor().createImage();
	}

	/**
	 * Returns relative path in the Project
	 * @param url
	 */
	public static Image getImageFromURL(URL url) {
		if (url != null) {
			return ImageDescriptor.createFromURL(url).createImage();
		}

		return getMissingImageDescriptor().createImage();
	}

	/**
	 * Returns missing image descriptor
	 */
	public static ImageDescriptor getMissingImageDescriptor() {
		return ImageDescriptor.getMissingImageDescriptor();
	}

	/**
	 * Disposes all images and clear all image keys.
	 */
	public static void dispose() {
		register.dispose();
		keys.clear();
	}

	/**
	 * init image keys
	 */
	private static void initialize() {
		register = new ImageRegistry();
		keys = new HashSet<String>();

		//
		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		URL url = bundle.getEntry(ICONS_ENTRY);
		try {
			url = FileLocator.toFileURL(url);
		}
		catch (Exception e) {
			e.printStackTrace();
			// ignore
		}

		// register all image files
		File file = new File(url.getPath());
		for (File f : file.listFiles()) {
			if (!f.isFile()) {
				continue;
			}
			String name = f.getName().trim().toLowerCase();
			for (String extesion : ICONS_EXTS) {
				if (name.endsWith(extesion)) {
					break;
				}
			}

			String key = name.substring(0, name.indexOf('.'));
			URL fullPathString = bundle.getEntry(ICONS_ENTRY + "/" + name); //$NON-NLS-1$
			ImageDescriptor des = ImageDescriptor.createFromURL(fullPathString);
			register.put(key, des);
			keys.add(key);
		}
	}

	// init
	static {
		initialize();
	}

}
