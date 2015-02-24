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
package org.eclipse.scout.rt.client.ui.form.fields.tablefield;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.scout.commons.BooleanUtility;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.NumberUtility;
import org.eclipse.scout.commons.XmlUtility;
import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.FormData;
import org.eclipse.scout.commons.annotations.FormData.DefaultSubtypeSdkCommand;
import org.eclipse.scout.commons.annotations.FormData.SdkCommand;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.status.IStatus;
import org.eclipse.scout.commons.status.Status;
import org.eclipse.scout.rt.client.extension.ui.form.fields.IFormFieldExtension;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tablefield.ITableFieldExtension;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tablefield.TableFieldChains.TableFieldReloadTableDataChain;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tablefield.TableFieldChains.TableFieldSaveChain;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tablefield.TableFieldChains.TableFieldSaveDeletedRowChain;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tablefield.TableFieldChains.TableFieldSaveInsertedRowChain;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tablefield.TableFieldChains.TableFieldSaveUpdatedRowChain;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tablefield.TableFieldChains.TableFieldUpdateTableStatusChain;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.MenuUtility;
import org.eclipse.scout.rt.client.ui.basic.cell.ICell;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableAdapter;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.INumberColumn;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;
import org.eclipse.scout.rt.client.ui.form.fields.IValidateContentDescriptor;
import org.eclipse.scout.rt.client.ui.form.fields.ValidateFormFieldDescriptor;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractFormFieldData;
import org.eclipse.scout.rt.shared.data.form.fields.tablefield.AbstractTableFieldBeanData;
import org.eclipse.scout.rt.shared.data.form.fields.tablefield.AbstractTableFieldData;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;
import org.w3c.dom.Element;

@ClassId("76887bde-6815-4f7d-9cbd-60409b49488d")
@FormData(value = AbstractTableFieldBeanData.class, sdkCommand = SdkCommand.USE, defaultSubtypeSdkCommand = DefaultSubtypeSdkCommand.CREATE)
public abstract class AbstractTableField<T extends ITable> extends AbstractFormField implements ITableField<T> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractTableField.class);

  private T m_table;
  private boolean m_tableExternallyManaged;
  private P_ManagedTableListener m_managedTableListener;
  private P_TableStatusListener m_tableStatusListener;

  public AbstractTableField() {
    this(true);
  }

  public AbstractTableField(boolean callInitializer) {
    super(callInitializer);
  }

  /*
   * Configuration
   */

  /**
   * Called when the table data is reloaded, i.e. when {@link #reloadTableData()} is called.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(190)
  protected void execReloadTableData() throws ProcessingException {
  }

  /**
   * @return the visible row count, filtered row count, selected row count and the sum of all numeric columns
   *         <p>
   *         returns null if no table is contained within this table field
   */
  @Override
  public String createDefaultTableStatus() {
    StringBuilder statusText = new StringBuilder();
    ITable table = getTable();
    if (table != null) {
      int nTotal = table.getFilteredRowCount();
      if (nTotal == 1) {
        statusText.append(ScoutTexts.get("OneRow"));
      }
      else {
        statusText.append(ScoutTexts.get("XRows", NumberUtility.format(nTotal)));
      }

      int fTotal = table.getRowCount() - nTotal;
      if (fTotal == 1) {
        statusText.append(", " + ScoutTexts.get("OneFiltered"));
      }
      else if (fTotal > 1) {
        statusText.append(", " + ScoutTexts.get("XFiltered", NumberUtility.format(fTotal)));
      }
      int nSel = table.getSelectedRowCount();
      if (nSel == 1) {
        statusText.append(", " + ScoutTexts.get("OneSelected"));
      }
      else if (nSel > 1) {
        statusText.append(", " + ScoutTexts.get("XSelected", NumberUtility.format(nSel)));
        // show sums of numeric columns
        for (IColumn<?> c : table.getColumnSet().getVisibleColumns()) {
          if (c instanceof INumberColumn) {
            NumberFormat fmt = null;
            Object sum = null;
            fmt = ((INumberColumn) c).getFormat();
            @SuppressWarnings("unchecked")
            INumberColumn<? extends Number> numberColumn = (INumberColumn) c;
            sum = NumberUtility.sum(numberColumn.getSelectedValues());
            if (fmt != null && sum != null) {
              statusText.append(", " + c.getHeaderCell().getText() + ": " + fmt.format(sum));
            }
          }
        }
      }
    }
    if (statusText.length() == 0) {
      return null;
    }
    return statusText.toString();
  }

  /**
   * Called when the table status is updated, i.e. when {@link #updateTableStatus()} is called due to a change in the
   * table (rows inserted, deleted, selected, ...).
   * <p>
   * Subclasses can override this method. The default calls {@link #createDefaultTableStatus()} and
   * {@link #setTableStatus(String)} if the table status is visible.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(195)
  protected void execUpdateTableStatus() {
    if (!isTableStatusVisible()) {
      return;
    }
    setTableStatus(createDefaultTableStatus());
  }

  /**
   * Called for batch processing when the table is saved. See {@link #doSave()} for the call order.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(200)
  protected void execSave(List<? extends ITableRow> insertedRows, List<? extends ITableRow> updatedRows, List<? extends ITableRow> deletedRows) {
  }

  /**
   * Called to handle deleted rows when the table is saved. See {@link #doSave()} for the call order.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(210)
  protected void execSaveDeletedRow(ITableRow row) throws ProcessingException {
  }

  /**
   * Called to handle inserted rows when the table is saved. See {@link #doSave()} for the call order.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(220)
  protected void execSaveInsertedRow(ITableRow row) throws ProcessingException {
  }

  /**
   * Called to handle updated rows when the table is saved. See {@link #doSave()} for the call order.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(230)
  protected void execSaveUpdatedRow(ITableRow row) throws ProcessingException {
  }

  @Override
  protected void execChangedMasterValue(Object newMasterValue) throws ProcessingException {
    reloadTableData();
  }

  /**
   * Configures the visibility of the table status.
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   *
   * @return {@code true} if the table status is visible, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(200)
  protected boolean getConfiguredTableStatusVisible() {
    return false;
  }

  protected Class<? extends ITable> getConfiguredTable() {
    Class<?>[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    List<Class<ITable>> f = ConfigurationUtility.filterClasses(dca, ITable.class);
    if (f.size() == 1) {
      return CollectionUtility.firstElement(f);
    }
    else {
      for (Class<? extends ITable> c : f) {
        if (c.getDeclaringClass() != AbstractTableField.class) {
          return c;
        }
      }
      return null;
    }
  }

  @Override
  protected double getConfiguredGridWeightY() {
    return 1;
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    setTableStatusVisible(getConfiguredTableStatusVisible());
    setTableInternal(createTable());
    // local enabled listener
    addPropertyChangeListener(PROP_ENABLED, new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        if (m_table != null) {
          m_table.setEnabled(isEnabled());
        }
      }
    });
  }

  @SuppressWarnings("unchecked")
  protected T createTable() {
    List<ITable> contributedFields = m_contributionHolder.getContributionsByClass(ITable.class);
    ITable result = CollectionUtility.firstElement(contributedFields);
    if (result != null) {
      return (T) result;
    }

    Class<? extends ITable> configuredTable = getConfiguredTable();
    if (configuredTable != null) {
      try {
        return (T) ConfigurationUtility.newInnerInstance(this, configuredTable);
      }
      catch (Exception e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + configuredTable.getName() + "'.", e));
      }
    }
    return null;
  }

  /*
   * Runtime
   */

  @Override
  protected void initFieldInternal() throws ProcessingException {
    if (m_table != null && !m_tableExternallyManaged) {
      m_table.initTable();
    }
    super.initFieldInternal();
  }

  @Override
  protected void disposeFieldInternal() {
    super.disposeFieldInternal();
    if (m_table != null && !m_tableExternallyManaged) {
      m_table.disposeTable();
    }
  }

  @Override
  public final T getTable() {
    return m_table;
  }

  @Override
  public void setTable(T newTable, boolean externallyManaged) {
    m_tableExternallyManaged = externallyManaged;
    setTableInternal(newTable);
  }

  private void setTableInternal(T table) {
    if (m_table == table) {
      return;
    }
    if (m_table instanceof AbstractTable) {
      ((AbstractTable) m_table).setContainerInternal(null);
    }
    if (m_table != null) {
      if (!m_tableExternallyManaged) {
        if (m_managedTableListener != null) {
          m_table.removeTableListener(m_managedTableListener);
          m_managedTableListener = null;
        }
      }
      if (m_tableStatusListener != null) {
        m_table.removeTableListener(m_tableStatusListener);
        m_tableStatusListener = null;
      }
    }
    m_table = table;
    if (m_table instanceof AbstractTable) {
      ((AbstractTable) m_table).setContainerInternal(this);
    }
    if (m_table != null) {
      if (!m_tableExternallyManaged) {
        // ticket 84893
        // m_table.setAutoDiscardOnDelete(false);
        m_managedTableListener = new P_ManagedTableListener();
        m_table.addTableListener(m_managedTableListener);
      }
      m_tableStatusListener = new P_TableStatusListener();
      m_table.addTableListener(m_tableStatusListener);
      updateTableStatus();
      m_table.setEnabled(isEnabled());
    }
    boolean changed = propertySupport.setProperty(PROP_TABLE, m_table);
    if (changed) {
      if (getForm() != null) {
        getForm().structureChanged(this);
      }
      updateKeyStrokes();
    }
  }

  @Override
  public void exportFormFieldData(AbstractFormFieldData target) throws ProcessingException {
    if (m_table != null) {
      if (target instanceof AbstractTableFieldData) {
        AbstractTableFieldData tableFieldData = (AbstractTableFieldData) target;
        m_table.extractTableData(tableFieldData);
      }
      else if (target instanceof AbstractTableFieldBeanData) {
        AbstractTableFieldBeanData tableBeanData = (AbstractTableFieldBeanData) target;
        m_table.exportToTableBeanData(tableBeanData);
        target.setValueSet(true);
      }
    }
  }

  @Override
  public void importFormFieldData(AbstractFormFieldData source, boolean valueChangeTriggersEnabled) throws ProcessingException {
    if (source.isValueSet()) {
      if (m_table != null) {
        try {
          if (!valueChangeTriggersEnabled) {
            setValueChangeTriggerEnabled(false);
          }
          if (source instanceof AbstractTableFieldData) {
            AbstractTableFieldData tableFieldData = (AbstractTableFieldData) source;
            m_table.updateTable(tableFieldData);
          }
          else if (source instanceof AbstractTableFieldBeanData) {
            AbstractTableFieldBeanData tableBeanData = (AbstractTableFieldBeanData) source;
            m_table.importFromTableBeanData(tableBeanData);
          }
          if (m_table.isCheckable()
              && m_table.getCheckableColumn() != null) {
            for (ITableRow row : m_table.getRows()) {
              row.setChecked(BooleanUtility.nvl(m_table.getCheckableColumn().getValue(row)));
            }
          }
        }
        finally {
          if (!valueChangeTriggersEnabled) {
            setValueChangeTriggerEnabled(true);
          }
        }
      }
    }
  }

  @Override
  public void loadFromXml(Element x) throws ProcessingException {
    super.loadFromXml(x);
    if (m_table != null) {
      int[] selectedRowIndices = null;
      try {
        selectedRowIndices = (int[]) XmlUtility.getObjectAttribute(x, "selectedRowIndices");
      }
      catch (Exception e) {
        LOG.warn("reading attribute 'selectedRowIndices'", e);
      }
      Object[][] dataMatrix = null;
      try {
        dataMatrix = (Object[][]) XmlUtility.getObjectAttribute(x, "rows");
      }
      catch (Exception e) {
        LOG.warn("reading attribute 'rows'", e);
      }
      m_table.discardAllRows();
      if (dataMatrix != null && dataMatrix.length > 0) {
        m_table.addRowsByMatrix(dataMatrix);
      }
      if (selectedRowIndices != null && selectedRowIndices.length > 0) {
        m_table.selectRows(m_table.getRows(selectedRowIndices));
      }
    }
  }

  @Override
  public void storeToXml(Element x) throws ProcessingException {
    super.storeToXml(x);
    if (m_table != null) {
      List<ITableRow> selectedRows = m_table.getSelectedRows();
      int[] selectedRowIndices = new int[selectedRows.size()];
      int i = 0;
      for (ITableRow selrow : selectedRows) {
        selectedRowIndices[i] = selrow.getRowIndex();
        i++;
      }
      try {
        XmlUtility.setObjectAttribute(x, "selectedRowIndices", selectedRowIndices);
      }
      catch (Exception e) {
        LOG.warn("writing attribute 'selectedRowIndices'", e);
      }
      Object[][] dataMatrix = m_table.getTableData();
      for (int r = 0; r < dataMatrix.length; r++) {
        for (int c = 0; c < dataMatrix[r].length; c++) {
          Object o = dataMatrix[r][c];
          if (o != null && !(o instanceof Serializable)) {
            LOG.warn("ignoring not serializable value at row=" + r + ", col=" + c + ": " + o + "[" + o.getClass() + "]");
            dataMatrix[r][c] = null;
          }
        }
      }
      try {
        XmlUtility.setObjectAttribute(x, "rows", dataMatrix);
      }
      catch (Exception e) {
        LOG.warn("writing attribute 'rows'", e);
      }
    }
  }

  @Override
  protected boolean execIsSaveNeeded() throws ProcessingException {
    boolean b = false;
    if (m_table != null && !m_tableExternallyManaged) {
      if (b == false && m_table.getDeletedRowCount() > 0) {
        b = true;
      }
      if (b == false && m_table.getInsertedRowCount() > 0) {
        b = true;
      }
      if (b == false && m_table.getUpdatedRowCount() > 0) {
        b = true;
      }
    }
    return b;
  }

  @Override
  protected void execMarkSaved() throws ProcessingException {
    super.execMarkSaved();
    if (m_table != null && !m_tableExternallyManaged) {
      try {
        m_table.setTableChanging(true);
        //
        for (int i = 0; i < m_table.getRowCount(); i++) {
          ITableRow row = m_table.getRow(i);
          if (!row.isStatusNonchanged()) {
            row.setStatusNonchanged();
          }
        }
        m_table.discardAllDeletedRows();
      }
      finally {
        m_table.setTableChanging(false);
      }
    }
  }

  @Override
  protected boolean execIsEmpty() throws ProcessingException {
    if (m_table != null) {
      return m_table.getRowCount() == 0;
    }
    else {
      return true;
    }
  }

  @Override
  public IValidateContentDescriptor validateContent() {
    IValidateContentDescriptor desc = super.validateContent();
    //super check
    if (desc != null) {
      return desc;
    }
    //check mandatory
    ITable table = getTable();
    if (isMandatory()) {
      if (table == null || table.getRowCount() < 1) {
        return new ValidateFormFieldDescriptor(this);
      }
    }
    //make editable columns visible if check fails
    HashSet<IColumn<?>> invisbleColumnsWithErrors = new HashSet<IColumn<?>>();
    //check editable cells
    ValidateTableFieldDescriptor tableDesc = null;
    TreeSet<String> columnNames = new TreeSet<String>();
    if (table != null) {
      for (ITableRow row : table.getRows()) {
        for (IColumn col : table.getColumns()) {
          if (col.isCellEditable(row)) {
            try {
              ICell cell = row.getCell(col);
              if (cell.getErrorStatus() != null) {
                if (col.isDisplayable() && !col.isVisible()) {
                  //column should become visible
                  invisbleColumnsWithErrors.add(col);
                }
                if (tableDesc == null) {
                  tableDesc = new ValidateTableFieldDescriptor(this, row, col);
                }
                columnNames.add(col.getHeaderCell().getText());
              }
            }
            catch (Throwable t) {
              LOG.error("validating " + getClass().getSimpleName() + " for row " + row.getRowIndex() + " for column " + col.getClass().getSimpleName(), t);
            }
          }
        }
      }
    }
    //make invalid invisible columns visible again
    for (IColumn col : invisbleColumnsWithErrors) {
      col.setVisible(true);
    }
    if (tableDesc == null) {
      return null;
    }
    tableDesc.setDisplayText(ScoutTexts.get("TableName") + " " + getLabel() + ": " + CollectionUtility.format(columnNames));
    return tableDesc;
  }

  @Override
  public String getTableStatus() {
    IStatus status = getTableSelectionStatus();
    return status != null ? status.getMessage() : null;
  }

  @Override
  public void setTableStatus(String status) {
    setTableSelectionStatus(status != null ? new Status(status, IStatus.INFO) : null);
  }

  @Override
  public IStatus getTableSelectionStatus() {
    return (IStatus) propertySupport.getProperty(PROP_TABLE_SELECTION_STATUS);
  }

  @Override
  public void setTableSelectionStatus(IStatus status) {
    propertySupport.setProperty(PROP_TABLE_SELECTION_STATUS, status);
  }

  @Override
  public IStatus getTablePopulateStatus() {
    return (IStatus) propertySupport.getProperty(PROP_TABLE_POPULATE_STATUS);
  }

  @Override
  public void setTablePopulateStatus(IStatus status) {
    propertySupport.setProperty(PROP_TABLE_POPULATE_STATUS, status);
  }

  @Override
  public boolean isTableStatusVisible() {
    return propertySupport.getPropertyBool(PROP_TABLE_STATUS_VISIBLE);
  }

  @Override
  public void setTableStatusVisible(boolean b) {
    propertySupport.setPropertyBool(PROP_TABLE_STATUS_VISIBLE, b);
    if (b) {
      updateTableStatus();
    }
  }

  @Override
  public void updateTableStatus() {
    try {
      interceptUpdateTableStatus();
    }
    catch (Throwable t) {
      LOG.warn("Updating status of " + AbstractTableField.this.getClass().getName(), t);
    }
  }

  /**
   * Saves the table. The call order is as follows:
   * <ol>
   * <li>{@link #interceptSave(ITableRow[], ITableRow[], ITableRow[])}</li>
   * <li>{@link #interceptSaveDeletedRow(ITableRow)}</li>
   * <li>{@link #interceptSaveInsertedRow(ITableRow)}</li>
   * <li>{@link #interceptSaveUpdatedRow(ITableRow)}</li>
   * </ol>
   */
  @Override
  public void doSave() throws ProcessingException {
    if (m_table != null && !m_tableExternallyManaged) {
      try {
        m_table.setTableChanging(true);
        //
        // 1. batch
        interceptSave(m_table.getInsertedRows(), m_table.getUpdatedRows(), m_table.getDeletedRows());
        // 2. per row
        // deleted rows
        for (ITableRow deletedRow : m_table.getDeletedRows()) {
          interceptSaveDeletedRow(deletedRow);
        }
        // inserted rows
        for (ITableRow insertedRow : m_table.getInsertedRows()) {
          interceptSaveInsertedRow(insertedRow);
          insertedRow.setStatusNonchanged();
          m_table.updateRow(insertedRow);
        }
        // updated rows
        for (ITableRow updatedRow : m_table.getUpdatedRows()) {
          interceptSaveUpdatedRow(updatedRow);
          updatedRow.setStatusNonchanged();
          m_table.updateRow(updatedRow);
        }
      }
      finally {
        m_table.setTableChanging(false);
      }
    }
    markSaved();
  }

  /**
   * Reloads the table data.
   * <p>
   * The default implementation calls {@link #interceptReloadTableData()}.
   */
  @Override
  public void reloadTableData() throws ProcessingException {
    interceptReloadTableData();
  }

  @Override
  public List<IKeyStroke> getContributedKeyStrokes() {
    if (getTable() != null) {
      return MenuUtility.getKeyStrokesFromMenus(getTable().getMenus());
    }
    return CollectionUtility.emptyArrayList();
  }

  private class P_ManagedTableListener extends TableAdapter {
    @Override
    public void tableChanged(TableEvent e) {
      switch (e.getType()) {
        case TableEvent.TYPE_ALL_ROWS_DELETED:
        case TableEvent.TYPE_ROWS_DELETED:
        case TableEvent.TYPE_ROWS_INSERTED:
        case TableEvent.TYPE_ROWS_UPDATED:
        case TableEvent.TYPE_COLUMN_STRUCTURE_CHANGED: {
          checkSaveNeeded();
          checkEmpty();
          break;
        }
      }
    }
  }

  private class P_TableStatusListener extends TableAdapter {
    @Override
    public void tableChanged(TableEvent e) {
      switch (e.getType()) {
        case TableEvent.TYPE_ROWS_INSERTED:
        case TableEvent.TYPE_ROWS_UPDATED:
        case TableEvent.TYPE_ROWS_DELETED:
        case TableEvent.TYPE_ROWS_SELECTED:
        case TableEvent.TYPE_ALL_ROWS_DELETED:
        case TableEvent.TYPE_ROW_FILTER_CHANGED:
        case TableEvent.TYPE_TABLE_POPULATED: {
          updateTableStatus();
          break;
        }
      }
    }
  }

  protected static class LocalTableFieldExtension<T extends ITable, OWNER extends AbstractTableField<T>> extends LocalFormFieldExtension<OWNER> implements ITableFieldExtension<T, OWNER> {

    public LocalTableFieldExtension(OWNER owner) {
      super(owner);
    }

    @Override
    public void execReloadTableData(TableFieldReloadTableDataChain<? extends ITable> chain) throws ProcessingException {
      getOwner().execReloadTableData();
    }

    @Override
    public void execUpdateTableStatus(TableFieldUpdateTableStatusChain<? extends ITable> chain) {
      getOwner().execUpdateTableStatus();
    }

    @Override
    public void execSaveInsertedRow(TableFieldSaveInsertedRowChain<? extends ITable> chain, ITableRow row) throws ProcessingException {
      getOwner().execSaveInsertedRow(row);
    }

    @Override
    public void execSaveUpdatedRow(TableFieldSaveUpdatedRowChain<? extends ITable> chain, ITableRow row) throws ProcessingException {
      getOwner().execSaveUpdatedRow(row);
    }

    @Override
    public void execSaveDeletedRow(TableFieldSaveDeletedRowChain<? extends ITable> chain, ITableRow row) throws ProcessingException {
      getOwner().execSaveDeletedRow(row);
    }

    @Override
    public void execSave(TableFieldSaveChain<? extends ITable> chain, List<? extends ITableRow> insertedRows, List<? extends ITableRow> updatedRows, List<? extends ITableRow> deletedRows) {
      getOwner().execSave(insertedRows, updatedRows, deletedRows);
    }
  }

  @Override
  protected ITableFieldExtension<T, ? extends AbstractTableField<T>> createLocalExtension() {
    return new LocalTableFieldExtension<T, AbstractTableField<T>>(this);
  }

  protected final void interceptReloadTableData() throws ProcessingException {
    List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions = getAllExtensions();
    TableFieldReloadTableDataChain<T> chain = new TableFieldReloadTableDataChain<T>(extensions);
    chain.execReloadTableData();
  }

  protected final void interceptUpdateTableStatus() {
    List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions = getAllExtensions();
    TableFieldUpdateTableStatusChain<T> chain = new TableFieldUpdateTableStatusChain<T>(extensions);
    chain.execUpdateTableStatus();
  }

  protected final void interceptSaveInsertedRow(ITableRow row) throws ProcessingException {
    List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions = getAllExtensions();
    TableFieldSaveInsertedRowChain<T> chain = new TableFieldSaveInsertedRowChain<T>(extensions);
    chain.execSaveInsertedRow(row);
  }

  protected final void interceptSaveUpdatedRow(ITableRow row) throws ProcessingException {
    List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions = getAllExtensions();
    TableFieldSaveUpdatedRowChain<T> chain = new TableFieldSaveUpdatedRowChain<T>(extensions);
    chain.execSaveUpdatedRow(row);
  }

  protected final void interceptSaveDeletedRow(ITableRow row) throws ProcessingException {
    List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions = getAllExtensions();
    TableFieldSaveDeletedRowChain<T> chain = new TableFieldSaveDeletedRowChain<T>(extensions);
    chain.execSaveDeletedRow(row);
  }

  protected final void interceptSave(List<? extends ITableRow> insertedRows, List<? extends ITableRow> updatedRows, List<? extends ITableRow> deletedRows) {
    List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions = getAllExtensions();
    TableFieldSaveChain<T> chain = new TableFieldSaveChain<T>(extensions);
    chain.execSave(insertedRows, updatedRows, deletedRows);
  }
}