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
package org.eclipse.scout.rt.client.ui.action.menu.root;

import java.util.EventObject;

/**
 *
 */
public class ContextMenuEvent extends EventObject {
  private static final long serialVersionUID = 1L;

  public static int TYPE_STRUCTURE_CHANGED = 1;

  private final int m_eventType;

  public ContextMenuEvent(IContextMenu source, int eventType) {
    super(source);
    m_eventType = eventType;
  }

  @Override
  public IContextMenu getSource() {
    return (IContextMenu) super.getSource();
  }

  public int getEventType() {
    return m_eventType;
  }
}
