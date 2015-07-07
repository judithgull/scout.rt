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
package org.eclipse.scout.rt.client.services.common.notification;

import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.CreateImmediately;
import org.eclipse.scout.rt.platform.context.RunMonitor;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.shared.SharedConfigProperties.NotificationSubjectProperty;
import org.eclipse.scout.rt.shared.services.common.notification.INotificationServerService;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnel;
import org.eclipse.scout.rt.shared.ui.UserAgent;

/**
 *
 */
@ApplicationScoped
@CreateImmediately
public class NotificationPoller {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(NotificationPoller.class);
  private IFuture<Void> m_pollerFuture;

  @PostConstruct
  protected void setup() {
    if (BEANS.get(IServiceTunnel.class).isActive() && BEANS.opt(INotificationServerService.class) != null) {
      P_NotificationPollJob pollJob = new P_NotificationPollJob();
      m_pollerFuture = Jobs.schedule(pollJob, Jobs.newInput(ClientRunContexts.copyCurrent().subject(BEANS.get(NotificationSubjectProperty.class).getValue()).userAgent(UserAgent.createDefault()).session(null, false)));
    }
    else {
      LOG.debug("Starting without notifications due to no proxy service is available");
    }
  }

  public void stopPoller() {
    if (m_pollerFuture != null) {
      m_pollerFuture.cancel(true);
    }
  }

  protected void handleNotificationMessagesReceived(List<NotificationMessage> notifications) {
    System.out.println(String.format("CLIENT NOTIFICATION returned with %s notifications (%s).", notifications.size(), notifications));
    // process notifications
    if (!notifications.isEmpty()) {
      BEANS.get(NotificationDispatcher.class).dispatchNotifications(notifications, new IFilter<NotificationMessage>() {
        @Override
        public boolean accept(NotificationMessage message) {
          return !CompareUtility.equals(message.getAddress().getExcludeNodeId(), INotificationClientService.NOTIFICATION_NODE_ID);
        }
      });
    }
  }

  private class P_NotificationPollJob implements IRunnable {
    @Override
    public void run() {
      while (!RunMonitor.CURRENT.get().isCancelled()) {
        handleNotificationMessagesReceived(BEANS.get(INotificationServerService.class).getNotifications(INotificationClientService.NOTIFICATION_NODE_ID));
      }
    }
  }
}
