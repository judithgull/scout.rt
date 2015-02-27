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
package org.eclipse.scout.rt.client.ui.basic.table.menus;

import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.services.common.clipboard.IClipboardService;
import org.eclipse.scout.rt.client.ui.action.menu.AbstractMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenuType;
import org.eclipse.scout.rt.client.ui.action.menu.TableMenuType;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.platform.Platform;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.ui.UserAgentUtility;
import org.eclipse.scout.service.SERVICES;

public class CopyWidthsOfColumnsMenu extends AbstractMenu {

  public static final String COLUMN_COPY_CLIPBOARD_IDENTIFIER = "dev.table.menu.column.width.copy.ident";
  private final ITable m_table;

  // FIXME AWE: (table) call 'copy column widths' menu from html UI
  public CopyWidthsOfColumnsMenu(ITable table) {
    m_table = table;
  }

  @Override
  protected String getConfiguredText() {
    return ScoutTexts.get("CopyWidthsOfColumnsMenu");
  }

  @Override
  protected boolean getConfiguredInheritAccessibility() {
    return false;
  }

  @Override
  protected Set<IMenuType> getConfiguredMenuTypes() {
    return CollectionUtility.<IMenuType> hashSet(TableMenuType.Header);
  }

  @Override
  protected void execInitAction() throws ProcessingException {
    // This menu is only visible in development mode and not in the web client
    setVisible(Platform.isDevelopmentMode() && !UserAgentUtility.isWebClient());
  }

  /**
   * This menu exports the fully qualified column class names and their widths to the clipboard
   * using the following format for each column:
   * [fully qualified column class name]\t[column width]\n
   */
  @Override
  protected void execAction() {
    try {
      StringBuffer buf = new StringBuffer();

      // Add an identifier for fast identification
      buf.append(COLUMN_COPY_CLIPBOARD_IDENTIFIER);
      buf.append("\n");

      // only visible columns are of interest
      for (IColumn<?> column : getTable().getColumnSet().getVisibleColumns()) {
        buf.append(column.getClass().getName());
        buf.append("\t");
        buf.append(column.getWidth());
        buf.append("\n");
      }

      // calling the service to write the buffer to the clipboard
      IClipboardService svc = SERVICES.getService(IClipboardService.class);
      svc.setTextContents(buf.toString());
    }
    catch (ProcessingException se) {
      se.addContextMessage(getText());
      SERVICES.getService(IExceptionHandlerService.class).handleException(se);
    }
  }

  public ITable getTable() {
    return m_table;
  }
}
