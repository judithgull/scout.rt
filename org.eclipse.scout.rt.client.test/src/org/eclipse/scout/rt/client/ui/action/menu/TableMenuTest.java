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
package org.eclipse.scout.rt.client.ui.action.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.IActionFilter;
import org.eclipse.scout.rt.client.ui.action.menu.fixture.OwnerValueCapturingMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.ITableContextMenu;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractStringColumn;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link AbstractTableMenu}
 */
@RunWith(ScoutClientTestRunner.class)
public class TableMenuTest {

  private OwnerValueCapturingMenu m_multi;
  private OwnerValueCapturingMenu m_single;
  private OwnerValueCapturingMenu m_emptySpace;
  private OwnerValueCapturingMenu m_combindedSingle;
  private OwnerValueCapturingMenu m_all;

  @Before
  public void before() {
    m_multi = new OwnerValueCapturingMenu(TableMenuType.MultiSelection);
    m_single = new OwnerValueCapturingMenu(TableMenuType.SingleSelection);
    m_emptySpace = new OwnerValueCapturingMenu(TableMenuType.EmptySpace);
    m_combindedSingle = new OwnerValueCapturingMenu(TableMenuType.EmptySpace, TableMenuType.SingleSelection);
    m_all = new OwnerValueCapturingMenu(TableMenuType.values());
  }

  private static final Object[][] TEST_ROWS = new Object[][]{
      {"Boss", "Hugo"},
      {"Meier", "Hans"}
  };

  /**
   * Tests the visibility for a single selection menu
   */
  @Test
  public void testSingleSelection() throws ProcessingException {
    Table t = new Table();
    t.addRowsByMatrix(TEST_ROWS);
    ITableContextMenu contextMenu = t.getContextMenu();

    // single hugo boss
    t.selectRows(CollectionUtility.arrayList(t.getRow(0)), false);
    IActionFilter filter = ActionUtility.createMenuFilterMenuTypes(contextMenu.getCurrentMenuTypes(), true);
    List<IMenu> visibleMenus = ActionUtility.normalizedActions(contextMenu.getChildActions(), filter);
    assertEquals(2, visibleMenus.size());
    assertEquals("SingleSelectionMenu", visibleMenus.get(0).getClass().getSimpleName());
    assertEquals("HugoBossMenu", visibleMenus.get(1).getClass().getSimpleName());

    // single only meier
    t.selectRows(CollectionUtility.arrayList(t.getRow(1)), false);
    filter = ActionUtility.createMenuFilterMenuTypes(contextMenu.getCurrentMenuTypes(), true);
    visibleMenus = ActionUtility.normalizedActions(contextMenu.getChildActions(), filter);
    assertEquals(1, visibleMenus.size());
    assertEquals("SingleSelectionMenu", visibleMenus.get(0).getClass().getSimpleName());
  }

  /**
   * Tests the visibility for a multi selection menu
   */
  @Test
  public void setMultiSeleciton() throws ProcessingException {
    Table t = new Table();
    t.addRowsByMatrix(TEST_ROWS);
    ITableContextMenu contextMenu = t.getContextMenu();
    // multi selection
    t.selectRows(CollectionUtility.arrayList(t.getRow(0), t.getRow(1)), false);
    IActionFilter filter = ActionUtility.createMenuFilterMenuTypes(contextMenu.getCurrentMenuTypes(), true);
    List<IMenu> visibleMenus = ActionUtility.normalizedActions(contextMenu.getChildActions(), filter);
    assertEquals(1, visibleMenus.size());
    assertEquals("MultiSelectionMenu", visibleMenus.get(0).getClass().getSimpleName());

  }

  /**
   * Tests the visibility for a empty space menu
   */
  @Test
  public void testEmptySeleciton() throws ProcessingException {
    Table t = new Table();
    t.addRowsByMatrix(TEST_ROWS);
    ITableContextMenu contextMenu = t.getContextMenu();
    // empty selection
    t.selectRows(CollectionUtility.<ITableRow> emptyArrayList(), false);
    IActionFilter filter = ActionUtility.createMenuFilterMenuTypes(contextMenu.getCurrentMenuTypes(), true);
    List<IMenu> visibleMenus = ActionUtility.normalizedActions(contextMenu.getChildActions(), filter);
    assertEquals(1, visibleMenus.size());
    assertEquals("EmptySpaceMenu", visibleMenus.get(0).getClass().getSimpleName());
  }

  /**
   * Tests that {@link AbstractMenu#execOwnerValueChanged} is only called only multi-selection menus, if multiple rows
   * are selected.
   */
  @Test
  public void testOwnerValueOnMultiSelection() throws ProcessingException {
    final ContextMenuTable table = createContextMenuTable();
    addTestMenus(table);

    table.selectAllRows();

    assertOwnerValueChange(m_multi, 2);
    assertOwnerValueChange(m_all, 2);
    assertNoOwnerValueChange(m_single);
    assertNoOwnerValueChange(m_emptySpace);
    assertNoOwnerValueChange(m_combindedSingle);
  }

  /**
   * Tests that {@link AbstractMenu#execOwnerValueChanged} is only called only single-selection menus, if the a single
   * row is selected.
   */
  @Test
  public void testOwnerValueOnSingleSelection() throws ProcessingException {
    final ContextMenuTable table = createContextMenuTable();
    addTestMenus(table);
    table.selectFirstRow();

    assertOwnerValueChange(m_single, 1);
    assertOwnerValueChange(m_all, 1);
    assertNoOwnerValueChange(m_multi);
    assertNoOwnerValueChange(m_emptySpace);
  }

  /**
   * Tests that {@link AbstractMenu#execOwnerValueChanged} is only called only empty space menus, if empty space is
   * selected.
   */
  @Test
  public void testOwnerValueOnEmptySpace() throws ProcessingException {
    final ContextMenuTable table = createContextMenuTable();
    table.selectAllRows();
    addTestMenus(table);
    table.deselectAllRows();

    assertOwnerValueChange(m_emptySpace, 0);
    assertOwnerValueChange(m_all, 0);
    assertNoOwnerValueChange(m_multi);
    assertNoOwnerValueChange(m_single);
  }

  /// HELPERS

  private void addTestMenus(ITable table) {
    table.addMenu(m_emptySpace);
    table.addMenu(m_single);
    table.addMenu(m_multi);
    table.addMenu(m_all);
    table.addMenu(m_combindedSingle);
  }

  private ContextMenuTable createContextMenuTable() throws ProcessingException {
    final ContextMenuTable table = new ContextMenuTable();
    table.addRowsByMatrix(TEST_ROWS);
    return table;
  }

  private void assertOwnerValueChange(OwnerValueCapturingMenu menu, int rows) {
    assertEquals(1, menu.getCount());
    assertTrue("Owner should be a collection of 2 rows " + menu.getLastOwnerValue().getClass(), menu.getLastOwnerValue() instanceof Collection);
    assertEquals(rows, ((Collection) menu.getLastOwnerValue()).size());
  }

  private void assertNoOwnerValueChange(OwnerValueCapturingMenu menu) {
    assertEquals(0, menu.getCount());
  }

  /// FIXTURES

  public class Table extends AbstractTable {
    public NameColumn getNameColumn() {
      return getColumnSet().getColumnByClass(NameColumn.class);
    }

    public PrenameColumn getPrenameColumn() {
      return getColumnSet().getColumnByClass(PrenameColumn.class);
    }

    @Order(10)
    public class NameColumn extends AbstractStringColumn {

    }

    @Order(10)
    public class PrenameColumn extends AbstractStringColumn {

    }

    @Order(100)
    public class SingleSelectionMenu extends AbstractMenu {

      @Override
      protected Set<? extends IMenuType> getConfiguredMenuTypes() {
        return CollectionUtility.hashSet(TableMenuType.SingleSelection);
      }

    }

    @Order(111)
    public class HugoBossMenu extends AbstractMenu {

      @Override
      protected Set<? extends IMenuType> getConfiguredMenuTypes() {
        return CollectionUtility.hashSet(TableMenuType.SingleSelection);
      }

      @Override
      protected void execOwnerValueChanged(Object newOwnerValue) throws ProcessingException {
        setVisible(CompareUtility.equals(getPrenameColumn().getSelectedValue(), "Hugo"));
      }
    }

    @Order(110)
    public class MultiSelectionMenu extends AbstractMenu {

      @Override
      protected Set<? extends IMenuType> getConfiguredMenuTypes() {
        return CollectionUtility.hashSet(TableMenuType.MultiSelection);
      }

    }

    @Order(120)
    public class EmptySpaceMenu extends AbstractMenu {

      @Override
      protected Set<? extends IMenuType> getConfiguredMenuTypes() {
        return CollectionUtility.hashSet(TableMenuType.EmptySpace);
      }
    }
  }

  private static class ContextMenuTable extends AbstractTable {
    @Override
    public void setContextMenu(ITableContextMenu contextMenu) {
      super.setContextMenu(contextMenu);
    }
  }

}
