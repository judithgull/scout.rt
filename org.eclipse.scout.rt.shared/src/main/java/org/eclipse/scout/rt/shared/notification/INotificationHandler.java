package org.eclipse.scout.rt.shared.notification;

import java.io.Serializable;

import org.eclipse.scout.rt.platform.ApplicationScoped;

/**
 * Notification handler for notifications of a given type.
 *
 * @param T
 *          the type of the notification
 * @see NotificationHandlerRegistry
 */
@ApplicationScoped
public interface INotificationHandler<T extends Serializable> {

  /**
   * Handle notifications of type T
   *
   * @param notification
   */
  void handleNotification(T notification);
}
