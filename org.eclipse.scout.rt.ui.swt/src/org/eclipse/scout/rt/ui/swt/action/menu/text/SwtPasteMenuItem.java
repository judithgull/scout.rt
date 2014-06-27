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
package org.eclipse.scout.rt.ui.swt.action.menu.text;

import org.eclipse.scout.rt.ui.swt.util.SwtUtility;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

/**
 *
 */
public class SwtPasteMenuItem extends AbstractTextSystemMenuItem {

  /**
   * @param menu
   * @param label
   * @param textAccess
   */
  public SwtPasteMenuItem(Menu menu, ITextAccess textAccess) {
    super(menu, SwtUtility.getNlsText(Display.getCurrent(), "Paste"), textAccess);
  }

  @Override
  protected void updateEnability() {
    setEnabled(getTextAccess().isEnabled() && getTextAccess().isEditable() && getTextAccess().hasTextOnClipboard());

  }

  @Override
  protected void doAction() {
    getTextAccess().paste();
  }

}
