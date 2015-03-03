/*******************************************************************************
 * Copyright (c) 2014 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.services.common.bookmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.scout.rt.client.testenvironment.TestEnvironmentClientSession;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.service.SERVICES;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test for {@link IBookmarkService}
 */
@RunWith(ScoutClientTestRunner.class)
public class BookmarkServiceTest {

  private IDesktop m_desktop;
  private IDesktop m_mockDesktop;

  @Before
  public void setUp() {
    m_desktop = TestEnvironmentClientSession.get().getDesktop();
    m_mockDesktop = Mockito.mock(IDesktop.class);
    TestEnvironmentClientSession.get().replaceDesktop(m_mockDesktop);
  }

  @After
  public void tearDown() {
    TestEnvironmentClientSession.get().replaceDesktop(m_desktop);
  }

  @Test
  public void testSetStartBookmark() throws Exception {
    Bookmark bookmark = new Bookmark();
    bookmark.setText("My Bookmark Text");
    Mockito.when(m_mockDesktop.createBookmark()).thenReturn(bookmark);
    IBookmarkService s = SERVICES.getService(IBookmarkService.class);
    Assert.assertNotNull(s);
    s.setStartBookmark();

    // Get the Bookmark
    Bookmark startBookmark = s.getStartBookmark();
    assertEquals("Kind", Bookmark.USER_BOOKMARK, startBookmark.getKind());
    assertEquals("Text", "My Bookmark Text", startBookmark.getText());
  }

  @Test
  public void testDeleteBookmark() throws Exception {
    Bookmark bookmark = new Bookmark();
    bookmark.setText("My Bookmark Text");
    Mockito.when(m_mockDesktop.createBookmark()).thenReturn(bookmark);
    IBookmarkService s = SERVICES.getService(IBookmarkService.class);
    Assert.assertNotNull(s);
    s.setStartBookmark();

    // Get the Bookmark
    Bookmark startBookmark = s.getStartBookmark();
    assertNotNull(startBookmark);

    // Delete the bookmark
    s.deleteStartBookmark();

    startBookmark = s.getStartBookmark();
    assertNull(startBookmark);
  }
}