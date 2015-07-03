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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.shared.services.common.notification.NotficationAddress;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;

/**
 *
 */
@ApplicationScoped
public class ClientNotificationCoalescer {

  public Set<NotificationMessage> coalesce(Set<NotificationMessage> inNotifications) {
    // sort by address
    Map<NotficationAddress, Set<Serializable>> notificationsPerAddress = new HashMap<>();
    for (NotificationMessage message : inNotifications) {
      Set<Serializable> notifications = notificationsPerAddress.get(message.getAddress());
      if (notifications == null) {
        notifications = new HashSet<>();
        notificationsPerAddress.put(message.getAddress(), notifications);
      }
      notifications.add(message.getNotification());
    }
    for (Entry<NotficationAddress, Set<Serializable>> e : notificationsPerAddress.entrySet()) {

    }
    return inNotifications;
  }

  protected Set<NotificationMessage> coalesce(NotficationAddress address, Set<Serializable> notificationsIn) {
    BEANS.get(NotificationCoalescer.class).coalesce(notificationsIn);
    return null;
  }
}
