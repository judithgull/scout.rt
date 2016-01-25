/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

import org.eclipse.scout.rt.client.ClientConfigProperties.JobCompletionDelayOnSessionShutdown;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.BeanMetaData;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IBeanInstanceProducer;
import org.eclipse.scout.rt.testing.platform.runner.JUnitExceptionHandler;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.eclipse.scout.rt.ui.html.json.JsonAdapterRegistry;
import org.eclipse.scout.rt.ui.html.json.JsonEvent;
import org.eclipse.scout.rt.ui.html.json.testing.JsonTestUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/*
 * This test must be executed by a bare JUnit runner.
 * Reason: The PlatformTestRunner and its sub classes keep track of every job scheduled during test execution and verify that they are completed. The list of scheduled jobs
 *         are referencing a JobInput which in turn references a RunContext and a session. The tests in this class will fail because they assert that the sessions are
 *         not referenced by any other object and therefore garbage collected.
 */
public class UiSessionTest {

  private List<IBean<?>> m_beans;

  @Before
  public void before() {
    m_beans = TestingUtility.registerBeans(new BeanMetaData(JobCompletionDelayOnSessionShutdown.class).withProducer(new IBeanInstanceProducer<JobCompletionDelayOnSessionShutdown>() {

      @Override
      public JobCompletionDelayOnSessionShutdown produce(IBean<JobCompletionDelayOnSessionShutdown> bean) {
        return new JobCompletionDelayOnSessionShutdown() {

          @Override
          protected Long getDefaultValue() {
            return 0L;
          }
        };
      }
    }));
  }

  @After
  public void after() {
    TestingUtility.unregisterBeans(m_beans);
  }

  @Test
  public void testDispose() throws Exception {
    UiSession session = (UiSession) JsonTestUtility.createAndInitializeUiSession();
    WeakReference<IUiSession> ref = new WeakReference<IUiSession>(session);

    JsonTestUtility.endRequest(session);
    session.dispose();
    assertTrue(session.isDisposed());
    session = null;
    TestingUtility.assertGC(ref);
  }

  @Test
  public void testLogout() throws Exception {
    IUiSession uiSession = JsonTestUtility.createAndInitializeUiSession();
    HttpSession httpSession = uiSession.currentHttpSession();

    uiSession.logout();
    List<JsonEvent> responseEvents = JsonTestUtility.extractEventsFromResponse(uiSession.currentJsonResponse(), "logout");
    JsonTestUtility.endRequest(uiSession);

    Mockito.verify(httpSession).invalidate();
    assertEquals(1, responseEvents.size());
  }

  // XXX
  @Test
  public void testSessionInvalidation() throws Exception {
    UiSession uiSession = (UiSession) JsonTestUtility.createAndInitializeUiSession();
    IClientSession clientSession = uiSession.getClientSession();
    assertFalse(uiSession.isDisposed());

    // Don't waste time waiting for client jobs to finish. Test job itself runs inside a client job so we always have to wait until max time
    WeakReference<IUiSession> ref = new WeakReference<IUiSession>(uiSession);
    HttpSessionBindingEvent mockEvent = Mockito.mock(HttpSessionBindingEvent.class);
    SessionStore sessionStore = BEANS.get(HttpSessionHelper.class).getOrCreateSessionStore(uiSession.currentHttpSession());
    sessionStore.putClientSession(clientSession);
    sessionStore.putUiSession(uiSession);

    JsonTestUtility.endRequest(uiSession);
    sessionStore.valueUnbound(mockEvent);
    BEANS.get(UiJobs.class).awaitModelJobs(clientSession, JUnitExceptionHandler.class);
    assertFalse(clientSession.isActive());
    assertTrue(uiSession.isDisposed());

    uiSession = null;
    TestingUtility.assertGC(ref);
  }

  /**
   * helper method for unit tests to access protected method "getJsonAdapterRegistry"
   */
  public static JsonAdapterRegistry getJsonAdapterRegistry(UiSession session) {
    return session.getJsonAdapterRegistry();
  }
}
