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
package org.eclipse.scout.rt.server.services.common.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.services.common.security.LogoutService;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;

/**
 *
 */
public class NotificationNodeQueue {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(LogoutService.class);

  private final String m_nodeId;
  private final int m_capacity;
  private final List<NotificationMessage> m_notifications = new LinkedList<NotificationMessage>();
  private final Map<String /*userId*/, List<IServerSession>> m_userSessions = new HashMap<>();

  public NotificationNodeQueue(String nodeId, int capacity) {
    m_nodeId = nodeId;
    m_capacity = capacity;
  }

  public String getNodeId() {
    return m_nodeId;
  }

  public int getCapacity() {
    return m_capacity;
  }

  /**
   * @param userId
   */
  public void registerUserId(String userId, IServerSession session) {
    synchronized (m_userSessions) {
      List<IServerSession> sessions = m_userSessions.get(userId);
      if (session == null) {
        sessions = new ArrayList<IServerSession>(3);
        m_userSessions.put(userId, sessions);
      }
      sessions.add(session);
    }
  }

  public void add(NotificationMessage notification) {
    synchronized (m_notifications) {
      m_notifications.add(notification);
      m_notifications.notify();
    }
  }

  public void put(NotificationMessage notificaiton) {
    putAll(CollectionUtility.arrayList(notificaiton));
  }

  public void putAll(List<? extends NotificationMessage> notifications) {
    synchronized (m_notifications) {
      int newSize = m_notifications.size() + notifications.size();
      if (newSize > getCapacity()) {
        LOG.warn(String.format("Notification queue capacity reached. Remove oldest %s notification messages.", newSize - getCapacity()));
        for (; newSize > getCapacity(); newSize--) {
          m_notifications.remove(0);
        }
      }
      m_notifications.addAll(notifications);
      m_notifications.notify();
    }
  }

  public void releaseAllWaitingThreads() {
    synchronized (m_notifications) {
      m_notifications.notifyAll();
    }
  }

  public List<NotificationMessage> consume(int maxAmount, long amount, TimeUnit unit) {
    List<NotificationMessage> result = new ArrayList<NotificationMessage>(maxAmount);
    getNotifications(maxAmount, amount, unit, result, true);
    LOG.debug(String.format("consumed %s notifications.", result.size()));
    return result;
  }

  protected void getNotifications(int maxAmount, long amount, TimeUnit unit, List<NotificationMessage> collector, boolean reschedule) {
    synchronized (m_notifications) {
      if (m_notifications.isEmpty()) {
        try {
          m_notifications.wait(unit.toMillis(amount));
        }
        catch (InterruptedException e) {
          LOG.warn("Consume notification thread waiting for notifications interrupted.");
        }
      }
      Iterator<NotificationMessage> it = m_notifications.iterator();
      int itemCount = collector.size();
      while (it.hasNext() && itemCount < maxAmount) {
        collector.add(it.next());
        it.remove();
        itemCount++;
      }
      // Optimization to not go back with one notification when some are about to pop up.
      if (reschedule && itemCount < maxAmount) {
        getNotifications(maxAmount, 234, TimeUnit.MILLISECONDS, collector, false);
      }
    }
  }

}
