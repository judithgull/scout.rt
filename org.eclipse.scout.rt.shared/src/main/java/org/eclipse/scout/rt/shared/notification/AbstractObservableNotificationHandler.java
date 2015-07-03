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
package org.eclipse.scout.rt.shared.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;

/**
 *
 */
public abstract class AbstractObservableNotificationHandler<T extends Serializable> implements INotificationHandler<T> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractObservableNotificationHandler.class);
  private final List<IClientNotificationListener<T>> m_listeners = new LinkedList<>();

  public void addListener(IClientNotificationListener<T> listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  public void removeListener(IClientNotificationListener<T> listener) {
    synchronized (m_listeners) {
      m_listeners.remove(listener);
    }
  }

  @Override
  public void handleNotification(T notification) {
    final List<IClientNotificationListener<T>> listeners;
    synchronized (m_listeners) {
      listeners = new ArrayList<IClientNotificationListener<T>>(m_listeners);
    }
    for (IClientNotificationListener<T> l : listeners) {
      try {
        l.handleNotification(notification);
      }
      catch (Exception e) {
        LOG.error(String.format("Error during notification of listener '%s'.", l), e);
      }
    }
  }
}
