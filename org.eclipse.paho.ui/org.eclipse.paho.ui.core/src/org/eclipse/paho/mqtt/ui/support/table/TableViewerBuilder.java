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
import java.util.Collections;
import java.util.Map;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.paho.mqtt.ui.util.Beans;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * TableViewer builder
 * 
 * @author Bin Zhang
 */
public final class TableViewerBuilder {
	private final TableViewer viewer;
	private final boolean checkable;
	private Map<String, PropertyDescriptor> mappings;

	/**
	 * Creates a new TableViewerBuilder
	 * 
	 * @param parent
	 * @param style
	 */
	public TableViewerBuilder(Composite parent, int style) {
		// check parent
		if (parent.getChildren().length > 0) {
			throw new IllegalArgumentException(
					"The parent composite for the table needs to be empty for TableColumnLayout."); //$NON-NLS-1$
		}

		mappings = Collections.emptyMap();
		checkable = (style & SWT.CHECK) == SWT.CHECK;

		if (checkable) {
			viewer = CheckboxTableViewer.newCheckList(parent, style);
		}
		else {
			viewer = new TableViewer(parent, style);
		}

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setUseHashlookup(true);

		// enable tooltip support
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);

		Table table = viewer.getTable();

		// set TableColumnLayout to table parent
		// Table parent layout needs to be a TableColumnLayout
		table.getParent().setLayout(new TableColumnLayout());

		// headers / lines visible by default
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	/**
	 * Creates a new TableViewerBuilder with default SWT styles.
	 */
	public TableViewerBuilder(Composite parent) {
		this(parent, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
	}

	/**
	 * Default activationStrategy and ColumnViewerEditor feature
	 */
	public TableViewerBuilder makeEditable() {
		return makeEditable(null, -1);
	}

	/**
	 * @param activationStrategy
	 * @param feature
	 */
	public TableViewerBuilder makeEditable(ColumnViewerEditorActivationStrategy activationStrategy, int feature) {
		ColumnViewerEditorActivationStrategy defaultActivationStrategy = new ColumnViewerEditorActivationStrategy(
				viewer) {
			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		TableViewerEditor.create(viewer, activationStrategy == null ? defaultActivationStrategy : activationStrategy,
				feature < 0 ? ColumnViewerEditor.DEFAULT : feature);
		return this;
	}

	/**
	 * @param listener
	 */
	public TableViewerBuilder selectionChangedListener(ISelectionChangedListener listener) {
		this.viewer.addSelectionChangedListener(listener);
		return this;
	}

	/**
	 * @param listener
	 */
	public TableViewerBuilder doubleClickListener(IDoubleClickListener listener) {
		this.viewer.addDoubleClickListener(listener);
		return this;
	}

	/**
	 * @param listener
	 */
	public TableViewerBuilder checkStateListener(ICheckStateListener listener) {
		if (!checkable) {
			throw new IllegalStateException("The table viewer is not a CheckboxTableViewer!"); //$NON-NLS-1$
		}

		((CheckboxTableViewer) viewer).addCheckStateListener(listener);
		return this;
	}

	/**
	 * @param modelClass
	 */
	public TableViewerBuilder modelClass(Class<?> modelClass) {
		this.mappings = Beans.introspect(modelClass);
		return this;
	}

	/**
	 * @param modelClass
	 */
	public TableViewerBuilder contentProvider(IContentProvider contentProvider) {
		viewer.setContentProvider(contentProvider);
		return this;
	}

	/**
	 * Sets the given collection as input object
	 */
	public TableViewerBuilder input(Object input) {
		viewer.setInput(input);
		return this;
	}

	/**
	 * Returns the JFace TableViewer.
	 */
	public TableViewer build() {
		return viewer;
	}

	/**
	 * Returns the JFace CheckboxTableViewer.
	 */
	public CheckboxTableViewer buildCheckable() {
		if (!checkable) {
			throw new IllegalStateException("The table viewer is not a CheckboxTableViewer!"); //$NON-NLS-1$
		}

		return (CheckboxTableViewer) viewer;
	}

	/**
	 * Creates a new ColumnBuilder that can be used to configure the table column. When you have finished configuring
	 * the column, call build() on the ColumnBuilder to create the actual column.
	 */
	public TableViewerColumnBuilder columnBuilder(String headerText, int style) {
		return new TableViewerColumnBuilder(this, headerText, style);
	}

	TableViewer getTableViewer() {
		return viewer;
	}

	PropertyDescriptor getPropertyDescriptor(String propertyName) {
		return mappings.get(propertyName);
	}

	TableColumnLayout getTableLayout() {
		return (TableColumnLayout) viewer.getTable().getParent().getLayout();
	}

}
