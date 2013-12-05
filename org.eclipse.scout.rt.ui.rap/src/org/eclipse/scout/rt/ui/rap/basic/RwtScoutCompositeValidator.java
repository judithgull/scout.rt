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
package org.eclipse.scout.rt.ui.rap.basic;

import org.eclipse.scout.rt.shared.validate.markup.AbstractMarkupValidator;
import org.eclipse.scout.rt.shared.validate.markup.IMarkupComponent;
import org.eclipse.scout.rt.shared.validate.markup.IMarkupValidator;
import org.eclipse.scout.rt.ui.rap.util.HtmlTextUtility;

/**
 * @since 3.10.0-M4
 */
public abstract class RwtScoutCompositeValidator<T extends IMarkupComponent> {
  private final IRwtScoutComposite<?> m_uiComposite;
  private final IMarkupValidator m_markupValidator;

  public RwtScoutCompositeValidator(IRwtScoutComposite<?> uiComposite, IMarkupValidator markupValidator) {
    m_uiComposite = uiComposite;
    m_markupValidator = markupValidator;
  }

  protected IRwtScoutComposite getUiComposite() {
    return m_uiComposite;
  }

  protected IMarkupValidator getMarkupValidator() {
    return m_markupValidator;
  }

  protected abstract boolean isMarkupValidationDisabledOnUiComposite();

  protected abstract boolean isMarkupEnabledOnUiComposite();

  public String validateText(T component, String text) {
    boolean isHtmlMarkupText = HtmlTextUtility.isTextWithHtmlMarkup(text);

    if (!component.hasHtmlMarkup() && isHtmlMarkupText) {
      return AbstractMarkupValidator.escapeHtml(text);
    }

    if (component.hasHtmlMarkup() && isMarkupValidationDisabledOnUiComposite() && isMarkupEnabledOnUiComposite()) {
      component.extendMarkupList(m_markupValidator.getMarkupList());
      return m_markupValidator.validate(text);
    }

    return text;
  }

}
