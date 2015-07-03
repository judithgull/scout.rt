package org.eclipse.scout.rt.shared.notification;

import java.io.Serializable;

import org.eclipse.scout.rt.platform.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public interface INotificationHandler<T extends Serializable> {

  void handleNotification(T notification);
}
