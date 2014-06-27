/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.form.fields.listbox;

import java.util.List;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.IListBox;
import org.eclipse.scout.rt.ui.rap.LogicalGridData;
import org.eclipse.scout.rt.ui.rap.LogicalGridLayout;
import org.eclipse.scout.rt.ui.rap.basic.IRwtScoutComposite;
import org.eclipse.scout.rt.ui.rap.basic.table.RwtScoutTable;
import org.eclipse.scout.rt.ui.rap.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.rap.ext.table.TableEx;
import org.eclipse.scout.rt.ui.rap.form.fields.LogicalGridDataBuilder;
import org.eclipse.scout.rt.ui.rap.form.fields.RwtScoutValueFieldComposite;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @since 3.8.0
 */
public class RwtScoutListBox extends RwtScoutValueFieldComposite<IListBox<?>> implements IRwtScoutListBox {

  private RwtScoutTable m_tableComposite;
  private Composite m_tableContainer;

  @Override
  protected void initializeUi(Composite parent) {
    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    StatusLabelEx label = getUiEnvironment().getFormToolkit().createStatusLabel(container, getScoutObject());

    Composite tableContainer = new Composite(container, SWT.NONE);
    tableContainer.setLayout(new LogicalGridLayout(1, 0));
    tableContainer.setData(RWT.CUSTOM_VARIANT, RwtUtility.VARIANT_LISTBOX);
    m_tableContainer = tableContainer;
    m_tableComposite = new RwtScoutTable(RwtUtility.VARIANT_LISTBOX);
    m_tableComposite.createUiField(tableContainer, getScoutObject().getTable(), getUiEnvironment());
    LogicalGridData fieldData = LogicalGridDataBuilder.createField(getScoutObject().getGridData());
    // filter box
    List<IFormField> childFields = getScoutObject().getFields();
    if (CollectionUtility.hasElements(childFields)) {
      IFormField firstField = CollectionUtility.firstElement(childFields);
      IRwtScoutComposite filterComposite = getUiEnvironment().createFormField(container, firstField);
      LogicalGridData filterData = LogicalGridDataBuilder.createField(firstField.getGridData());
      filterData.gridx = fieldData.gridx;
      filterData.gridy = fieldData.gridy + fieldData.gridh;
      filterData.gridw = fieldData.gridw;
      filterData.weightx = fieldData.weightx;
      filterComposite.getUiContainer().setLayoutData(filterData);
    }
    //
    setUiContainer(container);
    setUiLabel(label);
    TableEx tableField = m_tableComposite.getUiField();
    tableContainer.setLayoutData(fieldData);
    setUiField(tableField);

    // layout
    getUiContainer().setLayout(new LogicalGridLayout(1, 0));
  }

  /**
   * complete override
   */
  @Override
  protected void setFieldEnabled(Control uiField, boolean b) {
    m_tableComposite.setEnabledFromScout(b);
  }

  @Override
  protected void setEnabledFromScout(boolean b) {
    super.setEnabledFromScout(b);
    // Workaround, because ":disabled" state seems to be ignored by RAP
    if (m_tableContainer != null) {
      m_tableContainer.setData(RWT.CUSTOM_VARIANT, (b ? RwtUtility.VARIANT_LISTBOX : RwtUtility.VARIANT_LISTBOX_DISABLED));
    }
  }
}
