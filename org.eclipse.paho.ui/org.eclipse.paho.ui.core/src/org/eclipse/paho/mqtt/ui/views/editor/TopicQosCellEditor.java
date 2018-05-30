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
package org.eclipse.paho.mqtt.ui.views.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.paho.mqtt.ui.core.model.QoS;
import org.eclipse.paho.mqtt.ui.core.model.Topic;
import org.eclipse.swt.SWT;

/**
 * Topic QoS editor for subscription
 * 
 * @author Bin Zhang
 */
public class TopicQosCellEditor extends EditingSupport {
	private CellEditor cellEditor;

	/**
	 * @param viewer
	 */
	public TopicQosCellEditor(TableViewer viewer) {
		super(viewer);
		List<String> qosList = new ArrayList<String>();
		for (QoS qos : QoS.values()) {
			qosList.add(qos.getLabel());
		}
		cellEditor = new ComboBoxCellEditor(viewer.getTable(), qosList.toArray(new String[0]), SWT.READ_ONLY);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return cellEditor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		QoS qos = ((Topic) element).getQos();
		return qos.getValue();
	}

	@Override
	protected void setValue(Object element, Object value) {
		((Topic) element).setQos(QoS.valueOf((Integer) value));
		getViewer().update(element, null);
	}

}
