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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.CreateImmediately;

/**
 *
 */
@ApplicationScoped
@CreateImmediately
public class NotificationCoalescer {

  /**
   * static bindings of available {@link ITransactionalNotificationCoalescer}
   */
  private final Map<Class<? extends Serializable> /*notification class*/, Set<ITransactionalNotificationCoalescer<? extends Serializable>>> m_notificationClassToCoalescer = new HashMap<>();
  private final Map<Class<? extends Serializable> /*notification class*/, Set<ITransactionalNotificationCoalescer<? extends Serializable>>> m_cachedNotificationCoalescers = new HashMap<>();
  private final Object m_cacheLock = new Object();

  private boolean m_useCachedNotificationCoalescerLookup = true;

  /**
   * builds a linking of all handlers generic types to handlers. This is used to find the corresponding handler of a
   * notification.
   */
  @SuppressWarnings("unchecked")
  @PostConstruct
  protected void buildCoalescerLinking() {
    List<ITransactionalNotificationCoalescer> notificationCoalescers = BEANS.all(ITransactionalNotificationCoalescer.class);
    for (ITransactionalNotificationCoalescer<?> notificationCoalescer : notificationCoalescers) {
      Class notificationClass = TypeCastUtility.getGenericsParameterClass(notificationCoalescer.getClass(), ITransactionalNotificationCoalescer.class);
      Set<ITransactionalNotificationCoalescer<?>> coalescerSet = m_notificationClassToCoalescer.get(notificationClass);
      if (coalescerSet == null) {
        coalescerSet = new HashSet<>();
        m_notificationClassToCoalescer.put(notificationClass, coalescerSet);
      }
      coalescerSet.add(notificationCoalescer);
    }
  }

  protected Set<ITransactionalNotificationCoalescer<? extends Serializable>> getNotificationCoalescers(Class<? extends Serializable> notificationClass) {
    if (m_useCachedNotificationCoalescerLookup) {
      synchronized (m_cacheLock) {
        Set<ITransactionalNotificationCoalescer<? extends Serializable>> notificationCoalescers = m_cachedNotificationCoalescers.get(notificationClass);
        if (notificationCoalescers == null) {
          notificationCoalescers = findNotificationCoalescers(notificationClass);
          m_cachedNotificationCoalescers.put(notificationClass, notificationCoalescers);
        }
        return new HashSet<ITransactionalNotificationCoalescer<? extends Serializable>>(notificationCoalescers);
      }
    }
    else {
      return findNotificationCoalescers(notificationClass);
    }
  }

  private Set<ITransactionalNotificationCoalescer<? extends Serializable>> findNotificationCoalescers(Class<? extends Serializable> notificationClass) {
    Set<ITransactionalNotificationCoalescer<? extends Serializable>> coalescers = new HashSet<>();
    synchronized (m_cacheLock) {
      for (Entry<Class<? extends Serializable> /*notification class*/, Set<ITransactionalNotificationCoalescer<? extends Serializable>>> e : m_notificationClassToCoalescer.entrySet()) {
        if (e.getKey().isAssignableFrom(notificationClass)) {
          coalescers.addAll(e.getValue());
        }
      }
    }
    return coalescers;
  }

  public Set<? extends Serializable> coalesce(Set<? extends Serializable> notificationsIn) {
    Set<? extends Serializable> notifications = new HashSet<>(notificationsIn);
    List<ITransactionalNotificationCoalescer> notificationCoalescers = BEANS.all(ITransactionalNotificationCoalescer.class);
    for (ITransactionalNotificationCoalescer notificationCoalescer : notificationCoalescers) {
      notifications = coalesce(notificationCoalescer, notifications);
    }

    return notifications;
  }

  protected Set<? extends Serializable> coalesce(ITransactionalNotificationCoalescer coalescer, Set<? extends Serializable> notifications) {
    Set<Serializable> toCoalesceNotificaitons = new HashSet<>();
    Iterator<? extends Serializable> notificationIt = notifications.iterator();
    while (notificationIt.hasNext()) {
      Serializable notification = notificationIt.next();
      if (getNotificationCoalescers(notification.getClass()).contains(coalescer)) {
        toCoalesceNotificaitons.add(notification);
        notificationIt.remove();
      }
    }
    if (!toCoalesceNotificaitons.isEmpty()) {
      notifications.addAll(coalescer.coalesce(toCoalesceNotificaitons));
    }
    return notifications;
  }

}
