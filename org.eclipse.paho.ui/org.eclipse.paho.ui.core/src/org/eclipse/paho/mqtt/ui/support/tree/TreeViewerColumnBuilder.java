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
package org.eclipse.paho.mqtt.ui.support.tree;

import java.beans.PropertyDescriptor;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.paho.mqtt.ui.support.provider.IValueFormatter;
import org.eclipse.paho.mqtt.ui.support.provider.PropertyCellLabelProvider;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * 
 * @author Bin Zhang
 * 
 */
public final class TreeViewerColumnBuilder {
	private final TreeViewerBuilder builder;
	private final String headerText;
	private final int style;
	private boolean moveable;
	private boolean resizable;
	private String propertyName;
	private EditingSupport editingSupport;
	private CellLabelProvider cellLabelProvider;
	private IValueFormatter<?> valueFormatter;

	/**
	 * @param builder
	 * @param style
	 */
	TreeViewerColumnBuilder(TreeViewerBuilder builder, int style) {
		this(builder, null, style);
	}

	/**
	 * @param builder
	 * @param style
	 */
	TreeViewerColumnBuilder(TreeViewerBuilder builder, String headerText, int style) {
		this.builder = builder;
		this.style = style;
		this.headerText = headerText;
	}

	public TreeViewerColumnBuilder property(String propertyName) {
		this.propertyName = propertyName;
		return this;
	}

	/**
	 * Sets a formatter for this column that is responsible to convert the value into a String.
	 */
	public TreeViewerColumnBuilder format(IValueFormatter<?> valueFormatter) {
		this.valueFormatter = valueFormatter;
		return this;
	}

	/**
	 * If your column is not text based (for example a column with images that are owner-drawn), you can use a custom
	 * CellLabelProvider instead of a value and a value formatter.
	 */
	public TreeViewerColumnBuilder labelProvider(CellLabelProvider labelProvider) {
		this.cellLabelProvider = labelProvider;
		return this;
	}

	/**
	 * Display empty string for the label
	 */
	public TreeViewerColumnBuilder emptyLabelProvider() {
		this.cellLabelProvider = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return "";
			}
		};
		return this;
	}

	public TreeViewerColumnBuilder editingSupport(EditingSupport editingSupport) {
		this.editingSupport = editingSupport;
		return this;
	}

	public TreeViewerColumnBuilder moveable(boolean moveable) {
		this.moveable = moveable;
		return this;
	}

	public TreeViewerColumnBuilder resizable(boolean resizable) {
		this.resizable = resizable;
		return this;
	}

	/**
	 * Builds the column and returns the TreeViewerColumn
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TreeViewerColumn build() {
		// create column
		TreeViewerColumn viewerColumn = new TreeViewerColumn(builder.getViewer(), style);
		TreeColumn column = viewerColumn.getColumn();
		if (headerText != null) {
			column.setText(headerText);
		}
		column.setMoveable(moveable);
		column.setResizable(resizable);

		// set label provider
		if (cellLabelProvider != null) {
			viewerColumn.setLabelProvider(cellLabelProvider);
		}
		else {
			if (propertyName == null) {
				viewerColumn.setLabelProvider(new ColumnLabelProvider());
			}
			else {
				PropertyDescriptor descriptor = builder.getPropertyDescriptor(propertyName);
				viewerColumn.setLabelProvider(new PropertyCellLabelProvider(descriptor, valueFormatter));
			}
		}

		// set editing support
		if (editingSupport != null) {
			viewerColumn.setEditingSupport(editingSupport);
		}

		return viewerColumn;
	}

}
