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
package org.eclipse.scout.rt.client.ui.desktop.outline;

import java.security.Permission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.OptimisticLock;
import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.IOrdered;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.extension.ui.basic.tree.ITreeExtension;
import org.eclipse.scout.rt.client.extension.ui.desktop.outline.IOutlineExtension;
import org.eclipse.scout.rt.client.extension.ui.desktop.outline.OutlineChains.OutlineCreateChildPagesChain;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.MenuSeparator;
import org.eclipse.scout.rt.client.ui.action.menu.TableMenuType;
import org.eclipse.scout.rt.client.ui.action.menu.TreeMenuType;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.internal.TablePageTreeMenuWrapper;
import org.eclipse.scout.rt.client.ui.basic.tree.AbstractTree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNodeFilter;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeVisitor;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeAdapter;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.AbstractPageWithNodes;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithNodes;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.ISearchForm;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.service.SERVICES;

public abstract class AbstractOutline extends AbstractTree implements IOutline {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractOutline.class);

  // visible is defined as: visibleGranted && visibleProperty
  private boolean m_visibleGranted;
  private boolean m_visibleProperty;
  private IPage m_contextPage;
  private IPageChangeStrategy m_pageChangeStrategy;
  private OptimisticLock m_contextPageOptimisticLock;
  private OutlineMediator m_outlineMediator;

  // internal usage of menus temporarily added to the tree.
  private List<IMenu> m_inheritedMenusOfPage;

  public AbstractOutline() {
    super();
  }

  public AbstractOutline(boolean callInitialzier) {
    super(callInitialzier);
  }

  /*
   * Configuration
   */

  /**
   * Configures whether this outline is enabled.
   * <p>
   * Subclasses can override this method. Default is {@code true}.
   *
   * @return {@code true} if this outline is enabled, {@code false} otherwise
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(80)
  protected boolean getConfiguredEnabled() {
    return true;
  }

  /**
   * Configures the visibility of this outline.
   * <p>
   * Subclasses can override this method. Default is {@code true}.
   *
   * @return {@code true} if this outline is visible, {@code false} otherwise
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(90)
  protected boolean getConfiguredVisible() {
    return true;
  }

  /**
   * Configures the view order of this outline. The view order determines the order in which the outline appears.<br>
   * The view order of outlines with no view order configured ({@code < 0}) is initialized based on the {@link Order}
   * annotation of the class.
   * <p>
   * Subclasses can override this method. The default is {@link IOrdered#DEFAULT_ORDER}.
   *
   * @return View order of this outline.
   */
  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(120)
  protected double getConfiguredViewOrder() {
    return IOrdered.DEFAULT_ORDER;
  }

  /**
   * Called during initialization of this outline. Allows to add child pages to the outline tree. All added pages are
   * roots of the visible tree.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @param pageList
   *          live collection to add pages to the outline tree
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(90)
  protected void execCreateChildPages(List<IPage> pageList) throws ProcessingException {
  }

  protected void createChildPagesInternal(List<IPage> pageList) throws ProcessingException {
    interceptCreateChildPages(pageList);
  }

  /**
   * By default the outline tree tries to delegate the drop to the affected
   * page.
   */
  @Override
  protected void execDrop(ITreeNode node, TransferObject t) {
    if (node instanceof IPageWithTable) {
      ITable table = ((IPageWithTable) node).getTable();
      if (table.getDropType() != 0) {
        table.getUIFacade().fireRowDropActionFromUI(null, t);
      }
    }
  }

  @Override
  protected void initConfig() {
    m_visibleGranted = true;
    m_contextPageOptimisticLock = new OptimisticLock();
    setPageChangeStrategy(createPageChangeStrategy());
    m_outlineMediator = createOutlineMediator();
    addTreeListener(new P_OutlineListener());
    addNodeFilter(new P_TableFilterBasedTreeNodeFilter());
    super.initConfig();
    setRootNodeVisible(false);
    IPage rootPage = new InvisibleRootPage();
    setRootNode(rootPage);
    setEnabled(getConfiguredEnabled());
    setVisible(getConfiguredVisible());
    setOrder(calculateViewOrder());
  }

  /*
   * Runtime
   */

  @Override
  public IPage getActivePage() {
    return (IPage) getSelectedNode();
  }

  /**
   * Calculates the actions's view order, e.g. if the @Order annotation is set to 30.0, the method will
   * return 30.0. If no {@link Order} annotation is set, the method checks its super classes for an @Order annotation.
   *
   * @since 4.0.1
   */
  protected double calculateViewOrder() {
    double viewOrder = getConfiguredViewOrder();
    if (viewOrder == IOrdered.DEFAULT_ORDER) {
      Class<?> cls = getClass();
      while (cls != null && IOutline.class.isAssignableFrom(cls)) {
        if (cls.isAnnotationPresent(Order.class)) {
          Order order = (Order) cls.getAnnotation(Order.class);
          return order.value();
        }
        cls = cls.getSuperclass();
      }
    }
    return viewOrder;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void refreshPages(Class<?>... pageTypes) {
    if (pageTypes == null || pageTypes.length < 1) {
      return;
    }
    List<Class<? extends IPage>> list = new ArrayList<Class<? extends IPage>>(pageTypes.length);
    for (Class<?> c : pageTypes) {
      if (IPage.class.isAssignableFrom(c)) {
        list.add((Class<? extends IPage>) c);
      }
    }
    refreshPages(list);
  }

  @Override
  public void refreshPages(final List<Class<? extends IPage>> pageTypes) {
    final List<IPage> candidates = new ArrayList<IPage>();
    ITreeVisitor v = new ITreeVisitor() {
      @Override
      public boolean visit(ITreeNode node) {
        IPage page = (IPage) node;
        if (page == null) {
          return true;
        }
        Class<? extends IPage> pageClass = page.getClass();
        for (Class<? extends IPage> c : pageTypes) {
          if (c.isAssignableFrom(pageClass)) {
            candidates.add(page);
          }
        }
        return true;
      }
    };
    visitNode(getRootNode(), v);
    for (IPage page : candidates) {
      if (page.getTree() != null) {
        page.dataChanged();
      }
    }
  }

  @Override
  public void releaseUnusedPages() {
    final HashSet<IPage> preservationSet = new HashSet<IPage>();
    IPage oldSelection = (IPage) getSelectedNode();
    IPage p = oldSelection;
    if (p != null) {
      while (p != null) {
        preservationSet.add(p);
        p = p.getParentPage();
      }
    }
    ITreeVisitor v = new ITreeVisitor() {
      @Override
      public boolean visit(ITreeNode node) {
        IPage page = (IPage) node;
        if (preservationSet.contains(page)) {
          // nop
        }
        else if (page.isChildrenLoaded() && (!page.isExpanded() || !(page.getParentPage() != null && page.getParentPage().isChildrenLoaded()))) {
          try {
            unloadNode(page);
          }
          catch (ProcessingException e) {
            SERVICES.getService(IExceptionHandlerService.class).handleException(e);
          }
        }
        return true;
      }
    };
    try {
      setTreeChanging(true);
      visitNode(getRootNode(), v);
      if (oldSelection != null) {
        IPage selectedPage = (IPage) getSelectedNode();
        if (selectedPage == null) {
          try {
            getRootNode().ensureChildrenLoaded();
            List<ITreeNode> children = getRootNode().getFilteredChildNodes();
            if (CollectionUtility.hasElements(children)) {
              selectNode(CollectionUtility.firstElement(children));
            }
          }
          catch (ProcessingException e) {
            LOG.warn(null, e);
          }
        }
      }
    }
    finally {
      setTreeChanging(false);
    }
  }

  @Override
  public <T extends IPage> T findPage(final Class<T> pageType) {
    final Holder<T> result = new Holder<T>(pageType, null);
    ITreeVisitor v = new ITreeVisitor() {
      @Override
      @SuppressWarnings("unchecked")
      public boolean visit(ITreeNode node) {
        IPage page = (IPage) node;
        Class<? extends IPage> pageClass = page.getClass();
        if (pageType.isAssignableFrom(pageClass)) {
          result.setValue((T) page);
        }
        return result.getValue() == null;
      }
    };
    visitNode(getRootNode(), v);
    return result.getValue();
  }

  @Override
  public void setVisiblePermission(Permission p) {
    boolean b;
    if (p != null) {
      b = SERVICES.getService(IAccessControlService.class).checkPermission(p);
    }
    else {
      b = true;
    }
    setVisibleGranted(b);
  }

  @Override
  public boolean isVisibleGranted() {
    return m_visibleGranted;
  }

  @Override
  public void setVisibleGranted(boolean b) {
    m_visibleGranted = b;
    calculateVisible();
  }

  @Override
  public boolean isVisible() {
    return propertySupport.getPropertyBool(PROP_VISIBLE);
  }

  @Override
  public void setVisible(boolean b) {
    m_visibleProperty = b;
    calculateVisible();
  }

  private void calculateVisible() {
    propertySupport.setPropertyBool(PROP_VISIBLE, m_visibleGranted && m_visibleProperty);
  }

  @Override
  public IForm getDetailForm() {
    return (IForm) propertySupport.getProperty(PROP_DETAIL_FORM);
  }

  @Override
  public void setDetailForm(IForm form) {
    propertySupport.setProperty(PROP_DETAIL_FORM, form);
  }

  @Override
  public ITable getDetailTable() {
    return (ITable) propertySupport.getProperty(PROP_DETAIL_TABLE);
  }

  @Override
  public void setDetailTable(ITable table) {
    propertySupport.setProperty(PROP_DETAIL_TABLE, table);
  }

  @Override
  public IForm getSearchForm() {
    return (IForm) propertySupport.getProperty(PROP_SEARCH_FORM);
  }

  @Override
  public void setSearchForm(IForm form) {
    propertySupport.setProperty(PROP_SEARCH_FORM, form);
  }

  @Override
  public double getOrder() {
    return propertySupport.getPropertyDouble(PROP_VIEW_ORDER);
  }

  @Override
  public void setOrder(double order) {
    propertySupport.setPropertyDouble(PROP_VIEW_ORDER, order);
  }

  @Override
  public IPage getRootPage() {
    return (IPage) getRootNode();
  }

  @Override
  public void unloadNode(ITreeNode node) throws ProcessingException {
    try {
      setTreeChanging(true);
      //
      super.unloadNode(node);
      if (node instanceof IPageWithTable) {
        ((IPageWithTable) node).getTable().deleteAllRows();
      }
    }
    finally {
      setTreeChanging(false);
    }
  }

  @Override
  public void resetOutline() throws ProcessingException {
    if (getRootNode() != null) {
      try {
        setTreeChanging(true);
        //
        selectNode(null);
        unloadNode(getRootNode());
        getRootNode().ensureChildrenLoaded();
      }
      finally {
        setTreeChanging(false);
      }
      ITreeNode root = getRootNode();
      if (root instanceof IPageWithTable) {
        ISearchForm searchForm = ((IPageWithTable) root).getSearchFormInternal();
        if (searchForm != null) {
          searchForm.doReset();
        }
      }
      if (!isRootNodeVisible()) {
        root.setExpanded(true);
      }
      selectFirstNode();
      if (getSelectedNode() instanceof IPageWithTable) {
        getSelectedNode().setExpanded(true);
      }
    }
  }

  @Override
  public void makeActivePageToContextPage() {
    IPage activePage = getActivePage();
    if (activePage != null && m_contextPage != activePage) {
      m_contextPage = activePage;
      addMenusOfActivePageToContextMenu(activePage);
      activePage.pageActivatedNotify();
    }
  }

  @Override
  public List<IMenu> getMenusForPage(IPage page) {
    List<IMenu> result = new ArrayList<IMenu>();
    for (IMenu m : getContextMenu().getChildActions()) {
      if (!m_inheritedMenusOfPage.contains(m)) {
        result.add(m);
      }
    }
    result.addAll(computeInheritedMenusOfPage(page));
    return result;
  }

  /**
   * @see IOutline#getMenusForPage(IPage)
   */
  protected List<IMenu> computeInheritedMenusOfPage(IPage activePage) {
    List<IMenu> menus = new ArrayList<IMenu>();
    if (activePage instanceof IPageWithTable<?>) {
      // in case of a page with table the empty space actions of the table will be added to the context menu of the tree.
      IPageWithTable<?> pageWithTable = (IPageWithTable<?>) activePage;
      if (pageWithTable.isShowEmptySpaceMenus()) {
        ITable table = pageWithTable.getTable();
        List<IMenu> emptySpaceMenus = ActionUtility.getActions(table.getMenus(),
            ActionUtility.createMenuFilterMenuTypes(CollectionUtility.hashSet(TableMenuType.EmptySpace), false));
        if (emptySpaceMenus.size() > 0) {
          menus.add(new MenuSeparator());
          for (IMenu menu : emptySpaceMenus) {
            menus.add(menu);
          }
        }
      }
    }

    // in case of a page with nodes add the single selection menus of its parent table for the current node/row.
    IPage parentPage = activePage.getParentPage();
    if (parentPage instanceof IPageWithTable<?>) {
      IPageWithTable<?> pageWithTable = (IPageWithTable<?>) parentPage;
      ITableRow row = pageWithTable.getTableRowFor(activePage);
      ITable table = pageWithTable.getTable();
      if (row != null) {
        table.getUIFacade().setSelectedRowsFromUI(CollectionUtility.arrayList(row));
        List<IMenu> parentTableMenus = ActionUtility.getActions(table.getContextMenu().getChildActions(), ActionUtility.createMenuFilterMenuTypes(CollectionUtility.hashSet(TableMenuType.SingleSelection), false));
        if (parentTableMenus.size() > 0) {
          menus.add(new MenuSeparator());
          for (IMenu menu : parentTableMenus) {
            menus.add(menu);
          }
        }
      }
    }
    return menus;
  }

  protected void addMenusOfActivePageToContextMenu(IPage activePage) {
    List<IMenu> wrappedMenus = new ArrayList<IMenu>();
    for (IMenu m : computeInheritedMenusOfPage(activePage)) {
      if (m.isSeparator()) {
        wrappedMenus.add(m);
      }
      else {
        wrappedMenus.add(new TablePageTreeMenuWrapper(m, TreeMenuType.SingleSelection));
      }
    }
    m_inheritedMenusOfPage = wrappedMenus;
    getContextMenu().addChildActions(m_inheritedMenusOfPage);
  }

  @Override
  public void clearContextPage() {
    IPage page = m_contextPage;
    if (page != null) {
      // remove menus of the active page
      removeMenusOfActivePageToContextMenu();
      m_contextPage = null;
      page.pageDeactivatedNotify();
    }
  }

  protected void removeMenusOfActivePageToContextMenu() {
    if (m_inheritedMenusOfPage != null) {
      getContextMenu().removeChildActions(m_inheritedMenusOfPage);
      m_inheritedMenusOfPage = null;
    }
  }

  private void handleActivePageChanged(IPage deselectedPage, IPage selectedPage) {
    if (m_pageChangeStrategy == null) {
      return;
    }

    try {
      if (m_contextPageOptimisticLock.acquire()) {
        m_pageChangeStrategy.pageChanged(this, deselectedPage, selectedPage);
      }
    }
    finally {
      m_contextPageOptimisticLock.release();
    }
  }

  @Override
  public OutlineMediator getOutlineMediator() {
    return m_outlineMediator;
  }

  protected OutlineMediator createOutlineMediator() {
    return new OutlineMediator();
  }

  protected IPageChangeStrategy createPageChangeStrategy() {
    return new DefaultPageChangeStrategy();
  }

  @Override
  public void setPageChangeStrategy(IPageChangeStrategy pageChangeStrategy) {
    m_pageChangeStrategy = pageChangeStrategy;
  }

  @Override
  public IPageChangeStrategy getPageChangeStrategy() {
    return m_pageChangeStrategy;
  }

  private class P_OutlineListener extends TreeAdapter {
    @Override
    public void treeChanged(TreeEvent e) {
      switch (e.getType()) {
        case TreeEvent.TYPE_NODES_SELECTED: {
          handleActivePageChanged((IPage) e.getDeselectedNode(), (IPage) e.getNewSelectedNode());
          break;
        }
      }

      ITreeNode commonParentNode = e.getCommonParentNode();
      if (commonParentNode instanceof IPageWithNodes) {
        handlePageWithNodesTreeEvent(e, (IPageWithNodes) commonParentNode);
      }
      else if (commonParentNode instanceof IPageWithTable<?>) {
        handlePageWithTableTreeEvent(e, (IPageWithTable<?>) commonParentNode);
      }
    }

    private void handlePageWithNodesTreeEvent(TreeEvent e, IPageWithNodes pageWithNodes) {
      OutlineMediator outlineMediator = getOutlineMediator();
      if (outlineMediator == null) {
        return;
      }

      switch (e.getType()) {
        case TreeEvent.TYPE_CHILD_NODE_ORDER_CHANGED:
        case TreeEvent.TYPE_NODES_DELETED:
        case TreeEvent.TYPE_NODES_INSERTED:
        case TreeEvent.TYPE_NODES_UPDATED: {
          outlineMediator.mediateTreeNodesChanged(pageWithNodes);
          break;
        }
      }
    }

    private void handlePageWithTableTreeEvent(TreeEvent e, IPageWithTable<? extends ITable> pageWithTable) {
      OutlineMediator outlineMediator = getOutlineMediator();
      if (outlineMediator == null) {
        return;
      }

      switch (e.getType()) {
        case TreeEvent.TYPE_NODE_ACTION: {
          outlineMediator.mediateTreeNodeAction(e, pageWithTable);
          break;
        }
        case TreeEvent.TYPE_NODES_DRAG_REQUEST: {
          outlineMediator.mediateTreeNodesDragRequest(e, pageWithTable);
          break;
        }
        case TreeEvent.TYPE_NODE_DROP_ACTION: {
          outlineMediator.mediateTreeNodeDropAction(e, pageWithTable);
          break;
        }
      }

    }
  }

  /**
   * If the class is annotated with {@link ClassId}, the annotation value is returned.<br/>
   * Otherwise the class name.
   */
  @Override
  public String classId() {
    return ConfigurationUtility.getAnnotatedClassIdWithFallback(getClass(), false);
  }

  private class P_TableFilterBasedTreeNodeFilter implements ITreeNodeFilter {
    @Override
    public boolean accept(ITreeNode node, int level) {
      ITreeNode parentNode = node.getParentNode();
      if (parentNode != null && !parentNode.isFilterAccepted()) {
        // hide page if parent page is filtered
        return false;
      }
      if (parentNode instanceof IPageWithTable<?>) {
        ITableRow tableRow = ((IPageWithTable<?>) parentNode).getTableRowFor(node);
        return tableRow == null || tableRow.isFilterAccepted();
      }
      else if (parentNode instanceof IPageWithNodes) {
        for (ITreeNode child : parentNode.getChildNodes()) {
          if (child.equals(node)) {
            return ((IPageWithNodes) parentNode).isFilterAcceptedForChildNode(node);
          }
        }
      }
      return true;
    }
  }

  private class InvisibleRootPage extends AbstractPageWithNodes {
    @Override
    protected void execCreateChildPages(List<IPage> pageList) throws ProcessingException {
      AbstractOutline.this.createChildPagesInternal(pageList);
    }
  }

  protected final void interceptCreateChildPages(List<IPage> pageList) throws ProcessingException {
    List<? extends ITreeExtension<? extends AbstractTree>> extensions = getAllExtensions();
    OutlineCreateChildPagesChain chain = new OutlineCreateChildPagesChain(extensions);
    chain.execCreateChildPages(pageList);
  }

  protected static class LocalOutlineExtension<OWNER extends AbstractOutline> extends LocalTreeExtension<OWNER> implements IOutlineExtension<OWNER> {

    public LocalOutlineExtension(OWNER owner) {
      super(owner);
    }

    @Override
    public void execCreateChildPages(OutlineCreateChildPagesChain chain, List<IPage> pageList) throws ProcessingException {
      getOwner().execCreateChildPages(pageList);
    }
  }

  @Override
  protected IOutlineExtension<? extends AbstractOutline> createLocalExtension() {
    return new LocalOutlineExtension<AbstractOutline>(this);
  }

}
