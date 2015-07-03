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
package org.eclipse.scout.rt.shared.services.common.useractivity;

import java.io.Serializable;

public class UserActivityClientNotification implements Serializable {

  private static final long serialVersionUID = 1L;
  private final UserStatusMap m_map;

  public UserActivityClientNotification(UserStatusMap map) {
    m_map = map;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    return obj.getClass() == getClass();
  }

  public UserStatusMap getUserStatusMap() {
    return m_map;
  }

}
