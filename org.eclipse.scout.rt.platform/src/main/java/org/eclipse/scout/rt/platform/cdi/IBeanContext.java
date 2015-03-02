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

import java.util.List;

import org.eclipse.scout.commons.Assertions.AssertionException;

/**
 *
 */
public interface IBeanContext {

  /**
   * @param beanClazz
   * @return the instance of the given bean.
   * @throws AssertionException
   *           when no bean is registered to the given beanClazz
   */
  <T> T getInstance(Class<T> beanClazz);

  /**
   * Nullsave {@link #getInstance(Class)}
   *
   * @param beanClazz
   * @return
   */
  <T> T getInstanceOrNull(Class<T> beanClazz);

  /**
   * @param beanClazz
   * @return
   */
  <T> List<T> getInstances(Class<T> beanClazz);

  /**
   * @return the bean registered under the given beanClazz
   * @throws AssertionException
   *           when no bean is registered to the given beanClazz
   */
  <T> IBean<T> getBean(Class<T> beanClazz);

  /**
   * Nullsave {@link #getBean(Class)}
   *
   * @param beanClazz
   * @return
   */
  <T> IBean<T> getBeanOrNull(Class<T> beanClazz);

  /**
   * @param beanClazz
   * @return
   */
  <T> List<IBean<T>> getBeans(Class<T> beanClazz);

  /**
   * @return
   */
  List<IBean<?>> getAllRegisteredBeans();

  /**
   * @param beanClazz
   * @return
   */
  <T> IBean<T> registerClass(Class<T> clazz);

  /**
   * @param bean
   */
  void registerBean(IBean<?> bean);

  /**
   * @param bean
   */
  void unregisterBean(IBean<?> bean);

}