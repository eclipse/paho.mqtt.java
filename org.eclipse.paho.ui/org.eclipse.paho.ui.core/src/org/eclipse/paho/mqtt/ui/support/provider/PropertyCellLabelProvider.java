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
package org.eclipse.paho.mqtt.ui.support.provider;

import java.beans.PropertyDescriptor;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.paho.mqtt.ui.util.Beans;
import org.eclipse.paho.mqtt.ui.util.Colors;

/**
 * 
 * @author Bin Zhang
 * 
 */
public final class PropertyCellLabelProvider<T> extends ColumnLabelProvider {
	private final PropertyDescriptor descriptor;
	private final IValueFormatter<T> valueFormatter;

	/**
	 * @param descriptor
	 * @param valueFormatter
	 */
	public PropertyCellLabelProvider(PropertyDescriptor descriptor, IValueFormatter<T> valueFormatter) {
		this.descriptor = descriptor;
		this.valueFormatter = valueFormatter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(ViewerCell cell) {
		super.update(cell);

		Object value = Beans.readProperty(cell.getElement(), descriptor);

		if (value == null) {
			// SWT.COLOR_GRAY
			cell.setBackground(Colors.getColor("BFC1C0")); //$NON-NLS-1$
			cell.setText(null);
			return;
		}

		String text = String.valueOf(value);
		if (valueFormatter != null && value != null) {
			text = valueFormatter.format((T) value);
		}
		cell.setText(text);
	}

}
