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
package org.eclipse.scout.rt.ui.rap.action;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.rap.rwt.internal.lifecycle.UITestUtil;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.testing.CustomWidgetIdGenerator;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Widget;

@SuppressWarnings("restriction")
public class AbstractRwtMenuAction {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractRwtMenuAction.class);

  private final IAction m_scoutAction;
  private final IRwtEnvironment m_uiEnvironment;

  private boolean m_initialized;
  private boolean m_connectedToScout;
  private boolean m_addKeyStrokeTextEnabled;
  private P_ScoutPropertyChangeListener m_scoutPropertyListener;

  private MenuItem m_uiMenuItem;
  private final Menu m_uiMenu;
  private SelectionListener m_menuSelectionListener;

  public AbstractRwtMenuAction(Menu uiMenu, IAction scoutAction, IRwtEnvironment uiEnvironment, boolean callInitializer) {
    m_uiMenu = uiMenu;
    m_scoutAction = scoutAction;
    m_uiEnvironment = uiEnvironment;
    m_addKeyStrokeTextEnabled = true;
    if (callInitializer) {
      init();
    }

    m_uiMenu.addDisposeListener(new DisposeListener() {
      private static final long serialVersionUID = 1L;

      @Override
      public void widgetDisposed(DisposeEvent event) {
        disconnectFromScout();
      }
    });
  }

  public void init() {
    callInitializers(m_uiMenu);
  }

  protected final void callInitializers(Menu menu) {
    if (m_initialized) {
      return;
    }
    else {
      m_initialized = true;
      //
      initializeUi(menu);
      connectToScout();
    }
  }

  protected final void connectToScout() {
    if (!m_connectedToScout) {
      attachScoutListeners();
      applyScoutProperties();
      m_connectedToScout = true;
    }
  }

  protected final void disconnectFromScout() {
    if (m_connectedToScout) {
      detachScoutListeners();
      m_connectedToScout = false;
    }
  }

  protected void attachScoutListeners() {
    if (m_scoutPropertyListener == null) {
      m_scoutPropertyListener = new P_ScoutPropertyChangeListener();
      m_scoutAction.addPropertyChangeListener(m_scoutPropertyListener);
    }
  }

  protected void detachScoutListeners() {
    if (m_scoutPropertyListener != null) {
      m_scoutAction.removePropertyChangeListener(m_scoutPropertyListener);
      m_scoutPropertyListener = null;
    }
  }

  protected void applyScoutProperties() {
    IAction scoutAction = getScoutAction();
    setEnabledFromScout(scoutAction.isEnabled());
    setTextFromScout(scoutAction.getTextWithMnemonic());
    setTooltipTextFromScout(scoutAction.getTooltipText());
    setIconFromScout(scoutAction.getIconId());
  }

  protected void setIconFromScout(String iconId) {
    if (!getUiMenuItem().isDisposed()) {
      getUiMenuItem().setImage(getUiEnvironment().getIcon(iconId));
    }
  }

  protected void setTooltipTextFromScout(String tooltipText) {
    if (!StringUtility.isNullOrEmpty(tooltipText)) {
      LOG.warn("unsuported method on rwt");
    }
  }

  protected void setTextFromScout(String text) {
    if (getUiMenuItem().isDisposed()) {
      return;
    }

    if (text == null) {
      text = "";
    }

    if (isAddKeyStrokeTextEnabled()) {
      IAction action = getScoutAction();
      if (action != null && StringUtility.hasText(action.getKeyStroke())) {
        text += "\t" + RwtUtility.getKeyStrokePrettyPrinted(action);
      }
    }
    getUiMenuItem().setText(text);
  }

  protected void setEnabledFromScout(boolean enabled) {
    if (!getUiMenuItem().isDisposed()) {
      getUiMenuItem().setEnabled(enabled);
    }
  }

  protected void initializeUi(Menu menu) {
  }

  public IAction getScoutAction() {
    return m_scoutAction;
  }

  protected IRwtEnvironment getUiEnvironment() {
    return m_uiEnvironment;
  }

  public MenuItem getUiMenuItem() {
    return m_uiMenuItem;
  }

  public void setUiMenuItem(MenuItem uiMenuItem) {
    if (m_uiMenuItem != null) {
      m_uiMenuItem.removeSelectionListener(m_menuSelectionListener);
    }
    m_uiMenuItem = uiMenuItem;
    if (m_menuSelectionListener == null) {
      m_menuSelectionListener = new P_UiMenuItemSelectionListener();
    }

    m_uiMenuItem.addSelectionListener(m_menuSelectionListener);
    setCustomWidgetIds(m_uiMenuItem);
  }

  private void setCustomWidgetIds(Widget parent) {
    if (!UITestUtil.isEnabled()) {
      return;
    }

    CustomWidgetIdGenerator.getInstance().setCustomWidgetIds(parent, getScoutAction(), getClass().getName());
  }

  protected Menu getMenu() {
    return m_uiMenu;
  }

  private void handleUiAction() {
    RwtUtility.runUiInputVerifier();
    Runnable t = new Runnable() {
      @Override
      public void run() {
        getScoutAction().getUIFacade().fireActionFromUI();
      }
    };
    getUiEnvironment().invokeScoutLater(t, 0);
  }

  private boolean isHandleScoutPropertyChange(String propertyName, Object newValue) {
    return true;
  }

  public boolean isAddKeyStrokeTextEnabled() {
    return m_addKeyStrokeTextEnabled;
  }

  public void setAddKeyStrokeTextEnabled(boolean addKeyStrokeTextEnabled) {
    m_addKeyStrokeTextEnabled = addKeyStrokeTextEnabled;
  }

  /**
   * in rwt thread
   */
  protected void handleScoutPropertyChange(String name, Object newValue) {
    if (name.equals(IAction.PROP_ENABLED)) {
      setEnabledFromScout(((Boolean) newValue).booleanValue());
    }
    else if (name.equals(IAction.PROP_TEXT)) {
      setTextFromScout((String) newValue);
    }
    else if (name.equals(IAction.PROP_TOOLTIP_TEXT)) {
      setTooltipTextFromScout((String) newValue);
    }
    else if (name.equals(IAction.PROP_ICON_ID)) {
      setIconFromScout((String) newValue);
    }
    else if (name.equals(IAction.PROP_KEYSTROKE)) {
      //Menu keystrokes are handled along with regular keystrokes, therefore it's only necessary to set a textual hint
      setTextFromScout(getScoutAction().getText());
    }
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
        getUiEnvironment().invokeUiLater(t);
      }
    }

  }// end private class

  private class P_UiMenuItemSelectionListener extends SelectionAdapter {
    private static final long serialVersionUID = 1L;

    @Override
    public void widgetSelected(SelectionEvent e) {
      handleUiAction();
    }

  }
}
