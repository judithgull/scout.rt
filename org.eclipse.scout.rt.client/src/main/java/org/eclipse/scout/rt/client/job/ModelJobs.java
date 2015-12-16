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
package org.eclipse.scout.rt.client.job;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContext;
import org.eclipse.scout.rt.client.job.filter.event.ModelJobEventFilter;
import org.eclipse.scout.rt.client.job.filter.future.ModelJobFutureFilter;
import org.eclipse.scout.rt.client.session.ClientSessionProvider;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.filter.IFilter;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.IJobManager;
import org.eclipse.scout.rt.platform.job.ISchedulingSemaphore;
import org.eclipse.scout.rt.platform.job.JobInput;
import org.eclipse.scout.rt.platform.job.filter.event.JobEventFilterBuilder;
import org.eclipse.scout.rt.platform.job.filter.future.FutureFilterBuilder;
import org.eclipse.scout.rt.platform.job.listener.JobEvent;
import org.eclipse.scout.rt.platform.util.Assertions;
import org.eclipse.scout.rt.platform.util.concurrent.IRunnable;

/**
 * Helper class to schedule jobs that run on behalf of a {@link ClientRunContext} and are executed in sequence among the
 * same session. Such jobs are called model jobs with the characteristics that only one model job is active at any given
 * time for the same session. Model jobs are used to interact with the client model to read and write model values. This
 * class is for convenience purpose to facilitate the creation and scheduling of model jobs.
 * <p>
 * <strong>By definition, a <em>ModelJob</em> requires a {@link ClientRunContext} with a {@link IClientSession} and the
 * session's {@link ISchedulingSemaphore}. That causes all jobs within that session to be run in sequence in the model
 * thread. At any given time, there is only one model thread per client session.</strong>
 * <p>
 * Example:
 *
 * <pre>
 * ModeJobs.schedule(new IRunnable() {
 *
 *   &#64;Override
 *   public void run() throws Exception {
 *     // do something
 *   }
 * }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
 *     .withSchedulingDelay(2, TimeUnit.SECONDS)
 *     .withName("job example"));
 * </pre>
 *
 * The following code snippet illustrates how the job is finally run:
 *
 * <pre>
 * BEANS.get(IJobManager.class).schedule(new IRunnable() {
 *
 *     &#064;Override
 *     public void run() throws Exception {
 *       clientRunContext.run(new IRunnable() {
 *
 *         &#064;Override
 *         public void run() throws Exception {
 *           // do some work
 *         }
 *       });
 *     }
 *   }, BEANS.get(JobInput.class)
 *        .withRunContext(clientRunContext)
 *        .<strong>withSchedulingSemaphore(clientRunContext.getSession().getModelJobSchedulingSemaphore())</strong>);
 * </pre>
 *
 * @since 5.1
 * @see IClientSession#getModelJobSchedulingSemaphore()
 */
public final class ModelJobs {

  /**
   * Execution hint to signal that a model job requires interaction from UI, typically by a user like closing a message
   * box. This hint is usually set just before a blocking condition is entered ("waitFor()"). Threads that are waiting
   * for the model job to be completed can then return to the UI before the job is actually done (which would never
   * happen without the user interaction).
   * <p>
   * <b>Usage</b>
   * <p>
   * <i>Code that blocks, but requires user interaction to release the lock:</i><br>
   *
   * <pre>
   * private void waitFor() {
   *   m_blockingCondition.waitFor(ModelJobs.EXECUTION_HINT_UI_INTERACTION_REQUIRED);
   * }
   * </pre>
   *
   * <i>Code that waits for the model job, but should return to the UI when user interaction is required:</i><br>
   *
   * <pre>
   *   ...
   *   Jobs.getJobManager().awaitDone(ModelJobs.newFutureFilterBuilder()
   *      .andMatch(...) // any other conditions
   *      .andMatchNotExecutionHint(ModelJobs.EXECUTION_HINT_UI_INTERACTION_REQUIRED)
   *      .toFilter(), AWAIT_TIMEOUT, TimeUnit.MILLISECONDS)
   *   ...
   * </pre>
   *
   * @see {@link IJobManager#awaitDone(IFilter, long, TimeUnit)}
   */
  public static final String EXECUTION_HINT_UI_INTERACTION_REQUIRED = "ui.interaction.required";

  private ModelJobs() {
  }

  /**
   * Runs the given {@link IRunnable} asynchronously in the model thread once acquired the model permit. The submitter
   * of the job continues to run in parallel.
   * <p>
   * <strong>Do not wait for this job to complete if being a model job yourself as this would cause a deadlock.</strong>
   * <p>
   * The job manager will use the {@link JobInput} as given to control job execution.
   * <p>
   * The {@link IFuture} returned allows to wait for the job to complete or to cancel its execution.
   * <p>
   * Usage:
   *
   * <pre>
   * ModelJobs.schedule(new IRunnable() {
   *
   *   &#64;Override
   *   public void run() throws Exception {
   *     // do something
   *   }
   * }, ModelJobs.newInput(ClientRunContexts.copyCurrent()));
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
    return BEANS.get(IJobManager.class).schedule(runnable, ModelJobs.validateInput(input));
  }

  /**
   * Runs the given {@link Callable} asynchronously in the model thread once acquired the model permit. The submitter of
   * the job continues to run in parallel. Jobs in the form of a {@link Callable} typically return a computation result
   * to the submitter.
   * <p>
   * <strong>Do not wait for this job to complete if being a model job yourself as this would cause a deadlock.</strong>
   * <p>
   * * The job manager will use the {@link JobInput} as given to control job execution.
   * <p>
   * The {@link IFuture} returned allows to wait for the job to complete or to cancel its execution. To immediately
   * block waiting for the job to complete, you can use constructions of the following form.
   * <p>
   * Usage:
   *
   * <pre>
   * String result = ModelJobs.schedule(new Callable<String>() {
   *
   *   &#64;Override
   *   public String call() throws Exception {
   *     return "result";
   *   }
   * }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
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
    return BEANS.get(IJobManager.class).schedule(callable, ModelJobs.validateInput(input));
  }

  /**
   * Creates a {@link JobInput} specific for model jobs initialized with the given {@link ClientRunContext}.
   * <p>
   * The job input returned can be associated with meta information about the job and with execution instructions to
   * tell the job manager how to run the job. The input is to be given to the job manager alongside with the
   * {@link IRunnable} or {@link Callable} to be executed.
   * <p>
   * Example:
   *
   * <pre>
   * Jobs.newInput(RunContexts.copyCurrent())
   *     .withSchedulingDelay(10, TimeUnit.SECONDS)
   *     .withName("example job");
   * </pre>
   *
   * @param clientRunContext
   *          The {@link ClientRunContext} to be associated with the {@link JobInput} returned; must not be
   *          <code>null</code>.
   */
  public static JobInput newInput(final ClientRunContext clientRunContext) {
    Assertions.assertNotNull(clientRunContext, "ClientRunContext required for model jobs");
    Assertions.assertNotNull(clientRunContext.getSession(), "ClientSession required for model jobs");
    Assertions.assertNotNull(clientRunContext.getSession().getModelJobSchedulingSemaphore(), "SchedulingSemaphore required for model jobs");
    return BEANS.get(JobInput.class)
        .withThreadName("scout-model-thread")
        .withRunContext(clientRunContext)
        .withSchedulingSemaphore(clientRunContext.getSession().getModelJobSchedulingSemaphore());
  }

  /**
   * Returns a builder to create a filter for {@link IFuture} objects representing a model job. This builder facilitates
   * the creation of a {@link IFuture} filter and to match multiple criteria joined by logical 'AND' operation
   * <p>
   * Example usage:
   *
   * <pre>
   * Jobs.newFutureFilterBuilder()
   *     .andMatch(new SessionFutureFilter(ISession.CURRENT.get()))
   *     .andMatch(...)
   *     .toFilter();
   * </pre>
   */
  public static FutureFilterBuilder newFutureFilterBuilder() {
    return BEANS.get(FutureFilterBuilder.class).andMatch(ModelJobFutureFilter.INSTANCE);
  }

  /**
   * Returns a builder to create a filter for {@link JobEvent} objects originating from model jobs. This builder
   * facilitates the creation of a JobEvent filter and to match multiple criteria joined by logical 'AND' operation.
   * <p>
   * Example usage:
   *
   * <pre>
   * Jobs.newEventFilterBuilder()
   *     .andMatchEventType(JobEventType.SCHEDULED, JobEventType.DONE)
   *     .andMatch(...)
   *     .toFilter();
   * </pre>
   */
  public static JobEventFilterBuilder newEventFilterBuilder() {
    return BEANS.get(JobEventFilterBuilder.class).andMatch(ModelJobEventFilter.INSTANCE);
  }

  /**
   * Returns <code>true</code> if the current thread represents the model thread for the current client session. At any
   * given time, there is only one model thread per client session.
   */
  public static boolean isModelThread() {
    return ModelJobs.isModelThread(ClientSessionProvider.currentSession());
  }

  /**
   * Returns <code>true</code> if the current thread represents the model thread for the given client session. At any
   * given time, there is only one model thread per client session.
   */
  public static boolean isModelThread(final IClientSession clientSession) {
    final IFuture<?> currentFuture = IFuture.CURRENT.get();
    if (!ModelJobs.isModelJob(currentFuture)) {
      return false;
    }

    final ISchedulingSemaphore semaphore = currentFuture.getSchedulingSemaphore();
    return semaphore != null && semaphore.isPermitOwner(currentFuture);
  }

  /**
   * Returns <code>true</code> if the given Future belongs to a model job.
   */
  public static boolean isModelJob(final IFuture<?> future) {
    if (future == null) {
      return false;
    }

    if (future.getJobInput().getSchedulingSemaphore() == null) {
      return false;
    }

    if (!(future.getJobInput().getRunContext() instanceof ClientRunContext)) {
      return false;
    }

    final IClientSession clientSession = ((ClientRunContext) future.getJobInput().getRunContext()).getSession();
    if (clientSession == null) {
      return false;
    }

    return future.getJobInput().getSchedulingSemaphore() == clientSession.getModelJobSchedulingSemaphore();
  }

  /**
   * Validates the given {@link JobInput} to be a valid input for a model job.
   */
  private static JobInput validateInput(final JobInput input) {
    BEANS.get(ModelJobInputValidator.class).validate(input);
    return input;
  }
}
