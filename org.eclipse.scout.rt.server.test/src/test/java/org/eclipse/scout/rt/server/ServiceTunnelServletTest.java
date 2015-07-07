/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.BeanMetaData;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.server.commons.cache.ICacheEntry;
import org.eclipse.scout.rt.server.commons.cache.StickySessionCacheService;
import org.eclipse.scout.rt.server.commons.context.ServletRunContexts;
import org.eclipse.scout.rt.server.context.ServerRunContext;
import org.eclipse.scout.rt.server.context.ServerRunContexts;
import org.eclipse.scout.rt.server.services.common.security.AbstractAccessControlService;
import org.eclipse.scout.rt.server.session.ServerSessionProvider;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.rt.testing.platform.runner.RunWithSubject;
import org.eclipse.scout.rt.testing.server.runner.RunWithServerSession;
import org.eclipse.scout.rt.testing.server.runner.ServerTestRunner;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test for {@link ServiceTunnelServlet}
 */
@RunWith(ServerTestRunner.class)
@RunWithServerSession(TestServerSession.class)
@RunWithSubject("default")
public class ServiceTunnelServletTest {

  private static final int TEST_SERVICE_ORDER = -1000;

  private List<IBean<?>> m_beans;

  private ServiceTunnelServlet m_testServiceTunnelServlet;
  private HttpServletRequest m_requestMock;
  private HttpServletResponse m_responseMock;
  private HttpSession m_testHttpSession;

  private ServerSessionProvider m_serverSessionProviderSpy;

  @Before
  public void before() throws ServletException, InstantiationException, IllegalAccessException {
    m_serverSessionProviderSpy = spy(BEANS.get(ServerSessionProvider.class));

    m_beans = TestingUtility.registerBeans(
        new BeanMetaData(StickySessionCacheService.class).
        order(TEST_SERVICE_ORDER).
        applicationScoped(true),
        new BeanMetaData(IAccessControlService.class).
        initialInstance(new AbstractAccessControlService() {
        }).
        order(TEST_SERVICE_ORDER).
        applicationScoped(true),
        new BeanMetaData(ServerSessionProvider.class).
        initialInstance(m_serverSessionProviderSpy).
        order(TEST_SERVICE_ORDER).
        applicationScoped(true)
        );

    m_testServiceTunnelServlet = new ServiceTunnelServlet();
    m_testServiceTunnelServlet.lazyInit(null, null);
    m_requestMock = mock(HttpServletRequest.class);
    m_responseMock = mock(HttpServletResponse.class);
    m_testHttpSession = mock(HttpSession.class);
    when(m_requestMock.getSession()).thenReturn(m_testHttpSession);
    when(m_requestMock.getSession(true)).thenReturn(m_testHttpSession);
  }

  @After
  public void after() {
    TestingUtility.unregisterBeans(m_beans);
  }

  @Test
  public void testNewSessionCreatedOnLookupHttpSession() throws ProcessingException, ServletException {
    ServletRunContexts.empty().servletRequest(m_requestMock).servletResponse(m_responseMock).run(new IRunnable() {

      @Override
      public void run() throws Exception {
        IServerSession session = m_testServiceTunnelServlet.lookupServerSessionOnHttpSession(ServerRunContexts.empty());
        assertNotNull(session);
      }
    });
  }

  @Test
  public void testNoNewServerSessionOnLookup() throws ProcessingException, ServletException {
    final TestServerSession testSession = new TestServerSession();
    ICacheEntry cacheMock = mock(ICacheEntry.class);
    when(cacheMock.getValue()).thenReturn(testSession);
    when(cacheMock.isActive()).thenReturn(true);

    when(m_testHttpSession.getAttribute(IServerSession.class.getName())).thenReturn(cacheMock);

    ServletRunContexts.empty().servletRequest(m_requestMock).servletResponse(m_responseMock).run(new IRunnable() {

      @Override
      public void run() throws Exception {
        assertEquals(testSession, m_testServiceTunnelServlet.lookupServerSessionOnHttpSession(ServerRunContexts.empty()));
      }
    });
  }

  /**
   * Calls {@link ServiceTunnelServlet#lookupServerSessionOnHttpSession(ServerRunContext) in 4 different threads within
   * the same HTTP session. Test ensures that the same server session is returned in all threads and that
   *
   * @link ServerSessionProvider#provide(ServerJobInput)} is called only once.
   */
  @Test
  public void testLookupScoutServerSessionOnHttpSessionMultipleThreads() throws ProcessingException, ServletException, InterruptedException {
    final Map<String, ICacheEntry<?>> cache = new HashMap<String, ICacheEntry<?>>();

    final TestServerSession testServerSession = new TestServerSession();
    HttpServletRequest requestMock = mock(HttpServletRequest.class);
    HttpSession testHttpSession = mock(HttpSession.class);
    when(requestMock.getSession()).thenReturn(testHttpSession);
    when(requestMock.getSession(true)).thenReturn(testHttpSession);

    ICacheEntry cacheEntryMock = mock(ICacheEntry.class);
    when(cacheEntryMock.getValue()).thenReturn(testServerSession);
    when(cacheEntryMock.isActive()).thenReturn(true);

    doAnswer(putValueInCache(cache)).when(testHttpSession).setAttribute(eq(IServerSession.class.getName()), anyObject());
    when(testHttpSession.getAttribute(IServerSession.class.getName())).thenAnswer(getCachedValue(cache));

    doAnswer(slowCreateTestsession(testServerSession)).when(m_serverSessionProviderSpy).provide(any(ServerRunContext.class), any(String.class));
    List<HttpSessionLookupCallable> jobs = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      jobs.add(new HttpSessionLookupCallable(m_testServiceTunnelServlet, requestMock, m_responseMock));
    }

    List<IFuture<?>> futures = scheduleAndJoinJobs(jobs);

    Set<IServerSession> serverSessions = new HashSet<IServerSession>();
    for (IFuture<?> future : futures) {
      serverSessions.add((IServerSession) future.awaitDoneAndGet());
    }

    assertEquals(CollectionUtility.hashSet(testServerSession), serverSessions);

    verify(m_serverSessionProviderSpy, times(1)).provide(any(ServerRunContext.class), any(String.class));
  }

  private Answer<IServerSession> slowCreateTestsession(final TestServerSession testSession) {
    return new Answer<IServerSession>() {
      @Override
      public IServerSession answer(InvocationOnMock invocation) throws Throwable {
        Thread.sleep(2000); // simulate long running task
        return testSession;
      }
    };
  }

  private Answer<Object> putValueInCache(final Map<String, ICacheEntry<?>> cache) {
    return new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        String key = (String) args[0];
        ICacheEntry<?> value = (ICacheEntry<?>) args[1];
        cache.put(key, value);
        return null;
      }
    };
  }

  private Answer<Object> getCachedValue(final Map<String, ICacheEntry<?>> cache) {
    return new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        String key = (String) args[0];
        ICacheEntry<?> cacheEntry = cache.get(key);
        return cacheEntry;
      }
    };
  }

  private List<IFuture<?>> scheduleAndJoinJobs(List<? extends Callable<?>> jobs) throws ProcessingException {
    List<IFuture<?>> futures = new ArrayList<>();

    for (Callable<?> job : jobs) {
      futures.add(Jobs.schedule(job));
    }

    for (IFuture<?> future : futures) {
      future.awaitDoneAndGet();
    }

    return futures;
  }

  private static class HttpSessionLookupCallable implements Callable<IServerSession> {
    private final ServiceTunnelServlet m_serviceTunnelServlet;
    private final HttpServletRequest m_request;
    private final HttpServletResponse m_response;

    public HttpSessionLookupCallable(ServiceTunnelServlet serviceTunnelServlet, HttpServletRequest request, HttpServletResponse response) {
      m_serviceTunnelServlet = serviceTunnelServlet;
      m_request = request;
      m_response = response;
    }

    @Override
    public IServerSession call() throws Exception {
      return ServletRunContexts.empty().servletRequest(m_request).servletResponse(m_response).call(new Callable<IServerSession>() {

        @Override
        public IServerSession call() throws Exception {
          return m_serviceTunnelServlet.lookupServerSessionOnHttpSession(ServerRunContexts.empty());
        }
      });
    }
  }
}
