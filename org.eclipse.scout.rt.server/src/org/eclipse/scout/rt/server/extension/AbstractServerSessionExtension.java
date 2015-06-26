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
package org.eclipse.scout.rt.server.extension;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.server.AbstractServerSession;
import org.eclipse.scout.rt.server.extension.ServerSessionChains.ServerSessionLoadSessionChain;
import org.eclipse.scout.rt.shared.extension.AbstractSerializableExtension;

/**
 *
 */
public abstract class AbstractServerSessionExtension<OWNER extends AbstractServerSession> extends AbstractSerializableExtension<OWNER> implements IServerSessionExtension<OWNER> {
  private static final long serialVersionUID = 1L;

  public AbstractServerSessionExtension(OWNER owner) {
    super(owner);
  }

  @Override
  public void execLoadSession(ServerSessionLoadSessionChain chain) throws ProcessingException {
    chain.execLoadSession();
  }

}
