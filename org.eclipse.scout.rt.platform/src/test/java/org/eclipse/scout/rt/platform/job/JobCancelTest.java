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
package org.eclipse.scout.rt.platform.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.context.RunMonitor;
import org.eclipse.scout.rt.platform.job.internal.JobManager;
import org.eclipse.scout.rt.testing.commons.BlockingCountDownLatch;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.eclipse.scout.rt.testing.platform.runner.Times;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class JobCancelTest {

  private JobManager m_jobManager;
  private static ScheduledExecutorService s_executor;

  @BeforeClass
  public static void beforeClass() {
    s_executor = Executors.newScheduledThreadPool(5);
  }

  @AfterClass
  public static void afterClass() {
    s_executor.shutdown();
  }

  @Before
  public void before() {
    m_jobManager = new JobManager();
  }

  @After
  public void after() {
    m_jobManager.shutdown();
  }

  @Test
  public void testCancelSoft() throws ProcessingException, InterruptedException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(1);

    IFuture<Void> future = m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        if (RunMonitor.CURRENT.get().isCancelled()) {
          protocol.add("cancelled-before");
        }

        try {
          setupLatch.countDownAndBlock(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
          protocol.add("interrupted");
        }

        if (RunMonitor.CURRENT.get().isCancelled()) {
          protocol.add("cancelled-after");
        }

        verifyLatch.countDown();
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()));

    assertTrue(setupLatch.await());

    // RUN THE TEST
    assertTrue(future.cancel(false /* soft */));
    assertTrue(verifyLatch.await());

    // VERIFY
    assertEquals(Arrays.asList("cancelled-after"), protocol);
    assertTrue(future.isCancelled());

    assertTrue(future.awaitDone(1, TimeUnit.SECONDS));
    assertTrue(future.isCancelled());
  }

  @Test
  public void testCancelForce() throws ProcessingException, InterruptedException {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(1);

    IFuture<Void> future = m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        if (RunMonitor.CURRENT.get().isCancelled()) {
          protocol.add("cancelled-before");
        }

        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("interrupted");
        }

        if (RunMonitor.CURRENT.get().isCancelled()) {
          protocol.add("cancelled-after");
        }

        verifyLatch.countDown();
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).logOnError(false));

    assertTrue(setupLatch.await());

    // RUN THE TEST
    assertTrue(future.cancel(true /* force */));
    assertTrue(verifyLatch.await());

    // VERIFY
    assertTrue(future.isCancelled());
    assertEquals(Arrays.asList("interrupted", "cancelled-after"), protocol);

    assertTrue(future.awaitDone(5, TimeUnit.SECONDS));
    assertTrue(future.isCancelled());
  }

  @Test
  public void testCancelBeforeRunning() throws Exception {
    final List<String> protocol = Collections.synchronizedList(new ArrayList<String>()); // synchronized because modified/read by different threads.

    IFuture<Void> future = m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        protocol.add("running");
        return null;
      }
    }, 500, TimeUnit.MILLISECONDS, Jobs.newInput(RunContexts.copyCurrent()));

    // RUN THE TEST
    future.cancel(true);
    Thread.sleep(TimeUnit.MILLISECONDS.toMillis(600)); // Wait some time so that the job could be scheduled (should not happen).

    // VERIFY
    assertTrue(future.isCancelled());
    assertTrue(protocol.isEmpty());

    assertTrue(future.awaitDone(10, TimeUnit.SECONDS));
    assertTrue(future.isCancelled());
  }

  @Test
  public void testCancelPeriodicAction() throws Exception {
    final AtomicInteger count = new AtomicInteger();

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(1);

    final IFuture<Void> future = m_jobManager.scheduleAtFixedRate(new IRunnable() {

      @Override
      public void run() throws Exception {
        if (count.incrementAndGet() == 3) {
          setupLatch.countDown();
          verifyLatch.await();
        }
      }
    }, 10L, 10L, TimeUnit.MILLISECONDS, Jobs.newInput(RunContexts.empty()).logOnError(false));

    assertTrue(setupLatch.await());

    // RUN THE TEST
    future.cancel(false);
    verifyLatch.countDown();

    // VERIFY
    Thread.sleep(TimeUnit.SECONDS.toMillis(1)); // Wait some time so that the job could be rescheduled.  (should not happen).
    assertTrue(future.isCancelled());
    assertEquals(3, count.get());

    assertTrue(future.awaitDone(10, TimeUnit.SECONDS));
    assertTrue(future.isCancelled());
  }

  @Test
  public void testShutdownJobManagerAndSchedule() throws InterruptedException, ProcessingException {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch latch = new BlockingCountDownLatch(2);

    m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        protocol.add("running-1");
        try {
          latch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("interrupted-1");
        }
        finally {
          protocol.add("done-1");
        }
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).logOnError(false));

    IFuture<Void> future2 = m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        protocol.add("running-2");
        try {
          latch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("interrupted-2");
        }
        finally {
          protocol.add("done-2");
        }
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).logOnError(false));

    assertTrue(latch.await());

    // SHUTDOWN
    m_jobManager.shutdown();

    IFuture<Void> future3 = m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        protocol.add("running-3");
        try {
          latch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("running-3");
        }
        finally {
          protocol.add("done-3");
        }
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).logOnError(false));

    // VERIFY
    assertEquals(CollectionUtility.hashSet("running-1", "running-2", "interrupted-1", "interrupted-2", "done-1", "done-2"), protocol);
    assertTrue(future3.isCancelled());

    assertTrue(future2.awaitDone(1, TimeUnit.SECONDS));
    assertTrue(future2.isCancelled());
  }

  /**
   * Cancel of a job that has a child job.
   */
  @Test
  public void testCancelChildJob() throws Exception {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>());

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(2);
    final BlockingCountDownLatch job2DoneLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(2);

    final AtomicReference<IFuture<?>> childFutureRef = new AtomicReference<>();

    m_jobManager.schedule(new Callable<Void>() {
      @Override
      public Void call() throws Exception {

        //attach to child runmonitor -> nested cancel
        IFuture<?> childFuture = m_jobManager.schedule(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try {
              setupLatch.countDownAndBlock();
            }
            catch (InterruptedException e) {
              protocol.add("job-2-interrupted");
            }
            if (IFuture.CURRENT.get().isCancelled()) {
              protocol.add("job-2-cancelled (future)");
            }
            if (RunMonitor.CURRENT.get().isCancelled()) {
              protocol.add("job-2-cancelled (monitor)");
            }
            job2DoneLatch.countDown();
            verifyLatch.countDown();
            return null;
          }
        }, Jobs.newInput(RunContexts.copyCurrent()).name("job-2").logOnError(false));
        childFutureRef.set(childFuture);

        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job-1-interrupted");
        }
        if (IFuture.CURRENT.get().isCancelled()) {
          protocol.add("job-1-cancelled (future)");
        }
        if (RunMonitor.CURRENT.get().isCancelled()) {
          protocol.add("job-1-cancelled (monitor)");
        }
        verifyLatch.countDown();
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).name("job-1"));

    assertTrue(setupLatch.await());
    childFutureRef.get().cancel(true);
    assertTrue(job2DoneLatch.await());
    setupLatch.unblock();
    assertTrue(verifyLatch.await());

    assertEquals(CollectionUtility.hashSet(
        "job-2-interrupted",
        "job-2-cancelled (future)",
        "job-2-cancelled (monitor)"
        ), protocol);
  }

  /**
   * Cancel of a job that has a nested job.
   */
  @Test
  public void testCancelParentJob() throws Exception {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>());

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(3);
    final BlockingCountDownLatch job1DoneLatch = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(3);

    IFuture<Void> future1 = m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        //re-use runmonitor -> nested cancel
        m_jobManager.schedule(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try {
              setupLatch.countDownAndBlock();
            }
            catch (InterruptedException e) {
              protocol.add("job-2-interrupted");
            }
            if (IFuture.CURRENT.get().isCancelled()) {
              protocol.add("job-2-cancelled (future)");
            }
            if (RunMonitor.CURRENT.get().isCancelled()) {
              protocol.add("job-2-cancelled (monitor)");
            }
            verifyLatch.countDown();
            return null;
          }
        }, Jobs.newInput(RunContexts.copyCurrent()).name("job-2").logOnError(false));

        //does not re-use runmonitor -> no nested cancel
        m_jobManager.schedule(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try {
              setupLatch.countDownAndBlock();
            }
            catch (InterruptedException e) {
              protocol.add("job-3-interrupted");
            }
            if (IFuture.CURRENT.get().isCancelled()) {
              protocol.add("job-3-cancelled (future)");
            }
            if (RunMonitor.CURRENT.get().isCancelled()) {
              protocol.add("job-3-cancelled (monitor)");
            }
            verifyLatch.countDown();
            return null;
          }
        }, Jobs.newInput(RunContexts.copyCurrent().runMonitor(new RunMonitor())).name("job-3").logOnError(false));

        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job-1-interrupted");
        }
        if (IFuture.CURRENT.get().isCancelled()) {
          protocol.add("job-1-cancelled (future)");
        }
        if (RunMonitor.CURRENT.get().isCancelled()) {
          protocol.add("job-1-cancelled (monitor)");
        }
        job1DoneLatch.countDown();
        verifyLatch.countDown();
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).name("job-1"));

    assertTrue(setupLatch.await());
    future1.cancel(true);
    assertTrue(job1DoneLatch.await());
    setupLatch.unblock();
    assertTrue(verifyLatch.await());

    assertEquals(CollectionUtility.hashSet(
        "job-1-interrupted",
        "job-1-cancelled (future)",
        "job-1-cancelled (monitor)",
        "job-2-interrupted",
        "job-2-cancelled (future)",
        "job-2-cancelled (monitor)"
        ), protocol);
  }

  /**
   * Cancel multiple jobs with the same job-id.
   */
  @Test
  @Times(20)
  public void testCancelMultipleJobsByName() throws Exception {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>());

    final String commonJobName = "777";

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(6);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(4);

    // Job-1 (common-id) => CANCEL
    m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job-1-interrupted");
        }
        verifyLatch.countDown();
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).name(commonJobName).mutex(null).logOnError(false));

    // Job-2 (common-id) => CANCEL
    m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job-2-interrupted");
        }
        verifyLatch.countDown();
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).name(commonJobName).mutex(null).logOnError(false));

    // Job-3 (common-id) => CANCEL
    m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        // Job-3a (other name, same re-used runMonitor => CANCEL AS WELL)
        m_jobManager.schedule(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try {
              setupLatch.countDownAndBlock();
            }
            catch (InterruptedException e) {
              protocol.add("job-3a-interrupted");
            }
            verifyLatch.countDown();
            return null;
          }
        }, Jobs.newInput(RunContexts.copyCurrent()).name("otherName").mutex(null).logOnError(false));

        // Job-3b (other name, other runMonitor => NO CANCEL)
        m_jobManager.schedule(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try {
              setupLatch.countDownAndBlock();
            }
            catch (InterruptedException e) {
              protocol.add("job-3b-interrupted");
            }
            return null;
          }
        }, Jobs.newInput(RunContexts.copyCurrent().runMonitor(new RunMonitor())).name("otherName").mutex(null).logOnError(false));

        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job-3-interrupted");
        }
        verifyLatch.countDown();
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).name(commonJobName).mutex(null));

    // Job-4 (common-id, but not-null mutex)
    m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job-4-interrupted");
        }
        return null;
      }
    }, Jobs.newInput(RunContexts.copyCurrent()).name(commonJobName).mutex(new Object()).logOnError(false));

    assertTrue(setupLatch.await());
    m_jobManager.cancel(Jobs.newFutureFilter().andMatchNames(commonJobName).andMatchMutex(null), true);
    assertTrue(verifyLatch.await());

    assertEquals(CollectionUtility.hashSet("job-1-interrupted", "job-2-interrupted", "job-3-interrupted", "job-3a-interrupted"), protocol);
    setupLatch.unblock(); // release not cancelled jobs
  }

  /**
   * Tests that a job is not run if the RunMonitor is already cancelled.
   */
  @Test
  public void testCancelRunContextPriorSchedulingJob() throws ProcessingException {
    RunContext runContext = RunContexts.copyCurrent();

    // 1. Cancel the RunMonitor
    runContext.runMonitor().cancel(false);

    // 2. Schedule the job (should never)
    final AtomicBoolean executed = new AtomicBoolean(false);
    IFuture<Void> future = Jobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        executed.set(true);
      }
    }, Jobs.newInput(runContext));

    future.awaitDone();
    assertFalse(executed.get());
    assertTrue(future.isCancelled());
  }
}