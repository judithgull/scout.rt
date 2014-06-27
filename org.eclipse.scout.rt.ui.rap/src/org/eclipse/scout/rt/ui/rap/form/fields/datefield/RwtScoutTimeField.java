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
package org.eclipse.scout.rt.ui.rap.form.fields.datefield;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.job.JobEx;
import org.eclipse.scout.rt.client.ui.action.menu.root.IContextMenu;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.datefield.IDateField;
import org.eclipse.scout.rt.ui.rap.LogicalGridLayout;
import org.eclipse.scout.rt.ui.rap.RwtMenuUtility;
import org.eclipse.scout.rt.ui.rap.action.menu.RwtContextMenuMarkerComposite;
import org.eclipse.scout.rt.ui.rap.action.menu.RwtScoutContextMenu;
import org.eclipse.scout.rt.ui.rap.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.rap.ext.StyledTextEx;
import org.eclipse.scout.rt.ui.rap.ext.custom.StyledText;
import org.eclipse.scout.rt.ui.rap.form.fields.IPopupSupport;
import org.eclipse.scout.rt.ui.rap.form.fields.LogicalGridDataBuilder;
import org.eclipse.scout.rt.ui.rap.form.fields.RwtScoutBasicFieldComposite;
import org.eclipse.scout.rt.ui.rap.form.fields.datefield.chooser.TimeChooserDialog;
import org.eclipse.scout.rt.ui.rap.internal.TextFieldEditableSupport;
import org.eclipse.scout.rt.ui.rap.keystroke.RwtKeyStroke;
import org.eclipse.scout.rt.ui.rap.util.RwtLayoutUtility;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;

public class RwtScoutTimeField extends RwtScoutBasicFieldComposite<IDateField> implements IRwtScoutTimeField, IPopupSupport {

  private Button m_dropDownButton;
  private TextFieldEditableSupport m_editableSupport;

  private Set<IPopupSupportListener> m_popupEventListeners;
  private Object m_popupEventListenerLock;

  private boolean m_ignoreLabel = false;
  private Composite m_timeContainer;
  private boolean m_dateTimeCompositeMember;
  private String m_displayTextToVerify;
  private TimeChooserDialog m_timeChooserDialog = null;
  private FocusAdapter m_textFieldFocusAdapter = null;

  private RwtContextMenuMarkerComposite m_menuMarkerComposite;
  private RwtScoutContextMenu m_uiContextMenu;
  private P_ContextMenuPropertyListener m_contextMenuPropertyListener;

  @Override
  public void setIgnoreLabel(boolean ignoreLabel) {
    m_ignoreLabel = ignoreLabel;
    if (ignoreLabel) {
      getUiLabel().setVisible(false);
    }
    else {
      getUiLabel().setVisible(getScoutObject().isLabelVisible());
    }
  }

  public boolean isIgnoreLabel() {
    return m_ignoreLabel;
  }

  public boolean isDateTimeCompositeMember() {
    return m_dateTimeCompositeMember;
  }

  @Override
  public void setDateTimeCompositeMember(boolean dateTimeCompositeMember) {
    m_dateTimeCompositeMember = dateTimeCompositeMember;
  }

  @Override
  protected void initializeUi(Composite parent) {
    m_popupEventListeners = new HashSet<IPopupSupportListener>();
    m_popupEventListenerLock = new Object();

    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    StatusLabelEx label = getUiEnvironment().getFormToolkit().createStatusLabel(container, getScoutObject());

    m_timeContainer = getUiEnvironment().getFormToolkit().createComposite(container, SWT.BORDER);
    m_timeContainer.setData(RWT.CUSTOM_VARIANT, RwtUtility.VARIANT_COMPOSITE_INPUT_FIELD_BORDER);

    m_menuMarkerComposite = new RwtContextMenuMarkerComposite(m_timeContainer, getUiEnvironment(), SWT.NONE);
    getUiEnvironment().getFormToolkit().adapt(m_menuMarkerComposite);
    m_menuMarkerComposite.addSelectionListener(new SelectionAdapter() {
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected(SelectionEvent e) {
        if (getUiContextMenu() != null) {
          Menu uiMenu = getUiContextMenu().getUiMenu();
          if (e.widget instanceof Control) {
            Point loc = ((Control) e.widget).toDisplay(e.x, e.y);
            uiMenu.setLocation(RwtMenuUtility.getMenuLocation(getScoutObject().getContextMenu().getChildActions(), uiMenu, loc, getUiEnvironment()));
          }
          uiMenu.setVisible(true);
        }
      }
    });

    StyledText textField = new StyledTextEx(m_menuMarkerComposite, SWT.SINGLE);
    getUiEnvironment().getFormToolkit().adapt(textField, false, false);
    textField.setData(RWT.CUSTOM_VARIANT, VARIANT_TIMEFIELD);

    Button timeChooserButton = getUiEnvironment().getFormToolkit().createButton(m_timeContainer, "", SWT.PUSH | SWT.NO_FOCUS);
    timeChooserButton.setData(RWT.CUSTOM_VARIANT, VARIANT_TIMEFIELD);

    container.setTabList(new Control[]{m_timeContainer});

    // key strokes
    getUiEnvironment().addKeyStroke(textField, new P_TimeChooserOpenKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_TimeChooserCloseKeyStroke(), true);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftNextQuarterHourKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftPreviousQuarterHourKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftNextHourKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftPreviousHourKeyStroke(), false);

    // listener
    timeChooserButton.addSelectionListener(new P_RwtBrowseButtonListener());
    attachFocusListener(textField, true);
    textField.addMouseListener(new MouseAdapter() {
      private static final long serialVersionUID = 1L;

      @Override
      public void mouseUp(MouseEvent e) {
        if (e.button == 1) {
          handleUiTimeChooserAction();
        }
      }
    });

    //
    setUiContainer(container);
    setUiLabel(label);
    setDropDownButton(timeChooserButton);
    setUiField(textField);

    // layout
    container.setLayout(new LogicalGridLayout(1, 0));

    m_timeContainer.setLayoutData(LogicalGridDataBuilder.createField(((IFormField) getScoutObject()).getGridData()));
    m_timeContainer.setLayout(RwtLayoutUtility.createGridLayoutNoSpacing(2, false));

    GridData textLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    m_menuMarkerComposite.setLayoutData(textLayoutData);

    GridData buttonLayoutData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    buttonLayoutData.heightHint = 20;
    buttonLayoutData.widthHint = 20;
    timeChooserButton.setLayoutData(buttonLayoutData);
  }

  @Override
  public Button getDropDownButton() {
    return m_dropDownButton;
  }

  public void setDropDownButton(Button b) {
    m_dropDownButton = b;
  }

  @Override
  public StyledTextEx getUiField() {
    return (StyledTextEx) super.getUiField();
  }

  public RwtScoutContextMenu getUiContextMenu() {
    return m_uiContextMenu;
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    // context menu
    updateContextMenuVisibilityFromScout();
    if (getScoutObject().getContextMenu() != null && m_contextMenuPropertyListener == null) {
      m_contextMenuPropertyListener = new P_ContextMenuPropertyListener();
      getScoutObject().getContextMenu().addPropertyChangeListener(IContextMenu.PROP_VISIBLE, m_contextMenuPropertyListener);
    }
  }

  @Override
  protected void detachScout() {
    // context menu listener
    if (m_contextMenuPropertyListener != null) {
      getScoutObject().getContextMenu().removePropertyChangeListener(IContextMenu.PROP_VISIBLE, m_contextMenuPropertyListener);
      m_contextMenuPropertyListener = null;
    }
    super.detachScout();
  }

  public boolean isFocusInTimePicker() {
    Control focusControl = getUiEnvironment().getDisplay().getFocusControl();
    boolean isFocusInDatePicker = RwtUtility.isAncestorOf(m_timeChooserDialog.getShell(), focusControl);
    return isFocusInDatePicker;
  }

  private void installFocusListenerOnTextField() {
    if (getUiField().isDisposed()) {
      return;
    }

    getUiField().setFocus();
    if (m_textFieldFocusAdapter == null) {
      m_textFieldFocusAdapter = new FocusAdapter() {
        private static final long serialVersionUID = 1L;

        @Override
        public void focusLost(FocusEvent e) {
          handleUiFocusLostOnDatePickerPopup(e);
        }

      };
    }
    getUiField().addFocusListener(m_textFieldFocusAdapter);
  }

  /**
   * The event is fired only if the time picker popup is open.
   * <p>
   * The default sets the focus on the ui field if the new focus is inside the time picker. <br/>
   * If the new focus is outside the time picker it makes sure the time picker popup will be closed.
   * </p>
   */
  protected void handleUiFocusLostOnDatePickerPopup(FocusEvent event) {
    if (isFocusInTimePicker()) {
      getUiEnvironment().getDisplay().asyncExec(new Runnable() {

        @Override
        public void run() {
          getUiField().setFocus();
        }

      });
    }
    else {
      getUiEnvironment().getDisplay().asyncExec(new Runnable() {

        @Override
        public void run() {
          makeSureTimeChooserIsClosed();
        }

      });
    }
  }

  private void uninstallFocusListenerOnTextField() {
    if (!getUiField().isDisposed() && m_textFieldFocusAdapter != null) {
      getUiField().removeFocusListener(m_textFieldFocusAdapter);
    }
  }

  @Override
  protected void setEnabledFromScout(boolean b) {
    super.setEnabledFromScout(b);
    m_dropDownButton.setEnabled(b);
    getUiField().setEnabled(b);
    if (b) {
      m_timeContainer.setData(RWT.CUSTOM_VARIANT, RwtUtility.VARIANT_COMPOSITE_INPUT_FIELD_BORDER);
    }
    else {
      m_timeContainer.setData(RWT.CUSTOM_VARIANT, RwtUtility.VARIANT_COMPOSITE_INPUT_FIELD_BORDER_READONLY);
    }
  }

  @Override
  protected void setLabelVisibleFromScout() {
    if (!isIgnoreLabel()) {
      super.setLabelVisibleFromScout();
    }
  }

  @Override
  protected void setFieldEnabled(Control rwtField, boolean enabled) {
    if (m_editableSupport == null) {
      m_editableSupport = new TextFieldEditableSupport(getUiField());
    }
    m_editableSupport.setEditable(enabled);
  }

  @Override
  protected void setDisplayTextFromScout(String s) {
    IDateField scoutField = getScoutObject();
    if (s == null) {
      s = "";
    }
    m_displayTextToVerify = s;
    Date value = scoutField.getValue();
    if (value != null) {
      DateFormat format = scoutField.getIsolatedTimeFormat();
      if (format != null) {
        m_displayTextToVerify = format.format(value);
      }
    }
    getUiField().setText(m_displayTextToVerify);
    getUiField().setCaretOffset(0);
  }

  @Override
  protected void setBackgroundFromScout(String scoutColor) {
    setBackgroundFromScout(scoutColor, m_timeContainer);
  }

  protected void updateContextMenuVisibilityFromScout() {
    m_menuMarkerComposite.setMarkerVisible(getScoutObject().getContextMenu().isVisible());
    if (getScoutObject().getContextMenu().isVisible()) {
      if (m_uiContextMenu == null) {
        m_uiContextMenu = new RwtScoutContextMenu(getUiField().getShell(), getScoutObject().getContextMenu(), getUiEnvironment());
        if (getDropDownButton() != null) {
          getDropDownButton().setMenu(m_uiContextMenu.getUiMenu());
        }
      }
    }
    else {
      if (getDropDownButton() != null) {
        getDropDownButton().setMenu(null);
      }
      if (m_uiContextMenu != null) {
        m_uiContextMenu.dispose();
      }
      m_uiContextMenu = null;
    }
  }

  @Override
  protected void handleUiInputVerifier(boolean doit) {
    if (!doit) {
      return;
    }
    final String text = getUiField().getText();
    // only handle if text has changed
    if (CompareUtility.equals(text, m_displayTextToVerify) && (isDateTimeCompositeMember() || getScoutObject().getErrorStatus() == null)) {
      return;
    }
    m_displayTextToVerify = text;
    final Holder<Boolean> result = new Holder<Boolean>(Boolean.class, false);
    // notify Scout
    Runnable t = new Runnable() {
      @Override
      public void run() {
        boolean b = getScoutObject().getUIFacade().setTimeTextFromUI(text);
        result.setValue(b);
      }
    };
    JobEx job = getUiEnvironment().invokeScoutLater(t, 0);
    try {
      job.join(2345);
    }
    catch (InterruptedException e) {
      //nop
    }
    getUiEnvironment().dispatchImmediateUiJobs();
  }

  @Override
  protected void handleUiFocusGained() {
    if (isSelectAllOnFocusEnabled()) {
      getUiField().setSelection(0, getUiField().getText().length());
    }
  }

  protected void makeSureTimeChooserIsClosed() {
    if (m_timeChooserDialog != null) {
      m_timeChooserDialog.close();
    }

    uninstallFocusListenerOnTextField();
  }

  private void handleUiTimeChooserAction() {
    if (!getDropDownButton().isVisible() || !getDropDownButton().isEnabled()) {
      return;
    }

    Date oldTime = getScoutObject().getValue();
    if (oldTime == null) {
      oldTime = new Date();
    }
    notifyPopupEventListeners(IPopupSupportListener.TYPE_OPENING);

    makeSureTimeChooserIsClosed();
    m_timeChooserDialog = createTimeChooserDialog(oldTime);
    if (m_timeChooserDialog != null) {
      m_timeChooserDialog.getShell().addDisposeListener(new P_TimeChooserDisposeListener());

      m_timeChooserDialog.open();
      installFocusListenerOnTextField();
    }
  }

  protected TimeChooserDialog createTimeChooserDialog(Date currentTime) {
    return new TimeChooserDialog(getUiField().getShell(), getUiField(), currentTime);
  }

  private void getTimeFromClosedDateChooserDialog() {
    boolean setFocusToUiField = false;
    try {
      final Date newDate = m_timeChooserDialog.getReturnTime();
      if (newDate != null) {
        setFocusToUiField = true;
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().setTimeFromUI(newDate);
          }
        };
        getUiEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
    finally {
      notifyPopupEventListeners(IPopupSupportListener.TYPE_CLOSED);
      uninstallFocusListenerOnTextField();
      if (setFocusToUiField
          && !getUiField().isDisposed()) {
        getUiField().setFocus();
      }
    }
  }

  private void notifyPopupEventListeners(int eventType) {
    IPopupSupportListener[] listeners;
    synchronized (m_popupEventListenerLock) {
      listeners = m_popupEventListeners.toArray(new IPopupSupportListener[m_popupEventListeners.size()]);
    }
    for (IPopupSupportListener listener : listeners) {
      listener.handleEvent(eventType);
    }
  }

  @Override
  public void addPopupEventListener(IPopupSupportListener listener) {
    synchronized (m_popupEventListenerLock) {
      m_popupEventListeners.add(listener);
    }
  }

  @Override
  public void removePopupEventListener(IPopupSupportListener listener) {
    synchronized (m_popupEventListenerLock) {
      m_popupEventListeners.remove(listener);
    }
  }

  private final class P_TimeChooserDisposeListener implements DisposeListener {
    private static final long serialVersionUID = 1L;

    @Override
    public void widgetDisposed(DisposeEvent event) {
      getTimeFromClosedDateChooserDialog();
      m_timeChooserDialog = null;
    }
  }

  private class P_RwtBrowseButtonListener extends SelectionAdapter {
    private static final long serialVersionUID = 1L;

    @Override
    public void widgetSelected(SelectionEvent e) {
      getUiField().forceFocus();
      handleUiTimeChooserAction();
    }
  } // end class P_RwtBrowseButtonListener

  private void shiftTime(final int level, final int value) {
    if (getUiField().isDisposed()) {
      return;
    }
    if (getUiField().isEnabled()
        && getUiField().getEditable()
        && getUiField().isVisible()) {
      if (level >= 0) {
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().fireTimeShiftActionFromUI(level, value);
          }
        };
        getUiEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
  }

  private abstract class P_AbstractShiftTimeKeyStroke extends AbstractShiftKeyStroke {
    public P_AbstractShiftTimeKeyStroke(int keyCode, int level, int value) {
      super(keyCode, SWT.NONE, level, value);
    }

    public P_AbstractShiftTimeKeyStroke(int keyCode, int stateMask, int level, int value) {
      super(keyCode, stateMask, level, value);
    }

    @Override
    protected void shift(final int level, final int value) {
      shiftTime(level, value);
    }
  }

  private class P_ShiftNextQuarterHourKeyStroke extends P_AbstractShiftTimeKeyStroke {
    public P_ShiftNextQuarterHourKeyStroke() {
      super(SWT.ARROW_UP, 0, 1);
    }
  }

  private class P_ShiftPreviousQuarterHourKeyStroke extends P_AbstractShiftTimeKeyStroke {
    public P_ShiftPreviousQuarterHourKeyStroke() {
      super(SWT.ARROW_DOWN, 0, -1);
    }
  }

  private class P_ShiftNextHourKeyStroke extends P_AbstractShiftTimeKeyStroke {
    public P_ShiftNextHourKeyStroke() {
      super(SWT.ARROW_UP, SWT.SHIFT, 1, 1);
    }
  }

  private class P_ShiftPreviousHourKeyStroke extends P_AbstractShiftTimeKeyStroke {
    public P_ShiftPreviousHourKeyStroke() {
      super(SWT.ARROW_DOWN, SWT.SHIFT, 1, -1);
    }
  }

  private class P_TimeChooserOpenKeyStroke extends RwtKeyStroke {
    public P_TimeChooserOpenKeyStroke() {
      super(SWT.F2);
    }

    @Override
    public void handleUiAction(Event e) {
      handleUiTimeChooserAction();
    }
  }

  private class P_TimeChooserCloseKeyStroke extends RwtKeyStroke {
    public P_TimeChooserCloseKeyStroke() {
      super(SWT.ESC);
    }

    @Override
    public void handleUiAction(Event e) {
      if (m_timeChooserDialog != null) {
        makeSureTimeChooserIsClosed();
        e.doit = false;
      }
    }
  }

  private class P_ContextMenuPropertyListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      if (IContextMenu.PROP_VISIBLE.equals(evt.getPropertyName())) {
        // synchronize
        getUiEnvironment().invokeUiLater(new Runnable() {
          @Override
          public void run() {
            updateContextMenuVisibilityFromScout();
          }
        });
      }
    }
  }
}
