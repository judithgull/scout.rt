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
package org.eclipse.scout.rt.server.commons.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

/**
 * <b>abstract</b> Test class for {@link AbstractHttpSessionCacheService}
 */
public abstract class AbstractHttpSessionCacheServiceTest {
  protected HttpServletResponse m_responseMock;
  protected HttpServletRequest m_requestMock;
  protected final String m_testValue = "testValue";
  protected final Integer testExpiration = Integer.valueOf(10000);
  protected String m_testKey = "testKey";
  protected TestHttpSession m_testSession;
  protected AbstractHttpSessionCacheService m_cacheService;

  @Before
  public void setup() {
    m_requestMock = mock(HttpServletRequest.class);
    m_responseMock = mock(HttpServletResponse.class);
    m_testSession = new TestHttpSession();
    when(m_requestMock.getSession(true)).thenReturn(m_testSession);
    m_cacheService = createCacheService();
  }

  protected abstract AbstractHttpSessionCacheService createCacheService();

  @Test
  public void testGetExpired() {
    m_cacheService.put(m_testKey, m_testValue, m_requestMock, m_responseMock, 0L);
    m_cacheService.get(m_testKey, m_requestMock, m_responseMock);
    assertNull(m_cacheService.get(m_testKey, m_requestMock, m_responseMock));
    assertFalse(m_testSession.getAttributeNames().hasMoreElements());
  }

  @Test
  public void testGetUnknown() {
    m_cacheService.get(m_testKey, m_requestMock, m_responseMock);
    assertNull(m_cacheService.get(m_testKey, m_requestMock, m_responseMock));
  }

  @Test
  public void testPut() {
    m_cacheService.put(m_testKey, m_testValue, m_requestMock, m_responseMock);
    assertEquals(m_testValue, m_cacheService.get(m_testKey, m_requestMock, m_responseMock));
    assertTrue(m_testSession.getAttributeNames().hasMoreElements());
  }

}
