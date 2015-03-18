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
package org.eclipse.scout.rt.platform;

import java.util.List;

import org.eclipse.scout.rt.platform.internal.BeanContextImplementor;

/**
 * The static accessor to the {@link BeanContextImplementor}
 */
public final class OBJ {

  private OBJ() {
  }

  /**
   * @deprecated use {@link #get(Class)}
   */
  @Deprecated
  public static <T> T one(Class<T> beanClazz) {
    return get(beanClazz);
  }

  /**
   * @deprecated use {@link #getOptional(Class)}
   */
  @Deprecated
  public static <T> T oneOrNull(Class<T> beanClazz) {
    return getOptional(beanClazz);
  }

  /**
   * @return the first instance of this type.
   */
  public static <T> T get(Class<T> beanClazz) {
    return Platform.get().getBeanContext().getInstance(beanClazz);
  }

  public static <T> T getOptional(Class<T> beanClazz) {
    return Platform.get().getBeanContext().getInstanceOrNull(beanClazz);
  }

  /**
   * @return all instances of this type
   */
  public static <T> List<T> all(Class<T> beanClazz) {
    return Platform.get().getBeanContext().getInstances(beanClazz);
  }
}