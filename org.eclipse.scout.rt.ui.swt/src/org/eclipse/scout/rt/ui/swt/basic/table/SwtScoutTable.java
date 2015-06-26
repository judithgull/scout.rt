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
package org.eclipse.scout.rt.ui.swt.basic.table;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.beans.IPropertyObserver;
import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.job.JobEx;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.ui.IDNDSupport;
import org.eclipse.scout.rt.client.ui.IEventHistory;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.IActionFilter;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.TableMenuType;
import org.eclipse.scout.rt.client.ui.action.menu.root.IContextMenu;
import org.eclipse.scout.rt.client.ui.basic.table.IHeaderCell;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.RowIndexComparator;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.table.TableListener;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.shared.security.CopyToClipboardPermission;
import org.eclipse.scout.rt.shared.services.common.security.ACCESS;
import org.eclipse.scout.rt.ui.swt.ISwtEnvironment;
import org.eclipse.scout.rt.ui.swt.SwtMenuUtility;
import org.eclipse.scout.rt.ui.swt.action.menu.MenuPositionCorrectionListener;
import org.eclipse.scout.rt.ui.swt.action.menu.SwtScoutContextMenu;
import org.eclipse.scout.rt.ui.swt.basic.SwtScoutComposite;
import org.eclipse.scout.rt.ui.swt.basic.table.celleditor.TableEditingSupport;
import org.eclipse.scout.rt.ui.swt.ext.table.TableEx;
import org.eclipse.scout.rt.ui.swt.ext.table.util.TableRolloverSupport;
import org.eclipse.scout.rt.ui.swt.extension.UiDecorationExtensionPoint;
import org.eclipse.scout.rt.ui.swt.form.fields.AbstractSwtScoutDndSupport;
import org.eclipse.scout.rt.ui.swt.form.fields.ISwtScoutDndSupport;
import org.eclipse.scout.rt.ui.swt.keystroke.ISwtKeyStroke;
import org.eclipse.scout.rt.ui.swt.util.SwtTransferObject;
import org.eclipse.scout.rt.ui.swt.util.SwtUtility;
import org.eclipse.scout.rt.ui.swt.util.UiRedrawHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * <h3>SwtScoutTable</h3> ...
 * knownIssues - multi column sorting is not supported, unable to get any key
 * mask in the selection event.
 * <p>
 * - multi line support in headers is not supported by swt.
 * <p>
 * - multi line support in row texts is not supported so far. Might probably be done by customized table rows.
 *
 * @since 1.0.0 28.03.2008
 */
public class SwtScoutTable extends SwtScoutComposite<ITable> implements ISwtScoutTable {
  private P_ScoutTableListener m_scoutTableListener;
  private UiRedrawHandler m_redrawHandler;

  private Listener m_autoResizeColumnListener;
  private Listener m_columnListener = new P_TableColumnListener();
  private SelectionListener m_columnSortListener = new P_ColumnSortListener();
  private TableColumnManager m_columnManager = new TableColumnManager();
  private int[] m_uiColumnOrder;
  private Menu m_contextMenu;
  private Menu m_headerMenu;
  private TableViewer m_viewer;
  private ISwtKeyStroke[] m_keyStrokes;
  private ClientSyncJob m_storeColumnWidthsJob;
  private TableKeyboardNavigationSupport m_keyboardNavigationSupport;
  private TableMouseMoveSelectionSupport m_mouseMoveSelectionSupport;
  private ISwtScoutDndSupport m_dndSupport;

  public SwtScoutTable() {
  }

  @Override
  protected void initializeSwt(Composite parent) {
    m_redrawHandler = new UiRedrawHandler(parent);
    int style;
    if (getScoutObject() != null && getScoutObject().isMultiSelect()) {
      style = SWT.MULTI;
    }
    else {
      style = SWT.SINGLE;
    }
    style |= SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.BORDER;
    TableEx table = getEnvironment().getFormToolkit().createTable(parent, style, getScoutObject());
    table.setLinesVisible(false);
    table.setHeaderVisible(true);
    // correction of menu position
    table.addListener(SWT.MenuDetect, new MenuPositionCorrectionListener(table, MenuPositionCorrectionListener.HORIZONTAL_CENTER | MenuPositionCorrectionListener.VERTICAL_CENTER));

    new TableRolloverSupport(table);
    table.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        if (m_headerMenu != null && !m_headerMenu.isDisposed()) {
          m_headerMenu.dispose();
        }
        if (m_contextMenu != null && !m_contextMenu.isDisposed()) {
          m_contextMenu.dispose();
        }
      }
    });
    TableViewer viewer = new P_TableViewerEx(table);
    viewer.setUseHashlookup(true);
    setSwtTableViewer(viewer);
    setSwtField(table);

    // header menu
    m_headerMenu = new Menu(viewer.getTable().getShell(), SWT.POP_UP);
    table.addMenuDetectListener(new P_SwtHeaderMenuDetectListener());

    // ui listeners
    viewer.addSelectionChangedListener(new P_SwtSelectionListener());
    P_SwtTableListener swtTableListener = new P_SwtTableListener();
    table.addListener(SWT.MouseDown, swtTableListener);
    table.addListener(SWT.MouseUp, swtTableListener);
    table.addListener(SWT.MouseDoubleClick, swtTableListener);
    table.addListener(SWT.MenuDetect, swtTableListener);
    table.addListener(SWT.KeyUp, swtTableListener);

    // context menu
    m_contextMenu = new SwtScoutContextMenu(getSwtField().getShell(), getScoutObject().getContextMenu(), getEnvironment()).getSwtMenu();

    // CTRL-C listener to copy selected rows into clipboard)
    table.addKeyListener(new KeyAdapter() {

      @Override
      public void keyPressed(KeyEvent e) {
        if ((e.stateMask & SWT.MOD1) == 0 || e.keyCode != ((int) 'c')) {
          return;
        }

        // CTRL-C event
        TransferObject scoutTransferable = handleSwtCopyRequest();
        if (scoutTransferable == null) {
          return;
        }

        SwtTransferObject[] swtTransferables = SwtUtility.createSwtTransferables(scoutTransferable);
        if (swtTransferables.length == 0) {
          return;
        }

        Clipboard clipboard = new Clipboard(getEnvironment().getDisplay());
        try {
          Transfer[] dataTypes = new Transfer[swtTransferables.length];
          Object[] data = new Object[swtTransferables.length];
          for (int i = 0; i < swtTransferables.length; i++) {
            dataTypes[i] = swtTransferables[i].getTransfer();
            data[i] = swtTransferables[i].getData();
          }
          clipboard.setContents(data, dataTypes);
        }
        finally {
          clipboard.dispose();
        }
      }
    });

    // Install tooltip support for the table.
    ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.RECREATE);
  }

  @Override
  public boolean isDisposed() {
    return getSwtField() == null || getSwtField().isDisposed();
  }

  private void initializeTableModel() {
    getSwtTableViewer().setContentProvider(ArrayContentProvider.getInstance());
    getSwtTableViewer().setInput(getScoutObject().getFilteredRows());
  }

  public TableColumnManager getColumnManager() {
    return m_columnManager;
  }

  public void initializeColumns() {

    m_redrawHandler.pushControlChanging();
    try {
      for (TableColumn col : getSwtField().getColumns()) {
        col.dispose();
      }

      // SWT bug 43910: Create an invisible dummy column to omit SWT indention bug.
      TableColumn dummyCol = new TableColumn(getSwtField(), SWT.LEFT);
      dummyCol.setWidth(0);
      dummyCol.setResizable(false);
      dummyCol.setMoveable(false);

      boolean sortEnabled = false;
      List<IColumn<?>> scoutColumnsOrdered;
      if (getScoutObject() != null) {
        scoutColumnsOrdered = getScoutObject().getColumnSet().getVisibleColumns();
        sortEnabled = getScoutObject().isSortEnabled();
      }
      else {
        scoutColumnsOrdered = CollectionUtility.emptyArrayList();
      }
      if (m_columnManager == null) {
        m_columnManager = new TableColumnManager();
      }
      m_columnManager.initialize(scoutColumnsOrdered);

      boolean hasEditors = false;
      for (IColumn<?> scoutColumn : scoutColumnsOrdered) {
        IHeaderCell headerCell = scoutColumn.getHeaderCell();

        int style = SwtUtility.getHorizontalAlignment(headerCell.getHorizontalAlignment());
        final TableViewerColumn viewerColumn = new TableViewerColumn(getSwtTableViewer(), style);
        TableColumn swtColumn = viewerColumn.getColumn();
        swtColumn.setData(KEY_SCOUT_COLUMN, scoutColumn);

        // Install the column's LabelProvider.
        viewerColumn.setLabelProvider(new SwtScoutCellLabelProvider(scoutColumn, getEnvironment()));

        // Install editable support.
        if (scoutColumn.isEditable()) {
          viewerColumn.setEditingSupport(new TableEditingSupport(getSwtTableViewer(), swtColumn, getEnvironment()));
          hasEditors = true;
        }

        swtColumn.setMoveable(true);
        swtColumn.setToolTipText(headerCell.getTooltipText());
        updateHeaderText(swtColumn, scoutColumn);
        swtColumn.setWidth(scoutColumn.getWidth());
        if (scoutColumn.isFixedWidth()) {
          swtColumn.setResizable(false);
        }
        if (headerCell.isSortActive()) {
          getSwtField().setSortColumn(swtColumn);
          getSwtField().setSortDirection(headerCell.isSortAscending() ? SWT.UP : SWT.DOWN);
        }
        if (sortEnabled) {
          swtColumn.addSelectionListener(m_columnSortListener);
        }
        swtColumn.addListener(SWT.Move, m_columnListener);
        swtColumn.addListener(SWT.Resize, m_columnListener);
      }

      // increase row height when editors are present.
      if (hasEditors) {
        getSwtTableViewer().getTable().addListener(SWT.MeasureItem, new Listener() {
          @Override
          public void handleEvent(Event event) {
            event.height = Math.max(event.height, UiDecorationExtensionPoint.getLookAndFeel().getLogicalGridLayoutRowHeight());
          }
        });
      }

      m_uiColumnOrder = getSwtField().getColumnOrder();
    }
    finally {
      m_redrawHandler.popControlChanging();
    }
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    if (getScoutObject() == null) {
      return;
    }

    //table listener
    if (m_scoutTableListener == null) {
      m_scoutTableListener = new P_ScoutTableListener();
      getScoutObject().addUITableListener(m_scoutTableListener);
    }
    initializeColumns();
    initializeTableModel();
    setHeaderVisibleFromScout(getScoutObject().isHeaderVisible());
    setSelectionFromScout(getScoutObject().getSelectedRows());
    updateKeyStrokeFormScout();
    updateKeyboardNavigationFromScout();
    updateAutoResizeColumnsFromScout();

    if (m_dndSupport == null) {
      m_dndSupport = new P_DndSupport(getScoutObject(), getScoutObject(), getSwtField(), getEnvironment());
    }
    if (m_mouseMoveSelectionSupport == null && UiDecorationExtensionPoint.getLookAndFeel().isTableMouseMoveSelectionSupportEnabled()) {
      m_mouseMoveSelectionSupport = new P_MouseMoveSelectionSupport(getSwtField(), m_dndSupport);
    }

    //handle events from recent history
    final IEventHistory<TableEvent> h = getScoutObject().getEventHistory();
    if (h != null) {
      getEnvironment().getDisplay().asyncExec(new Runnable() {
        @Override
        public void run() {
          for (TableEvent e : h.getRecentEvents()) {
            handleScoutTableEventInSwt(e);
          }
        }
      });
    }
  }

  @Override
  protected void detachScout() {
    super.detachScout();
    if (getScoutObject() != null) {
      if (m_scoutTableListener != null) {
        getScoutObject().removeTableListener(m_scoutTableListener);
        m_scoutTableListener = null;
      }
    }

    removeAutoResizeColumnListener();
  }

  @Override
  public TableEx getSwtField() {
    return (TableEx) super.getSwtField();
  }

  @Override
  public TableViewer getSwtTableViewer() {
    return m_viewer;
  }

  public void setSwtTableViewer(TableViewer viewer) {
    m_viewer = viewer;
  }

  public ITableRow getSwtSelectedRow() {
    return CollectionUtility.firstElement(getSwtSelectedRows());
  }

  public List<ITableRow> getSwtSelectedRows() {
    TreeSet<ITableRow> sortedRows = new TreeSet<ITableRow>(new RowIndexComparator());

    StructuredSelection uiSelection = (StructuredSelection) getSwtTableViewer().getSelection();
    Iterator uiSelectionIt = uiSelection.iterator();
    while (uiSelectionIt.hasNext()) {
      sortedRows.add((ITableRow) uiSelectionIt.next());
    }
    return CollectionUtility.arrayList(sortedRows);
  }

  protected void updateKeyStrokeFormScout() {
    // remove old
    if (m_keyStrokes != null) {
      for (ISwtKeyStroke swtKeyStroke : m_keyStrokes) {
        getEnvironment().removeKeyStroke(getSwtField(), swtKeyStroke);
      }
    }
    // add new
    List<ISwtKeyStroke> newSwtKeyStrokes = new ArrayList<ISwtKeyStroke>();
    for (IKeyStroke scoutKeyStroke : getScoutObject().getKeyStrokes()) {
      ISwtKeyStroke[] swtStrokes = SwtUtility.getKeyStrokes(scoutKeyStroke, getEnvironment());
      for (ISwtKeyStroke swtStroke : swtStrokes) {
        getEnvironment().addKeyStroke(getSwtField(), swtStroke);
        newSwtKeyStrokes.add(swtStroke);
      }
    }
    m_keyStrokes = newSwtKeyStrokes.toArray(new ISwtKeyStroke[newSwtKeyStrokes.size()]);
  }

  protected void updateKeyboardNavigationFromScout() {
    if (getScoutObject().hasKeyboardNavigation()) {
      if (m_keyboardNavigationSupport == null) {
        m_keyboardNavigationSupport = new P_KeyBoardNavigationSupport(getSwtField());
      }
    }
    else {
      if (m_keyboardNavigationSupport != null) {
        m_keyboardNavigationSupport.dispose();
        m_keyboardNavigationSupport = null;
      }
    }
  }

  private void updateAutoResizeColumnsFromScout() {
    if (getSwtField() != null && !getSwtField().getParent().isDisposed()) {
      Composite parent = getSwtField().getParent();
      if (getScoutObject().isAutoResizeColumns()) {
        if (m_autoResizeColumnListener == null) {
          m_autoResizeColumnListener = new P_SwtResizeListener();
          parent.addListener(SWT.Resize, m_autoResizeColumnListener);

          //If the parent has already been resized no event will be fired anymore.
          //So it is necessary to request an auto resizing of the columns manually. (Bugzilla 355855)
          scheduleHandleAutoResizeColumn();
        }
      }
      else {
        removeAutoResizeColumnListener();
      }
    }
  }

  private void scheduleHandleAutoResizeColumn() {
    getSwtField().getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        handleAutoSizeColumns();
      }
    });
  }

  private void removeAutoResizeColumnListener() {
    if (m_autoResizeColumnListener == null) {
      return;
    }

    Composite parent = getSwtField().getParent();
    parent.removeListener(SWT.Resize, m_autoResizeColumnListener);
    m_autoResizeColumnListener = null;
  }

  /**
   * scout property observer
   */
  @Override
  protected void handleScoutPropertyChange(String propName, Object newValue) {
    if (propName.equals(ITable.PROP_HEADER_VISIBLE)) {
      setHeaderVisibleFromScout(((Boolean) newValue).booleanValue());
    }
    else if (propName.equals(ITable.PROP_KEY_STROKES)) {
      updateKeyStrokeFormScout();
    }
    else if (propName.equals(ITable.PROP_KEYBOARD_NAVIGATION)) {
      updateKeyboardNavigationFromScout();
    }
    else if (propName.equals(ITable.PROP_AUTO_RESIZE_COLUMNS)) {
      updateAutoResizeColumnsFromScout();
    }
    else if (propName.equals(ITable.PROP_SCROLL_TO_SELECTION)) {
      updateScrollToSelectionFromScout();
    }
  }

  /**
   * scout table observer
   */
  protected boolean isHandleScoutTableEvent(List<? extends TableEvent> events) {
    for (TableEvent element : events) {
      switch (element.getType()) {
        case TableEvent.TYPE_REQUEST_FOCUS:
        case TableEvent.TYPE_REQUEST_FOCUS_IN_CELL:
        case TableEvent.TYPE_ROWS_INSERTED:
        case TableEvent.TYPE_ROWS_UPDATED:
        case TableEvent.TYPE_ROWS_DELETED:
        case TableEvent.TYPE_ALL_ROWS_DELETED:
        case TableEvent.TYPE_ROW_ORDER_CHANGED:
        case TableEvent.TYPE_ROW_FILTER_CHANGED:
        case TableEvent.TYPE_COLUMN_ORDER_CHANGED:
        case TableEvent.TYPE_COLUMN_HEADERS_UPDATED:
        case TableEvent.TYPE_COLUMN_STRUCTURE_CHANGED:
        case TableEvent.TYPE_ROWS_SELECTED:
        case TableEvent.TYPE_SCROLL_TO_SELECTION: {
          return true;
        }
      }
    }
    return false;
  }

  protected void handleScoutTableEventInSwt(TableEvent e) {
    if (isDisposed()) {
      return;
    }
    SwtScoutTableEvent swtTableEvent = null;
    /*
     * check the scout observer to filter all events that are used here
     * @see isHandleScoutTableEvent()
     */
    switch (e.getType()) {
      case TableEvent.TYPE_REQUEST_FOCUS: {
        getSwtField().setFocus();
        break;
      }
      case TableEvent.TYPE_REQUEST_FOCUS_IN_CELL: {
        // start edit-mode when entering editable cell by traversal key.
        int swtCol = -1;
        TableColumn[] swtColumns = getSwtField().getColumns();
        for (int c = 0; c < swtColumns.length; c++) {
          if (swtColumns[c].getData(KEY_SCOUT_COLUMN) == e.getFirstColumn()) {
            swtCol = c;
            break;
          }
        }
        ITableRow scoutRow = e.getFirstRow();
        if (scoutRow != null && swtCol >= 0) {
          getSwtTableViewer().editElement(scoutRow, swtCol);
        }
        break;
      }
      case TableEvent.TYPE_SCROLL_TO_SELECTION: {
        scrollToSelection();
        break;
      }
      case TableEvent.TYPE_ROWS_DELETED:
        getSwtTableViewer().remove(e.getRows().toArray());
        swtTableEvent = new SwtScoutTableEvent();
        break;
      case TableEvent.TYPE_ALL_ROWS_DELETED:
        getSwtTableViewer().getTable().removeAll();
        swtTableEvent = new SwtScoutTableEvent();
        break;
      case TableEvent.TYPE_ROWS_INSERTED:
      case TableEvent.TYPE_ROWS_UPDATED:
      case TableEvent.TYPE_ROW_FILTER_CHANGED:
      case TableEvent.TYPE_ROW_ORDER_CHANGED: {
        swtTableEvent = new SwtScoutTableEvent();
        break;
      }
      case TableEvent.TYPE_COLUMN_ORDER_CHANGED:
        break;
      case TableEvent.TYPE_COLUMN_HEADERS_UPDATED:
        headerUpdateFromScout();
        break;
      case TableEvent.TYPE_COLUMN_STRUCTURE_CHANGED: {
        // re-install columns
        initializeColumns();
        if (getScoutObject().isAutoResizeColumns()) {
          handleAutoSizeColumns();
        }

        swtTableEvent = new SwtScoutTableEvent();
        break;
      }
      case TableEvent.TYPE_ROWS_SELECTED: {
        setSelectionFromScout(e.getRows());
        break;
      }
    }
    //
    if (swtTableEvent != null) {
      getSwtTableViewer().setInput(getScoutObject().getFilteredRows());
    }
    // refresh selection, indexes might have changed
    switch (e.getType()) {
      case TableEvent.TYPE_ROW_FILTER_CHANGED:
        // Update column title if filter changed (mark column as filtered)
        for (TableColumn swtCol : getSwtField().getColumns()) {
          updateHeaderText(swtCol);
        }
        setSelectionFromScout(e.getTable().getSelectedRows());
        break;
    }
  }

  private void updateHeaderText(TableColumn swtCol) {
    if (swtCol == null) {
      return;
    }
    Object data = swtCol.getData(KEY_SCOUT_COLUMN);
    if (data instanceof IColumn<?>) {
      updateHeaderText(swtCol, (IColumn<?>) data);
    }
  }

  private void updateHeaderText(TableColumn swtCol, IColumn<?> scoutCol) {
    IHeaderCell cell = scoutCol.getHeaderCell();
    String text = cell.getText();
    if (text == null) {
      text = "";
    }
    if (scoutCol.isColumnFilterActive()) {
      text = "((" + text + "))";
    }
    swtCol.setText(text);
  }

  private void updateScrollToSelectionFromScout() {
    if (getScoutObject().isScrollToSelection()) {
      scrollToSelection();
    }
  }

  protected void scrollToSelection() {
    getSwtField().showSelection();
  }

  protected void setHeaderVisibleFromScout(boolean headerVisible) {
    getSwtField().setHeaderVisible(headerVisible);
  }

  @Override
  public void setEnabledFromScout(boolean enabledFromScout) {
    getSwtField().setReadOnly(!enabledFromScout);
  }

  protected void setSelectionFromScout(List<? extends ITableRow> modelSelection) {
    if (getSwtField().isDisposed()) {
      return;
    }
    List<ITableRow> uiSelection = getSwtSelectedRows();
    if (CollectionUtility.equalsCollection(uiSelection, modelSelection)) {
      // no change
      return;
    }
    else {
      if (modelSelection == null) {
        modelSelection = Collections.emptyList();
      }
      getSwtTableViewer().setSelection(new StructuredSelection(modelSelection.toArray(new ITableRow[modelSelection.size()])), true);

      if (CollectionUtility.hasElements(modelSelection)) {
        getSwtTableViewer().reveal(CollectionUtility.firstElement(modelSelection));
      }
    }
    //ticket 96051
    if (getScoutObject().isScrollToSelection()) {
      scrollToSelection();
    }
  }

  protected void setContextColumnFromSwt(TableColumn swtColumn) {
    if (getScoutObject() != null && swtColumn != null) {
      //try to find correct location, since TableColumn has NO x,y and is not a Control!
      Point pDisp = getSwtField().toDisplay(-getSwtField().getHorizontalBar().getSelection(), 0);
      for (TableColumn c : getSwtField().getColumns()) {
        if (c == swtColumn) {
          break;
        }
        pDisp.x += c.getWidth();
      }
      getEnvironment().setPopupOwner(getSwtField(), new Rectangle(pDisp.x - 2, pDisp.y, 1, getSwtField().getHeaderHeight()));
      // notify Scout
      final IColumn finalCol = (IColumn<?>) swtColumn.getData(KEY_SCOUT_COLUMN);
      Runnable t = new Runnable() {
        @Override
        public void run() {
          getScoutObject().getUIFacade().setContextColumnFromUI(finalCol);
        }
      };
      getEnvironment().invokeScoutLater(t, 0);
      // end notify
    }
  }

  private int getVisualCellIndex(TableItem tableItem, int columnIndex) {
    int visualCellIndex = columnIndex;
    final int[] columnOrder = tableItem.getParent().getColumnOrder();
    for (int element : columnOrder) {
      if (element == columnIndex) {
        visualCellIndex = columnIndex;
      }
    }
    return visualCellIndex;
  }

  /**
   * @param p
   *          is the location of the Table control (i.e. not scrollbar adjusted)
   */
  private TableColumn getSwtColumnAt(Point p) {
    Table table = getSwtTableViewer().getTable();
    int x = p.x + getSwtField().getHorizontalBar().getSelection();
    int[] order = table.getColumnOrder();
    for (int index : order) {
      // loop over columns according current display-order
      TableColumn c = table.getColumn(index);
      if (c != null) {
        if (x >= 0 && x <= c.getWidth()) {
          return c;
        }
        x = x - c.getWidth();
      }
    }
    return null;
  }

  protected void setSelectionFromSwt(final StructuredSelection selection) {
    if (getUpdateSwtFromScoutLock().isAcquired()) {
      return;
    }
    //
    if (getScoutObject() != null) {
      // notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          try {
            addIgnoredScoutEvent(TableEvent.class, "" + TableEvent.TYPE_ROWS_SELECTED);
            //
            getScoutObject().getUIFacade().setSelectedRowsFromUI(SwtUtility.getItemsOfSelection(ITableRow.class, selection));
          }
          finally {
            removeIgnoredScoutEvent(TableEvent.class, "" + TableEvent.TYPE_ROWS_SELECTED);
          }
        }
      };
      getEnvironment().invokeScoutLater(t, 0);
      // end notify
    }
  }

  protected void headerUpdateFromScout() {
    getSwtField().setSortColumn(null);

    for (TableColumn col : getSwtField().getColumns()) {
      Object data = col.getData(KEY_SCOUT_COLUMN);
      if (data instanceof IColumn<?>) {
        IColumn<?> cell = (IColumn<?>) data;
        if (cell.isSortExplicit()) {
          getSwtField().setSortColumn(col);
          getSwtField().setSortDirection(cell.isSortAscending() ? SWT.UP : SWT.DOWN);
        }
        updateHeaderText(col);
      }
    }
  }

  protected void handleSwtRowClick(final ITableRow row, final int swtMouseButton) {
    if (getScoutObject() != null) {
      if (row != null) {
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().fireRowClickFromUI(row, SwtUtility.swtToScoutMouseButton(swtMouseButton));
          }
        };
        getEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
  }

  protected void handleSwtRowAction(final ITableRow row) {
    if (getScoutObject() != null) {
      if (!getScoutObject().isCheckable() && row != null) {
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().fireRowActionFromUI(row);
          }
        };
        getEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
  }

  /**
   * TODO: this method is currently not used: When swt table supports styled or html cells, add a hyperlink listener
   * there
   */
  protected void handleSwtHyperlinkAction(final ITableRow row, final IColumn col, final URL url) {
    if (getScoutObject() != null && row != null) {
      // notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          getScoutObject().getUIFacade().fireHyperlinkActionFromUI(row, col, url);
        }
      };
      getEnvironment().invokeScoutLater(t, 0);
      // end notify
    }
  }

  /**
   * <p>
   * Distributes the table width to the columns considering column weights of model. <br/>
   * The initial widths of the columns are used as weights as well as the minimum widths. <br/>
   * Empty space will be distributed weighted.
   * </p>
   */
  protected void handleAutoSizeColumns() {
    if (getSwtField() == null || getSwtField().isDisposed()) {
      return;
    }

    int totalWidth = getSwtField().getClientArea().width;
    if (getSwtField().getVerticalBar() != null && getSwtField().getVerticalBar().getVisible()) {
//      totalWidth -= getSwtField().getVerticalBar().getSize().x;
    }
    if (totalWidth < 32) {
      //either not showing or not yet layouted
      return;
    }
    int totalWeight = 0;
    int actualWidth = 0;
    HashMap<TableColumn, Integer> columnWeights = new HashMap<TableColumn, Integer>();
    for (TableColumn col : getSwtField().getColumns()) {
      if (col == null || col.isDisposed()) {
        continue;
      }
      actualWidth += col.getWidth();
      Object data = col.getData(SwtScoutTable.KEY_SCOUT_COLUMN);
      if (data instanceof IColumn<?> && !((IColumn<?>) data).isFixedWidth()) {
        int width = ((IColumn<?>) data).getInitialWidth();
        columnWeights.put(col, width);
        totalWeight += width;
      }
      else {
        totalWidth -= col.getWidth();
      }
    }

    //If the columns already have the correct size there is no need to recalculate it
    if (actualWidth == totalWidth) {
      return;
    }

    double factor = (double) totalWidth / (double) totalWeight;
    if (factor < 1) {
      factor = 1;
    }
    int i = 0;
    for (Entry<TableColumn, Integer> entry : columnWeights.entrySet()) {
      if (i < columnWeights.size() - 1) {
        int width = (int) (factor * entry.getValue().intValue());
        entry.getKey().setWidth(width);
        totalWidth -= width;
        i++;
      }
      else {
        //Column must not get smaller than initial width.
        int width = Math.max(totalWidth, entry.getValue());
        entry.getKey().setWidth(width);
      }
    }
  }

  protected void handleSwtColumnResized(TableColumn column) {
    if (column.isDisposed()) {
      return;
    }
    if (!column.getParent().isVisible()) {
      return;
    }
    if (getUpdateSwtFromScoutLock().isAcquired()) {
      return;
    }
    //
    final int width = column.getWidth();
    final IColumn<?> scoutColumn = (IColumn<?>) column.getData(KEY_SCOUT_COLUMN);
    if (scoutColumn != null) {
      if (scoutColumn.getWidth() != width) {

        //Cancel already scheduled resize job to protect the model from too many resize events.
        if (m_storeColumnWidthsJob != null) {
          m_storeColumnWidthsJob.cancel();
        }

        /*
         * imo, 04.06.2009: added swt-side optimistic lock check Method
         * autoSizeColumns indirectly calls this code by setting the width on a
         * table column if this code calls scout using synchronizer it may
         * invoke pending swt jobs while waiting, which can execute a dispose
         * form job. After that job all columns are disposed. This results in
         * WidgetDisposed exceptions on line with entry.getKey().setWidth(...)
         */
        m_storeColumnWidthsJob = new ClientSyncJob("Store column widths", getEnvironment().getClientSession()) {
          @Override
          protected IStatus runStatus(IProgressMonitor monitor) {
            try {
              addIgnoredScoutEvent(TableEvent.class, "" + TableEvent.TYPE_COLUMN_STRUCTURE_CHANGED);
              //
              getScoutObject().getUIFacade().setColumnWidthFromUI(scoutColumn, width);
            }
            finally {
              removeIgnoredScoutEvent(TableEvent.class, "" + TableEvent.TYPE_COLUMN_STRUCTURE_CHANGED);
            }

            return Status.OK_STATUS;
          }
        };
        m_storeColumnWidthsJob.schedule(400);
      }
    }
  }

  protected TransferObject handleSwtCopyRequest() {
    if (getUpdateSwtFromScoutLock().isAcquired()) {
      return null;
    }
    final Holder<TransferObject> result = new Holder<TransferObject>(TransferObject.class, null);
    if (getScoutObject() != null) {
      // notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          if (!ACCESS.check(new CopyToClipboardPermission())) {
            return;
          }
          TransferObject scoutTransferable = getScoutObject().getUIFacade().fireRowsCopyRequestFromUI();
          result.setValue(scoutTransferable);
        }
      };
      try {
        getEnvironment().invokeScoutLater(t, 20000).join(20000);
      }
      catch (InterruptedException e) {
        //nop
      }
      // end notify
    }
    return result.getValue();
  }

  protected void handleSwtColumnMoved() {
    if (getUpdateSwtFromScoutLock().isAcquired()) {
      return;
    }
    int[] uiColumnOrder = getSwtField().getColumnOrder();
    // do not allow to reorder icon and dummy column
    if (uiColumnOrder[0] != 0) {
      getSwtField().setColumnOrder(m_uiColumnOrder);
      return;
    }
    // if column with icon has changed position
    if (uiColumnOrder[1] != m_uiColumnOrder[1] && getScoutObject().getRowCount() > 0 && StringUtility.hasText(getScoutObject().getRow(0).getIconId())) {
      getSwtField().setRedraw(false);
      try {
        // Disable redraw during the time of refresh to omit flickering and boost performance.
        getSwtTableViewer().refresh();
      }
      finally {
        getSwtField().setRedraw(true);
      }
    }
    int[] truncatedColOrder = new int[uiColumnOrder.length - 1];
    for (int i = 0; i < truncatedColOrder.length; i++) {
      truncatedColOrder[i] = uiColumnOrder[i + 1] - 1;
    }
    final List<IColumn<?>> newOrder = m_columnManager.getOrderedColumns(truncatedColOrder);
    if (m_columnManager.applyNewOrder(newOrder)) {
      m_uiColumnOrder = uiColumnOrder;
      // notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          try {
            addIgnoredScoutEvent(TableEvent.class, "" + TableEvent.TYPE_COLUMN_ORDER_CHANGED);
            addIgnoredScoutEvent(TableEvent.class, "" + TableEvent.TYPE_COLUMN_STRUCTURE_CHANGED);
            //
            getScoutObject().getUIFacade().fireVisibleColumnsChangedFromUI(newOrder);
          }
          finally {
            removeIgnoredScoutEvent(TableEvent.class, "" + TableEvent.TYPE_COLUMN_ORDER_CHANGED);
            removeIgnoredScoutEvent(TableEvent.class, "" + TableEvent.TYPE_COLUMN_STRUCTURE_CHANGED);
          }
        }
      };
      getEnvironment().invokeScoutLater(t, 0);
      // end notify
    }
  }

  protected void handleKeyboardNavigationFromSwt(TableItem item) {
    if (getScoutObject().isCheckable()) {
      //nop
      return;
    }
    getSwtField().setSelection(item);
    Event selectionEvent = new Event();
    selectionEvent.type = SWT.DefaultSelection;
    selectionEvent.widget = getSwtField();
    for (Listener l : getSwtField().getListeners(SWT.DefaultSelection)) {
      l.handleEvent(selectionEvent);
    }
  }

  private class P_TableViewerEx extends TableViewer {

    public P_TableViewerEx(Table table) {
      super(table);
    }

    @Override
    public void applyEditorValue() { // public visibility to close editor in case of an empty row click.
      super.applyEditorValue();
    }
  }

  private class P_ScoutTableListener implements TableListener {
    @Override
    public void tableChanged(final TableEvent event) {
      if (isHandleScoutTableEvent(CollectionUtility.arrayList(event))) {
        if (isIgnoredScoutEvent(TableEvent.class, "" + event.getType())) {
          return;
        }
        Runnable t = new Runnable() {
          @Override
          public void run() {
            try {
              getUpdateSwtFromScoutLock().acquire();
              //
              handleScoutTableEventInSwt(event);
            }
            finally {
              getUpdateSwtFromScoutLock().release();
            }
          }
        };
        getEnvironment().invokeSwtLater(t);
      }
    }

    @Override
    public void tableChangedBatch(final List<? extends TableEvent> a) {
      if (isHandleScoutTableEvent(a)) {
        final List<TableEvent> filteredList = new ArrayList<TableEvent>();
        for (TableEvent event : a) {
          if (!isIgnoredScoutEvent(TableEvent.class, "" + event.getType())) {
            filteredList.add(event);
          }
        }
        if (CollectionUtility.hasElements(filteredList)) {
          Runnable t = new Runnable() {
            @Override
            public void run() {
              if (isDisposed()) {
                return;
              }
              m_redrawHandler.pushControlChanging();
              try {
                try {
                  getUpdateSwtFromScoutLock().acquire();
                  //
                  for (TableEvent element : filteredList) {
                    handleScoutTableEventInSwt(element);
                  }
                }
                finally {
                  getUpdateSwtFromScoutLock().release();
                }
              }
              finally {
                m_redrawHandler.popControlChanging();
              }
            }
          };
          getEnvironment().invokeSwtLater(t);
        }
      }
    }
  }// end P_ScoutTableListener

  private class P_SwtTableListener implements Listener {
    @Override
    public void handleEvent(Event event) {
      TableViewer swtTableViewer = getSwtTableViewer();
      switch (event.type) {
        case SWT.MouseDown: {
          // Close cell editor on empty space click.
          if (swtTableViewer.isCellEditorActive() && swtTableViewer.getTable().getItem(new Point(event.y, event.y)) == null) {
            ((P_TableViewerEx) swtTableViewer).applyEditorValue();
          }

          setContextColumnFromSwt(getSwtColumnAt(new Point(event.x, event.y)));
          if (getSwtField().getItem(new Point(event.x, event.y)) == null) {
            swtTableViewer.setSelection(null);
            setSelectionFromSwt(new StructuredSelection());
          }
          break;
        }
        case SWT.MouseUp: {
          if (event.count == 1) {
            StructuredSelection selection = (StructuredSelection) swtTableViewer.getSelection();
            if (selection.size() == 1) {
              handleSwtRowClick((ITableRow) selection.getFirstElement(), event.button);
            }
          }
          break;
        }
        case SWT.MouseDoubleClick: {
          StructuredSelection selection = (StructuredSelection) swtTableViewer.getSelection();
          if (selection.size() == 1) {
            handleSwtRowAction((ITableRow) selection.getFirstElement());
          }
          break;
        }
        case SWT.MenuDetect: {
          Point pt = getSwtField().getDisplay().map(null, getSwtField(), new Point(event.x, event.y));
          Rectangle clientArea = getSwtField().getClientArea();
          boolean header = clientArea.y <= pt.y && pt.y < clientArea.y + getSwtField().getHeaderHeight();
          getSwtField().setMenu(header ? m_headerMenu : m_contextMenu);
          break;
        }
        case SWT.KeyUp: {
          if (event.doit && getScoutObject().isCheckable()) {
            if (event.stateMask == 0) {
              switch (event.keyCode) {
                case ' ':
                case SWT.CR:
                  List<ITableRow> selectedRows = SwtUtility.getItemsOfSelection(ITableRow.class, (StructuredSelection) swtTableViewer.getSelection());
                  if (CollectionUtility.hasElements(selectedRows)) {
                    handleSwtRowClick(CollectionUtility.firstElement(selectedRows), event.button);
                  }
                  event.doit = false;
                  break;
              }
            }
          }
          break;
        }
      }
    }
  }

  private class P_SwtResizeListener implements Listener {
    @Override
    public void handleEvent(Event event) {
      //lazy column auto-fit
      if (getSwtField() != null && !getSwtField().isDisposed()) {
        scheduleHandleAutoResizeColumn();
      }
    }
  } // end class P_SwtResizeListener

  public class P_SwtSelectionListener implements ISelectionChangedListener {
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      setSelectionFromSwt((StructuredSelection) event.getSelection());
    }

  } // end class P_SwtSelectionListener

  private class P_TableColumnListener implements Listener {
    @Override
    public void handleEvent(Event event) {
      switch (event.type) {
        case SWT.Move:
          handleSwtColumnMoved();
          break;
        case SWT.Resize:
          if (event.widget instanceof TableColumn) {
            handleSwtColumnResized((TableColumn) event.widget);
          }

      }
    }

  } // end class P_TableColumnMoveListener

  private class P_ColumnSortListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      /*
       * TODO multi column search does work with control mask. The selection
       * event on a table column does not provide any key mask nor right mouse
       * click.
       */
      TableColumn col = (TableColumn) e.getSource();
      setContextColumnFromSwt(col);
      final IColumn<?> newColumn = (IColumn<?>) col.getData(KEY_SCOUT_COLUMN);
      if (getScoutObject() != null) {
        Runnable job = new Runnable() {
          @Override
          public void run() {
            // use always memory sort since SWT does not provide a keymask on
            // selection events
            getScoutObject().getUIFacade().fireHeaderSortFromUI(newColumn, false);
          }
        };
        getEnvironment().invokeScoutLater(job, 0);
      }
    }
  } // end class P_ColumnSortListener

  private class P_SwtHeaderMenuDetectListener implements MenuDetectListener {

    @Override
    public void menuDetected(MenuDetectEvent event) {
      Table table = getSwtField();
      Point pTable = table.getDisplay().map(null, table, new Point(event.x, event.y));
      Rectangle clientArea = table.getClientArea();
      boolean header = clientArea.y <= pTable.y && pTable.y < clientArea.y + table.getHeaderHeight();
      if (!header) {
        return;
      }
      // clear all previous
      // Windows BUG: fires menu hide before the selection on the menu item is
      // propagated.
      if (m_headerMenu != null) {
        for (MenuItem item : m_headerMenu.getItems()) {
          disposeMenuItem(item);
        }
      }
      setContextColumnFromSwt(getSwtColumnAt(pTable));
      Runnable t = new Runnable() {
        @Override
        public void run() {
          IActionFilter aboutToShowFilter = ActionUtility.createMenuFilterMenuTypes(CollectionUtility.hashSet(TableMenuType.EmptySpace, TableMenuType.Header), false);
          IContextMenu contextMenu = getScoutObject().getContextMenu();
          contextMenu.callAboutToShow(aboutToShowFilter);
        }
      };
      JobEx job = getEnvironment().invokeScoutLater(t, 1200);
      try {
        job.join(1200);
      }
      catch (InterruptedException ex) {
        //nop
      }
      // grab the actions out of the job, when the actions are providden
      // within the scheduled time the popup will be handled.
      IActionFilter displayFilter = ActionUtility.createMenuFilterMenuTypes(CollectionUtility.hashSet(TableMenuType.EmptySpace, TableMenuType.Header), true);
      SwtMenuUtility.fillMenu(m_headerMenu, getScoutObject().getContextMenu().getChildActions(), displayFilter, getEnvironment());
    }

    private void disposeMenuItem(MenuItem item) {
      Menu menu = item.getMenu();
      if (menu != null) {
        for (MenuItem childItem : menu.getItems()) {
          disposeMenuItem(childItem);
        }
        menu.dispose();
      }
      item.dispose();
    }

  } // end class P_HeaderMenuListener

  private class P_DndSupport extends AbstractSwtScoutDndSupport {
    public P_DndSupport(IPropertyObserver scoutObject, IDNDSupport scoutDndSupportable, Control control, ISwtEnvironment environment) {
      super(scoutObject, scoutDndSupportable, control, environment);
    }

    @Override
    protected boolean acceptDrag() {
      if (m_mouseMoveSelectionSupport != null) {
        return m_mouseMoveSelectionSupport.acceptDrag();
      }

      return super.acceptDrag();
    }

    @Override
    protected TransferObject handleSwtDragRequest() {
      final Holder<TransferObject> result = new Holder<TransferObject>(TransferObject.class, null);
      Runnable t = new Runnable() {
        @Override
        public void run() {
          TransferObject scoutTransferable = getScoutObject().getUIFacade().fireRowsDragRequestFromUI();
          result.setValue(scoutTransferable);
        }
      };
      try {
        getEnvironment().invokeScoutLater(t, 20000).join(20000);
      }
      catch (InterruptedException e) {
        //nop
      }
      return result.getValue();
    }

    @Override
    protected void handleSwtDropAction(DropTargetEvent event, final TransferObject scoutTransferObject) {
      Object dropTarget = event.item != null ? event.item.getData() : null;
      final ITableRow row = dropTarget instanceof ITableRow ? (ITableRow) dropTarget : null;
      Runnable job = new Runnable() {
        @Override
        public void run() {
          getScoutObject().getUIFacade().fireRowDropActionFromUI(row, scoutTransferObject);
        }
      };
      getEnvironment().invokeScoutLater(job, 200);
    }
  }// end class P_DndSupport

  private class P_KeyBoardNavigationSupport extends TableKeyboardNavigationSupport {
    /**
     * @param table
     * @param delay
     */
    public P_KeyBoardNavigationSupport(TableEx table) {
      super(table);
    }

    @Override
    void handleKeyboardNavigation(TableItem tableItem) {
      handleKeyboardNavigationFromSwt(tableItem);
    }
  } // P_KeyBoardNavigationSupport

  private class P_MouseMoveSelectionSupport extends TableMouseMoveSelectionSupport {

    public P_MouseMoveSelectionSupport(Table table, ISwtScoutDndSupport dndSupport) {
      super(table, dndSupport);
    }

    @Override
    protected void selectingStopped() {
      //Selection event seems not to be fired properly after selecting the items so it's necessary to manually call setSelectionFromSwt
      StructuredSelection selection = (StructuredSelection) getSwtTableViewer().getSelection();
      setSelectionFromSwt(selection);
    }

  }
}
