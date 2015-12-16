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
package org.eclipse.scout.rt.client.job.filter.future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContext;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.client.job.ModelJobs;
import org.eclipse.scout.rt.platform.filter.IFilter;
import org.eclipse.scout.rt.platform.job.IBlockingCondition;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.ISchedulingSemaphore;
import org.eclipse.scout.rt.platform.job.JobState;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.job.filter.future.FutureFilter;
import org.eclipse.scout.rt.platform.util.concurrent.IRunnable;
import org.eclipse.scout.rt.shared.ISession;
import org.eclipse.scout.rt.shared.job.filter.future.SessionFutureFilter;
import org.eclipse.scout.rt.testing.platform.job.JobTestUtil;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class ClientRunContextFutureFilterTest {

  private IFuture<?> m_clientJobFuture;
  private IFuture<?> m_modelJobFuture;
  private IClientSession m_clientSession1;
  private IClientSession m_clientSession2;

  @Before
  public void before() {
    m_clientSession1 = mock(IClientSession.class);
    when(m_clientSession1.getModelJobSchedulingSemaphore()).thenReturn(Jobs.newSchedulingSemaphore(1));

    m_clientSession2 = mock(IClientSession.class);
    when(m_clientSession2.getModelJobSchedulingSemaphore()).thenReturn(Jobs.newSchedulingSemaphore(1));

    m_clientJobFuture = Jobs.schedule(mock(IRunnable.class), Jobs.newInput().withRunContext(ClientRunContexts.empty().withSession(m_clientSession1, true)));
    m_modelJobFuture = ModelJobs.schedule(mock(IRunnable.class), ModelJobs.newInput(ClientRunContexts.empty().withSession(m_clientSession1, true)));
  }

  @Test
  public void testBlocked() {
    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    // Client Job
    m_clientJobFuture = Jobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        condition.waitFor(10, TimeUnit.SECONDS);
      }
    }, Jobs.newInput().withRunContext(ClientRunContexts.empty().withSession(m_clientSession1, true)));

    // Model Job
    m_modelJobFuture = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        condition.waitFor(10, TimeUnit.SECONDS);
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().withSession(m_clientSession1, true)));

    JobTestUtil.waitForState(m_clientJobFuture, JobState.WAITING_FOR_BLOCKING_CONDITION);
    JobTestUtil.waitForState(m_modelJobFuture, JobState.WAITING_FOR_BLOCKING_CONDITION);

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchState(JobState.WAITING_FOR_BLOCKING_CONDITION)
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchNotState(JobState.WAITING_FOR_BLOCKING_CONDITION)
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .toFilter()
        .accept(m_modelJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .andMatchState(JobState.WAITING_FOR_BLOCKING_CONDITION)
        .toFilter()
        .accept(m_modelJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatchNotState(JobState.WAITING_FOR_BLOCKING_CONDITION)
        .toFilter()
        .accept(m_modelJobFuture));

    // Release threads
    condition.setBlocking(false);
  }

  @Test
  public void testPeriodic() {
    IFuture<Void> clientJobFuture = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withRunContext(ClientRunContexts.empty().withSession(m_clientSession1, true))
        .withPeriodicExecutionAtFixedRate(1, TimeUnit.SECONDS));

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .toFilter()
        .accept(clientJobFuture));

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andArePeriodicExecuting()
        .toFilter()
        .accept(clientJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andAreSingleExecuting()
        .toFilter()
        .accept(clientJobFuture));

    IFuture<Void> modelJobFuture = ModelJobs.schedule(mock(IRunnable.class), ModelJobs.newInput(ClientRunContexts.empty().withSession(m_clientSession1, true))
        .withPeriodicExecutionAtFixedRate(1, TimeUnit.SECONDS));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .toFilter()
        .accept(modelJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .andArePeriodicExecuting()
        .toFilter()
        .accept(modelJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andAreSingleExecuting()
        .toFilter()
        .accept(modelJobFuture));

    // cleanup
    Jobs.getJobManager().cancel(Jobs.newFutureFilterBuilder()
        .andMatchFuture(clientJobFuture, modelJobFuture)
        .toFilter(), true);
  }

  @Test
  public void testSession() {
    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatch(new SessionFutureFilter(m_clientSession1))
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .andMatch(new SessionFutureFilter(m_clientSession1))
        .toFilter()
        .accept(m_modelJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatch(new SessionFutureFilter(m_clientSession2))
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatch(new SessionFutureFilter(m_clientSession2))
        .toFilter()
        .accept(m_modelJobFuture));
  }

  @Test
  public void testCurrentSession() {
    ISession.CURRENT.set(m_clientSession1);
    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class).andMatch(new SessionFutureFilter(ISession.CURRENT.get()))
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder().andMatch(new SessionFutureFilter(ISession.CURRENT.get()))
        .toFilter()
        .accept(m_modelJobFuture));

    ISession.CURRENT.set(m_clientSession2);

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class).andMatch(new SessionFutureFilter(ISession.CURRENT.get()))
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatch(new SessionFutureFilter(ISession.CURRENT.get()))
        .toFilter()
        .accept(m_modelJobFuture));
    ISession.CURRENT.remove();
  }

  @Test
  public void testNotCurrentSession() {
    ISession.CURRENT.set(m_clientSession1);
    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchNot(new SessionFutureFilter(ISession.CURRENT.get()))
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatchNot(new SessionFutureFilter(ISession.CURRENT.get()))
        .toFilter()
        .accept(m_modelJobFuture));

    ISession.CURRENT.set(m_clientSession2);

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchNot(new SessionFutureFilter(ISession.CURRENT.get()))
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .andMatchNot(new SessionFutureFilter(ISession.CURRENT.get()))
        .toFilter()
        .accept(m_modelJobFuture));
    ISession.CURRENT.remove();
  }

  @Test
  public void testFuture() {
    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchFuture(m_clientJobFuture, m_modelJobFuture)
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatchFuture(m_clientJobFuture, m_modelJobFuture)
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(new FutureFilter(m_clientJobFuture, m_modelJobFuture).accept(m_clientJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchNot(ModelJobFutureFilter.INSTANCE)
        .andMatchFuture(m_clientJobFuture, m_modelJobFuture)
        .toFilter()
        .accept(m_modelJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .andMatchFuture(m_clientJobFuture, m_modelJobFuture)
        .toFilter()
        .accept(m_modelJobFuture));

    assertTrue(new FutureFilter(m_clientJobFuture, m_modelJobFuture).accept(m_modelJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .andMatchFuture(m_modelJobFuture)
        .toFilter()
        .accept(m_modelJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchNot(ModelJobFutureFilter.INSTANCE)
        .andMatchFuture(m_modelJobFuture)
        .toFilter()
        .accept(m_modelJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatchFuture(m_clientJobFuture)
        .toFilter()
        .accept(m_modelJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchFuture(m_clientJobFuture)
        .toFilter()
        .accept(m_modelJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatchFuture(m_modelJobFuture)
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchFuture(m_modelJobFuture)
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatchFuture(m_clientJobFuture)
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchFuture(m_clientJobFuture)
        .toFilter()
        .accept(m_clientJobFuture));
  }

  @Test
  public void testCurrentFuture() {
    IFuture.CURRENT.set(m_clientJobFuture);

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchFuture(IFuture.CURRENT.get())
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatchFuture(IFuture.CURRENT.get())
        .toFilter()
        .accept(m_modelJobFuture));

    IFuture.CURRENT.set(m_modelJobFuture);

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchFuture(IFuture.CURRENT.get())
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .andMatchFuture(IFuture.CURRENT.get())
        .toFilter()
        .accept(m_modelJobFuture));

    IFuture.CURRENT.remove();
  }

  @Test
  public void testNotCurrentFuture() {
    IFuture.CURRENT.set(m_clientJobFuture);

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchNotFuture(IFuture.CURRENT.get())
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(ModelJobs.newFutureFilterBuilder()
        .andMatchNotFuture(IFuture.CURRENT.get())
        .toFilter()
        .accept(m_modelJobFuture));

    IFuture.CURRENT.set(m_modelJobFuture);

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchNotFuture(IFuture.CURRENT.get())
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(ModelJobs.newFutureFilterBuilder()
        .andMatchNotFuture(IFuture.CURRENT.get())
        .toFilter()
        .accept(m_modelJobFuture));

    IFuture.CURRENT.remove();
  }

  @Test
  public void testMutualExclusion() {
    ISchedulingSemaphore mutex1 = Jobs.newSchedulingSemaphore(1);
    ISchedulingSemaphore mutex2 = Jobs.newSchedulingSemaphore(1);

    m_clientJobFuture.getJobInput().withSchedulingSemaphore(mutex1);
    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchSchedulingSemaphore(null)
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchSchedulingSemaphore(mutex1)
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchSchedulingSemaphore(mutex2)
        .toFilter()
        .accept(m_clientJobFuture));

    m_clientJobFuture.getJobInput().withSchedulingSemaphore(null);
    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .toFilter()
        .accept(m_clientJobFuture));

    assertTrue(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchSchedulingSemaphore(null)
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchSchedulingSemaphore(mutex1)
        .toFilter()
        .accept(m_clientJobFuture));

    assertFalse(Jobs.newFutureFilterBuilder()
        .andMatchRunContext(ClientRunContext.class)
        .andMatchSchedulingSemaphore(mutex2)
        .toFilter()
        .accept(m_clientJobFuture));
  }

  @Test
  public void testCustomFilter() {
    // False Filter
    assertFalse(Jobs.newFutureFilterBuilder().andMatchRunContext(ClientRunContext.class).andMatch(new IFilter<IFuture<?>>() {

      @Override
      public boolean accept(IFuture<?> future) {
        return false;
      }
    }).toFilter().accept(m_clientJobFuture));

    // True Filter
    assertTrue(Jobs.newFutureFilterBuilder().andMatchRunContext(ClientRunContext.class).andMatch(new IFilter<IFuture<?>>() {

      @Override
      public boolean accept(IFuture<?> future) {
        return true;
      }
    }).toFilter().accept(m_clientJobFuture));

    // True/False Filter
    assertFalse(Jobs.newFutureFilterBuilder().andMatchRunContext(ClientRunContext.class).andMatch(new IFilter<IFuture<?>>() {

      @Override
      public boolean accept(IFuture<?> future) {
        return true;
      }
    }).andMatch(new IFilter<IFuture<?>>() {

      @Override
      public boolean accept(IFuture<?> future) {
        return false;
      }
    }).toFilter().accept(m_clientJobFuture));
  }
}
