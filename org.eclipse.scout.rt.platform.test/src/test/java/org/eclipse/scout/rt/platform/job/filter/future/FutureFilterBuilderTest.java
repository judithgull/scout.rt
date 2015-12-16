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
package org.eclipse.scout.rt.platform.job.filter.future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.filter.IFilter;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.ISchedulingSemaphore;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.util.concurrent.IRunnable;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class FutureFilterBuilderTest {

  private static final String JOB_IDENTIFIER = UUID.randomUUID().toString();

  @After
  public void after() {
    Jobs.getJobManager().cancel(Jobs.newFutureFilterBuilder()
        .andMatchExecutionHint(JOB_IDENTIFIER)
        .toFilter(), true);
  }

  @Test
  public void test() {
    ISchedulingSemaphore mutex1 = Jobs.newSchedulingSemaphore(1);
    ISchedulingSemaphore mutex2 = Jobs.newSchedulingSemaphore(1);

    IFuture<?> future1 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("A")
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future2 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("B")
        .withRunContext(RunContexts.empty())
        .withSchedulingSemaphore(mutex1)
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future3 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("C")
        .withRunContext(new P_RunContext())
        .withSchedulingSemaphore(mutex1)
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future4 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("D")
        .withPeriodicExecutionAtFixedRate(1, TimeUnit.SECONDS)
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future5 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("E")
        .withPeriodicExecutionAtFixedRate(1, TimeUnit.SECONDS)
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future6 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("E")
        .withRunContext(new P_RunContext())
        .withPeriodicExecutionAtFixedRate(1, TimeUnit.SECONDS)
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future7 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("F")
        .withSchedulingSemaphore(mutex1)
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future8 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("G")
        .withSchedulingSemaphore(mutex1)
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future9 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("H")
        .withSchedulingSemaphore(mutex2)
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future10 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("I")
        .withRunContext(new P_RunContext())
        .withSchedulingSemaphore(mutex1)
        .withExecutionHint(JOB_IDENTIFIER));

    assertTrue(new FutureFilterBuilder().toFilter().accept(future1));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future2));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future3));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future4));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future5));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future6));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future7));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future8));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future9));
    assertTrue(new FutureFilterBuilder().toFilter().accept(future10));

    // with filtering for futures
    IFilter<IFuture<?>> filter = new FutureFilterBuilder()
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .toFilter();
    assertTrue(filter.accept(future1));
    assertTrue(filter.accept(future2));
    assertTrue(filter.accept(future3));
    assertTrue(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertFalse(filter.accept(future6));
    assertFalse(filter.accept(future7));
    assertTrue(filter.accept(future8));
    assertTrue(filter.accept(future9));
    assertTrue(filter.accept(future10));

    // additionally with filtering for single executing jobs
    filter = new FutureFilterBuilder()
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .toFilter();
    assertTrue(filter.accept(future1));
    assertTrue(filter.accept(future2));
    assertTrue(filter.accept(future3));
    assertFalse(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertFalse(filter.accept(future6));
    assertFalse(filter.accept(future7));
    assertTrue(filter.accept(future8));
    assertTrue(filter.accept(future9));
    assertTrue(filter.accept(future10));

    // additionally with filtering for mutex
    filter = new FutureFilterBuilder()
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchSchedulingSemaphore(mutex1)
        .toFilter();
    assertFalse(filter.accept(future1));
    assertTrue(filter.accept(future2));
    assertTrue(filter.accept(future3));
    assertFalse(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertFalse(filter.accept(future6));
    assertFalse(filter.accept(future7));
    assertTrue(filter.accept(future8));
    assertFalse(filter.accept(future9));
    assertTrue(filter.accept(future10));

    // additionally with filtering for jobs running on behalf of a RunContext
    filter = new FutureFilterBuilder()
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchSchedulingSemaphore(mutex1)
        .andMatchRunContext(RunContext.class)
        .toFilter();
    assertFalse(filter.accept(future1));
    assertTrue(filter.accept(future2));
    assertTrue(filter.accept(future3));
    assertFalse(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertFalse(filter.accept(future6));
    assertFalse(filter.accept(future7));
    assertFalse(filter.accept(future8));
    assertFalse(filter.accept(future9));
    assertTrue(filter.accept(future10));

    // additionally with filtering for jobs running on behalf of a specific P_RunContext
    filter = new FutureFilterBuilder()
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchSchedulingSemaphore(mutex1)
        .andMatchRunContext(P_RunContext.class)
        .toFilter();
    assertFalse(filter.accept(future1));
    assertFalse(filter.accept(future2));
    assertTrue(filter.accept(future3));
    assertFalse(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertFalse(filter.accept(future6));
    assertFalse(filter.accept(future7));
    assertFalse(filter.accept(future8));
    assertFalse(filter.accept(future9));
    assertTrue(filter.accept(future10));

    // additionally with filtering for names
    filter = new FutureFilterBuilder()
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchSchedulingSemaphore(mutex1)
        .andMatchRunContext(P_RunContext.class)
        .andMatchName("A", "B", "C")
        .toFilter();
    assertFalse(filter.accept(future1));
    assertFalse(filter.accept(future2));
    assertTrue(filter.accept(future3));
    assertFalse(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertFalse(filter.accept(future6));
    assertFalse(filter.accept(future7));
    assertFalse(filter.accept(future8));
    assertFalse(filter.accept(future9));
    assertFalse(filter.accept(future10));

    // additionally with filtering for other names
    filter = new FutureFilterBuilder()
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchSchedulingSemaphore(mutex1)
        .andMatchRunContext(P_RunContext.class)
        .andMatchName("D", "E", "F")
        .toFilter();
    assertFalse(filter.accept(future1));
    assertFalse(filter.accept(future2));
    assertFalse(filter.accept(future3));
    assertFalse(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertFalse(filter.accept(future6));
    assertFalse(filter.accept(future7));
    assertFalse(filter.accept(future8));
    assertFalse(filter.accept(future9));
    assertFalse(filter.accept(future10));
  }

  @Test
  public void testFutureExclusion() {
    ISchedulingSemaphore mutex = Jobs.newSchedulingSemaphore(1);

    IFuture<?> future1 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future2 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future3 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future4 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future5 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER));

    IFuture<?> future6 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER)
        .withSchedulingSemaphore(mutex));

    IFuture<?> future7 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER)
        .withSchedulingSemaphore(mutex));

    IFuture<?> future8 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER)
        .withSchedulingSemaphore(mutex));

    IFuture<?> future9 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER)
        .withSchedulingSemaphore(mutex));

    IFuture<?> future10 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withExecutionHint(JOB_IDENTIFIER)
        .withSchedulingSemaphore(mutex));

    // One future exclusion with not other criteria
    IFilter<IFuture<?>> filter = Jobs.newFutureFilterBuilder()
        .andMatchNotFuture(future8).toFilter();
    assertTrue(filter.accept(future1));
    assertTrue(filter.accept(future2));
    assertTrue(filter.accept(future3));
    assertTrue(filter.accept(future4));
    assertTrue(filter.accept(future5));
    assertTrue(filter.accept(future6));
    assertTrue(filter.accept(future7));
    assertFalse(filter.accept(future8));
    assertTrue(filter.accept(future9));
    assertTrue(filter.accept(future10));

    // Multiple future exclusions with not other criteria
    filter = Jobs.newFutureFilterBuilder()
        .andMatchNotFuture(future8, future9).toFilter();
    assertTrue(filter.accept(future1));
    assertTrue(filter.accept(future2));
    assertTrue(filter.accept(future3));
    assertTrue(filter.accept(future4));
    assertTrue(filter.accept(future5));
    assertTrue(filter.accept(future6));
    assertTrue(filter.accept(future7));
    assertFalse(filter.accept(future8));
    assertFalse(filter.accept(future9));
    assertTrue(filter.accept(future10));

    // One future exclusion with other criterion (mutex)
    filter = Jobs.newFutureFilterBuilder()
        .andMatchSchedulingSemaphore(mutex)
        .andMatchNotFuture(future8).toFilter();
    assertFalse(filter.accept(future1));
    assertFalse(filter.accept(future2));
    assertFalse(filter.accept(future3));
    assertFalse(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertTrue(filter.accept(future6));
    assertTrue(filter.accept(future7));
    assertFalse(filter.accept(future8));
    assertTrue(filter.accept(future9));
    assertTrue(filter.accept(future10));

    // Multiple future exclusion with other criterion (mutex)
    filter = Jobs.newFutureFilterBuilder()
        .andMatchSchedulingSemaphore(mutex)
        .andMatchNotFuture(future8, future9).toFilter();
    assertFalse(filter.accept(future1));
    assertFalse(filter.accept(future2));
    assertFalse(filter.accept(future3));
    assertFalse(filter.accept(future4));
    assertFalse(filter.accept(future5));
    assertTrue(filter.accept(future6));
    assertTrue(filter.accept(future7));
    assertFalse(filter.accept(future8));
    assertFalse(filter.accept(future9));
    assertTrue(filter.accept(future10));
  }

  private static class P_RunContext extends RunContext {

    @Override
    public RunContext copy() {
      final P_RunContext copy = new P_RunContext();
      copy.copyValues(this);
      return copy;
    }
  }
}
