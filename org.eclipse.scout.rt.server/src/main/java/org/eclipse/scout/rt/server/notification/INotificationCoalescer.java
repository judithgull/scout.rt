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
package org.eclipse.scout.rt.server.notification;

import java.io.Serializable;
import java.util.Set;

import org.eclipse.scout.rt.platform.ApplicationScoped;

/**
 * Every implementation of this interface will be applied to coalesce notifications of the same super class (generic
 * type of this class).
 * <b>
 * Will only be called for notifications within a transaction
 */
@ApplicationScoped
public interface INotificationCoalescer<NOTIFICATION extends Serializable> {

  /**
   * @param notifications
   *          all notifications to coalesce.
   * @return a coalesced set of notifications. Never <code>null</code>.
   */
  Set<NOTIFICATION> coalesce(Set<NOTIFICATION> notifications);

}
