/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.desktop.outline.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.testenvironment.TestEnvironmentClientSession;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractStringColumn;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.outline.AbstractOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;
import org.eclipse.scout.rt.testing.commons.ScoutAssert;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that {@link AbstractPage#execPageDataLoaded()} is called after the data has been loaded.
 */
@RunWith(ScoutClientTestRunner.class)
public class PageWithTable7Test {

  @Test
  public void testExecPageDataLoaded() throws ProcessingException {
    IDesktop desktop = TestEnvironmentClientSession.get().getDesktop();
    desktop.setAvailableOutlines(Collections.singletonList(new PageWithTableOutline()));
    desktop.setOutline(PageWithTableOutline.class);
    IOutline outline = desktop.getOutline();
    PageWithTable page = (PageWithTable) outline.getActivePage();

    page.m_counter = 1;
    page.reloadPage();

    page.m_counter = 2;
    page.reloadPage();

    page.m_counter = 3;
    page.reloadPage();

    ScoutAssert.assertListEquals(new String[]{"counter: 0 value: first", "counter: 1 value: second", "counter: 2 value: third", "counter: 3 value: fourth"}, page.m_protocol);
  }

  public static class PageWithTableOutline extends AbstractOutline {

    @Override
    protected void execCreateChildPages(Collection<IPage> pageList) throws ProcessingException {
      pageList.add(new PageWithTable());
    }
  }

  public static class PageWithTable extends AbstractPageWithTable<PageWithTable.Table> {
    public List<String> m_protocol = new ArrayList<String>();
    public int m_counter = 0;
    private String[] data = new String[]{"first", "second", "third", "fourth"};

    @Override
    protected Object[][] execLoadTableData(SearchFilter filter) throws ProcessingException {
      return new Object[][]{new Object[]{data[m_counter]}};
    }

    @Override
    protected void execPageDataLoaded() throws ProcessingException {
      super.execPageDataLoaded();
      m_protocol.add("counter: " + m_counter + " value: " + getTable().getRow(0).getCell(0).getValue().toString());
    }

    @Override
    protected IPage execCreateChildPage(ITableRow row) throws ProcessingException {
      return new PageWithNode();
    }

    public class Table extends AbstractTable {
      @Order(10)
      public class FirstColumn extends AbstractStringColumn {
      }
    }
  }

  public static class PageWithNode extends AbstractPageWithNodes {

  }
}
