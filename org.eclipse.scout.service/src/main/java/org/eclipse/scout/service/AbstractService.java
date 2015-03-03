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
package org.eclipse.scout.service;

import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.cdi.ApplicationScoped;
import org.eclipse.scout.rt.platform.cdi.Instance;
import org.eclipse.scout.service.IServiceInitializer.ServiceInitializerResult;

/**
 * Convenience {@link IService} implementation with support for config.ini
 * variable injection. see {@link ServiceUtility#injectConfigProperties(IService)}
 */
@ApplicationScoped
public abstract class AbstractService implements IService {
  @SuppressWarnings("unused")
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractService.class);

  @Inject
  private Instance<IServiceInitializer> m_initializers;

  /**
   * This default implementation calls the default initializer {@link DefaultServiceInitializer} which calls
   * {@link org.eclipse.scout.service.ServiceUtility#injectConfigParams}(this).
   * It ensures that properties are getting initialized. This method can be overwritten by
   * implementers. Implementers should aware the property injection is only done if the super call is made.
   */
  @PostConstruct
  protected void initializeService() {
    Instance<IServiceInitializer> initializers = getInitializers();
    if (initializers != null) {
      Iterator<IServiceInitializer> it = initializers.iterator();
      while (it.hasNext()) {
        ServiceInitializerResult res = it.next().initializeService(this);
        if (ServiceInitializerResult.STOP.equals(res)) {
          break;
        }
      }
    }
  }

  protected Instance<IServiceInitializer> getInitializers() {
    return m_initializers;
  }

  /**
   * This default implementation does nothing
   */
  public void disposeServices() {
  }
}