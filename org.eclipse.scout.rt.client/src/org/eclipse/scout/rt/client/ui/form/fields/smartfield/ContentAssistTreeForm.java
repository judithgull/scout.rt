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
package org.eclipse.scout.rt.client.ui.form.fields.smartfield;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.TriState;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.job.JobEx;
import org.eclipse.scout.rt.client.ClientAsyncJob;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.ui.MouseButton;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.tree.AbstractTree;
import org.eclipse.scout.rt.client.ui.basic.tree.AbstractTreeNodeBuilder;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNodeFilter;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeVisitor;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeAdapter;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.form.AbstractFormHandler;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractButton;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractRadioButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.labelfield.AbstractLabelField;
import org.eclipse.scout.rt.client.ui.form.fields.radiobuttongroup.AbstractRadioButtonGroup;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistTreeForm.MainBox.ActiveStateRadioButtonGroup;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistTreeForm.MainBox.NewButton;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistTreeForm.MainBox.ResultTreeField;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistTreeForm.MainBox.ResultTreeField.Tree;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistTreeForm.MainBox.StatusField;
import org.eclipse.scout.rt.client.ui.form.fields.treefield.AbstractTreeField;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCallFetcher;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;
import org.eclipse.scout.rt.shared.services.lookup.LookupRow;
import org.eclipse.scout.service.SERVICES;

public class ContentAssistTreeForm<LOOKUP_KEY> extends AbstractContentAssistFieldProposalForm<LOOKUP_KEY> {
  /**
   * Boolean marker on {@link Job#getProperty(QualifiedName)} that can be used to detect that the tree is loading some
   * nodes.
   * <p>
   * This can be used for example to avoid busy handling when a tree smart popup is loading its incremental tree data
   * (only relevant when {@link IContentAssistField#isBrowseLoadIncremental()}=true.
   */
  public static final QualifiedName JOB_PROPERTY_LOAD_TREE = new QualifiedName(ContentAssistTreeForm.class.getName(), "loadTree");

  private P_ActiveNodesFilter m_activeNodesFilter;
  private P_MatchingNodesFilter m_matchingNodesFilter;
  private boolean m_selectCurrentValueRequested;
  private boolean m_populateInitialTreeDone;
  private JobEx m_populateInitialTreeJob;

  public ContentAssistTreeForm(IContentAssistField<?, LOOKUP_KEY> contentAssistField, boolean allowCustomText) throws ProcessingException {
    super(contentAssistField, allowCustomText);
  }

  /*
   * Operations
   */

  /**
   * Populate initial tree using a {@link ClientAsyncJob}. Amount of tree loaded is depending on
   * {@link IContentAssistField#isBrowseLoadIncremental()}.
   * <p>
   * loadIncremnental only loads the roots, whereas !loadIncremental loads the complete tree. Normally the latter is
   * configured together with {@link IContentAssistField#isBrowseAutoExpandAll()}
   *
   * @throws ProcessingException
   */
  private void startPopulateInitialTree() throws ProcessingException {
    if (getContentAssistField().isBrowseLoadIncremental()) {
      //do sync
      getResultTreeField().loadRootNode();
      commitPopulateInitialTree(getResultTreeField().getTree());
      structureChanged(getResultTreeField());
    }
    else {
      //show comment that smartfield is loading
      getStatusField().setValue(ScoutTexts.get("searchingProposals"));
      getStatusField().setVisible(true);
      //go async to fetch data
      m_populateInitialTreeJob = getContentAssistField().callBrowseLookupInBackground(IContentAssistField.BROWSE_ALL_TEXT, 100000, TriState.UNDEFINED, new ILookupCallFetcher<LOOKUP_KEY>() {
        @Override
        public void dataFetched(List<? extends ILookupRow<LOOKUP_KEY>> rows, ProcessingException failed) {
          if (failed == null) {
            try {
              getStatusField().setVisible(false);
              List<ITreeNode> subTree = new P_TreeNodeBuilder().createTreeNodes(rows, ITreeNode.STATUS_NON_CHANGED, true);
              ITree tree = getResultTreeField().getTree();
              try {
                tree.setTreeChanging(true);
                //
                updateSubTree(tree, tree.getRootNode(), subTree);
                if (getContentAssistField().isBrowseAutoExpandAll()) {
                  tree.expandAll(getResultTreeField().getTree().getRootNode());
                }
                commitPopulateInitialTree(tree);
              }
              finally {
                tree.setTreeChanging(false);
              }
              structureChanged(getResultTreeField());
            }
            catch (ProcessingException pe) {
              failed = pe;
            }
          }
          if (failed != null) {
            getStatusField().setValue(TEXTS.get("RequestProblem"));
            getStatusField().setVisible(true);
            return;
          }
        }
      });
    }
  }

  /**
   * Called when the initial tree has been loaded and the form is therefore ready to accept
   * {@link #update(boolean, boolean)} requests.
   *
   * @throws ProcessingException
   */
  private void commitPopulateInitialTree(ITree tree) throws ProcessingException {
    updateActiveFilter();
    if (m_selectCurrentValueRequested) {
      if (tree.getSelectedNodeCount() == 0) {
        selectCurrentValueInternal();
      }
    }
    m_populateInitialTreeDone = true;
    getContentAssistField().doSearch(m_selectCurrentValueRequested, true);
  }

  @Override
  public void forceProposalSelection() throws ProcessingException {
    ITree tree = getResultTreeField().getTree();
    tree.selectNextNode();
  }

  @Override
  protected void execInitForm() throws ProcessingException {
    m_activeNodesFilter = new P_ActiveNodesFilter();
    m_matchingNodesFilter = new P_MatchingNodesFilter();
    getResultTreeField().getTree().setIconId(getContentAssistField().getBrowseIconId());
    getResultTreeField().getTree().addTreeListener(new TreeAdapter() {
      @Override
      public void treeChanged(TreeEvent e) {
        switch (e.getType()) {
          case TreeEvent.TYPE_NODE_EXPANDED:
          case TreeEvent.TYPE_NODE_COLLAPSED: {
            structureChanged(getResultTreeField());
            break;
          }
        }
      }
    });
  }

  /**
   * @return the pattern used to filter tree nodes based on the text typed into the smartfield
   */
  @ConfigOperation
  @Order(100)
  protected Pattern execCreatePatternForTreeFilter(String filterText) {
    // check pattern
    String s = filterText;
    if (s == null) {
      s = "";
    }
    s = s.toLowerCase();
    IDesktop desktop = ClientSyncJob.getCurrentSession().getDesktop();
    if (desktop != null && desktop.isAutoPrefixWildcardForTextSearch()) {
      s = "*" + s;
    }
    if (!s.endsWith("*")) {
      s = s + "*";
    }
    s = StringUtility.toRegExPattern(s);
    return Pattern.compile(s, Pattern.DOTALL);
  }

  /**
   * @return true if the node is accepted by the tree filter pattern defined in
   *         {@link #execCreatePatternForTreeFilter(String)}
   */
  @ConfigOperation
  @Order(110)
  protected boolean execAcceptNodeByTreeFilter(Pattern filterPattern, ITreeNode node, int level) {
    IContentAssistField<?, LOOKUP_KEY> sf = getContentAssistField();
    @SuppressWarnings("unchecked")
    ILookupRow<LOOKUP_KEY> row = (ILookupRow<LOOKUP_KEY>) node.getCell().getValue();
    if (node.isChildrenLoaded()) {
      if (row != null) {
        String q1 = node.getTree().getPathText(node, "\n");
        String q2 = node.getTree().getPathText(node, " ");
        if (q1 != null && q2 != null) {
          String[] path = (q1 + "\n" + q2).split("\n");
          for (String pathText : path) {
            if (pathText != null && filterPattern.matcher(pathText.toLowerCase()).matches()) {
              // use "level-1" because a tree smart field assumes its tree to
              // have multiple roots, but the ITree model is built as
              // single-root tree with invisible root node
              if (sf.acceptBrowseHierarchySelection(row.getKey(), level - 1, node.isLeaf())) {
                return true;
              }
            }
          }
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Override this method to change that behaviour of what is a single match.
   * <p>
   * By default a single match is when there is a single enabled LEAF node in the tree
   * <p>
   */
  @ConfigOperation
  @Order(120)
  protected ILookupRow<LOOKUP_KEY> execGetSingleMatch() {
    // when load incremental is set, don't visit the tree but use text-to-key
    // lookup method on smartfield.
    if (getContentAssistField().isBrowseLoadIncremental()) {
      try {
        List<? extends ILookupRow<LOOKUP_KEY>> rows = getContentAssistField().callTextLookup(getSearchText(), 2);
        if (rows != null && rows.size() == 1) {
          return rows.get(0);
        }
        else {
          return null;
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
        return null;
      }
    }
    else {
      final List<ILookupRow<LOOKUP_KEY>> foundLeafs = new ArrayList<ILookupRow<LOOKUP_KEY>>();
      ITreeVisitor v = new ITreeVisitor() {
        @Override
        public boolean visit(ITreeNode node) {
          if (node.isEnabled() && node.isLeaf()) {
            @SuppressWarnings("unchecked")
            ILookupRow<LOOKUP_KEY> row = (ILookupRow<LOOKUP_KEY>) node.getCell().getValue();
            if (row != null && row.isEnabled()) {
              foundLeafs.add(row);
            }
          }
          return foundLeafs.size() <= 2;
        }
      };
      getResultTreeField().getTree().visitVisibleTree(v);
      if (foundLeafs.size() == 1) {
        return foundLeafs.get(0);
      }
      else {
        return null;
      }
    }
  }

  protected void execResultTreeNodeClick(ITreeNode node) throws ProcessingException {
    doOk();
  }

  /**
   * Override this method to adapt the menu list of the result {@link Tree}.<br>
   * To change the order or specify the insert position use {@link IMenu#setOrder(double)}.
   */
  protected void injectResultTreeMenus(OrderedCollection<IMenu> menus) {
  }

  /*
   * Operations
   */

  @Override
  public void setTablePopulateStatus(IProcessingStatus status) {
//    getStatusField().setErrorStatus(status);
  }

  @Override
  protected void dataFetchedDelegateImpl(IContentAssistFieldDataFetchResult<LOOKUP_KEY> result, int maxCount) {
    String searchText = null;
    boolean selectCurrentValue = false;
    if (result != null) {
      selectCurrentValue = result.isSelectCurrentValue();
      searchText = result.getSearchText();
    }
    if (!m_populateInitialTreeDone) {
      m_selectCurrentValueRequested = selectCurrentValue;
      return;
    }
    ITree tree = getResultTreeField().getTree();
    try {
      tree.setTreeChanging(true);
      //
      m_matchingNodesFilter.update(searchText);
      tree.addNodeFilter(m_matchingNodesFilter);
    }
    finally {
      tree.setTreeChanging(false);
    }
    String statusText = null;
    getStatusField().setValue(statusText);
    getStatusField().setVisible(statusText != null);
    if (getNewButton().isEnabled()) {
      getNewButton().setVisible(execGetSingleMatch() == null);
    }
    structureChanged(getResultTreeField());
  }

  private void updateActiveFilter() {
    ITree tree = getResultTreeField().getTree();
    try {
      tree.setTreeChanging(true);
      //
      if (getContentAssistField().isActiveFilterEnabled()) {
        m_activeNodesFilter.update(getContentAssistField().getActiveFilter());
      }
      else {
        m_activeNodesFilter.update(TriState.TRUE);
      }
      tree.addNodeFilter(m_activeNodesFilter);
    }
    finally {
      tree.setTreeChanging(false);
    }
    structureChanged(getResultTreeField());
  }

  private void updateSubTree(ITree tree, final ITreeNode parentNode, List<ITreeNode> subTree) throws ProcessingException {
    if (tree == null || parentNode == null || subTree == null) {
      return;
    }
    tree.removeAllChildNodes(parentNode);
    tree.addChildNodes(parentNode, subTree);
  }

  @Override
  public ILookupRow<LOOKUP_KEY> getAcceptedProposal() throws ProcessingException {
    ILookupRow<LOOKUP_KEY> row = getSelectedLookupRow();
    if (row != null && row.isEnabled()) {
      return row;
    }
    else if (isAllowCustomText()) {
      return null;
    }
    else {
      return execGetSingleMatch();
    }
  }

  @SuppressWarnings("unchecked")
  public ILookupRow<LOOKUP_KEY> getSelectedLookupRow() {
    ILookupRow<LOOKUP_KEY> row = null;
    ITree tree = getResultTreeField().getTree();
    ITreeNode node = null;
    if (tree.isCheckable()) {
      Collection<ITreeNode> checkedNodes = tree.getCheckedNodes();
      if (CollectionUtility.hasElements(checkedNodes)) {
        node = CollectionUtility.firstElement(checkedNodes);
      }
    }
    else {
      node = tree.getSelectedNode();
    }
    if (node != null && node.isFilterAccepted() && node.isEnabled()) {
      row = (ILookupRow<LOOKUP_KEY>) node.getCell().getValue();
    }

    return row;
  }

  /*
   * Dialog start
   */
  @Override
  public void startForm() throws ProcessingException {
    startInternal(new FormHandler());
  }

  @SuppressWarnings("unchecked")
  public MainBox getMainBox() {
    return (MainBox) getRootGroupBox();
  }

  public ResultTreeField getResultTreeField() {
    return getFieldByClass(ResultTreeField.class);
  }

  public ActiveStateRadioButtonGroup getActiveStateRadioButtonGroup() {
    return getFieldByClass(ActiveStateRadioButtonGroup.class);
  }

  /*
   * Fields
   */
  public StatusField getStatusField() {
    return getFieldByClass(StatusField.class);
  }

  public NewButton getNewButton() {
    return getFieldByClass(NewButton.class);
  }

  private boolean selectCurrentValueInternal() throws ProcessingException {
    final LOOKUP_KEY selectedKey = getContentAssistField().getValueAsLookupKey();
    if (selectedKey != null) {
      //check existing tree
      ITree tree = getResultTreeField().getTree();
      final ArrayList<ITreeNode> matchingNodes = new ArrayList<ITreeNode>();
      tree.visitTree(new ITreeVisitor() {
        @Override
        public boolean visit(ITreeNode node) {
          Object val = node.getCell().getValue();
          if (val instanceof ILookupRow && CompareUtility.equals(selectedKey, ((ILookupRow) val).getKey())) {
            matchingNodes.add(node);
          }
          return true;
        }
      });
      if (matchingNodes.size() > 0) {
        selectValue(tree, matchingNodes.get(0));

        //ticket 87030
        for (int i = 1; i < matchingNodes.size(); i++) {
          ITreeNode node = matchingNodes.get(i);
          tree.setNodeExpanded(node, true);
          tree.ensureVisible(matchingNodes.get(i));
        }
        return true;
      }
      else {
        //load tree
        ITreeNode node = loadNodeWithKey(selectedKey);
        if (node != null) {
          selectValue(tree, node);
          return true;
        }
      }
    }
    return false;
  }

  private void selectValue(ITree tree, ITreeNode node) {
    if (tree == null || node == null) {
      return;
    }

    tree.selectNode(node);

    if (tree.isCheckable()) {
      tree.setNodeChecked(node, true);
    }
  }

  private ITreeNode loadNodeWithKey(LOOKUP_KEY key) throws ProcessingException {
    ArrayList<ILookupRow<LOOKUP_KEY>> path = new ArrayList<ILookupRow<LOOKUP_KEY>>();
    LOOKUP_KEY t = key;
    while (t != null) {
      ILookupRow<LOOKUP_KEY> row = getLookupRowFor(t);
      if (row != null) {
        path.add(0, row);
        t = row.getParentKey();
      }
      else {
        t = null;
      }
    }
    ITree tree = getResultTreeField().getTree();
    ITreeNode parentNode = tree.getRootNode();
    for (int i = 0; i < path.size() && parentNode != null; i++) {
      parentNode.ensureChildrenLoaded();
      parentNode.setExpanded(true);
      Object childKey = path.get(i).getKey();
      ITreeNode nextNode = null;
      for (ITreeNode n : parentNode.getChildNodes()) {
        if (n.getCell().getValue() instanceof ILookupRow) {
          if (CompareUtility.equals(((ILookupRow) n.getCell().getValue()).getKey(), childKey)) {
            nextNode = n;
            break;
          }
        }
      }
      parentNode = nextNode;
    }
    //
    return parentNode;
  }

  private ILookupRow<LOOKUP_KEY> getLookupRowFor(LOOKUP_KEY key) throws ProcessingException {
    if (key instanceof Number && ((Number) key).longValue() == 0) {
      key = null;
    }
    if (key != null) {
      IContentAssistField<?, LOOKUP_KEY> sf = (IContentAssistField<?, LOOKUP_KEY>) getContentAssistField();
      for (ILookupRow<LOOKUP_KEY> row : sf.callKeyLookup(key)) {
        return row;
      }
    }
    return null;
  }

  public class MainBox extends AbstractGroupBox {

    @Override
    protected int getConfiguredGridColumnCount() {
      return 1;
    }

    @Override
    protected boolean getConfiguredGridUseUiWidth() {
      return true;
    }

    @Override
    protected boolean getConfiguredGridUseUiHeight() {
      return true;
    }

    @Order(10)
    public class ResultTreeField extends AbstractTreeField {

      public ResultTreeField() {
        super();
      }

      @Override
      protected boolean getConfiguredAutoLoad() {
        return false;
      }

      @Override
      protected double getConfiguredGridWeightY() {
        return 1;
      }

      @Override
      protected boolean getConfiguredGridUseUiWidth() {
        return true;
      }

      @Override
      protected boolean getConfiguredGridUseUiHeight() {
        return true;
      }

      @Override
      protected boolean getConfiguredLabelVisible() {
        return false;
      }

      @SuppressWarnings("unchecked")
      @Override
      protected void execLoadChildNodes(ITreeNode parentNode) throws ProcessingException {
        IContentAssistField<?, LOOKUP_KEY> contentAssistField = getContentAssistField();
        if (contentAssistField.isBrowseLoadIncremental()) {
          Job currentJob = Job.getJobManager().currentJob();
          //show loading status
          boolean statusWasVisible = getStatusField().isVisible();
          getStatusField().setValue(ScoutTexts.get("searchingProposals"));
          getStatusField().setVisible(true);
          try {
            currentJob.setProperty(JOB_PROPERTY_LOAD_TREE, Boolean.TRUE);
            //load node
            ILookupRow<LOOKUP_KEY> b = (LookupRow) (parentNode != null ? parentNode.getCell().getValue() : null);
            List<? extends ILookupRow<LOOKUP_KEY>> data = contentAssistField.callSubTreeLookup(b != null ? b.getKey() : null, TriState.UNDEFINED);
            List<ITreeNode> subTree = new P_TreeNodeBuilder().createTreeNodes(data, ITreeNode.STATUS_NON_CHANGED, false);
            updateSubTree(getTree(), parentNode, subTree);
          }
          finally {
            currentJob.setProperty(JOB_PROPERTY_LOAD_TREE, null);
          }
          //hide loading status
          getStatusField().setVisible(statusWasVisible);
        }
        /*
        else {
          //nop, since complete tree is already loaded (via async job)
        }
         */
      }

      /*
       * inner table
       */
      @Order(4)
      public class Tree extends AbstractTree {

        @Override
        protected void injectMenusInternal(OrderedCollection<IMenu> menus) {
          injectResultTreeMenus(menus);
        }

        @Override
        protected boolean getConfiguredMultiSelect() {
          return false;
        }

        @Override
        protected boolean getConfiguredMultiCheck() {
          return false;
        }

        @Override
        protected boolean getConfiguredRootNodeVisible() {
          return false;
        }

        @Override
        protected boolean getConfiguredScrollToSelection() {
          return true;
        }

        @Override
        protected void execNodeClick(ITreeNode node, MouseButton mouseButton) throws ProcessingException {
          execResultTreeNodeClick(node);
        }
      }
    }

    @Override
    protected boolean getConfiguredBorderVisible() {
      return false;
    }

    @Order(20)
    public class ActiveStateRadioButtonGroup extends AbstractRadioButtonGroup<TriState> {

      @Override
      protected boolean getConfiguredLabelVisible() {
        return false;
      }

      @Override
      protected void execChangedValue() throws ProcessingException {
        if (isVisible() && !isFormLoading()) {
          getContentAssistField().setActiveFilter(getValue());
          updateActiveFilter();
        }
      }

      @Order(1)
      public class ActiveButton extends AbstractRadioButton<TriState> {

        @Override
        protected String getConfiguredLabel() {
          return ScoutTexts.get("ActiveStates");
        }

        @Override
        protected TriState getConfiguredRadioValue() {
          return TriState.TRUE;
        }
      }

      @Order(2)
      public class InactiveButton extends AbstractRadioButton<TriState> {

        @Override
        protected String getConfiguredLabel() {
          return ScoutTexts.get("InactiveStates");
        }

        @Override
        protected TriState getConfiguredRadioValue() {
          return TriState.FALSE;
        }
      }

      @Order(3)
      public class ActiveAndInactiveButton extends AbstractRadioButton<TriState> {

        @Override
        protected String getConfiguredLabel() {
          return ScoutTexts.get("ActiveAndInactiveStates");
        }

        @Override
        protected TriState getConfiguredRadioValue() {
          return TriState.UNDEFINED;
        }
      }
    }

    @Order(25)
    public class NewButton extends AbstractButton {

      @Override
      protected boolean getConfiguredVisible() {
        return false;
      }

      @Override
      protected boolean getConfiguredEnabled() {
        return false;
      }

      @Override
      protected boolean getConfiguredLabelVisible() {
        return false;
      }

      @Override
      protected boolean getConfiguredFillHorizontal() {
        return false;
      }

      @Override
      protected int getConfiguredDisplayStyle() {
        return DISPLAY_STYLE_LINK;
      }

      @Override
      protected boolean getConfiguredProcessButton() {
        return false;
      }

      @Override
      protected void execClickAction() throws ProcessingException {
        getContentAssistField().doBrowseNew(getSearchText());
      }
    }// end field

    @Order(30)
    public class StatusField extends AbstractLabelField {
      @Override
      protected boolean getConfiguredLabelVisible() {
        return false;
      }

      @Override
      protected double getConfiguredGridWeightY() {
        return 1;
      }

    }// end field

  }// end main box

  private class P_ActiveNodesFilter implements ITreeNodeFilter {
    private TriState m_ts;

    public P_ActiveNodesFilter() {
    }

    public void update(TriState ts) {
      m_ts = ts;
    }

    @Override
    public boolean accept(ITreeNode node, int level) {
      if (m_ts.isUndefined()) {
        return true;
      }
      else {
        ILookupRow row = (LookupRow) node.getCell().getValue();
        if (row != null) {
          return row.isActive() == m_ts.equals(TriState.TRUE);
        }
        else {
          return true;
        }
      }
    }
  }

  private class P_MatchingNodesFilter implements ITreeNodeFilter {
    private Pattern m_searchPattern;

    public P_MatchingNodesFilter() {
    }

    public void update(String text) {
      m_searchPattern = execCreatePatternForTreeFilter(text);
    }

    @Override
    public boolean accept(ITreeNode node, int level) {
      return execAcceptNodeByTreeFilter(m_searchPattern, node, level);
    }
  }

  private class P_TreeNodeBuilder extends AbstractTreeNodeBuilder<LOOKUP_KEY> {
    @Override
    protected ITreeNode createEmptyTreeNode() throws ProcessingException {
      ITree tree = getResultTreeField().getTree();
      ITreeNode node = getResultTreeField().createTreeNode();
      if (tree.getIconId() != null) {
        Cell cell = node.getCellForUpdate();
        cell.setIconId(tree.getIconId());
      }
      return node;
    }
  }

  /*
   * handlers
   */
  private class FormHandler extends AbstractFormHandler {

    @SuppressWarnings("unchecked")
    @Override
    protected void execLoad() throws ProcessingException {
      getActiveStateRadioButtonGroup().setVisible(getContentAssistField().isActiveFilterEnabled());
      getActiveStateRadioButtonGroup().setValue(getContentAssistField().getActiveFilter());
      getNewButton().setEnabled(getContentAssistField().getBrowseNewText() != null);
      getNewButton().setLabel(getContentAssistField().getBrowseNewText());
      startPopulateInitialTree();
    }

    @Override
    protected boolean execValidate() throws ProcessingException {
      return getAcceptedProposal() != null;
    }

    @Override
    protected void execFinally() throws ProcessingException {
      if (m_populateInitialTreeJob != null) {
        m_populateInitialTreeJob.cancel();
      }
    }

  }
}
