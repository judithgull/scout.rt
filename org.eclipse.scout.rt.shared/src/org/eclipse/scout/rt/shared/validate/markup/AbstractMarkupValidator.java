/*******************************************************************************
 * Copyright (c) 2010, 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.validate.markup;

import org.eclipse.scout.commons.StringUtility;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @since 3.10.0-M4
 */
public abstract class AbstractMarkupValidator implements IMarkupValidator {

  private final IMarkupList m_markupList = createMarkupList();

  @Override
  public String validate(String text) {
    if (StringUtility.isNullOrEmpty(text)) {
      return text;
    }

    Document doc = convertToHtmlDoc(text);
    IMarkupVisitor markupVisitor = createMarkupVisitor(doc, m_markupList);
    IMarkupNodeTravesor traversor = createMarkupNodeTravesor(markupVisitor);
    traversor.traverse(doc);

    return doc.toString();
  }

  protected Document convertToHtmlDoc(String text) {
    return Jsoup.parse(text);
  }

  protected abstract IMarkupList createMarkupList();

  protected abstract IMarkupVisitor createMarkupVisitor(Element root, IMarkupList markupList);

  protected abstract IMarkupNodeTravesor createMarkupNodeTravesor(IMarkupVisitor markupVisitor);

  @Override
  public IMarkupList getMarkupList() {
    return m_markupList;
  }

  /**
   * Escapes HTML special characters with hex entities according to OWASP.
   * 
   * @param htmlText
   *          HTML text to be escaped
   * @return Escaped HTML text where &, <, >, ", ', / are replaced by hex entities.
   */
  public static String escapeHtml(String htmlText) {
    if (htmlText == null) {
      return null;
    }
    htmlText = htmlText.replaceAll("[&]", "&amp;");
    htmlText = htmlText.replaceAll("[<]", "&lt;");
    htmlText = htmlText.replaceAll("[>]", "&gt;");
    htmlText = htmlText.replaceAll("[\"]", "&quot;");
    htmlText = htmlText.replaceAll("[']", "&#x27;");
    htmlText = htmlText.replaceAll("[/]", "&#x2F;");

    return htmlText;
  }

}
