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
package org.eclipse.scout.rt.server.services.common.notification;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.server.services.common.clientnotification.ClientNotificationClusterNotification;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterSynchronizationService;
import org.eclipse.scout.rt.server.transaction.ITransaction;
import org.eclipse.scout.rt.shared.services.common.notification.NotficationAddress;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;

/**
 * The {@link NotificationRegistry} is the registry for all notifications. It keeps a {@link NotificationNodeQueue} for
 * each notification node (usually a client node).
 * The {@link NotificationServerService} consumes the notifications per node. The consumption of the notifications waits
 * for a given timeout for notifications. If no notifications are scheduled within this timeout the lock will be
 * released and returns without any notifications. In case a notification gets scheduled during this timeout the
 * request will be released immediately.
 */
@ApplicationScoped
public class NotificationRegistry {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(NotificationRegistry.class);
  private final Map<String /*notificationNodeId*/, NotificationNodeQueue> m_notificationQueues = new HashMap<>();

  /**
   * This method should only be accessed from {@link NotificationServerService}
   *
   * @param notificationNodeId
   * @param session
   */
  void registerSession(String notificationNodeId, String sessionId, String userId) {
    synchronized (m_notificationQueues) {
      NotificationNodeQueue queue = m_notificationQueues.get(Assertions.assertNotNull(notificationNodeId));
      queue.registerSession(sessionId, userId);
    }
  }

  /**
   * This method should only be accessed from {@link NotificationServerService}
   *
   * @param notificationNodeId
   */
  void unregisterNode(String notificationNodeId) {
  }

  /**
   * This method should only be accessed from {@link NotificationServerService}
   *
   * @param notificationNodeId
   * @param maxAmount
   * @param amount
   * @param unit
   * @return
   */
  List<NotificationMessage> consume(String notificationNodeId, int maxAmount, int amount, TimeUnit unit) {
    NotificationNodeQueue queue;
    synchronized (m_notificationQueues) {
      queue = m_notificationQueues.get(notificationNodeId);
      if (queue == null) {
        // create new
        // TODO[aho] make configurable
        queue = new NotificationNodeQueue(notificationNodeId, 200);
        m_notificationQueues.put(notificationNodeId, queue);
      }
    }
    return queue.consume(maxAmount, amount, unit);
  }

  /**
   * To access all session id's having to whom notifications will be providen by this server node.
   *
   * @return
   */
  public Set<String> getAllSessionIds() {
    Set<String> allSessionIds = new HashSet<>();
    synchronized (m_notificationQueues) {
      for (NotificationNodeQueue queue : m_notificationQueues.values()) {
        allSessionIds.addAll(queue.getAllSessionIds());
      }
    }
    return allSessionIds;
  }

  // put methods
  /**
   * The notification will be distributed to all sessions of the given userId.
   *
   * @param userId
   * @param notification
   */
  public void putForUser(String userId, Serializable notification) {
    NotificationMessage message = new NotificationMessage(NotficationAddress.createUserAddress(CollectionUtility.hashSet(userId)), notification);
    put(message);
  }

  /**
   * The notification will be distributed to the session addressed with the unique sessionId.
   *
   * @param sessionId
   *          the addressed session
   * @param notification
   */
  public void putForSession(String sessionId, Serializable notification) {
    NotificationMessage message = new NotificationMessage(NotficationAddress.createSessionAddress(CollectionUtility.hashSet(sessionId)), notification);
    put(message);
  }

  /**
   * This notification will be distributed to all sessions.
   *
   * @param notification
   */
  public void putForAllSessions(Serializable notification) {
    NotificationMessage message = new NotificationMessage(NotficationAddress.createAllSessionsAddress(), notification);
    put(message);
  }

  public void put(NotificationMessage message) {
    synchronized (m_notificationQueues) {
      for (NotificationNodeQueue queue : m_notificationQueues.values()) {
        queue.put(message);
      }
    }
  }

  public void put(Collection<? extends NotificationMessage> message) {
    synchronized (m_notificationQueues) {
      for (NotificationNodeQueue queue : m_notificationQueues.values()) {
        queue.put(message);
      }
    }
    // TODO aho/jgu clustersync
  }

  /**
   * To put a notifications with transactional behavior. The notification will be processed on successful commit of the
   * {@link ITransaction} surrounding the server call.
   * The notification will be distributed to all sessions of the given userId.
   *
   * @param userId
   *          the addressed user
   * @param notification
   */
  public void putTransactionalForUser(String userId, Serializable notification) {
    // exclude the node the request comes from
    NotificationMessage message = new NotificationMessage(NotficationAddress.createUserAddress(CollectionUtility.hashSet(userId),
        Assertions.assertNotNull(NotificationNodeId.CURRENT.get(), "No notification node id found on the thread context.")), notification);
    putTransactional(message);
  }

  /**
   * To put a notifications with transactional behavior. The notification will be processed on successful commit of the
   * {@link ITransaction} surrounding the server call.
   * The notification will be distributed to the session addressed with the unique sessionId.
   *
   * @param sessionId
   *          the addressed session
   * @param notification
   */
  public void putTransactionalForSession(String sessionId, Serializable notification) {
    NotificationMessage message = new NotificationMessage(NotficationAddress.createSessionAddress(CollectionUtility.hashSet(sessionId),
        Assertions.assertNotNull(NotificationNodeId.CURRENT.get(), "No notification node id found on the thread context.")), notification);
    putTransactional(message);
  }

  /**
   * To put a notifications with transactional behavior. The notification will be processed on successful commit of the
   * {@link ITransaction} surrounding the server call.
   * This notification will be distributed to all sessions.
   *
   * @param notification
   */
  public void putTransactionalForAllSessions(Serializable notification) {
    NotificationMessage message = new NotificationMessage(NotficationAddress.createAllSessionsAddress(
        Assertions.assertNotNull(NotificationNodeId.CURRENT.get(), "No notification node id found on the thread context.")), notification);
    putTransactional(message);
  }

  public void putTransactional(NotificationMessage message) {
    ITransaction transaction = Assertions.assertNotNull(ITransaction.CURRENT.get(), "No transaction found in curren run context for processing notification %s", message);
    try {
      NotificationTransactionMember tMember = (NotificationTransactionMember) transaction.getMember(NotificationTransactionMember.TRANSACTION_MEMBER_ID);
      if (tMember == null) {
        tMember = new NotificationTransactionMember();
        transaction.registerMember(tMember);
      }
      tMember.addNotification(message);
    }
    catch (ProcessingException e) {
      LOG.warn("Could not register transaction member. The notification will be processed immediately", e);
      put(message);

    }
  }

  private void distributeCluster(NotificationMessage message) {
    try {
      IClusterSynchronizationService service = BEANS.get(IClusterSynchronizationService.class);
      service.publish(new ClientNotificationClusterNotification(message));
    }
    catch (ProcessingException e) {
      LOG.error("Error distributing cluster notification ", e);
    }
  }

}
