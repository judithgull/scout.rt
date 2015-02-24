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
package org.eclipse.scout.rt.platform.cdi;

import org.eclipse.scout.rt.platform.cdi.internal.BeanContext;
import org.eclipse.scout.rt.testing.platform.ScoutPlatformTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ScoutPlatformTestRunner.class)
public class BeanRegisterUnregisterTest {

  @Test
  public void test() {
    BeanContext context = new BeanContext();
    context.register(TestObject.class);
    Assert.assertEquals(1, context.getAllRegisteredBeans().size());
    context.unregisterBean(TestObject.class);
    Assert.assertEquals(0, context.getAllRegisteredBeans().size());
  }

  private static class TestObject {

  }
}