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
package org.eclipse.scout.rt.client.services.common.notifications;

import java.util.List;

import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.services.common.notification.NotificationDispatcher;
import org.eclipse.scout.rt.client.services.common.notifications.fixtrue.NotificationA1;
import org.eclipse.scout.rt.client.services.common.notifications.fixtrue.NotificationHandlerA;
import org.eclipse.scout.rt.client.testenvironment.TestEnvironmentClientSession;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.BeanMetaData;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IBeanManager;
import org.eclipse.scout.rt.shared.services.common.notification.INotificationServerService;
import org.eclipse.scout.rt.testing.client.runner.ClientTestRunner;
import org.eclipse.scout.rt.testing.client.runner.RunWithClientSession;
import org.eclipse.scout.rt.testing.platform.runner.RunWithSubject;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 *
 */
@RunWith(ClientTestRunner.class)
@RunWithClientSession(TestEnvironmentClientSession.class)
@RunWithSubject("anna")
public class NotificationDispatcherTest {

  private List<IBean<?>> m_serviceReg;
  private NotificationHandlerA m_notificationHandlerMock;
  private INotificationServerService m_notificationServerServiceMock;

  @Before
  public void before() throws Exception {

    m_notificationServerServiceMock = Mockito.mock(INotificationServerService.class);
    m_notificationHandlerMock = Mockito.mock(NotificationHandlerA.class);
    m_serviceReg = TestingUtility.registerBeans(
        new BeanMetaData(NotificationHandlerA.class).initialInstance(m_notificationHandlerMock).applicationScoped(true),
        new BeanMetaData(INotificationServerService.class).initialInstance(m_notificationServerServiceMock).applicationScoped(true).order(-10)
        );
    // ensure bean hander cache of notification dispatcher gets refreshed
    IBeanManager beanManager = BEANS.getBeanManager();
    IBean<NotificationDispatcher> bean = beanManager.getBean(NotificationDispatcher.class);
    beanManager.registerBean(new BeanMetaData(bean));

  }

  @After
  public void after() {
    TestingUtility.unregisterBeans(m_serviceReg);
    // ensure bean hander cache of notification dispatcher gets refreshed
    IBeanManager beanManager = BEANS.getBeanManager();
    IBean<NotificationDispatcher> bean = beanManager.getBean(NotificationDispatcher.class);
    beanManager.registerBean(new BeanMetaData(bean));
  }

  @Test
  public void test() {
    BEANS.get(NotificationDispatcher.class).dispatch((IClientSession) IClientSession.CURRENT.get(), new NotificationA1());
  }
}
