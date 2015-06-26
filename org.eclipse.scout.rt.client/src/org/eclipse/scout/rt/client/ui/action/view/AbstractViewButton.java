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
package org.eclipse.scout.rt.client.ui.action.view;

import org.eclipse.scout.rt.client.extension.ui.action.view.IViewButtonExtension;
import org.eclipse.scout.rt.client.ui.action.AbstractAction;

public abstract class AbstractViewButton extends AbstractAction implements IViewButton {

  public AbstractViewButton() {
    super();
  }

  public AbstractViewButton(boolean callInitializer) {
    super(callInitializer);
  }

  protected static class LocalViewButtonExtension<OWNER extends AbstractViewButton> extends LocalActionExtension<OWNER> implements IViewButtonExtension<OWNER> {

    public LocalViewButtonExtension(OWNER owner) {
      super(owner);
    }
  }

  @Override
  protected IViewButtonExtension<? extends AbstractViewButton> createLocalExtension() {
    return new LocalViewButtonExtension<AbstractViewButton>(this);
  }
}
