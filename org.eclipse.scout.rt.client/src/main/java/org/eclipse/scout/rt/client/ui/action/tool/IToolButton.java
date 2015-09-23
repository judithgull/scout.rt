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
package org.eclipse.scout.rt.client.ui.action.tool;

import org.eclipse.scout.rt.client.ui.action.menu.IMenu;

/**
 * Interface for action with menus that normally appear in the gui on the toolbar
 */

public interface IToolButton extends IMenu {
  // FIXME AWE: should we rename to IToolMenu and extend IMenu?
}
