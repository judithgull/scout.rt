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
package org.eclipse.scout.rt.shared.notification;

import java.io.Serializable;
import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.BeanMetaData;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IBeanManager;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Tests for {@link NotificationHandlerRegistry}
 */
@RunWith(PlatformTestRunner.class)
public class NotificationHandlerRegistryTest {
  private List<IBean<?>> m_serviceReg;

  //notification handler for all Serializables
  private GlobalNotificationHandler m_globalNotificationHanlder;
  //notification handler for all INotificationGroup
  private GroupNotificationHandler m_groupNotificationHanlder;

  @Before
  public void before() throws Exception {
    m_globalNotificationHanlder = Mockito.mock(GlobalNotificationHandler.class);
    m_groupNotificationHanlder = Mockito.mock(GroupNotificationHandler.class);
    m_serviceReg = TestingUtility.registerBeans(
        new BeanMetaData(GlobalNotificationHandler.class).initialInstance(m_globalNotificationHanlder).applicationScoped(true),
        new BeanMetaData(GroupNotificationHandler.class).initialInstance(m_groupNotificationHanlder).applicationScoped(true)
        );

    // ensure bean hander cache of notification dispatcher gets refreshed
    ensureHandlerRegistryRefreshed();
  }

  @After
  public void after() {
    TestingUtility.unregisterBeans(m_serviceReg);
    ensureHandlerRegistryRefreshed();
  }

  private void ensureHandlerRegistryRefreshed() {
    IBeanManager beanManager = BEANS.getBeanManager();
    IBean<NotificationHandlerRegistry> bean = beanManager.getBean(NotificationHandlerRegistry.class);
    beanManager.unregisterBean(bean);
    beanManager.registerBean(new BeanMetaData(bean));
  }

  /**
   * Tests that a notification of type {@link String} is only handled by handlers for Strings.
   **/
  @Test
  public void testStringNotification() throws ProcessingException {
    NotificationHandlerRegistry reg = BEANS.get(NotificationHandlerRegistry.class);
    reg.notifyHandlers("A simple string notification");
    Mockito.verify(m_globalNotificationHanlder, Mockito.times(1)).handleNotification(Mockito.any(Serializable.class));
    Mockito.verify(m_groupNotificationHanlder, Mockito.times(0)).handleNotification(Mockito.any(INotificationGroup.class));
  }

  /**
   * Tests that a notification of type {@link INotificationGroup} is only handled by handlers for INotificationGroups.
   **/
  @Test
  public void testNotificationGroup() throws ProcessingException {
    NotificationHandlerRegistry reg = BEANS.get(NotificationHandlerRegistry.class);
    reg.notifyHandlers(new Notification01());
    reg.notifyHandlers(new Notification01());
    Mockito.verify(m_globalNotificationHanlder, Mockito.times(2)).handleNotification(Mockito.any(Serializable.class));
    Mockito.verify(m_groupNotificationHanlder, Mockito.times(2)).handleNotification(Mockito.any(INotificationGroup.class));
  }

  private static class GlobalNotificationHandler implements INotificationHandler<Serializable> {

    @Override
    public void handleNotification(Serializable notification) {
    }
  }

  private static class GroupNotificationHandler implements INotificationHandler<INotificationGroup> {

    @Override
    public void handleNotification(INotificationGroup notification) {
    }
  }

  public static interface INotificationGroup extends Serializable {

  }

  private static final class Notification01 implements INotificationGroup {

    private static final long serialVersionUID = 1L;
  }
}
