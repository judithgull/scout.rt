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
package org.eclipse.scout.rt.shared.services.common.security;

import java.io.Serializable;
import java.security.Permissions;

/**
 * Notification is sent from server to client to notify that the permission set
 * has changed
 */
public class AccessControlChangedNotification implements Serializable {
  private static final long serialVersionUID = 1L;

  private Permissions m_permissions;

  public AccessControlChangedNotification(Permissions permissions) {
    m_permissions = permissions;
  }

  public Permissions getPermissions() {
    return m_permissions;
  }

}
