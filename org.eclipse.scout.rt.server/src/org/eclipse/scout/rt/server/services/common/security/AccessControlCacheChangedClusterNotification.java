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
package org.eclipse.scout.rt.server.services.common.security;

import java.util.Collection;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotification;

public class AccessControlCacheChangedClusterNotification implements IClusterNotification {
  private static final long serialVersionUID = 128460814967537176L;

  private final Set<String> m_userIds;

  public AccessControlCacheChangedClusterNotification() {
    this(null);
  }

  public AccessControlCacheChangedClusterNotification(Collection<String> userIds) {
    m_userIds = CollectionUtility.hashSetWithoutNullElements(userIds);
  }

  public Set<String> getUserIds() {
    return m_userIds;
  }

  @Override
  public String toString() {
    return "AccessControlCacheChangedClusterNotification [m_userIds=" + m_userIds + "]";
  }

  @Override
  public boolean coalesce(IClusterNotification existingNotification0) {
    if (existingNotification0 instanceof AccessControlCacheChangedClusterNotification) {
      AccessControlCacheChangedClusterNotification existingNotification = (AccessControlCacheChangedClusterNotification) existingNotification0;
      m_userIds.addAll(existingNotification.getUserIds());
      return true;
    }
    return false;
  }
}
