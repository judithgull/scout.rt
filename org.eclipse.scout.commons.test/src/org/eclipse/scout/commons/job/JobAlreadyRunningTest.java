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
package org.eclipse.scout.commons.job;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JobAlreadyRunningTest {

  private JobManager m_jobManager;
  private static ExecutorService s_executor;

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
    m_jobManager = new JobManager();
  }

  @After
  public void after() {
    m_jobManager.shutdown();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAlreadyRunning() throws ProcessingException {
    final List<String> protocol = new ArrayList<>();
    // This job runs forever.
    Job<Void> job1 = new Job_<Void>("job-1") {

      @Override
      protected void onRunVoid(IProgressMonitor monitor) throws Exception {
        protocol.add("running-1");
        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
      }
    };
    job1.schedule();

    // Try to schedule job again.
    try {
      job1.schedule();
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isRejection());
      assertFalse(e.isCancellation());
      assertFalse(e.isInterruption());
      assertFalse(e.isTimeout());
    }

    // Try to schedule job again (with AsyncFuture)
    try {
      job1.schedule(mock(IAsyncFuture.class));
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isRejection());
      assertFalse(e.isCancellation());
      assertFalse(e.isInterruption());
      assertFalse(e.isTimeout());
    }

    // Try to schedule job again (with delay)
    try {
      job1.schedule(1, TimeUnit.SECONDS);
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isRejection());
      assertFalse(e.isCancellation());
      assertFalse(e.isInterruption());
      assertFalse(e.isTimeout());
    }

    // Try to schedule job again (with delay and AsyncFuture)
    try {
      job1.schedule(1, TimeUnit.SECONDS, mock(IAsyncFuture.class));
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isRejection());
      assertFalse(e.isCancellation());
      assertFalse(e.isInterruption());
      assertFalse(e.isTimeout());
    }

    // Try to schedule job again (fixed rate)
    try {
      job1.scheduleAtFixedRate(0, 0, TimeUnit.SECONDS);
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isRejection());
      assertFalse(e.isCancellation());
      assertFalse(e.isInterruption());
      assertFalse(e.isTimeout());
    }

    // Try to schedule job again (fixed delay)
    try {
      job1.scheduleWithFixedDelay(0, 0, TimeUnit.SECONDS);
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isRejection());
      assertFalse(e.isCancellation());
      assertFalse(e.isInterruption());
      assertFalse(e.isTimeout());
    }

    // Try to run job again (with AsyncFuture)
    try {
      job1.runNow();
      fail();
    }
    catch (JobExecutionException e) {
      assertTrue(e.isRejection());
      assertFalse(e.isCancellation());
      assertFalse(e.isInterruption());
      assertFalse(e.isTimeout());
    }
  }

  /**
   * Job with a dedicated {@link JobManager} per test-case.
   */
  public class Job_<R> extends Job<R> {

    public Job_(String name) {
      super(name);
    }

    @Override
    protected JobManager createJobManager() {
      return JobAlreadyRunningTest.this.m_jobManager;
    }
  }
}