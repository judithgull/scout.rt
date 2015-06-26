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
package org.eclipse.scout.rt.client.ui.action.menu.root.internal;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.IActionVisitor;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.MenuUtility;
import org.eclipse.scout.rt.client.ui.action.menu.root.AbstractPropertyObserverContextMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.ITreeContextMenu;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeAdapter;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;

/**
 *
 */
public class TreeContextMenu extends AbstractPropertyObserverContextMenu<ITree> implements ITreeContextMenu {
  private Set<? extends ITreeNode> m_currentSelection;

  /**
   * @param owner
   */
  public TreeContextMenu(ITree owner, List<? extends IMenu> initialChildMenus) {
    super(owner, initialChildMenus);
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    getOwner().addTreeListener(new P_OwnerTreeListener());
    // init current menu types
    setCurrentMenuTypes(MenuUtility.getMenuTypesForTreeSelection(getOwner().getSelectedNodes()));
    calculateLocalVisibility();
  }

  @Override
  protected void afterChildMenusAdd(Collection<? extends IMenu> newChildMenus) {
    super.afterChildMenusAdd(newChildMenus);
    handleOwnerEnabledChanged();
  }

  @Override
  protected void afterChildMenusRemove(Collection<? extends IMenu> childMenusToRemove) {
    super.afterChildMenusRemove(childMenusToRemove);
    handleOwnerEnabledChanged();
  }

  /**
   *
   */
  protected void handleOwnerEnabledChanged() {
    if (getOwner() != null) {
      final boolean enabled = getOwner().isEnabled();
      acceptVisitor(new IActionVisitor() {
        @Override
        public int visit(IAction action) {
          if (action instanceof IMenu) {
            IMenu menu = (IMenu) action;
            if (!menu.hasChildActions() && menu.isInheritAccessibility()) {
              menu.setEnabled(enabled);
            }
          }
          return CONTINUE;
        }
      });
    }
  }

  @Override
  public void callOwnerValueChanged() {
    handleOwnerValueChanged();
  }

  protected void handleOwnerValueChanged() {
    if (getOwner() != null) {
      final Set<ITreeNode> ownerSelection = getOwner().getSelectedNodes();
      m_currentSelection = CollectionUtility.hashSet(ownerSelection);
      setCurrentMenuTypes(MenuUtility.getMenuTypesForTreeSelection(ownerSelection));
      acceptVisitor(new MenuOwnerChangedVisitor(ownerSelection, getCurrentMenuTypes()));
      // update menu types
      calculateLocalVisibility();
      calculateEnableState(ownerSelection);
    }
  }

  /**
   * @param ownerSelection
   */
  protected void calculateEnableState(Collection<? extends ITreeNode> ownerSelection) {
    boolean enabled = true;
    for (ITreeNode node : ownerSelection) {
      if (!node.isEnabled()) {
        enabled = false;
        break;
      }
    }
    final boolean inheritedEnability = enabled;
    acceptVisitor(new IActionVisitor() {
      @Override
      public int visit(IAction action) {
        if (action instanceof IMenu) {
          IMenu menu = (IMenu) action;
          if (!menu.hasChildActions() && menu.isInheritAccessibility()) {
            menu.setEnabledInheritAccessibility(inheritedEnability);
          }
        }
        return CONTINUE;
      }
    });
  }

  /**
   * @param nodes
   */
  protected void handleNodesUpdated(Collection<ITreeNode> nodes) {
    if (CollectionUtility.containsAny(nodes, m_currentSelection)) {
      calculateEnableState(m_currentSelection);
    }
  }

  @Override
  protected void handleOwnerPropertyChanged(PropertyChangeEvent evt) {
    if (ITable.PROP_ENABLED.equals(evt.getPropertyName())) {
      handleOwnerEnabledChanged();
    }
  }

  private class P_OwnerTreeListener extends TreeAdapter {

    @Override
    public void treeChanged(TreeEvent e) {
      if (e.getType() == TreeEvent.TYPE_NODES_SELECTED) {
        handleOwnerValueChanged();
      }
      else if (e.getType() == TreeEvent.TYPE_NODES_UPDATED) {
        handleNodesUpdated(e.getNodes());
      }
    }
  }
}
