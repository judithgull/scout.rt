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

/**
 * @since 3.10.0-M4
 */
public interface IMarkupList {

  static final String ALL_TAGS = "*";
  static final String ALL_ATTRIBUTES = "**";

  IMarkupList addTags(String... tagNames);

  IMarkupList addAttributes(String tagName, String... attributeNames);

  boolean isAllowedTag(String tagName);

  boolean isAllowedAttribute(String tagName, String attributeName);

  boolean areAllTagsAndAttributesAllowed();

}
