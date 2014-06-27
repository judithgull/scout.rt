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
package org.eclipse.scout.rt.client.ui.form.fields.button;

import org.eclipse.scout.commons.annotations.ClassId;

@ClassId("bd045e80-5577-459c-a212-3d5c655e304a")
public abstract class AbstractLinkButton extends AbstractButton implements IButton {

  public AbstractLinkButton() {
    this(true);
  }

  public AbstractLinkButton(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected int getConfiguredDisplayStyle() {
    return DISPLAY_STYLE_LINK;
  }
}
