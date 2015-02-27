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
package org.eclipse.scout.testing.client;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.ui.desktop.AbstractDesktop;
import org.eclipse.scout.rt.client.ui.desktop.DesktopEvent;
import org.eclipse.scout.rt.client.ui.desktop.DesktopListener;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.messagebox.IMessageBox;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.eclipse.scout.testing.client.servicetunnel.http.MultiClientAuthenticator;
import org.osgi.framework.Bundle;

/**
 * Default implementation of {@link ITestClientSessionProvider}.
 */
public class DefaultTestClientSessionProvider implements ITestClientSessionProvider {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(DefaultTestClientSessionProvider.class);

  private static final Map<String, IClientSession> CACHE = new HashMap<String, IClientSession>();
  private static final Object CACHE_LOCK = new Object();

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IClientSession> T getOrCreateClientSession(Class<T> clazz, final String runAs, boolean createNew) {
    String symbolicName = clazz.getPackage().getName();
    Bundle bundleLocator = null;
    while (symbolicName != null) {
      bundleLocator = Platform.getBundle(symbolicName);
      int i = symbolicName.lastIndexOf('.');
      if (bundleLocator != null || i < 0) {
        break;
      }
      symbolicName = symbolicName.substring(0, i);
    }
    synchronized (CACHE_LOCK) {
      String cacheKey = createSessionCacheKey(clazz, runAs);
      IClientSession clientSession = CACHE.get(cacheKey);
      if (clientSession == null || !clientSession.isActive() || createNew) {
        try {
          clientSession = clazz.newInstance();
          CACHE.put(cacheKey, clientSession);
          clientSession.setUserAgent(UserAgent.createDefault());
          ClientSyncJob job = new ClientSyncJob("Session startup", clientSession) {
            @Override
            protected void runVoid(IProgressMonitor monitor) throws Throwable {
              beforeStartSession(getClientSession(), runAs);
              getCurrentSession().startSession();
              simulateDesktopOpened(getClientSession());
              afterStartSession(getClientSession(), runAs);
            }
          };
          job.schedule();
          job.join();
          job.throwOnError();
        }
        catch (Throwable t) {
          LOG.error("could not load session for " + symbolicName, t);
        }
      }
      return (T) clientSession;
    }
  }

  /**
   * Creates a cache key for the given session class, its hosting bundle and the name of the user the session is created
   * for.
   *
   * @param sessionClass
   * @param providingBundleSymbolicName
   * @param runAs
   * @return
   */
  protected String createSessionCacheKey(Class<? extends IClientSession> sessionClass, String runAs) {
    return StringUtility.join("-", sessionClass.getName(), runAs);
  }

  /**
   * Performs custom operations before the client session is started. This default implementation assigns the current
   * session on the {@link MultiClientAuthenticator}, so that a possibly arising HTTP BASIC authentication can be
   * performed. Additionally, all message boxes are automatically canceled.
   *
   * @param clientSession
   * @param runAs
   * @see MultiClientAuthenticator
   */
  protected void beforeStartSession(IClientSession clientSession, String runAs) {
    MultiClientAuthenticator.assignSessionToUser(clientSession, runAs);
    TestingUtility.clearHttpAuthenticationCache();
    // auto-cancel all message boxes
    clientSession.getVirtualDesktop().addDesktopListener(new DesktopListener() {
      @Override
      public void desktopChanged(DesktopEvent e) {
        switch (e.getType()) {
          case DesktopEvent.TYPE_MESSAGE_BOX_ADDED:
            e.getMessageBox().getUIFacade().setResultFromUI(IMessageBox.CANCEL_OPTION);
            break;
        }
      }
    });
  }

  /**
   * Performs custom operations after the client session has been started.
   *
   * @param clientSession
   * @param runAs
   */
  protected void afterStartSession(IClientSession clientSession, String runAs) {
  }

  /**
   * Simulates that the desktop has been opened. The method works also if the desktop has already been opened or if the
   * Scout client does not have a desktop at all.
   *
   * @param clientSession
   */
  protected void simulateDesktopOpened(IClientSession clientSession) {
    IDesktop desktop = clientSession.getDesktop();
    if (desktop instanceof AbstractDesktop && !desktop.isOpened()) {
      desktop.getUIFacade().fireGuiAttached();
      desktop.getUIFacade().fireDesktopOpenedFromUI();
    }
  }
}
