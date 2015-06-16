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
package org.eclipse.scout.rt.client;

import java.util.EventListener;

import org.eclipse.scout.rt.client.IClientSession.State;

/**
 *
 */
public interface IClientSessionStateListener extends EventListener {

  void stateChanged(IClientSession owner, State oldState, State newState);

}
