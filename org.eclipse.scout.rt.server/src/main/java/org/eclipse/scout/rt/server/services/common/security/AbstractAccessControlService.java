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

import java.io.Serializable;
import java.security.Permissions;
import java.util.Collection;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.service.IService;
import org.eclipse.scout.rt.server.services.common.clientnotification.IClientNotificationService;
import org.eclipse.scout.rt.server.services.common.clientnotification.SingleUserFilter;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotification;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationListener;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationListenerService;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationMessage;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterSynchronizationService;
import org.eclipse.scout.rt.server.transaction.ITransaction;
import org.eclipse.scout.rt.shared.services.common.security.AbstractSharedAccessControlService;
import org.eclipse.scout.rt.shared.services.common.security.AccessControlChangedNotification;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.rt.shared.services.common.security.ResetAccessControlChangedNotification;

/**
 * Implementations should override {@link #execLoadPermissions()}
 */
public abstract class AbstractAccessControlService extends AbstractSharedAccessControlService implements IClusterNotificationListenerService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractAccessControlService.class);

  @Override
  protected void notifySetPermisions(Permissions p) {
    // notify clients:
    String userId = BEANS.get(IAccessControlService.class).getUserIdOfCurrentSubject();
    BEANS.get(IClientNotificationService.class).putNotification(new AccessControlChangedNotification(p), new SingleUserFilter(userId, 120000L));
  }

  @Override
  public void clearCache() {
    clearCacheNoFire();

    //notify clients with a filter, that will be accepted nowhere:
    BEANS.get(IClientNotificationService.class).putNotification(new ResetAccessControlChangedNotification(), new SingleUserFilter(null, 0L));
    distributeCluster(new AccessControlCacheChangedClusterNotification());
  }

  @Override
  public void clearCacheOfUserIds(Collection<String> userIds) {
    clearCacheOfUserIdsNoFire(userIds);
    distributeCluster(new AccessControlCacheChangedClusterNotification(userIds));

    //notify clients:
    for (String userId : userIds) {
      if (userId != null) {
        BEANS.get(IClientNotificationService.class).putNotification(new AccessControlChangedNotification(null), new SingleUserFilter(userId, 120000L));
      }
    }
  }

  protected void distributeCluster(IClusterNotification notification) {
    IClusterSynchronizationService s = BEANS.opt(IClusterSynchronizationService.class);
    if (s != null) {
      try {
        if (ITransaction.CURRENT.get() != null) {
          s.publishTransactional(notification);
        }
        else {
          s.publish(notification);
        }
      }
      catch (ProcessingException e) {
        LOG.error("failed notifying cluster for permission changes", e);
      }
    }
  }

  @Override
  public Class<? extends IService> getDefiningServiceInterface() {
    return IAccessControlService.class;
  }

  @Override
  public IClusterNotificationListener getClusterNotificationListener() {
    return new IClusterNotificationListener() {

      @Override
      public void onNotification(IClusterNotificationMessage message) throws ProcessingException {
        Serializable clusterNotification = message.getNotification();
        if ((clusterNotification instanceof AccessControlCacheChangedClusterNotification)) {
          AccessControlCacheChangedClusterNotification n = (AccessControlCacheChangedClusterNotification) clusterNotification;
          if (n.getUserIds().isEmpty()) {
            clearCacheNoFire();
          }
          else {
            clearCacheOfUserIdsNoFire(n.getUserIds());
          }
        }
      }
    };
  }
}
