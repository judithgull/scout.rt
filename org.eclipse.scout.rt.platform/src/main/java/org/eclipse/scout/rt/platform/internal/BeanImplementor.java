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
package org.eclipse.scout.rt.platform.internal;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.platform.IBean;

public class BeanImplementor<T> implements IBean<T> {
  private final Class<? extends T> m_beanClazz;
  private final Map<Class<? extends Annotation>, Annotation> m_beanAnnotations;

  public BeanImplementor(Class<? extends T> clazz) {
    m_beanClazz = Assertions.assertNotNull(clazz);
    m_beanAnnotations = new HashMap<>();
    readStaticAnnoations(clazz, false);
  }

  /**
   * @return
   */
  protected void readStaticAnnoations(Class<?> clazz, boolean inheritedOnly) {
    if (clazz == null || Object.class.getName().equals(clazz.getName())) {
      return;
    }
    for (Annotation a : clazz.getAnnotations()) {
      if (inheritedOnly) {
        if (a.annotationType().getAnnotation(Inherited.class) != null) {
          m_beanAnnotations.put(a.annotationType(), a);
        }
      }
      else {
        m_beanAnnotations.put(a.annotationType(), a);
      }
    }
    readStaticAnnoations(clazz.getSuperclass(), true);
  }

  @Override
  public Class<? extends T> getBeanClazz() {
    return m_beanClazz;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <ANNOTATION extends Annotation> ANNOTATION getBeanAnnotation(Class<ANNOTATION> annotationClazz) {
    synchronized (m_beanAnnotations) {
      return (ANNOTATION) m_beanAnnotations.get(annotationClazz);
    }
  }

  @Override
  public Map<Class<? extends Annotation>, Annotation> getBeanAnnotations() {
    synchronized (m_beanAnnotations) {
      return new HashMap<Class<? extends Annotation>, Annotation>(m_beanAnnotations);
    }
  }

  public void setBeanAnnotations(Map<Class<? extends Annotation>, Annotation> annotations) {
    synchronized (m_beanAnnotations) {
      m_beanAnnotations.clear();
      m_beanAnnotations.putAll(annotations);
    }
  }

  public void addAnnotation(Annotation annotation) {
    synchronized (m_beanAnnotations) {
      m_beanAnnotations.put(annotation.annotationType(), annotation);
    }
  }

  public void removeAnnotation(Annotation annotation) {
    synchronized (m_beanAnnotations) {
      m_beanAnnotations.remove(annotation.annotationType());
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + CollectionUtility.hashCode(m_beanAnnotations.values());
    result = prime * result + m_beanClazz.hashCode();
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    BeanImplementor other = (BeanImplementor) obj;
    if (!CollectionUtility.equalsCollection(m_beanAnnotations.values(), other.m_beanAnnotations.values())) {
      return false;
    }
    if (!m_beanClazz.equals(other.m_beanClazz)) {
      return false;
    }
    return true;
  }
}