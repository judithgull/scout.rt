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
package org.eclipse.scout.commons.validation;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.scout.commons.xmlparser.SimpleXmlElement;

/**
 * @since 3.10.0-M2
 */
public abstract class AbstractMarkupValidatorOld implements IMarkupValidator {
  protected static final String ALL_ATTRIBUTES = "ALL_ATTRIBUTES";

  protected static Map<String, Set<String>> ALLOWED_ELEMENTS_ATTRIBUTES = createElementsAttributesWhitelist();
  protected static Map<String, List<Pattern>> FORBIDDEN_ATTRIBUTE_VALUES = createForbiddenAttributeValues();

  @Override
  public String validate(String text) {
    SimpleXmlElement xml = convertToXml(text);

    SimpleXmlElement root = xml.getRoot();
    if (!isElementAllowed(root)) {
      return "";
    }

    validateElements(xml);
    validateAttributes(xml);
    validateContents(xml);
    validateUrlEndings(xml);

    StringWriter writer = new StringWriter();
    try {
      xml.writeContent(writer);
    }
    catch (IOException e) {
      // nop // TODO:
    }
    return writer.toString();
  }

  protected static Set<String> createAttributesWhitelist(String... attributes) {
    Set<String> attributeWhitelist = new HashSet<String>();
    for (String attribute : attributes) {
      attributeWhitelist.add(attribute);
    }
    return attributeWhitelist;
  }

  protected static Map<String, List<Pattern>> createForbiddenAttributeValues() {
    Map<String, List<Pattern>> blacklist = new HashMap<String, List<Pattern>>();
    blacklist.put(ALL_ATTRIBUTES, createPatternBlacklistForAllAttributes());
    //blacklist.put("src", createPatternBlacklistForSrcAttributes());

    return blacklist;
  }

  protected static List<Pattern> createPatternBlacklistForAllAttributes() {
    List<Pattern> blacklist = new ArrayList<Pattern>();

    blacklist.add(Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE));
    blacklist.add(Pattern.compile("\\.js$", Pattern.CASE_INSENSITIVE));
    blacklist.add(Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE));
    blacklist.add(Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE));

    return blacklist;
  }

  protected static List<Pattern> createPatternBlacklistForSrcAttributes() {
    List<Pattern> blacklist = new ArrayList<Pattern>();

    blacklist.add(Pattern.compile("[\\s]*(.*?)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE));
    blacklist.add(Pattern.compile("[\\s]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE));

    return blacklist;
  }

  protected static Map<String, Set<String>> createElementsAttributesWhitelist() {
    Map<String, Set<String>> elemAttribWhitelist = new HashMap<String, Set<String>>();
    elemAttribWhitelist.put("html", Collections.<String> emptySet());
    elemAttribWhitelist.put("body", Collections.<String> emptySet());
    elemAttribWhitelist.put("table", createAttributesWhitelist("border", "align"));
    elemAttribWhitelist.put("tr", Collections.<String> emptySet());
//    elemAttribWhitelist.put("th", Collections.<String> emptySet());
    elemAttribWhitelist.put("td", Collections.<String> emptySet());
    elemAttribWhitelist.put("img", createAttributesWhitelist("src"));

    return elemAttribWhitelist;
  }

  protected boolean isElementAllowed(SimpleXmlElement element) {
    return ALLOWED_ELEMENTS_ATTRIBUTES.containsKey(element.getName());
  }

  protected SimpleXmlElement convertToXml(String text) {
    SimpleXmlElement xml = new SimpleXmlElement();
    xml.parseString(text);
    return xml;
  }

  abstract protected void validateElements(SimpleXmlElement xml);

  abstract protected void validateAttributes(SimpleXmlElement xml);

  abstract protected void validateContents(SimpleXmlElement xml);

  abstract protected void validateUrlEndings(SimpleXmlElement xml);

}
