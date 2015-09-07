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
package org.eclipse.scout.commons.html.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.html.IHtmlElement;

/**
 * Builder for a html node with start tag, end tag and attributes.
 * 
 * @since 5.1 (backported)
 */
public class HtmlNodeBuilder extends HtmlContentBuilder implements IHtmlElement {

  private final List<String> m_attributes = new ArrayList<String>();
  private String m_tag;

  protected String getTag() {
    return m_tag;
  }

  public HtmlNodeBuilder(String tag, CharSequence... texts) {
    this(tag, Arrays.asList(texts));
  }

  public HtmlNodeBuilder(String tag) {
    this(tag, new ArrayList<String>());
  }

  public HtmlNodeBuilder(String tag, List<? extends CharSequence> texts) {
    super(texts);
    m_tag = tag;
  }

  @Override
  public void build() {
    appendStartTag();
    if (getTexts().size() > 0) {
      appendText();
    }
    appendEndTag();
  }

  protected void appendStartTag() {
    append("<");
    append(getTag());
    appendAttributes();
    append(">");
  }

  protected void appendEndTag() {
    append("</");
    append(getTag());
    append(">");
  }

  private void appendAttributes() {
    if (m_attributes.size() > 0) {
      append(" ");
      append(CollectionUtility.format(m_attributes, " "));
    }
  }

  protected void addAttribute(String name, int value) {
    addAttribute(name, Integer.toString(value));
  }

  @Override
  public IHtmlElement addAttribute(String name, CharSequence value) {
    m_attributes.add(name + "=\"" + value + "\"");
    return this;
  }

  @Override
  public void replaceBinds(Map<String/*old Bind*/, String/*new Bind*/> bindMap) {
    super.replaceBinds(bindMap);
    getBinds().replaceBinds(bindMap);
  }

/// GLOBAL ATTRIBUTES
  @Override
  public IHtmlElement style(CharSequence value) {
    addAttribute("style", value);
    return this;
  }

  @Override
  public IHtmlElement cssClass(CharSequence cssClass) {
    addAttribute("class", cssClass);
    return this;
  }

  @Override
  public IHtmlElement appLink(CharSequence ref) {
    cssClass("app-link");
    addAttribute("data-ref", ref);
    return this;
  }

}
