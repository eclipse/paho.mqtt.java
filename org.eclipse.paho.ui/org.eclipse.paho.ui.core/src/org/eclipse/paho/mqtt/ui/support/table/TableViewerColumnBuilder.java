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
package org.eclipse.paho.mqtt.ui.support.table;

import java.beans.PropertyDescriptor;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.paho.mqtt.ui.support.provider.IValueFormatter;
import org.eclipse.paho.mqtt.ui.support.provider.PropertyCellLabelProvider;
import org.eclipse.swt.widgets.TableColumn;

/**
 * 
 * @author Bin Zhang
 * 
 */
public final class TableViewerColumnBuilder {
	private final TableViewerBuilder builder;
	private final String headerText;
	private final int style;
	private Integer widthPixel;
	private Integer widthPercent;
	private boolean moveable;
	private boolean resizable;
	private String toolTipText;
	private String propertyName;
	private EditingSupport editingSupport;
	private CellLabelProvider cellLabelProvider;
	private IValueFormatter<?> valueFormatter;

	/**
	 * @param builder
	 * @param headerText
	 * @param style
	 */
	TableViewerColumnBuilder(TableViewerBuilder builder, String headerText, int style) {
		this.builder = builder;
		this.style = style;
		this.headerText = headerText;
	}

	public TableViewerColumnBuilder property(String propertyName) {
		this.propertyName = propertyName;
		return this;
	}

	/**
	 * Sets a formatter for this column that is responsible to convert the value into a String.
	 */
	public TableViewerColumnBuilder format(IValueFormatter<?> valueFormatter) {
		this.valueFormatter = valueFormatter;
		return this;
	}

	/**
	 * If your column is not text based (for example a column with images that are owner-drawn), you can use a custom
	 * CellLabelProvider instead of a value and a value formatter.
	 */
	public TableViewerColumnBuilder cellLabelProvider(CellLabelProvider labelProvider) {
		this.cellLabelProvider = labelProvider;
		return this;
	}

	/**
	 * Display empty string for the label
	 */
	public TableViewerColumnBuilder emptyCellLabelProvider() {
		this.cellLabelProvider = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return "";
			}
		};
		return this;
	}

	public TableViewerColumnBuilder editingSupport(EditingSupport editingSupport) {
		this.editingSupport = editingSupport;
		return this;
	}

	/**
	 * Sets column width in percent
	 */
	public TableViewerColumnBuilder percentWidth(int width) {
		this.widthPercent = width;
		return this;
	}

	/**
	 * Sets column width in pixel
	 */
	public TableViewerColumnBuilder pixelWidth(int width) {
		this.widthPixel = width;
		return this;
	}

	public TableViewerColumnBuilder moveable(boolean moveable) {
		this.moveable = moveable;
		return this;
	}

	public TableViewerColumnBuilder resizable(boolean resizable) {
		this.resizable = resizable;
		return this;
	}

	public TableViewerColumnBuilder toolTipText(String toolTipText) {
		this.toolTipText = toolTipText;
		return this;
	}

	/**
	 * Builds the column and returns the TableViewerColumn
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TableViewerColumn build() {
		// create column
		TableViewerColumn viewerColumn = new TableViewerColumn(builder.getTableViewer(), style);
		TableColumn column = viewerColumn.getColumn();
		column.setText(headerText);
		column.setMoveable(moveable);
		column.setResizable(resizable);
		column.setToolTipText(toolTipText);

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

		// set column layout data
		if (widthPixel != null && widthPercent != null) {
			throw new IllegalArgumentException("Cannot specify a width both in pixel and in percent!");
		}

		if (widthPercent == null) {
			// default width of 100px if nothing specified
			builder.getTableLayout().setColumnData(column, new ColumnPixelData(widthPixel == null ? 100 : widthPixel));
		}
		else {
			builder.getTableLayout().setColumnData(column, new ColumnWeightData(widthPercent));
		}

		// set editing support
		if (editingSupport != null) {
			viewerColumn.setEditingSupport(editingSupport);
		}

		return viewerColumn;
	}

}
