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
package org.eclipse.scout.rt.client.session;

import java.util.UUID;
import java.util.concurrent.Callable;

import javax.security.auth.Subject;

import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.annotations.Internal;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContext;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.client.job.ModelJobs;
import org.eclipse.scout.rt.client.services.common.notification.INotificationClientService;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.shared.ISession;

/**
 * Provider for client sessions.
 */
@ApplicationScoped
public class ClientSessionProvider {

  /**
   * @see ClientSessionProvider#provide(ClientRunContext, String)
   */
  public <SESSION extends IClientSession> SESSION provide(ClientRunContext runContext) throws ProcessingException {
    return provide(runContext, UUID.randomUUID().toString());
  }

  /**
   * Provides a new {@link IClientSession} for the {@link Subject} of the given {@link ClientRunContext}.
   *
   * @param runContext
   *          <code>RunContext</code> initialized with the Subject used to create and load the session.
   * @param sessionId
   *          the unique session id for the new session.
   * @return {@link IClientSession} created; is never <code>null</code>.
   * @throws ProcessingException
   *           is thrown if the {@link IClientSession} could not be created or initialized.
   */
  public <SESSION extends IClientSession> SESSION provide(final ClientRunContext runContext, final String sessionId) throws ProcessingException {
    return runContext.call(new Callable<SESSION>() {

      @Override
      public SESSION call() throws Exception {
        // 1. Create an empty session instance.
        final SESSION clientSession = ClientSessionProvider.cast(BEANS.get(IClientSession.class));

        // 2. Load the session.
        ModelJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            beforeStartSession(clientSession);
            clientSession.start(sessionId);
          }
        }, ModelJobs.newInput(ClientRunContexts.copyCurrent().session(clientSession, true)).name("initialize ClientSession [user=%s]", runContext.subject()).logOnError(false)).awaitDoneAndGet();

        registerClientSessionForNotifications(clientSession);
        return clientSession;
      }
    });
  }

  protected void registerClientSessionForNotifications(IClientSession session) {
    // register client session for notifications
    BEANS.get(INotificationClientService.class).registerClientSession(session);
  }

  /**
   * Callback method for performing any operations before the given session is started.
   *
   * @param clientSession
   */
  protected void beforeStartSession(final IClientSession clientSession) {
  }

  /**
   * @return The {@link IClientSession} which is associated with the current thread, or <code>null</code> if not found.
   */
  public static final IClientSession currentSession() {
    final ISession session = ISession.CURRENT.get();
    return (IClientSession) (session instanceof IClientSession ? session : null);
  }

  /**
   * @return The {@link IClientSession} which is associated with the current thread, or <code>null</code> if not found
   *         or not of the expected type.
   */
  public static final <SESSION extends IClientSession> SESSION currentSession(final Class<SESSION> type) {
    final IClientSession clientSession = currentSession();
    if (clientSession == null) {
      return null;
    }

    try {
      return ClientSessionProvider.cast(clientSession);
    }
    catch (final ProcessingException e) {
      return null; // NOOP
    }
  }

  @Internal
  @SuppressWarnings("unchecked")
  protected static <SESSION extends IClientSession> SESSION cast(final IClientSession clientSession) throws ProcessingException {
    try {
      return (SESSION) clientSession;
    }
    catch (final ClassCastException e) {
      throw new ProcessingException(String.format("Wrong session class [actual=%s]", clientSession.getClass().getName()), e);
    }
  }
}
