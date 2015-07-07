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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.server.services.common.security.AccessControlClusterNotification;
import org.eclipse.scout.rt.testing.commons.ScoutAssert;
import org.junit.Test;

/**
 * Tests for {@link NotificationCoalescer}
 */
public class NotificationCoalescerTest {

  @Test
  public void testCoalesceEmptySet() {
    Set<? extends Serializable> res = BEANS.get(NotificationCoalescer.class).coalesce(new HashSet<Serializable>());
    assertTrue(res.isEmpty());
  }

  /**
   * Tests that AccessControlClusterNotification are coalesced to one notification containing all users.
   */
  @Test
  public void test() {
    AccessControlClusterNotification n1 = new AccessControlClusterNotification(CollectionUtility.hashSet("user1", "user2"));
    AccessControlClusterNotification n2 = new AccessControlClusterNotification(CollectionUtility.hashSet("user1", "user3"));
    AccessControlClusterNotification n3 = new AccessControlClusterNotification();
    HashSet<AccessControlClusterNotification> testNotifications = CollectionUtility.hashSet(n1, n2, n3);

    Set<? extends Serializable> res = BEANS.get(NotificationCoalescer.class).coalesce(testNotifications);

    assertEquals(1, res.size());
    AccessControlClusterNotification firstNotification = (AccessControlClusterNotification) res.iterator().next();
    ScoutAssert.assertSetEquals(CollectionUtility.hashSet("user1", "user2", "user3"), firstNotification.getUserIds());
  }

}
