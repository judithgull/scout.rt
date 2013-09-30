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
 *
 */
public class BlacklistMarkupValidatorTest {

  public static final String INPUT = "" +
      "<html>" +
      "  <head>" +
      "    <title>" +
      "        Scout" +
      "    </title>" +
      "    <style type=\"text/css\">" +
      "    <!--" +
      "    #scout_center {" +
      "        width: 460px;" +
      "        margin: 0 auto 0 auto;" +
      "    }" +
      "    #scout_banner {" +
      "     background: #FE9915 url(\"http://eclipse.org/scout/img/s.png\") no-repeat;" +
      "     width: 460px;" +
      "     height: 154px;" +
      "     padding-left: 200px;  " +
      "     padding-top: 10px;" +
      "     float: left;" +
      "    }" +
      "    #scout_banner h1 {" +
      "     color: #FFFFFF;" +
      "     font: bold 22pt \"Lucida Grande\", Lucida, Verdana, sans-serif;" +
      "     padding: 0px;" +
      "    }" +
      "    #scout_banner p {" +
      "     color: #FFFFFF;" +
      "     font: 16pt \"Lucida Grande\", Lucida, Verdana, sans-serif;" +
      "     margin-top: 15px;" +
      "    }" +
      "    -->" +
      "    </style>" +
      "  </head>" +
      "  <body>" +
      "    <div id=\"scout_center\">" +
      "        <div id=\"scout_banner\">" +
      "            <h1>Eclipse Scout</h1>" +
      "            <p>Business application framework</p>" +
      "            <p><a href=\"http://eclipse.org/scout/downloads/\"><img title=\"Download Eclipse Scout\" src=\"http://eclipse.org/scout/img/download.png\"/></a></p>" +
      "        </div>" +
      "        <br />&nbsp;<br />&nbsp;<br />" +
      "        <p>This is an example for the HtmlField of Eclipse Scout. <a href=\"http://wiki.eclipse.org/Scout/Concepts/HtmlField\"> Learn more &gt;&gt;</a></p>" +
      "    </div>" +
      "  </body>" +
      "</html>";

  public static final String INPUT2 = "" +
      "<html>" +
      "  <body>" +
      "        <p>This is an example for the HtmlField of Eclipse Scout. <a href=\"http://wiki.eclipse.org/Scout/Concepts/HtmlField\"> Learn more</a></p>" +
      "  </body>" +
      "</html>";

  @Test
  public void testValidate() {
    BlacklistMarkupValidator validator = new BlacklistMarkupValidator();
    String validatedInput = validator.validate(INPUT);

//    String validatedInput = Jsoup.clean(INPUT, Whitelist.relaxed());

    System.out.println("validatedInput:\n " + validatedInput.toString());
  }

}
