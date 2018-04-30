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

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.paho.mqtt.ui.core.DataBindings.Validators;
import org.eclipse.paho.mqtt.ui.core.model.ServerURI;

/**
 * Server URI editor for the HA table of options tab
 * 
 * @author Bin Zhang
 */
public class ServerUriCellEditor extends EditingSupport {
	private final CellEditor cellEditor;

	public ServerUriCellEditor(TableViewer viewer) {
		super(viewer);
		this.cellEditor = new TextCellEditor(viewer.getTable());
		final IValidator validator = Validators.decorate(Validators.serverUri, cellEditor.getControl());
		this.cellEditor.setValidator(new ICellEditorValidator() {
			@Override
			public String isValid(Object value) {
				IStatus status = validator.validate(value);
				return (status == null || status.isOK()) ? null : status.getMessage();
			}
		});
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
		return ((ServerURI) element).getValue();
	}

	@Override
	protected void setValue(Object element, Object value) {
		((ServerURI) element).setValue(String.valueOf(value));
		getViewer().update(element, null);
	}

}
