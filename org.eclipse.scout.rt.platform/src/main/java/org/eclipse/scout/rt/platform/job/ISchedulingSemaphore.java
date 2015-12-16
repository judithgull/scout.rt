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

import org.eclipse.scout.rt.platform.util.Assertions.AssertionException;

/**
 * Represents a fair counting semaphore used in Job API to control the maximal number of jobs running concurrently.
 * <p>
 * Jobs which belong to the same semaphore run concurrently until they reach the maximal concurrency level defined for
 * that semaphore. Subsequent tasks then wait in the queue until a permit becomes available.
 * <p>
 * A semaphore initialized to one allows to run jobs in a mutually exclusive manner, and a semaphore initialized to zero
 * to run no job. The number of total permits available can be changed even if the semaphore is already in use. By
 * default, this semaphore is unbounded.
 *
 * @since 5.2
 */
public interface ISchedulingSemaphore {

  /**
   * Returns whether the given task currently owns a permit.
   */
  boolean isPermitOwner(IFuture<?> task);

  /**
   * Returns the number of tasks currently competing for a permit. That are the tasks currently owning a permit, plus
   * all tasks waiting for a permit to become free.
   */
  int getCompetitorCount();

  /**
   * Sets the number of permits.
   * <p>
   * The number of permits can be changed, and even if this semaphore is already in use. That allows to set the maximal
   * concurrency level depending on some dynamic criteria like time of day or system load. However, once calling
   * {@link #seal()}, the number of permits cannot be changed anymore, and any attempts will result in an
   * {@link AssertionException}.
   *
   * @return <code>this</code> to support for method chaining.
   */
  ISchedulingSemaphore withPermits(int permits);

  /**
   * Seals this semaphore, so that the number of permits cannot be changed afterwards. Any attempt to change the number
   * of permits results in an {@link AssertionException}.
   */
  ISchedulingSemaphore seal();
}
