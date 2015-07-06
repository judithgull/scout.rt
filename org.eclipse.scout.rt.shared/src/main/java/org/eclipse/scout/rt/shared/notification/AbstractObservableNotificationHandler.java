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
package org.eclipse.scout.rt.shared.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.shared.ISession;

/**
 *
 */
public abstract class AbstractObservableNotificationHandler<T extends Serializable> implements INotificationHandler<T> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractObservableNotificationHandler.class);
  private final List<IClientNotificationListener<T>> m_globalListeners = new LinkedList<>();
  private final Map<ISession, List<IClientNotificationListener<T>>> m_listeners = new WeakHashMap<>();

  public void addGlobalListener(IClientNotificationListener<T> listener) {
    synchronized (m_globalListeners) {
      m_globalListeners.add(listener);
    }
  }

  public void removeGlobalListeners(IClientNotificationListener<T> listener) {
    synchronized (m_globalListeners) {
      m_globalListeners.remove(listener);
    }
  }

  /**
   * @param listener
   */
  public void addListener(IClientNotificationListener<T> listener) {
    ISession session = Assertions.assertNotNull(ISession.CURRENT.get());
    synchronized (m_listeners) {
      List<IClientNotificationListener<T>> listeners = m_listeners.get(session);
      if (listeners == null) {
        listeners = new ArrayList<>();
        m_listeners.put(session, listeners);
      }
      listeners.add(listener);
    }
  }

  public void removeListener(IClientNotificationListener<T> listener) {
    ISession session = Assertions.assertNotNull(ISession.CURRENT.get());
    synchronized (m_listeners) {
      List<IClientNotificationListener<T>> listeners = m_listeners.get(session);
      if (listeners != null) {
        listeners.remove(listener);
      }
    }
  }

  @Override
  public void handleNotification(T notification) {
    ISession session = ISession.CURRENT.get();
    if (session == null) {
      notifyGlobalListeners(notification);
    }
    else {
      notifySessionBasedListeners(session, notification);
    }

  }

  /**
   * @param notification
   */
  protected void notifyGlobalListeners(T notification) {
    final List<IClientNotificationListener<T>> listeners;
    synchronized (m_globalListeners) {
      listeners = new ArrayList<IClientNotificationListener<T>>(m_globalListeners);
    }
    for (IClientNotificationListener<T> l : listeners) {
      try {
        l.handleNotification(notification);
      }
      catch (Exception e) {
        LOG.error(String.format("Error during notification of global listener '%s'.", l), e);
      }
    }
  }

  /**
   * @param notification
   */
  protected void notifySessionBasedListeners(ISession session, T notification) {
    final List<IClientNotificationListener<T>> listeners;
    synchronized (m_listeners) {
      List<IClientNotificationListener<T>> list = m_listeners.get(session);
      if (list != null) {
        listeners = new ArrayList<IClientNotificationListener<T>>(list);
      }
      else {
        return;
      }
    }
    for (IClientNotificationListener<T> l : listeners) {
      try {
        l.handleNotification(notification);
      }
      catch (Exception e) {
        LOG.error(String.format("Error during notification of listener '%s'.", l), e);
      }
    }
  }
}
