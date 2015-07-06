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
package org.eclipse.scout.rt.server.services.common.code;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.server.notification.INotificationCoalescer;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;

/**
 * Coalesce {@link UnloadCodeTypeCacheClusterNotification}s.
 */
public class UnloadCodeTypeClusterCoalescer implements INotificationCoalescer<UnloadCodeTypeCacheClusterNotification> {

  /**
   * Coalesce all {@link UnloadCodeTypeCacheClusterNotification}s to a single notification with all codetypes.
   */
  @Override
  public Set<UnloadCodeTypeCacheClusterNotification> coalesce(Set<UnloadCodeTypeCacheClusterNotification> notifications) {
    if (notifications.isEmpty()) {
      return CollectionUtility.emptyHashSet();
    }

    List<Class<? extends ICodeType<?, ?>>> codeTypeList = CollectionUtility.arrayList(collectCodeTypes(notifications));
    return CollectionUtility.hashSet(new UnloadCodeTypeCacheClusterNotification(codeTypeList));
  }

  private Set<Class<? extends ICodeType<?, ?>>> collectCodeTypes(Set<UnloadCodeTypeCacheClusterNotification> notifications) {
    Set<Class<? extends ICodeType<?, ?>>> codeTypes = new HashSet<>();
    for (UnloadCodeTypeCacheClusterNotification notification : notifications) {
      List<Class<? extends ICodeType<?, ?>>> types = notification.getTypes();
      codeTypes.addAll(types);
    }
    return codeTypes;
  }
}
