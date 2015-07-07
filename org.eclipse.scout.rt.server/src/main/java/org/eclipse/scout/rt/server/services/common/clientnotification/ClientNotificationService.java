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
package org.eclipse.scout.rt.server.services.common.clientnotification;

import java.util.LinkedList;
import java.util.Set;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.service.AbstractService;
import org.eclipse.scout.rt.platform.service.IService;
import org.eclipse.scout.rt.server.services.common.clientnotification.internal.ClientNotificationQueue;
import org.eclipse.scout.rt.server.services.common.clientnotification.internal.ClientNotificationQueueElement;
import org.eclipse.scout.rt.server.services.common.clientnotification.internal.ConsumableClientNotificationQueueElement;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationListener;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationListenerService;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationMessage;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterSynchronizationService;
import org.eclipse.scout.rt.server.transaction.AbstractTransactionMember;
import org.eclipse.scout.rt.server.transaction.ITransaction;
import org.eclipse.scout.rt.shared.services.common.clientnotification.IClientNotification;
import org.eclipse.scout.rt.shared.servicetunnel.RemoteServiceAccessDenied;

public class ClientNotificationService extends AbstractService implements IClientNotificationService, IClusterNotificationListenerService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(ClientNotificationService.class);
  private static final String TRANSACTION_MEMBER_ID = ClientNotificationService.class.getName();

  private final ClientNotificationQueue m_clientNotificationQueue;

  public ClientNotificationService() {
    m_clientNotificationQueue = new ClientNotificationQueue();
  }

  @Override
  public Set<IClientNotification> getNextNotifications(long blockingTimeout) {
    Set<IClientNotification> n = m_clientNotificationQueue.getNextNotifications(blockingTimeout);
    addClusterInfo(n);
    return n;
  }

  @Override
  public void putNotification(IClientNotification notification, IClientNotificationFilter filter) {
    tryPutNotification(new ConsumableClientNotificationQueueElement(notification, filter));

    // TODO [dwi/jgu]
//    Subject serverJobSubject = BEANS.get(IServerJobService.class).getServerSubject();
//    Subject contextSubject = Subject.getSubject(AccessController.getContext());
//    if (CompareUtility.notEquals(serverJobSubject, contextSubject)) {
//      // send cluster notification to other nodes right away since cluster notification is also transactional
//      distributeCluster(new ClientNotificationQueueElement(notification, filter));
//    }

    distributeCluster(new ClientNotificationQueueElement(notification, filter));
  }

  @Override
  public void putNonClusterDistributedNotification(IClientNotification notification, IClientNotificationFilter filter) {
    tryPutNotification(new ConsumableClientNotificationQueueElement(notification, filter));
  }

  private void tryPutNotification(ConsumableClientNotificationQueueElement queueElement) {
    try {
      ensureTransactionMember().putNotication(queueElement);
    }
    catch (ProcessingException e) {
      LOG.error("Error adding client notification", e);
    }
  }

  @Override
  public void ackNotifications(Set<String> notificationIds) {
    m_clientNotificationQueue.ackNotifications(notificationIds);
  }

  @Override
  @RemoteServiceAccessDenied
  public void addClientNotificationQueueListener(IClientNotificationQueueListener listener) {
    m_clientNotificationQueue.addClientNotificationQueueListener(listener);
  }

  @Override
  @RemoteServiceAccessDenied
  public void removeClientNotificationQueueListener(IClientNotificationQueueListener listener) {
    m_clientNotificationQueue.removeClientNotificationQueueListener(listener);
  }

  private ClientNotificationTransactionMember ensureTransactionMember() throws ProcessingException {
    ITransaction tx = Assertions.assertNotNull(ITransaction.CURRENT.get(), "Transaction required");
    ClientNotificationTransactionMember m = (ClientNotificationTransactionMember) tx.getMember(TRANSACTION_MEMBER_ID);
    if (m == null) {
      m = new ClientNotificationTransactionMember();
      tx.registerMember(m);
    }
    return m;
  }

//  /**
//   * Has no effect, if no cluster service is registered
//   */
  protected void distributeCluster(IClientNotificationQueueElement element) {
    //TODO remove
//    try {
//      IClusterSynchronizationService s = BEANS.opt(IClusterSynchronizationService.class);
//      if (s != null) {
//        element.getNotification().setOriginalServerNode(s.getNodeId());
//        s.publishNotification(new ClientNotificationClusterNotification(element));
//      }
//    }
//    catch (ProcessingException e) {
//      LOG.error("could not send cluster sync message", e);
//    }
  }

  /**
   * Has no effect, if no cluster service is registered
   */
  protected void addClusterInfo(Set<IClientNotification> notifications) {
    IClusterSynchronizationService s = BEANS.opt(IClusterSynchronizationService.class);
    if (s != null) {
      for (IClientNotification n : notifications) {
        n.setProvidingServerNode(s.getNodeId());
      }
    }
  }

  @Override
  public Class<? extends IService> getDefiningServiceInterface() {
    return IClientNotificationService.class;
  }

//  protected boolean accept(IClusterNotification notification) {
//    return (notification instanceof ClientNotificationClusterNotification) &&
//        ((ClientNotificationClusterNotification) notification).getQueueElement().isActive();
//  }

  /**
   * Client notifications fired within transaction are cached in a transaction-local queue and published into global
   * queue after commit.
   */
  private class ClientNotificationTransactionMember extends AbstractTransactionMember {

    private final LinkedList<ConsumableClientNotificationQueueElement> m_transactionLocalQueue;
    private final Object m_queueLock = new Object();

    public ClientNotificationTransactionMember() {
      super(TRANSACTION_MEMBER_ID);
      m_transactionLocalQueue = new LinkedList<ConsumableClientNotificationQueueElement>();
    }

    /**
     * @param n
     */
    public void putNotication(ConsumableClientNotificationQueueElement n) {
      synchronized (m_queueLock) {
        m_transactionLocalQueue.add(n);
      }
    }

    @Override
    public boolean needsCommit() {
      return !m_transactionLocalQueue.isEmpty();
    }

    @Override
    public boolean commitPhase1() {
      return true;
    }

    @Override
    public void commitPhase2() {
      // write whole transaction-local queue to global queue
      synchronized (m_queueLock) {
        for (IClientNotificationQueueElement e : m_transactionLocalQueue) {
          m_clientNotificationQueue.putNotification(e.getNotification(), e.getFilter());
        }
      }
    }

    @Override
    public void rollback() {
      synchronized (m_queueLock) {
        m_transactionLocalQueue.clear();
      }
    }

    @Override
    public void release() {
    }
  }

  @Override
  public IClusterNotificationListener getClusterNotificationListener() {
    return new IClusterNotificationListener() {

      @Override
      public void onNotification(IClusterNotificationMessage message) throws ProcessingException {
//        if (accept(message.getNotification())) {
//          ClientNotificationClusterNotification n = (ClientNotificationClusterNotification) message.getNotification();
//          BEANS.get(IClientNotificationService.class).putNonClusterDistributedNotification(n.getQueueElement().getNotification(), n.getQueueElement().getFilter());
//        }

        //TODO remove
      }
    };
  }
}
