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
package org.eclipse.scout.rt.client.ui.action.menu.root.internal;

import java.beans.PropertyChangeEvent;
import java.util.List;

import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.IActionVisitor;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.AbstractPropertyObserverContextMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.IFormFieldContextMenu;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;

/**
 *
 */
public class FormFieldContextMenu<T extends IFormField> extends AbstractPropertyObserverContextMenu<T> implements IFormFieldContextMenu {

  /**
   * @param owner
   */
  public FormFieldContextMenu(T owner, List<? extends IMenu> initialChildMenus) {
    super(owner, initialChildMenus);
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    handleOwnerEnabledChanged();
  }

  @Override
  protected void afterChildMenusAdd(List<? extends IMenu> newChildMenus) {
    super.afterChildMenusAdd(newChildMenus);
    handleOwnerEnabledChanged();
  }

  @Override
  protected void afterChildMenusRemove(List<? extends IMenu> childMenusToRemove) {
    super.afterChildMenusRemove(childMenusToRemove);
    handleOwnerEnabledChanged();
  }

  protected void handleOwnerEnabledChanged() {
    if (getOwner() != null) {
      final boolean enabled = getOwner().isEnabled();
      acceptVisitor(new IActionVisitor() {
        @Override
        public int visit(IAction action) {
          if (action instanceof IMenu) {
            IMenu menu = (IMenu) action;
            if (!menu.hasChildActions() && menu.isInheritAccessibility()) {
              menu.setEnabled(enabled);
            }
          }
          return CONTINUE;
        }
      });
    }
  }

  @Override
  protected void handleOwnerPropertyChanged(PropertyChangeEvent evt) {
    super.handleOwnerPropertyChanged(evt);
    if (IFormField.PROP_ENABLED.equals(evt.getPropertyName())) {
      handleOwnerEnabledChanged();
    }
  }
}
