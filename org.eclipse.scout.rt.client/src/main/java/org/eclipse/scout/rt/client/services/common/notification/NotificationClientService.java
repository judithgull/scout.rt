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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.security.auth.Subject;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.shared.SharedConfigProperties.NotificationSubjectProperty;
import org.eclipse.scout.rt.shared.services.common.notification.INotificationServerService;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;
import org.eclipse.scout.rt.shared.session.ISessionListener;
import org.eclipse.scout.rt.shared.session.SessionEvent;
import org.eclipse.scout.rt.shared.ui.UserAgent;

/**
 *
 */
public class NotificationClientService implements INotificationClientService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(NotificationClientService.class);

  private final Subject NOTIFICATION_SUBJECT = CONFIG.getPropertyValue(NotificationSubjectProperty.class);

  private Object m_cacheLock = new Object();
  private final Map<String /*sessionId*/, WeakReference<IClientSession>> m_sessionIdToSession = new HashMap<>();
  private final Map<String /*userId*/, List<WeakReference<IClientSession>>> m_userToSessions = new HashMap<>();

  private final ISessionListener m_clientSessionStateListener = new P_ClientSessionStateListener();

  private boolean m_offline = false;

  @PostConstruct
  protected void startSmartPollJob() {
    if (BEANS.opt(INotificationServerService.class) != null) {
      m_offline = false;
      try {
        RunContexts.empty().subject(NOTIFICATION_SUBJECT).run(new IRunnable() {
          @Override
          public void run() throws Exception {
            BEANS.get(INotificationServerService.class).registerNotificationNode(NOTIFICATION_NODE_ID);
          }
        });
      }
      catch (ProcessingException e) {
        LOG.warn(String.format("Could not register notification node[%s].", NOTIFICATION_NODE_ID), e);
      }
      P_NotificationPollJob pollJob = new P_NotificationPollJob();
      Jobs.schedule(pollJob, Jobs.newInput(ClientRunContexts.copyCurrent().subject(NOTIFICATION_SUBJECT).session(null)));
    }
    else {
      m_offline = true;
      LOG.debug("Starting without notifications due to no proxy service is available");
    }
  }

  @Override
  public void register(IClientSession clientSession) {
    if (!m_offline) {
      clientSession.addListener(m_clientSessionStateListener);
      // if the client session is already started, otherwise the listener will invoke the clientSessionStated method.
      if (clientSession.isActive()) {
        clientSessionStarted(clientSession);
      }
    }
  }

  /**
   * this method is expected to be called in the context of the specific session.
   *
   * @param session
   */
  protected void clientSessionStopping(IClientSession session) {
    String userId = session.getUserId();
    LOG.debug(String.format("client session [%s] stopping", userId));
    session.removeListener(m_clientSessionStateListener);
    // unregister user remote
    try {
      RunContexts.empty().subject(NOTIFICATION_SUBJECT).run(new IRunnable() {
        @Override
        public void run() throws Exception {
          BEANS.get(INotificationServerService.class).unregisterSession(NOTIFICATION_NODE_ID);
        }
      });
    }
    catch (ProcessingException e) {
      LOG.warn(String.format("Could not unregister session[%s] for remote notifications.", session), e);
    }
    // client session household
    synchronized (m_sessionIdToSession) {
      m_sessionIdToSession.remove(session.getId());
    }
  }

  /**
   * this method is expected to be called in the context of the specific session.
   *
   * @param session
   * @throws ProcessingException
   */
  public void clientSessionStarted(final IClientSession session) {
    LOG.debug(String.format("client session [sessionid=%s, userId=%s] started", session.getId(), session.getUserId()));
    // lookup the userid remote because the user is not necessarily set on the client session.
    final String userId = BEANS.get(INotificationServerService.class).getUserIdOfCurrentSession();
    // local linking
    synchronized (m_cacheLock) {
      m_sessionIdToSession.put(session.getId(), new WeakReference<IClientSession>(session));
      List<WeakReference<IClientSession>> sessionRefs = m_userToSessions.get(userId);
      if (sessionRefs != null) {
        // clean cache
        boolean toBeAdded = true;
        Iterator<WeakReference<IClientSession>> sessionRefIt = sessionRefs.iterator();
        while (sessionRefIt.hasNext()) {
          WeakReference<IClientSession> sessionRef = sessionRefIt.next();
          if (sessionRef.get() == null) {
            sessionRefIt.remove();
          }
          else if (sessionRef.get() == session) {
            // already registered
            toBeAdded = false;
          }
        }
        if (toBeAdded) {
          sessionRefs.add(new WeakReference<>(session));
        }
      }
      else {
        sessionRefs = new LinkedList<>();
        sessionRefs.add(new WeakReference<>(session));
        m_userToSessions.put(userId, sessionRefs);
      }
    }
    // register on backend
    try {
      RunContexts.empty().subject(NOTIFICATION_SUBJECT).run(new IRunnable() {
        @Override
        public void run() throws Exception {
          BEANS.get(INotificationServerService.class).registerSession(NOTIFICATION_NODE_ID, session.getId(), userId);
        }
      });
    }
    catch (ProcessingException e) {
      LOG.warn(String.format("Could not register session[%s] for remote notifications.", session), e);
    }
  }

  private class P_ClientSessionStateListener implements ISessionListener {

    @Override
    public void sessionChanged(SessionEvent event) {
      switch (event.getType()) {
        case SessionEvent.TYPE_STARTED:
          clientSessionStarted((IClientSession) event.getSource());
          break;
        case SessionEvent.TYPE_STOPPED:
          clientSessionStopping((IClientSession) event.getSource());
        default:
          break;
      }
    }
  }

  /**
   * @param notifications
   */
  public void dispatchNotifications(List<NotificationMessage> notifications) {
    NotificationDispatcher dispatcher = BEANS.get(NotificationDispatcher.class);
    for (NotificationMessage message : notifications) {
      if (CompareUtility.equals(message.getExcludeNodeId(), NOTIFICATION_NODE_ID)) {
        continue;
      }
      if (message.isNotifyAll()) {
        // notify all sessions
        synchronized (m_cacheLock) {
          for (WeakReference<IClientSession> sessionRef : m_sessionIdToSession.values()) {
            if (sessionRef.get() != null) {
              dispatcher.dispatch(sessionRef.get(), message.getNotification());
            }
          }
        }
      }
      else {
        if (CollectionUtility.hasElements(message.getSessionIds())) {
          for (String sessionId : message.getSessionIds()) {
            WeakReference<IClientSession> sessionRef = m_sessionIdToSession.get(sessionId);
            if (sessionRef.get() != null) {
              dispatcher.dispatch(sessionRef.get(), message.getNotification());
            }
          }
          if (CollectionUtility.hasElements(message.getUserIds())) {
            for (String userId : message.getUserIds()) {
              List<WeakReference<IClientSession>> sessionRefs = m_userToSessions.get(userId);
              Iterator<WeakReference<IClientSession>> sessionRefIt = sessionRefs.iterator();
              while (sessionRefIt.hasNext()) {
                WeakReference<IClientSession> sessionRef = sessionRefIt.next();
                if (sessionRef.get() == null) {
                  sessionRefIt.remove();
                }
                else {
                  dispatcher.dispatch(sessionRef.get(), message.getNotification());
                }
              }
            }
          }
        }
      }
    }
  }

  private class P_NotificationPollJob implements IRunnable {
    @Override
    public void run() {
      List<NotificationMessage> notifications = BEANS.get(INotificationServerService.class).getNotifications(NOTIFICATION_NODE_ID);
      System.out.println(String.format("CLIENT NOTIFICATION returned with %s notifications (%s).", notifications.size(), notifications));
      // process notifications
      if (!notifications.isEmpty()) {
        dispatchNotifications(notifications);
      }
      Jobs.schedule(this, Jobs.newInput(ClientRunContexts.empty().subject(NOTIFICATION_SUBJECT).userAgent(UserAgent.createDefault())));
    }
  }

}
