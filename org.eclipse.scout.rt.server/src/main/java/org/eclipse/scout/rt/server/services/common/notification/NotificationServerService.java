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
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.Server;
import org.eclipse.scout.rt.server.session.ServerSessionProvider;
import org.eclipse.scout.rt.shared.services.common.notification.INotificationServerService;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;

/**
 *
 */
@Server
public class NotificationServerService implements INotificationServerService {

  private NotificationRegistry m_notificationRegistry;

  @PostConstruct
  protected void setup() {
    m_notificationRegistry = BEANS.get(NotificationRegistry.class);
  }

  @Override
  public void register(String notificationNodeId) {
    IServerSession session = ServerSessionProvider.currentSession();
    m_notificationRegistry.registerUser(notificationNodeId, session);
  }

  @Override
  public void unregister(String notificationNodeId) {

  }

  public void destroy(String notificationNodeId) {

  }

  @Override
  public List<NotificationMessage> getNotifications(String notificationNodeId) {
    // TODO to be configured
    return m_notificationRegistry.consume(notificationNodeId, 30, 10, TimeUnit.SECONDS);
  }

  public void push(Serializable data, IUserProvider userProvider) {

  }

}
