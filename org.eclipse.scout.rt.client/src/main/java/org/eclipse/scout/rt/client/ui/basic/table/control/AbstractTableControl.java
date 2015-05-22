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
package org.eclipse.scout.rt.client.ui.basic.table.control;

import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.action.AbstractAction;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.exception.ExceptionHandler;

/**
 * @since 5.1.0
 */
public abstract class AbstractTableControl extends AbstractAction implements ITableControl {
  private ITable m_table;

  public AbstractTableControl() {
    this(true);
  }

  public AbstractTableControl(boolean callInitializer) {
    if (callInitializer) {
      callInitializer();
    }
  }

  @Override
  protected boolean getConfiguredToggleAction() {
    return true;
  }

  /**
   * Configures the form to be used with this control. The form is lazily created and started when the control gets
   * selected.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   *
   * @return a form type token
   * @see {@link #startForm(IForm)} for details how the form gets started
   */
  @ConfigProperty(ConfigProperty.FORM)
  @Order(90)
  protected Class<? extends IForm> getConfiguredForm() {
    return null;
  }

  /**
   * Initializes the form associated with this button. This method is called before the
   * form is used for the first time.
   * <p>
   * Subclasses can override this method. The default does nothing.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(120)
  protected void execInitForm() throws ProcessingException {
  }

  protected IForm createForm() throws ProcessingException {
    if (getConfiguredForm() == null) {
      return null;
    }
    try {
      return getConfiguredForm().newInstance();
    }
    catch (Exception e) {
      BEANS.get(ExceptionHandler.class).handle(new ProcessingException("error creating instance of class '" + getConfiguredForm().getName() + "'.", e));
    }
    return null;
  }

  /**
   * Starts the form.
   * <p>
   * The default uses {@link IForm#start()} and therefore expects a form handler to be previously set. Override to call
   * a custom start method or implement a {@link IForm#start()} on the detail form.
   */
  protected void startForm() throws ProcessingException {
    getForm().start();
  }

  @Override
  public void setForm(IForm form) {
    propertySupport.setProperty(PROP_FORM, form);
  }

  @Override
  public final IForm getForm() {
    return (IForm) propertySupport.getProperty(PROP_FORM);
  }

  public void setTable(ITable table) {
    m_table = table;
  }

  @Override
  protected void execSelectionChanged(boolean selected) throws ProcessingException {
    if (!selected) {
      return;
    }
    // Deselect other controls
    for (ITableControl control : m_table.getTableControls()) {
      if (control != this && control.isSelected()) {
        control.setSelected(false);
      }
    }
    ensureFormCreated();
    ensureFormStarted();
  }

  public void ensureFormCreated() throws ProcessingException {
    if (getForm() != null) {
      return;
    }
    IForm form = createForm();
    if (form != null) {
      setForm(form);
      decorateForm();
      execInitForm();
    }
  }

  public void ensureFormStarted() throws ProcessingException {
    if (getForm() == null || getForm().isFormOpen()) {
      return;
    }
    startForm();
  }

  public void decorateForm() {
    getForm().setAutoAddRemoveOnDesktop(false);
  }
}