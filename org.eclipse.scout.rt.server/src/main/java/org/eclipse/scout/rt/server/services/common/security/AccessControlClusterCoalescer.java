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
package org.eclipse.scout.rt.server.services.common.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.server.notification.ICoalescer;

/**
 * Coalesce {@link AccessControlClusterNotification}s.
 */
public class AccessControlClusterCoalescer implements ICoalescer<AccessControlClusterNotification> {

  /**
   * Coalesce all {@link AccessControlClusterNotification}s to a single notification with all user ids.
   */
  @Override
  public List<AccessControlClusterNotification> coalesce(List<AccessControlClusterNotification> notifications) {
    if (notifications.isEmpty()) {
      return CollectionUtility.emptyArrayList();
    }

    Set<String> userIds = collectUserIds(notifications);
    return CollectionUtility.arrayList(new AccessControlClusterNotification(userIds));
  }

  private Set<String> collectUserIds(List<AccessControlClusterNotification> notifications) {
    Set<String> userIds = new HashSet<>();
    for (AccessControlClusterNotification notification : notifications) {
      userIds.addAll(notification.getUserIds());
    }
    return userIds;
  }
}
