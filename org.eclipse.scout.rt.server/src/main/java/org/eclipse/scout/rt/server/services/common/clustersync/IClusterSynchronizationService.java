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
package org.eclipse.scout.rt.server.services.common.clustersync;

import java.io.Serializable;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.platform.service.IService;

/**
 * Service for synchronizing server nodes by sending messages and receiving messages.
 */
public interface IClusterSynchronizationService extends IService {

  /**
   * Publish a message with the given notification for the other server nodes.
   */
  void publish(Serializable notification) throws ProcessingException;

  /**
   * Publish a message with the given notification for the other server nodes, if the transaction is commited.
   */
  void publishTransactional(Serializable notification) throws ProcessingException;

  /**
   * @return the node of the currently connected cluster
   */
  String getNodeId();

  /**
   * @return info about sent and received messages
   */
  IClusterNodeStatusInfo getStatusInfo();

  /**
   * Starts listening to notifications
   *
   * @return <code>true</code>, if successful
   */
  boolean enable();

  /**
   * Stops listening to notifications
   *
   * @return <code>true</code>, if successful
   */
  boolean disable();

  /**
   * @return <code>true</code>, if started and listening to cluster notifications
   */
  boolean isEnabled();

  /**
   * @param listener
   *          {@link IClusterNotificationListener}
   */
  void addListener(IClusterNotificationListener listener);

  /**
   * @param listener
   *          {@link IClusterNotificationListener}
   */
  void removeListener(IClusterNotificationListener listener);

}
