/*******************************************************************************
 * Copyright (c) 2014 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.extension;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.scout.extension.AbstractLocalExtensionTestCase;
import org.eclipse.scout.rt.shared.extension.fixture.BasicForm;
import org.eclipse.scout.rt.shared.extension.fixture.BasicFormExtension;
import org.eclipse.scout.rt.shared.extension.fixture.NameFieldExtension;
import org.eclipse.scout.rt.shared.extension.fixture.NameFieldExtension02;
import org.eclipse.scout.rt.shared.extension.fixture.NestedBasicFormExtension;
import org.eclipse.scout.rt.shared.extension.fixture.StaticPojoContainerExtension;
import org.eclipse.scout.service.SERVICES;
import org.junit.Test;

public class ExtensionRegistryInstantiationTest extends AbstractLocalExtensionTestCase {

  @Test
  public void testBasicFormExtension() throws Exception {
    SERVICES.getService(IExtensionRegistry.class).register(BasicFormExtension.class);

    BasicForm basicForm = new BasicForm();
    assertExtensions(basicForm.getAllExtensions(), BasicFormExtension.class);
  }

  @Test
  public void testNameFieldExtension() throws Exception {
    SERVICES.getService(IExtensionRegistry.class).register(NameFieldExtension.class);

    BasicForm basicForm = new BasicForm();
    assertExtensions(basicForm.getAllExtensions());

    assertExtensions(basicForm.getMainBox().getAllExtensions());
    assertExtensions(basicForm.getTopBox().getAllExtensions());
    assertExtensions(basicForm.getNameField().getAllExtensions(), NameFieldExtension.class);
    assertExtensions(basicForm.getBottomBox().getAllExtensions());
    assertExtensions(basicForm.getFirstNameField().getAllExtensions());
  }

  @Test
  public void testNestedBasicFormExtension() throws Exception {
    SERVICES.getService(IExtensionRegistry.class).register(NestedBasicFormExtension.class);

    BasicForm basicForm = new BasicForm();
    assertExtensions(basicForm.getAllExtensions(), NestedBasicFormExtension.class);

    assertExtensions(basicForm.getMainBox().getAllExtensions());
    assertExtensions(basicForm.getTopBox().getAllExtensions());
    assertExtensions(basicForm.getNameField().getAllExtensions(), NestedBasicFormExtension.NameFieldExtension.class);
    assertExtensions(basicForm.getBottomBox().getAllExtensions());
    assertExtensions(basicForm.getFirstNameField().getAllExtensions(), NestedBasicFormExtension.FirstNameFieldExtension.class);
  }

  @Test
  public void testStaticPojoContainerExtension() throws Exception {
    SERVICES.getService(IExtensionRegistry.class).register(StaticPojoContainerExtension.NameFieldExtension.class);
    SERVICES.getService(IExtensionRegistry.class).register(StaticPojoContainerExtension.FirstNameFieldExtension.class);

    BasicForm basicForm = new BasicForm();
    assertExtensions(basicForm.getAllExtensions());

    assertExtensions(basicForm.getMainBox().getAllExtensions());
    assertExtensions(basicForm.getTopBox().getAllExtensions());
    assertExtensions(basicForm.getNameField().getAllExtensions(), StaticPojoContainerExtension.NameFieldExtension.class);
    assertExtensions(basicForm.getBottomBox().getAllExtensions());
    assertExtensions(basicForm.getFirstNameField().getAllExtensions(), StaticPojoContainerExtension.FirstNameFieldExtension.class);
  }

  @Test
  public void testExtensionOrder() throws Exception {
    IExtensionRegistry registry = SERVICES.getService(IExtensionRegistry.class);
    registry.register(NameFieldExtension.class);
    registry.register(NameFieldExtension02.class);

    // test code
    BasicForm basicForm = new BasicForm();
    assertExtensions(basicForm.getAllExtensions());
    assertExtensions(basicForm.getNameField().getAllExtensions(), NameFieldExtension02.class, NameFieldExtension.class);
  }

  private void assertExtensions(List<? extends IExtension<?>> extensions, Class<?>... classes) {

    if (classes == null || classes.length == 0) {
      assertEquals(1, extensions.size());
      return;
    }
    assertEquals(classes.length, extensions.size() - 1);
    for (int i = 0; i < classes.length; i++) {
      assertEquals(classes[i], extensions.get(i).getClass());
    }
  }
}
