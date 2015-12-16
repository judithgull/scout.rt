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
package org.eclipse.scout.rt.platform.job;

import java.util.concurrent.Callable;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.job.filter.event.JobEventFilterBuilder;
import org.eclipse.scout.rt.platform.job.filter.future.FutureFilterBuilder;
import org.eclipse.scout.rt.platform.job.internal.SchedulingSemaphore;
import org.eclipse.scout.rt.platform.job.listener.JobEvent;
import org.eclipse.scout.rt.platform.util.concurrent.IRunnable;

/**
 * Helper class to schedule jobs that optionally run on behalf of a {@link RunContext}. This class is for convenience
 * purpose to facilitate the creation and scheduling of jobs.
 * <p/>
 * Example:
 *
 * <pre>
 * Jobs.schedule(new IRunnable() {
 *
 *   &#64;Override
 *   public void run() throws Exception {
 *     // do something
 *   }
 * }, Jobs.newInput()
 *     .withRunContext(RunContexts.copyCurrent()
 *         .withSubject(...)
 *         .withLocale(Locale.US))
 *     .withSchedulingDelay(10, TimeUnit.SECONDS)
 *     .withName("job example"));
 * </pre>
 *
 * The following code snippet illustrates how the job is finally run:
 *
 * <pre>
 * BEANS.get(IJobManager.class).schedule(new IRunnable() {
 *
 *   &#64;Override
 *   public void run() throws Exception {
 *     runContext.run(new IRunnable() {
 *
 *       &#064;Override
 *       public void run() throws Exception {
 *         // do some work
 *       }
 *     });
 *   }
 * });
 * </pre>
 *
 * @since 5.1
 * @see IJobManager
 * @see RunContext
 */
public final class Jobs {

  private Jobs() {
  }

  /**
   * Returns the job manager.
   */
  public static IJobManager getJobManager() {
    return BEANS.get(IJobManager.class);
  }

  /**
   * Runs the given {@link IRunnable} asynchronously in another thread at the next reasonable opportunity. The submitter
   * of the job continues to run in parallel. If the job belongs to a {@link ISchedulingSemaphore} and the maximal
   * concurrency level for that semaphore is reached, the job is queued until a permit becomes available.
   * <p>
   * The job manager will use the {@link JobInput} as given to control job execution.
   * <p>
   * The {@link IFuture} returned allows to wait for the job to complete or to cancel its execution. To immediately
   * block waiting for the job to complete, you can use constructions of the following form.
   * <p>
   * Usage:
   *
   * <pre>
   * Jobs.schedule(new IRunnable() {
   *
   *   &#64;Override
   *   public void run() throws Exception {
   *     // do something
   *   }
   * }, Jobs.newInput()
   *     .withRunContext(ClientRunContexts.copyCurrent())
   *     .withName("job name"));
   * </pre>
   *
   * @param runnable
   *          <code>IRunnable</code> to be executed.
   * @param input
   *          information about the job with execution instructions for the job manager to run the job.
   * @return Future to interact with the job like waiting for its completion or to cancel its execution.
   * @see IJobManager#schedule(IRunnable, JobInput)
   */
  public static IFuture<Void> schedule(final IRunnable runnable, final JobInput input) {
    return BEANS.get(IJobManager.class).schedule(runnable, input);
  }

  /**
   * Runs the given {@link Callable} asynchronously in another thread at the next reasonable opportunity. The submitter
   * of the job continues to run in parallel. If the job belongs to a {@link ISchedulingSemaphore} and the maximal
   * concurrency level for that semaphore is reached, the job is queued until a permit becomes available. Jobs in the
   * form of a {@link Callable} typically return a computation result to the submitter.
   * <p>
   * The job manager will use the {@link JobInput} as given to control job execution.
   * <p>
   * The {@link IFuture} returned allows to wait for the job to complete or to cancel its execution. To immediately
   * block waiting for the job to complete, you can use constructions of the following form.
   * <p>
   * Usage:
   *
   * <pre>
   * String result = Jobs.schedule(new Callable<String>() {
   *
   *   &#64;Override
   *   public String call() throws Exception {
   *     return "result";
   *   }
   * }, Jobs.newInput()
   *     .withRunContext(ClientRunContexts.copyCurrent())
   *     .withName("job name"))
   *     .awaitDoneAndGet();
   * </pre>
   *
   * @param callable
   *          <code>Callable</code> to be executed.
   * @param input
   *          information about the job with execution instructions for the job manager to run the job.
   * @return Future to interact with the job like waiting for its completion, or to cancel its execution, or to get its
   *         computation result.
   * @see IJobManager#schedule(Callable, JobInput)
   */
  public static <RESULT> IFuture<RESULT> schedule(final Callable<RESULT> callable, final JobInput input) {
    return BEANS.get(IJobManager.class).schedule(callable, input);
  }

  /**
   * Creates a new {@link JobInput}.
   * <p>
   * The job input returned can be associated with meta information about the job, with execution instructions to tell
   * the job manager how to run the job, and with a {@link RunContext} to be applied during the job's execution. The
   * input is to be given to the job manager alongside with the {@link IRunnable} or {@link Callable} to be executed.
   * <p>
   * Example:
   *
   * <pre>
   * Jobs.newInput()
   *     .withRunContext(RunContexts.copyCurrent())
   *     .withSchedulingDelay(10, TimeUnit.SECONDS)
   *     .withPeriodicExecutionAtFixedRate(5, TimeUnit.SECONDS)
   *     .withName("example job");
   * </pre>
   */
  public static JobInput newInput() {
    return BEANS.get(JobInput.class);
  }

  /**
   * Returns a builder to create a filter for {@link IFuture} objects. This builder facilitates the creation of a
   * {@link IFuture} filter and to match multiple criteria joined by logical 'AND' operation
   * <p>
   * Example usage:
   *
   * <pre>
   * Jobs.newFutureFilterBuilder()
   *     .andMatchFuture(...)
   *     .andMatch(...)
   *     .toFilter();
   * </pre>
   */
  public static FutureFilterBuilder newFutureFilterBuilder() {
    return BEANS.get(FutureFilterBuilder.class);
  }

  /**
   * Returns a builder to create a filter for {@link JobEvent} objects. This builder facilitates the creation of a
   * JobEvent filter and to match multiple criteria joined by logical 'AND' operation.
   * <p>
   * Example usage:
   *
   * <pre>
   * Jobs.newEventFilterBuilder()
   *     .andMatchEventType(JobEventType.SCHEDULED, JobEventType.DONE)
   *     .andMatchFuture(...)
   *     .andMatch(...)
   *     .toFilter();
   * </pre>
   */
  public static JobEventFilterBuilder newEventFilterBuilder() {
    return BEANS.get(JobEventFilterBuilder.class);
  }

  /**
   * Creates a scheduling semaphore to control the maximal number of jobs running concurrently among the same semaphore.
   * <p>
   * With a semaphore in place, this job only commences execution, once a permit is or gets available. Otherwise, the
   * job commences execution immediately at the next reasonable opportunity, unless no worker thread is available.
   *
   * @param permits
   *          the number of permits.
   */
  public static ISchedulingSemaphore newSchedulingSemaphore(final int permits) {
    return BEANS.get(SchedulingSemaphore.class).withPermits(permits);
  }

  /**
   * Creates a blocking condition to put a job into waiting mode until the condition falls.
   * <p>
   * If the job belongs to a {@link ISchedulingSemaphore}, the job's permit is released and passed to the next competing
   * job of that same semaphore while being blocked.
   * <p>
   * See {@link IBlockingCondition} for more information.
   *
   * @param blocking
   *          initial blocking-state of the blocking condition.
   */
  public static IBlockingCondition newBlockingCondition(final boolean blocking) {
    return BEANS.get(IJobManager.class).newBlockingCondition(blocking);
  }
}
