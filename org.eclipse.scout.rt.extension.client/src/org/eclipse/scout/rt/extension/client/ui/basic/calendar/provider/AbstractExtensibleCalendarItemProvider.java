/*******************************************************************************
 * Copyright (c) 2012 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.extension.client.ui.basic.calendar.provider;

import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.calendar.provider.AbstractCalendarItemProvider;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.extension.client.ExtensionUtility;
import org.eclipse.scout.rt.extension.client.IExtensibleScoutObject;
import org.eclipse.scout.rt.extension.client.ui.action.menu.MenuExtensionUtility;

/**
 * Calendar item provider supporting the following Scout extension features:
 * <ul>
 * <li>adding, removing and modifying statically configured menus</li>
 * </ul>
 *
 * @since 3.9.0
 */
public abstract class AbstractExtensibleCalendarItemProvider extends AbstractCalendarItemProvider implements IExtensibleScoutObject {

  public AbstractExtensibleCalendarItemProvider() {
    super();
  }

  public AbstractExtensibleCalendarItemProvider(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected void injectMenusInternal(OrderedCollection<IMenu> menus) {
    super.injectMenusInternal(menus);
    Object enclosingObject = ExtensionUtility.getEnclosingObject(this, IFormField.class);
    if (enclosingObject != null) {
      MenuExtensionUtility.adaptMenus(enclosingObject, this, menus);
    }
  }
}
