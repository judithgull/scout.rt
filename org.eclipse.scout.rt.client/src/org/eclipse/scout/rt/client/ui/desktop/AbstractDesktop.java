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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.commons.annotations.OrderedComparator;
import org.eclipse.scout.commons.beans.AbstractPropertyObserver;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.exception.ProcessingStatus;
import org.eclipse.scout.commons.exception.VetoException;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.holders.IHolder;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.extension.ui.action.tree.MoveActionNodesHandler;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopAddTrayMenusChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopBeforeClosingChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopClosingChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopGuiAttachedChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopGuiDetachedChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopInitChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopOpenedChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopOutlineChangedChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopPageDetailFormChangedChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopPageDetailTableChangedChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopPageSearchFormChangedChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.DesktopChains.DesktopTablePageLoadedChain;
import org.eclipse.scout.rt.client.services.common.bookmark.internal.BookmarkUtility;
import org.eclipse.scout.rt.client.ui.DataChangeListener;
import org.eclipse.scout.rt.client.ui.action.ActionFinder;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.keystroke.KeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.tool.IToolButton;
import org.eclipse.scout.rt.client.ui.action.view.IViewButton;
import org.eclipse.scout.rt.client.ui.basic.filechooser.IFileChooser;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeAdapter;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;
import org.eclipse.scout.rt.client.ui.desktop.navigation.INavigationHistoryService;
import org.eclipse.scout.rt.client.ui.desktop.outline.AbstractFormToolButton;
import org.eclipse.scout.rt.client.ui.desktop.outline.AbstractOutlineViewButton;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutlineTableForm;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.ISearchForm;
import org.eclipse.scout.rt.client.ui.form.FormEvent;
import org.eclipse.scout.rt.client.ui.form.FormListener;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.client.ui.form.PrintDevice;
import org.eclipse.scout.rt.client.ui.form.fields.GridData;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.button.IButton;
import org.eclipse.scout.rt.client.ui.messagebox.IMessageBox;
import org.eclipse.scout.rt.client.ui.messagebox.MessageBoxEvent;
import org.eclipse.scout.rt.client.ui.messagebox.MessageBoxListener;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.extension.AbstractExtension;
import org.eclipse.scout.rt.shared.extension.ContributionComposite;
import org.eclipse.scout.rt.shared.extension.ExtensionUtility;
import org.eclipse.scout.rt.shared.extension.IContributionOwner;
import org.eclipse.scout.rt.shared.extension.IExtensibleObject;
import org.eclipse.scout.rt.shared.extension.IExtension;
import org.eclipse.scout.rt.shared.extension.ObjectExtensions;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.common.shell.IShellService;
import org.eclipse.scout.rt.shared.ui.UserAgentUtility;
import org.eclipse.scout.service.SERVICES;

/**
 * The desktop model (may) consist of
 * <ul>
 * <li>set of available outlines
 * <li>active outline
 * <li>active table view
 * <li>active detail form
 * <li>active search form
 * <li>form stack (swing: dialogs on desktop as {@code JInternalFrame}s; SWT: editors or views)
 * <li>dialog stack of modal and non-modal dialogs (swing: dialogs as {@code JDialog}, {@code JFrame}; SWT: dialogs in a
 * new Shell)
 * <li>active message box stack
 * <li>menubar menus
 * <li>toolbar and viewbar actions
 * </ul>
 * The Eclipse Scout SDK creates a subclass of this class that can be used as
 * initial desktop.
 */
public abstract class AbstractDesktop extends AbstractPropertyObserver implements IDesktop, IContributionOwner, IExtensibleObject {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractDesktop.class);

  private final IDesktopExtension m_localDesktopExtension;
  private List<IDesktopExtension> m_desktopExtensions;
  private final EventListenerList m_listenerList;
  private int m_dataChanging;
  private final List<Object[]> m_dataChangeEventBuffer;
  private final Map<Object, EventListenerList> m_dataChangeListenerList;
  private final IDesktopUIFacade m_uiFacade;
  private List<IOutline> m_availableOutlines;
  private IOutline m_outline;
  private boolean m_outlineChanging = false;
  private P_ActiveOutlineListener m_activeOutlineListener;
  private final P_ActivatedFormListener m_activatedFormListener;
  private final List<WeakReference<IForm>> m_lastActiveFormList;
  private ITable m_pageDetailTable;
  private IOutlineTableForm m_outlineTableForm;
  private boolean m_outlineTableFormVisible;
  private IForm m_pageDetailForm;
  private IForm m_pageSearchForm;
  private final List<IForm> m_viewStack;
  private final List<IForm> m_dialogStack;
  private final List<IMessageBox> m_messageBoxStack;
  private List<IMenu> m_menus;
  private List<IViewButton> m_viewButtons;
  private List<IToolButton> m_toolButtons;
  private boolean m_autoPrefixWildcardForTextSearch;
  private boolean m_desktopInited;
  private boolean m_trayVisible;
  private boolean m_isForcedClosing = false;
  private IContributionOwner m_contributionHolder;
  private final ObjectExtensions<AbstractDesktop, org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> m_objectExtensions;

  // used to restore the outline state correctly in case a page unload is forced of changes in an other outline.
  private Map<IOutline, Bookmark> m_bookmarkPerOutline = new HashMap<IOutline, Bookmark>();

  /**
   * do not instantiate a new desktop<br>
   * get it via {@code ClientScoutSession.getSession().getModelManager()}
   */
  public AbstractDesktop() {
    this(true);
  }

  public AbstractDesktop(boolean callInitializer) {
    m_localDesktopExtension = new P_LocalDesktopExtension();
    m_listenerList = new EventListenerList();
    m_dataChangeListenerList = new HashMap<Object, EventListenerList>();
    m_dataChangeEventBuffer = new ArrayList<Object[]>();
    m_viewStack = new ArrayList<IForm>();
    m_dialogStack = new ArrayList<IForm>();
    m_messageBoxStack = new ArrayList<IMessageBox>();
    m_uiFacade = new P_UIFacade();
    m_outlineTableFormVisible = true;
    m_activatedFormListener = new P_ActivatedFormListener();
    m_lastActiveFormList = new LinkedList<WeakReference<IForm>>();
    m_objectExtensions = new ObjectExtensions<AbstractDesktop, org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>>(this);
    if (callInitializer) {
      callInitializer();
    }
  }

  protected final void callInitializer() {
    interceptInitConfig();
  }

  @Override
  public final List<Object> getAllContributions() {
    return m_contributionHolder.getAllContributions();
  }

  @Override
  public final <T> List<T> getContributionsByClass(Class<T> type) {
    return m_contributionHolder.getContributionsByClass(type);
  }

  @Override
  public final <T> T getContribution(Class<T> contribution) {
    return m_contributionHolder.getContribution(contribution);
  }

  /*
   * Configuration
   */
  /**
   * Configures the title of this desktop. The title is typically used as title for the main application
   * window.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   *
   * @return the title of this desktop
   */
  @ConfigProperty(ConfigProperty.TEXT)
  @Order(10)
  protected String getConfiguredTitle() {
    return null;
  }

  /**
   * Configures whether this Scout application should be represented within the OS system tray.
   * Representations in the system tray might differ for different operating systems or different UI.
   * A system tray may not be available at all.
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   *
   * @return {@code true} if this application should be visible in the system tray, {@code false} otherwise
   * @see #interceptAddTrayMenus(List)
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(15)
  protected boolean getConfiguredTrayVisible() {
    return false;
  }

  /**
   * Configures the outlines associated with this desktop. If multiple outlines are configured,
   * there is typically a need to provide some means of switching between different outlines,
   * such as a {@link AbstractOutlineViewButton}.
   * <p>
   * Note that {@linkplain IDesktopExtension desktop extensions} might contribute additional outlines to this desktop.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   *
   * @return an array of outline type tokens
   * @see IOutline
   */
  @ConfigProperty(ConfigProperty.OUTLINES)
  @Order(20)
  protected List<Class<? extends IOutline>> getConfiguredOutlines() {
    return null;
  }

  private List<Class<? extends IAction>> getConfiguredActions() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    List<Class<IAction>> fca = ConfigurationUtility.filterClasses(dca, IAction.class);
    return ConfigurationUtility.removeReplacedClasses(fca);
  }

  /**
   * Called while this desktop is initialized.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(10)
  protected void execInit() throws ProcessingException {
  }

  /**
   * Called after this desktop was opened and displayed on the GUI.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(12)
  protected void execOpened() throws ProcessingException {
  }

  /**
   * Called just after the core desktop receives the request to close the desktop.
   * <p>
   * Subclasses can override this method to execute some custom code before the desktop gets into its closing state. The
   * default behavior is to do nothing. By throwing an explicit {@link VetoException} the closing process will be
   * stopped.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(14)
  protected void execBeforeClosing() throws ProcessingException {
  }

  /**
   * Called before this desktop is being closed.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(15)
  protected void execClosing() throws ProcessingException {
  }

  /**
   * Called after a UI has been attached to this desktop. This desktop must not necessarily be open.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(20)
  protected void execGuiAttached() throws ProcessingException {
  }

  /**
   * Called after a UI has been detached from this desktop. This desktop must not necessarily be open.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(25)
  protected void execGuiDetached() throws ProcessingException {
  }

  /**
   * Called whenever a new outline has been activated on this desktop.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @param oldOutline
   *          old outline that was active before
   * @param newOutline
   *          new outline that is active after the change
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(30)
  protected void execOutlineChanged(IOutline oldOutline, IOutline newOutline) throws ProcessingException {
  }

  /**
   * Called whenever a new page has been activated (selected) on this desktop.
   * <p>
   * Subclasses can override this method.<br/>
   * This default implementation {@linkplain #removeForm(IForm) removes} the old form from this desktop and
   * {@linkplain #addForm(IForm) adds} the new form to this desktop.
   *
   * @param oldForm
   *          is the search form of the old (not selected anymore) page or {@code null}
   * @param newForm
   *          is the search form of the new (selected) page or {@code null}
   * @throws ProcessingException
   */
  @Order(40)
  @ConfigOperation
  protected void execPageSearchFormChanged(IForm oldForm, IForm newForm) throws ProcessingException {
    if (oldForm != null) {
      removeForm(oldForm);
    }
    if (newForm != null) {
      //ticket 89617: make new form height fixed, non-resizable
      GridData gd = newForm.getRootGroupBox().getGridData();
      if (gd.weightY <= 0) {
        gd.weightY = 0;
        newForm.getRootGroupBox().setGridDataInternal(gd);
      }
      addForm(newForm);
    }
  }

  /**
   * Called whenever a new page has been activated (selected) on this desktop.
   * <p>
   * Subclasses can override this method.<br/>
   * This default implementation {@linkplain #removeForm(IForm) removes} the old form from this desktop and
   * {@linkplain #addForm(IForm) adds} the new form to this desktop.
   *
   * @param oldForm
   *          is the detail form of the old (not selected anymore) page or {@code null}
   * @param newForm
   *          is the detail form of the new (selected) page or {@code null}
   * @throws ProcessingException
   */
  @Order(50)
  @ConfigOperation
  protected void execPageDetailFormChanged(IForm oldForm, IForm newForm) throws ProcessingException {
    if (oldForm != null) {
      removeForm(oldForm);
    }
    if (newForm != null) {
      addForm(newForm);
    }
  }

  /**
   * Called whenever a new page has been activated (selected) on this desktop.
   * <p>
   * Subclasses can override this method.<br/>
   * This default implementation keeps track of the current outline table form and updates it accordingly (including
   * visibility). See also {@link #getOutlineTableForm()}.
   *
   * @param oldTable
   *          is the table of the old (not selected anymore) table page or {@code null}
   * @param newTable
   *          is the table of the new (selected) table page or {@code null}
   * @throws ProcessingException
   */
  @Order(60)
  @ConfigOperation
  protected void execPageDetailTableChanged(ITable oldTable, ITable newTable) throws ProcessingException {
    if (m_outlineTableForm != null) {
      m_outlineTableForm.setCurrentTable(newTable);
    }
    setOutlineTableFormVisible(newTable != null);
  }

  /**
   * Called after a table page was loaded or reloaded.
   * <p>
   * Subclasses can override this method.<br/>
   * This default implementation minimizes the page search form when data has been found.
   *
   * @param tablePage
   *          the table page that has been (re)loaded
   * @throws ProcessingException
   */
  @Order(62)
  @ConfigOperation
  protected void execTablePageLoaded(IPageWithTable<?> tablePage) throws ProcessingException {
    ISearchForm searchForm = tablePage.getSearchFormInternal();
    if (searchForm != null) {
      searchForm.setMinimized(tablePage.getTable().getRowCount() > 0);
    }
  }

  /**
   * Called while the tray popup is being built. This method may call {@link #getMenu(Class)} to find an existing
   * menu on this desktop by class type.
   * <p>
   * The (potential) menus added to the {@code menus} list will be post processed. {@link IMenu#prepareAction()} is
   * called on each and then checked if the menu is visible.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @param menus
   *          a live list to add menus to the tray
   * @throws ProcessingException
   */
  @Order(70)
  @ConfigOperation
  protected void execAddTrayMenus(List<IMenu> menus) throws ProcessingException {
  }

  public List<IDesktopExtension> getDesktopExtensions() {
    return CollectionUtility.arrayList(m_desktopExtensions);
  }

  /**
   * @return the special extension that contributes the contents of this desktop itself
   */
  protected IDesktopExtension getLocalDesktopExtension() {
    return m_localDesktopExtension;
  }

  protected org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop> createLocalExtension() {
    return new LocalDesktopExtension<AbstractDesktop>(this);
  }

  @Override
  public final List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> getAllExtensions() {
    return m_objectExtensions.getAllExtensions();
  }

  @Override
  public <T extends IExtension<?>> T getExtension(Class<T> c) {
    return m_objectExtensions.getExtension(c);
  }

  protected final void interceptInitConfig() {
    m_objectExtensions.initConfig(createLocalExtension(), new Runnable() {
      @Override
      public void run() {
        initConfig();
      }
    });
  }

  protected void initConfig() {
    initDesktopExtensions();
    setTitle(getConfiguredTitle());
    setTrayVisible(getConfiguredTrayVisible());
    List<IDesktopExtension> extensions = getDesktopExtensions();
    m_contributionHolder = new ContributionComposite(this);

    //outlines
    OrderedCollection<IOutline> outlines = new OrderedCollection<IOutline>();
    for (IDesktopExtension ext : extensions) {
      try {
        ext.contributeOutlines(outlines);
      }
      catch (Throwable t) {
        LOG.error("contrinuting outlines by " + ext, t);
      }
    }
    List<IOutline> contributedOutlines = m_contributionHolder.getContributionsByClass(IOutline.class);
    outlines.addAllOrdered(contributedOutlines);

    // move outlines
    ExtensionUtility.moveModelObjects(outlines);
    m_availableOutlines = outlines.getOrderedList();

    //actions (keyStroke, menu, viewButton, toolButton)
    List<IAction> actionList = new ArrayList<IAction>();
    for (IDesktopExtension ext : extensions) {
      try {
        ext.contributeActions(actionList);
      }
      catch (Throwable t) {
        LOG.error("contrinuting actions by " + ext, t);
      }
    }

    List<IAction> contributedActions = m_contributionHolder.getContributionsByClass(IAction.class);
    actionList.addAll(contributedActions);
    //extract keystroke hints from menus
    for (IMenu menu : new ActionFinder().findActions(actionList, IMenu.class, true)) {
      if (menu.getKeyStroke() != null) {
        try {
          IKeyStroke ks = new KeyStroke(menu.getKeyStroke(), menu);
          ks.initAction();
          actionList.add(ks);
        }
        catch (Throwable t) {
          LOG.error(null, t);
        }
      }
    }
    //build completed menu, viewButton, toolButton lists
    // only top level menus
    OrderedCollection<IMenu> menus = new OrderedCollection<IMenu>();
    menus.addAllOrdered(new ActionFinder().findActions(actionList, IMenu.class, false));
    new MoveActionNodesHandler<IMenu>(menus).moveModelObjects();
    m_menus = menus.getOrderedList();

    OrderedComparator orderedComparator = new OrderedComparator();
    List<IViewButton> viewButtonList = new ActionFinder().findActions(actionList, IViewButton.class, false);
    ExtensionUtility.moveModelObjects(viewButtonList);
    Collections.sort(viewButtonList, orderedComparator);
    m_viewButtons = viewButtonList;

    List<IToolButton> toolButtonList = new ActionFinder().findActions(actionList, IToolButton.class, false);
    ExtensionUtility.moveModelObjects(toolButtonList);
    Collections.sort(toolButtonList, orderedComparator);
    m_toolButtons = toolButtonList;

    //add dynamic keyStrokes
    List<IKeyStroke> ksList = new ActionFinder().findActions(actionList, IKeyStroke.class, true);
    for (IKeyStroke ks : ksList) {
      try {
        ks.initAction();
      }
      catch (ProcessingException e) {
        LOG.error("could not initialize key stroke '" + ks + "'.", e);
      }
    }
    addKeyStrokes(ksList.toArray(new IKeyStroke[ksList.size()]));

    //init outlines
    for (IOutline o : m_availableOutlines) {
      try {
        o.initTree();
      }
      catch (Throwable t) {
        LOG.error(null, t);
      }
    }
  }

  protected final void interceptInit() throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends org.eclipse.scout.rt.client.ui.desktop.AbstractDesktop>> extensions = getAllExtensions();
    DesktopInitChain chain = new DesktopInitChain(extensions);
    chain.execInit();
  }

  private void initDesktopExtensions() {
    m_desktopExtensions = new LinkedList<IDesktopExtension>();
    m_desktopExtensions.add(getLocalDesktopExtension());
    injectDesktopExtensions(m_desktopExtensions);
  }

  /**
   * Override to provide a set of extensions (modules) that contribute their content to this desktop.
   * <p>
   * The default list contains only the {@link #getLocalDesktopExtension()}
   * </p>
   * <p>
   * The extension that are held by this desktop must call {@link IDesktopExtension#setCoreDesktop(this)} before using
   * the extension. That way the extension can use and access this desktop's methods.
   * </p>
   *
   * @param desktopExtensions
   *          a live list can be modified.
   */
  protected void injectDesktopExtensions(List<IDesktopExtension> desktopExtensions) {
  }

  @Override
  public void initDesktop() throws ProcessingException {
    if (!m_desktopInited) {
      m_desktopInited = true;
      //local
      prepareAllMenus();
      //extensions
      for (IDesktopExtension ext : getDesktopExtensions()) {
        try {
          ContributionCommand cc = ext.initDelegate();
          if (cc == ContributionCommand.Stop) {
            break;
          }
        }
        catch (Throwable t) {
          LOG.error("extension " + ext);
        }
      }
      // init actions
      ActionUtility.initActions(getMenus());
      ActionUtility.initActions(getToolButtons());
      ActionUtility.initActions(getViewButtons());
    }
  }

  @Override
  public boolean isTrayVisible() {
    return m_trayVisible;
  }

  @Override
  public void setTrayVisible(boolean b) {
    m_trayVisible = b;
  }

  @Override
  public boolean isShowing(IForm form) {
    if (form == null) {
      return false;
    }
    if (form.getOuterForm() != null) {
      return form.getOuterForm().isShowing();
    }

    for (IForm f : m_viewStack) {
      if (f == form) {
        return true;
      }
    }
    for (IForm f : m_dialogStack) {
      if (f == form) {
        return true;
      }
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IForm> T findForm(Class<T> formType) {
    ArrayList<IForm> list = new ArrayList<IForm>();
    list.addAll(m_viewStack);
    list.addAll(m_dialogStack);
    for (IForm f : list) {
      if (formType.isAssignableFrom(f.getClass())) {
        return (T) f;
      }
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IOutline> T findOutline(Class<T> outlineType) {
    for (IOutline o : getAvailableOutlines()) {
      if (outlineType.isAssignableFrom(o.getClass())) {
        return (T) o;
      }
    }
    return null;
  }

  @Override
  public <T extends IAction> T findAction(Class<T> actionType) {
    return new ActionFinder().findAction(getActions(), actionType);
  }

  @Override
  public <T extends IToolButton> T findToolButton(Class<T> toolButtonType) {
    return findAction(toolButtonType);
  }

  @Override
  public <T extends IViewButton> T findViewButton(Class<T> viewButtonType) {
    return findAction(viewButtonType);
  }

  @Override
  public IFormField getFocusOwner() {
    return fireFindFocusOwner();
  }

  @Override
  public IForm getActiveForm() {
    return fireFindActiveForm();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IForm> List<T> findForms(Class<T> formType) {
    List<T> resultList = new ArrayList<T>();
    if (formType != null) {
      List<IForm> list = new ArrayList<IForm>();
      list.addAll(m_viewStack);
      list.addAll(m_dialogStack);
      for (IForm f : list) {
        if (formType.isAssignableFrom(f.getClass())) {
          resultList.add((T) f);
        }
      }
    }
    return resultList;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IForm> T findLastActiveForm(Class<T> formType) {
    if (m_lastActiveFormList != null && formType != null) {
      for (WeakReference<IForm> formRef : m_lastActiveFormList) {
        if (formRef.get() != null && formType.isAssignableFrom(formRef.get().getClass())) {
          return (T) formRef.get();
        }
      }
    }
    return null;
  }

  @Override
  public <T extends IMenu> T getMenu(Class<? extends T> searchType) {
    // ActionFinder performs instance-of checks. Hence the menu replacement mapping is not required
    return new ActionFinder().findAction(getMenus(), searchType);
  }

  @Override
  public List<IForm> getViewStack() {
    return CollectionUtility.arrayList(m_viewStack);
  }

  @Override
  public List<IForm> getDialogStack() {
    return CollectionUtility.arrayList(m_dialogStack);
  }

  /**
   * returns all forms except the searchform and the current detail form with
   * the same fully qualified classname and an equal primary key different from
   * null.
   *
   * @param form
   * @return
   */
  @Override
  public List<IForm> getSimilarViewForms(IForm form) {
    List<IForm> forms = new ArrayList<IForm>(3);
    try {
      if (form != null && form.computeExclusiveKey() != null) {
        Object originalKey = form.computeExclusiveKey();
        for (IForm f : m_viewStack) {
          Object candidateKey = f.computeExclusiveKey();
          if (getPageDetailForm() == f || getPageSearchForm() == f) {
            continue;
          }
          else if (candidateKey == null || originalKey == null) {
            continue;
          }
          else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("form: " + candidateKey + " vs " + originalKey);
            }

            if (f.getClass().getName().equals(form.getClass().getName()) && originalKey.equals(candidateKey)) {
              forms.add(f);
            }
          }
        }
      }
    }
    catch (ProcessingException e) {
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
    return forms;
  }

  @Override
  public void ensureViewStackVisible() {
    if (CollectionUtility.isEmpty(m_viewStack)) {
      return;
    }
    for (IForm form : m_viewStack) {
      ensureVisible(form);
    }
  }

  @Override
  public void ensureVisible(IForm form) {
    if (form != null && (m_viewStack.contains(form) || m_dialogStack.contains(form))) {
      fireFormEnsureVisible(form);
    }
  }

  @Override
  public void addForm(IForm form) {
    //Allow DesktopExtensions to do any modifications on forms before UI is informed
    final IHolder<IForm> formHolder = new Holder<IForm>(IForm.class, form);
    for (IDesktopExtension ext : getDesktopExtensions()) {
      try {
        ContributionCommand cc = ext.customFormModificationDelegate(formHolder);
        if (cc == ContributionCommand.Stop) {
          break;
        }
      }
      catch (ProcessingException e) {
        formHolder.setValue(form);
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
      catch (Throwable t) {
        formHolder.setValue(form);
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Formmodification error: " + form, t));
      }
    }
    form = formHolder.getValue();
    if (form != null) {
      switch (form.getDisplayHint()) {
        case IForm.DISPLAY_HINT_POPUP_WINDOW:
        case IForm.DISPLAY_HINT_POPUP_DIALOG:
        case IForm.DISPLAY_HINT_DIALOG: {
          if (m_viewStack.remove(form)) {
            fireFormRemoved(form);
          }
          //remove all open popup windows
          if (form.getDisplayHint() == IForm.DISPLAY_HINT_POPUP_WINDOW) {
            for (IForm f : new ArrayList<IForm>(m_dialogStack)) {
              if (f.getDisplayHint() == IForm.DISPLAY_HINT_POPUP_WINDOW) {
                try {
                  f.doClose();
                }
                catch (Throwable t) {
                  LOG.error("Failed closing popup " + f, t);
                }
              }
            }
          }
          if (!m_dialogStack.contains(form)) {
            m_dialogStack.add(form);
            fireFormAdded(form);
          }
          break;
        }
        case IForm.DISPLAY_HINT_VIEW: {
          if (m_dialogStack.remove(form)) {
            fireFormRemoved(form);
          }
          if (!m_viewStack.contains(form)) {
            m_viewStack.add(form);
            fireFormAdded(form);
          }
          break;
        }
      }
      m_lastActiveFormList.add(new WeakReference<IForm>(form));
      form.addFormListener(m_activatedFormListener);
    }
  }

  @Override
  public void removeForm(IForm form) {
    if (form != null) {
      // remove form from last active form list
      if (m_lastActiveFormList != null) {
        for (Iterator<WeakReference<IForm>> it = m_lastActiveFormList.iterator(); it.hasNext();) {
          WeakReference<IForm> formRef = it.next();
          if (formRef.get() == null || form.equals(formRef.get())) {
            it.remove();
          }
        }
      }
      form.removeFormListener(m_activatedFormListener);
      boolean b1 = m_dialogStack.remove(form);
      boolean b2 = m_viewStack.remove(form);
      if (b1 || b2) {
        fireFormRemoved(form);
      }
    }
  }

  @Override
  public List<IMessageBox> getMessageBoxStack() {
    return CollectionUtility.arrayList(m_messageBoxStack);
  }

  @Override
  public void addMessageBox(final IMessageBox mb) {
    m_messageBoxStack.add(mb);
    mb.addMessageBoxListener(new MessageBoxListener() {
      @Override
      public void messageBoxChanged(MessageBoxEvent e) {
        switch (e.getType()) {
          case MessageBoxEvent.TYPE_CLOSED: {
            removeMessageBoxInternal(mb);
          }
        }
      }
    });
    fireMessageBoxAdded(mb);
  }

  private void removeMessageBoxInternal(IMessageBox mb) {
    m_messageBoxStack.remove(mb);
  }

  @Override
  public List<IOutline> getAvailableOutlines() {
    return CollectionUtility.arrayList(m_availableOutlines);
  }

  @Override
  public void setAvailableOutlines(List<? extends IOutline> availableOutlines) {
    setOutline((IOutline) null);
    m_availableOutlines = CollectionUtility.arrayList(availableOutlines);
  }

  @Override
  public IOutline getOutline() {
    return m_outline;
  }

  @Override
  public void setOutline(IOutline outline) {
    outline = resolveOutline(outline);
    if (m_outline == outline
        || m_outlineChanging) {
      return;
    }
    synchronized (this) {
      try {
        m_outlineChanging = true;
        if (m_outline != null) {
          IPage oldActivePage = m_outline.getActivePage();
          if (oldActivePage != null) {
            Bookmark bm = SERVICES.getService(INavigationHistoryService.class).addStep(0, oldActivePage);
            /*
             *  prevent the bookmark for the next visit of the outline. The bookmark is needed in case
             *  a page on the path to the currently selected page is getting invalidated due to changes
             *  in an other outline.
             */
            if (bm != null) {
              m_bookmarkPerOutline.put(m_outline, bm);
            }
          }
        }
        //
        IOutline oldOutline = m_outline;
        if (m_activeOutlineListener != null && oldOutline != null) {
          oldOutline.removeTreeListener(m_activeOutlineListener);
          oldOutline.removePropertyChangeListener(m_activeOutlineListener);
          m_activeOutlineListener = null;
        }
        // set new outline to set facts
        m_outline = outline;
        // deactivate old page
        if (oldOutline != null) {
          oldOutline.clearContextPage();
        }
        //
        if (m_outline != null) {
          m_activeOutlineListener = new P_ActiveOutlineListener();
          m_outline.addTreeListener(m_activeOutlineListener);
          m_outline.addPropertyChangeListener(m_activeOutlineListener);
        }
        // <bsh 2010-10-15>
        // Those three "setXyz(null)" statements used to be called unconditionally. Now, they
        // are only called when the new outline is null. When the new outline is _not_ null, we
        // will override the "null" anyway (see below).
        // This change is needed for the "on/off semantics" of the tool tab buttons to work correctly.
        if (m_outline == null) {
          setPageDetailForm(null);
          setPageDetailTable(null);
          setPageSearchForm(null, true);
        }
        // </bsh>
        fireOutlineChanged(oldOutline, m_outline);
        if (m_outline != null) {
          // reload selected page in case it is marked dirty
          if (m_outline.getActivePage() != null) {
            try {
              m_outline.getActivePage().ensureChildrenLoaded();
            }
            catch (ProcessingException e) {
              SERVICES.getService(IExceptionHandlerService.class).handleException(e);
            }
          }
          m_outline.setNodeExpanded(m_outline.getRootNode(), true);
          setPageDetailForm(m_outline.getDetailForm());
          setPageDetailTable(m_outline.getDetailTable());
          setPageSearchForm(m_outline.getSearchForm(), true);
          m_outline.makeActivePageToContextPage();
          IPage newActivePage = m_outline.getActivePage();
          if (newActivePage == null) {
            // try to reload state of last outline visit.
            Bookmark bookmark = m_bookmarkPerOutline.get(m_outline);
            if (bookmark != null && Boolean.TRUE) {
              try {
                activateBookmark(bookmark, false);
              }
              catch (ProcessingException e) {
                LOG.warn(String.format("Could not activate bookmark '%s' for restoring state of outline '%s'.", bookmark.getText(), m_outline), e);
              }
            }
            // default selection in outline
            else {
              // if there is no active page, set it now
              if (m_outline.isRootNodeVisible()) {
                m_outline.selectNode(m_outline.getRootNode(), false);
              }
              else {
                List<ITreeNode> children = m_outline.getRootNode().getChildNodes();
                if (CollectionUtility.hasElements(children)) {
                  for (ITreeNode node : children) {
                    if (node.isVisible() && node.isEnabled()) {
                      m_outline.selectNode(node, false);
                      break;
                    }
                  }
                }
              }
              newActivePage = m_outline.getActivePage();
            }
          }
          if (newActivePage != null) {
            SERVICES.getService(INavigationHistoryService.class).addStep(0, newActivePage);
          }
        }
      }
      finally {
        m_outlineChanging = false;
      }
    }
  }

  private IOutline resolveOutline(IOutline outline) {
    for (IOutline o : getAvailableOutlines()) {
      if (o == outline) {
        return o;
      }
    }
    return null;
  }

  @Override
  public void setOutline(Class<? extends IOutline> outlineType) {
    if (outlineType == null) {
      return;
    }
    for (IOutline o : getAvailableOutlines()) {
      if (outlineType.isInstance(o)) {
        setOutline(o);
        return;
      }
    }
  }

  @Override
  public Set<IKeyStroke> getKeyStrokes() {
    return CollectionUtility.hashSet(propertySupport.<IKeyStroke> getPropertySet(PROP_KEY_STROKES));
  }

  @Override
  public void setKeyStrokes(Collection<? extends IKeyStroke> ks) {
    propertySupport.setPropertySet(PROP_KEY_STROKES, CollectionUtility.<IKeyStroke> hashSetWithoutNullElements(ks));
  }

  @Override
  public void addKeyStrokes(IKeyStroke... keyStrokes) {
    if (keyStrokes != null && keyStrokes.length > 0) {
      Map<String, IKeyStroke> map = new HashMap<String, IKeyStroke>();
      for (IKeyStroke ks : getKeyStrokes()) {
        map.put(ks.getKeyStroke(), ks);
      }
      for (IKeyStroke ks : keyStrokes) {
        map.put(ks.getKeyStroke(), ks);
      }
      setKeyStrokes(map.values());
    }
  }

  @Override
  public void removeKeyStrokes(IKeyStroke... keyStrokes) {
    if (keyStrokes != null && keyStrokes.length > 0) {
      Map<String, IKeyStroke> map = new HashMap<String, IKeyStroke>();
      for (IKeyStroke ks : getKeyStrokes()) {
        map.put(ks.getKeyStroke(), ks);
      }
      for (IKeyStroke ks : keyStrokes) {
        map.remove(ks.getKeyStroke());
      }
      setKeyStrokes(map.values());
    }
  }

  @Override
  public List<IMenu> getMenus() {
    return CollectionUtility.arrayList(m_menus);
  }

  @Override
  public void prepareAllMenus() {
    for (IMenu child : getMenus()) {
      prepareMenuRec(child);
    }
  }

  @SuppressWarnings("deprecation")
  private void prepareMenuRec(IMenu menu) {
    menu.prepareAction();
    for (IMenu child : menu.getChildActions()) {
      prepareMenuRec(child);
    }
  }

  @Override
  public List<IAction> getActions() {
    List<IAction> result = new ArrayList<IAction>();
    result.addAll(getKeyStrokes());
    result.addAll(getMenus());
    result.addAll(getViewButtons());
    result.addAll(getToolButtons());
    return result;
  }

  @Override
  public <T extends IToolButton> T getToolButton(Class<? extends T> searchType) {
    // ActionFinder performs instance-of checks. Hence the toolbutton replacement mapping is not required
    return new ActionFinder().findAction(getMenus(), searchType);
  }

  @Override
  public List<IToolButton> getToolButtons() {
    return CollectionUtility.arrayList(m_toolButtons);
  }

  @Override
  public <T extends IViewButton> T getViewButton(Class<? extends T> searchType) {
    // ActionFinder performs instance-of checks. Hence the viewbutton replacement mapping is not required
    return new ActionFinder().findAction(getMenus(), searchType);
  }

  @Override
  public List<IViewButton> getViewButtons() {
    return CollectionUtility.arrayList(m_viewButtons);
  }

  @Override
  public IForm getPageDetailForm() {
    return m_pageDetailForm;
  }

  @Override
  public void setPageDetailForm(IForm f) {
    if (m_pageDetailForm != f) {
      IForm oldForm = m_pageDetailForm;
      m_pageDetailForm = f;
      //extensions
      for (IDesktopExtension ext : getDesktopExtensions()) {
        try {
          ContributionCommand cc = ext.pageDetailFormChangedDelegate(oldForm, m_pageDetailForm);
          if (cc == ContributionCommand.Stop) {
            break;
          }
        }
        catch (Throwable t) {
          LOG.error("extension " + ext, t);
        }
      }
    }
  }

  @Override
  public IForm getPageSearchForm() {
    return m_pageSearchForm;
  }

  @Override
  public void setPageSearchForm(IForm f) {
    setPageSearchForm(f, false);
  }

  public void setPageSearchForm(IForm f, boolean force) {
    if (force || m_pageSearchForm != f) {
      IForm oldForm = m_pageSearchForm;
      m_pageSearchForm = f;
      //extensions
      for (IDesktopExtension ext : getDesktopExtensions()) {
        try {
          ContributionCommand cc = ext.pageSearchFormChangedDelegate(oldForm, m_pageSearchForm);
          if (cc == ContributionCommand.Stop) {
            break;
          }
        }
        catch (Throwable t) {
          LOG.error("extension " + ext, t);
        }
      }
    }
  }

  @Override
  public IOutlineTableForm getOutlineTableForm() {
    return m_outlineTableForm;
  }

  @Override
  public void setOutlineTableForm(IOutlineTableForm f) {
    if (f != m_outlineTableForm) {
      if (m_outlineTableForm != null) {
        removeForm(m_outlineTableForm);
      }
      m_outlineTableForm = f;
      if (m_outlineTableForm != null) {
        m_outlineTableForm.setCurrentTable(getPageDetailTable());
        setOutlineTableFormVisible(getPageDetailTable() != null);
      }
      if (m_outlineTableForm != null && m_outlineTableFormVisible) {
        addForm(m_outlineTableForm);
      }
    }
  }

  @Override
  public boolean isOutlineTableFormVisible() {
    return m_outlineTableFormVisible;
  }

  @Override
  public void setOutlineTableFormVisible(boolean b) {
    if (m_outlineTableFormVisible != b) {
      m_outlineTableFormVisible = b;
      if (m_outlineTableForm != null) {
        if (m_outlineTableFormVisible) {
          addForm(m_outlineTableForm);
        }
        else {
          removeForm(m_outlineTableForm);
        }
      }
    }
  }

  @Override
  public ITable getPageDetailTable() {
    return m_pageDetailTable;
  }

  @Override
  public void setPageDetailTable(ITable t) {
    if (m_pageDetailTable != t) {
      ITable oldTable = m_pageDetailTable;
      m_pageDetailTable = t;
      //extensions
      for (IDesktopExtension ext : getDesktopExtensions()) {
        try {
          ContributionCommand cc = ext.pageDetailTableChangedDelegate(oldTable, m_pageDetailTable);
          if (cc == ContributionCommand.Stop) {
            break;
          }
        }
        catch (Throwable x) {
          LOG.error("extension " + ext, x);
        }
      }
    }
  }

  @Override
  public String getTitle() {
    return propertySupport.getPropertyString(PROP_TITLE);
  }

  @Override
  public void setTitle(String s) {
    propertySupport.setPropertyString(PROP_TITLE, s);
  }

  @Override
  public IProcessingStatus getStatus() {
    return (IProcessingStatus) propertySupport.getProperty(PROP_STATUS);
  }

  @Override
  public void setStatus(IProcessingStatus status) {
    propertySupport.setProperty(PROP_STATUS, status);
  }

  @Override
  public void setStatusText(String s) {
    if (s != null) {
      setStatus(new ProcessingStatus(s, null, 0, IProcessingStatus.INFO));
    }
    else {
      setStatus(null);
    }
  }

  @Override
  public void printDesktop(PrintDevice device, Map<String, Object> parameters) {
    try {
      firePrint(device, parameters);
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormPrint") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  @Override
  public void addFileChooser(IFileChooser fc) {
    fireFileChooserAdded(fc);
  }

  @Override
  public void openUrlInBrowser(String url) {
    openUrlInBrowser(url, null);
  }

  @Override
  public void openUrlInBrowser(String url, IUrlTarget target) {
    if (!UserAgentUtility.isWebClient()) {
      try {
        SERVICES.getService(IShellService.class).shellOpen(url);
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
    }
    else {
      if (target == null) {
        target = UrlTarget.AUTO;
      }
      fireOpenUrlInBrowser(url, target);
    }
  }

  @Override
  public boolean isAutoPrefixWildcardForTextSearch() {
    return m_autoPrefixWildcardForTextSearch;
  }

  @Override
  public void setAutoPrefixWildcardForTextSearch(boolean b) {
    m_autoPrefixWildcardForTextSearch = b;
  }

  @Override
  public boolean isOpened() {
    return propertySupport.getPropertyBool(PROP_OPENED);
  }

  private void setOpenedInternal(boolean b) {
    propertySupport.setPropertyBool(PROP_OPENED, b);
  }

  private void setGuiAvailableInternal(boolean guiAvailable) {
    propertySupport.setPropertyBool(PROP_GUI_AVAILABLE, guiAvailable);
  }

  @Override
  public boolean isGuiAvailable() {
    return propertySupport.getPropertyBool(PROP_GUI_AVAILABLE);
  }

  @Override
  public void addDesktopListener(DesktopListener l) {
    m_listenerList.add(DesktopListener.class, l);
  }

  @Override
  public void removeDesktopListener(DesktopListener l) {
    m_listenerList.remove(DesktopListener.class, l);
  }

  @Override
  public void addDataChangeListener(DataChangeListener listener, Object... dataTypes) {
    if (dataTypes == null || dataTypes.length == 0) {
      EventListenerList list = m_dataChangeListenerList.get(null);
      if (list == null) {
        list = new EventListenerList();
        m_dataChangeListenerList.put(null, list);
      }
      list.add(DataChangeListener.class, listener);
    }
    else {
      for (Object dataType : dataTypes) {
        if (dataType != null) {
          EventListenerList list = m_dataChangeListenerList.get(dataType);
          if (list == null) {
            list = new EventListenerList();
            m_dataChangeListenerList.put(dataType, list);
          }
          list.add(DataChangeListener.class, listener);
        }
      }
    }
  }

  @Override
  public void removeDataChangeListener(DataChangeListener listener, Object... dataTypes) {
    if (dataTypes == null || dataTypes.length == 0) {
      for (Iterator<EventListenerList> it = m_dataChangeListenerList.values().iterator(); it.hasNext();) {
        EventListenerList list = it.next();
        list.remove(DataChangeListener.class, listener);
        if (list.getListenerCount(DataChangeListener.class) == 0) {
          it.remove();
        }
      }
    }
    else {
      for (Object dataType : dataTypes) {
        if (dataType != null) {
          EventListenerList list = m_dataChangeListenerList.get(dataType);
          if (list != null) {
            list.remove(DataChangeListener.class, listener);
            if (list.getListenerCount(DataChangeListener.class) == 0) {
              m_dataChangeListenerList.remove(dataType);
            }
          }
        }
      }
    }
  }

  @Override
  public boolean isDataChanging() {
    return m_dataChanging > 0;
  }

  @Override
  public void setDataChanging(boolean b) {
    if (b) {
      m_dataChanging++;
    }
    else {
      if (m_dataChanging > 0) {
        m_dataChanging--;
        if (m_dataChanging == 0) {
          processDataChangeBuffer();
        }
      }
    }
  }

  @Override
  public void dataChanged(Object... dataTypes) {
    if (isDataChanging()) {
      if (dataTypes != null && dataTypes.length > 0) {
        m_dataChangeEventBuffer.add(dataTypes);
      }
    }
    else {
      fireDataChangedImpl(dataTypes);
    }
  }

  private void processDataChangeBuffer() {
    Set<Object> knownEvents = new HashSet<Object>();
    for (Object[] dataTypes : m_dataChangeEventBuffer) {
      for (Object dataType : dataTypes) {
        knownEvents.add(dataType);
      }
    }
    m_dataChangeEventBuffer.clear();
    fireDataChangedImpl(knownEvents.toArray(new Object[knownEvents.size()]));
  }

  private void fireDataChangedImpl(Object... dataTypes) {
    if (dataTypes != null && dataTypes.length > 0) {
      HashMap<DataChangeListener, Set<Object>> map = new HashMap<DataChangeListener, Set<Object>>();
      for (Object dataType : dataTypes) {
        if (dataType != null) {
          EventListenerList list = m_dataChangeListenerList.get(dataType);
          if (list != null) {
            for (DataChangeListener listener : list.getListeners(DataChangeListener.class)) {
              Set<Object> typeSet = map.get(listener);
              if (typeSet == null) {
                typeSet = new HashSet<Object>();
                map.put(listener, typeSet);
              }
              typeSet.add(dataType);
            }
          }
        }
      }
      for (Map.Entry<DataChangeListener, Set<Object>> e : map.entrySet()) {
        DataChangeListener listener = e.getKey();
        Set<Object> typeSet = e.getValue();
        try {
          listener.dataChanged(typeSet.toArray());
        }
        catch (Throwable t) {
          LOG.error(null, t);
        }
      }
    }
  }

  @Override
  public void traverseFocusNext() {
    fireTransferFocusNext();
  }

  @Override
  public void traverseFocusPrevious() {
    fireTransferFocusPrevious();
  }

  private void fireTransferFocusNext() {
    fireDesktopEvent(new DesktopEvent(this, DesktopEvent.TYPE_TRAVERSE_FOCUS_NEXT));
  }

  private void fireTransferFocusPrevious() {
    fireDesktopEvent(new DesktopEvent(this, DesktopEvent.TYPE_TRAVERSE_FOCUS_PREVIOUS));
  }

  private void fireDesktopClosed() {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_DESKTOP_CLOSED);
    fireDesktopEvent(e);
  }

  private void firePrint(PrintDevice device, Map<String, Object> parameters) throws ProcessingException {
    fireDesktopEvent(new DesktopEvent(this, DesktopEvent.TYPE_PRINT, device, parameters));
  }

  private List<IMenu> fireTrayPopup() {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_TRAY_POPUP);
    // single observer for exec callback
    addLocalPopupMenus(e);
    fireDesktopEvent(e);
    return e.getPopupMenus();
  }

  /**
   * @param printedFile
   */
  private void fireDesktopPrinted(File printedFile) {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_PRINTED, printedFile);
    fireDesktopEvent(e);
  }

  private void fireOutlineChanged(IOutline oldOutline, IOutline newOutline) {
    if (oldOutline != newOutline) {
      //extensions
      for (IDesktopExtension ext : getDesktopExtensions()) {
        try {
          ContributionCommand cc = ext.outlineChangedDelegate(oldOutline, newOutline);
          if (cc == ContributionCommand.Stop) {
            break;
          }
        }
        catch (ProcessingException e) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(e);
        }
        catch (Throwable t) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException(oldOutline + " -> " + newOutline, t));
        }
      }
    }
    // fire
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_OUTLINE_CHANGED, newOutline);
    fireDesktopEvent(e);
  }

  private void fireFormAdded(IForm form) {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_FORM_ADDED, form);
    fireDesktopEvent(e);
  }

  private void fireFormEnsureVisible(IForm form) {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_FORM_ENSURE_VISIBLE, form);
    fireDesktopEvent(e);
  }

  private void fireFormRemoved(IForm form) {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_FORM_REMOVED, form);
    fireDesktopEvent(e);
  }

  private void fireMessageBoxAdded(IMessageBox mb) {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_MESSAGE_BOX_ADDED, mb);
    fireDesktopEvent(e);
  }

  private void fireFileChooserAdded(IFileChooser fc) {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_FILE_CHOOSER_ADDED, fc);
    fireDesktopEvent(e);
  }

  private void fireOpenUrlInBrowser(String path, IUrlTarget target) {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_OPEN_URL_IN_BROWSER, path, target);
    fireDesktopEvent(e);
  }

  private IFormField fireFindFocusOwner() {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_FIND_FOCUS_OWNER);
    fireDesktopEvent(e);
    return e.getFocusedField();
  }

  private IForm fireFindActiveForm() {
    DesktopEvent e = new DesktopEvent(this, DesktopEvent.TYPE_FIND_ACTIVE_FORM);
    fireDesktopEvent(e);
    return e.getActiveForm();
  }

  // main handler
  private void fireDesktopEvent(DesktopEvent e) {
    EventListener[] listeners = m_listenerList.getListeners(DesktopListener.class);
    if (listeners != null && listeners.length > 0) {
      for (EventListener element : listeners) {
        try {
          ((DesktopListener) element).desktopChanged(e);
        }
        catch (Throwable t) {
          LOG.error(null, t);
        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void addLocalPopupMenus(DesktopEvent event) {
    try {
      List<IMenu> list = new ArrayList<IMenu>();
      //extensions
      for (IDesktopExtension ext : getDesktopExtensions()) {
        try {
          ContributionCommand cc = ext.addTrayMenusDelegate(list);
          if (cc == ContributionCommand.Stop) {
            break;
          }
        }
        catch (Throwable t) {
          LOG.error("extension " + ext, t);
        }
      }
      for (IMenu m : list) {
        if (m != null) {
          m.prepareAction();
        }
      }
      for (IMenu m : list) {
        if (m != null && m.isVisible()) {
          event.addPopupMenu(m);
        }
      }
    }
    catch (Throwable t) {
      SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected", t));
    }
  }

  /**
   * @deprecated use {@link #activateBookmark(Bookmark)} instead. Will be removed in the N-Release.
   */
  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public void activateBookmark(Bookmark bm, boolean forceReload) throws ProcessingException {
    BookmarkUtility.activateBookmark(this, bm);
  }

  @Override
  public void activateBookmark(Bookmark bm) throws ProcessingException {
    BookmarkUtility.activateBookmark(this, bm);
  }

  @Override
  public Bookmark createBookmark() throws ProcessingException {
    return BookmarkUtility.createBookmark(this);
  }

  @Override
  public Bookmark createBookmark(IPage page) throws ProcessingException {
    return BookmarkUtility.createBookmark(page);
  }

  @Override
  public void refreshPages(Class<?>... pageTypes) {
    for (IOutline outline : getAvailableOutlines()) {
      outline.refreshPages(pageTypes);
    }
  }

  @Override
  public void refreshPages(List<Class<? extends IPage>> pages) {
    for (IOutline outline : getAvailableOutlines()) {
      outline.refreshPages(pages);
    }
  }

  @Override
  public void releaseUnusedPages() {
    for (IOutline outline : getAvailableOutlines()) {
      outline.releaseUnusedPages();
    }
  }

  @Override
  public void afterTablePageLoaded(IPageWithTable<?> tablePage) throws ProcessingException {
    //extensions
    for (IDesktopExtension ext : getDesktopExtensions()) {
      try {
        ContributionCommand cc = ext.tablePageLoadedDelegate(tablePage);
        if (cc == ContributionCommand.Stop) {
          break;
        }
      }
      catch (Throwable t) {
        LOG.error("extension " + ext, t);
      }
    }
  }

  @Override
  public void closeInternal() throws ProcessingException {
    setOpenedInternal(false);
    detachGui();

    List<IForm> openForms = new ArrayList<IForm>();
    // remove views
    for (IForm view : getViewStack()) {
      removeForm(view);
      openForms.add(view);
    }
    // remove forms
    for (IForm dialog : getDialogStack()) {
      removeForm(dialog);
      openForms.add(dialog);
    }
    //extensions
    for (IDesktopExtension ext : getDesktopExtensions()) {
      try {
        ContributionCommand cc = ext.desktopClosingDelegate();
        if (cc == ContributionCommand.Stop) {
          break;
        }
      }
      catch (Throwable t) {
        LOG.error("extension " + ext, t);
      }
    }

    // gather tool button forms
    for (IToolButton toolButton : getToolButtons()) {
      if (toolButton instanceof AbstractFormToolButton) {
        AbstractFormToolButton<?> formToolButton = (AbstractFormToolButton<?>) toolButton;
        IForm form = formToolButton.getForm();
        if (form != null) {
          openForms.add(form);
          formToolButton.setForm(null);
        }
      }
    }

    // close open forms
    for (IForm form : openForms) {
      if (form != null) {
        try {
          form.doClose();
        }
        catch (ProcessingException e) {
          LOG.error("Exception while closing form", e);
        }
      }
    }

    // outlines
    for (IOutline outline : getAvailableOutlines()) {
      outline.removeAllChildNodes(outline.getRootNode());
      outline.disposeTree();
    }

    fireDesktopClosed();
  }

  private void attachGui() {
    if (isGuiAvailable()) {
      return;
    }
    setGuiAvailableInternal(true);

    //extensions
    for (IDesktopExtension ext : getDesktopExtensions()) {
      try {
        ContributionCommand cc = ext.guiAttachedDelegate();
        if (cc == ContributionCommand.Stop) {
          break;
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
      catch (Throwable t) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected by " + ext, t));
      }
    }
  }

  private void detachGui() {
    if (!isGuiAvailable()) {
      return;
    }
    setGuiAvailableInternal(false);

    //extensions
    for (IDesktopExtension ext : getDesktopExtensions()) {
      try {
        ContributionCommand cc = ext.guiDetachedDelegate();
        if (cc == ContributionCommand.Stop) {
          break;
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
      catch (Throwable t) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected by " + ext, t));
      }
    }
  }

  public boolean runMenu(Class<? extends IMenu> menuType) throws ProcessingException {
    for (IMenu m : getMenus()) {
      if (runMenuRec(m, menuType)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  private boolean runMenuRec(IMenu m, Class<? extends IMenu> menuType) throws ProcessingException {
    if (m.getClass() == menuType) {
      m.prepareAction();
      if (m.isVisible() && m.isEnabled()) {
        m.doAction();
        return true;
      }
      else {
        return false;
      }
    }
    // children
    for (IMenu c : m.getChildActions()) {
      if (runMenuRec(c, menuType)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public IDesktopUIFacade getUIFacade() {
    return m_uiFacade;
  }

  private boolean isForcedClosing() {
    return m_isForcedClosing;
  }

  private void setForcedClosing(boolean forcedClosing) {
    m_isForcedClosing = forcedClosing;
  }

  /**
   * local desktop extension that calls local exec methods and returns local contributions in this class itself
   */
  private class P_LocalDesktopExtension implements IDesktopExtension {
    @Override
    public IDesktop getCoreDesktop() {
      return AbstractDesktop.this;
    }

    @Override
    public void setCoreDesktop(IDesktop desktop) {
      //nop
    }

    @Override
    public void contributeOutlines(OrderedCollection<IOutline> outlines) {
      List<Class<? extends IOutline>> configuredOutlines = getConfiguredOutlines();
      if (configuredOutlines != null) {
        for (Class<?> element : configuredOutlines) {
          try {
            IOutline o = (IOutline) element.newInstance();
            outlines.addOrdered(o);
          }
          catch (Throwable t) {
            SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + element.getName() + "'.", t));
          }
        }
      }
    }

    @Override
    public void contributeActions(Collection<IAction> actions) {
      for (Class<? extends IAction> actionClazz : getConfiguredActions()) {
        try {
          actions.add(ConfigurationUtility.newInnerInstance(AbstractDesktop.this, actionClazz));
        }
        catch (Exception e) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + actionClazz.getName() + "'.", e));
        }
      }
    }

    @Override
    public ContributionCommand initDelegate() throws ProcessingException {
      interceptInit();
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand desktopOpenedDelegate() throws ProcessingException {
      interceptOpened();
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand desktopBeforeClosingDelegate() throws ProcessingException {
      interceptBeforeClosing();
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand desktopClosingDelegate() throws ProcessingException {
      interceptClosing();
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand guiAttachedDelegate() throws ProcessingException {
      interceptGuiAttached();
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand guiDetachedDelegate() throws ProcessingException {
      interceptGuiDetached();
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand outlineChangedDelegate(IOutline oldOutline, IOutline newOutline) throws ProcessingException {
      interceptOutlineChanged(oldOutline, newOutline);
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand customFormModificationDelegate(IHolder<IForm> formHolder) throws ProcessingException {
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand pageSearchFormChangedDelegate(IForm oldForm, IForm newForm) throws ProcessingException {
      interceptPageSearchFormChanged(oldForm, newForm);
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand pageDetailFormChangedDelegate(IForm oldForm, IForm newForm) throws ProcessingException {
      interceptPageDetailFormChanged(oldForm, newForm);
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand pageDetailTableChangedDelegate(ITable oldTable, ITable newTable) throws ProcessingException {
      interceptPageDetailTableChanged(oldTable, newTable);
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand tablePageLoadedDelegate(IPageWithTable<?> tablePage) throws ProcessingException {
      interceptTablePageLoaded(tablePage);
      return ContributionCommand.Continue;
    }

    @Override
    public ContributionCommand addTrayMenusDelegate(List<IMenu> menus) throws ProcessingException {
      interceptAddTrayMenus(menus);
      return ContributionCommand.Continue;
    }
  }

  private class P_UIFacade implements IDesktopUIFacade {

    @Override
    public void fireGuiAttached() {
      attachGui();
    }

    @Override
    public void fireGuiDetached() {
      detachGui();
    }

    @Override
    public void fireDesktopOpenedFromUI() {
      setOpenedInternal(true);
      //extensions
      for (IDesktopExtension ext : getDesktopExtensions()) {
        try {
          ContributionCommand cc = ext.desktopOpenedDelegate();
          if (cc == ContributionCommand.Stop) {
            break;
          }
        }
        catch (ProcessingException e) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(e);
        }
        catch (Throwable t) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected by " + ext, t));
        }
      }
    }

    @Override
    public void fireDesktopClosingFromUI(boolean forcedClosing) {
      setForcedClosing(forcedClosing);
      // necessary so that no forms can be opened anymore.
      if (forcedClosing) {
        setOpenedInternal(false);
      }
      ClientSyncJob.getCurrentSession().stopSession();
    }

    @Override
    public List<IMenu> fireTrayPopupFromUI() {
      return fireTrayPopup();
    }

    @Override
    public void fireDesktopPrintedFromUI(File printedFile) {
      fireDesktopPrinted(printedFile);
    }

  }

  private class P_ActiveOutlineListener extends TreeAdapter implements PropertyChangeListener {
    @Override
    public void treeChanged(TreeEvent e) {
      switch (e.getType()) {
        case TreeEvent.TYPE_BEFORE_NODES_SELECTED: {
          if (e.getDeselectedNode() instanceof IPage) {
            IPage deselectedPage = (IPage) e.getDeselectedNode();
            SERVICES.getService(INavigationHistoryService.class).addStep(0, deselectedPage);
          }
          break;
        }
        case TreeEvent.TYPE_NODES_SELECTED: {
          IPage page = m_outline.getActivePage();
          if (page != null) {
            SERVICES.getService(INavigationHistoryService.class).addStep(0, page);
          }
          try {
            ClientSyncJob.getCurrentSession().getMemoryPolicy().afterOutlineSelectionChanged(AbstractDesktop.this);
          }
          catch (Throwable t) {
            LOG.warn("MemoryPolicy.afterOutlineSelectionChanged", t);
          }
          break;
        }
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
      if (e.getPropertyName().equals(IOutline.PROP_DETAIL_FORM)) {
        setPageDetailForm(((IOutline) e.getSource()).getDetailForm());
      }
      else if (e.getPropertyName().equals(IOutline.PROP_DETAIL_TABLE)) {
        setPageDetailTable(((IOutline) e.getSource()).getDetailTable());
      }
      else if (e.getPropertyName().equals(IOutline.PROP_SEARCH_FORM)) {
        setPageSearchForm(((IOutline) e.getSource()).getSearchForm());
      }
    }
  }

  private class P_ActivatedFormListener implements FormListener {
    @Override
    public void formChanged(FormEvent e) throws ProcessingException {
      if (e.getType() != FormEvent.TYPE_ACTIVATED) {
        return;
      }
      // remove garbage collected references and the one of the given form event
      for (Iterator<WeakReference<IForm>> it = m_lastActiveFormList.iterator(); it.hasNext();) {
        WeakReference<IForm> formRef = it.next();
        if (formRef.get() == null || formRef.get().equals(e.getForm())) {
          it.remove();
        }
      }

      // add changed form at the beginning -> last activated form
      m_lastActiveFormList.add(0, new WeakReference<IForm>(e.getForm()));
    }
  }

  @Override
  public void changeVisibilityAfterOfflineSwitch() {
  }

  /**
   * <p>
   * Default behavior of the pre-hook is to delegate the before closing call to the each desktop extension. The method
   * {@link IDesktopExtension#desktopBeforeClosingDelegate()} of each desktop extension will be called. If at least one
   * of the desktop extension's delegate throws a {@link VetoException}, the closing process will be stopped.
   * </p>
   * <p>
   * Additionally, before closing, a dialog is shown to allow the user to save unsaved forms. At this point it is also
   * possible to cancel closing.
   * </p>
   * If the flag {@link AbstractDesktop#m_isForcedClosing} is set to <code>true</code>, closing is always continued,
   * without considering desktopExtensions or unsaved forms.
   */
  @Override
  public boolean doBeforeClosingInternal() {
    return isForcedClosing() || (continueClosingInDesktopExtensions() && continueClosingConsideringUnsavedForms());
  }

  protected boolean continueClosingConsideringUnsavedForms() {
    List<IForm> forms = getUnsavedForms();
    if (forms.size() > 0) {
      try {
        UnsavedFormChangesForm f = new UnsavedFormChangesForm(forms);
        f.startNew();
        f.waitFor();
        if (f.getCloseSystemType() == IButton.SYSTEM_TYPE_CANCEL) {
          return false;
        }
      }
      catch (ProcessingException e) {
        LOG.error("Error closing forms", e);
      }
    }
    return true;
  }

  private boolean continueClosingInDesktopExtensions() {
    boolean continueClosing = true;
    List<IDesktopExtension> extensions = getDesktopExtensions();
    if (extensions != null) {
      for (IDesktopExtension ext : extensions) {
        try {
          ContributionCommand cc = ext.desktopBeforeClosingDelegate();
          if (cc == ContributionCommand.Stop) {
            break;
          }
        }
        catch (VetoException e) {
          continueClosing = false;
        }
        catch (ProcessingException e) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(e);
        }
        catch (Throwable t) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Desktop before closing error", t));
        }
      }
    }
    return continueClosing;
  }

  @Override
  public List<IForm> getUnsavedForms() {
    List<IForm> saveNeededForms = new ArrayList<IForm>();
    List<IForm> openForms = CollectionUtility.combine(getViewStack(), getDialogStack());
    // last element on the stack is the first that needs to be saved: iterate from end to start
    for (int i = openForms.size() - 1; i >= 0; i--) {
      IForm f = openForms.get(i);
      if (f.isAskIfNeedSave() && f.isSaveNeeded()) {
        saveNeededForms.add(f);
      }
    }
    return saveNeededForms;
  }

  /**
   * The extension delegating to the local methods. This Extension is always at the end of the chain and will not call
   * any further chain elements.
   */
  protected static class LocalDesktopExtension<DESKTOP extends AbstractDesktop> extends AbstractExtension<DESKTOP> implements org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<DESKTOP> {

    public LocalDesktopExtension(DESKTOP desktop) {
      super(desktop);
    }

    @Override
    public void execInit(DesktopInitChain chain) throws ProcessingException {
      getOwner().execInit();
    }

    @Override
    public void execOpened(DesktopOpenedChain chain) throws ProcessingException {
      getOwner().execOpened();
    }

    @Override
    public void execAddTrayMenus(DesktopAddTrayMenusChain chain, List<IMenu> menus) throws ProcessingException {
      getOwner().execAddTrayMenus(menus);
    }

    @Override
    public void execBeforeClosing(DesktopBeforeClosingChain chain) throws ProcessingException {
      getOwner().execBeforeClosing();
    }

    @Override
    public void execPageDetailFormChanged(DesktopPageDetailFormChangedChain chain, IForm oldForm, IForm newForm) throws ProcessingException {
      getOwner().execPageDetailFormChanged(oldForm, newForm);
    }

    @Override
    public void execTablePageLoaded(DesktopTablePageLoadedChain chain, IPageWithTable<?> tablePage) throws ProcessingException {
      getOwner().execTablePageLoaded(tablePage);
    }

    @Override
    public void execOutlineChanged(DesktopOutlineChangedChain chain, IOutline oldOutline, IOutline newOutline) throws ProcessingException {
      getOwner().execOutlineChanged(oldOutline, newOutline);
    }

    @Override
    public void execClosing(DesktopClosingChain chain) throws ProcessingException {
      getOwner().execClosing();
    }

    @Override
    public void execPageSearchFormChanged(DesktopPageSearchFormChangedChain chain, IForm oldForm, IForm newForm) throws ProcessingException {
      getOwner().execPageSearchFormChanged(oldForm, newForm);
    }

    @Override
    public void execPageDetailTableChanged(DesktopPageDetailTableChangedChain chain, ITable oldTable, ITable newTable) throws ProcessingException {
      getOwner().execPageDetailTableChanged(oldTable, newTable);
    }

    @Override
    public void execGuiAttached(DesktopGuiAttachedChain chain) throws ProcessingException {
      getOwner().execGuiAttached();
    }

    @Override
    public void execGuiDetached(DesktopGuiDetachedChain chain) throws ProcessingException {
      getOwner().execGuiDetached();
    }

  }

  protected final void interceptOpened() throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopOpenedChain chain = new DesktopOpenedChain(extensions);
    chain.execOpened();
  }

  protected final void interceptAddTrayMenus(List<IMenu> menus) throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopAddTrayMenusChain chain = new DesktopAddTrayMenusChain(extensions);
    chain.execAddTrayMenus(menus);
  }

  protected final void interceptBeforeClosing() throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopBeforeClosingChain chain = new DesktopBeforeClosingChain(extensions);
    chain.execBeforeClosing();
  }

  protected final void interceptPageDetailFormChanged(IForm oldForm, IForm newForm) throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopPageDetailFormChangedChain chain = new DesktopPageDetailFormChangedChain(extensions);
    chain.execPageDetailFormChanged(oldForm, newForm);
  }

  protected final void interceptTablePageLoaded(IPageWithTable<?> tablePage) throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopTablePageLoadedChain chain = new DesktopTablePageLoadedChain(extensions);
    chain.execTablePageLoaded(tablePage);
  }

  protected final void interceptOutlineChanged(IOutline oldOutline, IOutline newOutline) throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopOutlineChangedChain chain = new DesktopOutlineChangedChain(extensions);
    chain.execOutlineChanged(oldOutline, newOutline);
  }

  protected final void interceptClosing() throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopClosingChain chain = new DesktopClosingChain(extensions);
    chain.execClosing();
  }

  protected final void interceptPageSearchFormChanged(IForm oldForm, IForm newForm) throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopPageSearchFormChangedChain chain = new DesktopPageSearchFormChangedChain(extensions);
    chain.execPageSearchFormChanged(oldForm, newForm);
  }

  protected final void interceptPageDetailTableChanged(ITable oldTable, ITable newTable) throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopPageDetailTableChangedChain chain = new DesktopPageDetailTableChangedChain(extensions);
    chain.execPageDetailTableChanged(oldTable, newTable);
  }

  protected final void interceptGuiAttached() throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopGuiAttachedChain chain = new DesktopGuiAttachedChain(extensions);
    chain.execGuiAttached();
  }

  protected final void interceptGuiDetached() throws ProcessingException {
    List<? extends org.eclipse.scout.rt.client.extension.ui.desktop.IDesktopExtension<? extends AbstractDesktop>> extensions = getAllExtensions();
    DesktopGuiDetachedChain chain = new DesktopGuiDetachedChain(extensions);
    chain.execGuiDetached();
  }
}
