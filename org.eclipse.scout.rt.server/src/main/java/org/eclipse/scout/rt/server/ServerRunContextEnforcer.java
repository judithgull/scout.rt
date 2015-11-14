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
package org.eclipse.scout.rt.server;

import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.interceptor.IBeanInterceptor;
import org.eclipse.scout.rt.platform.interceptor.IBeanInvocationContext;

/**
 * Used in mixed-application to ensure proper ServerRunContext for service calls.
 */
@ApplicationScoped
public class ServerRunContextEnforcer<T> implements IBeanInterceptor<T> {

  @Override
  public Object invoke(final IBeanInvocationContext<T> context) {
    // TODO ensure proper ServerRunContext: only create ServerRunContext if not running in such a ServerRunContext yet.
    System.out.printf("Ensure proper ServerRunContext for [%s]", context.getTargetMethod()).println();
    return context.proceed();
  }
}
