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

import java.util.Collection;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.TriState;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.exception.ProcessingStatus;
import org.eclipse.scout.commons.job.JobEx;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.table.TableListener;
import org.eclipse.scout.rt.client.ui.form.AbstractFormHandler;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractButton;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractRadioButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.radiobuttongroup.AbstractRadioButtonGroup;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistTableForm.MainBox.ActiveStateRadioButtonGroup;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistTableForm.MainBox.NewButton;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistTableForm.MainBox.ResultTableField;
import org.eclipse.scout.rt.client.ui.form.fields.tablefield.AbstractTableField;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;
import org.eclipse.scout.service.SERVICES;

public class ContentAssistTableForm<LOOKUP_KEY> extends AbstractContentAssistFieldProposalForm<LOOKUP_KEY> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(ContentAssistTableForm.class);

  private JobEx m_dataLoadJob;
  private LOOKUP_KEY m_lastSelectedKey;

  public ContentAssistTableForm(IContentAssistField<?, LOOKUP_KEY> contentAssistField, boolean allowCustomText) throws ProcessingException {
    super(contentAssistField, allowCustomText);
  }

  @Override
  public void forceProposalSelection() throws ProcessingException {
    @SuppressWarnings("unchecked")
    ContentAssistTableForm<LOOKUP_KEY>.MainBox.ResultTableField tableField = getResultTableField();
    IContentAssistFieldTable<LOOKUP_KEY> table = tableField.getTable();
    table.selectNextRow();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void dataFetchedDelegateImpl(IContentAssistFieldDataFetchResult<LOOKUP_KEY> result, int maxCount) {
    List<? extends ILookupRow<LOOKUP_KEY>> rows = null;
    boolean selectCurrentValue = false;
    ProcessingException failed = null;
    String searchText = null;
    if (result != null) {
      rows = result.getLookupRows();
      selectCurrentValue = result.isSelectCurrentValue();
      failed = result.getProcessingException();
      searchText = result.getSearchText();
    }
    if (rows == null) {
      rows = CollectionUtility.emptyArrayList();
    }

    try {
      // populate table
      IContentAssistFieldTable<LOOKUP_KEY> table = (IContentAssistFieldTable<LOOKUP_KEY>) getResultTableField().getTable();
      table.setTableChanging(true);
      table.setLookupRows(CollectionUtility.truncateList(rows, maxCount));
      try {
        //restore selection
        LOOKUP_KEY keyToSelect = null;
        if (selectCurrentValue) {
          m_lastSelectedKey = getContentAssistField().getValueAsLookupKey();
          keyToSelect = m_lastSelectedKey;
        }
        else if (rows.size() == 1 && !isAllowCustomText()) {
          // select first
          keyToSelect = (LOOKUP_KEY) CollectionUtility.firstElement(rows).getKey();
        }
        if (keyToSelect != null) {
          table.select(keyToSelect);
        }
      }
      finally {
        table.setTableChanging(false);
      }
      String statusText = null;
      int severity = ProcessingStatus.INFO;
      if (failed != null) {
        statusText = failed.getStatus().getMessage();
        severity = ProcessingStatus.ERROR;
      }
      else if (rows.size() <= 0) {
        statusText = ScoutTexts.get("SmartFieldCannotComplete", (searchText == null) ? ("") : (searchText));
        severity = ProcessingStatus.WARNING;
      }
      else if (rows.size() > getContentAssistField().getBrowseMaxRowCount()) {
        statusText = ScoutTexts.get("SmartFieldMoreThanXRows", "" + getContentAssistField().getBrowseMaxRowCount());
        severity = ProcessingStatus.INFO;
      }
      if (statusText != null) {
        getResultTableField().setTablePopulateStatus(new ProcessingStatus(statusText, severity));
      }
      else {
        getResultTableField().setTablePopulateStatus(null);
      }
      if (getNewButton().isEnabled()) {
        getNewButton().setVisible(table.getRowCount() <= 0);
      }
      structureChanged(getResultTableField());

    }
    catch (ProcessingException e) {
      LOG.warn("update proposal list", e);
    }
  }

  @Override
  public void setTablePopulateStatus(IProcessingStatus status) {
    getResultTableField().setTablePopulateStatus(status);
  }

  /**
   * Override this method to change that behavior of what is a single match.
   * <p>
   * By default a single match is when there is only one enabled row in the drop down table
   * </p>
   */
  @ConfigOperation
  @Order(120)
  protected ILookupRow<LOOKUP_KEY> execGetSingleMatch() {
    int matchCount = 0;
    ILookupRow<LOOKUP_KEY> foundRow = null;
    @SuppressWarnings("unchecked")
    IContentAssistFieldTable<LOOKUP_KEY> table = (IContentAssistFieldTable<LOOKUP_KEY>) getResultTableField().getTable();
    List<ILookupRow<LOOKUP_KEY>> values = table.getLookupRows();
    for (ILookupRow<LOOKUP_KEY> row : values) {
      if (row.isEnabled()) {
        foundRow = row;
        matchCount++;
      }
    }
    if (matchCount == 1) {
      return foundRow;
    }
    else {
      return null;
    }
  }

  /**
   * Override this method to change the behavior when a row is clicked in the result {@link IContentAssistFieldTable}.
   * <p>
   * By default the form is closed with {@link #doOk()}.
   * </p>
   *
   * @param row
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(130)
  protected void execResultTableRowClicked(ITableRow row) throws ProcessingException {
    @SuppressWarnings("unchecked")
    IContentAssistFieldTable<LOOKUP_KEY> table = (IContentAssistFieldTable<LOOKUP_KEY>) getResultTableField().getTable();
    ILookupRow<LOOKUP_KEY> lrow = table.getSelectedLookupRow();
    if (lrow != null && lrow.isEnabled()) {
      doOk();
    }
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
    IContentAssistFieldTable<LOOKUP_KEY> table = (IContentAssistFieldTable<LOOKUP_KEY>) getResultTableField().getTable();
    ILookupRow<LOOKUP_KEY> row = null;
    if (table.isCheckable()) {
      Collection<ITableRow> checkedRows = table.getCheckedRows();
      if (CollectionUtility.hasElements(checkedRows)) {
        row = (ILookupRow<LOOKUP_KEY>) table.getCheckedLookupRow();
      }
    }
    else {
      row = (ILookupRow<LOOKUP_KEY>) table.getSelectedLookupRow();
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

  public ResultTableField getResultTableField() {
    return getFieldByClass(ResultTableField.class);
  }

  public ActiveStateRadioButtonGroup getActiveStateRadioButtonGroup() {
    return getFieldByClass(ActiveStateRadioButtonGroup.class);
  }

  /*
   * Fields
   */
  public NewButton getNewButton() {
    return getFieldByClass(NewButton.class);
  }

  /**
   * might be used to add special listeners to the smart table.
   */
  protected void execDecorateTable(IContentAssistFieldTable<LOOKUP_KEY> table) {

  }

  @Order(10)
  public class MainBox extends AbstractGroupBox {

    @Override
    protected boolean getConfiguredBorderVisible() {
      return false;
    }

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
    public class ResultTableField extends AbstractTableField<IContentAssistFieldTable<LOOKUP_KEY>> {

      public ResultTableField() {
        super();
      }

      @SuppressWarnings("unchecked")
      @Override
      protected IContentAssistFieldTable<LOOKUP_KEY> createTable() {
        Class<?> contentAssistFieldTableClazz = getContentAssistField().getContentAssistFieldTableClass();
        if (contentAssistFieldTableClazz != null) {
          try {
            return (IContentAssistFieldTable<LOOKUP_KEY>) ConfigurationUtility.newInnerInstance(getContentAssistField(), contentAssistFieldTableClazz);
          }
          catch (Exception e) {
            SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + contentAssistFieldTableClazz.getName() + "'.", e));
          }
        }
        return null;
      }

      @Override
      protected void execInitField() throws ProcessingException {
        getTable().addTableListener(new TableListener() {

          @Override
          public void tableChangedBatch(List<? extends TableEvent> batch) {
          }

          @Override
          public void tableChanged(TableEvent e) {
            if (e.getType() == TableEvent.TYPE_ROW_CLICK) {
              try {
                execResultTableRowClicked(CollectionUtility.firstElement(e.getRows()));
              }
              catch (ProcessingException e1) {
                LOG.warn("could not handle smart table selection.", e);
              }
            }
          }
        });
        execDecorateTable(getTable());
      }

      @Override
      protected boolean getConfiguredLabelVisible() {
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
      protected boolean getConfiguredTableStatusVisible() {
        return true;
      }

      @Override
      protected void execUpdateTableStatus() {
        //nop
      }
    }

    @Order(30)
    public class ActiveStateRadioButtonGroup extends AbstractRadioButtonGroup<TriState> {

      @Override
      protected boolean getConfiguredLabelVisible() {
        return false;
      }

      @Override
      protected void execChangedValue() throws ProcessingException {
        if (isVisible() && !isFormLoading()) {
          getContentAssistField().setActiveFilter(getValue());
          getContentAssistField().doSearch(false, false);
        }
      }

      @Order(10)
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

      @Order(20)
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

      @Order(30)
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

    @Order(40)
    public class NewButton extends AbstractButton {
      @Override
      protected boolean getConfiguredLabelVisible() {
        return false;
      }

      @Override
      protected boolean getConfiguredVisible() {
        return false;
      }

      @Override
      protected boolean getConfiguredEnabled() {
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

  }// end main box

  /*
   * handlers
   */
  private class FormHandler extends AbstractFormHandler {

    @SuppressWarnings("unchecked")
    @Override
    protected void execLoad() throws ProcessingException {

      ITable table = getResultTableField().getTable();
      if (table.getDefaultIconId() == null) {
        table.setDefaultIconId(getContentAssistField().getBrowseIconId());
      }
      getActiveStateRadioButtonGroup().setVisible(getContentAssistField().isActiveFilterEnabled());
      getActiveStateRadioButtonGroup().setValue(getContentAssistField().getActiveFilter());
      getNewButton().setEnabled(getContentAssistField().getBrowseNewText() != null);
      getNewButton().setLabel(getContentAssistField().getBrowseNewText());
    }

    @Override
    protected boolean execValidate() throws ProcessingException {
      return getAcceptedProposal() != null || isAllowCustomText();
    }

    @Override
    protected void execFinally() throws ProcessingException {
      if (m_dataLoadJob != null) {
        m_dataLoadJob.cancel();
      }
    }
  }

}
