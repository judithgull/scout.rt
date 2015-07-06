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
package org.eclipse.scout.rt.server.clientnotification;

import java.io.Serializable;

import org.eclipse.scout.commons.ToStringBuilder;
import org.eclipse.scout.rt.shared.services.common.notification.NotificationMessage;

/**
 *
 */
public class ClientNotificationClusterNotification implements Serializable {
  private static final long serialVersionUID = -8513131031858145786L;
  private final NotificationMessage m_cnMessage;

  public ClientNotificationClusterNotification(NotificationMessage message) {
    m_cnMessage = message;
  }

  public NotificationMessage getClientNotificationMessage() {
    return m_cnMessage;
  }

  @Override
  public String toString() {
    ToStringBuilder tsb = new ToStringBuilder(this);
    tsb.attr("ClientNotificationMessage", m_cnMessage);
    return tsb.toString();
  }

}
