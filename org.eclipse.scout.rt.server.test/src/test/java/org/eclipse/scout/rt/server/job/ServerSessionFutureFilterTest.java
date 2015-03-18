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
package org.eclipse.scout.rt.server.job;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.JobInput;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(PlatformTestRunner.class)
public class ServerSessionFutureFilterTest {

  @Mock
  private IFuture<Object> m_future;
  @Mock
  private IServerSession m_session1;
  @Mock
  private IServerSession m_session2;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void test1() {
    ServerJobInput input = ServerJobInput.empty().setSessionRequired(false).setSession(null);
    when(m_future.getJobInput()).thenReturn(input);

    assertTrue(new ServerSessionFutureFilter(null).accept(m_future));
  }

  @Test
  public void test2() {
    ServerJobInput input = ServerJobInput.empty().setSessionRequired(false).setSession(null);
    when(m_future.getJobInput()).thenReturn(input);

    assertFalse(new ServerSessionFutureFilter(m_session1).accept(m_future));
  }

  @Test
  public void test3() {
    ServerJobInput input = ServerJobInput.empty().setSession(m_session1);
    when(m_future.getJobInput()).thenReturn(input);

    assertFalse(new ServerSessionFutureFilter(m_session2).accept(m_future));
  }

  @Test
  public void test4() {
    ServerJobInput input = ServerJobInput.empty().setSession(m_session1);
    when(m_future.getJobInput()).thenReturn(input);

    assertTrue(new ServerSessionFutureFilter(m_session1).accept(m_future));
  }

  @Test
  public void test5() {
    JobInput input = JobInput.empty();
    when(m_future.getJobInput()).thenReturn(input);

    assertFalse(new ServerSessionFutureFilter(m_session1).accept(m_future));
  }
}