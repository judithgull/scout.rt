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

import java.util.List;

import org.eclipse.scout.rt.shared.TunnelToServer;

@TunnelToServer
public interface INotificationServerService {

  void register(String notificationNodeId);

  void unregister(String notificationNodeId);

  List<NotificationMessage> getNotifications(String notificationNodeId);

}
