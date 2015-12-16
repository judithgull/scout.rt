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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.eclipse.scout.rt.platform.Bean;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.ISchedulingSemaphore;
import org.eclipse.scout.rt.platform.job.JobInput;
import org.eclipse.scout.rt.platform.util.Assertions;
import org.eclipse.scout.rt.platform.util.Assertions.AssertionException;
import org.eclipse.scout.rt.platform.util.ToStringBuilder;
import org.eclipse.scout.rt.platform.util.concurrent.InterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ISchedulingSemaphoreed}.
 *
 * @since 5.2
 */
@Bean
public class SchedulingSemaphore implements ISchedulingSemaphore {

  private static final Logger LOG = LoggerFactory.getLogger(SchedulingSemaphore.class);

  private final ReadLock m_readLock;
  private final WriteLock m_writeLock;

  private volatile int m_permits;
  private volatile boolean m_sealed;
  private final Deque<AcquisitionTask> m_queue;
  private final Set<IFuture<?>> m_permitOwners;

  public SchedulingSemaphore() {
    m_permits = Integer.MAX_VALUE;
    m_queue = new ArrayDeque<>();
    m_permitOwners = new HashSet<>();

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    m_readLock = lock.readLock();
    m_writeLock = lock.writeLock();
  }

  @Override
  public SchedulingSemaphore withPermits(final int permits) {
    Assertions.assertFalse(m_sealed, "The number of permits cannot be changed because the semaphore is sealed [semaphore={}]", this);
    Assertions.assertGreaterOrEqual(permits, 0, "Number of semaphore permits must be '>= 0'");
    m_permits = permits;
    return this;
  }

  @Override
  public ISchedulingSemaphore seal() {
    m_sealed = true;
    return this;
  }

  @Override
  public int getCompetitorCount() {
    m_readLock.lock();
    try {
      return m_queue.size() + m_permitOwners.size();
    }
    finally {
      m_readLock.unlock();
    }
  }

  @Override
  public boolean isPermitOwner(final IFuture<?> task) {
    m_readLock.lock();
    try {
      return m_permitOwners.contains(task);
    }
    finally {
      m_readLock.unlock();
    }
  }

  /**
   * Acquires a permit from this semaphore, blocking until one is available, or the thread is interrupted.
   *
   * @param task
   *          the task to acquire a permit for.
   * @param queuePosition
   *          the position where to place the task in the queue of competing tasks if no permit is free at the time of
   *          invocation.
   * @throws InterruptedException
   *           if the current thread was interrupted while waiting.
   */
  protected void acquire(final IFuture<?> task, final QueuePosition queuePosition) {
    assertSameSemaphore(task);

    final Object acquisitionLock = new Object();
    final AtomicBoolean waitingForPermit = new AtomicBoolean(true);

    compete(task, queuePosition, new IPermitAcquiredCallback() {

      @Override
      public void onPermitAcquired() {
        synchronized (acquisitionLock) {
          if (waitingForPermit.get()) {
            acquisitionLock.notify();
          }
          else {
            release(task);
          }
        }
      }
    });

    // Block the current thread until a permit is acquired.
    synchronized (acquisitionLock) {
      while (!isPermitOwner(task)) {
        try {
          acquisitionLock.wait();
        }
        catch (final java.lang.InterruptedException e) {
          Thread.currentThread().interrupt(); // Restore the interrupted status because cleared by catching InterruptedException.
          waitingForPermit.set(false);

          throw new InterruptedException("Interrupted while competing for a permit")
              .withContextInfo("task", task.getJobInput().getName())
              .withContextInfo("schedulingSemaphore", this);
        }
      }
    }
  }

  /**
   * Makes the given task to compete for a permit. Upon permit acquisition, the given callback is invoked. The callback
   * is invoked immediately and on behalf of the current thread, if being available at the time of invocation.
   * Otherwise, this method returns immediately.
   *
   * @param task
   *          the task to acquire a permit for.
   * @param queuePosition
   *          the position where to place the task in the queue of competing tasks if no permit is free at the time of
   *          invocation.
   * @param permitAcquiredCallback
   *          the callback to be invoked once the given task acquired a permit.
   * @return <code>true</code> if acquired a permit, or <code>false</code> otherwise.
   */
  protected boolean compete(final IFuture<?> task, final QueuePosition queuePosition, final IPermitAcquiredCallback permitAcquiredCallback) {
    assertSameSemaphore(task);

    boolean permitFree;
    m_writeLock.lock();
    try {
      permitFree = (m_permitOwners.size() < m_permits && m_queue.isEmpty());

      if (permitFree) {
        m_permitOwners.add(task);
      }
      else {
        switch (queuePosition) {
          case HEAD:
            m_queue.offerFirst(new AcquisitionTask(task, permitAcquiredCallback));
            break;
          case TAIL:
            m_queue.offerLast(new AcquisitionTask(task, permitAcquiredCallback));
            break;
          default:
            throw new IllegalArgumentException("illegal queue position");
        }
      }
    }
    finally {
      m_writeLock.unlock();
    }

    // Notify the new permit owner about its permit acquisition.
    if (permitFree) {
      permitAcquiredCallback.onPermitAcquired();
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Releases a permit, returning it to the semaphore.
   *
   * @param permitOwner
   *          the task to release the permit from.
   */
  protected void release(final IFuture<?> permitOwner) {
    assertSameSemaphore(permitOwner);
    assertPermitOwner(permitOwner);

    final AcquisitionTask acquisitionTask;
    m_writeLock.lock();
    try {
      m_permitOwners.remove(permitOwner);

      acquisitionTask = m_queue.poll();
      if (acquisitionTask != null) {
        m_permitOwners.add(acquisitionTask.getCompetingTask());
      }
    }
    finally {
      m_writeLock.unlock();
    }

    if (acquisitionTask != null) {
      acquisitionTask.notifyPermitAcquired();
    }
  }

  protected void assertSameSemaphore(final IFuture<?> task) {
    Assertions.assertSame(this, task.getJobInput().getSchedulingSemaphore(), "Wrong scheduling semaphore [expected={}, actual={}]", this, task.getJobInput().getSchedulingSemaphore());
  }

  protected void assertPermitOwner(final IFuture<?> task) {
    Assertions.assertTrue(isPermitOwner(task), "Task does not own a permit [task={}]", task);
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.attr("permitOwners", m_permitOwners);
    builder.attr("queue", m_queue);
    return builder.toString();
  }

  // ==== Helper classes ==== //

  protected static class AcquisitionTask {

    private final IFuture<?> m_competingTask;
    private final IPermitAcquiredCallback m_callback;

    public AcquisitionTask(final IFuture<?> competingTask, final IPermitAcquiredCallback callback) {
      m_competingTask = competingTask;
      m_callback = callback;
    }

    public IFuture<?> getCompetingTask() {
      return m_competingTask;
    }

    public void notifyPermitAcquired() {
      try {
        m_callback.onPermitAcquired();
      }
      catch (final RuntimeException e) {
        LOG.error("Failed to notify new permit owner about permit acquisition [task={}]", m_competingTask, e);
      }
    }
  }

  /**
   * Position in the queue of competing tasks.
   */
  protected static enum QueuePosition {
    HEAD, TAIL;
  }

  /**
   * Returns the {@link SchedulingSemaphore} of the given {@link JobInput}, or <code>null</code> if not set, or throws
   * {@link AssertionException} if not of type {@link SchedulingSemaphore}.
   */
  protected static SchedulingSemaphore get(final JobInput input) {
    if (input.getSchedulingSemaphore() == null) {
      return null;
    }

    Assertions.assertTrue(input.getSchedulingSemaphore() instanceof SchedulingSemaphore, "Semaphore must be of type {} [semaphore={}]", SchedulingSemaphore.class.getName(), input.getSchedulingSemaphore().getClass().getName());
    return (SchedulingSemaphore) input.getSchedulingSemaphore();
  }

  /**
   * Callback to be notified once a permit is acquired.
   *
   * @since 5.2
   */
  public static interface IPermitAcquiredCallback {

    /**
     * Method invoked once a permit is acquired, and is invoked from the thread releasing a permit. Hence, the
     * implementor should execute any long running operation asynchronously in another thread. Also, the implementor is
     * responsible for releasing the permit to the next waiting task if not needed anymore.
     */
    void onPermitAcquired();
  }
}
