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
package org.eclipse.scout.rt.client.ui.form.fields.button;

import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.IContextMenu;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;

/**
 * A Button has 2 aspects
 * <ol>
 * <li>System-button / NonSystem-button is marked by getSystemType()<br>
 * System buttons in a dialog have a pre-defined action handling
 * <li>Process-button / NonProcess-button is marked by isProcessButton()<br>
 * Process buttons are normally placed on dialogs button bar on the lower dialog bar
 * </ol>
 */
public interface IButton extends IFormField {
  /*
   * Properties
   */
  String PROP_ICON_ID = "iconId";
  String PROP_IMAGE = "image";
  String PROP_SELECTED = "selected";
  String PROP_CONTEXT_MENU = "contextMenu";

  /*
   * System Types
   */
  int SYSTEM_TYPE_NONE = 0;
  int SYSTEM_TYPE_CANCEL = 1;
  int SYSTEM_TYPE_CLOSE = 2;
  int SYSTEM_TYPE_OK = 3;
  int SYSTEM_TYPE_RESET = 4;
  int SYSTEM_TYPE_SAVE = 5;
  int SYSTEM_TYPE_SAVE_WITHOUT_MARKER_CHANGE = 6;
  /*
   * Display Styles
   */
  int DISPLAY_STYLE_DEFAULT = 0;
  int DISPLAY_STYLE_TOGGLE = 1;
  int DISPLAY_STYLE_RADIO = 2;
  int DISPLAY_STYLE_LINK = 3;

  void doClick() throws ProcessingException;

  int getSystemType();

  void setSystemType(int systemType);

  boolean isProcessButton();

  void setProcessButton(boolean on);

  void addButtonListener(ButtonListener listener);

  void removeButtonListener(ButtonListener listener);

  String getIconId();

  void setIconId(String iconId);

  Object getImage();

  void setImage(Object nativeImg);

  /**
   * display style<br>
   * default, toggle, radio
   */
  int getDisplayStyle();

  /**
   * do not change this property when ui is attached<br>
   * major gui frameworks don't support runtime itemtype changes
   */
  void setDisplayStyleInternal(int i);

  /**
   * @return toggle and radio button state
   */
  boolean isSelected();

  /**
   * @param b
   *          toggle and radio button state
   */
  void setSelected(boolean b);

  /**
   * In case button was pressed and armed, this event sets armed=false<br>
   * and prevents the button from firing an action
   */
  void disarm();

  /**
   * request showing the (dropdown) menu popup
   */
  void requestPopup();

  IButtonUIFacade getUIFacade();

  /**
   * @return
   */
  List<IMenu> getMenus();

  /**
   * @return
   */
  IContextMenu getContextMenu();
}
