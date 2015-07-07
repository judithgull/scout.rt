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
package org.eclipse.scout.rt.server.services.common.bookmark;

import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.server.notification.INotificationCoalescer;
import org.eclipse.scout.rt.shared.services.common.bookmark.BookmarkChangedClientNotification;

/**
 *
 */
public class BookmarkNotificationCoalescer implements INotificationCoalescer<BookmarkChangedClientNotification> {

  @Override
  public Set<BookmarkChangedClientNotification> coalesce(Set<BookmarkChangedClientNotification> notifications) {
    // reduce to one
    return CollectionUtility.hashSet(CollectionUtility.firstElement(notifications));
  }

}
