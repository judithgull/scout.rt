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

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * DefaultMarkupValidatorConfigurator is only a dummy implementation that does not
 * add nor remove any elements or attributes.
 * 
 * @since 3.10.0-M2
 */
public class DefaultMarkupValidatorConfigurator implements IMarkupValidatorConfigurator {

  @Override
  public void addElements(Set<String> elements) {
  }

  @Override
  public void removeElements(Set<String> elements) {
  }

  @Override
  public void addAttributeNames(Set<String> attributeNames) {
  }

  @Override
  public void removeAttributeNames(Set<String> attributeNames) {
  }

  @Override
  public void addAttributeValues(List<Pattern> attributeValues) {
  }

  @Override
  public void removeAttributeValues(List<Pattern> attributeValues) {
  }

}
