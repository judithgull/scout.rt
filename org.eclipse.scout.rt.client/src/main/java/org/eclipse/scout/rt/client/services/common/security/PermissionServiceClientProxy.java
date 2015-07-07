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
package org.eclipse.scout.rt.client.services.common.security;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.Client;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.session.ClientSessionProvider;
import org.eclipse.scout.rt.platform.service.AbstractService;
import org.eclipse.scout.rt.shared.services.common.security.IPermissionService;
import org.eclipse.scout.rt.shared.servicetunnel.ServiceTunnelUtility;

/**
 * Cache of available permission types (not instances)
 * <p>
 * Service state is per {@link IClientSession} type and stored in global map
 */
@Client
@Order(-2)
public class PermissionServiceClientProxy extends AbstractService implements IPermissionService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(PermissionServiceClientProxy.class);

  private final Object m_stateLock = new Object();
  private final Map<Object, ServiceState> m_stateMap = new HashMap<Object, ServiceState>();

  public PermissionServiceClientProxy() {
  }

  private ServiceState getServiceState() {
    IClientSession session = ClientSessionProvider.currentSession();
    if (session == null) {
      LOG.warn("could not find a client session");
      return null;
    }
    Object key = session.getClass();
    synchronized (m_stateLock) {
      ServiceState data = (ServiceState) m_stateMap.get(key);
      if (data == null) {
        data = new ServiceState();
        m_stateMap.put(key, data);
      }
      return data;
    }
  }

  @Override
  public Set<Class<? extends Permission>> getAllPermissionClasses() {
    ServiceState state = getServiceState();
    checkCache(state);
    return state.m_permissionClasses;
  }

  private void checkCache(ServiceState state) {
    synchronized (state.m_permissionClassesLock) {
      if (state.m_permissionClasses == null) {
        state.m_permissionClasses = getRemoteService().getAllPermissionClasses();
      }
    }
  }

  private IPermissionService getRemoteService() {
    return ServiceTunnelUtility.createProxy(IPermissionService.class);
  }

  private static final class ServiceState {
    private final Object m_permissionClassesLock = new Object();
    private Set<Class<? extends Permission>> m_permissionClasses;
  }
}
