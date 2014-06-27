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
package org.eclipse.scout.rt.ui.rap.window.messagebox;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.rt.client.ui.messagebox.IMessageBox;
import org.eclipse.scout.rt.client.ui.messagebox.MessageBoxEvent;
import org.eclipse.scout.rt.client.ui.messagebox.MessageBoxListener;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.extension.UiDecorationExtensionPoint;
import org.eclipse.scout.rt.ui.rap.form.fields.groupbox.layout.ButtonBarLayout;
import org.eclipse.scout.rt.ui.rap.form.fields.groupbox.layout.ButtonBarLayoutData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * <h3>MessageBoxDialog</h3> ...
 * 
 * @since 3.7.0 June 2011
 */
public class RwtScoutMessageBoxDialog extends Dialog {
  private static final long serialVersionUID = 1L;

  private P_ScoutMessageBoxListener m_scoutMessageBoxListener;

  private final IMessageBox m_scoutObject;
  private IRwtEnvironment m_uiEnvironment;

  private Label m_imageLabel;
  private Label m_introLabel;
  private Label m_actionLabel;

  public RwtScoutMessageBoxDialog(Shell parentShell, IMessageBox scoutObject, IRwtEnvironment uiEnvironment) {
    super(parentShell);
    m_scoutObject = scoutObject;
    m_uiEnvironment = uiEnvironment;
    int dialogStyle = SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL;
    setShellStyle(dialogStyle);
    setBlockOnOpen(false);
  }

  @Override
  public void create() {
    super.create();
    attachScout();
    getShell().addDisposeListener(new P_DisposeListener());
  }

  void attachScout() {
    if (m_scoutMessageBoxListener == null) {
      m_scoutMessageBoxListener = new P_ScoutMessageBoxListener();
      getScoutObject().addMessageBoxListener(m_scoutMessageBoxListener);
    }
    setTitleTextFromScout(getScoutObject().getTitle());
    setIntroTextFromScout(getScoutObject().getIntroText());
    setActionTextFromScout(getScoutObject().getActionText());
    setImageFromScout(getScoutObject().getIconId());
  }

  protected void setImageFromScout(String iconId) {
    Image icon = getUiEnvironment().getIcon(iconId);
    setImageInternal(icon);
  }

  private void setImageInternal(Image img) {
    boolean exclude = false;
    if (img == null) {
      exclude = true;
    }
    m_imageLabel.setImage(img);
    if (m_imageLabel.getLayoutData() instanceof GridData) {
      if (((GridData) m_imageLabel.getLayoutData()).exclude != exclude) {
        GridData gridData = ((GridData) m_imageLabel.getLayoutData());
        if (gridData.exclude != exclude) {
          gridData.exclude = exclude;
          m_imageLabel.getParent().getParent().layout(true, true);
          getShell().layout(true);
        }
      }
    }
  }

  protected void setTitleTextFromScout(String titleText) {
    if (titleText == null) {
      titleText = "";
    }
    getShell().setText(titleText);
    getShell().layout(true);
  }

  protected void setIntroTextFromScout(String introText) {
    boolean exclude = false;
    if (introText == null) {
      introText = "";
      exclude = true;
    }
    m_introLabel.setText(introText);
    if (m_introLabel.getLayoutData() instanceof GridData) {
      GridData gridData = ((GridData) m_introLabel.getLayoutData());
      if (gridData.exclude != exclude) {
        gridData.exclude = exclude;
        getShell().layout(true);
        m_introLabel.getParent().getParent().layout(true, true);
      }
    }
    // Hide empty labels
    m_introLabel.setVisible(StringUtility.hasText(m_introLabel.getText()));
  }

  protected void setActionTextFromScout(String actionText) {
    // When the header text is not set we would like to have the action text in
    // the header area
    if (getScoutObject().getIntroText() == null) {
      setIntroTextFromScout(actionText);
      actionText = null;
    }

    boolean exclude = false;
    if (actionText == null) {
      actionText = "";
      exclude = true;
    }
    m_actionLabel.setText(actionText);
    if (m_actionLabel.getLayoutData() instanceof GridData) {
      GridData gridData = ((GridData) m_actionLabel.getLayoutData());
      if (gridData.exclude != exclude) {
        gridData.exclude = exclude;
        getShell().layout(true, true);
        m_actionLabel.getParent().getParent().layout(true, true);
      }
    }
    // Hide empty labels
    m_actionLabel.setVisible(StringUtility.hasText(m_actionLabel.getText()));
  }

  protected void dettachScout() {
    if (m_scoutMessageBoxListener != null) {
      getScoutObject().removeMessageBoxListener(m_scoutMessageBoxListener);
      m_scoutMessageBoxListener = null;
    }
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    Control header = createHeaderArea(container);
    m_actionLabel = getUiEnvironment().getFormToolkit().createLabel(container, "", SWT.WRAP | SWT.LEFT);

    // layout
    container.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

    GridLayout dialogAreaLayout = new GridLayout(1, true);
    dialogAreaLayout.marginHeight = 12;
    dialogAreaLayout.marginWidth = 12;
    container.setLayout(dialogAreaLayout);

    GridData gridData = new GridData(GridData.GRAB_HORIZONTAL);
    header.setLayoutData(gridData);
    gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
    gridData.horizontalIndent = 5;
    m_actionLabel.setLayoutData(gridData);

    // No control in the dialog area should be in the tab list
    container.setTabList(new Control[]{});
    return container;
  }

  private Control createHeaderArea(Composite parent) {
    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    m_imageLabel = getUiEnvironment().getFormToolkit().createLabel(container, "");
    m_introLabel = getUiEnvironment().getFormToolkit().createLabel(container, "", SWT.WRAP | SWT.LEFT);

    // layout
    GridLayout headerLayout = new GridLayout(2, false);
    headerLayout.marginBottom = 7;
    headerLayout.marginWidth = 0;
    headerLayout.horizontalSpacing = 5;
    container.setLayout(headerLayout);
    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    gridData.exclude = true;
    m_imageLabel.setLayoutData(gridData);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
    gridData.horizontalIndent = 7;
    gridData.verticalIndent = 0;
    gridData.exclude = true;
    m_introLabel.setLayoutData(gridData);
    return container;
  }

  @Override
  protected Control createButtonBar(Composite parent) {
    if (getScoutObject().getYesButtonText() == null
        && getScoutObject().getNoButtonText() == null
        && getScoutObject().getCancelButtonText() == null
        && getScoutObject().getHiddenText() == null) {
      return null;
    }
    Composite buttonArea = getUiEnvironment().getFormToolkit().createComposite(parent);

    int inset = 10;
    Button defaultButton = null;

    // Set the background color of the button bar
    buttonArea.setBackground(getUiEnvironment().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    // yes button
    if (getScoutObject().getYesButtonText() != null) {
      Button yesButton = createButton(buttonArea, getScoutObject().getYesButtonText(), null, IMessageBox.YES_OPTION);
      ButtonBarLayoutData data = new ButtonBarLayoutData();
      data.insetTop = inset;
      data.insetBottom = inset;
      data.insetRight = inset;
      yesButton.setLayoutData(data);
      defaultButton = (defaultButton == null ? yesButton : defaultButton);
    }
    if (getScoutObject().getNoButtonText() != null) {
      Button noButton = createButton(buttonArea, getScoutObject().getNoButtonText(), null, IMessageBox.NO_OPTION);
      ButtonBarLayoutData data = new ButtonBarLayoutData();
      data.insetTop = inset;
      data.insetBottom = inset;
      data.insetRight = inset;
      noButton.setLayoutData(data);
      defaultButton = (defaultButton == null ? noButton : defaultButton);
    }
    if (getScoutObject().getCancelButtonText() != null) {
      Button cancelButton = createButton(buttonArea, getScoutObject().getCancelButtonText(), null, IMessageBox.CANCEL_OPTION);
      ButtonBarLayoutData data = new ButtonBarLayoutData();
      data.insetTop = inset;
      data.insetBottom = inset;
      data.insetRight = inset;
      cancelButton.setLayoutData(data);
      defaultButton = (defaultButton == null ? cancelButton : defaultButton);
    }
    // Set default button
    if (defaultButton != null) {
      defaultButton.setFocus();
      getShell().setDefaultButton(defaultButton);
    }

    // layout
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
    buttonArea.setLayoutData(gridData);

    ButtonBarLayout layout = new ButtonBarLayout(SWT.RIGHT);
    layout.horizontalGap = 0;
    buttonArea.setLayout(layout);
    return buttonArea;
  }

  protected Button createButton(Composite parent, String text, String iconId, int buttonId) {
    Button b = getUiEnvironment().getFormToolkit().createButton(parent, text, SWT.PUSH);
    if (iconId != null) {
      b.setImage(getUiEnvironment().getIcon(iconId));
    }
    if (buttonId >= 0) {
      b.addSelectionListener(new P_RwtButtonListener(buttonId));
    }
    return b;
  }

  @Override
  protected Point getInitialSize() {
    // get the preferred width of the button bar
    Point buttonBarSize = getButtonBar() == null ? new Point(0, 0) : getButtonBar().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

    // get the preferred height of the control
    Point initialSize = getShell().computeSize(UiDecorationExtensionPoint.getLookAndFeel().getMessageBoxMinWidth(), SWT.DEFAULT, true);

    // The width and height of the messageBox is defined in the lookAndFeel extension point.
    // If we have a lot of big buttons we would like to scale the dialog in order to show every button. In this case the width of the button bar is used.
    initialSize.x = Math.max(buttonBarSize.x + 20, UiDecorationExtensionPoint.getLookAndFeel().getMessageBoxMinWidth());
    initialSize.y = Math.max(initialSize.y, UiDecorationExtensionPoint.getLookAndFeel().getMessageBoxMinHeight());
    return initialSize;
  }

  @Override
  public int open() {
    if (getShell() == null || getShell().isDisposed()) {
      create();
    }
    initializeBounds();
    return super.open();
  }

  @Override
  public boolean close() {
    handleUiButtonSelection(IMessageBox.CANCEL_OPTION);
    return false;
  }

  public void closeNoFire() {
    super.close();
  }

  private IRwtEnvironment getUiEnvironment() {
    return m_uiEnvironment;
  }

  public IMessageBox getScoutObject() {
    return m_scoutObject;
  }

  /*
   * event handlers
   */
  protected void handleScoutMessageBoxClosed(MessageBoxEvent e) {
    // dialog model detach
    dettachScout();
    closeNoFire();
  }

  private void handleUiButtonSelection(final int buttonId) {
    Runnable j = new Runnable() {
      @Override
      public void run() {
        getScoutObject().getUIFacade().setResultFromUI(buttonId);
      }
    };
    getUiEnvironment().invokeScoutLater(j, 0);
  }

  private class P_RwtButtonListener extends SelectionAdapter {
    private static final long serialVersionUID = 1L;

    private final int m_buttonId;

    P_RwtButtonListener(int buttonId) {
      m_buttonId = buttonId;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      handleUiButtonSelection(m_buttonId);
    }

  } // end class P_RwtButtonListener

  /*
   * other observers
   */
  private class P_ScoutMessageBoxListener implements MessageBoxListener {
    @Override
    public void messageBoxChanged(final MessageBoxEvent e) {
      switch (e.getType()) {
        case MessageBoxEvent.TYPE_CLOSED: {
          Runnable t = new Runnable() {
            @Override
            public void run() {
              switch (e.getType()) {
                case MessageBoxEvent.TYPE_CLOSED: {
                  handleScoutMessageBoxClosed(e);
                  break;
                }
              }
            }
          };
          getUiEnvironment().invokeUiLater(t);
          break;
        }
      }
    }
  }// end private class

  private class P_DisposeListener implements DisposeListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void widgetDisposed(DisposeEvent event) {
      dettachScout();
    }

  }
}
