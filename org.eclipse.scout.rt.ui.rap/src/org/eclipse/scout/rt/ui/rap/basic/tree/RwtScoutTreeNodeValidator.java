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
package org.eclipse.scout.rt.ui.rap.basic.tree;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.shared.validate.markup.IMarkupValidator;
import org.eclipse.scout.rt.ui.rap.basic.RwtScoutCompositeValidator;
import org.eclipse.swt.internal.widgets.MarkupValidator;

/**
 * @since 3.10.0-M3
 */
@SuppressWarnings("restriction")
public class RwtScoutTreeNodeValidator extends RwtScoutCompositeValidator<ITreeNode> {

  public RwtScoutTreeNodeValidator(IRwtScoutTree uiTree, IMarkupValidator markupValidator) {
    super(uiTree, markupValidator);
  }

  @Override
  protected IRwtScoutTree getUiComposite() {
    return (IRwtScoutTree) super.getUiComposite();
  }

  @Override
  protected boolean isMarkupValidationDisabledOnUiComposite() {
    return Boolean.TRUE.equals(getUiComposite().getUiField().getData(MarkupValidator.MARKUP_VALIDATION_DISABLED));
  }

  @Override
  protected boolean isMarkupEnabledOnUiComposite() {
    return Boolean.TRUE.equals(getUiComposite().getUiField().getData(RWT.MARKUP_ENABLED));
  }
}
