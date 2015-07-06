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
package org.eclipse.scout.rt.server.services.common.context;

import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.server.notification.INotificationCoalescer;
import org.eclipse.scout.rt.shared.services.common.context.SharedContextChangedNotification;

/**
 *
 */
public class SharedContextNotificationCoalescer implements INotificationCoalescer<SharedContextChangedNotification> {

  @Override
  public Set<SharedContextChangedNotification> coalesce(Set<SharedContextChangedNotification> notifications) {
    // reduce to one
    return CollectionUtility.hashSet(CollectionUtility.firstElement(notifications));
  }

}
