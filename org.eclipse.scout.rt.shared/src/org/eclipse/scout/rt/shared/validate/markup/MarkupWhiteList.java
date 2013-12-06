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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.StringUtility;

/**
 * Inspired by org.jsoup.safety.Whitelist by Jonathan Hedley
 * 
 * @since 3.10.0-M4
 */
public class MarkupWhiteList implements IMarkupList {
  public final static String[] DEFAULT_TAGS = new String[]{
      "html", "head", "body", "style", "type",
      "a", "b", "blockquote", "br", "caption", "cite", "code", "col",
      "colgroup", "dd", "div", "dl", "dt", "em", "font", "h1", "h2", "h3", "h4", "h5", "h6", "hr",
      "i", "img", "li", "meta", "ol", "p", "pre", "q", "small", "strike", "strong",
      "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u",
      "ul", "title", "style", "html"
  };

  private Set<String> m_tags;
  private Map<String, Set<String>> m_attributes;

  public static MarkupWhiteList emptyList() {
    return new MarkupWhiteList();
  }

  public static MarkupWhiteList defaultList() {
    return new MarkupWhiteList()
        .addTags(DEFAULT_TAGS)

        .addAttributes("a", "href", "title")
        .addAttributes("body", "style")
        .addAttributes("blockquote", "cite")
        .addAttributes("col", "span", "width")
        .addAttributes("colgroup", "span", "width")
        .addAttributes("div", "id")
        .addAttributes("font", "color")
        .addAttributes("img", "align", "alt", "height", "src", "title", "width")
        .addAttributes("meta", "http-equiv", "content")
        .addAttributes("ol", "start", "type")
        .addAttributes("q", "cite")
        .addAttributes("style", "type")
        .addAttributes("table", "summary", "width")
        .addAttributes("td", "abbr", "axis", "colspan", "rowspan", "width")
        .addAttributes("th", "abbr", "axis", "colspan", "rowspan", "scope", "width")
        .addAttributes("ul", "type");
  }

  protected MarkupWhiteList() {
    m_tags = new HashSet<String>();
    m_attributes = new HashMap<String, Set<String>>();
  }

  @Override
  public MarkupWhiteList addTags(String... tagNames) {
    if (tagNames != null) {
      for (String tagName : tagNames) {
        if (StringUtility.hasText(tagName)) {
          m_tags.add(tagName.toLowerCase());
        }
      }
    }
    return this;
  }

  @Override
  public MarkupWhiteList addAttributes(String tagName, String... attributeNames) {
    if (StringUtility.hasText(tagName) && attributeNames != null && attributeNames.length > 0) {
      if (!m_tags.contains(tagName.toLowerCase())) {
        addTags(tagName);
      }

      Set<String> attrs = m_attributes.get(tagName.toLowerCase());
      if (attrs == null) {
        attrs = new HashSet<String>();
        m_attributes.put(tagName.toLowerCase(), attrs);
      }
      for (String attrName : attributeNames) {
        if (StringUtility.hasText(attrName)) {
          attrs.add(attrName.toLowerCase());
        }
      }
    }
    return this;
  }

  @Override
  public boolean isAllowedTag(String tagName) {
    if (StringUtility.isNullOrEmpty(tagName)) {
      return false;
    }

    if (allTagsAllowed()) {
      return true;
    }

    return m_tags.contains(tagName.toLowerCase());
  }

  private boolean allTagsAllowed() {
    return m_tags.contains(ALL_TAGS);
  }

  @Override
  public boolean isAllowedAttribute(String tagName, String attributeName) {
    if (StringUtility.isNullOrEmpty(tagName) || StringUtility.isNullOrEmpty(attributeName)) {
      return false;
    }

    Set<String> attrs = m_attributes.get(tagName.toLowerCase());
    if (attrs != null) {
      if (allAttributesAllowed(attrs)) {
        return true;
      }
      return attrs.contains(attributeName.toLowerCase());
    }

    // check if attribute is defined in ALL_TAG
    attrs = m_attributes.get(ALL_TAGS);
    if (attrs != null) {
      if (allAttributesAllowed(attrs)) {
        return true;
      }
      return attrs.contains(attributeName.toLowerCase());
    }

    return false;
  }

  private boolean allAttributesAllowed(Set<String> attrs) {
    return attrs.contains(ALL_ATTRIBUTES);
  }

  private boolean allAttributesAllowed(String tagName) {
    Set<String> attrs = m_attributes.get(tagName.toLowerCase());
    if (attrs != null) {
      if (allAttributesAllowed(attrs)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean areAllTagsAndAttributesAllowed() {
    return allTagsAllowed() && allAttributesAllowed(ALL_TAGS);
  }
}
