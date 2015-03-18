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
package org.eclipse.scout.rt.server.job;

import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.JobInput;
import org.eclipse.scout.rt.server.IServerSession;

/**
 * Filter which accepts Futures only if belonging to the given session.
 *
 * @since 5.1
 */
public class ServerSessionFutureFilter implements IFilter<IFuture<?>> {

  private final IServerSession m_session;

  public ServerSessionFutureFilter(final IServerSession session) {
    m_session = session;
  }

  @Override
  public boolean accept(final IFuture<?> future) {
    final JobInput jobInput = future.getJobInput();
    if (jobInput instanceof ServerJobInput) {
      return m_session == ((ServerJobInput) jobInput).getSession();
    }
    else {
      return false;
    }
  }
}