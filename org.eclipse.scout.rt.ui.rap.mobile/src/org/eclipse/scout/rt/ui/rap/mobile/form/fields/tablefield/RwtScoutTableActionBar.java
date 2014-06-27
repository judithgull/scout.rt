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
package org.eclipse.scout.rt.ui.rap.mobile.form.fields.tablefield;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.rt.client.mobile.ui.action.ActionButtonBarUtility;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.TableMenuType;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.TableAdapter;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.IContentAssistFieldProposalForm;
import org.eclipse.scout.rt.client.ui.form.fields.tablefield.ITableField;
import org.eclipse.scout.rt.ui.rap.LogicalGridData;
import org.eclipse.scout.rt.ui.rap.form.fields.LogicalGridDataBuilder;
import org.eclipse.scout.rt.ui.rap.mobile.action.AbstractRwtScoutActionBar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * @since 3.9.0
 */
public class RwtScoutTableActionBar extends AbstractRwtScoutActionBar<ITableField<? extends ITable>> {
  private static final String VARIANT_SMART_FIELD_ACTION_BAR = "smartFieldActionBar";
  private P_TableRowSelectionListener m_rowSelectionListener;
  private ITable m_table;

  public RwtScoutTableActionBar() {
    setMenuOpeningDirection(SWT.UP);
  }

  @Override
  protected void initLayout(Composite container) {
    super.initLayout(container);

    int tableStatusGridH = 1;
    LogicalGridData tableGridData = LogicalGridDataBuilder.createField(getScoutObject().getGridData());
    LogicalGridData gd = new LogicalGridData();
    gd.gridx = tableGridData.gridx;
    gd.gridy = tableGridData.gridy + tableGridData.gridh + tableStatusGridH;
    gd.gridw = tableGridData.gridw;
    gd.topInset = 0;
    gd.gridh = 1;
    if (getHeightHint() != null) {
      gd.heightHint = getHeightHint();
    }
    else {
      gd.useUiHeight = true;
    }
    gd.weightx = tableGridData.weightx;
    gd.weighty = 0.0;
    gd.fillHorizontal = true;
    container.setLayoutData(gd);
  }

  @Override
  protected String getActionBarContainerVariant() {
    if (getScoutObject().getForm() instanceof IContentAssistFieldProposalForm) {
      return VARIANT_SMART_FIELD_ACTION_BAR;
    }

    return super.getActionBarContainerVariant();
  }

  @Override
  protected void collectMenusForLeftButtonBar(List<IMenu> menuList) {
    ITable table = getScoutObject().getTable();
    if (table == null) {
      return;
    }

    List<IMenu> emptySpaceMenus = ActionUtility.getActions(table.getMenus(), ActionUtility.createMenuFilterVisibleAndMenuTypes(TableMenuType.EmptySpace));
    if (emptySpaceMenus != null) {
      menuList.addAll(emptySpaceMenus);
    }

    if (table.getSelectedRowCount() > 0) {
      List<IMenu> rowMenus = ActionUtility.getActions(table.getMenus(), table.getContextMenu().getActiveFilter());
      if (rowMenus != null) {
        List<IMenu> editableRowMenus = new ArrayList<IMenu>(rowMenus);
        ActionButtonBarUtility.distributeRowActions(menuList, emptySpaceMenus, editableRowMenus);
        //Add remaining row menus
        menuList.addAll(editableRowMenus);
      }
    }
  }

  @Override
  protected void collectMenusForRightButtonBar(List<IMenu> menuList) {

  }

  @Override
  protected void attachScout() {
    super.attachScout();

    m_table = getScoutObject().getTable();

    addRowSelectionListener(m_table);
  }

  @Override
  protected void detachScout() {
    super.detachScout();

    removeRowSelectionListener(m_table);

    m_table = null;
  }

  private void addRowSelectionListener(ITable table) {
    if (m_rowSelectionListener != null || table == null) {
      return;
    }

    m_rowSelectionListener = new P_TableRowSelectionListener();
    table.addTableListener(m_rowSelectionListener);
  }

  private void removeRowSelectionListener(ITable table) {
    if (m_rowSelectionListener == null || table == null) {
      return;
    }

    table.removeTableListener(m_rowSelectionListener);
    m_rowSelectionListener = null;
  }

  @Override
  protected void handleScoutPropertyChange(String name, Object newValue) {
    super.handleScoutPropertyChange(name, newValue);

    if (name.equals(ITableField.PROP_TABLE)) {
      removeRowSelectionListener(m_table);

      m_table = (ITable) newValue;

      addRowSelectionListener(m_table);
    }
  }

  private class P_TableRowSelectionListener extends TableAdapter {

    @Override
    public void tableChanged(TableEvent e) {
      if (e.getType() == TableEvent.TYPE_ROWS_SELECTED) {
        rowSelected();
      }
    }

    private void rowSelected() {
      rebuildContentFromScout();
    }

  }

}
