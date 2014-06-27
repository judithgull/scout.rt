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
package org.eclipse.scout.rt.ui.rap.window.desktop.nonmodalFormBar;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.tool.IToolButton;
import org.eclipse.scout.rt.ui.rap.basic.RwtScoutComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class RwtScoutFormButton extends RwtScoutComposite<IAction> implements IRwtScoutFormButton {
  private final boolean m_iconVisible;
  private final boolean m_textVisible;
  private String m_variantInActive;
  private String m_variantActive;

  public RwtScoutFormButton(boolean textVisible, boolean iconVisible, String variantInActive, String variantActive) {
    m_textVisible = textVisible;
    m_iconVisible = iconVisible;
    m_variantActive = variantActive;
    m_variantInActive = variantInActive;
  }

  @Override
  protected void initializeUi(Composite parent) {
    Button tabButton = getUiEnvironment().getFormToolkit().createButton(parent, "", SWT.TOGGLE);
    tabButton.setData(RWT.CUSTOM_VARIANT, m_variantInActive);
    setUiField(tabButton);
    makeButtonActive();
    tabButton.addSelectionListener(new SelectionAdapter() {
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetSelected(SelectionEvent e) {
        handleUiSelection();
      }
    });
  }

  @Override
  public void makeButtonActive() {
    Control[] children = getUiField().getParent().getChildren();
    for (Control child : children) {
      child.setData(RWT.CUSTOM_VARIANT, m_variantInActive);
    }
    getUiField().setData(RWT.CUSTOM_VARIANT, m_variantActive);
  }

  @Override
  public void makeButtonInactive() {
    getUiField().setData(RWT.CUSTOM_VARIANT, m_variantInActive);
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    updateIconFromScout();
    updateTextFromScout();
  }

  @Override
  public Button getUiField() {
    return (Button) super.getUiField();
  }

  protected void handleUiSelection() {
    //notify Scout
    Runnable t = new Runnable() {
      @Override
      public void run() {
        getScoutObject().getUIFacade().fireActionFromUI();
      }
    };
    getUiEnvironment().invokeScoutLater(t, 0);
  }

  protected void updateIconFromScout() {
    if (m_iconVisible) {
      getUiField().setImage(getUiEnvironment().getIcon(getScoutObject().getIconId()));
    }
  }

  protected void updateTextFromScout() {
    if (m_textVisible) {
      String text = getScoutObject().getText();
      if (text == null) {
        text = "";
      }
      getUiField().setText(text);
    }
  }

  private void updateEnabledFormScout() {
    getUiField().setEnabled(getScoutObject().isEnabled());
  }

  private void updateVisibleFromScout() {
    getUiField().setVisible(getScoutObject().isVisible());
  }

  @Override
  protected void handleScoutPropertyChange(String name, Object newValue) {
    if (IToolButton.PROP_ICON_ID.equals(name)) {
      updateIconFromScout();
    }
    else if (IToolButton.PROP_TEXT.equals(name)) {
      updateTextFromScout();
    }
    else if (IToolButton.PROP_ENABLED.equals(name)) {
      updateEnabledFormScout();
    }
    else if (IToolButton.PROP_VISIBLE.equals(name)) {
      updateVisibleFromScout();
    }
  }
}
