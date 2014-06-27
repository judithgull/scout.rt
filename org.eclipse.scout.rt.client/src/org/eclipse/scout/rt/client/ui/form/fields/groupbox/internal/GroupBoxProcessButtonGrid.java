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
package org.eclipse.scout.rt.client.ui.form.fields.groupbox.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.form.fields.GridData;
import org.eclipse.scout.rt.client.ui.form.fields.button.IButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.IGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.internal.GridDataBuilder;

/**
 * Grid (model) layout of process buttons only visible process-buttons are used
 */
public class GroupBoxProcessButtonGrid {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(GroupBoxProcessButtonGrid.class);

  private IGroupBox m_groupBox = null;
  private boolean m_includeSystemButtons;
  private boolean m_includeCustomButtons;
  private List<IButton> m_buttons;

  public GroupBoxProcessButtonGrid(IGroupBox groupBox, boolean includeCustomButtons, boolean includeSystemButtons) {
    m_groupBox = groupBox;
    m_includeCustomButtons = includeCustomButtons;
    m_includeSystemButtons = includeSystemButtons;
  }

  public void validate() {
    // reset
    List<IButton> buttonList = new ArrayList<IButton>();
    if (m_includeCustomButtons) {
      buttonList.addAll(m_groupBox.getCustomProcessButtons());
    }
    if (m_includeSystemButtons) {
      buttonList.addAll(m_groupBox.getSystemProcessButtons());
    }
    // filter
    for (Iterator it = buttonList.iterator(); it.hasNext();) {
      IButton b = (IButton) it.next();
      if (b.isVisible()) {
      }
      else {
        GridData data = GridDataBuilder.createFromHints(b, 1);
        b.setGridDataInternal(data);
        it.remove();
      }
    }
    m_buttons = buttonList;
    layoutStatic();
  }

  private void layoutStatic() {
    int i = 0;
    for (IButton button : m_buttons) {
      GridData data = GridDataBuilder.createFromHints(button, 1);
      data.x = i;
      data.y = 0;
      button.setGridDataInternal(data);
      i++;
    }
  }
}
