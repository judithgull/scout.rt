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
package org.eclipse.scout.rt.server.services.common.security.fixture;

import java.security.Permissions;
import java.util.Collection;

import org.eclipse.scout.rt.server.services.common.security.AbstractAccessControlService;
import org.eclipse.scout.rt.shared.security.RemoteServiceAccessPermission;

/**
 * An access control service with {@link TestPermission1} for testing
 */
public class TestAccessControlService extends AbstractAccessControlService {
  /**
   * Loads a test permission
   */
  @Override
  protected Permissions execLoadPermissions() {
    Permissions permissions = new Permissions();
    permissions.add(new TestPermission1());
    permissions.add(new RemoteServiceAccessPermission("*.shared.*", "*"));
    return permissions;
  }

  public void callClearCacheNoFire() {
    clearCacheNoFire();
  }

  public void callClearCacheOfUserIdsNoFire(Collection<String> userIds) {
    clearCacheOfUserIdsNoFire(userIds);
  }
}