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
package org.eclipse.scout.rt.server.services.common.code;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.service.IService;
import org.eclipse.scout.rt.server.Server;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationListener;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationListenerService;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationMessage;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterSynchronizationService;
import org.eclipse.scout.rt.server.session.ServerSessionProvider;
import org.eclipse.scout.rt.server.transaction.ITransaction;
import org.eclipse.scout.rt.shared.services.common.code.AbstractSharedCodeService;
import org.eclipse.scout.rt.shared.services.common.code.CodeTypeChangedNotification;
import org.eclipse.scout.rt.shared.services.common.code.ICodeService;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;

@Server
@Order(2)
public class CodeService extends AbstractSharedCodeService implements IClusterNotificationListenerService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(CodeService.class);

  @Override
  protected Long provideCurrentPartitionId() {
    Map<String, Object> sharedVariableMap = ServerSessionProvider.currentSession().getSharedVariableMap();
    if (sharedVariableMap.containsKey(ICodeType.PROP_PARTITION_ID)) {
      return (Long) sharedVariableMap.get(ICodeType.PROP_PARTITION_ID);
    }
    return super.provideCurrentPartitionId();
  }

  @Override
  protected void notifyReloadCodeTypes(List<Class<? extends ICodeType<?, ?>>> codetypeList) throws ProcessingException {
    // notify clients:
    BEANS.get(IClientNotificationService.class).putNotification(new CodeTypeChangedNotification(codetypeList), new AllUserFilter(AllUserFilter.DEFAULT_TIMEOUT));
    distributeCluster(new UnloadCodeTypeCacheClusterNotification(codetypeList));
  }

  protected void distributeCluster(Serializable notification) {
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
        LOG.error("failed notifying cluster", e);
      }
    }
  }

  @Override
  public IClusterNotificationListener getClusterNotificationListener() {
    return new IClusterNotificationListener() {
      @Override
      public void onNotification(IClusterNotificationMessage message) throws ProcessingException {
        Serializable clusterNotification = message.getNotification();
        if (clusterNotification instanceof UnloadCodeTypeCacheClusterNotification) {
          UnloadCodeTypeCacheClusterNotification n = (UnloadCodeTypeCacheClusterNotification) clusterNotification;
          reloadCodeTypesNoFire(n.getTypes());
        }
      }
    };
  }

  @Override
  public Class<? extends IService> getDefiningServiceInterface() {
    return ICodeService.class;
  }
}
