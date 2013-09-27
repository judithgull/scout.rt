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

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.xmlparser.SimpleXmlElement;

/**
 * @since 3.10.0-M2
 */
public class DefaultMarkupValidator extends AbstractMarkupValidatorOld {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(DefaultMarkupValidator.class);

  public DefaultMarkupValidator() {
  }

  @Override
  protected SimpleXmlElement convertToXml(String text) {
    SimpleXmlElement xml = new SimpleXmlElement();
    xml.parseString(text);
    return xml;
  }

  @Override
  protected void validateElements(SimpleXmlElement xml) {
    SimpleXmlElement rootElement = xml.getRoot();
    System.out.println("root element is " + rootElement.getName());
    checkAllowedElements(rootElement);
  }

  private void checkAllowedElements(SimpleXmlElement element) {
    System.out.println("checkAllowedElements for element " + element.getName());

    for (SimpleXmlElement childElement : element.getChildren()) {
      if (ALLOWED_ELEMENTS_ATTRIBUTES.containsKey(childElement.getName())) {
        checkAllowedAttributes(childElement);
      }
      else {
        System.out.println("checkAllowedElements removes child element " + childElement.getName());
        element.removeChild(childElement);
      }
      checkAllowedElements(childElement);
    }
  }

  private void checkAllowedAttributes(SimpleXmlElement element) {
    Set<String> allowedAttributes = ALLOWED_ELEMENTS_ATTRIBUTES.get(element.getName());

    Iterator<Entry<String, String>> attributeIterator = element.getAttributes().entrySet().iterator();
    while (attributeIterator.hasNext()) {
      Entry<String, String> attributeEntry = attributeIterator.next();
      System.out.println("checkAllowedAttributes attribute " + attributeEntry.getKey());

      String attributeName = attributeEntry.getKey();
      String attributeValue = attributeEntry.getValue();

      if (!allowedAttributes.contains(attributeName)) {
        System.out.println("checkAllowedAttributes removes invalid attributeName name " + attributeName);
        element.removeAttribute(attributeName);
        continue;
      }

      if (!isAllowedAttributeValue(attributeValue, attributeName)) {
        System.out.println("checkAllowedAttributes removes invalid attributeValue value " + attributeValue);
        element.removeAttribute(attributeName);
      }
    }
  }

  private boolean isAllowedAttributeValue(String attributeValue, String attributeName) {
    List<Pattern> patterns = FORBIDDEN_ATTRIBUTE_VALUES.get(ALL_ATTRIBUTES);
    for (Pattern p : patterns) {
      if (p.matcher(attributeValue).find()) {
        return false;
      }
    }

    patterns = FORBIDDEN_ATTRIBUTE_VALUES.get(attributeName);
    if (patterns != null) {
      for (Pattern p : patterns) {
        if (p.matcher(attributeValue).find()) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  protected void validateAttributes(SimpleXmlElement xml) {
  }

  @Override
  protected void validateContents(SimpleXmlElement xml) {
  }

  @Override
  protected void validateUrlEndings(SimpleXmlElement xml) {
  }
}
