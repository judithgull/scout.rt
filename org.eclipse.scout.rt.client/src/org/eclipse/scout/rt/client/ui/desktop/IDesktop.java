/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.desktop;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.beans.IPropertyObserver;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.DataChangeListener;
import org.eclipse.scout.rt.client.ui.action.ActionFinder;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.AbstractMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.tool.IToolButton;
import org.eclipse.scout.rt.client.ui.action.view.IViewButton;
import org.eclipse.scout.rt.client.ui.basic.filechooser.IFileChooser;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutlineTableForm;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.AbstractPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.client.ui.form.PrintDevice;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.messagebox.IMessageBox;
import org.eclipse.scout.rt.shared.services.common.bookmark.AbstractPageState;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;

/**
 * The desktop model (may) consist of
 * <ul>
 * <li>set of available outline
 * <li>active outline
 * <li>active tableview
 * <li>active detail form
 * <li>active search form
 * <li>form stack (swing: dialogs on desktop as JInternalFrames; eclipse: editors or views)
 * <li>dialog stack of model and non-modal dialogs (swing: dialogs as JDialog, JFrame; eclipse: dialogs in a new Shell)
 * <li>active message box stack
 * <li>top-level menus (menu tree)
 * </ul>
 */
public interface IDesktop extends IPropertyObserver {
  /**
   * String
   */
  String PROP_TITLE = "title";
  /**
   * {@link IProcessingStatus}
   */
  String PROP_STATUS = "status";
  /**
   * {@link List<IKeyStroke>}
   */
  String PROP_KEY_STROKES = "keyStrokes";
  /**
   * {@link Boolean}
   */
  String PROP_OPENED = "opened";
  /**
   * {@link Boolean}
   */
  String PROP_GUI_AVAILABLE = "guiAvailable";

  void initDesktop() throws ProcessingException;

  <T extends IForm> T findForm(Class<T> formType);

  <T extends IForm> List<T> findForms(Class<T> formType);

  <T extends IForm> T findLastActiveForm(Class<T> formType);

  /**
   * @return an available outline of this type ({@link #getAvailableOutlines()}
   */
  <T extends IOutline> T findOutline(Class<T> outlineType);

  /**
   * Find a toolButton or a viewButton in the desktop
   */
  <T extends IAction> T findAction(Class<T> actionType);

  /**
   * Convenience alias for {@link #findAction(Class)}
   */
  <T extends IToolButton> T findToolButton(Class<T> toolButtonType);

  /**
   * Convenience alias for {@link #findAction(Class)}
   */
  <T extends IViewButton> T findViewButton(Class<T> viewButtonType);

  boolean isTrayVisible();

  void setTrayVisible(boolean b);

  /**
   * @param form
   * @return all forms except the searchform and the current detail form with
   *         the same fully qualified classname and the same primary key.
   */
  List<IForm> getSimilarViewForms(IForm form);

  /**
   * @return the {@link IFormField} that owns the focus
   */
  IFormField getFocusOwner();

  /**
   * fires a ensure visible event for every form in viewStack
   */
  void ensureViewStackVisible();

  /**
   * fires a ensure visible event
   *
   * @param form
   */
  void ensureVisible(IForm form);

  /**
   * DISPLAY_HINT_VIEW
   */
  List<IForm> getViewStack();

  /**
   * DISPLAY_HINT_DIALOG
   */
  List<IForm> getDialogStack();

  /**
   * Open dialogs or views that need to be saved
   */
  List<IForm> getUnsavedForms();

  /**
   * add form to desktop and notify attached listeners (incl. gui)
   */
  void addForm(IForm form);

  /**
   * remove form from desktop and notify attached listeners (incl. gui)
   */
  void removeForm(IForm form);

  List<IMessageBox> getMessageBoxStack();

  void addMessageBox(IMessageBox mb);

  List<IOutline> getAvailableOutlines();

  void setAvailableOutlines(List<? extends IOutline> availableOutlines);

  Set<IKeyStroke> getKeyStrokes();

  void setKeyStrokes(Collection<? extends IKeyStroke> ks);

  void addKeyStrokes(IKeyStroke... keyStrokes);

  void removeKeyStrokes(IKeyStroke... keyStrokes);

  /**
   * @return true if the form is currently attached to the desktop, false if the
   *         form is not attached to the desktop<br>
   *         This method can be used to determine if a possibly active form
   *         (started with a running form handler) is currently showing on the
   *         desktop.
   */
  boolean isShowing(IForm form);

  /**
   * @return true after desktop was opened and setup in any UI.
   */
  boolean isOpened();

  /**
   * @return the currently active outline on the desktop
   */
  IOutline getOutline();

  /**
   * set the currently active outline on the desktop
   */
  void setOutline(IOutline outline);

  /**
   * set the currently active outline on the desktop using its type
   */
  void setOutline(Class<? extends IOutline> outlineType);

  /**
   * Call this method to refresh all existing pages in all outlines<br>
   * If currently active page(s) are affected they reload their data, otherwise
   * the pages is simply marked dirty and reloaded on next activation
   */
  void refreshPages(List<Class<? extends IPage>> pages);

  /**
   * @see IDesktop#refreshPages(List)
   * @param pageTypes
   *          Must be classes that implement {@link IPage}.
   */
  void refreshPages(Class<?>... pageTypes);

  /**
   * add Property Observer
   */
  @Override
  void addPropertyChangeListener(PropertyChangeListener listener);

  @Override
  void removePropertyChangeListener(PropertyChangeListener listener);

  /**
   * add Property Observer
   */
  @Override
  void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

  @Override
  void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

  /**
   * add Model Observer
   */
  void addDesktopListener(DesktopListener l);

  void removeDesktopListener(DesktopListener l);

  /**
   * add Data Change Observer
   */
  void addDataChangeListener(DataChangeListener listener, Object... dataTypes);

  void removeDataChangeListener(DataChangeListener listener, Object... dataTypes);

  /**
   * Call this method to refresh all listeners on that dataTypes.<br>
   * These might include pages, forms, fields etc.<br>
   *
   * @see {@link AbstractForm#execDataChanged(Object...)} {@link AbstractForm#execDataChanged(Object...)}
   *      {@link AbstractFormField#execDataChanged(Object...)} {@link AbstractFormField#execDataChanged(Object...)}
   *      {@link AbstractPage#execDataChanged(Object...)} {@link AbstractPage#execDataChanged(Object...)}
   */
  void dataChanged(Object... dataTypes);

  /**
   * marks desktop data as changing and all data changed events are cached until the change is done
   * <p>
   * when done, all cached events are coalesced and sent as a batch
   */
  void setDataChanging(boolean b);

  boolean isDataChanging();

  /**
   * Called after a page was loaded or reloaded.
   * <p>
   * Default minimizes page search form when data was found.
   *
   * @param page
   */
  void afterTablePageLoaded(IPageWithTable<?> page) throws ProcessingException;

  /**
   * Unload and release unused pages in all outlines, such as closed and
   * non-selected nodes
   */
  void releaseUnusedPages();

  /**
   * @return the top level menus
   *         <p>
   *         normally presented in the menubar
   */
  List<IMenu> getMenus();

  /**
   * Convenience to find a menu in the desktop, uses {@link ActionFinder}
   */
  <T extends IMenu> T getMenu(Class<? extends T> searchType);

  /**
   * Prepare all (menubar) menus on the desktop.<br>
   * Calls {@link AbstractMenu#execPrepareAction()} on every menu in the menu
   * tree recursively
   */
  void prepareAllMenus();

  /**
   * @return all actions including keyStroke, menu, toolButton and viewButton
   */
  List<IAction> getActions();

  /**
   * Convenience to find a toolbutton in the desktop, uses {@link ActionFinder}
   */
  <T extends IToolButton> T getToolButton(Class<? extends T> searchType);

  /**
   * @return all {@link IToolButton} actions
   */
  List<IToolButton> getToolButtons();

  /**
   * Convenience to find a menu in the desktop, uses {@link ActionFinder}
   */
  <T extends IViewButton> T getViewButton(Class<? extends T> searchType);

  /**
   * @return all {@link IViewButton} actions
   */
  List<IViewButton> getViewButtons();

  /**
   * @return the detail form of the active (selected) page {@link IPage#getDetailForm()} of the active outline
   *         {@link IOutline#getDetailForm()}
   */
  IForm getPageDetailForm();

  /**
   * see {@link #getPageDetailForm()}, {@link AbstractDesktop#execChangedPageDetailForm(IForm)}
   */
  void setPageDetailForm(IForm f);

  /**
   * @return the detail table of the active (selected) page {@link IPage#getDetailTable()} of the active outline
   *         {@link IOutline#getDetailTable()}
   */
  ITable getPageDetailTable();

  /**
   * see {@link #getPageDetailTable()}, {@link AbstractDesktop#execChangedPageDetailTable(IForm)}
   */
  void setPageDetailTable(ITable t);

  /**
   * @return the form that displays the table of the (selected) page {@link IPage#getTable()} of the active outline
   *         {@link IOutline#getDetailTable()}<br>
   * @see {@link #isOutlineTableFormVisible()}
   */
  IOutlineTableForm getOutlineTableForm();

  /**
   * set the detail table form of the active (selected) page {@link IPage#getTable()} of the active outline
   * {@link IOutline#getDetailTable()}
   *
   * @see {@link #setOutlineTableFormVisible(boolean)}
   */
  void setOutlineTableForm(IOutlineTableForm f);

  /**
   * @return true if the outline table form is visible
   */
  boolean isOutlineTableFormVisible();

  /**
   * set the detail table form of the active (selected) page {@link IPage#getTable()} of the active outline
   * {@link IOutline#getDetailTable()}
   */
  void setOutlineTableFormVisible(boolean b);

  /**
   * @return the search form of the active (selected) page {@link IPageWithTable#getSearchFormInternal()} of the active
   *         outline {@link IOutline#getSearchForm()}
   */
  IForm getPageSearchForm();

  /**
   * see {@link #getPageSearchForm()}, {@link AbstractDesktop#execChangedPageSearchForm(IForm)}
   */
  void setPageSearchForm(IForm f);

  String getTitle();

  void setTitle(String s);

  /**
   * @return true: automatically prefix a * on any text field's search value
   */
  boolean isAutoPrefixWildcardForTextSearch();

  void setAutoPrefixWildcardForTextSearch(boolean b);

  /**
   * get the status of the desktop
   * <p>
   * see also {@link IForm#getFormStatus(IProcessingStatus)}
   */
  IProcessingStatus getStatus();

  /**
   * set a status on the desktop
   * <p>
   * this is normally displayed in as a tray message box
   * <p>
   * see also {@link IForm#setFormStatus(IProcessingStatus)}
   */
  void setStatus(IProcessingStatus status);

  /**
   * set a status on the desktop
   * <p>
   * this is normally displayed in as a tray message box
   * <p>
   * see also {@link IForm#setFormStatusText(String)}
   */
  void setStatusText(String s);

  /**
   * Retrieve files via a user interface
   */
  void addFileChooser(IFileChooser fc);

  /**
   * Opens the link in the browser.
   */
  void openUrlInBrowser(String url);

  /**
   * Opens the link in the browser.
   *
   * @param target
   *          used to specify where the url should be opened. Only considered by the web ui.
   */
  void openUrlInBrowser(String url, IUrlTarget target);

  /**
   * Prints the desktop
   * <p>
   * The method returns immediately, the print is done int the background.
   * <p>
   * For details and parameter details see {@link PrintDevice}
   */
  void printDesktop(PrintDevice device, Map<String, Object> parameters);

  /**
   * Activates a {@link Bookmark} on this desktop.
   * <p />
   * First the specific {@link Bookmark#getOutlineClassName()} is evaluated and selected, afterwards every page from the
   * {@link Bookmark#getPath()} will be selected (respecting the {@link AbstractPageState}).
   * <p />
   * Finally the path will be expanded. Possible exceptions might occur if no outline is set in the {@link Bookmark} or
   * the outline is not available.
   *
   * @param forceReload
   *          parameter without any function
   * @deprecated use {@link #activateBookmark(Bookmark)} instead, see
   *             https://bugs.eclipse.org/bugs/show_bug.cgi?id=439867, parameter forceReload is without any
   *             function. Will be removed in the N-Release.
   */
  @Deprecated
  void activateBookmark(Bookmark bm, boolean forceReload) throws ProcessingException;

  /**
   * Activates a {@link Bookmark} on this desktop.
   * <p />
   * First the specific {@link Bookmark#getOutlineClassName()} is evaluated and selected, afterwards every page from the
   * {@link Bookmark#getPath()} will be selected (respecting the {@link AbstractPageState}).
   * <p />
   * Finally the path will be expanded. Possible exceptions might occur if no outline is set in the {@link Bookmark} or
   * the outline is not available.
   */
  void activateBookmark(Bookmark bm) throws ProcessingException;

  /**
   * Creates a bookmark of the active page
   */
  Bookmark createBookmark() throws ProcessingException;

  /**
   * Creates a bookmark of the given page
   *
   * @since 3.8.0
   */
  Bookmark createBookmark(IPage page) throws ProcessingException;

  /**
   * do not use this internal method.<br>
   * for closing scout see <code>ClientScoutSession.getSession().close()</code>
   */
  void closeInternal() throws ProcessingException;

  IDesktopUIFacade getUIFacade();

  boolean isGuiAvailable();

  void changeVisibilityAfterOfflineSwitch();

  /**
   * This method is used internally within the framework.
   * <p>
   * Called before the desktop gets into its closing state, i.e. the desktop just received a request to close itself.
   * This pre-hook of the closing process adds the possibility to execute some custom code and to abort the closing
   * process.
   * <p>
   * Subclasses can override this method.
   *
   * @return <code>true</code> to allow the desktop to proceed with closing. Otherwise <code>false</code> to veto the
   *         closing process.
   */
  boolean doBeforeClosingInternal();

  /**
   * Transfers the keyboard focus to the next possible location. The next location is defined as the default focus
   * traversal
   * as defined by the UI layer.<br>
   * This operation is not supported in the RAP UI!
   *
   * @since 4.0.0
   */
  void traverseFocusNext();

  /**
   * Transfers the keyboard focus to the previous location. The previous location is defined as the default backwards
   * focus
   * traversal as defined by the UI layer.<br>
   * This operation is not supported in the RAP UI!
   *
   * @since 4.0.0
   */
  void traverseFocusPrevious();

  /**
   * Gets the currently active (focused) {@link IForm}.
   *
   * @return The currently active {@link IForm} or <code>null</code> if no {@link IForm} is active.
   * @since 4.2.0
   */
  IForm getActiveForm();
}
