package org.eclipse.scout.rt.shared.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;

/**
 *
 */
@ApplicationScoped
public class NotificationHandlerRegistry {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(NotificationHandlerRegistry.class);

  private final Map<Class<? extends Serializable> /*notification class*/, List<INotificationHandler<? extends Serializable>>> m_notificationClassToHandler = new HashMap<>();
  private final Map<Class<? extends Serializable> /*notification class*/, List<INotificationHandler<? extends Serializable>>> m_cachedHandlers = new HashMap<>();
  private final Object m_cacheLock = new Object();

  /**
   * builds a linking of all handlers generic types to handlers. This is used to find the corresponding handler of a
   * notification.
   */
  @SuppressWarnings("unchecked")
  @PostConstruct
  protected void buildHandlerLinking() {
    List<INotificationHandler> notificationHandlers = BEANS.all(INotificationHandler.class);
    for (INotificationHandler<?> notificationHandler : notificationHandlers) {
      Class notificationClass = TypeCastUtility.getGenericsParameterClass(notificationHandler.getClass(), INotificationHandler.class);
      List<INotificationHandler<?>> handlerList = m_notificationClassToHandler.get(notificationClass);
      if (handlerList == null) {
        handlerList = new LinkedList<>();
        m_notificationClassToHandler.put(notificationClass, handlerList);
      }
      handlerList.add(notificationHandler);
    }
  }

  @SuppressWarnings("unchecked")
  public void notifyHandlers(Serializable notification) {
    List<INotificationHandler<? extends Serializable>> handlers = getNotificationHandlers(notification.getClass());
    for (INotificationHandler handler : handlers) {
      try {
        handler.handleNotification(notification);
      }
      catch (Exception e) {
        LOG.error(String.format("Handler '%s' notification with notification '%s' failed.", handler, notification), e);
      }
    }
  }

  protected List<INotificationHandler<? extends Serializable>> getNotificationHandlers(Class<? extends Serializable> notificationClass) {
    synchronized (m_cacheLock) {
      List<INotificationHandler<? extends Serializable>> notificationHandlers = m_cachedHandlers.get(notificationClass);
      if (notificationHandlers == null) {
        notificationHandlers = findHandlers(notificationClass);
        m_cachedHandlers.put(notificationClass, notificationHandlers);
      }
      return new ArrayList<INotificationHandler<? extends Serializable>>(notificationHandlers);
    }
  }

  protected List<INotificationHandler<? extends Serializable>> findHandlers(Class<? extends Serializable> notificationClass) {
    List<INotificationHandler<? extends Serializable>> handlers = new LinkedList<>();
    synchronized (m_cacheLock) {
      for (Entry<Class<? extends Serializable> /*notification class*/, List<INotificationHandler<? extends Serializable>>> e : m_notificationClassToHandler.entrySet()) {
        if (e.getKey().isAssignableFrom(notificationClass)) {
          handlers.addAll(e.getValue());
        }
      }
    }
    return handlers;
  }

}
