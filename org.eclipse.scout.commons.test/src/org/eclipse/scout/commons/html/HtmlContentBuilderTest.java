/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons.html;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.html.internal.HtmlContentBuilder;
import org.junit.Test;

/**
 * Tests for {@link HtmlContentBuilder}
 * 
 * @since 5.1 (backported)
 */
public class HtmlContentBuilderTest {

  @Test
  public void test2Binds() {
    IHtmlElement h1 = HTML.h2("0");
    IHtmlElement h2 = HTML.h2("1");
    HtmlContentBuilder cb = new HtmlContentBuilder(h1, h2);
    assertEquals(2, cb.getBinds().getBindMap().size());
    assertEquals("0", cb.getBinds().getBindMap().get(":b__0"));
    assertEquals("1", cb.getBinds().getBindMap().get(":b__1"));
  }

  @Test
  public void testImportBinds() {
    IHtmlElement h2 = HTML.h2("0");
    System.out.println(h2.getBinds());
    IHtmlElement h22 = HTML.h2("1", HTML.bold("2"));
    System.out.println(h22.getBinds());

    HtmlContentBuilder cb = new HtmlContentBuilder(h2, h22);
    assertEquals("0", cb.getBinds().getBindMap().get(":b__0"));
    assertEquals("1", cb.getBinds().getBindMap().get(":b__1"));
    assertEquals("2", cb.getBinds().getBindMap().get(":b__2"));
  }

  @Test
  public void testManyBinds() throws Exception {
    IHtmlElement h2 = HTML.h2("h2");
    System.out.println(h2.getBinds());
    System.out.println(h2.toString());
    IHtmlTable table = createTable("0");
    System.out.println(table.getBinds());
    System.out.println(table.toString());

    IHtmlElement html = HTML.div(
        h2,
        table
        );

    String exp = "<div><h2>h2</h2>" + createTableString("0") + "</div>";
    assertEquals(exp, html.toEncodedHtml());
  }

  private String createTableString(String prefix) {
    List<String> rows = new ArrayList<String>();
    for (int i = 0; i < 1; i++) {
      rows.add(createRowString(prefix, i));
    }
    return "<table>" + CollectionUtility.format(rows, "") + "</table>";

  }

  private String createRowString(String prefix, int i) {
    return HTML.row(HTML.cell("A" + prefix + i), HTML.cell("B" + prefix + i)).toEncodedHtml();
  }

  private IHtmlTable createTable(String prefix) {
    List<IHtmlTableRow> rows = new ArrayList<IHtmlTableRow>();
    for (int i = 0; i < 1; i++) {
      rows.add(createRow(prefix, i));
    }
    return HTML.table(rows);
  }

  private IHtmlTableRow createRow(String prefix, int i) {
    return HTML.row(HTML.cell("A" + prefix + i), HTML.cell("B" + prefix + i));
  }

}
