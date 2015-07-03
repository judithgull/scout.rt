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
  private final boolean m_notifyAllSessions;
  private final boolean m_notifyAllNodes;

  private final Serializable m_notification;

  public NotificationMessage(Set<String> sessionIds, Set<String> userIds, boolean notifyAllSessions, boolean notifyAllNodes, String excludeNodeId, Serializable notification) {
    m_sessionIds = Collections.unmodifiableSet(CollectionUtility.hashSet(sessionIds));
    m_userIds = Collections.unmodifiableSet(CollectionUtility.hashSet(userIds));
    m_notifyAllSessions = notifyAllSessions;
    m_notifyAllNodes = notifyAllNodes;
    m_excludeNodeId = excludeNodeId;
    m_notification = notification;
  }

  public static NotificationMessage createSessionNotification(Set<String> sessionIds, Serializable notification) {
    return createSessionNotification(sessionIds, null, notification);
  }

  public static NotificationMessage createSessionNotification(Set<String> sessionIds, String excludeNodeId, Serializable notification) {
    return new NotificationMessage(sessionIds, null, false, false, excludeNodeId, notification);
  }

  public static NotificationMessage createUserNotification(Set<String> userIds, Serializable notification) {
    return createUserNotification(userIds, null, notification);
  }

  public static NotificationMessage createUserNotification(Set<String> userIds, String excludeNodeId, Serializable notification) {
    return new NotificationMessage(null, userIds, false, false, excludeNodeId, notification);
  }

  public static NotificationMessage createAllSessionsNotification(Serializable notification) {
    return createAllSessionsNotification(null, notification);
  }

  public static NotificationMessage createAllSessionsNotification(String excludeNodeId, Serializable notification) {
    return new NotificationMessage(null, null, true, false, excludeNodeId, notification);
  }

  public static NotificationMessage createAllNodesNotification(Serializable notification) {
    return createAllNodesNotification(null, notification);
  }

  public static NotificationMessage createAllNodesNotification(String excludeNodeId, Serializable notification) {
    return new NotificationMessage(null, null, false, true, excludeNodeId, notification);
  }

  public Set<String> getSessionIds() {
    return m_sessionIds;
  }

  public Set<String> getUserIds() {
    return m_userIds;
  }

  public boolean isNotifyAllSessions() {
    return m_notifyAllSessions;
  }

  public boolean isNotifyAllNodes() {
    return m_notifyAllNodes;
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
    builder.append("notifyAllSessions=").append(isNotifyAllSessions()).append(", ");
    builder.append("notifyAllNodes=").append(isNotifyAllNodes()).append(", ");
    builder.append("notification=").append(getNotification());
    return builder.toString();
  }
}
