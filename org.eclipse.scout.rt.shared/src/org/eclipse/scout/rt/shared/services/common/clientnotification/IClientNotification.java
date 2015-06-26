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
package org.eclipse.scout.rt.shared.services.common.clientnotification;

import java.io.Serializable;

/**
 * Client notifications are used to trigger events from the server to the client
 * (reverse HTTP) <br>
 * These notifications are collected on the server side in a {@link ClientNotificationQueue} and
 * fetched by the {@link ClientNotificationConsumer} using {@link IClientNotificationService} with intelligent polling. <br>
 * Note that client notifications must be serializable because they are transferred between server and client. <br>
 * All client notifications must therefore be defined in "shared" plugins known by server and client.
 * <p>
 * Examples:
 * <code>AccessControlChangedNotification, SharedContextChangedNotification, CodeTypeChangedNotification</code>
 */
public interface IClientNotification extends Serializable {

  /**
   * @return the unique id of this notification.
   */
  String getId();

  /**
   * @return the timeout in ms. After this timeout the notification is expired.
   */
  long getTimeout();

  /**
   * Merge with other notifications of the same type Same type means
   * n1.getClass()==n2.getClass()
   *
   * @return true if existingNotification was coalesced and therefore is
   *         consumed. The existingNotification is then removed from the queue.
   */
  boolean coalesce(IClientNotification existingNotification);

  /**
   * the node where the notification is originally fired. For cluster environments.
   */
  String getOriginalServerNode();

  /**
   * Sets the node where the notification is originally fired. For cluster environments.
   *
   * @param nodeId
   */
  void setOriginalServerNode(String nodeId);

  /**
   * @return id of the server node delivering the notification to the client
   */
  String getProvidingServerNode();

  /**
   * id of the server node delivering the notification to the client
   *
   * @param nodeId
   */
  void setProvidingServerNode(String nodeId);
}
