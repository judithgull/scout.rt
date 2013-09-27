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

import org.junit.Test;

/**
 * @since 3.10.0-M2
 */
public class DefaultMarkupValidatorTest {

  static final String INPUT = "" +
      "<html>" +
      //      "<body onload=\"alert(\\\"XSS\\\")\">" +
      "<body onload=\"alert('XSS')\">" +
      "<table align=\"javascript:alert('XSS')\" border=\"1\">" +
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
      "<img src=\"javascript:alert('XSS')\"/>" +
      "<img src=\"http://www.google.com/images\"/>" +
      "</body>" +
      "</html>";

  @Test
  public void testValidate() {
    DefaultMarkupValidator validator = new DefaultMarkupValidator();
    String validatedInput = validator.validate(INPUT);

    System.out.println("validatedInput " + validatedInput.toString());
  }

}
