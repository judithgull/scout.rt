/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.services.common.clientnotification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.server.AbstractServerSession;
import org.eclipse.scout.rt.server.clientnotification.ClientNotificationService;
import org.eclipse.scout.rt.shared.OfflineState;
import org.eclipse.scout.rt.shared.clientnotification.ClientNotificationMessage;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This class tests the behavior of sent client notifications when
 * the shared variable map changes on the server side.<br/>
 * {@code SharedContextChangedNotification}s should only be sent if
 * the default offline state matches the offline state of the current thread.
 *
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=379721">Bugzilla 379721</a>
 */
@RunWith(PlatformTestRunner.class)
public class OfflineSharedVariableNotificationTest {

  private TestServerSession m_serverSession;
  private ClientNotificationService svc;

  private final String testSessionId = "testSessionId";
  private final String testNodeId = "testNodeId";
  private final String testUserId = "testUser";

  @Before
  public void setUp() throws ProcessingException {
    OfflineState.setOfflineDefault(false);
    OfflineState.CURRENT.remove();
    svc = BEANS.get(ClientNotificationService.class);
    svc.registerSession(testNodeId, testSessionId, testUserId);
    m_serverSession = new TestServerSession();
    m_serverSession.start(testSessionId);
    //consume notifications during initialization
    getNotifications();
  }

  @After
  public void tearDown() {
    OfflineState.setOfflineDefault(false);
    OfflineState.CURRENT.remove();
    m_serverSession = null;
  }

  /**
   * Slow test, no notifications available
   */
  @Test
  @Ignore
  //slow, ignore until timeout is configurable
  public void testInitiallyNoNotifications() {
    assertNoNotifications();
  }

  /**
   * Tests shared variable map notifications with following setup:
   * <ul>
   * <li>Offline default = true</li>
   * <li>Offline current thread = null</li>
   * </ul>
   * A client notification should be sent in this case.
   */
  @Test
  public void testDefaultOffline() {
    OfflineState.setOfflineDefault(true);
    OfflineState.CURRENT.remove();

    m_serverSession.setMySharedVariable(new Object());

    assertSharedVariableSet();
    assertSingleNotification();
  }

  /**
   * Tests shared variable map notifications with following setup:
   * <ul>
   * <li>Offline default = true</li>
   * <li>Offline current thread = true</li>
   * </ul>
   * A client notification should be sent in this case.
   */
  @Test
  public void testDefaultOfflineThreadOffline() {
    OfflineState.setOfflineDefault(true);
    OfflineState.CURRENT.set(true);
    m_serverSession.setMySharedVariable(new Object());

    assertSharedVariableSet();
    assertSingleNotification();
  }

  /**
   * Tests shared variable map notifications with following setup:
   * <ul>
   * <li>Offline default = true</li>
   * <li>Offline current thread = false</li>
   * </ul>
   * No client notification should be sent in this case.
   */
  @Test
  @Ignore
  //slow, ignore until timeout is configurable
  public void testDefaultOfflineThreadOnline() {
    OfflineState.setOfflineDefault(true);
    OfflineState.CURRENT.set(false);

    m_serverSession.setMySharedVariable(new Object());

    assertSharedVariableSet();
    assertNoNotifications();
  }

  /**
   * TESTS SHARED VARIABLE MAP NOTIFICATIONS WITH FOLLOWING SETUP:
   * <UL>
   * <LI>OFFLINE DEFAULT = FALSE</LI>
   * <LI>OFFLINE CURRENT THREAD = NULL</LI>
   * </UL>
   * A CLIENT NOTIFICATION SHOULD BE SENT IN THIS CASE.
   */
  @Test
  public void testDefaultOnline() {
    OfflineState.setOfflineDefault(false);
    OfflineState.CURRENT.remove();
    m_serverSession.setMySharedVariable(new Object());

    assertSharedVariableSet();
    assertSingleNotification();
  }

  /**
   * Tests shared variable map notifications with following setup:
   * <ul>
   * <li>Offline default = false</li>
   * <li>Offline current thread = false</li>
   * </ul>
   * A client notification should be sent in this case.
   */
  @Test
  public void testDefaultOnlineThreadOnline() {
    OfflineState.setOfflineDefault(false);
    OfflineState.CURRENT.set(false);
    m_serverSession.setMySharedVariable(new Object());

    assertSharedVariableSet();
    assertSingleNotification();
  }

  /**
   * Tests shared variable map notifications with following setup:
   * <ul>
   * <li>Offline default = false</li>
   * <li>Offline current thread = true</li>
   * </ul>
   * No client notification should be sent in this case.
   */
  @Test
  @Ignore
  //slow, ignore until timeout is configurable
  public void testDefaultOnlineThreadOffline() {
    OfflineState.setOfflineDefault(false);
    OfflineState.CURRENT.set(true);
    m_serverSession.setMySharedVariable(new Object());

    assertSharedVariableSet();
    assertNoNotifications();
  }

  private List<ClientNotificationMessage> getNotifications() {
    return svc.getNotifications(testNodeId);
  }

  private void assertSharedVariableSet() {
    assertNotNull("Shared variable should be set.", m_serverSession.getMySharedVariable());
  }

  private void assertNoNotifications() {
    List<ClientNotificationMessage> notifications = getNotifications();
    assertTrue("No notification expected.", notifications.isEmpty());
  }

  private void assertSingleNotification() {
    List<ClientNotificationMessage> notifications = getNotifications();
    assertEquals("Notification expected.", 1, notifications.size());
  }

  /**
   * A server session with access to a shared variable for testing purposes.
   */
  private static class TestServerSession extends AbstractServerSession {
    private static final long serialVersionUID = -3811354009155350753L;
    private static final String KEY_MY_SHARED_VARIABLE = "mySharedVariable";

    public TestServerSession() {
      // make sure initConfig is called in super class constructor
      super(true);
    }

    public void setMySharedVariable(Object value) {
      setSharedContextVariable(KEY_MY_SHARED_VARIABLE, Object.class, value);
    }

    public Object getMySharedVariable() {
      return getSharedContextVariable(KEY_MY_SHARED_VARIABLE, Object.class);
    }
  }

}
