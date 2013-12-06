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

import org.jsoup.nodes.Element;

/**
 * @since 3.10.0-M4
 */
public class WhitelistMarkupValidator extends AbstractMarkupValidator {
  @Override
  protected IMarkupList createMarkupList() {
    // TODO: customizable lists?
    return MarkupWhiteList.defaultList();
  }

  @Override
  protected IMarkupVisitor createMarkupVisitor(Element root, IMarkupList markupList) {
    return new MarkupVisitor(root, markupList);
  }

  @Override
  protected IMarkupNodeTravesor createMarkupNodeTravesor(IMarkupVisitor markupVisitor) {
    return new MarkupNodeTravesor(markupVisitor);
  }
}
