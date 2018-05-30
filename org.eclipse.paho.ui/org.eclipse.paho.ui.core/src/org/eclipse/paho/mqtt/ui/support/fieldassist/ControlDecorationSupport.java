package org.eclipse.paho.mqtt.ui.support.fieldassist;

/*******************************************************************************
 * Copyright (c) 2009, 2013 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 268472)
 *     Matthew Hall - bug 300953
 *     Bin Zhang	- Fixed ControlDecoration dispose
 ******************************************************************************/

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IDecoratingObservable;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.IObserving;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.list.ListDiffVisitor;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservable;
import org.eclipse.jface.databinding.viewers.IViewerObservable;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

/**
 * Copied from eclipse SDK, and fixed ControlDecoration dispose which dosen't dispose the decorated image
 * 
 * Decorates the underlying controls of the target observables of a {@link ValidationStatusProvider} with
 * {@link ControlDecoration}s mirroring the current validation status. Only those target observables which implement
 * {@link ISWTObservable} or {@link IViewerObservable} are decorated.
 * 
 * @since 1.4
 */
public class ControlDecorationSupport {
	/**
	 * Creates a ControlDecorationSupport which observes the validation status of the specified
	 * {@link ValidationStatusProvider}, and displays a {@link ControlDecoration} over the underlying SWT control of all
	 * target observables that implement {@link ISWTObservable} or {@link IViewerObservable}.
	 * 
	 * @param validationStatusProvider the {@link ValidationStatusProvider} to monitor.
	 * @param position SWT alignment constant (e.g. SWT.LEFT | SWT.TOP) to use when constructing
	 * {@link ControlDecorationSupport}
	 * @return a ControlDecorationSupport which observes the validation status of the specified
	 * {@link ValidationStatusProvider}, and displays a {@link ControlDecoration} over the underlying SWT control of all
	 * target observables that implement {@link ISWTObservable} or {@link IViewerObservable}.
	 */
	public static ControlDecorationSupport create(ValidationStatusProvider validationStatusProvider, int position) {
		return create(validationStatusProvider, position, null, new ControlDecorationUpdater());
	}

	/**
	 * Creates a ControlDecorationSupport which observes the validation status of the specified
	 * {@link ValidationStatusProvider}, and displays a {@link ControlDecoration} over the underlying SWT control of all
	 * target observables that implement {@link ISWTObservable} or {@link IViewerObservable}.
	 * 
	 * @param validationStatusProvider the {@link ValidationStatusProvider} to monitor.
	 * @param position SWT alignment constant (e.g. SWT.LEFT | SWT.TOP) to use when constructing
	 * {@link ControlDecoration} instances.
	 * @param composite the composite to use when constructing {@link ControlDecoration} instances.
	 * @return a ControlDecorationSupport which observes the validation status of the specified
	 * {@link ValidationStatusProvider}, and displays a {@link ControlDecoration} over the underlying SWT control of all
	 * target observables that implement {@link ISWTObservable} or {@link IViewerObservable}.
	 */
	public static ControlDecorationSupport create(ValidationStatusProvider validationStatusProvider, int position,
			Composite composite) {
		return create(validationStatusProvider, position, composite, new ControlDecorationUpdater());
	}

	/**
	 * Creates a ControlDecorationSupport which observes the validation status of the specified
	 * {@link ValidationStatusProvider}, and displays a {@link ControlDecoration} over the underlying SWT control of all
	 * target observables that implement {@link ISWTObservable} or {@link IViewerObservable}.
	 * 
	 * @param validationStatusProvider the {@link ValidationStatusProvider} to monitor.
	 * @param position SWT alignment constant (e.g. SWT.LEFT | SWT.TOP) to use when constructing
	 * {@link ControlDecoration} instances.
	 * @param composite the composite to use when constructing {@link ControlDecoration} instances.
	 * @param updater custom strategy for updating the {@link ControlDecoration}(s) whenever the validation status
	 * changes.
	 * @return a ControlDecorationSupport which observes the validation status of the specified
	 * {@link ValidationStatusProvider}, and displays a {@link ControlDecoration} over the underlying SWT control of all
	 * target observables that implement {@link ISWTObservable} or {@link IViewerObservable}.
	 */
	public static ControlDecorationSupport create(ValidationStatusProvider validationStatusProvider, int position,
			Composite composite, ControlDecorationUpdater updater) {
		return new ControlDecorationSupport(validationStatusProvider, position, composite, updater);
	}

	private final int position;
	private final Composite composite;
	private final ControlDecorationUpdater updater;

	private IObservableValue validationStatus;
	private IObservableList targets;

	private IDisposeListener disposeListener = new IDisposeListener() {
		public void handleDispose(DisposeEvent staleEvent) {
			dispose();
		}
	};

	private IValueChangeListener statusChangeListener = new IValueChangeListener() {
		public void handleValueChange(ValueChangeEvent event) {
			statusChanged((IStatus) validationStatus.getValue());
		}
	};

	private IListChangeListener targetsChangeListener = new IListChangeListener() {
		public void handleListChange(ListChangeEvent event) {
			event.diff.accept(new ListDiffVisitor() {
				public void handleAdd(int index, Object element) {
					targetAdded((IObservable) element);
				}

				public void handleRemove(int index, Object element) {
					targetRemoved((IObservable) element);
				}
			});
			statusChanged((IStatus) validationStatus.getValue());
		}
	};

	private static class TargetDecoration {
		public final IObservable target;
		public final ControlDecoration decoration;

		TargetDecoration(IObservable target, ControlDecoration decoration) {
			this.target = target;
			this.decoration = decoration;
		}

		public void dispose() {
			decoration.hide();
			// XXX BUG: why dispose dosen't destroy the decorated icon? so we just fix it by hide it before dispose
			decoration.dispose();
		}
	}

	private List<TargetDecoration> targetDecorations;

	/**
	 * @param validationStatusProvider
	 * @param position
	 * @param composite
	 * @param updater
	 */
	@SuppressWarnings("rawtypes")
	private ControlDecorationSupport(ValidationStatusProvider validationStatusProvider, int position,
			Composite composite, ControlDecorationUpdater updater) {
		this.position = position;
		this.composite = composite;
		this.updater = updater;

		this.validationStatus = validationStatusProvider.getValidationStatus();
		Assert.isTrue(!this.validationStatus.isDisposed());

		this.targets = validationStatusProvider.getTargets();
		Assert.isTrue(!this.targets.isDisposed());

		this.targetDecorations = new ArrayList<TargetDecoration>();

		validationStatus.addDisposeListener(disposeListener);
		validationStatus.addValueChangeListener(statusChangeListener);

		targets.addDisposeListener(disposeListener);
		targets.addListChangeListener(targetsChangeListener);

		for (Iterator it = targets.iterator(); it.hasNext();)
			targetAdded((IObservable) it.next());

		statusChanged((IStatus) validationStatus.getValue());
	}

	private void targetAdded(IObservable target) {
		Control control = findControl(target);
		if (control != null)
			targetDecorations.add(new TargetDecoration(target, new ControlDecoration(control, position, composite)));
	}

	private void targetRemoved(IObservable target) {
		for (Iterator<TargetDecoration> it = targetDecorations.iterator(); it.hasNext();) {
			TargetDecoration targetDecoration = (TargetDecoration) it.next();
			if (targetDecoration.target == target) {
				targetDecoration.dispose();
				it.remove();
			}
		}
	}

	private Control findControl(IObservable target) {
		if (target instanceof ISWTObservable) {
			Widget widget = ((ISWTObservable) target).getWidget();
			if (widget instanceof Control)
				return (Control) widget;
		}

		if (target instanceof IViewerObservable) {
			Viewer viewer = ((IViewerObservable) target).getViewer();
			return viewer.getControl();
		}

		if (target instanceof IDecoratingObservable) {
			IObservable decorated = ((IDecoratingObservable) target).getDecorated();
			Control control = findControl(decorated);
			if (control != null)
				return control;
		}

		if (target instanceof IObserving) {
			Object observed = ((IObserving) target).getObserved();
			if (observed instanceof IObservable)
				return findControl((IObservable) observed);
		}

		return null;
	}

	private void statusChanged(IStatus status) {
		for (Iterator<TargetDecoration> it = targetDecorations.iterator(); it.hasNext();) {
			TargetDecoration targetDecoration = (TargetDecoration) it.next();
			ControlDecoration decoration = targetDecoration.decoration;
			updater.update(decoration, status);
		}
	}

	/**
	 * Hide Decorations
	 */
	public void hideDecorations() {
		for (Iterator<TargetDecoration> it = targetDecorations.iterator(); it.hasNext();) {
			TargetDecoration targetDecoration = (TargetDecoration) it.next();
			ControlDecoration decoration = targetDecoration.decoration;
			updater.update(decoration, null);
		}
	}

	/**
	 * Disposes this ControlDecorationSupport, including all control decorations managed by it. A
	 * ControlDecorationSupport is automatically disposed when its target ValidationStatusProvider is disposed.
	 */
	public void dispose() {
		if (validationStatus != null) {
			validationStatus.removeDisposeListener(disposeListener);
			validationStatus.removeValueChangeListener(statusChangeListener);
			validationStatus = null;
		}

		if (targets != null) {
			targets.removeDisposeListener(disposeListener);
			targets.removeListChangeListener(targetsChangeListener);
			targets = null;
		}

		disposeListener = null;
		statusChangeListener = null;
		targetsChangeListener = null;

		if (targetDecorations != null) {
			for (Iterator<TargetDecoration> it = targetDecorations.iterator(); it.hasNext();) {
				it.next().dispose();
			}
			targetDecorations.clear();
			targetDecorations = null;
		}
	}
}
