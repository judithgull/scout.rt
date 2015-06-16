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
package org.eclipse.scout.rt.client.services.common.notification;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.IClientSession.State;
import org.eclipse.scout.rt.client.IClientSessionStateListener;

/**
 *
 */
public class NotificationClientService implements INotificationClientService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(NotificationClientService.class);

  private final IClientSessionStateListener m_clientSessionStateListener = new P_ClientSessionStateListener();

  @Override
  public void register(IClientSession clientSession) {
    clientSession.addClientSessionStateListener(m_clientSessionStateListener);

  }

  /**
   * this method is expected to be called in the context of the specific session.
   *
   * @param session
   */
  protected void clientSessionStopping(IClientSession session) {
    session.removeClientSessionStateListener(m_clientSessionStateListener);
    // remove remote
    LOG.debug(String.format("client session [%s] stopping", session.getUserId()));
  }

  /**
   * this method is expected to be called in the context of the specific session.
   *
   * @param session
   */
  public void clientSessionStarted(IClientSession session) {
    // register remote
    LOG.debug(String.format("client session [%s] started", session.getUserId()));
  }

  private class P_ClientSessionStateListener implements IClientSessionStateListener {

    @Override
    public void stateChanged(IClientSession owner, State oldState, State newState) {
      switch (newState) {
        case Stopping:
          clientSessionStopping(owner);
          break;
        case Started:
          clientSessionStarted(owner);
          break;
      }
    }
  }

}
