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
package org.eclipse.scout.rt.client.job;

import java.util.Collection;

import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.commons.filter.NotFilter;
import org.eclipse.scout.commons.filter.OrFilter;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.job.ClientJobFutureFilters.ClientJobFilter;
import org.eclipse.scout.rt.client.job.ClientJobFutureFilters.ModelJobFilter;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.JobEventFilters.FutureEventFilterDelegate;
import org.eclipse.scout.rt.platform.job.listener.JobEvent;
import org.eclipse.scout.rt.platform.job.listener.JobEventType;
import org.eclipse.scout.rt.shared.ISession;

/**
 * Factory methods to create event filters related to client- and model jobs.
 *
 * @since 5.1
 */
public final class ClientJobEventFilters {

  private static final IFilter<JobEvent> CLIENT_JOB_EVENT_FILTER = new FutureEventFilterDelegate(ClientJobFilter.INSTANCE);
  private static final IFilter<JobEvent> MODEL_JOB_EVENT_FILTER = new FutureEventFilterDelegate(ModelJobFilter.INSTANCE);

  private ClientJobEventFilters() {
  }

  /**
   * Creates a filter to accept events of all client- or model jobs that comply with some specific characteristics. By
   * default, the filter returned accepts all client/model-job related events. The filter is designed to support method
   * chaining.
   */
  public static Filter allFilter() {
    return new Filter();
  }

  /**
   * Filter to accept events of all client- and model jobs that comply with the given characteristics.<br/>
   * The 'setter-methods' returns <code>this</code> in order to support for method chaining.
   *
   * @since 5.1
   */
  public static class Filter extends org.eclipse.scout.rt.platform.job.JobEventFilters.Filter {

    @Override
    protected void postConstruct() {
      andFilter(new OrFilter<>(CLIENT_JOB_EVENT_FILTER, MODEL_JOB_EVENT_FILTER));
    }

    @Override
    public Filter andFilter(final IFilter<JobEvent> filter) {
      return (Filter) super.andFilter(filter);
    }

    @Override
    public Filter eventTypes(final JobEventType... eventTypes) {
      return (Filter) super.eventTypes(eventTypes);
    }

    @Override
    public Filter ids(final String... ids) {
      return (Filter) super.ids(ids);
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
     * To accept only events for jobs which are run on behalf of the given client session.
     */
    public Filter session(final IClientSession session) {
      andFilter(new FutureEventFilterDelegate(new ClientJobFutureFilters.SessionFilter(session)));
      return this;
    }

    /**
     * To accept only events for jobs which are run on behalf of the current client session.
     *
     * @see ISession#CURRENT
     */
    public Filter currentSession() {
      andFilter(new FutureEventFilterDelegate(new ClientJobFutureFilters.SessionFilter(ISession.CURRENT.get())));
      return this;
    }

    /**
     * To accept only events for jobs which are not run on behalf of the current server session.
     *
     * @see ISession#CURRENT
     */
    public Filter notCurrentSession() {
      andFilter(new FutureEventFilterDelegate(new NotFilter<>(new ClientJobFutureFilters.SessionFilter(ISession.CURRENT.get()))));
      return this;
    }

    /**
     * To accept only events for model jobs, and not client jobs.
     */
    public Filter modelJobsOnly() {
      andFilter(MODEL_JOB_EVENT_FILTER);
      return this;
    }

    /**
     * To accept only events for client jobs, and not model jobs.
     */
    public Filter clientJobsOnly() {
      andFilter(CLIENT_JOB_EVENT_FILTER);
      return this;
    }
  }
}