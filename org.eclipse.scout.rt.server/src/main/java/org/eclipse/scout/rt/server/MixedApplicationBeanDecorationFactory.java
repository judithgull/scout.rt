package org.eclipse.scout.rt.server;

import org.eclipse.scout.commons.annotations.Replace;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IBeanDecorationFactory;
import org.eclipse.scout.rt.platform.SimpleBeanDecorationFactory;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.interceptor.IBeanInterceptor;
import org.eclipse.scout.rt.server.context.ServerRunContext;
import org.eclipse.scout.rt.shared.SharedConfigProperties.CreateTunnelToServerBeansProperty;
import org.eclipse.scout.rt.shared.TunnelToServer;

/**
 * {@link IBeanDecorationFactory} to be installed in mixed-application mode to ensure proper {@link ServerRunContext}
 * when invoking services with a server-side implementation.
 */
@Replace
public class MixedApplicationBeanDecorationFactory extends SimpleBeanDecorationFactory {

  @Override
  public <T> IBeanInterceptor<T> decorate(final IBean<T> bean, final Class<? extends T> queryType) {
    if (!CONFIG.getPropertyValue(CreateTunnelToServerBeansProperty.class) && bean.getBeanAnnotation(TunnelToServer.class) != null) {
      return installServerRunContextEnforcer(bean, queryType);
    }
    return super.decorate(bean, queryType);
  }

  protected <T> IBeanInterceptor<T> installServerRunContextEnforcer(final IBean<T> bean, final Class<? extends T> queryType) {
    return new ServerRunContextEnforcer<T>();
  }
}
