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
package org.eclipse.scout.rt.ui.swt.form.fields.datefield;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.job.JobEx;
import org.eclipse.scout.rt.client.ui.form.fields.datefield.IDateField;
import org.eclipse.scout.rt.shared.AbstractIcons;
import org.eclipse.scout.rt.ui.swt.LogicalGridLayout;
import org.eclipse.scout.rt.ui.swt.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.swt.form.fields.LogicalGridDataBuilder;
import org.eclipse.scout.rt.ui.swt.form.fields.SwtScoutBasicFieldComposite;
import org.eclipse.scout.rt.ui.swt.form.fields.datefield.chooser.TimeChooserDialog;
import org.eclipse.scout.rt.ui.swt.internal.TextFieldEditableSupport;
import org.eclipse.scout.rt.ui.swt.keystroke.SwtKeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

public class SwtScoutTimeField extends SwtScoutBasicFieldComposite<IDateField> implements ISwtScoutTimeField {
  private Button m_timeChooserButton;
  private TextFieldEditableSupport m_editableSupport;

  @Override
  protected void initializeSwt(Composite parent) {
    Composite container = getEnvironment().getFormToolkit().createComposite(parent);
    StatusLabelEx label = getEnvironment().getFormToolkit().createStatusLabel(container, getEnvironment(), getScoutObject());

    StyledText textField = getEnvironment().getFormToolkit().createStyledText(container, SWT.SINGLE | SWT.BORDER);
    Button timeChooserButton = getEnvironment().getFormToolkit().createButton(container, SWT.PUSH);
    timeChooserButton.setImage(getEnvironment().getIcon(AbstractIcons.DateFieldTime));
    // prevent the button from grabbing focus
    container.setTabList(new Control[]{textField});

    // ui key strokes
    getEnvironment().addKeyStroke(container, new P_TimeChooserOpenKeyStroke());

    // listener
    timeChooserButton.addSelectionListener(new P_SwtBrowseButtonListener());
    addModifyListenerForBasicField(textField);
    //
    setSwtContainer(container);
    setSwtLabel(label);
    setTimeChooserButton(timeChooserButton);
    setSwtField(textField);
    // layout
    container.setLayout(new LogicalGridLayout(1, 0));
    timeChooserButton.setLayoutData(LogicalGridDataBuilder.createButton1());
  }

  @Override
  public Button getTimeChooserButton() {
    return m_timeChooserButton;
  }

  public void setTimeChooserButton(Button timeChooserButton) {
    m_timeChooserButton = timeChooserButton;
  }

  @Override
  public StyledText getSwtField() {
    return (StyledText) super.getSwtField();
  }

  @Override
  protected void setEnabledFromScout(boolean b) {
    super.setEnabledFromScout(b);
    m_timeChooserButton.setEnabled(b);
  }

  @Override
  protected String getText() {
    return getSwtField().getText();
  }

  @Override
  protected void setText(String text) {
    getSwtField().setText(text);
  }

  @Override
  protected Point getSelection() {
    return getSwtField().getSelection();
  }

  @Override
  protected void setSelection(int startIndex, int endIndex) {
    getSwtField().setSelection(startIndex, endIndex);
  }

  @Override
  protected int getCaretOffset() {
    return getSwtField().getCaretOffset();
  }

  @Override
  protected void setCaretOffset(int caretPosition) {
    getSwtField().setCaretOffset(caretPosition);
  }

  @Override
  protected TextFieldEditableSupport createEditableSupport() {
    return new TextFieldEditableSupport(getSwtField());
  }

  @Override
  protected void setFieldEnabled(Control swtField, boolean enabled) {
    if (m_editableSupport == null) {
      m_editableSupport = new TextFieldEditableSupport(getSwtField());
    }
    m_editableSupport.setEditable(enabled);
  }

  @Override
  protected boolean handleSwtInputVerifier() {
    final String text = getSwtField().getText();
    // only handle if text has changed
    if (!m_updateDisplayTextOnModifyWasTrueSinceLastWriteDown && CompareUtility.equals(text, getScoutObject().getDisplayText()) && getScoutObject().getErrorStatus() == null) {
      return true;
    }
    final Holder<Boolean> result = new Holder<Boolean>(Boolean.class, false);
    // notify Scout
    Runnable t = new Runnable() {
      @Override
      public void run() {
        boolean b = getScoutObject().getUIFacade().setTimeTextFromUI(text);
        result.setValue(b);
      }
    };
    JobEx job = getEnvironment().invokeScoutLater(t, 0);
    try {
      job.join(2345);
    }
    catch (InterruptedException e) {
      //nop
    }
    getEnvironment().dispatchImmediateSwtJobs();
    // end notify
    if (m_updateDisplayTextOnModifyWasTrueSinceLastWriteDown && !m_updateDisplayTextOnModify) {
      m_updateDisplayTextOnModifyWasTrueSinceLastWriteDown = false;
    }
    return true;// continue always
  }

  @Override
  protected void handleSwtFocusGained() {
    super.handleSwtFocusGained();

    scheduleSelectAll();
  }

  @Override
  protected void handleSwtFocusLost() {
    getSwtField().setSelection(0, 0);
  }

  private void handleSwtTimeChooserAction() {
    if (getTimeChooserButton().isVisible() && getTimeChooserButton().isEnabled()) {
      Date d = getScoutObject().getValue();
      if (d == null) {
        d = new Date();
      }
      try {
        TimeChooserDialog dialog = new TimeChooserDialog(getSwtField().getShell(), d, getEnvironment());
        Date newDate = dialog.openDateChooser(getSwtField());
        if (newDate != null) {
          getSwtField().setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(newDate));
          handleSwtInputVerifier();
        }
      }
      finally {
        if (!getSwtField().isDisposed()) {
          getSwtField().setFocus();
        }
      }
    }
  }

  private class P_SwtBrowseButtonListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      handleSwtTimeChooserAction();
    }
  } // end class P_SwtBrowseButtonListener

  private class P_TimeChooserOpenKeyStroke extends SwtKeyStroke {
    public P_TimeChooserOpenKeyStroke() {
      super(SWT.F2);
    }

    @Override
    public void handleSwtAction(Event e) {
      handleSwtTimeChooserAction();
    }
  }
}
