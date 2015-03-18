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
package org.eclipse.scout.rt.platform.job.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.ToStringBuilder;
import org.eclipse.scout.commons.annotations.Internal;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.job.JobExecutionException;
import org.eclipse.scout.rt.platform.job.internal.future.IFutureTask;
import org.eclipse.scout.rt.platform.job.internal.future.Job;
import org.eclipse.scout.rt.platform.job.internal.future.MutexAcquisitionFutureTask;

/**
 * Provides a thread-safe implementation of a non-blocking 1-permit-per-mutex semaphore backed with a fair queue. For
 * each mutex object, a separate {@link MutexSemaphore} is created to hold the mutex state.
 *
 * @since 5.1
 */
@Internal
public class MutexSemaphores {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(MutexSemaphores.class);

  private static final boolean POSITION_TAIL = true;
  private static final boolean POSITION_HEAD = false;

  private final ReadLock m_readLock;
  private final WriteLock m_writeLock;

  private final Map<Object, MutexSemaphore> m_mutexSemaphores;

  private final ExecutorService m_executor;

  public MutexSemaphores(final ExecutorService executor) {
    m_executor = executor;
    m_mutexSemaphores = new HashMap<>();

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    m_readLock = lock.readLock();
    m_writeLock = lock.writeLock();
  }

  /**
   * @return <code>true</code> if the given task is a mutex task and currently owns the mutex.
   */
  public boolean isMutexOwner(final IFutureTask<?> task) {
    m_readLock.lock();
    try {
      final MutexSemaphore mutexSemaphore = m_mutexSemaphores.get(task.getMutexObject());
      if (mutexSemaphore == null) {
        return false;
      }
      else {
        return mutexSemaphore.isMutexOwner(task);
      }
    }
    finally {
      m_readLock.unlock();
    }
  }

  /**
   * Tries to acquire the mutex for the task's mutex-object. If not available at the time of invocation, the task is put
   * into the queue of pending tasks and will compete for the mutex once all queued tasks of that mutex-object
   * acquired/released the mutex.
   *
   * @param task
   *          the task to acquire the mutex; must be a mutex task.
   * @return <code>true</code> if the mutex was acquired, <code>false</code> if being queued.
   */
  public boolean tryAcquireElseOfferTail(final IFutureTask<?> task) {
    return tryAcquireElseOffer(task, POSITION_TAIL);
  }

  private boolean tryAcquireElseOffer(final IFutureTask<?> task, final boolean position) {
    Assertions.assertTrue(task.isMutexTask(), "Task must be a mutex task [task=%s]", task);

    m_writeLock.lock();
    try {
      if (m_executor.isShutdown()) {
        task.cancel(true);
        return false;
      }

      return getMutexSemaphore(task.getMutexObject()).tryAcquireElseOffer(task, position);
    }
    finally {
      m_writeLock.unlock();
    }
  }

  /**
   * Blocks the calling thread until the given task acquired the task's mutex-object.
   *
   * @param task
   *          the task to acquire the mutex; must be a mutex task.
   * @throws JobExecutionException
   *           is thrown if the current thread is interrupted while waiting for the mutex to become available, or the
   *           executor is shutdown.
   */
  public void acquire(final IFutureTask<?> task) throws JobExecutionException {
    Assertions.assertTrue(task.isMutexTask(), "Task must be a mutex task [task=%s]", task);
    final Object mutexObject = task.getMutexObject();

    if (m_executor.isShutdown()) {
      task.cancel(true);
      throw new JobExecutionException(String.format("Failed to acquire mutex because job manager is shutdown [task=%s]", task));
    }

    // Create the task to re-acquire the mutex. This task is called if having to wait for the mutex to become available.
    final MutexAcquisitionFutureTask mutexAcquisitionTask = new MutexAcquisitionFutureTask(this, mutexObject) {

      @Override
      protected synchronized void mutexAcquired() {
        m_writeLock.lock();
        try {
          final MutexSemaphore mutexSemaphore = getMutexSemaphore(mutexObject);

          if (isAwaitMutex()) {
            mutexSemaphore.replaceMutexOwner(this, task);
            notify(); // notify blocked thread.
          }
          else {
            passMutexToNextTask(this);
          }
        }
        finally {
          m_writeLock.unlock();
        }
      }
    };

    if (tryAcquireElseOffer(mutexAcquisitionTask, POSITION_HEAD)) {
      getMutexSemaphore(mutexObject).replaceMutexOwner(mutexAcquisitionTask, task);
    }
    else {
      synchronized (mutexAcquisitionTask) {
        while (!task.isMutexOwner()) {
          try {
            mutexAcquisitionTask.wait();
          }
          catch (final InterruptedException e) {
            mutexAcquisitionTask.stopAwaitMutex();

            Thread.currentThread().interrupt(); // Restore the interrupted status because cleared by catching InterruptedException.
            throw new JobExecutionException(String.format("Interrupted while re-acquiring the mutex [%s]", task), e);
          }
        }
      }
    }
  }

  /**
   * Passes the taks's mutex to the first task in the queue.
   *
   * @param task
   *          task which currently is the mutex-owner.
   * @return task which is the new mutex-owner, <code>null</code> if the queue was empty.
   */
  public IFutureTask<?> releaseAndPoll(final IFutureTask<?> task) {
    Assertions.assertTrue(task.isMutexTask(), "Task must be a mutex task [task=%s]", task);
    final Object mutexObject = task.getMutexObject();

    m_writeLock.lock();
    try {
      if (m_executor.isShutdown()) {
        return null;
      }

      final MutexSemaphore mutexSemaphore = m_mutexSemaphores.get(mutexObject);
      if (mutexSemaphore == null) {
        LOG.error("Unexpected inconsistency while releasing mutex: mutex-semaphore must not be null.");
        return null;
      }

      if (!mutexSemaphore.isMutexOwner(task)) {
        LOG.error("Unexpected inconsistency while releasing  mutex: wrong mutex owner [expected=%s, actual=%s].", task, mutexSemaphore.getMutexOwner());
      }

      final IFutureTask<?> nextTask = mutexSemaphore.releaseAndPoll();
      if (nextTask == null) {
        m_mutexSemaphores.remove(mutexObject);
      }

      return nextTask;
    }
    finally {
      m_writeLock.unlock();
    }
  }

  /**
   * Use this method to pass the mutex to the next task in the queue. If applicable, the task is scheduled for
   * execution.
   */
  public void passMutexToNextTask(final IFutureTask<?> currentMutexOwner) {
    final IFutureTask<?> nextTask = releaseAndPoll(currentMutexOwner);
    if (nextTask != null) {
      m_executor.submit(Job.callable(nextTask.getJob()));
    }
  }

  /**
   * Returns the mutex-semaphore for the given mutex-object.
   */
  private MutexSemaphore getMutexSemaphore(final Object mutexObject) {
    m_writeLock.lock();
    try {
      MutexSemaphore mutexSemaphore = m_mutexSemaphores.get(mutexObject);
      if (mutexSemaphore == null) {
        mutexSemaphore = new MutexSemaphore();
        m_mutexSemaphores.put(mutexObject, mutexSemaphore);
      }
      return mutexSemaphore;
    }
    finally {
      m_writeLock.unlock();
    }
  }

  /**
   * Returns the current permit count for the given mutex-object. This is the mutex-owner plus all pending tasks; if
   * <code>0</code>, the mutex is not acquired.
   */
  public int getPermitCount(final Object mutexObject) {
    Assertions.assertNotNull(mutexObject, "Mutex object must not be null");

    m_readLock.lock();
    try {
      final MutexSemaphore mutexSemaphore = m_mutexSemaphores.get(mutexObject);
      return (mutexSemaphore != null ? mutexSemaphore.getPermitCount() : 0);
    }
    finally {
      m_readLock.unlock();
    }
  }

  /**
   * Clears all queued tasks and mutex-owners.
   */
  void clear() {
    m_writeLock.lock();
    try {
      for (final MutexSemaphore mutexSemaphore : m_mutexSemaphores.values()) {
        mutexSemaphore.clear();
      }
      m_mutexSemaphores.clear();
    }
    finally {
      m_writeLock.unlock();
    }
  }

  // === Mutex-Semaphore per mutex object ===

  private static class MutexSemaphore {

    private final Deque<IFutureTask<?>> m_pendingQueue;

    private int m_permits;
    private IFutureTask<?> m_mutexOwner;

    private MutexSemaphore() {
      m_permits = 0;
      m_pendingQueue = new ArrayDeque<>();
    }

    private int getPermitCount() {
      return m_permits;
    }

    private boolean isMutexOwner(final IFutureTask<?> task) {
      return m_mutexOwner == task;
    }

    private IFutureTask<?> getMutexOwner() {
      return m_mutexOwner;
    }

    private boolean tryAcquireElseOffer(final IFutureTask<?> task, final boolean tail) {
      if (m_permits++ == 0) {
        m_mutexOwner = task;
        return true;
      }
      else {
        if (tail) {
          m_pendingQueue.offerLast(task);
        }
        else {
          m_pendingQueue.offerFirst(task);
        }
        return false;
      }
    }

    public void replaceMutexOwner(final IFutureTask<?> currentMutexOwner, final IFutureTask<?> newMutexOwner) {
      if (!isMutexOwner(currentMutexOwner)) {
        LOG.error("Unexpected inconsistency: current task must be mutex-owner [currentTask={}, newMutexOwner={}, currentMutexOwner={}]", new Object[]{currentMutexOwner, newMutexOwner, getMutexOwner()});
      }
      m_mutexOwner = newMutexOwner;
    }

    private IFutureTask<?> releaseAndPoll() {
      m_mutexOwner = m_pendingQueue.poll();

      m_permits--;

      if (m_permits < 0) {
        LOG.error("Unexpected inconsistency while releasing mutex: permit count must not be '0'.", m_permits);
        m_permits = 0;
      }

      return m_mutexOwner;
    }

    private void clear() {
      m_permits = 0;
      m_pendingQueue.clear();
      m_mutexOwner = null;
    }

    @Override
    public String toString() {
      final ToStringBuilder builder = new ToStringBuilder(this);
      builder.attr("mutexOwner", m_mutexOwner);
      builder.attr("permits", m_permits);
      builder.attr("pendingQueue", m_pendingQueue);
      return builder.toString();
    }
  }
}