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
package org.eclipse.scout.rt.server.services.common.code;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.shared.services.common.code.AbstractCodeType;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;
import org.eclipse.scout.rt.testing.commons.ScoutAssert;
import org.junit.Test;

/**
 * Tests for {@link UnloadCodeTypeClusterCoalescer}
 */
public class UnloadCodeTypeClusterCoalescerTest {

  @Test
  public void testCoalesceEmptySet() {
    UnloadCodeTypeClusterCoalescer coalescer = new UnloadCodeTypeClusterCoalescer();
    Set<UnloadCodeTypeCacheClusterNotification> res = coalescer.coalesce(new HashSet<UnloadCodeTypeCacheClusterNotification>());
    assertTrue(res.isEmpty());
  }

  @Test
  public void testCoalesceNotificationsSet() {
    UnloadCodeTypeClusterCoalescer coalescer = new UnloadCodeTypeClusterCoalescer();
    List<Class<? extends ICodeType<?, ?>>> testTypes1 = new ArrayList<>();
    List<Class<? extends ICodeType<?, ?>>> testTypes2 = new ArrayList<>();
    testTypes1.add(CodeType1.class);
    testTypes1.add(CodeType2.class);
    testTypes2.add(CodeType2.class);
    Set<UnloadCodeTypeCacheClusterNotification> testList = CollectionUtility.hashSet(
        new UnloadCodeTypeCacheClusterNotification(testTypes1),
        new UnloadCodeTypeCacheClusterNotification(testTypes1));
    CollectionUtility.arrayList(CodeType2.class);
    Set<UnloadCodeTypeCacheClusterNotification> res = coalescer.coalesce(testList);
    assertEquals(1, res.size());
    UnloadCodeTypeCacheClusterNotification firstNotification = res.iterator().next();
    ScoutAssert.assertSetEquals(testTypes1, firstNotification.getTypes());

  }

  class CodeType1 extends AbstractCodeType<Long, Long> {
    private static final long serialVersionUID = 1L;

    @Override
    public Long getId() {
      return 0L;
    }
  }

  class CodeType2 extends AbstractCodeType<Long, Long> {
    private static final long serialVersionUID = 1L;

    @Override
    public Long getId() {
      return 0L;
    }
  }

}
