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

import org.eclipse.scout.rt.ui.swt.action.menu.SwtScoutContextMenu;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 *
 */
public abstract class AbstractTextSystemMenuItem extends MenuItem {
  private ITextAccess m_textAccess;

  public AbstractTextSystemMenuItem(Menu menu, String label, ITextAccess textAccess) {
    super(menu, SWT.PUSH);
    setData(SwtScoutContextMenu.DATA_SYSTEM_MENU, Boolean.TRUE);
    setText(label);
    m_textAccess = textAccess;
    initMenuItem(menu);
  }

  @Override
  protected void checkSubclass() {

  }

  protected void initMenuItem(Menu parentMenu) {
    addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        doAction();
      }

    });
    parentMenu.addListener(SWT.Show, new Listener() {
      @Override
      public void handleEvent(Event event) {
        updateEnability();
      }
    });

  }

  public ITextAccess getTextAccess() {
    return m_textAccess;
  }

  /**
   *
   */
  protected void updateEnability() {
  }

  /**
   *
   */
  protected void doAction() {
  }
}
