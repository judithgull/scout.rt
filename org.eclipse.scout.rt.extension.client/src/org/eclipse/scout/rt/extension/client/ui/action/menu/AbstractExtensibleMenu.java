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
package org.eclipse.scout.rt.extension.client.ui.action.menu;

import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.rt.client.ui.action.menu.AbstractMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.extension.client.IExtensibleScoutObject;

/**
 * Menu supporting the following Scout extension features:
 * <ul>
 * <li>adding, removing and modifying statically configured sub menus</li>
 * </ul>
 *
 * @since 3.9.0
 */
public abstract class AbstractExtensibleMenu extends AbstractMenu implements IExtensibleScoutObject {

  public AbstractExtensibleMenu() {
    super();
  }

  public AbstractExtensibleMenu(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected void injectActionNodesInternal(OrderedCollection<IMenu> actionNodes) {
    super.injectActionNodesInternal(actionNodes);
    MenuExtensionUtility.adaptMenus(this, this, actionNodes);
  }
}
