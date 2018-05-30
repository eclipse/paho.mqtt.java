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
import java.util.Collections;
import java.util.Map;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.paho.mqtt.ui.util.Beans;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;

/**
 * TreeViewer builder
 * 
 * @author Bin Zhang
 */
public final class TreeViewerBuilder {
	private final TreeViewer viewer;
	private final boolean checkable;
	private Map<String, PropertyDescriptor> mappings;

	/**
	 * Creates a new TreeViewerBuilder.
	 */
	public TreeViewerBuilder(Composite parent, int style) {
		mappings = Collections.emptyMap();
		checkable = (style & SWT.CHECK) == SWT.CHECK;

		if (checkable) {
			viewer = new CheckboxTreeViewer(parent, style);
		}
		else {
			viewer = new TreeViewer(parent, style);
		}

		// enable tooltip support
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
	}

	/**
	 * Creates a new TreeViewerBuilder with default SWT styles.
	 */
	public TreeViewerBuilder(Composite parent) {
		this(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
	}

	/**
	 * Default activationStrategy and ColumnViewerEditor feature
	 */
	public TreeViewerBuilder makeEditable() {
		return makeEditable(null, -1);
	}

	/**
	 * @param activationStrategy
	 * @param feature
	 */
	public TreeViewerBuilder makeEditable(ColumnViewerEditorActivationStrategy activationStrategy, int feature) {
		ColumnViewerEditorActivationStrategy defaultActivationStrategy = new ColumnViewerEditorActivationStrategy(
				viewer) {
			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		TreeViewerEditor.create(viewer, activationStrategy == null ? defaultActivationStrategy : activationStrategy,
				feature < 0 ? ColumnViewerEditor.DEFAULT : feature);
		return this;
	}

	/**
	 * @param listener
	 */
	public TreeViewerBuilder controlListener(ControlListener listener) {
		viewer.getControl().addControlListener(listener);
		return this;
	}

	/**
	 * @param listener
	 */
	public TreeViewerBuilder selectionChangedListener(ISelectionChangedListener listener) {
		viewer.addSelectionChangedListener(listener);
		return this;
	}

	/**
	 * @param listener
	 */
	public TreeViewerBuilder doubleClickListener(IDoubleClickListener listener) {
		viewer.addDoubleClickListener(listener);
		return this;
	}

	/**
	 * @param listener
	 */
	public TreeViewerBuilder checkStateListener(ICheckStateListener listener) {
		if (!checkable) {
			throw new IllegalStateException("The tree viewer is not a CheckboxTreeViewer!"); //$NON-NLS-1$
		}

		((CheckboxTreeViewer) viewer).addCheckStateListener(listener);
		return this;
	}

	/**
	 * @param modelClass
	 */
	public TreeViewerBuilder modelClass(Class<?> modelClass) {
		mappings = Beans.introspect(modelClass);
		return this;
	}

	/**
	 * @param modelClass
	 */
	public TreeViewerBuilder contentProvider(IContentProvider contentProvider) {
		viewer.setContentProvider(contentProvider);
		return this;
	}

	/**
	 * @param modelClass
	 */
	public TreeViewerBuilder labelProvider(IBaseLabelProvider labelProvider) {
		viewer.setLabelProvider(labelProvider);
		return this;
	}

	/**
	 * Sets the given collection as input object
	 */
	public TreeViewerBuilder input(Object input) {
		viewer.setInput(input);
		return this;
	}

	/**
	 * Returns the JFace viewer.
	 */
	public TreeViewer build() {
		return viewer;
	}

	/**
	 * Returns the JFace CheckboxTreeViewer.
	 */
	public CheckboxTreeViewer buildCheckable() {
		if (!checkable) {
			throw new IllegalStateException("The tree viewer is not a CheckboxTreeViewer!"); //$NON-NLS-1$
		}

		return (CheckboxTreeViewer) viewer;
	}

	/**
	 * Creates a new column builder that can be used to configure the column. When you have finished configuring the
	 * column, call build() on the column builder to create the actual column.
	 */
	public TreeViewerColumnBuilder columnBuilder(String headerText, int style) {
		return new TreeViewerColumnBuilder(this, headerText, style);
	}

	public TreeViewerColumnBuilder columnBuilder(int style) {
		return columnBuilder(null, style);
	}

	public TreeViewerColumnBuilder columnBuilder() {
		return columnBuilder(SWT.NONE);
	}

	TreeViewer getViewer() {
		return viewer;
	}

	PropertyDescriptor getPropertyDescriptor(String propertyName) {
		return mappings.get(propertyName);
	}

}
