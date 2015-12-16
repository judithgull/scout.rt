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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.Bean;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.context.RunMonitor;
import org.eclipse.scout.rt.platform.exception.ExceptionHandler;
import org.eclipse.scout.rt.platform.util.Assertions;
import org.eclipse.scout.rt.platform.util.ToStringBuilder;
import org.slf4j.helpers.MessageFormatter;

/**
 * A <code>JobInput</code> contains information about a job like its name with execution instructions for the job
 * manager to run the job.
 * <p>
 * The 'setter-methods' return <code>this</code> in order to support for method chaining.
 *
 * @see RunContext
 * @since 5.1
 */
@Bean
public class JobInput {

  /**
   * Indicates to execute a job exactly one time.
   */
  public static final int EXECUTION_MODE_SINGLE = 1 << 0;
  /**
   * Indicates to execute a job periodically with a fixed delay.
   */
  public static final int EXECUTION_MODE_PERIODIC_WITH_FIXED_DELAY = 1 << 1;
  /**
   * Indicates to execute a job periodically at a fixed rate.
   */
  public static final int EXECUTION_MODE_PERIODIC_AT_FIXED_RATE = 1 << 2;
  /**
   * Indicates that an executable always should commence execution regardless of how long it was waiting for its
   * execution to start.
   */
  public static final long INFINITE_EXPIRATION = 0;

  protected String m_name;
  protected ISchedulingSemaphore m_schedulingSemaphore;
  protected long m_expirationTime = INFINITE_EXPIRATION;
  protected String m_threadName = "scout-thread";
  protected RunContext m_runContext;
  protected long m_schedulingDelay;
  protected long m_periodicDelay;
  protected int m_executionMode = EXECUTION_MODE_SINGLE;

  protected Class<? extends ExceptionHandler> m_exceptionHandler = ExceptionHandler.class;
  protected boolean m_swallowException = false;

  protected Set<String> m_executionHints = new HashSet<>();

  public String getName() {
    return m_name;
  }

  /**
   * Instruments the job manager to delay the execution of the job until the delay elapsed. For periodic jobs, this is
   * the initial delay to start with the periodic execution.
   *
   * @param delay
   *          the delay to delay the execution.
   * @param unit
   *          the time unit of the <code>period</code> argument.
   */
  public JobInput withSchedulingDelay(final long delay, final TimeUnit unit) {
    m_schedulingDelay = unit.toMillis(delay);
    return this;
  }

  /**
   * Returns the scheduling delay [millis] to indicate, that the job should commence execution only after the delay
   * elapsed.
   */
  public long getSchedulingDelay() {
    return m_schedulingDelay;
  }

  /**
   * A periodic delay is only set for periodic jobs. That are jobs with an execution mode
   * {@link #EXECUTION_MODE_PERIODIC_WITH_FIXED_DELAY} or {@link #EXECUTION_MODE_PERIODIC_AT_FIXED_RATE}.
   * <p>
   * Returns the rate for 'at-fixed-rate' jobs, or the delay for 'with-fixed-delay' jobs. The delay is given in
   * milliseconds, and is ignored for one-time executing jobs. The delay is used by the job manager to reschedule a
   * periodic job.
   */
  public long getPeriodicDelay() {
    return m_periodicDelay;
  }

  /**
   * Instruments the job manager to run the job periodically at a fixed rate, until being cancelled, or the job throws
   * an exception, or the job manager is shutdown. Also, periodic jobs do not return a result to the caller.
   * <p>
   * The term 'at fixed rate' means, that the job is run consequently at that rate. The first execution starts
   * immediately, unless configured to run with an initial delay as set via {@link #withSchedulingDelay(long, TimeUnit)}
   * . The second execution is after 'initialDelay' plus one period, the third execution after 'initialDelay' plus 2
   * periods, and so on.
   * <p>
   * If an execution 'A' takes longer than the <code>period</code>, the subsequent execution 'B' is delayed and starts
   * immediately after execution 'A' completes. In such a case, all subsequent executions are shifted by the delay of
   * execution 'A'. In other words, the clock to trigger subsequent executions is reset to the start time of execution
   * 'B'.
   *
   * @param period
   *          the period between successive runs.
   * @param unit
   *          the time unit of the <code>period</code> argument.
   */
  public JobInput withPeriodicExecutionAtFixedRate(final long period, final TimeUnit unit) {
    Assertions.assertTrue(m_executionMode == EXECUTION_MODE_SINGLE || m_executionMode == EXECUTION_MODE_PERIODIC_AT_FIXED_RATE, "Periodic execution mode already set");
    m_executionMode = EXECUTION_MODE_PERIODIC_AT_FIXED_RATE;
    m_periodicDelay = unit.toMillis(period);
    return this;
  }

  /**
   * Instruments the job manager to run the job periodically with a fixed delay, until being cancelled, or the job
   * throws an exception, or the job manager is shutdown. Also, periodic jobs do not return a result to the caller.
   * <p>
   * The term 'with fixed delay' means, that there is a fixed delay between the termination of one execution and the
   * commencement of the next.
   *
   * @param period
   *          the delay between successive runs.
   * @param unit
   *          the time unit of the <code>delay</code> argument.
   */
  public JobInput withPeriodicExecutionWithFixedDelay(final long delay, final TimeUnit unit) {
    Assertions.assertTrue(m_executionMode == EXECUTION_MODE_SINGLE || m_executionMode == EXECUTION_MODE_PERIODIC_WITH_FIXED_DELAY, "Periodic execution mode already set");
    m_executionMode = EXECUTION_MODE_PERIODIC_WITH_FIXED_DELAY;
    m_periodicDelay = unit.toMillis(delay);
    return this;
  }

  /**
   * Sets the name of the job, which is used to name the worker thread and for logging purpose.
   * <p>
   * Optionally, <em>formatting anchors</em> in the form of {} pairs can be used in the name, which will be replaced by
   * the respective argument.
   *
   * @param name
   *          the name with support for <em>formatting anchors</em> in the form of {} pairs.
   * @param valueArgs
   *          optional arguments to substitute <em>formatting anchors</em> in the name.
   */
  public JobInput withName(final String name, final Object... args) {
    m_name = MessageFormatter.arrayFormat(name, args).getMessage();
    return this;
  }

  /**
   * Returns the scheduling semaphore which this job belongs to, or <code>null</code> if there is no maximal concurrency
   * restriction for this job.
   * <p>
   * With a semaphore in place, this job only commences execution, once a permit is or gets available. Otherwise, the
   * job commences execution immediately at the next reasonable opportunity, unless no worker thread is available.
   */
  public ISchedulingSemaphore getSchedulingSemaphore() {
    return m_schedulingSemaphore;
  }

  /**
   * Sets the scheduling semaphore to control the maximal number of jobs running concurrently among the same semaphore.
   * <p>
   * With a semaphore in place, this job only commences execution once a permit is or gets available. Otherwise, the job
   * commences execution immediately at the next reasonable opportunity, unless no worker thread is available.
   *
   * @see Jobs#newSchedulingSemaphore()
   */
  public JobInput withSchedulingSemaphore(final ISchedulingSemaphore schedulingSemaphore) {
    m_schedulingSemaphore = schedulingSemaphore;
    return this;
  }

  public long getExpirationTimeMillis() {
    return m_expirationTime;
  }

  /**
   * Sets the maximal expiration time, until the job must commence execution. If elapsed, the executable is cancelled
   * and never commence execution. This is useful when using a scheduling strategy which might queue scheduled
   * executables prior execution. By default, there is no expiration time set.
   *
   * @param time
   *          the maximal expiration time until an executable must commence execution.
   * @param timeUnit
   *          the time unit of the <code>time</code> argument.
   */
  public JobInput withExpirationTime(final long time, final TimeUnit timeUnit) {
    m_expirationTime = timeUnit.toMillis(time);
    return this;
  }

  public RunContext getRunContext() {
    return m_runContext;
  }

  /**
   * Sets the {@link RunContext} to be installed during job execution. Also, the context's {@link RunMonitor} is
   * associated with the jobs's {@link IFuture}, meaning that cancellation requests to the {@link IFuture} or
   * {@link RunContext} are equivalent. However, if no context is provided, the job manager ensures a {@link RunMonitor}
   * to be installed, so that executing code can always query the cancellation status by
   * <code>RunMonitor.CURRENT.get().isCancelled()</code> .
   */
  public JobInput withRunContext(final RunContext runContext) {
    m_runContext = runContext;
    return this;
  }

  public Class<? extends ExceptionHandler> getExceptionHandler() {
    return m_exceptionHandler;
  }

  public boolean isSwallowException() {
    return m_swallowException;
  }

  /**
   * Controls the handling of uncaught exceptions.
   * <p>
   * By default, an uncaught exception is handled by {@link ExceptionHandler} bean and then propagated to the submitter,
   * unless the submitter is not waiting for the job to complete via {@link IFuture#awaitDoneAndGet()}.
   * <p>
   * If running a periodic job with <code>swallowException=true</code>, the job will continue periodic execution upon an
   * uncaught exception. If set to <code>false</code>, the execution would exit.
   *
   * @param exceptionHandler
   *          optional handler to handle an uncaught exception, or <code>null</code> to not handle the exception. By
   *          default, {@link ExceptionHandler} bean is used.
   * @param swallowException
   *          <code>true</code> to swallow an uncaught exception, meaning that the exception is not propagated to the
   *          submitter. By default, exceptions are not swallowed and propagated to the submitter.
   */
  public JobInput withExceptionHandling(final Class<? extends ExceptionHandler> exceptionHandler, final boolean swallowException) {
    m_exceptionHandler = exceptionHandler;
    m_swallowException = swallowException;
    return this;
  }

  public String getThreadName() {
    return m_threadName;
  }

  /**
   * Sets the thread name of the worker thread that will execute the job.
   */
  public JobInput withThreadName(final String threadName) {
    m_threadName = threadName;
    return this;
  }

  public Set<String> getExecutionHints() {
    return m_executionHints;
  }

  /**
   * Associates the job with an execution hint, which can be evaluated by filters like when listening to job lifecycle
   * events, or when waiting for job completion, or when cancelling jobs, or by the job manager.
   * <p>
   * A job may have multiple hints associated, and hints are not propagated to nested jobs.
   */
  public JobInput withExecutionHint(final String hint) {
    m_executionHints.add(hint);
    return this;
  }

  /**
   * Conditionally associates the job with an execution hint. Unlike {@link #withExecutionHint(String)}, this method
   * sets the hint only if the condition is <code>true</code>, and is used to ease fluent usage of {@link JobInput}.
   *
   * @param hint
   *          the execution hint to be set.
   * @param setExecutionHint
   *          <code>true</code> to set the execution hint, or <code>false</code> otherwise.
   * @see #withExecutionHint(String)
   */
  public JobInput withExecutionHint(final String hint, final boolean setExecutionHint) {
    if (setExecutionHint) {
      m_executionHints.add(hint);
    }
    return this;
  }

  /**
   * Returns the execution mode of the job, and is one of {@link #EXECUTION_MODE_SINGLE}, or
   * {@link #EXECUTION_MODE_PERIODIC_AT_FIXED_RATE}, or {@link #EXECUTION_MODE_PERIODIC_WITH_FIXED_DELAY}.
   */
  public int getExecutionMode() {
    return m_executionMode;
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.attr("name", m_name);
    builder.ref("schedulingSemaphore", m_schedulingSemaphore);
    builder.attr("expirationTime", m_expirationTime);
    builder.attr("exceptionHandler", m_exceptionHandler);
    builder.attr("swallowException", m_swallowException);
    builder.attr("threadName", m_threadName);
    builder.attr("executionMode", m_executionMode);
    builder.attr("schedulingDelay", m_schedulingDelay);
    builder.attr("periodicDelay", m_periodicDelay);
    builder.attr("runContext", m_runContext);
    builder.attr("executionHints", m_executionHints);

    return builder.toString();
  }

  /**
   * Creates a copy of <code>this</code> input.
   */
  public JobInput copy() {
    final JobInput copy = BEANS.get(JobInput.class);
    copy.m_name = m_name;
    copy.m_schedulingSemaphore = m_schedulingSemaphore;
    copy.m_expirationTime = m_expirationTime;
    copy.m_exceptionHandler = m_exceptionHandler;
    copy.m_swallowException = m_swallowException;
    copy.m_threadName = m_threadName;
    copy.m_runContext = (m_runContext != null ? m_runContext.copy() : null);
    copy.m_schedulingDelay = m_schedulingDelay;
    copy.m_periodicDelay = m_periodicDelay;
    copy.m_executionMode = m_executionMode;
    copy.m_executionHints = new HashSet<>(m_executionHints);

    return copy;
  }
}
