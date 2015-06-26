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

import java.util.EventListener;

import org.eclipse.scout.commons.exception.ProcessingException;

/**
 * Listener for reacting to cluster notifications
 */
public interface IClusterNotificationListener extends EventListener {

  /**
   * Handle the cluster notification
   * 
   * @param notificationMessage
   */
  void onNotification(IClusterNotificationMessage message) throws ProcessingException;

}
