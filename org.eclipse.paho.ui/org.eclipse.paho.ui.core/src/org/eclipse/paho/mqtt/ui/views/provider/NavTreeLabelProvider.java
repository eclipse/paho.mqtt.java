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
package org.eclipse.paho.mqtt.ui.views.provider;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.paho.mqtt.ui.Constants.ImageKeys;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.util.Images;
import org.eclipse.swt.graphics.Image;

/**
 * Nav tree label provider
 * 
 * @author Bin Zhang
 */
public class NavTreeLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		return ((Connection) element).getName();
	}

	@Override
	public Image getImage(Object obj) {
		return Images.get(ImageKeys.IMG_CONNECTION);
	}
}
