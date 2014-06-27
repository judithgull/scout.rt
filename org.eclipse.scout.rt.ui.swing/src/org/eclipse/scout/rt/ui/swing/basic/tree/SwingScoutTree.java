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
package org.eclipse.scout.rt.ui.swing.basic.tree;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientJob;
import org.eclipse.scout.rt.client.ui.IEventHistory;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.IActionFilter;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.TreeMenuType;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeListener;
import org.eclipse.scout.rt.ui.swing.SwingPopupWorker;
import org.eclipse.scout.rt.ui.swing.SwingUtility;
import org.eclipse.scout.rt.ui.swing.action.SwingScoutAction;
import org.eclipse.scout.rt.ui.swing.basic.SwingLinkDetectorMouseMotionListener;
import org.eclipse.scout.rt.ui.swing.basic.SwingScoutComposite;
import org.eclipse.scout.rt.ui.swing.dnd.TransferHandlerEx;
import org.eclipse.scout.rt.ui.swing.ext.JScrollPaneEx;
import org.eclipse.scout.rt.ui.swing.ext.JTreeEx;
import org.eclipse.scout.rt.ui.swing.ext.MouseClickedBugFix;

/**
 * Implementation of the Scout tree in Swing.
 */
public class SwingScoutTree extends SwingScoutComposite<ITree> implements ISwingScoutTree {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(SwingScoutTree.class);

  private P_ScoutTreeListener m_scoutTreeListener;
  private JScrollPane m_swingScrollPane;
  // cache
  private List<IKeyStroke> m_installedScoutKs;

  public SwingScoutTree() {
  }

  @Override
  protected void initializeSwing() {
    JTreeEx tree = new JTreeEx();
    m_swingScrollPane = new JScrollPaneEx(tree);
    m_swingScrollPane.setBackground(tree.getBackground());
    setSwingField(tree);
    // swing properties
    tree.setDragEnabled(true);
    // models
    tree.setModel(new SwingTreeModel(this));
    tree.setSelectionModel(new DefaultTreeSelectionModel());
    // renderers
    tree.setCellRenderer(new SwingTreeCellRenderer(getSwingEnvironment(), tree.getCellRenderer(), this));
    // listeners
    tree.addMouseMotionListener(new SwingLinkDetectorMouseMotionListener<JTree>(new TreeHtmlLinkDetector()));
    tree.addMouseListener(new P_SwingMouseListener());
    tree.addTreeSelectionListener(new P_SwingSelectionListener());
    tree.addTreeExpansionListener(new P_SwingExpansionListener());
    // attach drag and remove default transfer handler
    P_SwingDragAndDropTransferHandler th = new P_SwingDragAndDropTransferHandler();
    tree.setTransferHandler(th);
    try {
      tree.getDropTarget().addDropTargetListener(new P_DropTargetListener());
    }
    catch (TooManyListenersException e1) {
      LOG.error(e1.getMessage());
    }
    //ticket 87030, bug 365161
    //attach delayed resize: make selection visible
    m_swingScrollPane.addComponentListener(new ComponentAdapter() {
      private int m_oldHeight = -1;

      @Override
      public void componentResized(ComponentEvent e) {
        int newHeight = e.getComponent().getHeight();
        if (m_oldHeight >= 0 && m_oldHeight == newHeight) {
          return;
        }
        m_oldHeight = newHeight;
        ITree t = getScoutObject();
        if (t != null && t.isScrollToSelection()) {
          if (e.getComponent().isShowing()) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                scrollToSelection();
              }
            });
          }
        }
      }
    });
    //add context menu key stroke
    tree.getInputMap(JComponent.WHEN_FOCUSED).put(SwingUtility.createKeystroke("CONTEXT_MENU"), "contextMenu");
    tree.getActionMap().put("contextMenu", new AbstractAction() {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e) {
        if (getUpdateSwingFromScoutLock().isAcquired()) {
          return;
        }
        //
        if (getScoutObject() != null) {
          TreePath selectedPath = getSwingTree().getSelectionPath();
          if (selectedPath != null) {
            final Point p = getSwingTree().getPathBounds(selectedPath).getLocation();
            final Component source = getSwingTree();
            p.translate(2, 2);
            // notify Scout
            Runnable t = new Runnable() {
              @Override
              public void run() {
                // call swing menu
                new SwingPopupWorker(getSwingEnvironment(), source, p, getScoutObject().getContextMenu()).enqueue();
              }
            };
            getSwingEnvironment().invokeScoutLater(t, 5678);
            // end notify
          }
        }
      }
    });
  }

  @Override
  public JTreeEx getSwingTree() {
    return (JTreeEx) getSwingField();
  }

  protected TreeSelectionModel getSwingTreeSelectionModel() {
    return getSwingTree().getSelectionModel();
  }

  @Override
  public JScrollPane getSwingScrollPane() {
    return m_swingScrollPane;
  }

  @Override
  protected void detachScout() {
    super.detachScout();
    if (getScoutObject() == null) {
      return;
    }
    saveScrollbarValues();
    if (m_scoutTreeListener != null) {
      getScoutObject().removeTreeListener(m_scoutTreeListener);
      m_scoutTreeListener = null;
    }
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    if (getScoutObject() == null) {
      return;
    }
    if (m_scoutTreeListener == null) {
      m_scoutTreeListener = new P_ScoutTreeListener();
      getScoutObject().addUITreeListener(m_scoutTreeListener);
    }
    setMultiSelectFromScout(getScoutObject().isMultiSelect());
    setRootNodeVisibleFromScout();
    setRootHandlesVisibleFromScout();
    setExpansionFromScout(getScoutObject().getRootNode());
    setSelectionFromScout(getScoutObject().getSelectedNodes());
    setKeyStrokesFromScout();
    // add checkable key mappings
    if (getScoutObject().isCheckable()) {
      getSwingTree().getInputMap(JComponent.WHEN_FOCUSED).put(SwingUtility.createKeystroke("SPACE"), "toggleRow");
      getSwingTree().getActionMap().put("toggleRow", new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
          handleSwingNodeClick(getSwingTree().getSelectionPath());
        }
      });
    }
    //handle events from recent history
    final IEventHistory<TreeEvent> h = getScoutObject().getEventHistory();
    if (h != null) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          for (TreeEvent e : h.getRecentEvents()) {
            handleScoutTreeEventInSwing(e);
          }
        }
      });
    }
    restoreScrollbarValues();
  }

  /*
   * scout settings
   */

  protected void setMultiSelectFromScout(boolean on) {
    if (on) {
      getSwingTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }
    else {
      getSwingTree().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }
  }

  protected void setExpansionFromScout(ITreeNode scoutNode) {
    setExpansionFromScoutRec(scoutNode);
    revertSelectionFromScout();
    // ensure that the lead path is set
    if (getSwingTree().getLeadSelectionPath() == null) {
      TreeModel swingModel = getSwingTree().getModel();
      Object leadNode = null;
      if (getSwingTree().isRootVisible()) {
        leadNode = swingModel.getRoot();
      }
      else {
        Object root = swingModel.getRoot();
        if (root != null) {
          leadNode = swingModel.getChild(root, 0);
        }
      }
      if (leadNode != null) {
        getSwingTree().setLeadSelectionPath(scoutNodeToTreePath((ITreeNode) leadNode));
      }
    }
  }

  private void setExpansionFromScoutRec(ITreeNode scoutNode) {
    boolean expanded;
    if (scoutNode.getParentNode() == null) {
      expanded = true;
    }
    else {
      expanded = scoutNode.isExpanded();
    }
    //
    TreePath path = scoutNodeToTreePath(scoutNode);
    if (expanded) {
      getSwingTree().expandPath(path);
    }
    else {
      if (getSwingTree().isExpanded(path)) {
        getSwingTree().collapsePath(path);
      }
    }
    // children only if node was expanded
    if (expanded) {
      List<ITreeNode> childs = scoutNode.getFilteredChildNodes();
      for (ITreeNode node : childs) {
        setExpansionFromScoutRec(node);
      }
    }
  }

  protected void setExpansionFromSwing(TreePath path, final boolean b) {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (getScoutObject() != null) {
      final ITreeNode scoutNode = treePathToScoutNode(path);
      // notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          getScoutObject().getUIFacade().setNodeExpandedFromUI(scoutNode, b);
        }
      };

      getSwingEnvironment().invokeScoutLater(t, 0);
      // end notify
    }
  }

  protected void setRootNodeVisibleFromScout() {
    getSwingTree().setRootVisible(getScoutObject().isRootNodeVisible());
    getSwingTree().repaint();
  }

  protected void setRootHandlesVisibleFromScout() {
    getSwingTree().setShowsRootHandles(getScoutObject().isRootHandlesVisible());
    getSwingTree().repaint();
  }

  protected void setSelectionFromScout(Collection<ITreeNode> newScoutNodes) {
    List<ITreeNode> oldScoutNodes = treePathsToScoutNodes(CollectionUtility.arrayList(getSwingTree().getSelectionPaths()));
    if (!CollectionUtility.equalsCollection(oldScoutNodes, newScoutNodes)) {
      List<TreePath> paths = scoutNodesToTreePaths(newScoutNodes);
      TreePath anchorPath = getSwingTree().getAnchorSelectionPath();
      TreePath leadPath = getSwingTree().getLeadSelectionPath();

      getSwingTree().setSelectionPaths(paths.toArray(new TreePath[paths.size()]));

      // restore anchor path if its last segment is still valid
      ITreeNode anchorScoutNode = treePathToScoutNode(anchorPath);
      if (anchorScoutNode != null && anchorScoutNode.getTree() != null
          && anchorScoutNode.isVisible() && anchorScoutNode.isFilterAccepted()) {
        getSwingTree().setAnchorSelectionPath(anchorPath);
      }

      // restore lead path if its last segment is still valid
      ITreeNode leadScoutNode = treePathToScoutNode(leadPath);
      if (leadScoutNode != null && leadScoutNode.getTree() != null
          && leadScoutNode.isVisible() && leadScoutNode.isFilterAccepted()) {
        getSwingTree().setLeadSelectionPath(leadPath);
      }
    }
  }

  protected void revertSelectionFromScout() {
    Collection<ITreeNode> newScoutNodes = getScoutObject().getSelectedNodes();
    List<ITreeNode> oldScoutNodes = treePathsToScoutNodes(CollectionUtility.arrayList(getSwingTree().getSelectionPaths()));
    if (!CollectionUtility.equalsCollection(oldScoutNodes, newScoutNodes)) {
      List<TreePath> paths = scoutNodesToTreePaths(newScoutNodes);
      getSwingTree().setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
    }
  }

  protected void setKeyStrokesFromScout() {
    JComponent component = getSwingContainer();
    if (component == null) {
      component = getSwingField();
    }
    if (component != null) {
      // remove old key strokes
      if (m_installedScoutKs != null) {
        for (IKeyStroke scoutKs : m_installedScoutKs) {
          KeyStroke swingKs = SwingUtility.createKeystroke(scoutKs);
          //
          InputMap imap = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
          imap.remove(swingKs);
          ActionMap amap = component.getActionMap();
          amap.remove(scoutKs.getActionId());
        }
      }
      m_installedScoutKs = null;
      // add new key strokes
      List<IKeyStroke> scoutKeyStrokes = getScoutObject().getKeyStrokes();
      for (IKeyStroke scoutKs : scoutKeyStrokes) {
        int swingWhen = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        KeyStroke swingKs = SwingUtility.createKeystroke(scoutKs);
        SwingScoutAction<IAction> action = new SwingScoutAction<IAction>();
        action.createField(scoutKs, getSwingEnvironment());
        //
        InputMap imap = component.getInputMap(swingWhen);
        imap.put(swingKs, scoutKs.getActionId());
        ActionMap amap = component.getActionMap();
        amap.put(scoutKs.getActionId(), action.getSwingAction());
      }
      m_installedScoutKs = scoutKeyStrokes;
    }
  }

  /**
   * Fires changes of selection as well as changes on lead/anchor indices
   */
  protected void setSelectionFromSwing(TreePath[] paths) {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (getScoutObject() != null) {
      if (paths != null && paths.length > 0) {
        final List<ITreeNode> scoutNodes = treePathsToScoutNodes(CollectionUtility.arrayList(paths));
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            try {
              addIgnoredScoutEvent(TreeEvent.class, "" + TreeEvent.TYPE_NODES_SELECTED);
              //
              getScoutObject().getUIFacade().setNodesSelectedFromUI(scoutNodes);
            }
            finally {
              removeIgnoredScoutEvent(TreeEvent.class, "" + TreeEvent.TYPE_NODES_SELECTED);
            }
          }
        };
        getSwingEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
      else {
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            try {
              addIgnoredScoutEvent(TreeEvent.class, "" + TreeEvent.TYPE_NODES_SELECTED);
              //
              getScoutObject().getUIFacade().setNodesSelectedFromUI(null);
            }
            finally {
              removeIgnoredScoutEvent(TreeEvent.class, "" + TreeEvent.TYPE_NODES_SELECTED);
            }
          }
        };
        getSwingEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
  }

  protected void setScrollToSelectionFromScout() {
    if (getScoutObject().isScrollToSelection()) {
      scrollToSelection();
    }
  }

  /**
   * @rn imo, 05.03.2009, tickets #73324, #73707, #74018
   * @rn imo, 18.11.2009, ticket #83255
   */
  protected void scrollToSelection() {
    int[] selectedRows = getSwingTree().getSelectionRows();
    if (selectedRows != null && selectedRows.length > 0) {
      int index = selectedRows[0];
      int rowCount = getSwingTree().getRowCount();
      if (index >= 0 && index < rowCount) {
        TreePath selectedPath = getSwingTree().getPathForRow(index);
        int nextIndex = index;
        while (nextIndex + 1 < rowCount) {
          TreePath path = getSwingTree().getPathForRow(nextIndex + 1);
          if (path.getPathCount() > selectedPath.getPathCount()) {
            nextIndex = nextIndex + 1;
          }
          else {
            break;
          }
        }
        if (nextIndex != index) {
          getSwingTree().scrollRowToVisible(nextIndex);
        }
        getSwingTree().scrollRowToVisible(index);
      }
    }
  }

  /**
   * Saves the coordinates of the vertical and horizontal scrollbars to the {@link ClientSession} if the tree's model
   * method {@code isSaveAndRestoreScrollbars()} returns {@code true}.
   */
  protected void saveScrollbarValues() {
    if (getScoutObject() == null || !getScoutObject().isSaveAndRestoreScrollbars()) {
      return;
    }

    final int verticalValue = m_swingScrollPane.getVerticalScrollBar() != null ? m_swingScrollPane.getVerticalScrollBar().getValue() : 0;
    final int horizontalValue = m_swingScrollPane.getHorizontalScrollBar() != null ? m_swingScrollPane.getHorizontalScrollBar().getValue() : 0;

    // save values in Scout ClientSession
    Runnable t = new Runnable() {
      @Override
      public void run() {
        ClientJob.getCurrentSession().setData(Scrollbar.VERTICAL.getType() + "_" + getScoutObject().toString(), verticalValue);
        ClientJob.getCurrentSession().setData(Scrollbar.HORIZONTAL.getType() + "_" + getScoutObject().toString(), horizontalValue);
      }
    };

    getSwingEnvironment().invokeScoutLater(t, 1234);
  }

  /**
   * Restores the coordinates of the vertical and horizontal scrollbars if available in the {@link ClientSession} if the
   * tree's model method {@code isSaveAndRestoreScrollbars()} return {@code true}.
   */
  protected void restoreScrollbarValues() {
    if (getScoutObject() == null || !getScoutObject().isSaveAndRestoreScrollbars()) {
      return;
    }

    final AtomicReference<ScrollbarValues> scrollbarValues = new AtomicReference<ScrollbarValues>();

    // restore Scrollbar values from Scout
    Runnable t = new Runnable() {
      @Override
      public void run() {
        Integer verticalValue = (Integer) ClientJob.getCurrentSession().getData(Scrollbar.VERTICAL.getType() + "_" + getScoutObject().toString());
        Integer horizontalValue = (Integer) ClientJob.getCurrentSession().getData(Scrollbar.HORIZONTAL.getType() + "_" + getScoutObject().toString());

        if (horizontalValue != null || verticalValue != null) {
          scrollbarValues.set(new ScrollbarValues(horizontalValue, verticalValue));
        }
      }
    };

    try {
      getSwingEnvironment().invokeScoutLater(t, 1234).join(1234);
    }
    catch (InterruptedException e) {
      LOG.debug("exception occured while joining on model thread: " + e);
    }
    if (scrollbarValues.get() != null) {
      /*
       * Since the scrollbars are relative to the selection before, we need to scroll to the selected
       * tree node before restoring the scrollbars.
       */
      scrollToSelection();

      Integer horizontal = scrollbarValues.get().horizontal;
      Integer vertical = scrollbarValues.get().vertical;

      JScrollBar horizontalScrollBar = m_swingScrollPane.getHorizontalScrollBar();
      if (horizontal != null && horizontalScrollBar != null && horizontalScrollBar.isVisible()) {
        horizontalScrollBar.setValue(horizontal);
      }

      JScrollBar verticalScrollBar = m_swingScrollPane.getVerticalScrollBar();
      if (vertical != null && verticalScrollBar != null && verticalScrollBar.isVisible()) {
        verticalScrollBar.setValue(vertical);
      }
    }
  }

  /**
   * scout property observer
   */
  @Override
  protected void handleScoutPropertyChange(String propName, Object newValue) {
    if (propName.equals(ITree.PROP_MULTI_SELECT)) {
      setMultiSelectFromScout(((Boolean) newValue).booleanValue());
    }
    else if (propName.equals(ITree.PROP_ROOT_NODE_VISIBLE)) {
      setRootNodeVisibleFromScout();
    }
    else if (propName.equals(ITree.PROP_ROOT_HANDLES_VISIBLE)) {
      setRootHandlesVisibleFromScout();
    }
    else if (propName.equals(ITree.PROP_KEY_STROKES)) {
      setKeyStrokesFromScout();
    }
    else if (propName.equals(ITree.PROP_SCROLL_TO_SELECTION)) {
      setScrollToSelectionFromScout();
    }
  }

  /**
   * scout table observer
   */
  protected boolean isHandleScoutTreeEvent(List<? extends TreeEvent> events) {
    for (TreeEvent event : events) {
      switch (event.getType()) {
        case TreeEvent.TYPE_REQUEST_FOCUS:
        case TreeEvent.TYPE_NODE_EXPANDED:
        case TreeEvent.TYPE_NODE_COLLAPSED:
        case TreeEvent.TYPE_NODES_INSERTED:
        case TreeEvent.TYPE_NODES_UPDATED:
        case TreeEvent.TYPE_NODE_CHANGED:
        case TreeEvent.TYPE_NODES_DELETED:
        case TreeEvent.TYPE_NODE_FILTER_CHANGED:
        case TreeEvent.TYPE_NODES_SELECTED:
        case TreeEvent.TYPE_CHILD_NODE_ORDER_CHANGED:
        case TreeEvent.TYPE_SCROLL_TO_SELECTION: {
          return true;
        }
      }
    }
    return false;
  }

  protected void handleScoutTreeEventInSwing(TreeEvent e) {
    switch (e.getType()) {
      case TreeEvent.TYPE_NODES_INSERTED: {
        updateTreeStructureAndKeepSelectionFromScout(e.getCommonParentNode());
        setExpansionFromScout(e.getCommonParentNode());
        break;
      }
      case TreeEvent.TYPE_NODES_UPDATED: {
        updateTreeStructureAndKeepSelectionFromScout(e.getCommonParentNode());
        setExpansionFromScout(e.getCommonParentNode());
        break;
      }
      case TreeEvent.TYPE_NODE_CHANGED: {
        updateTreeNode(e.getNode());
        break;
      }
      case TreeEvent.TYPE_NODES_DELETED: {
        updateTreeStructureAndKeepSelectionFromScout(e.getCommonParentNode());
        setExpansionFromScout(e.getCommonParentNode());
        break;
      }
      case TreeEvent.TYPE_CHILD_NODE_ORDER_CHANGED: {
        updateTreeStructureAndKeepSelectionFromScout(e.getCommonParentNode());
        setExpansionFromScout(e.getCommonParentNode());
        break;
      }
      case TreeEvent.TYPE_NODE_FILTER_CHANGED: {
        updateTreeStructureAndKeepSelectionFromScout(getScoutObject().getRootNode());
        setExpansionFromScout(getScoutObject().getRootNode());
        break;
      }
      case TreeEvent.TYPE_REQUEST_FOCUS: {
        getSwingTree().requestFocus();
        break;
      }
      case TreeEvent.TYPE_NODE_EXPANDED:
      case TreeEvent.TYPE_NODE_COLLAPSED: {
        setExpansionFromScout(e.getNode());
        break;
      }
      case TreeEvent.TYPE_NODES_SELECTED: {
        setSelectionFromScout(e.getNodes());
        break;
      }
      case TreeEvent.TYPE_SCROLL_TO_SELECTION: {
        scrollToSelection();
        break;
      }
    }
  }

  /**
   * bsi ticket 95090: avoid multiple and excessive tree structure updates
   */
  protected void handleScoutTreeEventBatchInSwing(List<TreeEvent> eventList) {
    //phase 1: collect all parent nodes that need to be refreshed and refresh once per node
    HashSet<ITreeNode> processedParentNodes = new HashSet<ITreeNode>();
    for (TreeEvent e : eventList) {
      ITreeNode parentNode = null;
      switch (e.getType()) {
        case TreeEvent.TYPE_NODES_INSERTED:
        case TreeEvent.TYPE_NODES_UPDATED:
        case TreeEvent.TYPE_NODES_DELETED:
        case TreeEvent.TYPE_CHILD_NODE_ORDER_CHANGED: {
          parentNode = e.getCommonParentNode();
          break;
        }
        case TreeEvent.TYPE_NODE_FILTER_CHANGED: {
          parentNode = getScoutObject().getRootNode();
          break;
        }
      }
      if (parentNode != null) {
        if (!processedParentNodes.contains(parentNode)) {
          processedParentNodes.add(parentNode);
          updateTreeStructureAndKeepSelectionFromScout(parentNode);
          setExpansionFromScout(parentNode);
        }
      }
    }
    //phase 2: apply remaining events
    for (TreeEvent e : eventList) {
      switch (e.getType()) {
        case TreeEvent.TYPE_REQUEST_FOCUS: {
          getSwingTree().requestFocus();
          break;
        }
        case TreeEvent.TYPE_NODE_EXPANDED:
        case TreeEvent.TYPE_NODE_COLLAPSED: {
          setExpansionFromScout(e.getNode());
          break;
        }
        case TreeEvent.TYPE_NODES_SELECTED: {
          setSelectionFromScout(e.getNodes());
          break;
        }
        case TreeEvent.TYPE_SCROLL_TO_SELECTION: {
          scrollToSelection();
          break;
        }
        case TreeEvent.TYPE_NODE_CHANGED: {
          updateTreeNode(e.getNode());
          break;
        }
      }
    }
  }

  /**
   * update the given node
   * 
   * @since 3.10.0-M5
   */
  protected void updateTreeNode(ITreeNode node) {
    if (getScoutObject() != null) {
      SwingTreeModel swingTreeModel = (SwingTreeModel) getSwingTree().getModel();
      swingTreeModel.updateNode(node);
    }
  }

  private void updateTreeStructureAndKeepSelectionFromScout(ITreeNode node) {
    if (getScoutObject() != null) {
      SwingTreeModel swingTreeModel = (SwingTreeModel) getSwingTree().getModel();
      swingTreeModel.fireStructureChanged(node);
      List<TreePath> paths = scoutNodesToTreePaths(getScoutObject().getSelectedNodes());
      getSwingTree().setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
    }
  }

  protected Transferable handleSwingDragRequest() {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return null;
    }
    //
    final Holder<TransferObject> result = new Holder<TransferObject>(TransferObject.class, null);
    if (getScoutObject() != null) {
      // notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          TransferObject scoutTransferable = getScoutObject().getUIFacade().fireNodesDragRequestFromUI();
          result.setValue(scoutTransferable);
        }
      };
      try {
        getSwingEnvironment().invokeScoutLater(t, 20000).join(20000);
      }
      catch (InterruptedException e) {
        //nop
      }
      // end notify
    }
    TransferObject scoutTransferable = result.getValue();
    Transferable swingTransferable = SwingUtility.createSwingTransferable(scoutTransferable);
    return swingTransferable;
  }

  protected void handleSwingDropAction(TreePath path, Transferable swingTransferable) {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (swingTransferable != null) {
      if (getScoutObject() != null) {
        final ITreeNode scoutNode = treePathToScoutNode(path);
        final TransferObject scoutTransferable = SwingUtility.createScoutTransferable(swingTransferable);
        if (scoutTransferable != null) {
          // notify Scout (asynchronous !)
          Runnable t = new Runnable() {
            @Override
            public void run() {
              getScoutObject().getUIFacade().fireNodeDropActionFromUI(scoutNode, scoutTransferable);
            }
          };
          getSwingEnvironment().invokeScoutLater(t, 0);
          // end notify
        }
      }
    }
  }

  protected void handleSwingDropTargetChanged(TreePath path, Transferable swingTransferable) {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (swingTransferable != null && getScoutObject() != null) {
      final ITreeNode scoutNode = treePathToScoutNode(path);
      // notify Scout (asynchronous !)
      Runnable t = new Runnable() {
        @Override
        public void run() {
          getScoutObject().getUIFacade().fireNodeDropTargetChangedFromUI(scoutNode);
        }
      };
      getSwingEnvironment().invokeScoutLater(t, 0);
      // end notify
    }
  }

  protected void handleSwingDragFinished() {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    Runnable t = new Runnable() {
      @Override
      public void run() {
        getScoutObject().getUIFacade().fireDragFinishedFromUI();
      }
    };
    getSwingEnvironment().invokeScoutLater(t, 0);
    // end notify
  }

  protected void handleSwingNodePopup(final MouseEvent e) {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (getScoutObject() != null) {
      TreePath path = getPathForLocation(e.getX(), e.getY());
      ensurePathSelected(path);

      final ITreeNode node = path != null ? (ITreeNode) path.getLastPathComponent() : null;
      // notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          IActionFilter filter;
          if (node == null) {
            filter = ActionUtility.createMenuFilterVisibleAndMenuTypes(TreeMenuType.EmptySpace);
          }
          else {
            filter = getScoutObject().getContextMenu().getActiveFilter();
          }
          // call swing menu
          new SwingPopupWorker(getSwingEnvironment(), e.getComponent(), null, e.getPoint(), getScoutObject().getContextMenu(), filter, false).enqueue();
        }
      };
      getSwingEnvironment().invokeScoutLater(t, 5678);
      // end notify
    }
  }

  protected void handleSwingNodeClick(TreePath path) {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (getScoutObject() != null) {
      ensurePathSelected(path);

      final ITreeNode scoutNode = treePathToScoutNode(path);
      if (scoutNode != null) {
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().fireNodeClickFromUI(scoutNode);
          }
        };

        getSwingEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
  }

  /**
   * @param path
   *          the {@link TreePath} to be selected if it isn't selected yet
   */
  private void ensurePathSelected(TreePath path) {
    if (path != null && !getSwingTree().isPathSelected(path)) {
      getSwingTree().setSelectionPath(path);
    }
  }

  /**
   * Returns the path to the node thats path bounds ({@link javax.swing.JTree#getPathBounds(TreePath)}) contains the
   * given x,y coordinates. Thereby the empty space on the left and right side of nodes will be considered too.
   * 
   * @see javax.swing.JTree#getClosestPathForLocation(int, int)
   */
  private TreePath getPathForLocation(int x, int y) {
    TreePath closestPath = getSwingTree().getClosestPathForLocation(x, y);
    Rectangle pathBounds = getSwingTree().getPathBounds(closestPath);
    TreePath path = null;
    if (closestPath != null && pathBounds != null && pathBounds.contains(pathBounds.x, y)) {
      path = closestPath;
    }
    return path;
  }

  protected void handleSwingNodeAction(TreePath path) {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (getScoutObject() != null) {
      final ITreeNode scoutNode = treePathToScoutNode(path);
      if (scoutNode != null) {
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().fireNodeActionFromUI(scoutNode);
          }
        };
        getSwingEnvironment().invokeScoutLater(t, 400);
        // end notify
      }
    }
  }

  protected void handleSwingHyperlinkAction(TreePath path, final URL url) {
    if (getUpdateSwingFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (getScoutObject() != null && path != null) {
      final ITreeNode scoutNode = treePathToScoutNode(path);
      if (scoutNode != null) {
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().fireHyperlinkActionFromUI(scoutNode, url);
          }
        };
        getSwingEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
  }

  /*
   * static Convenience helpers
   */
  public static ITreeNode treePathToScoutNode(TreePath path) {
    if (path == null) {
      return null;
    }
    return (ITreeNode) path.getLastPathComponent();
  }

  public static List<ITreeNode> treePathsToScoutNodes(Collection<? extends TreePath> paths) {
    if (!CollectionUtility.hasElements(paths)) {
      return CollectionUtility.emptyArrayList();
    }
    List<ITreeNode> scoutNodes = new ArrayList<ITreeNode>(paths.size());
    for (TreePath path : paths) {
      scoutNodes.add(treePathToScoutNode(path));
    }
    return scoutNodes;
  }

  public static TreePath scoutNodeToTreePath(ITreeNode scoutNode) {
    if (scoutNode == null) {
      return null;
    }
    Object[] path = getPathToRoot(scoutNode, 0);
    return new TreePath(path);
  }

  public static List<TreePath> scoutNodesToTreePaths(Collection<? extends ITreeNode> scoutNodes) {
    if (!CollectionUtility.hasElements(scoutNodes)) {
      return CollectionUtility.emptyArrayList();
    }
    List<TreePath> paths = new ArrayList<TreePath>(scoutNodes.size());
    for (ITreeNode node : scoutNodes) {
      paths.add(scoutNodeToTreePath(node));
    }
    return paths;
  }

  public static ITreeNode[] getPathToRoot(ITreeNode scoutNode, int depth) {
    ITreeNode[] retNodes;
    if (scoutNode == null) {
      if (depth == 0) {
        return null;
      }
      else {
        retNodes = new ITreeNode[depth];
      }
    }
    else {
      depth++;
      if (scoutNode.getParentNode() == null) {
        retNodes = new ITreeNode[depth];
      }
      else {
        retNodes = getPathToRoot(scoutNode.getParentNode(), depth);
      }
      retNodes[retNodes.length - depth] = scoutNode;
    }
    return retNodes;
  }

  /*
   * private inner classes
   */
  private class P_ScoutTreeListener implements TreeListener {
    @Override
    public void treeChanged(final TreeEvent e) {
      if (isHandleScoutTreeEvent(CollectionUtility.arrayList(e))) {
        if (isIgnoredScoutEvent(TreeEvent.class, "" + e.getType())) {
          return;
        }
        //
        Runnable t = new Runnable() {
          @Override
          public void run() {
            try {
              getUpdateSwingFromScoutLock().acquire();
              //
              handleScoutTreeEventInSwing(e);
            }
            finally {
              getUpdateSwingFromScoutLock().release();
            }
          }
        };
        getSwingEnvironment().invokeSwingLater(t);
      }
    }

    @Override
    public void treeChangedBatch(final List<? extends TreeEvent> a) {
      //
      if (isHandleScoutTreeEvent(a)) {
        final List<TreeEvent> filteredList = new ArrayList<TreeEvent>();
        for (TreeEvent event : a) {
          if (!isIgnoredScoutEvent(TreeEvent.class, "" + event.getType())) {
            filteredList.add(event);
          }
        }
        if (CollectionUtility.hasElements(filteredList)) {
          Runnable t = new Runnable() {
            @Override
            public void run() {
              try {
                getUpdateSwingFromScoutLock().acquire();
                //
                handleScoutTreeEventBatchInSwing(filteredList);
              }
              finally {
                getUpdateSwingFromScoutLock().release();
              }
            }
          };
          getSwingEnvironment().invokeSwingLater(t);
        }
      }
    }
  }// end private class

  /**
   * @since 4.0-M7
   */
  private class P_DropTargetListener extends DropTargetAdapter {
    private static final long serialVersionUID = 1L;

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
      DataFlavor[] currentFlavors = dtde.getCurrentDataFlavors();
      JComponent jcomponent = (JComponent) dtde.getDropTargetContext().getComponent();
      TransferHandler transferHandler = jcomponent.getTransferHandler();
      if (transferHandler != null && transferHandler.canImport(jcomponent, currentFlavors)) {

        Point location = dtde.getLocation();
        TreePath path = getSwingTree().getPathForLocation(location.x, location.y);
        Transferable t = dtde.getTransferable();
        handleSwingDropTargetChanged(path, t);
      }
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
    }

  } // end class P_DropTargetListener

  /**
   * Implementation of DropSource's DragGestureListener support for drag/drop
   * 
   * @since Build 202
   */
  private class P_SwingDragAndDropTransferHandler extends TransferHandlerEx {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean canDrag() {
      return getScoutObject().isDragEnabled();
    }

    @Override
    public Transferable createTransferable(JComponent c) {
      TreePath[] paths = getSwingTree().getSelectionPaths();
      if (paths != null && paths.length > 0) {
        return handleSwingDragRequest();
      }
      else {
        return null;
      }
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
      return SwingUtility.isSupportedTransfer(getScoutObject().getDropType(), transferFlavors);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
      super.exportDone(source, data, action);
      handleSwingDragFinished();
    }

    @Override
    public boolean importDataEx(JComponent comp, Transferable t, Point location) {
      if (location != null) {
        TreePath dropPath = getSwingTree().getPathForLocation(location.x, location.y);
        handleSwingDropAction(dropPath, t);
        return true;
      }
      return false;
    }

  }// end private class

  private class P_SwingSelectionListener implements TreeSelectionListener {
    @Override
    public void valueChanged(TreeSelectionEvent e) {
      setSelectionFromSwing(getSwingTree().getSelectionPaths());
    }
  }// end private class

  private class P_SwingExpansionListener implements TreeExpansionListener {
    @Override
    public void treeCollapsed(TreeExpansionEvent e) {
      TreePath path = e.getPath();
      if (path != null) {
        setExpansionFromSwing(path, false);
      }
    }

    @Override
    public void treeExpanded(TreeExpansionEvent e) {
      TreePath path = e.getPath();
      if (path != null) {
        setExpansionFromSwing(path, true);
      }
    }
  }// end private class

  private class P_SwingMouseListener extends MouseAdapter {
    private Point m_pressedLocation;
    MouseClickedBugFix fix;

    @Override
    public void mousePressed(MouseEvent e) {
      fix = new MouseClickedBugFix(e);
      m_pressedLocation = e.getPoint();
      e.getComponent().requestFocus();
      if (e.isMetaDown()) {
        TreePath path = getPathForLocation(e.getPoint().x, e.getPoint().y);
        ensurePathSelected(path);
      }
      // Mac popup
      if (e.isPopupTrigger()) {
        handleSwingNodePopup(e);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (e.isPopupTrigger()) {
        handleSwingNodePopup(e);
      }
      else {
        //hyperlink
        TreeHtmlLinkDetector detector = new TreeHtmlLinkDetector();
        if (detector.detect((JTree) e.getComponent(), e.getPoint())) {
          handleSwingHyperlinkAction(detector.getTreePath(), detector.getHyperlink());
        }
      }
      if (fix != null) {
        fix.mouseReleased(this, e);
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (fix.mouseClicked()) {
        return;
      }
      if (e.getButton() == MouseEvent.BUTTON1) {
        if (e.getClickCount() == 1) {
          // ticket 86377
          TreePath path = getPathForLocation(m_pressedLocation.x, m_pressedLocation.y);
          if (path != null) {
            // no click on +/- icon
            if (e.getPoint().x >= getSwingTree().getPathBounds(path).x) {
              handleSwingNodeClick(path);
            }
          }
        }
        else if (e.getClickCount() == 2) {
          // ticket 86377
          TreePath path = getPathForLocation(m_pressedLocation.x, m_pressedLocation.y);
          if (path != null) {
            Rectangle pathBounds = getSwingTree().getPathBounds(path);
            if (m_pressedLocation.x > pathBounds.x + pathBounds.width) {
              // double click in the empty space on the right side of a node.
              // in this case we have to do the default double click behavior manually, because
              // JTree only supports double clicks on the node itself and not in the empty space.
              getSwingTree().toggleExpandState(path);
            }

            handleSwingNodeAction(path);
          }
        }
      }
    }
  }// end private class

  private enum Scrollbar {
    HORIZONTAL("HORIZONTAL_SCROLLBAR"),
    VERTICAL("VERTICAL_SCROLLBAR");

    private final String m_scrollbarType;

    Scrollbar(String scrollbarType) {
      m_scrollbarType = scrollbarType;
    }

    public String getType() {
      return m_scrollbarType;
    }
  }

  private static class ScrollbarValues {
    protected final Integer horizontal;
    protected final Integer vertical;

    public ScrollbarValues(Integer horizontal, Integer vertical) {
      this.horizontal = horizontal;
      this.vertical = vertical;
    }
  }

}
