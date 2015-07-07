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
package org.eclipse.scout.rt.shared.services.common.notification;

import java.io.Serializable;

/**
 *
 */
public class NotificationMessage {
  private final String m_userId;
  private final Serializable m_notification;

  public NotificationMessage(String userId, Serializable notification) {
    m_userId = userId;
    m_notification = notification;
  }

  public String getUserId() {
    return m_userId;
  }

  public Serializable getNotification() {
    return m_notification;
  }

}
