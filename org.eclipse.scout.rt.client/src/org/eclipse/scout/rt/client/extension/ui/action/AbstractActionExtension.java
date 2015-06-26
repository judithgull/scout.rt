/*******************************************************************************
 * Copyright (c) 2014 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.extension.ui.action;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.extension.ui.action.ActionChains.ActionActionChain;
import org.eclipse.scout.rt.client.extension.ui.action.ActionChains.ActionInitActionChain;
import org.eclipse.scout.rt.client.extension.ui.action.ActionChains.ActionSelectionChangedChain;
import org.eclipse.scout.rt.client.ui.action.AbstractAction;
import org.eclipse.scout.rt.shared.extension.AbstractExtension;

/**
 *
 */
public abstract class AbstractActionExtension<OWNER extends AbstractAction> extends AbstractExtension<OWNER> implements IActionExtension<OWNER> {

  public AbstractActionExtension(OWNER owner) {
    super(owner);
  }

  @Override
  public void execSelectionChanged(ActionSelectionChangedChain chain, boolean selection) throws ProcessingException {
    chain.execSelectionChanged(selection);
  }

  @Override
  public void execAction(ActionActionChain chain) throws ProcessingException {
    chain.execAction();
  }

  @Override
  public void execInitAction(ActionInitActionChain chain) throws ProcessingException {
    chain.execInitAction();
  }

}
