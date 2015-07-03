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
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;

/**
 *
 */
@ApplicationScoped
public class NotificationHandlerRegistry {

  private final Map<Class<? extends Serializable> /*notification class*/, List<INotificationHandler<? extends Serializable>>> m_notificationClassToNotifcationHandler = new HashMap<>();
  private final Map<Class<? extends Serializable> /*notification class*/, List<INotificationHandler<? extends Serializable>>> m_cachedNotificationHandlers = new HashMap<>();
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
      List<INotificationHandler<?>> handlerList = m_notificationClassToNotifcationHandler.get(notificationClass);
      if (handlerList == null) {
        handlerList = new LinkedList<>();
        m_notificationClassToNotifcationHandler.put(notificationClass, handlerList);
      }
      handlerList.add(notificationHandler);
    }
  }

  public List<INotificationHandler<? extends Serializable>> getNotificationHandlers(Class<? extends Serializable> notificationClass) {
    synchronized (m_cacheLock) {
      List<INotificationHandler<? extends Serializable>> notificationHandlers = m_cachedNotificationHandlers.get(notificationClass);
      if (notificationHandlers == null) {
        notificationHandlers = findNotificationHandlers(notificationClass);
        m_cachedNotificationHandlers.put(notificationClass, notificationHandlers);
      }
      return new ArrayList<INotificationHandler<? extends Serializable>>(notificationHandlers);
    }
  }

  protected List<INotificationHandler<? extends Serializable>> findNotificationHandlers(Class<? extends Serializable> notificationClass) {
    List<INotificationHandler<? extends Serializable>> handlers = new LinkedList<>();
    synchronized (m_cacheLock) {
      for (Entry<Class<? extends Serializable> /*notification class*/, List<INotificationHandler<? extends Serializable>>> e : m_notificationClassToNotifcationHandler.entrySet()) {
        if (e.getKey().isAssignableFrom(notificationClass)) {
          handlers.addAll(e.getValue());
        }
      }
    }
    return handlers;
  }

}
