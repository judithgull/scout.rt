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

import org.eclipse.scout.commons.html.IHtmlElement;

/**
 * Builder for a html image.
 * 
 * @since 5.1 (backported)
 */
public class HtmlImageBuilder extends EmptyHtmlNodeBuilder implements IHtmlElement {

  public HtmlImageBuilder(CharSequence path) {
    super("img");
    addAttribute("src", path);
  }

}
