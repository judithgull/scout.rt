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
package org.eclipse.scout.rt.platform.context;

import org.eclipse.scout.rt.platform.job.IFuture;

/**
 * This is any kind of object interested in active cancellation of a {@link IRunMonitor#cancel(boolean)}.
 * <p>
 * Note that a {@link IFuture} represents itself a {@link IRunMonitor}
 *
 * @since 5.1
 */
public interface ICancellable {

  /**
   * Attempts to cancel the execution of the associated run phase (maybe inside a job). This attempt will be ignored if
   * the job has already completed or was cancelled. If not running yet, the job will never run. If the job has already
   * started, then the <code>interruptIfRunning</code> parameter determines whether the thread executing this job should
   * be interrupted in an attempt to stop the job.
   *
   * @param interruptIfRunning
   *          <code>true</code> if the thread executing this run phase (maybe inside a job) should be interrupted;
   *          otherwise, in-progress jobs are allowed to complete.
   * @return <code>false</code> if the job could not be cancelled, typically because it has already completed normally.
   */
  boolean cancel(boolean interruptIfRunning);

  /**
   * @return <code>true</code> if the associated run phase (maybe inside a job) was cancelled before it completed
   *         normally.
   */
  boolean isCancelled();
}