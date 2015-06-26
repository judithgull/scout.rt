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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.server.transaction.AbstractTransactionMember;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;

/**
 *
 */
public class NotificationTransactionMember extends AbstractTransactionMember {

  public static final String TRANSACTION_MEMBER_ID = "notification.transactionMemberId";

  private final List<NotificationMessage> m_notifications = new LinkedList<>();

  public NotificationTransactionMember() {
    super(TRANSACTION_MEMBER_ID);
  }

  public void addNotification(NotificationMessage message) {
    m_notifications.add(message);
  }

  @Override
  public void commitPhase2() {
    BEANS.get(NotificationRegistry.class).put(m_notifications);
  }

}
