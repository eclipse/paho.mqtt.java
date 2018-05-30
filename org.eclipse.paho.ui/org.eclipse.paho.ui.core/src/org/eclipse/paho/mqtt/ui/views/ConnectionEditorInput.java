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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.paho.mqtt.ui.Constants.ImageKeys;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.util.Images;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * Connection editor input
 * 
 * @author Bin Zhang
 */
public class ConnectionEditorInput implements IEditorInput {
	private final String id;
	private final Connection connection;

	/**
	 * @param connection
	 */
	public ConnectionEditorInput(Connection connection) {
		this.connection = connection;
		this.id = connection.getId();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Images.getDescriptor(ImageKeys.IMG_CONNECTION);
	}

	@Override
	public String getName() {
		return connection.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return connection.getName();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == Connection.class) {
			return connection;
		}

		if (adapter == IEditorInput.class) {
			return this;
		}

		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConnectionEditorInput other = (ConnectionEditorInput) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("id=").append(id).append(",")
				.append("name=").append(connection.getName()).append("]").toString();
	}

}
