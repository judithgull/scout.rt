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
package org.eclipse.scout.rt.ui.swt.form.fields.checkbox;

import org.eclipse.scout.commons.BooleanUtility;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.rt.client.ui.form.fields.booleanfield.IBooleanField;
import org.eclipse.scout.rt.ui.swt.LogicalGridData;
import org.eclipse.scout.rt.ui.swt.LogicalGridLayout;
import org.eclipse.scout.rt.ui.swt.ext.ILabelComposite;
import org.eclipse.scout.rt.ui.swt.ext.MultilineCheckbox;
import org.eclipse.scout.rt.ui.swt.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.swt.form.fields.LogicalGridDataBuilder;
import org.eclipse.scout.rt.ui.swt.form.fields.SwtScoutValueFieldComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * <h3>SwtScoutCheckbox</h3> ...
 * 
 * @since 1.0.0 14.04.2008
 */
public class SwtScoutCheckbox extends SwtScoutValueFieldComposite<IBooleanField> implements ISwtScoutCheckbox {

  private P_SwtButtonListener m_swtButtonListener;
  private boolean m_mandatoryCached;
  private StatusLabelEx m_labelPlaceholder;

  @Override
  protected void initializeSwt(Composite parent) {
    super.initializeSwt(parent);
    Composite container = getEnvironment().getFormToolkit().createComposite(parent);
    m_labelPlaceholder = new StatusLabelEx(container, SWT.NONE, getEnvironment());
    getEnvironment().getFormToolkit().getFormToolkit().adapt(m_labelPlaceholder, false, false);
    m_labelPlaceholder.setLayoutData(LogicalGridDataBuilder.createLabel(getScoutObject().getGridData()));

    MultilineCheckbox checkbox = getEnvironment().getFormToolkit().createMultilineCheckBox(container);

    LogicalGridData checkboxData = LogicalGridDataBuilder.createField(getScoutObject().getGridData());
    checkboxData.fillHorizontal = false;
    checkboxData.useUiWidth = true;
    checkboxData.weightx = 0;
    checkbox.setLayoutData(checkboxData);

    // This label is only used to dispatch some properties to the checkbox label (see updateLabel)
    // So it has to be invisible.
    StatusLabelEx dispatcherLabel = new StatusLabelEx(container, SWT.NONE, getEnvironment());
    dispatcherLabel.setVisible(false);
    setSwtLabel(dispatcherLabel);

    //
    setSwtContainer(container);
    setSwtField(checkbox);

    // layout
    container.setLayout(new LogicalGridLayout(1, 0));
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    if (m_swtButtonListener == null) {
      m_swtButtonListener = new P_SwtButtonListener();
    }
    getSwtField().addListener(SWT.Selection, m_swtButtonListener);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.scout.rt.ui.swt.form.fields.SwtScoutValueFieldComposite#setErrorStatusFromScout(org.eclipse.scout.commons.exception.IProcessingStatus)
   */
  @Override
  protected void setErrorStatusFromScout(IProcessingStatus s) {
    // Update the status of the labelPlaceholder and not the dispatcherLabel
    m_labelPlaceholder.setStatus(s);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.scout.rt.ui.swt.form.fields.SwtScoutFieldComposite#setMandatoryFromScout(boolean)
   */
  @Override
  protected void setMandatoryFromScout(boolean b) {
    super.setMandatoryFromScout(b);

    updateLabel();
  }

  /**
   * Updates the label of the checkbox with the properties of the dispatcher label.
   * This makes sure that the mandatory appearance is reflected correctly.
   */
  protected void updateLabel() {
    if (getSwtLabel() instanceof StatusLabelEx) {
      StatusLabelEx swtLabel = (StatusLabelEx) getSwtLabel();

      if (swtLabel.getText() != null) {
        getSwtField().setText(swtLabel.getText());
      }

      getSwtField().setFont(swtLabel.getFont());
      getSwtField().setForeground(swtLabel.getForeground());
      getSwtField().setBackground(swtLabel.getBackground());
    }
  }

  @Override
  protected void detachScout() {
    super.detachScout();
    getSwtField().removeListener(SWT.Selection, m_swtButtonListener);
  }

  @Override
  public MultilineCheckbox getSwtField() {
    return (MultilineCheckbox) super.getSwtField();
  }

  @Override
  public ILabelComposite getPlaceholderLabel() {
    return m_labelPlaceholder;
  }

  @Override
  protected void setLabelVisibleFromScout() {
    boolean b = getScoutObject().isLabelVisible();
    if (m_labelPlaceholder != null && b != m_labelPlaceholder.getVisible()) {
      m_labelPlaceholder.setVisible(b);
      if (getSwtContainer() != null && isConnectedToScout()) {
        getSwtContainer().layout(true, true);
      }
    }
  }

  @Override
  protected void setLabelFromScout(String s) {
    super.setLabelFromScout(s);
    updateLabel();
  }

  @Override
  protected void updateValueFromScout() {
    getSwtField().setSelection(BooleanUtility.nvl(getScoutObject() == null ? null : getScoutObject().getValue()));
  }

  protected void handleSwtAction(final boolean selection) {
    if (!getSwtField().isEnabled()) {
      return;
    }
    // notify Scout
    Runnable t = new Runnable() {
      @Override
      public void run() {
        getScoutObject().getUIFacade().setSelectedFromUI(selection);
        // ensure the selection state of model and UI matches.
        if (selection != getScoutObject().isChecked()) {
          Runnable r = new Runnable() {
            @Override
            public void run() {
              getSwtField().setSelection(getScoutObject().isChecked());
            }
          };
          getEnvironment().invokeSwtLater(r);
        }
      }
    };
    getEnvironment().invokeScoutLater(t, 0);
    // end notify
  }

  private class P_SwtButtonListener implements Listener {
    @Override
    public void handleEvent(Event event) {
      switch (event.type) {
        case SWT.Selection:
          handleSwtAction(getSwtField().getSelection());
          break;
      }
    }
  } // end class P_SwtButtonListener

}
