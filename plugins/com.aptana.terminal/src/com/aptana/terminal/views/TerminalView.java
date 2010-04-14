/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.terminal.views;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tm.internal.terminal.control.ITerminalListener;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionClearAll;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionCopy;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionCut;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionPaste;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionSelectAll;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalConnectorExtension;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.internal.keys.WorkbenchKeyboard.KeyDownFilter;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.ViewPart;

import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.theme.IThemeManager;
import com.aptana.terminal.Activator;
import com.aptana.terminal.Closeable;
import com.aptana.terminal.Utils;
import com.aptana.terminal.connector.LocalTerminalConnector;
import com.aptana.terminal.editor.TerminalEditor;
import com.aptana.terminal.internal.IProcessListener;
import com.aptana.terminal.internal.emulator.VT100TerminalControl;
import com.aptana.terminal.preferences.IPreferenceConstants;

/**
 * @author Max Stepanov
 *
 */
@SuppressWarnings("restriction")
public class TerminalView extends ViewPart implements Closeable, ITerminalListener, IProcessListener, IPreferenceChangeListener {

	public static final String ID = "com.aptana.terminal.views.terminal"; //$NON-NLS-1$

	private static final String PROP_TITLE = "title"; //$NON-NLS-1$
	private static final String PROP_WORKING_DIRECTORY = "workingDirectory"; //$NON-NLS-1$

	private ITerminalViewControl fCtlTerminal;
	private IMemento savedState = null;
	
	private Action fOpenEditorAction;
	private TerminalActionCopy fActionEditCopy;
	private TerminalActionCut fActionEditCut;
	private TerminalActionPaste fActionEditPaste;
	private TerminalActionClearAll fActionEditClearAll;
	private TerminalActionSelectAll fActionEditSelectAll;

	/**
	 * @param id
	 *            The secondary id of the view. Used to uniquely identify and address a specific instance of this view.
	 * @param title
	 *            the title used in the UI tab for the instance of the view.
	 * @param workingDirectory
	 *            The directory in which to set the view initially.
	 * @return
	 */
	public static TerminalView openView(String secondaryId, String title, IPath workingDirectory) {
		TerminalView view = null;
		secondaryId  = secondaryId != null ? secondaryId : Long.toHexString(System.currentTimeMillis());
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			view = (TerminalView) page.showView(TerminalView.ID, secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
			view.initialize(title, workingDirectory);
		} catch (PartInitException e) {
			Activator.logError("Terminal view creation failed.", e);
		}
		return view;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		savedState = memento;
		new InstanceScope().getNode(CommonEditorPlugin.PLUGIN_ID).addPreferenceChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		new InstanceScope().getNode(CommonEditorPlugin.PLUGIN_ID).removePreferenceChangeListener(this);
		fCtlTerminal.disposeTerminal();
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {		
		fCtlTerminal = new VT100TerminalControl(this, parent, getTerminalConnectors());
		fCtlTerminal.setConnector(fCtlTerminal.getConnectors()[0]);
		if (getViewSite().getSecondaryId() == null || savedState != null) {
			if (savedState != null) {
				loadState(savedState);
			}
			fCtlTerminal.connectTerminal();
			hookProcessListener();
		}
		makeActions();
		hookContextMenu();
		contributeToActionBars();
		
		fCtlTerminal.getControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				updateActions();
			}
		});
		fCtlTerminal.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.doit) {
					IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
					Event event = new Event();
					event.character = e.character;
					event.keyCode = e.keyCode;
					event.stateMask = e.stateMask;
					event.doit = e.doit;
					event.display = e.display;
					event.widget = e.widget;
					event.time = e.time;
					event.data = e.data;
					KeyDownFilter keyDownFilter = ((BindingService) bindingService).getKeyboard().getKeyDownFilter();
					boolean enabled = keyDownFilter.isEnabled();
					try {
						keyDownFilter.setEnabled(true);
						keyDownFilter.handleEvent(event);
					} finally {
						keyDownFilter.setEnabled(enabled);
					}
				}
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see com.aptana.terminal.Closeable#close()
	 */
	@Override
	public void close() {
		if (fCtlTerminal != null && !fCtlTerminal.isDisposed()) {
			fCtlTerminal.getControl().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					getSite().getPage().hideView((IViewPart) getSite().getPart());
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.terminal.internal.IProcessListener#processCompleted()
	 */
	@Override
	public void processCompleted() {
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		boolean closeViewOnExit = prefs.getBoolean(IPreferenceConstants.CLOSE_VIEW_ON_EXIT);
		if (closeViewOnExit) {
			close();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		IMemento child = memento.createChild(PROP_TITLE);
		child.putTextData(getPartName());
		child = memento.createChild(PROP_WORKING_DIRECTORY);
		IPath workingDirectory = getWorkingDirectory();
		if (workingDirectory != null) {
			child.putTextData(workingDirectory.toOSString());
		}
	}

	private void loadState(IMemento memento) {
		IMemento child = memento.getChild(PROP_TITLE);
		if (child != null) {
			setPartName(child.getTextData());
		}
		child = memento.getChild(PROP_WORKING_DIRECTORY);
		if (child != null) {
			String value = child.getTextData();
			if (value != null) {
				setWorkingDirectory(Path.fromOSString(value));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		fCtlTerminal.setFocus();
	}
	
	private ITerminalConnector[] getTerminalConnectors() {
		return new ITerminalConnector[] { TerminalConnectorExtension.makeTerminalConnector(LocalTerminalConnector.ID) };
	}

	@Override
	public void setState(TerminalState state) {
	}

	@Override
	public void setTerminalTitle(final String title) {
		Utils.runInDisplayThread(new Runnable() {
			@Override
			public void run() {
				setContentDescription(title);
			}
		});
	}
	
	protected void initialize(String title, IPath workingDirectory) {
		if (fCtlTerminal.isConnected()) {
			return;
		}
		setPartName(title);
		setWorkingDirectory(workingDirectory);
		fCtlTerminal.connectTerminal();
		hookProcessListener();
	}
	
	protected void setWorkingDirectory(IPath workingDirectory) {
		if (workingDirectory != null) {
			LocalTerminalConnector localTerminalConnector = (LocalTerminalConnector) fCtlTerminal.getTerminalConnector().getAdapter(LocalTerminalConnector.class);
			if (localTerminalConnector != null) {
				localTerminalConnector.setWorkingDirectory(workingDirectory);
			}		
		}
	}
	
	protected IPath getWorkingDirectory() {
		LocalTerminalConnector localTerminalConnector = (LocalTerminalConnector) fCtlTerminal.getTerminalConnector().getAdapter(LocalTerminalConnector.class);
		if (localTerminalConnector != null) {
			return localTerminalConnector.getWorkingDirectory();
		}
		return null;
	}

	protected void hookProcessListener() {
		LocalTerminalConnector localTerminalConnector = (LocalTerminalConnector) fCtlTerminal.getTerminalConnector().getAdapter(LocalTerminalConnector.class);
		if (localTerminalConnector != null) {
			localTerminalConnector.addProcessListener(this);
		}
	}

	public void sendInput(String text) {
		fCtlTerminal.pasteString(text);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent)
	 */
	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (IThemeManager.THEME_CHANGED.equals(event.getKey())) {
			if (fCtlTerminal != null && !fCtlTerminal.isDisposed()) {
				fCtlTerminal.getControl().redraw();
			}
		}
	}

	/**
	 * hookContextMenu
	 */
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		Control control = fCtlTerminal.getControl();
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);
	}
	
	private void fillContextMenu(IMenuManager menuMgr) {
		menuMgr.add(fActionEditCopy);
		menuMgr.add(fActionEditPaste);
		menuMgr.add(new Separator());
		menuMgr.add(fActionEditClearAll);
		menuMgr.add(fActionEditSelectAll);
		menuMgr.add(new Separator());
		menuMgr.add(fOpenEditorAction);

		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/**
	 * contributeToActionBars
	 */
	private void contributeToActionBars() {
		IActionBars actionBars = getViewSite().getActionBars();
		fillLocalPullDown(actionBars.getMenuManager());
		fillLocalToolBar(actionBars.getToolBarManager());
		
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fActionEditCopy);
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), fActionEditPaste);
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fActionEditSelectAll);

	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(fOpenEditorAction);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(fOpenEditorAction);
		manager.add(new Separator());
		manager.add(fActionEditClearAll);
	}
	
	private void updateActions() {
		fActionEditCut.updateAction(true);
		fActionEditCopy.updateAction(true);
		fActionEditPaste.updateAction(true);
		fActionEditSelectAll.updateAction(true);
		fActionEditClearAll.updateAction(true);
	}

	/**
	 * makeActions
	 */
	private void makeActions() {
		fActionEditCopy = new TerminalActionCopy(fCtlTerminal);
		fActionEditCut = new TerminalActionCut(fCtlTerminal);
		fActionEditPaste = new TerminalActionPaste(fCtlTerminal);
		fActionEditClearAll = new TerminalActionClearAll(fCtlTerminal);
		fActionEditSelectAll = new TerminalActionSelectAll(fCtlTerminal);

		// open editor action
		fOpenEditorAction = new Action(Messages.TerminalView_Open_Terminal_Editor, Activator.getImageDescriptor("/icons/terminal.png")) { //$NON-NLS-1$
			@Override
			public void run() {
				Utils.openTerminalEditor(TerminalEditor.ID, true);
			}
		};
		fOpenEditorAction.setToolTipText(Messages.TerminalView_Create_Terminal_Editor_Tooltip);
	}

}
