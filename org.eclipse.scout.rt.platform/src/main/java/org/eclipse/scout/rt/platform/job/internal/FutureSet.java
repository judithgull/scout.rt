/*******************************************************************************
 * Copyright (c) 2010-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.platform.job.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.eclipse.scout.rt.platform.Bean;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.config.PlatformConfigProperties.JobManagerInitialFutureSetCapacityProperty;
import org.eclipse.scout.rt.platform.filter.AlwaysFilter;
import org.eclipse.scout.rt.platform.filter.AndFilter;
import org.eclipse.scout.rt.platform.filter.IFilter;
import org.eclipse.scout.rt.platform.filter.NotFilter;
import org.eclipse.scout.rt.platform.filter.OrFilter;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.IJobListenerRegistration;
import org.eclipse.scout.rt.platform.job.IJobManager;
import org.eclipse.scout.rt.platform.job.listener.IJobListener;
import org.eclipse.scout.rt.platform.job.listener.JobEvent;
import org.eclipse.scout.rt.platform.util.Assertions;
import org.eclipse.scout.rt.platform.visitor.IVisitor;

/**
 * Thread-safe implementation of a {@link Set} to contain {@link IFuture}s.
 *
 * @since 5.1
 */
@Bean
public class FutureSet {

  private final Set<JobFutureTask<?>> m_futures;

  private final ReadLock m_readLock;
  private final WriteLock m_writeLock;
  private final Condition m_changedCondition;

  private IJobListenerRegistration m_jobListenerRegistration;

  public FutureSet() {
    m_futures = new HashSet<>(CONFIG.getPropertyValue(JobManagerInitialFutureSetCapacityProperty.class));

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    m_readLock = lock.readLock();
    m_writeLock = lock.writeLock();
    m_changedCondition = m_writeLock.newCondition();
  }

  /**
   * Invoke to initialize this {@link FutureSet}.
   */
  public void init(final IJobManager jobManager) {
    m_jobListenerRegistration = jobManager.addListener(newSignalingFilter(), new IJobListener() {

      @Override
      public void changed(final JobEvent event) {
        m_writeLock.lock();
        try {
          m_changedCondition.signalAll();
        }
        finally {
          m_writeLock.unlock();
        }
      }
    });
  }

  /**
   * Invoke to destroy this {@link FutureSet}.
   */
  public void dispose() {
    m_jobListenerRegistration.dispose();

    // Clear and cancel all futures.
    final List<JobFutureTask<?>> runningFutures;
    m_writeLock.lock();
    try {
      runningFutures = copyFutures();
      m_futures.clear();
      m_changedCondition.signalAll();
    }
    finally {
      m_writeLock.unlock();
    }

    for (final JobFutureTask<?> runningFuture : runningFutures) {
      runningFuture.cancel(true);
    }
  }

  /**
   * Adds the given Future to this {@link FutureSet}.
   */
  public void add(final JobFutureTask<?> future) {
    m_writeLock.lock();
    try {
      m_futures.add(future);
      m_changedCondition.signalAll();
    }
    finally {
      m_writeLock.unlock();
    }
  }

  /**
   * Removes the given Future from this {@link FutureSet}.
   */
  public void remove(final JobFutureTask<?> future) {
    m_writeLock.lock();
    try {
      m_futures.remove(future);
      m_changedCondition.signalAll();
    }
    finally {
      m_writeLock.unlock();
    }
  }

  /**
   * Returns <code>true</code>, if all Futures which are accepted by the given filter match the specified matcher.
   *
   * @param filter
   *          to limit the Futures to be matched by the matcher. If <code>null</code>, all contained Futures are
   *          checked, which is the same as using {@link AlwaysFilter}.
   * @param matcher
   *          to match the filtered Futures.
   * @return <code>true</code> if all Futures accepted by the specified Filter are successfully matched.
   */
  public boolean matchesAll(final IFilter<IFuture<?>> filter, final IFilter<JobFutureTask<?>> matcher) {
    for (final JobFutureTask<?> future : copyFutures()) {
      if (filter != null && !filter.accept(future)) {
        continue;
      }

      if (!matcher.accept(future)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Waits if necessary for at most the given time for the futures matching the given filter to be in 'done' state
   * (completed or cancelled), or the timeout elapses.
   *
   * @param filter
   *          filter to limit the Futures to await to become 'done'. If <code>null</code>, all Futures are awaited,
   *          which is the same as using {@link AlwaysFilter}.
   * @param timeout
   *          the maximal time to wait.
   * @param unit
   *          unit of the given timeout.
   * @throws InterruptedException
   *           if the current thread was interrupted while waiting.
   * @throws TimeoutException
   *           if the wait timed out.
   */
  public void awaitDone(final IFilter<IFuture<?>> filter, final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
    Assertions.assertGreater(timeout, 0L, "Invalid timeout; must be > 0 [timeout={}]", timeout);

    // Wait until all Futures matching the filter are 'done' or the deadline is passed.
    m_writeLock.lockInterruptibly();
    try {
      long nanos = unit.toNanos(timeout);
      while (!matchesAll(filter, DonePromise.PROMISE_DONE_MATCHER) && nanos > 0L) {
        nanos = m_changedCondition.awaitNanos(nanos);
      }

      if (nanos <= 0L) {
        throw new TimeoutException();
      }
    }
    finally {
      m_writeLock.unlock();
    }
  }

  /**
   * Visits all Futures that are accepted by the given Filter and are not in 'done-state'.
   * <p>
   * Filters can be plugged by using logical filters like {@link AndFilter} or {@link OrFilter}, or negated by enclosing
   * a filter in {@link NotFilter}.
   *
   * @param filter
   *          to limit the Futures to be visited. If <code>null</code>, all contained Futures are visited, which is the
   *          same as using {@link AlwaysFilter}.
   * @param visitor
   *          called for each Futures that passed the filter.
   */
  public final void visit(final IFilter<IFuture<?>> filter, final IVisitor<IFuture<?>> visitor) {
    for (final JobFutureTask<?> future : copyFutures()) {
      if (future.isDone() || (filter != null && !filter.accept(future))) {
        continue;
      }
      if (!visitor.visit(future)) {
        return;
      }
    }
  }

  /**
   * Cancels all Futures which are accepted by the given Filter.
   * <p>
   * Filters can be plugged by using logical filters like {@link AndFilter} or {@link OrFilter}, or negated by enclosing
   * a filter in {@link NotFilter}.
   *
   * @param filter
   *          to limit the Futures to be cancelled. If <code>null</code>, all contained Futures are cancelled, which is
   *          the same as using {@link AlwaysFilter}.
   * @param interruptIfRunning
   *          <code>true</code> to interrupt in-progress jobs.
   * @return <code>true</code> if all Futures matching the Filter are cancelled successfully, or <code>false</code>, if
   *         a Future could not be cancelled, typically because already completed normally.
   */
  public boolean cancel(final IFilter<IFuture<?>> filter, final boolean interruptIfRunning) {
    final Set<Boolean> success = new HashSet<>();

    visit(filter, new IVisitor<IFuture<?>>() {

      @Override
      public boolean visit(final IFuture<?> future) {
        success.add(future.cancel(interruptIfRunning));
        return true;
      }
    });

    return Collections.singleton(Boolean.TRUE).equals(success);
  }

  protected List<JobFutureTask<?>> copyFutures() {
    m_readLock.lock();
    try {
      return new ArrayList<>(m_futures); // performance hint: creating an ArrayList has much better performance than creating a HashSet.
    }
    finally {
      m_readLock.unlock();
    }
  }

  /**
   * Creates the filter to signal waiting threads upon a job event.
   */
  protected IFilter<JobEvent> newSignalingFilter() {
    return new IFilter<JobEvent>() {

      @Override
      public boolean accept(final JobEvent event) {
        switch (event.getType()) {
          case JOB_EXECUTION_HINT_ADDED:
          case JOB_EXECUTION_HINT_REMOVED:
            return true; // manual signaling required
          case JOB_STATE_CHANGED:
            switch (event.getData().getState()) {
              case PENDING:
              case RUNNING:
              case WAITING_FOR_BLOCKING_CONDITION:
              case WAITING_FOR_PERMIT:
                return true; // manual signaling required
              default:
                return false; // signaling done by adding/removing the Future
            }
          default:
            return false;
        }
      }
    };
  }
}
