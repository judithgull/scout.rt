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
package org.eclipse.scout.rt.platform.inventory.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.scout.rt.platform.cdi.ApplicationScoped;
import org.eclipse.scout.rt.platform.cdi.Bean;
import org.eclipse.scout.rt.platform.cdi.IBean;
import org.eclipse.scout.rt.platform.cdi.IBeanContext;
import org.eclipse.scout.rt.platform.cdi.internal.PlatformBeanContributor;
import org.eclipse.scout.rt.platform.cdi.internal.scan.fixture.TestingBean;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class BeanScannerTest {

  @Test
  public void testJandex() throws Exception {
    JandexInventoryBuilder finder = new JandexInventoryBuilder();

    String basePath = TestingBean.class.getName().replace('.', '/');
    for (String path : new String[]{
        Bean.class.getName().replace('.', '/'),
        ApplicationScoped.class.getName().replace('.', '/'),
        basePath,
        basePath + "2",
        basePath + "$S1",
        basePath + "$S2",
        basePath + "$S3",
        basePath + "$S4",
        basePath + "$M1",
        basePath + "$I1",
        basePath + "$I2",
        basePath + "$E1",
        basePath + "$A1",
    }) {
      finder.handleClass(path.replace('/', '.'), TestingBean.class.getClassLoader().getResource(path + ".class"));
    }
    finder.finish();
    JandexClassInventory classInventory = new JandexClassInventory(finder.getIndex());

    final Set<Class> actual = new HashSet<>();
    IBeanContext context = Mockito.mock(IBeanContext.class);
    Mockito.when(context.registerClass(Mockito.<Class<?>> any())).thenAnswer(new Answer<IBean<?>>() {
      @Override
      public IBean<?> answer(InvocationOnMock invocation) throws Throwable {
        actual.add((Class) invocation.getArguments()[0]);
        return null;
      }
    });

    PlatformBeanContributor contrib = new PlatformBeanContributor();
    contrib.contributeBeans(classInventory, context);

    HashSet<Class> expected = new HashSet<Class>();
    expected.add(org.eclipse.scout.rt.platform.cdi.internal.scan.fixture.TestingBean.class);
    expected.add(org.eclipse.scout.rt.platform.cdi.internal.scan.fixture.TestingBean.S1.class);
    expected.add(Class.forName(TestingBean.class.getName() + "$S2"));
    expected.add(org.eclipse.scout.rt.platform.cdi.internal.scan.fixture.TestingBean.I1.class);

    Assert.assertEquals(expected, actual);
  }
}