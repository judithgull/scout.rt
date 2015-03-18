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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.filter.AlwaysFilter;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.job.IBlockingCondition;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.JobExecutionException;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.job.internal.JobManager;
import org.eclipse.scout.rt.platform.job.internal.MutexSemaphores;
import org.eclipse.scout.rt.platform.job.internal.future.IFutureTask;
import org.eclipse.scout.rt.platform.job.internal.future.Job;
import org.eclipse.scout.rt.shared.ISession;
import org.eclipse.scout.rt.testing.commons.BlockingCountDownLatch;
import org.eclipse.scout.rt.testing.commons.UncaughtExceptionRunnable;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(PlatformTestRunner.class)
public class MutualExclusionTest {

  private static ExecutorService s_executor;

  private P_JobManager m_jobManager;
  private List<IBean<?>> m_beans;

  @Mock
  private IClientSession m_clientSession;

  @BeforeClass
  public static void beforeClass() {
    s_executor = Executors.newCachedThreadPool();
  }

  @AfterClass
  public static void afterClass() {
    s_executor.shutdown();
  }

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);

    m_jobManager = new P_JobManager();
    m_beans = TestingUtility.registerServices(1000, m_jobManager);

    ISession.CURRENT.set(m_clientSession);
  }

  @After
  public void after() {
    TestingUtility.unregisterServices(m_beans);

    ISession.CURRENT.remove();
  }

  /**
   * Tests serial execution of model jobs.
   */
  @Test
  public void testModelJobs() throws ProcessingException, InterruptedException {
    final Set<Integer> protocol = Collections.synchronizedSet(new HashSet<Integer>()); // synchronized because modified/read by different threads.
    final List<String> modelThreadProtocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add(1);
        if (ModelJobs.isModelThread()) {
          modelThreadProtocol.add("model-thread-1");
        }
      }
    });

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add(2);
        if (ModelJobs.isModelThread()) {
          modelThreadProtocol.add("model-thread-2");
        }
      }
    });

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add(3);
        if (ModelJobs.isModelThread()) {
          modelThreadProtocol.add("model-thread-3");
        }
      }
    });

    m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS);

    assertEquals(CollectionUtility.hashSet(1, 2, 3), protocol);
    assertEquals(CollectionUtility.arrayList("model-thread-1", "model-thread-2", "model-thread-3"), modelThreadProtocol);
  }

  /**
   * Tests serial execution of nested model jobs.
   */
  @Test
  public void testNestedModelJobs() throws ProcessingException, InterruptedException {
    final List<Integer> protocol = Collections.synchronizedList(new ArrayList<Integer>()); // synchronized because modified/read by different threads.

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add(1);

        // SCHEDULE
        ModelJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            protocol.add(4);

            // SCHEDULE
            IFuture<Void> future = ModelJobs.schedule(new IRunnable() {

              @Override
              public void run() throws Exception {
                protocol.add(9);
              }
            });

            try {
              future.awaitDone(1, TimeUnit.SECONDS);
            }
            catch (JobExecutionException e) {
              if (e.isTimeout()) {
                protocol.add(5);
              }
            }

            protocol.add(6);
          }
        });

        protocol.add(2);

        // SCHEDULE
        ModelJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            protocol.add(7);

            ModelJobs.schedule(new IRunnable() {

              @Override
              public void run() throws Exception {
                protocol.add(10);
              }
            });

            protocol.add(8);
          }
        });

        protocol.add(3);
      }
    });

    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 30, TimeUnit.SECONDS));
    assertEquals(CollectionUtility.arrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), protocol);
  }

  /**
   * Tests that a model-job cannot wait for a scheduled job.
   */
  @Test
  public void testMutexDeadlock() throws ProcessingException, InterruptedException {
    final List<Integer> protocol = Collections.synchronizedList(new ArrayList<Integer>()); // synchronized because modified/read by different threads.

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add(1);

        IFuture<Void> future = ModelJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            protocol.add(4);
          }
        });

        try {
          future.awaitDone(1, TimeUnit.SECONDS);
        }
        catch (JobExecutionException e) {
          protocol.add(2);
          if (e.isTimeout()) {
            protocol.add(3);
          }
        }
      }
    });

    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 30, TimeUnit.SECONDS));
    assertEquals(CollectionUtility.arrayList(1, 2, 3, 4), protocol);
  }

  /**
   * Tests a BlockingCondition that blocks a single model-thread.
   */
  @Test
  public void testBlockingConditionSingle() throws ProcessingException, InterruptedException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final IBlockingCondition BC = Jobs.getJobManager().createBlockingCondition("bc", true);

    IFuture<Void> future1 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("1: running");

        if (m_jobManager.isDone(new AlwaysFilter<IFuture<?>>())) {
          protocol.add("1: idle [a]");
        }
        if (IFuture.CURRENT.get().isBlocked()) {
          protocol.add("1: blocked [a]");
        }
        if (ModelJobs.isModelThread()) {
          protocol.add("1: modelThread [a]");
        }

        protocol.add("1: beforeAwait");
        BC.waitFor();
        protocol.add("1: afterAwait");

        if (m_jobManager.isDone(new AlwaysFilter<IFuture<?>>())) {
          protocol.add("1: idle [b]");
        }
        if (IFuture.CURRENT.get().isBlocked()) {
          protocol.add("1: blocked [b]");
        }
        if (ModelJobs.isModelThread()) {
          protocol.add("1: modelThread [b]");
        }
      }
    }, ModelJobInput.defaults().setName("job-1"));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("2: running");

        if (m_jobManager.isDone(new AlwaysFilter<IFuture<?>>())) {
          protocol.add("2: idle [a]");
        }
        if (IFuture.CURRENT.get().isBlocked()) {
          protocol.add("2: blocked [a]");
        }
        if (ModelJobs.isModelThread()) {
          protocol.add("2: modelThread [a]");
        }

        // RELEASE THE BlockingCondition
        protocol.add("2: beforeSignaling");
        BC.setBlocking(false);
        protocol.add("2: afterSignaling");

        if (m_jobManager.isDone(new AlwaysFilter<IFuture<?>>())) {
          protocol.add("2: idle [b]");
        }
        if (IFuture.CURRENT.get().isBlocked()) {
          protocol.add("2: blocked [b]");
        }
        if (ModelJobs.isModelThread()) {
          protocol.add("2: modelThread [b]");
        }
      }
    }, ModelJobInput.defaults().setName("job-2"));

    // Wait until job1 completed.
    future1.awaitDone(30, TimeUnit.SECONDS);

    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 30, TimeUnit.SECONDS));

    List<String> expected = new ArrayList<>();
    expected.add("1: running");
    expected.add("1: modelThread [a]");
    expected.add("1: beforeAwait");
    expected.add("2: running");
    expected.add("2: modelThread [a]");
    expected.add("2: beforeSignaling");
    expected.add("2: afterSignaling");
    expected.add("2: modelThread [b]");
    expected.add("1: afterAwait");
    expected.add("1: modelThread [b]");

    assertEquals(expected, protocol);
  }

  /**
   * Tests a BlockingCondition that blocks multiple model-threads.
   */
  @Test
  public void testBlockingConditionMultipleFlat() throws ProcessingException, InterruptedException {
    final IBlockingCondition BC = m_jobManager.createBlockingCondition("bc", true);

    // run the test 2 times to also test reusability of a blocking condition.
    runTestBlockingConditionMultipleFlat(BC);
    runTestBlockingConditionMultipleFlat(BC);
  }

  /**
   * Tests a BlockingCondition that blocks multiple model-threads that were scheduled as nested jobs.
   */
  @Test
  public void testBlockingConditionMultipleNested() throws ProcessingException, InterruptedException {
    final IBlockingCondition BC = m_jobManager.createBlockingCondition("bc", true);

    // run the test 2 times to also test reusability of a blocking condition.
    runTestBlockingCondition(BC);
    runTestBlockingCondition(BC);
  }

  /**
   * We have 3 jobs that are scheduled simultaneously. Thereby, job1 enters a blocking condition which in turn lets job2
   * run. Job2 goes to sleep forever, meaning that job3 will never start running. The test verifies, that when job1
   * is interrupted, job3 must not be scheduled because the mutex-owner is still job2.
   */
  @Test
  public void testBlockingCondition_InterruptedWhileBeingBlocked() throws JobExecutionException, InterruptedException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.
    final IBlockingCondition BC = m_jobManager.createBlockingCondition("bc", true);

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(1);

    final IFuture<Void> future1 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-1");

        try {
          BC.waitFor();
        }
        catch (ProcessingException e) {
          if (e.isInterruption()) {
            protocol.add("interrupted-1");
          }

          if (!ModelJobs.isModelThread()) {
            protocol.add("non-model-thread-1");
          }
        }
        verifyLatch.countDown();
      }
    });

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-2");
        setupLatch.countDownAndBlock();
      }
    });
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-3");
      }
    });

    setupLatch.await();
    assertTrue(future1.isBlocked());

    // RUN THE TEST
    future1.cancel(true);

    // VERIFY
    verifyLatch.await();
    assertFalse(m_jobManager.isDone(new AlwaysFilter<IFuture<?>>()));
    assertFalse(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 1, TimeUnit.MILLISECONDS));
    assertEquals(Arrays.asList("running-1", "running-2", "interrupted-1", "non-model-thread-1"), protocol);
  }

  /**
   * We have 3 jobs that are scheduled simultaneously. Thereby, job1 enters a blocking condition which in turn lets job2
   * running. Job2 unblocks job1 so that job1 is trying to re-acquire the mutex. While waiting for the mutex to be
   * available, job1 is interrupted due to a cancel-request of job2.<br/>
   * This test verifies, that job1 is interrupted but is not the model thread. Also, the 're-acquire-mutex'-task of job1
   * is still pending as long as job2 does not complete. Also, job3 must not start running as long as job2 did not
   * complete, so that the 're-acquire-mutex'-task for job1 can finally schedule job3.
   */
  @Test
  public void testBlockingCondition_InterruptedWhileReAcquiringTheMutex() throws ProcessingException, InterruptedException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.
    final IBlockingCondition BC = m_jobManager.createBlockingCondition("bc", true);

    final BlockingCountDownLatch latchJob2 = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch job1FinishLatch = new BlockingCountDownLatch(1);

    final IFuture<Void> future1 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-1");

        try {
          protocol.add("before-blocking-1");
          BC.waitFor();
          protocol.add("not-interrupted-1");
        }
        catch (JobExecutionException e) {
          protocol.add("jobExecutionException-1");
          if (e.isInterruption()) {
            protocol.add("interrupted-1");
          }
          if (!ModelJobs.isModelThread()) {
            protocol.add("not-model-thread-1");
          }
        }
        protocol.add("done-1");
        job1FinishLatch.countDown();
      }
    }, ModelJobInput.defaults().setName("job-1"));

    final IFuture<Void> future2 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-2a");
        BC.setBlocking(false);
        protocol.add("before-cancel-job1-2");
        future1.cancel(true); // interrupt job1 while acquiring the mutex
        assertTrue(job1FinishLatch.await());
        protocol.add("running-2b");
        latchJob2.countDownAndBlock();
        protocol.add("done-2");
      }
    }, ModelJobInput.defaults().setName("job-2"));

    final IFuture<Void> future3 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("done-3");
      }
    }, ModelJobInput.defaults().setName("job-3"));

    assertTrue(latchJob2.await());
    waitForPermitsAcquired(m_jobManager.getMutexSemaphores(), m_clientSession, 3); // job-1 (interrupted, but re-acquire mutex task still pending), job32 (latch), job-3 (pending)

    List<String> expectedProtocol = new ArrayList<>();
    expectedProtocol.add("running-1");
    expectedProtocol.add("before-blocking-1");
    expectedProtocol.add("running-2a");
    expectedProtocol.add("before-cancel-job1-2");
    expectedProtocol.add("jobExecutionException-1");
    expectedProtocol.add("interrupted-1");
    expectedProtocol.add("not-model-thread-1");
    expectedProtocol.add("done-1");
    expectedProtocol.add("running-2b");

    assertEquals(expectedProtocol, protocol);

    assertFalse(future1.isBlocked()); // the blocking condition is reversed but still job1 is  not running yet because waiting for the mutex.
    assertFalse(future2.isBlocked());
    assertFalse(future3.isBlocked());

    assertTrue(future1.isCancelled());
    try {
      assertNull(future1.awaitDone(0, TimeUnit.MILLISECONDS));
      assertTrue(future1.isCancelled());
    }
    catch (JobExecutionException e) {
      fail();
    }

    assertFalse(future2.isCancelled());
    try {
      future2.awaitDone(0, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isTimeout());
    }

    assertFalse(future3.isCancelled());
    try {
      future3.awaitDone(0, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isTimeout());
    }

    // let job2 finish its work so that job1 can re-acquire the mutex.
    latchJob2.unblock();
    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS));

    expectedProtocol.add("done-2");
    expectedProtocol.add("done-3");
    assertEquals(expectedProtocol, protocol);
    try {
      assertNull(future2.awaitDone(0, TimeUnit.MILLISECONDS));
    }
    catch (JobExecutionException e) {
      fail();
    }

    try {
      assertNull(future3.awaitDone(0, TimeUnit.MILLISECONDS));
    }
    catch (JobExecutionException e) {
      fail();
    }
  }

  /**
   * We have 3 jobs that get scheduled simultaneously. The first waits some time so that job2 and job3 get queued. If
   * job2 gets scheduled, it is rejected by the executor. This test verifies, that job3 still gets scheduled.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testRejection() throws InterruptedException, JobExecutionException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final ScheduledThreadPoolExecutor executorMock = mock(ScheduledThreadPoolExecutor.class);

    class TestJobManager extends JobManager {

      @Override
      protected ScheduledExecutorService createExecutor() {
        return executorMock;
      }
    }

    final TestJobManager jobManager = new TestJobManager();
    m_beans.addAll(TestingUtility.registerServices(1001, jobManager));

    final BlockingCountDownLatch jobsScheduledLatch = new BlockingCountDownLatch(1);

    // Simulate the executor.
    final AtomicInteger count = new AtomicInteger();
    doAnswer(new Answer<Future>() {

      @Override
      public Future answer(InvocationOnMock invocation) throws Throwable {
        final Job<?> job = (Job<?>) invocation.getArguments()[0];

        switch (count.incrementAndGet()) {
          case 1: // job-1: RUN
            s_executor.execute(new Runnable() {

              @Override
              public void run() {
                try {
                  jobsScheduledLatch.await(); // wait for all jobs to be scheduled.
                  registerScheduledFuture(job).run(); // let job1 run
                }
                catch (InterruptedException e) {
                  // NOOP
                }
              }
            });
            break;
          case 2: // job-2: REJECT
            job.getFutureTask().reject();
            break;
          case 3: // job-3: RUN
            s_executor.execute(new Runnable() {

              @Override
              public void run() {
                registerScheduledFuture(job).run();
              }
            });
            break;
        }
        return null;
      }
    }).when(executorMock).submit(any(Callable.class));

    final IFuture<Void> future1 = jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-job-1");
      }
    }, ModelJobInput.defaults());
    final IFuture<Void> future2 = jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-job-2");
      }
    }, ModelJobInput.defaults());
    IFuture<Void> future3 = jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-job-3");
      }
    }, ModelJobInput.defaults());

    jobsScheduledLatch.countDown(); // notify that all jobs are scheduled.
    assertTrue(jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS));

    assertEquals(Arrays.asList("running-job-1", "running-job-3"), protocol);
    assertTrue(jobManager.isDone(new AlwaysFilter<IFuture<?>>()));
    assertFalse(future1.isCancelled());
    assertTrue(future2.isCancelled());
    assertFalse(future3.isCancelled());
  }

  /**
   * We have 4 jobs that get scheduled simultaneously. The first waits some time so that job2, job3 and job4 get queued.
   * Job1 then enters a blocking condition, which allows job2 to run. While job2 is running, it unblocks job1 so that
   * job1 competes for the mutex anew. After job2 completes, job1 is rejected by the executor. However, to mutex was
   * acquired and therefore job1 can run. After job1 complete, job3 and job4 gets scheduled.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testBlockedJobRejection() throws InterruptedException, ProcessingException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);
    class TestJobManager extends JobManager {

      @Override
      protected ScheduledExecutorService createExecutor() {
        return executorMock;
      }

      public MutexSemaphores getMutexSemaphores() {
        return m_mutexSemaphores;
      }
    }
    final TestJobManager jobManager = new TestJobManager();

    m_beans.addAll(TestingUtility.registerServices(1001, jobManager));

    final BlockingCountDownLatch jobsScheduledLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch job3RunningLatch = new BlockingCountDownLatch(1);

    // mock the executor: Executor#execute
    final AtomicInteger count = new AtomicInteger();
    doAnswer(new Answer<Future>() {

      @Override
      public Future answer(InvocationOnMock invocation) throws Throwable {
        final Job<?> job = (Job<?>) invocation.getArguments()[0];

        switch (count.incrementAndGet()) {
          case 1: // job-1: RUN
            s_executor.execute(new Runnable() {

              @Override
              public void run() {
                try {
                  jobsScheduledLatch.await(); // wait for all jobs to be scheduled.
                  registerScheduledFuture(job).run();
                }
                catch (InterruptedException e) {
                  // NOOP
                }
              }
            });
            break;
          case 2: // job-2: RUN after job1 enters blocking condition
          case 4: // job-3: gets scheduled
          case 5: // job-4: gets scheduled
            s_executor.execute(new Runnable() {

              @Override
              public void run() {
                registerScheduledFuture(job).run();
              }
            });
            break;
          case 3: // job-1:  re-acquires the mutex after being released from the blocking condition
            job.getFutureTask().reject();
            break;
        }
        return null;
      }
    }).when(executorMock).submit(any(Callable.class));

    final IBlockingCondition BC = jobManager.createBlockingCondition("bc", true);

    IFuture<Void> future1 = jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        try {
          protocol.add("running-job-1 (a)");
          BC.waitFor();
          protocol.add("running-job-1 (b)");
        }
        catch (JobExecutionException e) {
          protocol.add("jobExecutionException");
        }

        if (ModelJobs.isModelThread()) {
          protocol.add("running-job-1 (e) [model-thread]");
        }
      }
    }, ModelJobInput.defaults().setName("job-1"));

    IFuture<Void> future2 = jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-job-2 (a)");
        BC.setBlocking(false);

        // Wait until job-1 tried to re-acquire the mutex.
        waitForPermitsAcquired(jobManager.getMutexSemaphores(), m_clientSession, 4); // 4 = job1(re-acquiring), job2(owner), job3, job4
        protocol.add("running-job-2 (b)");
      }
    }, ModelJobInput.defaults().setName("job-2"));

    IFuture<Void> future3 = jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-job-3");
        job3RunningLatch.countDownAndBlock();
      }
    }, ModelJobInput.defaults().setName("job-3"));

    IFuture<Void> future4 = jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-job-4");
      }
    }, ModelJobInput.defaults().setName("job-4"));

    jobsScheduledLatch.countDown(); // notify that all jobs are scheduled.

    assertTrue(job3RunningLatch.await());
    assertFalse(jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 1, TimeUnit.MILLISECONDS)); // job-4 is pending
    assertFalse(jobManager.isDone(new AlwaysFilter<IFuture<?>>())); // job-4 is pending

    List<String> expectedProtocol = new ArrayList<>();
    expectedProtocol.add("running-job-1 (a)");
    expectedProtocol.add("running-job-2 (a)");
    expectedProtocol.add("running-job-2 (b)");
    expectedProtocol.add("running-job-1 (b)");
    expectedProtocol.add("running-job-1 (e) [model-thread]");
    expectedProtocol.add("running-job-3");
    assertEquals(expectedProtocol, protocol);

    assertFalse(future1.isCancelled());
    assertTrue(future1.isDone());

    assertFalse(future2.isCancelled());
    assertTrue(future2.isDone());

    assertFalse(future3.isCancelled());
    assertFalse(future3.isDone());

    assertFalse(future4.isCancelled());
    assertFalse(future4.isDone());

    // cancel job3
    future3.cancel(true);
    assertTrue(jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS));

    expectedProtocol.add("running-job-4");
    assertEquals(expectedProtocol, protocol);

    assertTrue(jobManager.isDone(new AlwaysFilter<IFuture<?>>()));

    assertTrue(future3.isCancelled());
    assertTrue(future3.isDone());

    assertFalse(future4.isCancelled());
    assertTrue(future4.isDone());
  }

  public void runTestBlockingConditionMultipleFlat(final IBlockingCondition BC) throws ProcessingException, InterruptedException {
    BC.setBlocking(true);

    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final IFuture<Void> future1 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-1-beforeAwait");
        BC.waitFor();
        protocol.add("job-X-afterAwait");
      }
    });

    final IFuture<Void> future2 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-2-beforeAwait");
        BC.waitFor();
        protocol.add("job-X-afterAwait");
      }
    });

    final IFuture<Void> future3 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-3-beforeAwait");
        BC.waitFor();
        protocol.add("job-X-afterAwait");
      }
    });

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-4-running");
      }
    });

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-5-running");
        if (future1.isBlocked()) {
          protocol.add("job-1-blocked");
        }
        if (future2.isBlocked()) {
          protocol.add("job-2-blocked");
        }
        if (future3.isBlocked()) {
          protocol.add("job-3-blocked");
        }

        protocol.add("job-5-signaling");
        BC.setBlocking(false);

        // Wait until the other jobs tried to re-acquire the mutex.
        waitForPermitsAcquired(m_jobManager.getMutexSemaphores(), m_clientSession, 4);

        if (!future1.isBlocked()) {
          protocol.add("job-1-unblocked");
        }
        if (!future2.isBlocked()) {
          protocol.add("job-2-unblocked");
        }
        if (!future3.isBlocked()) {
          protocol.add("job-3-unblocked");
        }

        if (!future1.isDone()) {
          protocol.add("job-1-stillRunning");
        }
        if (!future2.isDone()) {
          protocol.add("job-2-stillRunning");
        }
        if (!future3.isDone()) {
          protocol.add("job-3-stillRunning");
        }

        protocol.add("job-5-ending");
      }
    });

    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS));

    List<String> expected = new ArrayList<>();
    expected.add("job-1-beforeAwait");
    expected.add("job-2-beforeAwait");
    expected.add("job-3-beforeAwait");
    expected.add("job-4-running");
    expected.add("job-5-running");
    expected.add("job-1-blocked");
    expected.add("job-2-blocked");
    expected.add("job-3-blocked");
    expected.add("job-5-signaling");
    expected.add("job-1-unblocked");
    expected.add("job-2-unblocked");
    expected.add("job-3-unblocked");
    expected.add("job-1-stillRunning");
    expected.add("job-2-stillRunning");
    expected.add("job-3-stillRunning");
    expected.add("job-5-ending");
    expected.add("job-X-afterAwait"); // no guarantee about the sequence in which waiting threads are released when the blocking condition falls.
    expected.add("job-X-afterAwait"); // no guarantee about the sequence in which waiting threads are released when the blocking condition falls.
    expected.add("job-X-afterAwait"); // no guarantee about the sequence in which waiting threads are released when the blocking condition falls.

    assertEquals(expected, protocol);
  }

  private void runTestBlockingCondition(final IBlockingCondition BC) throws InterruptedException, JobExecutionException {
    BC.setBlocking(true);

    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        final IFuture<?> iFuture1 = IFuture.CURRENT.get();

        protocol.add("job-1-running");

        ModelJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            final IFuture<?> iFuture2 = IFuture.CURRENT.get();

            protocol.add("job-2-running");

            if (iFuture1.isBlocked()) {
              protocol.add("job-1-blocked");
            }

            ModelJobs.schedule(new IRunnable() {

              @Override
              public void run() throws Exception {
                protocol.add("job-3-running");

                if (iFuture1.isBlocked()) {
                  protocol.add("job-1-blocked");
                }
                if (iFuture2.isBlocked()) {
                  protocol.add("job-2-blocked");
                }

                ModelJobs.schedule(new IRunnable() {

                  @Override
                  public void run() throws Exception {
                    protocol.add("job-4-running");
                  }
                });

                protocol.add("job-3-before-signaling");
                BC.setBlocking(false);

                waitForPermitsAcquired(m_jobManager.getMutexSemaphores(), m_clientSession, 4); // Wait for the other jobs to have tried re-acquiring the mutex.

                protocol.add("job-3-after-signaling");

                ModelJobs.schedule(new IRunnable() {

                  @Override
                  public void run() throws Exception {
                    protocol.add("job-5-running");
                  }
                });
              }
            });

            protocol.add("job-2-beforeAwait");
            BC.waitFor();
            protocol.add("JOB-X-AFTERAWAIT");
          }
        });

        protocol.add("job-1-beforeAwait");
        BC.waitFor();
        protocol.add("JOB-X-AFTERAWAIT");
      }
    });
    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS));

    List<String> expected = new ArrayList<>();
    expected.add("job-1-running");
    expected.add("job-1-beforeAwait");
    expected.add("job-2-running");
    expected.add("job-1-blocked");
    expected.add("job-2-beforeAwait");
    expected.add("job-3-running");
    expected.add("job-1-blocked");
    expected.add("job-2-blocked");
    expected.add("job-3-before-signaling");
    expected.add("job-3-after-signaling");
    expected.add("JOB-X-AFTERAWAIT"); // no guarantee about the sequence in which waiting threads are released when the blocking condition falls.
    expected.add("JOB-X-AFTERAWAIT"); // no guarantee about the sequence in which waiting threads are released when the blocking condition falls.
    expected.add("job-4-running");
    expected.add("job-5-running");

    assertEquals(expected, protocol);
  }

  /**
   * Tests that an invalidated Blocking condition does not block.
   */
  @Test
  public void testBlockingConditionNotBlocking() throws InterruptedException {
    final IBlockingCondition BC = m_jobManager.createBlockingCondition("BC", false);
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        BC.waitFor();
      }
    });
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        BC.waitFor();
      }
    });

    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS));
  }

  /**
   * A job enters a blocking condition, which in the meantime was invalidated. This test verifies, that the job is not
   * blocked when calling waitFor.
   */
  @Test
  public void testEnterUnblockedBlockingCondition() throws Throwable {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch unblockedLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch done = new BlockingCountDownLatch(1);

    final IBlockingCondition BC = m_jobManager.createBlockingCondition("BC", true);

    final UncaughtExceptionRunnable runnable = new UncaughtExceptionRunnable() {
      @Override
      protected void runSafe() throws Exception {
        BC.setBlocking(false);
        protocol.add("1: afterUnblock [inner]");
        unblockedLatch.countDown();
        done.await();
        protocol.add("4: done");
      }
    };

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        s_executor.execute(runnable);

        assertTrue(unblockedLatch.await()); // wait until the BC in unblocked

        protocol.add("2: beforeWaitFor [outer]");
        BC.waitFor();
        protocol.add("3: afterWaitFor [outer]");
        done.release();
      }
    });

    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS));
    runnable.throwOnError();

    List<String> expected = new ArrayList<>();
    expected.add("1: afterUnblock [inner]");
    expected.add("2: beforeWaitFor [outer]");
    expected.add("3: afterWaitFor [outer]");
    expected.add("4: done");
  }

  /**
   * Job1 gets scheduled and enters blocking condition. If waiting for the condition to fall, the main thread
   * invalidates the condition. Afterwards, another job gets scheduled which should not wait for the blocking condition
   * to fall because still invalidated.
   *
   * @throws Throwable
   */
  @Test
  public void testReuseUnblockedBlockingCondition() throws Throwable {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final IBlockingCondition BC = m_jobManager.createBlockingCondition("BC", true);

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("1: beforeWaitFor");
        BC.waitFor();
        protocol.add("3: afterWaitFor");
      }
    });

    waitForPermitsAcquired(m_jobManager.getMutexSemaphores(), m_clientSession, 0); // Wait until job1 is blocked
    assertTrue(BC.isBlocking());
    protocol.add("2: setBlocking=false");
    BC.setBlocking(false);
    assertFalse(BC.isBlocking());
    m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS);

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("4: beforeWaitFor");
        BC.waitFor();
        protocol.add("5: afterWaitFor");
      }
    }).awaitDone();

    assertTrue(m_jobManager.awaitDone(new AlwaysFilter<IFuture<?>>(), 10, TimeUnit.SECONDS));

    List<String> expected = new ArrayList<>();
    expected.add("1: beforeWaitFor");
    expected.add("2: setBlocking=false");
    expected.add("3: afterWaitFor");
    expected.add("4: beforeWaitFor");
    expected.add("5: afterWaitFor");
  }

  /**
   * Tests that a job continues execution after waiting for a blocking condition to fall.
   */
  @Test
  public void testExpiredWhenReAcquiringMutex() throws ProcessingException, InterruptedException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final IBlockingCondition BC = m_jobManager.createBlockingCondition("BC", true);

    IFuture<Void> future = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("1: running");
        ModelJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            BC.setBlocking(false);
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
          }
        });
        BC.waitFor();
        protocol.add("2: running");
      }
    }, ModelJobInput.defaults().setExpirationTime(1, TimeUnit.MILLISECONDS));

    // Wait until entering blocking condition
    waitForPermitsAcquired(m_jobManager.getMutexSemaphores(), m_clientSession, 0);

    // Expect the job to continue running.
    try {
      future.awaitDone();

      List<String> expected = new ArrayList<>();
      expected.add("1: running");
      expected.add("2: running");
      assertEquals(expected, protocol);
    }
    catch (JobExecutionException e) {
      fail();
    }
  }

  private <T> IFutureTask<T> registerScheduledFuture(final Job<T> job) {
    final FutureTask<T> task = new FutureTask<>(job);
    @SuppressWarnings("unchecked")
    final RunnableScheduledFuture<T> javaFuture = mock(RunnableScheduledFuture.class);
    doAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        task.run();
        return null;
      }
    }).when(javaFuture).run();
    when(javaFuture.isCancelled()).thenAnswer(new Answer<Boolean>() {

      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        return task.isCancelled();
      }
    });
    when(javaFuture.isDone()).thenAnswer(new Answer<Boolean>() {

      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        return task.isDone();
      }
    });
    when(javaFuture.cancel(anyBoolean())).thenAnswer(new Answer<Boolean>() {

      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        return task.cancel((boolean) invocation.getArguments()[0]);
      }
    });

    job.getFutureTask().setDelegate(javaFuture);

    return job.getFutureTask();
  }

  private class P_JobManager extends JobManager {

    public MutexSemaphores getMutexSemaphores() {
      return m_mutexSemaphores;
    }
  }

  /**
   * Blocks the current thread until the expected number of mutex-permits is acquired; Waits for maximal 30s.
   */
  public static void waitForPermitsAcquired(MutexSemaphores mutexSemaphores, IClientSession session, int expectedPermitCount) throws InterruptedException {
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);

    // Wait until the other jobs tried to re-acquire the mutex.
    while (mutexSemaphores.getPermitCount(session) != expectedPermitCount) {
      if (System.currentTimeMillis() > deadline) {
        fail(String.format("Timeout elapsed while waiting for a mutex-permit count. [expectedPermitCount=%s, actualPermitCount=%s]", expectedPermitCount, mutexSemaphores.getPermitCount(session)));
      }
      Thread.sleep(10);
    }
  }
}