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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;

/**
 *
 */
@ApplicationScoped
public class NotificationRegistry {

  private final Map<String /*notificationNodeId*/, NotificationNodeQueue> m_notificationQueues = new HashMap<>();

  public void registerUser(String notificationNodeId, IServerSession session) {
    Assertions.assertNotNull(session);
    NotificationNodeQueue queue = m_notificationQueues.get(notificationNodeId);
    if (queue == null) {
      // create new
      // TODO make configurable
      queue = new NotificationNodeQueue(notificationNodeId, 200);
      m_notificationQueues.put(notificationNodeId, queue);
    }
    queue.registerUserId(session.getUserId(), session);
  }

  public List<NotificationMessage> consume(String notificationNodeId, int maxAmount, int amount, TimeUnit unit) {
    NotificationNodeQueue queue = m_notificationQueues.get(notificationNodeId);
    return Assertions.assertNotNull(queue).consume(maxAmount, amount, unit);
  }

  public void put(String userId, Serializable notification) {

  }

  protected List<IServerSession> getServerSessions(String userId) {
    return null;//
  }

}
