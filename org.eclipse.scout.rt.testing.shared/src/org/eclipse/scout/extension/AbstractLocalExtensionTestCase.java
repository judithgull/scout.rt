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
package org.eclipse.scout.extension;

import java.util.List;

import org.eclipse.scout.rt.shared.Activator;
import org.eclipse.scout.rt.shared.extension.ExtensionRegistry;
import org.eclipse.scout.rt.shared.extension.IExtensionRegistry;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.ServiceRegistration;

/**
 * use this superclass for test requiring a local extension registry ({@link IExtensionRegistry}). Extensions can be
 * added in every test method of subclasses and will be available only for that test method.
 */
public abstract class AbstractLocalExtensionTestCase {

  private List<ServiceRegistration> m_localServiceRegistrations;

  @Before
  public void registerLocalRegistry() {
    IExtensionRegistry localRegistry = new ExtensionRegistry();
    m_localServiceRegistrations = TestingUtility.registerServices(Activator.getDefault().getBundle(), 100, localRegistry);
  }

  @After
  public void unregisterLocalRegistry() {
    TestingUtility.unregisterServices(m_localServiceRegistrations);
  }
}
