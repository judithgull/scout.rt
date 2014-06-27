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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

/**
 * Test for {@link StickySessionCacheService}
 */
public class StickySessionCacheStoreServiceTest extends AbstractHttpSessionCacheServiceTest {

  @Test
  public void testTouchAttribute() {
    ICacheEntry mockEntry = mock(ICacheEntry.class);
    when(mockEntry.isActive()).thenReturn(true);
    m_testSession.setAttribute(m_testKey, mockEntry);
    m_cacheService.touch(m_testKey, m_requestMock, m_responseMock);
    verify(mockEntry, times(1)).touch();
  }

//  @Test
//  public void testExpiredValuesRemoved() {
//    ICacheEntry mockEntry = mock(ICacheEntry.class);
//    when(mockEntry.isActive()).thenReturn(true);
//    m_testSession.setAttribute(m_testKey, mockEntry);
//    m_cacheService.get(m_testKey, m_requestMock, m_responseMock);
//    verify(m_testSession, times(1)).removeAttribute(m_testKey);
//  }

  @Override
  protected AbstractHttpSessionCacheService createCacheService() {
    return new StickySessionCacheService();
  }

}
