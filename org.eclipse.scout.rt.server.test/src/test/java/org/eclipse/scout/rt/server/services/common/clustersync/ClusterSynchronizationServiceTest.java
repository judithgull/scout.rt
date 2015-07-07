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
package org.eclipse.scout.rt.server.services.common.clustersync;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.platform.BeanMetaData;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.server.TestServerSession;
import org.eclipse.scout.rt.server.services.common.clustersync.internal.ClusterNotificationMessage;
import org.eclipse.scout.rt.server.services.common.clustersync.internal.ClusterNotificationMessageProperties;
import org.eclipse.scout.rt.server.services.common.security.AccessControlClusterNotification;
import org.eclipse.scout.rt.server.transaction.ITransaction;
import org.eclipse.scout.rt.shared.services.common.code.AbstractCodeType;
import org.eclipse.scout.rt.shared.services.common.code.CodeTypeChangedNotification;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;
import org.eclipse.scout.rt.testing.platform.runner.RunWithSubject;
import org.eclipse.scout.rt.testing.server.runner.RunWithServerSession;
import org.eclipse.scout.rt.testing.server.runner.ServerTestRunner;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ClusterSynchronizationService} without transactions.
 */
@RunWith(ServerTestRunner.class)
@RunWithServerSession(TestServerSession.class)
@RunWithSubject("default")
public class ClusterSynchronizationServiceTest {
  private static final String TEST_NODE = "node";
  private static final String TEST_USER = "user";

  private ClusterNotificationMessage m_message;
  private List<IBean<?>> m_beans = new ArrayList<>();
  private IPublishSubscribeMessageService m_messageService;
  private ClusterSynchronizationService m_svc = null;

  @Before
  public void before() {
    ClusterNotificationMessageProperties testProps = new ClusterNotificationMessageProperties(TEST_NODE, TEST_USER);
    m_message = new ClusterNotificationMessage("notification", testProps);

    m_messageService = mock(IPublishSubscribeMessageService.class);
    m_beans.add(
        TestingUtility.registerBean(
            new BeanMetaData(IPublishSubscribeMessageService.class).
                initialInstance(m_messageService).
                applicationScoped(true)
            ));

    m_svc = new ClusterSynchronizationService();
    m_svc.initializeService();
    m_svc.enable();
  }

  @After
  public void tearDown() {
    TestingUtility.unregisterBeans(m_beans);
  }

  /**
   * Tests that the statusInfo is updated up on receipt of a message.
   */
  @Test
  public void testReveiveInfoUpdated() throws ProcessingException {
    m_svc.onMessage(m_message);
    IClusterNodeStatusInfo nodeInfo = m_svc.getStatusInfo();
    assertEquals(1, nodeInfo.getReceivedMessageCount());
    assertEquals(0, nodeInfo.getSentMessageCount());
    assertEquals(TEST_NODE, nodeInfo.getLastChangedOriginNodeId());
  }

  /**
   * Tests that the message is sent when publishing it.
   */
  @Test
  public void testSendNoTransaction() throws ProcessingException {
    m_svc.publish("Testnotification");
    assertSingleMessageSent();
  }

  /**
   * Tests that no message is sent, if the transaction is not committed.
   */
  @Test
  public void testSendTransactional_NotCommitted() throws Exception {
    m_svc.publishTransactional("Testnotification");
    assertNoMessageSent();
  }

  /**
   * Tests that no message is sent, if the transaction is not committed.
   */
  @Test
  public void testSendTransactional_Committed() throws Exception {
    m_svc.publishTransactional("Testnotification");
    ITransaction.CURRENT.get().commitPhase1();
    ITransaction.CURRENT.get().commitPhase2();
    assertSingleMessageSent();
  }

  @Test
  public void testSendTransactional_Rollback() throws Exception {
    m_svc.publishTransactional("Testnotification");
    ITransaction.CURRENT.get().rollback();
    ITransaction.CURRENT.get().commitPhase1();
    ITransaction.CURRENT.get().commitPhase2();
    assertNoMessageSent();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTransactionalSendMultipleMessages() throws Exception {
    m_svc.publishTransactional("Testnotification");
    m_svc.publishTransactional("Testnotification");
    ITransaction.CURRENT.get().commitPhase1();
    ITransaction.CURRENT.get().commitPhase2();
    verify(m_messageService, times(1)).publishNotifications(any(List.class));
    assertEquals(2, m_svc.getStatusInfo().getSentMessageCount());
  }

  @Test
  public void testDisabledSendTransactional() throws Exception {
    m_svc.disable();
    m_svc.publishTransactional("Testnotification");
    ITransaction.CURRENT.get().commitPhase1();
    ITransaction.CURRENT.get().commitPhase2();
    assertNoMessageSent();
  }

  @Test
  public void testDisabledSend_NoTransaction() throws Exception {
    m_svc.disable();
    m_svc.publish("Testnotification");
    assertNoMessageSent();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTransactionalWithCoalesce() throws Exception {
    m_svc.publishTransactional(new AccessControlClusterNotification());
    m_svc.publishTransactional(new AccessControlClusterNotification());
    ArrayList<Class<? extends ICodeType<?, ?>>> codeTypes = new ArrayList<>();
    codeTypes.add(TestCodeType.class);
    m_svc.publishTransactional(new CodeTypeChangedNotification(codeTypes));
    ITransaction.CURRENT.get().commitPhase1();
    ITransaction.CURRENT.get().commitPhase2();
    verify(m_messageService, times(1)).publishNotifications(any(List.class));
    assertEquals(2, m_svc.getStatusInfo().getSentMessageCount());
  }

  @SuppressWarnings("unchecked")
  private void assertNoMessageSent() {
    verify(m_messageService, never()).publishNotifications(any(List.class));
    assertEmptyNodeInfo(m_svc.getStatusInfo());
  }

  @SuppressWarnings("unchecked")
  private void assertSingleMessageSent() {
    verify(m_messageService, times(1)).publishNotifications(any(List.class));
    IClusterNodeStatusInfo statusInfo = m_svc.getStatusInfo();
    assertEquals(0, statusInfo.getReceivedMessageCount());
    assertEquals(1, statusInfo.getSentMessageCount());
    assertEquals(m_svc.getNodeId(), statusInfo.getLastChangedOriginNodeId());
    assertEquals("default", statusInfo.getLastChangedUserId());
  }

  private void assertEmptyNodeInfo(IClusterNodeStatusInfo status) {
    assertEquals(0, status.getReceivedMessageCount());
    assertEquals(0, status.getSentMessageCount());
    assertEquals(null, status.getLastChangedOriginNodeId());
    assertEquals(null, status.getLastChangedUserId());
  }

  class TestCodeType extends AbstractCodeType<Long, Long> {

    private static final long serialVersionUID = 1L;

    @Override
    public Long getId() {
      return null;
    }
  }

}
