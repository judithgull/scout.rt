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
package org.eclipse.scout.commons.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

/**
 * @since 3.10.0-M2
 */
public class WhitelistMarkupValidator implements IMarkupValidator {

  protected static final String[] ALLOWED_TAGS = new String[]{"title", "style"};

  protected static final Map<String, String[]> ALLOWED_ATTRIBUTES = new HashMap<String, String[]>() {
    private static final long serialVersionUID = 1L;
    {
      put("style", new String[]{"type"});
      put("div", new String[]{"id"});
    }
  };

  protected static final Whitelist WHITELIST;

  protected Cleaner m_cleaner;

  static {
    WHITELIST = Whitelist.relaxed();
    WHITELIST.addTags(ALLOWED_TAGS);
    Iterator<Entry<String, String[]>> iterator = ALLOWED_ATTRIBUTES.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, String[]> entry = iterator.next();
      WHITELIST.addAttributes(entry.getKey(), entry.getValue());
    }
  }

  public WhitelistMarkupValidator() {
    m_cleaner = new Cleaner(WHITELIST);
  }

  @Override
  public String validate(String text) {
    Document htmlDoc = Jsoup.parse(text);

    for (Element e : htmlDoc.getElementsByTag("p")) {
      System.out.println("element tag name: " + e.tagName() + ", has children: " + e.children().size() + ", val " + e.ownText());

    }

//    System.out.println("html element: " + htmlDoc.children());
    Document validatedHtmlDoc = m_cleaner.clean(htmlDoc);

    return validatedHtmlDoc.html();
  }
}
