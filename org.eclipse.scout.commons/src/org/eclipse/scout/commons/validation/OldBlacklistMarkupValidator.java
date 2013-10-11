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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @since 3.10.0-M2
 */
public class OldBlacklistMarkupValidator extends OldAbstractMarkupValidator {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(OldBlacklistMarkupValidator.class);

  protected static final String ALL_ATTRIBUTES = "ALL_ATTRIBUTES";
  protected static final String[] BLACKLISTED_ELEMENTS = new String[]{"script", "iframe", "object", "embed"};
  protected static final Pattern[] BLACKLISTED_ATTRIBUTE_VALUES = new Pattern[]{
      Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\.js$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE),
      Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE)
  };

  private static final String EXTENSION_POINT_PLUGIN_ID = "org.eclipse.scout.commons";
  private static final String EXTENSION_POINT_ID = "markupValidatorConfigurator";

  protected static OldIMarkupValidatorConfigurator MARKUP_CONFIGURATOR;
  protected static Set<String> FORBIDDEN_ELEMENTS;
  protected static Set<String> FORBIDDEN_ATTRIBUTE_NAMES;
  protected static Map<String, List<Pattern>> FORBIDDEN_ATTRIBUTE_VALUES;

  static {
    MARKUP_CONFIGURATOR = readExtensionPoint();
    FORBIDDEN_ELEMENTS = createBlacklistedElements();
    FORBIDDEN_ATTRIBUTE_NAMES = createBlacklistedAttributeNames();
    FORBIDDEN_ATTRIBUTE_VALUES = createBlacklistedAttributeValues();
  }

  protected static Set<String> createBlacklistedElements() {
    Set<String> blacklistedElements = new HashSet<String>();
    blacklistedElements.addAll(Arrays.asList(BLACKLISTED_ELEMENTS));

    MARKUP_CONFIGURATOR.addElements(blacklistedElements);
    MARKUP_CONFIGURATOR.removeElements(blacklistedElements);

    return blacklistedElements;
  }

  protected static OldIMarkupValidatorConfigurator readExtensionPoint() {
//    IExtensionPoint ext = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT_PLUGIN_ID, EXTENSION_POINT_ID);
//    for (IConfigurationElement element : ext.getConfigurationElements()) {
//      try {
//        IMarkupValidatorConfigurator configurator = (IMarkupValidatorConfigurator) element.createExecutableExtension("class");
//        LOG.info("using contributed markup validator configurator class " + configurator.getClass().getCanonicalName() + " " + configurator.toString());
//        return configurator;
//      }
//      catch (CoreException e) {
//        LOG.error("failed to create instance of IMarkupValidatorConfigurator", e);
//      }
//    }
//    LOG.info("using default markup validator configurator");
    return new OldDefaultMarkupValidatorConfigurator();
  }

  protected static Set<String> createBlacklistedAttributeNames() {
    Set<String> blacklistedAttributeNames = new HashSet<String>();
    // Global Event Attributes cf. http://www.w3schools.com/tags/ref_eventattributes.asp
    blacklistedAttributeNames.addAll(Arrays.asList(WINDOW_EVENT_ATTRIBUTES));
    blacklistedAttributeNames.addAll(Arrays.asList(FORM_EVENT_ATTRIBUTES));
    blacklistedAttributeNames.addAll(Arrays.asList(KEYBOARD_EVENT_ATTRIBUTES));
    blacklistedAttributeNames.addAll(Arrays.asList(MOUSE_EVENT_ATTRIBUTES));
    blacklistedAttributeNames.addAll(Arrays.asList(MEDIA_EVENT_ATTRIBUTES));

    MARKUP_CONFIGURATOR.addAttributeNames(blacklistedAttributeNames);
    MARKUP_CONFIGURATOR.removeAttributeNames(blacklistedAttributeNames);

    return blacklistedAttributeNames;
  }

  protected static Map<String, List<Pattern>> createBlacklistedAttributeValues() {
    Map<String, List<Pattern>> blacklist = new HashMap<String, List<Pattern>>();
    blacklist.put(ALL_ATTRIBUTES, createPatternForAllBlacklistedAttributeValues());

    return blacklist;
  }

  protected static List<Pattern> createPatternForAllBlacklistedAttributeValues() {
    List<Pattern> blacklist = new ArrayList<Pattern>();
    blacklist.addAll(Arrays.asList(BLACKLISTED_ATTRIBUTE_VALUES));

    MARKUP_CONFIGURATOR.addAttributeValues(blacklist);
    MARKUP_CONFIGURATOR.removeAttributeValues(blacklist);

    return blacklist;
  }

  @Override
  public String validate(String text) {
    if (StringUtility.isNullOrEmpty(text)) {
      return text;
    }

    Document doc = convertToHtmlDoc(text);
    Element root = doc.getElementsByTag("html").first();
    if (isForbiddenElement(root)) {
      return "";
    }

    validateChildElements(root);

//    String validatedText = "";
//    try {
//      doc.toString();
//
//
//      StringWriter writer = new StringWriter();
//      xml.writeContent(writer); // writer will be closed in the writeContent method
//      validatedText = writer.toString();
//    }
//    catch (IOException e) {
//      LOG.warn("Could not write content of " + xml.toString());
//    }
    return doc.toString();
  }

  private Document convertToHtmlDoc(String text) {
    return Jsoup.parse(text);
  }

  private boolean isForbiddenElement(Element element) {
    return FORBIDDEN_ELEMENTS.contains(element.tagName().toLowerCase());
  }

  protected void validateChildElements(Element element) {
    for (Element childElement : element.children()) {
      validateElement(childElement, element);
    }
  }

  protected void validateElement(Element element, Element parent) {
    if (FORBIDDEN_ELEMENTS.contains(element.tagName().toLowerCase())) {
      element.remove();
      return;
    }

    validateAttributes(element);
    validateChildElements(element);
  }

  protected void validateAttributes(Element element) {
    Iterator<Attribute> iterator = element.attributes().iterator();
    while (iterator.hasNext()) {
      Attribute attribute = iterator.next();

      String attributeName = attribute.getKey();
      String attributeValue = attribute.getValue();

      if (isForbiddenAttributeName(attributeName)) {
        LOG.info("Removed attribute " + attributeName + " from element " + element.toString());
        element.removeAttr(attributeName);
        continue;
      }

      if (isForbiddenAttributeValue(attributeValue, attributeName)) {
        LOG.info("Removed attribute value " + attributeValue + " of attribute " + attributeName + " from element " + element.toString());
        element.removeAttr(attributeName);
      }
    }
  }

  private boolean isForbiddenAttributeName(String attributeName) {
    return FORBIDDEN_ATTRIBUTE_NAMES.contains(attributeName.toLowerCase());
  }

  private boolean isForbiddenAttributeValue(String attributeValue, String attributeName) {
    List<Pattern> patterns = FORBIDDEN_ATTRIBUTE_VALUES.get(ALL_ATTRIBUTES);
    for (Pattern p : patterns) {
      if (p.matcher(attributeValue).find()) {
        return true;
      }
    }

    patterns = FORBIDDEN_ATTRIBUTE_VALUES.get(attributeName.toLowerCase());
    if (patterns != null) {
      for (Pattern p : patterns) {
        if (p.matcher(attributeValue).find()) {
          return true;
        }
      }
    }
    return false;
  }
}
