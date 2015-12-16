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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IgnoreBean;
import org.eclipse.scout.rt.platform.Replace;
import org.eclipse.scout.rt.platform.exception.DefaultExceptionTranslator;
import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.eclipse.scout.rt.platform.job.IBlockingCondition;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.IJobManager;
import org.eclipse.scout.rt.platform.job.ISchedulingSemaphore;
import org.eclipse.scout.rt.platform.job.JobState;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.job.internal.JobFutureTask;
import org.eclipse.scout.rt.platform.job.internal.JobManager;
import org.eclipse.scout.rt.platform.util.Assertions.AssertionException;
import org.eclipse.scout.rt.platform.util.CollectionUtility;
import org.eclipse.scout.rt.platform.util.concurrent.IRunnable;
import org.eclipse.scout.rt.platform.util.concurrent.InterruptedException;
import org.eclipse.scout.rt.platform.util.concurrent.TimeoutException;
import org.eclipse.scout.rt.shared.ISession;
import org.eclipse.scout.rt.testing.platform.job.JobTestUtil;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.eclipse.scout.rt.testing.platform.util.BlockingCountDownLatch;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(PlatformTestRunner.class)
public class MutualExclusionTest {

  private static final String JOB_IDENTIFIER = UUID.randomUUID().toString();

  private static ExecutorService s_executor;

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
    m_clientSession = mock(IClientSession.class);
    when(m_clientSession.getModelJobSchedulingSemaphore()).thenReturn(Jobs.newSchedulingSemaphore(1));

    ISession.CURRENT.set(m_clientSession);
  }

  @After
  public void after() {
    ISession.CURRENT.remove();
  }

  /**
   * Tests serial execution of model jobs.
   */
  @Test
  public void testModelJobs() {
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
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add(2);
        if (ModelJobs.isModelThread()) {
          modelThreadProtocol.add("model-thread-2");
        }
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add(3);
        if (ModelJobs.isModelThread()) {
          modelThreadProtocol.add("model-thread-3");
        }
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent()).withExecutionHint(JOB_IDENTIFIER));

    awaitDoneElseFail(JOB_IDENTIFIER);

    assertEquals(CollectionUtility.hashSet(1, 2, 3), protocol);
    assertEquals(CollectionUtility.arrayList("model-thread-1", "model-thread-2", "model-thread-3"), modelThreadProtocol);
  }

  @Test(expected = AssertionException.class, timeout = 5000)
  public void testAwaitDoneWithSameMutex() {
    final ISchedulingSemaphore mutex = Jobs.newSchedulingSemaphore(1);
    Jobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        Jobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            // NOOP
          }
        }, Jobs.newInput()
            .withSchedulingSemaphore(mutex))
            .awaitDone();
      }
    }, Jobs.newInput()
        .withSchedulingSemaphore(mutex))
        .awaitDoneAndGet();
  }

  /**
   * A mutual exclusive job is running, and passes the mutex via BlockingCondition.waitFor() to the next task. But the
   * blocking condition is never unblocked, which results in a timeout, without that the mutex is acquired anew.
   * <p>
   * This test tests, the the job can wait for another job with the same mutex, because not being the mutex owner
   * itself.
   */
  @Test(timeout = 5000)
  public void testAwaitDoneWithSameMutexButNotMutexOwner() {
    final ISchedulingSemaphore mutex = Jobs.newSchedulingSemaphore(1);
    Jobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        IBlockingCondition bc = Jobs.newBlockingCondition(true);
        try {
          bc.waitFor(1, TimeUnit.SECONDS);
          fail("timeout expected");
        }
        catch (TimeoutException e) {
          // This job is not the mutex owner anymore

          try {
            final AtomicBoolean run = new AtomicBoolean(false);
            Jobs.schedule(new IRunnable() {

              @Override
              public void run() throws Exception {
                run.set(true);
              }
            }, Jobs.newInput()
                .withSchedulingSemaphore(mutex))
                .awaitDone(1, TimeUnit.SECONDS);
            assertTrue(run.get());
          }
          catch (TimeoutException e1) {
            fail("no timeout expected");
          }
          catch (AssertionException e1) {
            fail("no mutex assertion expected");
          }
        }
      }

    }, Jobs.newInput().withSchedulingSemaphore(mutex)).awaitDoneAndGet();
  }

  /**
   * Tests serial execution of nested model jobs.
   */
  @Test
  public void testNestedModelJobs() {
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
            }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
                .withExecutionHint(JOB_IDENTIFIER));

            try {
              future.awaitDoneAndGet(1, TimeUnit.SECONDS);
            }
            catch (AssertionException e) {
              protocol.add(5);
            }

            protocol.add(6);
          }
        }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
            .withExecutionHint(JOB_IDENTIFIER));

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
            }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
                .withExecutionHint(JOB_IDENTIFIER));

            protocol.add(8);
          }
        }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
            .withExecutionHint(JOB_IDENTIFIER));

        protocol.add(3);
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    awaitDoneElseFail(JOB_IDENTIFIER);
    assertEquals(CollectionUtility.arrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), protocol);
  }

  /**
   * Tests that a model-job cannot wait for a scheduled job.
   */
  @Test
  public void testMutexDeadlock() {
    final List<Integer> protocol = Collections.synchronizedList(new ArrayList<Integer>()); // synchronized because modified/read by different threads.

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add(1);

        IFuture<Void> future = ModelJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            protocol.add(3);
          }
        }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
            .withExecutionHint(JOB_IDENTIFIER));

        try {
          future.awaitDoneAndGet(1, TimeUnit.SECONDS, DefaultExceptionTranslator.class);
        }
        catch (AssertionException e) {
          protocol.add(2);
        }
        catch (Exception e) {
          protocol.add(4);
        }
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    awaitDoneElseFail(JOB_IDENTIFIER);
    assertEquals(CollectionUtility.arrayList(1, 2, 3), protocol);
  }

  /**
   * Tests a BlockingCondition that blocks a single model-thread.
   */
  @Test
  public void testBlockingConditionSingle() {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    final IFuture<Void> future1 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("1: running");

        if (Jobs.getJobManager().isDone(Jobs.newFutureFilterBuilder()
            .andMatchExecutionHint(JOB_IDENTIFIER)
            .toFilter())) {
          protocol.add("1: idle [a]");
        }
        if (IFuture.CURRENT.get().getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("1: blocked [a]");
        }
        if (ModelJobs.isModelThread()) {
          protocol.add("1: modelThread [a]");
        }

        protocol.add("1: beforeAwait");
        condition.waitFor();
        protocol.add("1: afterAwait");
        if (IFuture.CURRENT.get().getState() == JobState.RUNNING) {
          protocol.add("1: state=running");
        }

        if (Jobs.getJobManager().isDone(Jobs.newFutureFilterBuilder()
            .andMatchExecutionHint(JOB_IDENTIFIER)
            .toFilter())) {
          protocol.add("1: idle [b]");
        }
        if (IFuture.CURRENT.get().getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("1: blocked [b]");
        }
        if (ModelJobs.isModelThread()) {
          protocol.add("1: modelThread [b]");
        }
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withName("job-1")
        .withExecutionHint(JOB_IDENTIFIER)
        .withExceptionHandling(null, false));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("2: running");

        if (Jobs.getJobManager().isDone(Jobs.newFutureFilterBuilder()
            .andMatchExecutionHint(JOB_IDENTIFIER)
            .toFilter())) {
          protocol.add("2: idle [a]");
        }
        if (future1.getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("2: job-1-state: waiting-for-condition");
        }
        if (IFuture.CURRENT.get().getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("2: blocked [a]");
        }
        if (ModelJobs.isModelThread()) {
          protocol.add("2: modelThread [a]");
        }

        // RELEASE THE BlockingCondition
        protocol.add("2: beforeSignaling");
        condition.setBlocking(false);
        protocol.add("2: afterSignaling");

        // Wait until job1 is competing for the mutex anew
        JobTestUtil.waitForMutexCompetitors(ClientRunContexts.copyCurrent().getSession().getModelJobSchedulingSemaphore(), 2);

        if (future1.getState() == JobState.WAITING_FOR_PERMIT) {
          protocol.add("2: job-1-state: waiting-for-mutex");
        }

        if (Jobs.getJobManager().isDone(Jobs.newFutureFilterBuilder()
            .andMatchExecutionHint(JOB_IDENTIFIER)
            .toFilter())) {
          protocol.add("2: idle [b]");
        }
        if (IFuture.CURRENT.get().getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("2: blocked [b]");
        }

        if (ModelJobs.isModelThread()) {
          protocol.add("2: modelThread [b]");
        }
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withName("job-2")
        .withExecutionHint(JOB_IDENTIFIER)
        .withExceptionHandling(null, false));

    // Wait until job1 completed.
    future1.awaitDoneAndGet(30, TimeUnit.SECONDS);

    awaitDoneElseFail(JOB_IDENTIFIER);

    List<String> expected = new ArrayList<>();
    expected.add("1: running");
    expected.add("1: modelThread [a]");
    expected.add("1: beforeAwait");
    expected.add("2: running");
    expected.add("2: job-1-state: waiting-for-condition");
    expected.add("2: modelThread [a]");
    expected.add("2: beforeSignaling");
    expected.add("2: afterSignaling");
    expected.add("2: job-1-state: waiting-for-mutex");
    expected.add("2: modelThread [b]");
    expected.add("1: afterAwait");
    expected.add("1: state=running");
    expected.add("1: modelThread [b]");

    assertEquals(expected, protocol);
  }

  /**
   * Tests a BlockingCondition that blocks multiple model-threads.
   */
  @Test
  public void testBlockingConditionMultipleFlat() {
    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    // run the test 2 times to also test reusability of a blocking condition.
    runTestBlockingConditionMultipleFlat(condition);
    runTestBlockingConditionMultipleFlat(condition);
  }

  /**
   * Tests a BlockingCondition that blocks multiple model-threads that were scheduled as nested jobs.
   */
  @Test
  public void testBlockingConditionMultipleNested() {
    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    // run the test 2 times to also test reusability of a blocking condition.
    runTestBlockingCondition(condition);
    runTestBlockingCondition(condition);
  }

  /**
   * We have 3 jobs that are scheduled simultaneously. Thereby, job1 enters a blocking condition which in turn lets job2
   * run. Job2 goes to sleep forever, meaning that job3 will never start running. The test verifies, that when job1 is
   * interrupted, job3 must not be scheduled because the mutex-owner is still job2.
   */
  @Test
  public void testBlockingCondition_InterruptedWhileBeingBlocked() throws java.lang.InterruptedException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.
    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(1);

    final IFuture<Void> future1 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-1");

        try {
          condition.waitFor();
        }
        catch (InterruptedException e) {
          protocol.add("interrupted-1");

          if (!ModelJobs.isModelThread()) {
            protocol.add("non-model-thread-1");
          }
        }
        verifyLatch.countDown();
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-2");
        setupLatch.countDownAndBlock();
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExceptionHandling(null, false)
        .withExecutionHint(JOB_IDENTIFIER));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-3");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    setupLatch.await();
    assertEquals(future1.getState(), JobState.WAITING_FOR_BLOCKING_CONDITION);

    // RUN THE TEST
    future1.cancel(true);

    // VERIFY
    verifyLatch.await();
    assertFalse(Jobs.getJobManager().isDone(Jobs.newFutureFilterBuilder().andMatchExecutionHint(JOB_IDENTIFIER).toFilter()));
    try {
      Jobs.getJobManager().awaitDone(Jobs.newFutureFilterBuilder().andMatchExecutionHint(JOB_IDENTIFIER).toFilter(), 10, TimeUnit.MILLISECONDS);
      fail("timeout expected");
    }
    catch (TimeoutException e) {
      // NOOP
    }
    assertEquals(Arrays.asList("running-1", "running-2", "interrupted-1", "non-model-thread-1"), protocol);

    // clenaup
    setupLatch.unblock();
    awaitDoneElseFail(JOB_IDENTIFIER);
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
  public void testBlockingCondition_InterruptedWhileReAcquiringTheMutex() throws java.lang.InterruptedException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.
    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    final BlockingCountDownLatch latchJob2 = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch job1FinishLatch = new BlockingCountDownLatch(1);

    final IFuture<Void> future1 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-1");

        try {
          protocol.add("before-blocking-1");
          condition.waitFor();
          protocol.add("not-interrupted-1");
        }
        catch (InterruptedException e) {
          protocol.add("interrupted-1");
        }
        catch (RuntimeException e) {
          protocol.add("jobException-1");
        }

        if (!ModelJobs.isModelThread()) {
          protocol.add("not-model-thread-1");
        }

        protocol.add("done-1");
        job1FinishLatch.countDown();
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withName("job-1")
        .withExecutionHint(JOB_IDENTIFIER)
        .withExceptionHandling(null, false));

    final IFuture<Void> future2 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("running-2a");

        if (future1.getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("job2: job-1-waiting-for-blocking-condition");
        }

        protocol.add("unblocking condition");
        condition.setBlocking(false);

        JobTestUtil.waitForState(future1, JobState.WAITING_FOR_PERMIT);
        if (future1.getState() == JobState.WAITING_FOR_PERMIT) {
          protocol.add("job2: job-1-waiting-for-mutex");
        }

        protocol.add("before-cancel-job1-2");
        future1.cancel(true); // interrupt job1 while acquiring the mutex
        assertTrue(job1FinishLatch.await());
        protocol.add("running-2b");
        latchJob2.countDownAndBlock();
        protocol.add("done-2");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withName("job-2")
        .withExecutionHint(JOB_IDENTIFIER)
        .withExceptionHandling(null, false));

    final IFuture<Void> future3 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("done-3");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withName("job-3")
        .withExecutionHint(JOB_IDENTIFIER)
        .withExceptionHandling(null, false));

    assertTrue(latchJob2.await());
    JobTestUtil.waitForMutexCompetitors(m_clientSession.getModelJobSchedulingSemaphore(), 3); // job-1 (interrupted, but re-acquire mutex task still pending), job-2 (latch), job-3 (waiting for mutex)
    future1.awaitDone(1, TimeUnit.SECONDS);

    List<String> expectedProtocol = new ArrayList<>();
    expectedProtocol.add("running-1");
    expectedProtocol.add("before-blocking-1");
    expectedProtocol.add("running-2a");
    expectedProtocol.add("job2: job-1-waiting-for-blocking-condition");
    expectedProtocol.add("unblocking condition");
    expectedProtocol.add("job2: job-1-waiting-for-mutex");
    expectedProtocol.add("before-cancel-job1-2");
    expectedProtocol.add("interrupted-1");
    expectedProtocol.add("not-model-thread-1");
    expectedProtocol.add("done-1");
    expectedProtocol.add("running-2b");

    assertEquals(expectedProtocol, protocol);

    assertEquals(future1.getState(), JobState.DONE);
    assertEquals(future2.getState(), JobState.RUNNING);
    assertEquals(future3.getState(), JobState.WAITING_FOR_PERMIT);

    assertTrue(future1.isCancelled());
    try {
      future1.awaitDone();
      assertTrue(future1.isCancelled());
    }
    catch (ProcessingException e) {
      fail();
    }

    assertFalse(future2.isCancelled());
    try {
      future2.awaitDoneAndGet(1, TimeUnit.NANOSECONDS);
      fail("timeout expected");
    }
    catch (TimeoutException e) {
      // NOOP
    }

    assertFalse(future3.isCancelled());
    try {
      future3.awaitDoneAndGet(1, TimeUnit.NANOSECONDS);
      fail("timeout expected");
    }
    catch (TimeoutException e) {
      // NOOP
    }

    // let job2 finish its work so that job1 can re-acquire the mutex.
    latchJob2.unblock();
    awaitDoneElseFail(JOB_IDENTIFIER);

    expectedProtocol.add("done-2");
    expectedProtocol.add("done-3");
    assertEquals(expectedProtocol, protocol);
    try {
      assertNull(future2.awaitDoneAndGet(1, TimeUnit.NANOSECONDS));
    }
    catch (ProcessingException e) {
      e.printStackTrace();
      fail();
    }

    try {
      assertNull(future3.awaitDoneAndGet(1, TimeUnit.NANOSECONDS));
    }
    catch (ProcessingException e) {
      fail();
    }
  }

  /**
   * We have 3 jobs that get scheduled simultaneously. The first waits some time so that job2 and job3 get queued. When
   * job2 gets scheduled, it is rejected by the executor. This test verifies, that job2 still gets scheduled.
   */
  @Test
  public void testRejection() {
    P_JobManager jobManager = new P_JobManager();
    ExecutorService executorMock = jobManager.getExecutorMock();
    IBean<IJobManager> jobManagerBean = JobTestUtil.replaceCurrentJobManager(jobManager);
    try {
      final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

      // Executor mock
      doAnswer(new Answer<Future>() {

        @Override
        public Future answer(InvocationOnMock invocation) throws Throwable {
          final Runnable runnable = (Runnable) invocation.getArguments()[0];

          // Reject job-2 from being scheduled
          if (runnable instanceof JobFutureTask) {
            JobFutureTask job = (JobFutureTask) runnable;
            if ("job-2".equals(job.getJobInput().getName())) {
              job.reject();
              return null;
            }
          }

          s_executor.execute(runnable);
          return null;
        }
      }).when(executorMock).execute(any(Runnable.class));

      // Job-1
      final IFuture<Void> future1 = Jobs.schedule(new IRunnable() {

        @Override
        public void run() throws Exception {
          // Wait until all 3 jobs are scheduled.
          JobTestUtil.waitForMutexCompetitors(m_clientSession.getModelJobSchedulingSemaphore(), 3);

          protocol.add("running-job-1");
        }
      }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
          .withName("job-1")
          .withExecutionHint(JOB_IDENTIFIER));

      // Job-2
      final IFuture<Void> future2 = Jobs.schedule(new IRunnable() {

        @Override
        public void run() throws Exception {
          protocol.add("running-job-2");
        }
      }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
          .withName("job-2")
          .withExecutionHint(JOB_IDENTIFIER));

      // Job-3
      IFuture<Void> future3 = Jobs.schedule(new IRunnable() {

        @Override
        public void run() throws Exception {
          protocol.add("running-job-3");
        }
      }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
          .withName("job-3")
          .withExecutionHint(JOB_IDENTIFIER));

      awaitDoneElseFail(JOB_IDENTIFIER);
      assertEquals(Arrays.asList("running-job-1", "running-job-3"), protocol);
      assertTrue(Jobs.getJobManager().isDone(Jobs.newFutureFilterBuilder().andMatchExecutionHint(JOB_IDENTIFIER).toFilter()));
      assertFalse(future1.isCancelled());
      assertTrue(future2.isCancelled());
      assertFalse(future3.isCancelled());
    }
    finally {
      JobTestUtil.unregisterAndShutdownJobManager(jobManagerBean);
    }
  }

  /**
   * We have 5 jobs that get scheduled simultaneously. The first waits some time so that job2, job3, job4 and job5 get
   * queued. Job1 then enters a blocking condition, which allows job2 to run. But job2 gets rejected by the executor,
   * which allows job3 to run. After job3 completes, job1 is resumed and continues running. After job1 complete, job4
   * gets scheduled. Job4 in turn gets blocked, which prevents job5 from running.
   */
  @Test
  public void testBlockedJobs() throws java.lang.InterruptedException {
    P_JobManager jobManager = new P_JobManager();
    ExecutorService executorMock = jobManager.getExecutorMock();
    IBean<IJobManager> jobManagerBean = JobTestUtil.replaceCurrentJobManager(jobManager);
    try {
      final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

      // Executor mock
      doAnswer(new Answer<Future>() {

        @Override
        public Future answer(InvocationOnMock invocation) throws Throwable {
          final Runnable runnable = (Runnable) invocation.getArguments()[0];

          // Reject job-2 from being scheduled
          if (runnable instanceof JobFutureTask) {
            JobFutureTask job = (JobFutureTask) runnable;
            if ("job-2".equals(job.getJobInput().getName())) {
              job.reject();
              return null;
            }
          }

          s_executor.execute(runnable);
          return null;
        }
      }).when(executorMock).execute(any(Runnable.class));

      final BlockingCountDownLatch job4RunningLatch = new BlockingCountDownLatch(1);

      final IBlockingCondition condition = Jobs.newBlockingCondition(true);

      // Job-1
      IFuture<Void> future1 = Jobs.schedule(new IRunnable() {

        @Override
        public void run() throws Exception {
          // Wait until all 5 jobs are scheduled.
          JobTestUtil.waitForMutexCompetitors(m_clientSession.getModelJobSchedulingSemaphore(), 5);

          try {
            protocol.add("running-job-1 (a)");
            condition.waitFor();
            protocol.add("running-job-1 (b)");
          }
          catch (ProcessingException e) {
            protocol.add("jobException");
          }

          if (ModelJobs.isModelThread()) {
            protocol.add("running-job-1 (e) [model-thread]");
          }
        }
      }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
          .withName("job-1")
          .withExecutionHint(JOB_IDENTIFIER)
          .withExceptionHandling(null, false));

      // Job-2
      IFuture<Void> future2 = Jobs.schedule(new IRunnable() {

        @Override
        public void run() throws Exception {
          protocol.add("running-job-2");
        }

      }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
          .withName("job-2")
          .withExecutionHint(JOB_IDENTIFIER)
          .withExceptionHandling(null, false));

      // Job-3
      IFuture<Void> future3 = Jobs.schedule(new IRunnable() {

        @Override
        public void run() throws Exception {
          protocol.add("running-job-3 (a)");

          condition.setBlocking(false);

          // Wait until job-1 tried to re-acquire the mutex.
          JobTestUtil.waitForMutexCompetitors(m_clientSession.getModelJobSchedulingSemaphore(), 4); // 4 = job1(re-acquiring), job3(owner), job4, job5
          protocol.add("running-job-3 (b)");

        }
      }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
          .withName("job-3")
          .withExecutionHint(JOB_IDENTIFIER)
          .withExceptionHandling(null, false));

      // Job-4
      IFuture<Void> future4 = Jobs.schedule(new IRunnable() {

        @Override
        public void run() throws Exception {
          protocol.add("running-job-4");

          try {
            job4RunningLatch.countDownAndBlock();
          }
          catch (java.lang.InterruptedException e) {
            protocol.add("job-4 [interrupted]");
          }
        }
      }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
          .withName("job-4")
          .withExecutionHint(JOB_IDENTIFIER)
          .withExceptionHandling(null, false));

      // Job-5
      IFuture<Void> future5 = Jobs.schedule(new IRunnable() {

        @Override
        public void run() throws Exception {
          protocol.add("running-job-5");
        }
      }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
          .withName("job-5")
          .withExecutionHint(JOB_IDENTIFIER)
          .withExceptionHandling(null, false));

      assertTrue(job4RunningLatch.await());
      try {
        Jobs.getJobManager().awaitDone(Jobs.newFutureFilterBuilder().andMatchExecutionHint(JOB_IDENTIFIER).toFilter(), 1, TimeUnit.MILLISECONDS);
        // job-4 and job-5 are pending
        fail("timeout expected");
      }
      catch (TimeoutException e) {
        // NOOP
      }
      assertFalse(Jobs.getJobManager().isDone(Jobs.newFutureFilterBuilder().andMatchExecutionHint(JOB_IDENTIFIER).toFilter())); // job-4 and job-5 are pending

      List<String> expectedProtocol = new ArrayList<>();
      expectedProtocol.add("running-job-1 (a)");
      expectedProtocol.add("running-job-3 (a)");
      expectedProtocol.add("running-job-3 (b)");
      expectedProtocol.add("running-job-1 (b)");
      expectedProtocol.add("running-job-1 (e) [model-thread]");
      expectedProtocol.add("running-job-4");
      assertEquals(expectedProtocol, protocol);

      assertFalse(future1.isCancelled());
      assertTrue(future1.isDone());

      assertTrue(future2.isCancelled());
      assertTrue(future2.isDone());

      assertFalse(future3.isCancelled());
      assertTrue(future3.isDone());

      assertFalse(future4.isCancelled());
      assertFalse(future4.isDone());

      assertFalse(future5.isCancelled());
      assertFalse(future5.isDone());

      // cancel job4
      future4.cancel(true);
      awaitDoneElseFail(JOB_IDENTIFIER);

      expectedProtocol.add("job-4 [interrupted]");
      expectedProtocol.add("running-job-5");
      assertEquals(expectedProtocol, protocol);

      assertTrue(Jobs.getJobManager().isDone(Jobs.newFutureFilterBuilder().andMatchExecutionHint(JOB_IDENTIFIER).toFilter()));

      assertTrue(future4.isCancelled());
      assertTrue(future4.isDone());

      assertFalse(future5.isCancelled());
      assertTrue(future5.isDone());
    }
    finally {
      JobTestUtil.unregisterAndShutdownJobManager(jobManagerBean);
    }
  }

  private void runTestBlockingConditionMultipleFlat(final IBlockingCondition condition) {
    condition.setBlocking(true);

    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final IFuture<Void> future1 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-1-beforeAwait");
        condition.waitFor();
        protocol.add("job-X-afterAwait");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER)
        .withName("job-1"));

    final IFuture<Void> future2 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-2-beforeAwait");
        condition.waitFor();
        protocol.add("job-X-afterAwait");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER)
        .withName("job-2"));

    final IFuture<Void> future3 = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-3-beforeAwait");
        condition.waitFor();
        protocol.add("job-X-afterAwait");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER)
        .withName("job-3"));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-4-running");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER)
        .withName("job-4"));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job-5-running");
        if (future1.getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("job-1-blocked");
        }
        if (future2.getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("job-2-blocked");
        }
        if (future3.getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
          protocol.add("job-3-blocked");
        }

        protocol.add("job-5-signaling");
        condition.setBlocking(false);

        // Wait until the other jobs tried to re-acquire the mutex.
        JobTestUtil.waitForMutexCompetitors(m_clientSession.getModelJobSchedulingSemaphore(), 4);

        if (future1.getState() == JobState.WAITING_FOR_PERMIT) {
          protocol.add("job-1-unblocked");
        }
        if (future2.getState() == JobState.WAITING_FOR_PERMIT) {
          protocol.add("job-2-unblocked");
        }
        if (future3.getState() == JobState.WAITING_FOR_PERMIT) {
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
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER)
        .withName("job-5"));

    awaitDoneElseFail(JOB_IDENTIFIER);

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

  private void runTestBlockingCondition(final IBlockingCondition condition) {
    condition.setBlocking(true);

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

            if (iFuture1.getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
              protocol.add("job-1-blocked");
            }

            ModelJobs.schedule(new IRunnable() {

              @Override
              public void run() throws Exception {
                protocol.add("job-3-running");

                if (iFuture1.getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
                  protocol.add("job-1-blocked");
                }
                if (iFuture2.getState() == JobState.WAITING_FOR_BLOCKING_CONDITION) {
                  protocol.add("job-2-blocked");
                }

                ModelJobs.schedule(new IRunnable() {

                  @Override
                  public void run() throws Exception {
                    protocol.add("job-4-running");
                  }
                }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
                    .withExecutionHint(JOB_IDENTIFIER));

                protocol.add("job-3-before-signaling");
                condition.setBlocking(false);

                JobTestUtil.waitForMutexCompetitors(m_clientSession.getModelJobSchedulingSemaphore(), 4); // Wait for the other jobs to have tried re-acquiring the mutex.

                protocol.add("job-3-after-signaling");

                ModelJobs.schedule(new IRunnable() {

                  @Override
                  public void run() throws Exception {
                    protocol.add("job-5-running");
                  }
                }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
                    .withExecutionHint(JOB_IDENTIFIER));
              }
            }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
                .withExecutionHint(JOB_IDENTIFIER));

            protocol.add("job-2-beforeAwait");
            condition.waitFor();
            protocol.add("JOB-X-AFTERAWAIT");
          }
        }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
            .withExecutionHint(JOB_IDENTIFIER));

        protocol.add("job-1-beforeAwait");
        condition.waitFor();
        protocol.add("JOB-X-AFTERAWAIT");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));
    awaitDoneElseFail(JOB_IDENTIFIER);

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
  public void testBlockingConditionNotBlocking() {
    final IBlockingCondition condition = Jobs.newBlockingCondition(false);
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        condition.waitFor();
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        condition.waitFor();
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    awaitDoneElseFail(JOB_IDENTIFIER);
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

    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    final AtomicReference<Throwable> throwableHolder = new AtomicReference<>();
    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        s_executor.execute(new Runnable() {

          @Override
          public void run() {
            try {
              condition.setBlocking(false);
              protocol.add("1: afterUnblock [inner]");
              unblockedLatch.countDown();
              done.await();
              protocol.add("4: done");
            }
            catch (final Throwable t) {
              throwableHolder.set(t);
            }
          }
        });

        assertTrue(unblockedLatch.await()); // wait until the condition in unblocked

        protocol.add("2: beforeWaitFor [outer]");
        condition.waitFor();
        protocol.add("3: afterWaitFor [outer]");
        done.release();
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    awaitDoneElseFail(JOB_IDENTIFIER);

    Throwable throwable = throwableHolder.get();
    if (throwable != null) {
      throw throwable;
    }

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
  public void testReuseUnblockedBlockingCondition() {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("1: beforeWaitFor");
        condition.waitFor();
        protocol.add("3: afterWaitFor");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExecutionHint(JOB_IDENTIFIER));

    JobTestUtil.waitForMutexCompetitors(m_clientSession.getModelJobSchedulingSemaphore(), 0); // Wait until job1 is blocked
    assertTrue(condition.isBlocking());
    protocol.add("2: setBlocking=false");
    condition.setBlocking(false);
    assertFalse(condition.isBlocking());
    awaitDoneElseFail(JOB_IDENTIFIER);

    ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("4: beforeWaitFor");
        condition.waitFor();
        protocol.add("5: afterWaitFor");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent()))
        .awaitDoneAndGet();

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
  public void testExpiredWhenReAcquiringMutex() {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final IBlockingCondition condition = Jobs.newBlockingCondition(true);

    IFuture<Void> future = ModelJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("1: running");
        ModelJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            protocol.add("2: running");
            condition.setBlocking(false);
          }
        }, ModelJobs.newInput(ClientRunContexts.copyCurrent()));
        condition.waitFor();
        protocol.add("3: running");
      }
    }, ModelJobs.newInput(ClientRunContexts.copyCurrent())
        .withExceptionHandling(null, false)
        .withExpirationTime(100, TimeUnit.MILLISECONDS));

    // Wait until entering blocking condition
    JobTestUtil.waitForMutexCompetitors(m_clientSession.getModelJobSchedulingSemaphore(), 0);

    // Expect the job to continue running.
    try {
      future.awaitDoneAndGet();

      List<String> expected = new ArrayList<>();
      expected.add("1: running");
      expected.add("2: running");
      expected.add("3: running");
      assertEquals(expected, protocol);
    }
    catch (ProcessingException e) {
      fail();
    }
  }

  @Replace
  @IgnoreBean
  private static class P_JobManager extends JobManager {

    @Override
    protected ExecutorService createExecutor() {
      return mock(ExecutorService.class);
    }

    public ExecutorService getExecutorMock() {
      return m_executor;
    }
  }

  private static void awaitDoneElseFail(String executionHint) {
    Jobs.getJobManager().awaitDone(Jobs.newFutureFilterBuilder()
        .andMatchExecutionHint(executionHint)
        .toFilter(), 10, TimeUnit.SECONDS);
  }
}
