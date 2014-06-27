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
package org.eclipse.scout.rt.ui.rap.action.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.scout.rt.client.ui.action.IActionFilter;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.RwtMenuUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 *
 */
public class RwtScoutMenuItem {
  private final IMenu m_scoutMenu;

  private final IRwtEnvironment m_environment;
  private final Menu m_parentMenu;
  private MenuItem m_swtMenuItem;
  private boolean m_handleSelectionPending;

  private PropertyChangeListener m_scoutPropertyChangeListener;
  private Listener m_swtSelectionListener;
  private Listener m_swtMenuDisposeListener;

  private IActionFilter m_filter;

  public RwtScoutMenuItem(IMenu scoutMenu, Menu parentMenu, IActionFilter filter, IRwtEnvironment environment) {
    this(scoutMenu, parentMenu, filter, environment, true);
  }

  public RwtScoutMenuItem(IMenu scoutMenu, Menu parentMenu, IActionFilter filter, IRwtEnvironment environment, boolean callInitializer) {
    m_filter = filter;
    m_environment = environment;
    m_scoutMenu = scoutMenu;
    m_parentMenu = parentMenu;
    if (callInitializer) {
      createMenu(scoutMenu, parentMenu, environment);
    }
  }

  protected void createMenu(IMenu scoutMenu, Menu parentMenu, IRwtEnvironment environment) {
    m_swtMenuItem = RwtMenuUtility.createRwtMenuItem(parentMenu, scoutMenu, getFilter(), environment);
    m_swtMenuItem.setData(getScoutMenu());
    m_swtSelectionListener = new P_SwtMenuListener();
    m_swtMenuItem.addListener(SWT.Selection, m_swtSelectionListener);
    m_swtMenuDisposeListener = new P_SwtMenuDisposeListener();
    m_swtMenuItem.addListener(SWT.Dispose, m_swtMenuDisposeListener);

    attachScout();
  }

  /**
   *
   */
  protected void attachScout() {
    m_scoutPropertyChangeListener = new P_ScoutPropertyChangeListener();
    m_scoutMenu.addPropertyChangeListener(m_scoutPropertyChangeListener);
    // init
    updateEnabledFromScout();
    updateIconFromScout();
    updateKeyStrokeFromScout();
    updateTextWithMnemonicFromScout();
    updateTooltipTextFromScout();
    updateSelectedFromScout();
  }

  protected void dettachScout() {
    getScoutMenu().removePropertyChangeListener(m_scoutPropertyChangeListener);
  }

  public IActionFilter getFilter() {
    return m_filter;
  }

  public IRwtEnvironment getEnvironment() {
    return m_environment;
  }

  public IMenu getScoutMenu() {
    return m_scoutMenu;
  }

  public Menu getParentMenu() {
    return m_parentMenu;
  }

  public MenuItem getSwtMenuItem() {
    return m_swtMenuItem;
  }

  private boolean isHandleScoutPropertyChange(String propertyName, Object newValue) {
    return true;
  }

  /**
   * in swt thread
   */
  protected void handleScoutPropertyChange(String name, Object newValue) {
    if (name.equals(IMenu.PROP_ENABLED)) {
      updateEnabledFromScout();
    }
    else if (name.equals(IMenu.PROP_TEXT_WITH_MNEMONIC)) {
      updateTextWithMnemonicFromScout();
    }
    else if (name.equals(IMenu.PROP_TOOLTIP_TEXT)) {
      updateTooltipTextFromScout();
    }
    else if (name.equals(IMenu.PROP_ICON_ID)) {
      updateIconFromScout();
    }
    else if (name.equals(IMenu.PROP_KEYSTROKE)) {
      updateKeyStrokeFromScout();
    }
    else if (name.equals(IMenu.PROP_VISIBLE)) {
      updateVisibilityFromScout();
    }
    else if (name.equals(IMenu.PROP_SELECTED)) {
      updateSelectedFromScout();
    }

  }

  protected void updateKeyStrokeFromScout() {
    if (getSwtMenuItem() != null && !getSwtMenuItem().isDisposed()) {
      // void see settext mnemonic
    }
  }

  protected void updateIconFromScout() {
    if (getSwtMenuItem() != null && !getSwtMenuItem().isDisposed()) {
      getSwtMenuItem().setImage(getEnvironment().getIcon(getScoutMenu().getIconId()));
    }
  }

  protected void updateTooltipTextFromScout() {
    if (getSwtMenuItem() != null && !getSwtMenuItem().isDisposed()) {
      // not supported in swt
    }
  }

  protected void updateTextWithMnemonicFromScout() {
    if (getSwtMenuItem() != null && !getSwtMenuItem().isDisposed()) {
      String text = getScoutMenu().getTextWithMnemonic();
      if (text == null) {
        text = "";
      }
      getSwtMenuItem().setText(text);
    }
  }

  protected void updateEnabledFromScout() {
    if (getSwtMenuItem() != null && !getSwtMenuItem().isDisposed()) {
      getSwtMenuItem().setEnabled(getScoutMenu().isEnabled());
    }
  }

  /**
   *
   */
  protected void updateVisibilityFromScout() {
    // not supported on swt MenuItem
  }

  protected void updateSelectedFromScout() {
    if (getSwtMenuItem() != null && !getSwtMenuItem().isDisposed()) {
      getSwtMenuItem().setSelection(getScoutMenu().isSelected());
    }
  }

  protected void handleSwtMenuSelection() {
    if (!m_handleSelectionPending) {
      m_handleSelectionPending = true;
      //notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          try {
            getScoutMenu().getUIFacade().fireActionFromUI();
          }
          finally {
            m_handleSelectionPending = false;
          }
        }
      };
      getEnvironment().invokeScoutLater(t, 0);
      //end notify
    }
  }

  /**
  *
  */
  protected void handleSwtMenuItemDispose() {
    if (getSwtMenuItem() != null && !getSwtMenuItem().isDisposed()) {
      getSwtMenuItem().removeListener(SWT.Dispose, m_swtMenuDisposeListener);
      m_swtMenuDisposeListener = null;
    }
    getSwtMenuItem().removeListener(SWT.Selection, m_swtSelectionListener);
    m_swtSelectionListener = null;
    dettachScout();
  }

  private class P_ScoutPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent e) {
      if (isHandleScoutPropertyChange(e.getPropertyName(), e.getNewValue())) {
        Runnable t = new Runnable() {
          @Override
          public void run() {
            handleScoutPropertyChange(e.getPropertyName(), e.getNewValue());
          }

        };
        getEnvironment().invokeUiLater(t);
      }
    }
  }

  private class P_SwtMenuListener implements Listener {
    private static final long serialVersionUID = 1L;

    @Override
    public void handleEvent(Event event) {
      switch (event.type) {
        case SWT.Selection:
          handleSwtMenuSelection();
          break;
      }
    }

  }

  private class P_SwtMenuDisposeListener implements Listener {
    private static final long serialVersionUID = 1L;

    @Override
    public void handleEvent(Event event) {
      switch (event.type) {
        case SWT.Dispose:
          handleSwtMenuItemDispose();
          break;
      }
    }
  }
}
