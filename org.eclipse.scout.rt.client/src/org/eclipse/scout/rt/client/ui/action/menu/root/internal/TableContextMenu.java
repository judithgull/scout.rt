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

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.IActionVisitor;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.MenuUtility;
import org.eclipse.scout.rt.client.ui.action.menu.root.AbstractPropertyObserverContextMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.ITableContextMenu;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableAdapter;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;

/**
 * The invisible root menu node of any table. (internal usage only)
 */
public class TableContextMenu extends AbstractPropertyObserverContextMenu<ITable> implements ITableContextMenu {
  private List<? extends ITableRow> m_currentSelection;

  /**
   * @param owner
   */
  public TableContextMenu(ITable owner, List<? extends IMenu> initialChildMenus) {
    super(owner, initialChildMenus);
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    getOwner().addTableListener(new P_OwnerTableListener());
    // set active filter
    setCurrentMenuTypes(MenuUtility.getMenuTypesForTableSelection(getOwner().getSelectedRows()));
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
    m_currentSelection = null;
    if (getOwner() != null) {
      final List<ITableRow> ownerValue = getOwner().getSelectedRows();
      m_currentSelection = CollectionUtility.arrayList(ownerValue);
      setCurrentMenuTypes(MenuUtility.getMenuTypesForTableSelection(ownerValue));
      acceptVisitor(new MenuOwnerChangedVisitor(ownerValue, getCurrentMenuTypes()));
      calculateLocalVisibility();
      calculateEnableState(ownerValue);
    }
  }

  /**
   * @param ownerValue
   */
  protected void calculateEnableState(List<? extends ITableRow> ownerValue) {
    boolean enabled = true;
    for (ITableRow row : ownerValue) {
      if (!row.isEnabled()) {
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
   * @param rows
   */
  protected void handleRowsUpdated(List<ITableRow> rows) {
    if (CollectionUtility.containsAny(rows, m_currentSelection)) {
      calculateEnableState(m_currentSelection);
    }
  }

  @Override
  protected void handleOwnerPropertyChanged(PropertyChangeEvent evt) {
    if (ITable.PROP_ENABLED.equals(evt.getPropertyName())) {
      handleOwnerEnabledChanged();
    }
  }

  private class P_OwnerTableListener extends TableAdapter {
    @Override
    public void tableChanged(TableEvent e) {
      if (e.getType() == TableEvent.TYPE_ROWS_SELECTED) {
        handleOwnerValueChanged();
      }
      else if (e.getType() == TableEvent.TYPE_ROWS_UPDATED) {
        handleRowsUpdated(e.getRows());
      }
    }

  }
}
