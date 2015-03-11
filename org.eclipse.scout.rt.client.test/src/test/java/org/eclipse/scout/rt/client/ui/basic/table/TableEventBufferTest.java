/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.basic.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.client.ui.basic.cell.ICell;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link TableEventBuffer}
 */
public class TableEventBufferTest {
  private TableEventBuffer m_testBuffer;

  @Before
  public void setup() {
    m_testBuffer = new TableEventBuffer();
  }

  /**
   * EventBuffer should be initially empty.
   */
  @Test
  public void testEmpty() {
    assertTrue(m_testBuffer.isEmpty());
    assertTrue(m_testBuffer.removeEvents().isEmpty());
  }

  /**
   * A single event should remain the same when removed from the buffer.
   */
  @Test
  public void testSingleEvent() {
    final TableEvent se = mockEvent(TableEvent.TYPE_ROWS_SELECTED);
    m_testBuffer.add(se);
    final List<TableEvent> events = m_testBuffer.removeEvents();
    assertEquals(se, events.iterator().next());
    assertTrue(m_testBuffer.isEmpty());
  }

  /**
   * Some events should not be coalesced: selected, updated, row_action.
   */
  @Test
  public void testNoCoalesce() {
    final TableEvent e1 = mockEvent(TableEvent.TYPE_ROWS_SELECTED);
    List<ITableRow> rows = new ArrayList<>();
    rows.add(mock(ITableRow.class));
    final TableEvent e2 = new TableEvent(mock(ITable.class), TableEvent.TYPE_ROWS_UPDATED, rows);
    final TableEvent e3 = mockEvent(TableEvent.TYPE_ROW_ACTION);
    m_testBuffer.add(e1);
    m_testBuffer.add(e2);
    m_testBuffer.add(e3);
    final List<TableEvent> coalesced = m_testBuffer.removeEvents();
    assertEquals(3, coalesced.size());
    assertSame(e1, coalesced.get(0));
    assertSame(e2, coalesced.get(1));
    assertSame(e3, coalesced.get(2));
  }

  /**
   * Only the last selection event should be kept.
   */
  @Test
  public void testSelections() {
    final TableEvent se1 = mockEvent(TableEvent.TYPE_ROWS_SELECTED);
    final TableEvent se2 = mockEvent(TableEvent.TYPE_SCROLL_TO_SELECTION);
    final TableEvent se3 = mockEvent(TableEvent.TYPE_ROWS_SELECTED);
    m_testBuffer.add(se1);
    m_testBuffer.add(se2);
    m_testBuffer.add(se3);
    final List<TableEvent> events = m_testBuffer.removeEvents();
    assertEquals(2, events.size());
    assertSame(se2, events.get(0));
    assertSame(se3, events.get(1));
  }

  /**
   * If all rows are deleted, previous row related events should be removed: row_action, scroll_to_selection,...
   * Other events should remain in the list.
   */
  @Test
  public void testAllRowsDeleted() {
    m_testBuffer.add(mockEvent(TableEvent.TYPE_SCROLL_TO_SELECTION));
    m_testBuffer.add(mockEvent(TableEvent.TYPE_ROW_ACTION));
    final TableEvent columnEvent = mockEvent(TableEvent.TYPE_COLUMN_HEADERS_UPDATED);
    m_testBuffer.add(columnEvent);
    m_testBuffer.add(mockEvent(TableEvent.TYPE_ROW_CLICK));
    m_testBuffer.add(mockEvent(TableEvent.TYPE_ROW_ORDER_CHANGED));
    m_testBuffer.add(mockEvent(TableEvent.TYPE_ROWS_INSERTED));
    m_testBuffer.add(mockEvent(TableEvent.TYPE_ROWS_DELETED));
    final TableEvent alldeleted = mockEvent(TableEvent.TYPE_ALL_ROWS_DELETED);
    m_testBuffer.add(alldeleted);
    final List<TableEvent> events = m_testBuffer.removeEvents();
    assertEquals(2, events.size());
    assertSame(columnEvent, events.get(0));
    assertSame(alldeleted, events.get(1));
  }

  /**
   * Multiple update events should be merged into a single event with the rows combined in the correct order.
   */
  @Test
  public void testCombineMultipleUpdates() {
    List<ITableRow> rows1 = new ArrayList<>();
    final ITableRow r1 = mockTableRow(0);
    final ITableRow r2 = mockTableRow(1);
    rows1.add(r1);
    rows1.add(r2);
    final TableEvent e1 = new TableEvent(mock(ITable.class), TableEvent.TYPE_ROWS_UPDATED, rows1);
    List<ITableRow> rows2 = new ArrayList<>();
    final ITableRow r3 = mockTableRow(2);
    rows2.add(r3);
    final TableEvent e2 = new TableEvent(mock(ITable.class), TableEvent.TYPE_ROWS_UPDATED, rows2);
    m_testBuffer.add(e1);
    m_testBuffer.add(e2);

    final List<TableEvent> events = m_testBuffer.removeEvents();

    assertEquals(1, events.size());
    final List<ITableRow> resultRows = events.get(0).getRows();
    assertEquals(3, resultRows.size());

    List<ITableRow> expected = new ArrayList<>();
    expected.add(r1);
    expected.add(r2);
    expected.add(r3);
    assertTrue(CollectionUtility.equalsCollection(expected, resultRows));
  }

  /**
   * Multiple consecutive column headers updated should be merged in such a way that only the last event is kept with
   * the merged columns.
   */
  @Test
  public void testCombineMultipleInsertsSameRow() {
    List<IColumn<?>> cols1 = new ArrayList<>();
    final IColumn<?> c1 = mockColumn(0);

    final IColumn<?> c2 = mockColumn(1);
    cols1.add(c1);
    cols1.add(c2);
    List<IColumn<?>> cols2 = new ArrayList<>();
    final IColumn<?> c3 = mockColumn(0);
    cols2.add(c3);
    final TableEvent e1 = mockEvent(TableEvent.TYPE_COLUMN_HEADERS_UPDATED);
    e1.setColumns(cols1);
    m_testBuffer.add(e1);
    final TableEvent e2 = mockEvent(TableEvent.TYPE_COLUMN_HEADERS_UPDATED);
    e2.setColumns(cols2);
    m_testBuffer.add(e2);

    final List<TableEvent> events = m_testBuffer.removeEvents();

    assertEquals(1, events.size());
    final Collection<IColumn<?>> resultRows = events.get(0).getColumns();
    assertEquals(2, resultRows.size());

    List<IColumn<?>> expected = new ArrayList<>();
    expected.add(c2);
    expected.add(c3);
    assertTrue(CollectionUtility.equalsCollection(expected, resultRows));
  }

  /**
   * If, a row is inserted and immediately after that updated, only an insert event with the updated value needs to be
   * kept.
   */
  @Test
  public void testInsertedFollowedByUpdated() {
    List<ITableRow> rows = new ArrayList<>();
    final ITableRow newRow = mockTableRow(0);
    rows.add(newRow);

    List<ITableRow> updatedRows = new ArrayList<>();
    final ITableRow updatedRow = mockTableRow(0);
    when(updatedRow.getCell(0)).thenReturn(mock(ICell.class));
    updatedRows.add(updatedRow);

    final TableEvent insert = new TableEvent(mock(ITable.class), TableEvent.TYPE_ROWS_INSERTED, rows);
    final TableEvent update = new TableEvent(mock(ITable.class), TableEvent.TYPE_ROWS_UPDATED, updatedRows);
    m_testBuffer.add(insert);
    m_testBuffer.add(update);
    final List<TableEvent> events = m_testBuffer.removeEvents();
    assertEquals(1, events.size());
    assertNotNull(events.get(0).getFirstRow().getCell(0));
  }

  /**
   * Updates that are not consecutive are not combined.
   */
  @Test
  public void testCombineOnlyConsecutiveUpdates() {
    final TableEvent e1 = createTestUpdateEvent();
    m_testBuffer.add(e1);
    m_testBuffer.add(mockEvent(TableEvent.TYPE_ROWS_INSERTED));
    final TableEvent e2 = createTestUpdateEvent();
    m_testBuffer.add(e2);
    final List<TableEvent> events = m_testBuffer.removeEvents();
    assertEquals(3, events.size());
    assertEquals(2, events.get(0).getRows().size());
  }

  /**
   * If an insert is followed by an update, the update can be merged with the insert.
   */
  @Test
  public void testInsertFollowedByUpdatesOnSameRow() {
    List<ITableRow> rows = new ArrayList<>();
    final ITableRow newRow = mockTableRow(0);
    final ICell cell1 = mock(ICell.class);
    when(newRow.getCell(0)).thenReturn(cell1);
    rows.add(newRow);

    List<ITableRow> updatedRows = new ArrayList<>();
    final ITableRow updatedRow = mock(ITableRow.class);
    when(updatedRow.getRowIndex()).thenReturn(1);
    when(updatedRow.getCell(0)).thenReturn(cell1);
    updatedRows.add(updatedRow);
    final TableEvent insert = new TableEvent(mock(ITable.class), TableEvent.TYPE_ROWS_INSERTED, rows);
    final TableEvent update = new TableEvent(mock(ITable.class), TableEvent.TYPE_ROWS_UPDATED, updatedRows);

    m_testBuffer.add(insert);
    m_testBuffer.add(update);
    final List<TableEvent> events = m_testBuffer.removeEvents();
    assertEquals(2, events.size());
    assertEquals(cell1, events.get(0).getFirstRow().getCell(0));
  }

  private TableEvent createTestUpdateEvent() {
    List<ITableRow> rows = new ArrayList<>();
    rows.add(mock(ITableRow.class));
    rows.add(mock(ITableRow.class));
    return new TableEvent(mock(ITable.class), TableEvent.TYPE_ROWS_UPDATED, rows);
  }

  private TableEvent mockEvent(int type) {
    return new TableEvent(mock(ITable.class), type);
  }

  private ITableRow mockTableRow(int rowIndex) {
    final ITableRow r = mock(ITableRow.class);
    when(r.getRowIndex()).thenReturn(rowIndex);
    return r;
  }

  private IColumn<?> mockColumn(int colIndex) {
    final IColumn col = mock(IColumn.class);
    when(col.getColumnIndex()).thenReturn(colIndex);
    return col;
  }

}