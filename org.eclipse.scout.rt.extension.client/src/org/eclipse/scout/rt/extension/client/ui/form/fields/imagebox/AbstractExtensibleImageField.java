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
package org.eclipse.scout.rt.extension.client.ui.form.fields.imagebox;

import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.form.fields.imagebox.AbstractImageField;
import org.eclipse.scout.rt.extension.client.IExtensibleScoutObject;
import org.eclipse.scout.rt.extension.client.ui.action.menu.MenuExtensionUtility;

/**
 * Image field supporting the following Scout extension features:
 * <ul>
 * <li>adding, removing and modifying statically configured menus</li>
 * </ul>
 * 
 * @since 3.9.0
 */
@ClassId("784c57b0-3206-4530-b149-cf3e0426737c")
public abstract class AbstractExtensibleImageField extends AbstractImageField implements IExtensibleScoutObject {

  public AbstractExtensibleImageField() {
    super();
  }

  public AbstractExtensibleImageField(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected void injectMenusInternal(OrderedCollection<IMenu> menus) {
    super.injectMenusInternal(menus);
    MenuExtensionUtility.adaptMenus(this, this, menus);
  }
}
