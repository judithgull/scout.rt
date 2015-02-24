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
package org.eclipse.scout.rt.platform.cdi.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.interceptor.InterceptorBinding;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.BeanUtility;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.cdi.ApplicationScoped;
import org.eclipse.scout.rt.platform.cdi.Bean;
import org.eclipse.scout.rt.platform.cdi.CreateImmediately;
import org.eclipse.scout.rt.platform.cdi.IBean;
import org.eclipse.scout.rt.platform.cdi.IBeanContext;

/**
 *
 */
public class BeanContext implements IBeanContext {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(BeanContext.class);
  private final Map<Class<?>, TreeSet<IBean<?>>> m_beans;
  private final Map<Class<? extends Annotation>, ?> m_interceptors;

  public BeanContext() {
    m_beans = new HashMap<Class<?>, TreeSet<IBean<?>>>();
    m_interceptors = new BeansXmlParser().getInterceptors();
  }

  @Override
  public <T> IBean<T> register(Class<T> beanClazz) {
    Bean<T> bean = new Bean<T>(beanClazz);
    register(bean);
    return bean;
  }

  @Override
  public void register(IBean<?> bean) {
    Class[] interfacesHierarchy = BeanUtility.getInterfacesHierarchy(bean.getBeanClazz(), Object.class);
    List<Class<?>> clazzes = new ArrayList<Class<?>>(interfacesHierarchy.length + 1);
    clazzes.add(bean.getBeanClazz());
    for (Class<?> c : interfacesHierarchy) {
      clazzes.add(c);
    }
    registerBean(clazzes, bean);
  }

  public synchronized void registerBean(List<Class<?>> clazzes, IBean<?> bean) {
    IBean<?> interceptedBean = createInterceptedBean(bean);
    for (Class<?> clazz : clazzes) {
      TreeSet<IBean<?>> beans = m_beans.get(clazz);
      if (beans == null) {
        beans = new TreeSet<IBean<?>>(new P_BeanComparator());
        m_beans.put(clazz, beans);
      }
      beans.add(interceptedBean);
    }
  }

  /**
   * @param bean
   * @return
   */
  private <T> IBean<T> createInterceptedBean(IBean<T> bean) {
    for (Annotation a : bean.getBeanAnnotations().values()) {
      if (a.annotationType().getAnnotation(InterceptorBinding.class) != null) {
        Object interceptor = m_interceptors.get(a.annotationType());
        if (interceptor != null) {
          return new InterceptedBean<T>(bean, interceptor);
        }
      }
    }
    return bean;
  }

  @Override
  public synchronized void unregisterBean(Class<?> clazz) {
    for (Set<IBean<?>> beans : m_beans.values()) {
      Iterator<IBean<?>> beanIt = beans.iterator();
      while (beanIt.hasNext()) {
        if (beanIt.next().getBeanClazz().equals(clazz)) {
          beanIt.remove();
        }
      }
    }
  }

  @Override
  public synchronized void unregisterBean(IBean<?> bean) {
    Assertions.assertNotNull(bean);
    for (Set<IBean<?>> beans : m_beans.values()) {
      Iterator<IBean<?>> beanIt = beans.iterator();
      while (beanIt.hasNext()) {
        if (beanIt.next().equals(bean)) {
          beanIt.remove();
        }
      }
    }
  }

  @Override
  public <T> List<T> getInstances(Class<T> beanClazz) {
    List<IBean<T>> beans = getBeans(beanClazz);
    List<T> instances = new ArrayList<T>(beans.size());
    for (IBean<T> bean : beans) {
      instances.add(bean.get());
    }
    return instances;
  }

  @Override
  public <T> T getInstance(Class<T> beanClazz, T defaultBean) {
    IBean<T> bean = getBean(beanClazz);
    if (bean != null) {
      return bean.get();
    }
    else {
      return defaultBean;
    }
  }

  @Override
  public <T> T getInstance(Class<T> beanClazz) {
    T bean = getInstance(beanClazz, null);
    if (bean == null) {
      LOG.warn(String.format("No beans bound to '%s'", beanClazz), new Exception());
    }
    return bean;
  }

  @SuppressWarnings("unchecked")
  public <T> IBean<T> getBean(Class<T> beanClazz) {
    TreeSet<IBean<?>> beans = getBeansInternal(beanClazz);
    if (beans != null && beans.size() > 0) {
      return (IBean<T>) beans.first();
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T> List<IBean<T>> getBeans(Class<T> beanClazz) {
    TreeSet<IBean<?>> beans = getBeansInternal(beanClazz);
    List<IBean<T>> result = new ArrayList<IBean<T>>(beans.size());
    for (IBean<?> bean : beans) {
      result.add((IBean<T>) bean);
    }
    return result;
  }

  private synchronized TreeSet<IBean<?>> getBeansInternal(Class<?> beanClazz) {
    Assertions.assertNotNull(beanClazz);
    TreeSet<IBean<?>> beans = m_beans.get(beanClazz);
    if (beans == null) {
      return CollectionUtility.emptyTreeSet();
    }
    if (!beanClazz.isInterface()) {
      // check no proxy
      for (IBean<?> b : beans) {
        if (b.isIntercepted()) {
          throw new IllegalArgumentException(String.format("Intercepted beans can only be accessed with an interface. '%s' is not an interface.", beanClazz.getName()));
        }
      }
    }
    return beans;
  }

  public List<IBean<?>> getAllRegisteredBeans() {
    List<IBean<?>> allBeans = new LinkedList<IBean<?>>();
    for (Set<IBean<?>> beans : m_beans.values()) {
      allBeans.addAll(beans);
    }
    return allBeans;
  }

  public static <T> T createInstance(Class<T> clazz) {
    Assertions.assertNotNull(clazz);
    T instance = null;
    try {
      Constructor<? extends T> constructor = clazz.getDeclaredConstructor();
      if (constructor != null) {
        constructor.setAccessible(true);
        instance = constructor.newInstance();

      }
      else {
        LOG.error(String.format("Default constructor of '%s' not found. Ensure to have an empty constructor.", clazz.getName()));
      }
    }
    catch (Exception e) {
      LOG.error(String.format("Could not instantiate '%s'.", clazz), e);
    }
    return instance;
  }

  private class P_BeanComparator implements Comparator<IBean<?>> {
    @Override
    public int compare(IBean<?> bean1, IBean<?> bean2) {
      if (bean1 == bean2) {
        return 0;
      }
      if (bean1 == null) {
        return -1;
      }
      if (bean2 == null) {
        return 1;
      }

      int result = Float.compare(getPriority(bean2), getPriority(bean1));
      if (result != 0) {
        return result;
      }

      return bean1.getBeanClazz().getName().compareTo(bean2.getBeanClazz().getName());
    }

    public float getPriority(IBean<?> bean) {
      float prio = -1;
      Priority priorityAnnotation = bean.getBeanAnnotation(Priority.class);
      if (priorityAnnotation != null) {
        prio = priorityAnnotation.value();
      }
      return prio;
    }
  }

  public static boolean isCreateImmediately(IBean<?> bean) {
    return bean.getBeanAnnotation(CreateImmediately.class) != null;
  }

  public static boolean isApplicationScoped(IBean<?> bean) {
    return bean.getBeanAnnotation(ApplicationScoped.class) != null;
  }
}