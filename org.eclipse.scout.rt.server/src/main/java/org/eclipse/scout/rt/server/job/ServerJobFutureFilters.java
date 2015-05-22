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

import java.util.Collection;
import java.util.regex.Pattern;

import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.commons.filter.NotFilter;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.context.ServerRunContext;
import org.eclipse.scout.rt.shared.ISession;

/**
 * Filters for server job Futures.
 *
 * @since 5.1
 */
public final class ServerJobFutureFilters {

  private ServerJobFutureFilters() {
  }

  /**
   * Filter to accept Futures of all server jobs that comply with the given characteristics.<br/>
   * The 'setter-methods' returns <code>this</code> in order to support for method chaining.
   *
   * @since 5.1
   */
  public static class Filter extends org.eclipse.scout.rt.platform.job.JobFutureFilters.Filter {

    @Override
    protected void postConstruct() {
      andFilter(ServerJobFilter.INSTANCE);
    }

    @Override
    public Filter andFilter(final IFilter<IFuture<?>> filter) {
      return (Filter) super.andFilter(filter);
    }

    @Override
    public Filter names(final String... names) {
      return (Filter) super.names(names);
    }

    @Override
    public Filter nameRegex(final Pattern regex) {
      return (Filter) super.nameRegex(regex);
    }

    @Override
    public Filter futures(final IFuture<?>... futures) {
      return (Filter) super.futures(futures);
    }

    @Override
    public Filter futures(final Collection<IFuture<?>> futures) {
      return (Filter) super.futures(futures);
    }

    @Override
    public Filter currentFuture() {
      return (Filter) super.currentFuture();
    }

    @Override
    public Filter notCurrentFuture() {
      return (Filter) super.notCurrentFuture();
    }

    @Override
    public Filter blocked() {
      return (Filter) super.blocked();
    }

    @Override
    public Filter notBlocked() {
      return (Filter) super.notBlocked();
    }

    @Override
    public Filter periodic() {
      return (Filter) super.periodic();
    }

    @Override
    public Filter notPeriodic() {
      return (Filter) super.notPeriodic();
    }

    @Override
    public Filter mutex(final Object mutexObject) {
      return (Filter) super.mutex(mutexObject);
    }

    /**
     * To accept only jobs which are run on behalf of the given server session.
     */
    public Filter session(final IServerSession session) {
      andFilter(new SessionFilter(session));
      return this;
    }

    /**
     * To accept only jobs which are run on behalf of the current server session.
     *
     * @see ISession#CURRENT
     */
    public Filter currentSession() {
      andFilter(new SessionFilter(ISession.CURRENT.get()));
      return this;
    }

    /**
     * To accept only jobs which are not run on behalf of the current server session.
     *
     * @see ISession#CURRENT
     */
    public Filter notCurrentSession() {
      andFilter(new NotFilter<>(new SessionFilter(ISession.CURRENT.get())));
      return this;
    }
  }

  /**
   * Filter which accepts Futures only if belonging to the given server session.
   *
   * @since 5.1
   */
  public static class SessionFilter implements IFilter<IFuture<?>> {

    private final ISession m_session;

    public SessionFilter(final ISession session) {
      m_session = session;
    }

    @Override
    public boolean accept(final IFuture<?> future) {
      final RunContext runContext = future.getJobInput().runContext();
      if (runContext instanceof ServerRunContext) {
        return m_session == ((ServerRunContext) runContext).session();
      }
      else {
        return false;
      }
    }
  }

  /**
   * Filter which accepts only Futures from server jobs.
   *
   * @since 5.1
   */
  public static class ServerJobFilter implements IFilter<IFuture<?>> {

    public static final IFilter<IFuture<?>> INSTANCE = new ServerJobFilter();

    private ServerJobFilter() {
    }

    @Override
    public boolean accept(final IFuture<?> future) {
      return ServerJobs.isServerJob(future);
    }
  }
}