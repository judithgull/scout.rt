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
package org.eclipse.scout.rt.ui.rap.ext;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;

public class DropDownButton extends Button {
  private static final long serialVersionUID = 1L;

  private Rectangle m_buttonArea = new Rectangle(1, 1, 13, 17);
  private Rectangle m_dropDownArea = new Rectangle(14, 1, 10, 17);
  private EventListenerList m_eventListeners = new EventListenerList();

  private Point m_mouseDownPosition;
  private boolean m_dropdownEnabled = true;
  private boolean m_buttonEnabled = true;
  private MouseListener m_mouseListener;

  private String m_originalVariant = "";

  public DropDownButton(Composite parent, int style) {
    super(parent, style | SWT.DOUBLE_BUFFERED);
    addOrRemoveMouseListener();
  }

  protected void addOrRemoveMouseListener() {
    if (isButtonEnabled() || isDropdownEnabled()) {
      addMouseListener();
    }
    else {
      removeMouseListener();
    }
  }

  protected void addMouseListener() {
    if (m_mouseListener != null) {
      return;
    }
    m_mouseListener = new MouseAdapter() {
      private static final long serialVersionUID = 1L;

      @Override
      public void mouseUp(MouseEvent event) {
        if (event.button == 1) {
          handleSelectionInternal(event);
        }
      }
    };
    addMouseListener(m_mouseListener);
  }

  protected void removeMouseListener() {
    if (m_mouseListener == null) {
      return;
    }
    removeMouseListener(m_mouseListener);
    m_mouseListener = null;
  }

  /**
   * since tab list on parent does not work
   */
  @Override
  public boolean forceFocus() {
    if ((getStyle() & SWT.NO_FOCUS) != 0) {
      return false;
    }
    else {
      return super.forceFocus();
    }
  }

  protected void handleSelectionInternal(MouseEvent event) {
    Point pt = new Point(event.x, event.y);
    if (m_buttonArea.contains(pt) && isButtonEnabled()) {
      Event e = new Event();
      e.button = event.button;
      e.count = 1;
      e.data = event.data;
      e.display = event.display;
      e.stateMask = event.stateMask;
      e.time = event.time;
      e.widget = event.widget;
      e.x = event.x;
      e.y = event.y;
      fireSelectionEvent(new SelectionEvent(e));
    }
    else if (m_dropDownArea.contains(pt) && isDropdownEnabled()) {
      Menu menu = createMenu();
      if (menu != null) {
        menu.setLocation(toDisplay(event.x, event.y));
        menu.setVisible(true);
      }
    }
  }

  private Menu createMenu() {
    if (getMenu() != null) {
      getMenu().dispose();
      setMenu(null);
    }
    Menu contextMenu = new Menu(getShell(), SWT.POP_UP);
    for (MenuListener listener : m_eventListeners.getListeners(MenuListener.class)) {
      contextMenu.addMenuListener(listener);
    }

    setMenu(contextMenu);

    return contextMenu;
  }

  public void fireSelectionEvent(SelectionEvent e) {
    if (isButtonEnabled()) {
      for (SelectionListener l : m_eventListeners.getListeners(SelectionListener.class)) {
        l.widgetSelected(e);
      }
    }
  }

  @Override
  public void addSelectionListener(SelectionListener listener) {
    m_eventListeners.add(SelectionListener.class, listener);
  }

  @Override
  public void removeSelectionListener(SelectionListener listener) {
    m_eventListeners.remove(SelectionListener.class, listener);
  }

  public void addMenuListener(MenuListener listener) {
    m_eventListeners.add(MenuListener.class, listener);
  }

  public void removeMenuListener(MenuListener listener) {
    m_eventListeners.remove(MenuListener.class, listener);
  }

  public void setDropdownEnabled(boolean enabled) {
    m_dropdownEnabled = enabled;
    setCustomVariant();
    addOrRemoveMouseListener();
  }

  public boolean isDropdownEnabled() {
    return m_dropdownEnabled;
  }

  public void setButtonEnabled(boolean enabled) {
    m_buttonEnabled = enabled;
    setCustomVariant();
    addOrRemoveMouseListener();
  }

  public boolean isButtonEnabled() {
    return m_buttonEnabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    m_buttonEnabled = enabled;
    m_dropdownEnabled = enabled;
  }

  private void setCustomVariant() {
    if (!StringUtility.hasText(m_originalVariant)) {
      m_originalVariant = (String) getData(RWT.CUSTOM_VARIANT);
    }
    // valid combinations: smartfield_browse, smartfield_browse_disabled, smartfield_browse_menu, smartfield_browse_disabled_menu
    // impossible combinations: smartfield_browse_menu_disabled, smartfield_browse_disabled_menu_disabled
    if (m_originalVariant != null) {
      String customVariant = m_originalVariant;
      if (!m_buttonEnabled) {
        customVariant += "_disabled";
      }
      if (m_dropdownEnabled) {
        customVariant += "_menu";
      }
      setData(RWT.CUSTOM_VARIANT, customVariant);
    }
  }

  @Override
  protected void checkSubclass() {
    // allow subclassing
  }

  private IRwtEnvironment getUiEnvironment() {
    return (IRwtEnvironment) getDisplay().getData(IRwtEnvironment.class.getName());
  }

}
