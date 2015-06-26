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
package org.eclipse.scout.rt.client.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.filter.AlwaysFilter;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IBeanMetaDataFacotry;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.job.internal.JobManager;
import org.eclipse.scout.rt.testing.commons.BlockingCountDownLatch;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class MultipleSessionTest {

  private IClientSession m_clientSession1;
  private IClientSession m_clientSession2;

  private List<IBean<?>> m_beans;

  @Before
  public void before() {
    m_beans = TestingUtility.registerBeans(
        BEANS.get(IBeanMetaDataFacotry.class).create(JobManager.class).
            applicationScoped(true));
    m_clientSession1 = mock(IClientSession.class);
    m_clientSession2 = mock(IClientSession.class);
  }

  @After
  public void after() {
    TestingUtility.unregisterBeans(m_beans);
  }

  @Test
  public void testMutalExclusion() throws ProcessingException, InterruptedException {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch latch1 = new BlockingCountDownLatch(2);
    final BlockingCountDownLatch latch2 = new BlockingCountDownLatch(2);

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job1-S1");
        latch1.countDownAndBlock();
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().session(m_clientSession1)).name("job-1-S1").logOnError(false));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job2-S1");
        latch2.countDownAndBlock();
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().session(m_clientSession1)).name("job-2-S1").logOnError(false));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job1-S2");
        latch1.countDownAndBlock();
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().session(m_clientSession2)).name("job-1-S2").logOnError(false));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job2-S2");
        latch2.countDownAndBlock();
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().session(m_clientSession2)).name("job-2-S2").logOnError(false));

    assertTrue(latch1.await());
    assertEquals(CollectionUtility.hashSet("job1-S1", "job1-S2"), protocol);
    latch1.unblock();

    assertTrue(latch2.await());
    assertEquals(CollectionUtility.hashSet("job1-S1", "job1-S2", "job2-S1", "job2-S2"), protocol);
    latch2.unblock();

    assertTrue(Jobs.getJobManager().awaitDone(new AlwaysFilter<IFuture<?>>(), 30, TimeUnit.SECONDS));
  }

  @Test
  public void testCancel() throws InterruptedException, ProcessingException {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch setupLatch1 = new BlockingCountDownLatch(2);
    final BlockingCountDownLatch setupLatch2 = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch interruptedJob1_S1_Latch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch awaitAllCancelledLatch = new BlockingCountDownLatch(1);

    // Session 1 (job1)
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job1-S1");
        try {
          setupLatch1.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job1-S1-interrupted");
        }
        finally {
          interruptedJob1_S1_Latch.countDown();
        }
        awaitAllCancelledLatch.await();
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().session(m_clientSession1)).name("job-1-S1").logOnError(false));

    // Session 1 (job2) --> never starts running because cancelled while job1 is mutex-owner
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job2-S1");
        try {
          setupLatch2.countDownAndBlock();
        }
        catch (InterruptedException e) {
          System.err.println("WHY");
          protocol.add("job2-S1-interrupted");
        }
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().session(m_clientSession1)).name("job-2-S1").logOnError(false));

    // Session 2 (job1)
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job1-S2");
        try {
          setupLatch1.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job1-S2-interrupted");
        }
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().session(m_clientSession2)).name("job-1-S2").logOnError(false));

    // Session 2 (job2)
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job2-S2");
        try {
          setupLatch2.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job2-S2-interrupted");
        }
      }
    }, ModelJobs.newInput(ClientRunContexts.empty().session(m_clientSession2)).name("job-2-S2").logOnError(false));

    assertTrue(setupLatch1.await());
    assertEquals(CollectionUtility.hashSet("job1-S1", "job1-S2"), protocol);

    Jobs.getJobManager().cancel(ModelJobs.newFutureFilter().andMatchSession(m_clientSession1), true); // cancels job-1-S1 and job-2-S1, meaning that job-2-S1 never starts running.
    awaitAllCancelledLatch.unblock();

    assertTrue(interruptedJob1_S1_Latch.await());

    setupLatch1.unblock();

    assertTrue(setupLatch2.await());
    assertEquals(CollectionUtility.hashSet("job1-S1", "job1-S1-interrupted", "job1-S2", "job2-S2"), protocol);
    setupLatch2.unblock();

    assertTrue(Jobs.getJobManager().awaitDone(new AlwaysFilter<IFuture<?>>(), 30, TimeUnit.SECONDS));
  }
}