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
package org.eclipse.scout.rt.shared.services.common.notification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;

/**
 *
 */
public class NotificationMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final Set<String> m_sessionIds;
  private final Set<String> m_userIds;
  private final String m_excludeNodeId;
  private final boolean m_notifyAll;
  private final Serializable m_notification;

  public NotificationMessage(Set<String> sessionIds, Set<String> userIds, boolean notifyAll, Serializable notification) {
    this(sessionIds, userIds, notifyAll, null, notification);
  }

  public NotificationMessage(Set<String> sessionIds, Set<String> userIds, boolean notifyAll, String excludeNodeId, Serializable notification) {
    m_sessionIds = Collections.unmodifiableSet(CollectionUtility.hashSet(sessionIds));
    m_userIds = Collections.unmodifiableSet(CollectionUtility.hashSet(userIds));
    m_notifyAll = notifyAll;
    m_excludeNodeId = excludeNodeId;
    m_notification = notification;
  }

  public Set<String> getSessionIds() {
    return m_sessionIds;
  }

  public Set<String> getUserIds() {
    return m_userIds;
  }

  public boolean isNotifyAll() {
    return m_notifyAll;
  }

  public String getExcludeNodeId() {
    return m_excludeNodeId;
  }

  public Serializable getNotification() {
    return m_notification;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("NotificationMessage sessions=").append(getSessionIds()).append(", ");
    builder.append("userIds=").append(getUserIds()).append(", ");
    builder.append("excludeNodeId=").append(getExcludeNodeId()).append(", ");
    builder.append("notification=").append(getNotification());
    return builder.toString();
  }
}
