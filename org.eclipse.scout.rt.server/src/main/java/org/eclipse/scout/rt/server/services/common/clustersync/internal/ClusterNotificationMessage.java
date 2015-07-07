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
package org.eclipse.scout.rt.server.services.common.clustersync.internal;

import java.io.Serializable;

import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationMessage;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationMessageProperties;

public class ClusterNotificationMessage implements IClusterNotificationMessage {
  private static final long serialVersionUID = -4471640837086802256L;
  private final Serializable m_notification;
  private final IClusterNotificationMessageProperties m_props;

  public ClusterNotificationMessage(Serializable notification, IClusterNotificationMessageProperties props) {
    m_notification = notification;
    m_props = props;
  }

  @Override
  public Serializable getNotification() {
    return m_notification;
  }

  @Override
  public IClusterNotificationMessageProperties getProperties() {
    return m_props;
  }

  @Override
  public String toString() {
    return "ClusterNotificationMessage [m_notification=" + m_notification + ", m_props=" + m_props + "]";
  }

}
