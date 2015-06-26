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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.scout.commons.Assertions.AssertionException;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.shared.ISession;
import org.eclipse.scout.rt.testing.commons.BlockingCountDownLatch;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(PlatformTestRunner.class)
public class ClientJobTest {

  @Mock
  private IClientSession m_clientSession1;
  @Mock
  private IClientSession m_clientSession2;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void after() {
    ISession.CURRENT.remove();
  }

  @Test(expected = AssertionException.class)
  public void testNoSession() throws ProcessingException {
    ISession.CURRENT.remove();
    ClientJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
      }
    }).awaitDoneAndGet();
  }

  @Test
  public void testNotModelThread() throws ProcessingException {
    final AtomicBoolean modelThread = new AtomicBoolean();

    assertFalse(ModelJobs.isModelThread());

    ClientJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        modelThread.set(ModelJobs.isModelThread());
      }
    }, ClientJobs.newInput(ClientRunContexts.empty().session(m_clientSession1))).awaitDoneAndGet();

    assertFalse(ModelJobs.isModelThread());
    assertFalse(modelThread.get());
  }

  @Test
  public void testThreadName() throws ProcessingException, InterruptedException {
    ISession.CURRENT.set(m_clientSession1);
    Thread.currentThread().setName("main");

    final Holder<String> actualThreadName1 = new Holder<>();
    final Holder<String> actualThreadName2 = new Holder<>();

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(2);

    ClientJobs.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        actualThreadName1.setValue(Thread.currentThread().getName());
        setupLatch.countDown();

        ClientJobs.schedule(new IRunnable() {

          @Override
          public void run() throws Exception {
            actualThreadName2.setValue(Thread.currentThread().getName());
            setupLatch.countDown();
          }
        }, ClientJobs.newInput(ClientRunContexts.copyCurrent()).name("XYZ"));
      }
    }, ClientJobs.newInput(ClientRunContexts.copyCurrent()).name("ABC"));

    assertTrue(setupLatch.await());

    assertTrue(actualThreadName1.getValue().matches("scout-client-thread-(\\d)+ \\[Running\\] ABC"));
    assertTrue(actualThreadName2.getValue().matches("scout-client-thread-(\\d)+ \\[Running\\] XYZ"));
    assertEquals("main", Thread.currentThread().getName());
  }
}