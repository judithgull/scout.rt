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
package org.eclipse.scout.rt.ui.rap.form.fields.button;

import java.util.Arrays;

import org.eclipse.scout.commons.OptimisticLock;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.WeakEventListener;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.form.fields.button.ButtonEvent;
import org.eclipse.scout.rt.client.ui.form.fields.button.ButtonListener;
import org.eclipse.scout.rt.client.ui.form.fields.button.IButton;
import org.eclipse.scout.rt.ui.rap.LogicalGridData;
import org.eclipse.scout.rt.ui.rap.LogicalGridLayout;
import org.eclipse.scout.rt.ui.rap.RwtMenuUtility;
import org.eclipse.scout.rt.ui.rap.action.MenuSizeEstimator;
import org.eclipse.scout.rt.ui.rap.ext.ButtonEx;
import org.eclipse.scout.rt.ui.rap.ext.MenuAdapterEx;
import org.eclipse.scout.rt.ui.rap.ext.MultilineButton;
import org.eclipse.scout.rt.ui.rap.ext.MultilineRadioButton;
import org.eclipse.scout.rt.ui.rap.extension.IUiDecoration;
import org.eclipse.scout.rt.ui.rap.extension.UiDecorationExtensionPoint;
import org.eclipse.scout.rt.ui.rap.form.fields.RwtScoutFieldComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * @since 3.8.0
 */
public class RwtScoutButton extends RwtScoutFieldComposite<IButton> implements IRwtScoutButton {
  private static IScoutLogger LOG = ScoutLogManager.getLogger(RwtScoutButton.class);

  private ButtonListener m_scoutButtonListener;
  private OptimisticLock m_selectionLock;
  //ticket 86811: avoid double-action in queue
  private boolean m_handleActionPending;

  private IMenu[] m_scoutActions;

  public RwtScoutButton() {
    m_selectionLock = new OptimisticLock();
  }

  @Override
  protected void initializeUi(Composite parent) {
    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    setUiContainer(container);
    Control uiFieldAsButton = null;
    Hyperlink uiFieldAsLink = null;
    switch (getScoutObject().getDisplayStyle()) {
      case IButton.DISPLAY_STYLE_RADIO: {
        MultilineRadioButton uiButton = getUiEnvironment().getFormToolkit().createMultilineRadioButton(container);
        uiFieldAsButton = uiButton;
        break;
      }
      case IButton.DISPLAY_STYLE_TOGGLE: {
        ButtonEx uiButton = getUiEnvironment().getFormToolkit().createButtonEx(container, SWT.CENTER | SWT.TOGGLE);
        uiFieldAsButton = uiButton;
        break;
      }
      case IButton.DISPLAY_STYLE_LINK: {
        HyperlinkGroup linkGroup = getUiEnvironment().getFormToolkit().getHyperlinkGroup();
        linkGroup.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
        int style = SWT.CENTER;
        Hyperlink uiLink = getUiEnvironment().getFormToolkit().createHyperlink(container, "", style);
        uiFieldAsLink = uiLink;
        break;
      }
      default: {
        int style = SWT.CENTER | SWT.PUSH;
        if (getScoutObject().hasMenus()) {
          style |= SWT.DROP_DOWN;
        }
        ButtonEx uiButton = getUiEnvironment().getFormToolkit().createButtonEx(container, style);
        uiButton.setDropDownEnabled(true);
        uiFieldAsButton = uiButton;
      }
    }
    //
    setUiLabel(null);
    if (uiFieldAsButton != null) {

      // attach rwt listeners
      uiFieldAsButton.addListener(ButtonEx.SELECTION_ACTION, new P_RwtSelectionListener());
      uiFieldAsButton.addMenuDetectListener(new P_RwtMenuDetectListener());

      setUiField(uiFieldAsButton);

      LogicalGridData gd = (LogicalGridData) getUiField().getLayoutData();
      adaptButtonLayoutData(gd);
    }
    else if (uiFieldAsLink != null) {
      uiFieldAsLink.addHyperlinkListener(new HyperlinkAdapter() {
        @Override
        public void linkActivated(HyperlinkEvent e) {
          handleUiAction();
        }
      });
      setUiField(uiFieldAsLink);
      getUiContainer().setTabList(new Control[]{uiFieldAsLink});
    }
    // layout
    getUiContainer().setLayout(new LogicalGridLayout(0, 0));
  }

  protected void adaptButtonLayoutData(LogicalGridData gd) {
    //set default button height
    if (!getScoutObject().isProcessButton() || gd.useUiHeight) {
      return;
    }

    gd.useUiHeight = true;
    IUiDecoration deco = UiDecorationExtensionPoint.getLookAndFeel();
    gd.heightHint = deco.getProcessButtonHeight();
  }

  @Override
  protected void setBackgroundFromScout(String scoutColor) {
    // XXX hstaudacher We need to override this method because when not it overrides RWT theme
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    IButton b = getScoutObject();
    if (b.hasMenus()) {
      // XXX button menus
    }
    setIconIdFromScout(b.getIconId());
    setImageFromScout(b.getImage());
    setSelectionFromScout(b.isSelected());
    setLabelFromScout(b.getLabel());
    if (m_scoutButtonListener == null) {
      m_scoutButtonListener = new P_ScoutButtonListener();
      getScoutObject().addButtonListener(m_scoutButtonListener);
    }
  }

  @Override
  protected void detachScout() {
    if (m_scoutButtonListener != null) {
      getScoutObject().removeButtonListener(m_scoutButtonListener);
      m_scoutButtonListener = null;
    }
    super.detachScout();
  }

  @Override
  protected void setForegroundFromScout(String scoutColor) {
    if (getScoutObject().getDisplayStyle() == IButton.DISPLAY_STYLE_LINK && scoutColor == null) {
      // XXX BSH Get rid of hard-coded color value when bug 346438 is fixed
      scoutColor = "67A8CE";
    }
    super.setForegroundFromScout(scoutColor);
  }

  protected void setIconIdFromScout(String s) {
    if (s != null) {
      Image icon = getUiEnvironment().getIcon(s);
      Control comp = getUiField();
      if (comp instanceof Button) {
        Button b = (Button) comp;
        b.setImage(icon);
      }
      else if (comp instanceof MultilineButton) {
        MultilineButton b = (MultilineButton) comp;
        b.setImage(icon);
      }
      else if (comp instanceof Hyperlink) {
      }
    }
  }

  @Override
  protected void setLabelFromScout(String s) {
    Control comp = getUiField();
    if (comp instanceof Button) {
      Button b = (Button) comp;
      String label = s;
      b.setText(label == null ? "" : label);
    }
    else if (comp instanceof Hyperlink) {
      Hyperlink t = (Hyperlink) comp;
      String label = StringUtility.removeMnemonic(s);
      t.setText(label == null ? "" : label);
    }
    else if (comp instanceof MultilineRadioButton) {
      MultilineRadioButton b = (MultilineRadioButton) comp;
      b.setText(s == null ? "" : s);
    }
  }

  protected void setSelectionFromScout(boolean b) {
    try {
      if (m_selectionLock.acquire()) {
        switch (getScoutObject().getDisplayStyle()) {
          case IButton.DISPLAY_STYLE_RADIO:
          case IButton.DISPLAY_STYLE_TOGGLE:
            Control comp = getUiField();
            if (comp instanceof Button) {
              Button but = (Button) comp;
              if (b != but.getSelection()) {
                but.setSelection(b);
              }
            }
            else if (comp instanceof MultilineRadioButton) {
              MultilineRadioButton but = (MultilineRadioButton) comp;
              if (b != but.getSelection()) {
                but.setSelection(b);
              }
            }
            break;
        }
      }
    }
    finally {
      m_selectionLock.release();
    }
  }

  protected void setSelectionFromUi(final boolean b) {
    try {
      if (m_selectionLock.acquire()) {
        //notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            if (getScoutObject().isSelected() != b) {
              getScoutObject().getUIFacade().setSelectedFromUI(b);
            }
            getScoutObject().getUIFacade().fireButtonClickedFromUI();
          }
        };
        getUiEnvironment().invokeScoutLater(t, 0);
        //end notify
      }
    }
    finally {
      m_selectionLock.release();
    }
  }

  protected void handleUiAction() {
    if (!m_handleActionPending) {
      m_handleActionPending = true;
      //notify Scout
      Runnable t = new Runnable() {
        @Override
        public void run() {
          try {
            getScoutObject().getUIFacade().fireButtonClickedFromUI();
          }
          finally {
            m_handleActionPending = false;
          }
        }
      };
      getUiEnvironment().invokeScoutLater(t, 0);
      //end notify
    }
  }

  protected void setImageFromScout(Object img) {
    if (img instanceof Image) {
      Control comp = getUiField();
      if (comp instanceof Button) {
        Button b = (Button) comp;
        b.setImage((Image) img);
      }
      else if (comp instanceof MultilineButton) {
        MultilineButton b = (MultilineButton) comp;
        b.setImage((Image) img);
      }
      else if (comp instanceof Hyperlink) {
      }
    }
  }

  protected void disarmButtonFromScout() {
  }

  protected void requestPopupFromScout() {
    Menu menu = createMenu();

    m_scoutActions = RwtMenuUtility.collectMenus(getScoutObject(), getUiEnvironment());
    int menuHeight = new MenuSizeEstimator(menu).estimateMenuHeight(Arrays.asList(m_scoutActions));
    Point menuPosition = null;
    if (shouldMenuOpenOnTop(menuHeight)) {
      menuPosition = computeMenuPositionForTop(menuHeight);
    }
    else {
      menuPosition = computeMenuPositionForBottom();
    }

    showMenu(menu, menuPosition);
  }

  private Menu createMenu() {
    if (getUiField().getMenu() != null) {
      getUiField().getMenu().dispose();
      getUiField().setMenu(null);
    }
    Menu contextMenu = new Menu(getUiField().getShell(), SWT.POP_UP);
    contextMenu.addMenuListener(new P_ContextMenuListener(getUiField(), getUiField().getParent()));
    getUiField().setMenu(contextMenu);

    return contextMenu;
  }

  private void createAndShowMenu(Point location) {
    Menu menu = createMenu();
    showMenu(menu, location);
  }

  private void showMenu(Menu menu, Point location) {
    menu.setLocation(location);
    menu.setVisible(true);
  }

  private boolean shouldMenuOpenOnTop(int menuHeight) {
    Rectangle buttonBounds = getUiField().getBounds();
    Point menuLocationAbsolute = getUiField().getParent().toDisplay(buttonBounds.x, buttonBounds.y);
    int displayHeight = getUiEnvironment().getDisplay().getBounds().height;
    return menuLocationAbsolute.y + buttonBounds.height + menuHeight > displayHeight;
  }

  private Point computeMenuPositionForTop(int menuHeight) {
    Rectangle buttonBounds = getUiField().getBounds();
    int menuLocationX = buttonBounds.x;
    int menuLocationY = buttonBounds.y - menuHeight;
    return getUiField().toDisplay(menuLocationX, menuLocationY);
  }

  private Point computeMenuPositionForBottom() {
    Rectangle buttonBounds = getUiField().getBounds();
    int menuLocationX = buttonBounds.x;
    int menuLocationY = buttonBounds.y + buttonBounds.height;
    return getUiField().toDisplay(menuLocationX, menuLocationY);
  }

  /**
   * in rwt thread
   */
  @Override
  protected void handleScoutPropertyChange(String name, Object newValue) {
    super.handleScoutPropertyChange(name, newValue);
    if (name.equals(IButton.PROP_ICON_ID)) {
      setIconIdFromScout((String) newValue);
    }
    else if (name.equals(IButton.PROP_IMAGE)) {
      setImageFromScout(newValue);
    }
    else if (name.equals(IButton.PROP_SELECTED)) {
      setSelectionFromScout(((Boolean) newValue).booleanValue());
    }
  }

  private class P_RwtSelectionListener implements Listener {
    private static final long serialVersionUID = 1L;

    @Override
    public void handleEvent(Event event) {
      switch (event.type) {
        case ButtonEx.SELECTION_ACTION: {
          switch (getScoutObject().getDisplayStyle()) {
            case IButton.DISPLAY_STYLE_RADIO:
            case IButton.DISPLAY_STYLE_TOGGLE: {
              if (getUiField() instanceof Button) {
                setSelectionFromUi(((Button) getUiField()).getSelection());
              }
              else if (getUiField() instanceof MultilineRadioButton) {
                setSelectionFromUi(((MultilineRadioButton) getUiField()).getSelection());
              }
              break;
            }
            default: {
              handleUiAction();
              break;
            }
          }
          break;
        }
      }
    }
  }// end private class

  private class P_ScoutButtonListener implements ButtonListener, WeakEventListener {
    @Override
    public void buttonChanged(ButtonEvent e) {
      switch (e.getType()) {
        case ButtonEvent.TYPE_DISARM: {
          getUiEnvironment().invokeUiLater(
              new Runnable() {
                @Override
                public void run() {
                  disarmButtonFromScout();
                }
              });
          break;
        }
        case ButtonEvent.TYPE_REQUEST_POPUP: {
          getUiEnvironment().invokeUiLater(
              new Runnable() {
                @Override
                public void run() {
                  requestPopupFromScout();
                }
              });
          break;
        }
      }
    }
  } // end class P_ScoutButtonListener

  private class P_ContextMenuListener extends MenuAdapterEx {
    private static final long serialVersionUID = 1L;

    public P_ContextMenuListener(Control menuControl, Control keyStrokeWidget) {
      super(menuControl, keyStrokeWidget);
    }

    @Override
    public void menuShown(MenuEvent e) {
      super.menuShown(e);

      try {
        if (m_scoutActions == null) {
          m_scoutActions = RwtMenuUtility.collectMenus(getScoutObject(), getUiEnvironment());
        }
        Menu menu = ((Menu) e.getSource());
        RwtMenuUtility.fillContextMenu(m_scoutActions, RwtScoutButton.this.getUiEnvironment(), menu);
      }
      finally {
        m_scoutActions = null;
      }
    }
  } // end class P_ContextMenuListener

  private class P_RwtMenuDetectListener implements MenuDetectListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void menuDetected(MenuDetectEvent e) {
      createAndShowMenu(new Point(e.x, e.y));
    }

  }
}
