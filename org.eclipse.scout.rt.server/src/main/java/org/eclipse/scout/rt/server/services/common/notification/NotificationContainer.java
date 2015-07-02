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

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.scout.rt.server.context.ServerRunContext;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;

/**
 * The container of all transactional notifications during a server request. Is kept on the {@link ServerRunContext}.
 *
 * @see NotificationTransactionMember
 */
public class NotificationContainer {

  /**
   * The {@link Locale} which is currently associated with the current thread.
   */
  public static final ThreadLocal<NotificationContainer> CURRENT = new ThreadLocal<>();

  private final Set<NotificationMessage> m_notifications = new HashSet<>();

  public NotificationContainer() {
  }

  /**
   */
  public static NotificationContainer get() {
    return CURRENT.get();
  }

  public boolean add(NotificationMessage message) {
    return m_notifications.add(message);
  }

  public void addAll(Collection<NotificationMessage> messages) {
    m_notifications.addAll(messages);
  }

  public Set<NotificationMessage> getNotifications() {
    return new HashSet<NotificationMessage>(m_notifications);
  }

}
