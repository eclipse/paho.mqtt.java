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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.paho.mqtt.ui.Constants.ImageKeys;
import org.eclipse.paho.mqtt.ui.Paho;
import org.eclipse.paho.mqtt.ui.core.event.Event;
import org.eclipse.paho.mqtt.ui.core.event.Events;
import org.eclipse.paho.mqtt.ui.core.event.IEventHandler;
import org.eclipse.paho.mqtt.ui.core.event.IEventService;
import org.eclipse.paho.mqtt.ui.core.event.Selector;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.core.model.Pair;
import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.paho.mqtt.ui.support.tree.TreeViewerBuilder;
import org.eclipse.paho.mqtt.ui.util.Images;
import org.eclipse.paho.mqtt.ui.views.provider.NavTreeContentProvider;
import org.eclipse.paho.mqtt.ui.views.provider.NavTreeLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;

/**
 * Navigation Tree View for connections
 * 
 * @author Bin Zhang
 */
public class NavigationView extends ViewPart {
	public static final String ID = "org.eclipse.paho.ui.navigationView"; //$NON-NLS-1$

	// connection name suffix counter
	private static int index = 1;
	private TreeViewer viewer;
	private LinkedList<Connection> connections;
	// for multi-deletion
	private List<Connection> selectedConnections;
	private Map<String, IEditorPart> editors;
	//
	private IEventService eventService;

	/**
	 * Navigation tree view
	 */
	public NavigationView() {
		connections = new LinkedList<Connection>();
		editors = new HashMap<String, IEditorPart>();
	}

	@Override
	public void createPartControl(Composite parent) {
		setPartName(Messages.NAV_TREE_TITLE);

		// load the data from data store
		connections.addAll(Paho.loadConnections());
		Collections.sort(connections, new Comparator<Connection>() {
			@Override
			public int compare(Connection o1, Connection o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});

		// build tree
		TreeViewerBuilder builder = new TreeViewerBuilder(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.BORDER);
		builder.contentProvider(new NavTreeContentProvider()).labelProvider(new NavTreeLabelProvider())
				.selectionChangedListener(new ISelectionChangedListener() {
					@SuppressWarnings("unchecked")
					public void selectionChanged(final SelectionChangedEvent event) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								IStructuredSelection selection = (IStructuredSelection) event.getSelection();
								selectedConnections = selection.toList();

								// only open one connection each selection
								Connection connection = (Connection) selection.getFirstElement();
								if (connection != null) {
									IEditorPart editor = Paho.openConnectionEditor(connection);
									editors.put(connection.getId(), editor);
								}
							}
						});
					}
				}).makeEditable().input(connections);

		// build view
		viewer = builder.build();

		// build column
		final TreeViewerColumn column = builder.columnBuilder().labelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Connection) element).getName();
			}

			@Override
			public Image getImage(Object obj) {
				return Images.get(ImageKeys.IMG_CONNECTION);
			}

			@Override
			public String getToolTipText(Object element) {
				return Messages.TOOLTIP_DBCLICK_TO_EDIT;
			}

		}).editingSupport(new NavTreeEditor(viewer)).build();

		// control listener
		viewer.getControl().addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				column.getColumn().setWidth(((Tree) e.getSource()).getBounds().width - 10);
			}
		});

		// Add action
		final Action addAction = new Action() {
			@Override
			public String getText() {
				return Messages.LABEL_NEW_CONNECTION;
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				return Images.getDescriptor(ImageKeys.IMG_ADD);
			}

			@Override
			public void run() {
				// send new connection event to request connection creation on
				// nav tree
				Paho.getEventService().sendEvent(Events.of(Selector.ofNewConnection()));
			}
		};

		// Delete action
		final Action deleteAction = new Action() {
			@Override
			public String getText() {
				return Messages.LABEL_DELETE;
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				return Images.getDescriptor(ImageKeys.IMG_REMOVE);
			}

			@Override
			public void run() {
				// confirm deletion
				if (!MessageDialog.openConfirm(getViewSite().getShell(),Messages.NAV_TREE_DELETE_CONFIRM_TITLE,
						Messages.NAV_TREE_DELETE_CONFIRM_MESSAGE)) {
					return;
				}

				// delete connections
				for (Connection connection : selectedConnections) {
					doDelete(connection);
				}

				// make selection after deletion
				if (!connections.isEmpty()) {
					// should be previous or next of the removed?
					viewer.setSelection(new TreeSelection(new TreePath(
							new Object[] { connections.getLast() })), true);
				}

				// refresh view
				viewer.refresh();
			}

			/**
			 * @param connection
			 */
			private void doDelete(Connection connection) {
				// try disconnect it
				Paho.getConnectionManager().disconnect(connection);

				// remove from the view
				IEditorPart editor = editors.get(connection.getId());
				if (editor != null) {
					Paho.closeConnectionEditor(editor, true);
				}

				// remove from model
				connections.remove(connection);

				// perform persistence deletion
				Paho.deleteConnection(connection);
			}
		};

		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(addAction);

				ISelection selection = viewer.getSelection();
				if (!selection.isEmpty()) {
					if (selection instanceof IStructuredSelection) {
						Connection connection = (Connection) ((IStructuredSelection) selection)
								.getFirstElement();
						if (connection != null) {
							manager.add(deleteAction);
						}
					}
				}
			}
		});
		menuMgr.setRemoveAllWhenShown(true);
		viewer.getControl().setMenu(menu);

		// register event handlers
		eventService = Paho.getEventService();
		eventService.registerHandler(Selector.ofNewConnection(), new IEventHandler<Void>() {
			@Override
			public void handleEvent(Event<Void> e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						Connection connection = new Connection("connection" + index++); //$NON-NLS-1$
						connections.add(connection);
						viewer.refresh();

						// select and begin edit
						viewer.setSelection(new TreeSelection(new TreePath(new Object[] { connection })), true);
						viewer.editElement(connection, 0);

						// save connection
						Paho.saveConnection(connection);
					}
				});
			}
		});

		// auto save the connection
		final Runnable autoSaveJob = new Runnable() {
			@Override
			public void run() {
				List<Connection> copiedConnections = new ArrayList<Connection>();
				copiedConnections.addAll(connections);
				for (Connection connection : copiedConnections) {
					// save connection
					Paho.saveConnection(connection);
				}
			}
		};

		// save changes on data model when shutting down
		Runtime.getRuntime().addShutdownHook(new Thread(autoSaveJob));
	}

	/**
	 * Passing the focus request to the listViewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	// ********************************************************
	// Nav tree editor
	// ********************************************************
	class NavTreeEditor extends EditingSupport {
		private final CellEditor cellEditor;

		public NavTreeEditor(TreeViewer viewer) {
			super(viewer);
			cellEditor = new TreeTextCellEditor(viewer.getTree());
		}

		@Override
		protected void setValue(Object element, Object value) {
			Connection connection = (Connection) element;
			String oldName = connection.getName();
			String newName = ((String) value).trim();
			if (oldName.equals(newName)) {
				return;
			}

			// update it
			connection.setName(newName);
			viewer.update(element, null);

			// publish event to notify the editor to update the editor title
			eventService.sendEvent(Events.of(Selector.ofRenameConnection(connection), Pair.of(oldName, newName)));
		}

		@Override
		protected Object getValue(Object element) {
			return ((Connection) element).getName();
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}

	// Text cell editor
	class TreeTextCellEditor extends TextCellEditor {
		private int minHeight = 0;

		public TreeTextCellEditor(Tree tree) {
			super(tree, SWT.BORDER);
			Text txt = (Text) getControl();
			Font fnt = txt.getFont();
			FontData[] fontData = fnt.getFontData();
			if (fontData != null && fontData.length > 0) {
				minHeight = fontData[0].getHeight() + 10;
			}
		}

		public LayoutData getLayoutData() {
			LayoutData data = super.getLayoutData();
			if (minHeight > 0) {
				data.minimumHeight = minHeight;
			}
			return data;
		}
	}

}