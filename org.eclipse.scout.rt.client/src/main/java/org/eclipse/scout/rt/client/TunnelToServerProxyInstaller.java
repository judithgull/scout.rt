package org.eclipse.scout.rt.client;

import org.eclipse.scout.commons.VerboseUtility;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.BeanMetaData;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IBeanInstanceProducer;
import org.eclipse.scout.rt.platform.IBeanManager;
import org.eclipse.scout.rt.platform.IPlatform;
import org.eclipse.scout.rt.platform.IPlatformListener;
import org.eclipse.scout.rt.platform.PlatformEvent;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.exception.PlatformException;
import org.eclipse.scout.rt.platform.interceptor.IBeanInterceptor;
import org.eclipse.scout.rt.platform.interceptor.IBeanInvocationContext;
import org.eclipse.scout.rt.platform.interceptor.internal.BeanProxyImplementor;
import org.eclipse.scout.rt.platform.inventory.ClassInventory;
import org.eclipse.scout.rt.platform.inventory.IClassInfo;
import org.eclipse.scout.rt.shared.SharedConfigProperties.CreateTunnelToServerBeansProperty;
import org.eclipse.scout.rt.shared.TunnelToServer;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform listener which registers a remote proxy for every service interface annotated with {@link TunnelToServer}.
 * <p>
 * This listener is not to be used in mixed-application mode with client and server running in the same classloader.
 */
public class TunnelToServerProxyInstaller implements IPlatformListener {

  private static final Logger LOG = LoggerFactory.getLogger(TunnelToServerProxyInstaller.class);

  @Override
  public void stateChanged(final PlatformEvent event) throws PlatformException {
    if (event.getState() == IPlatform.State.BeanManagerPrepared) {
      registerTunnelToServerProxies(event.getSource().getBeanManager());
    }
  }

  protected void registerTunnelToServerProxies(final IBeanManager beanManager) {
    if (!CONFIG.getPropertyValue(CreateTunnelToServerBeansProperty.class)) {
      return;
    }

    for (final IClassInfo ci : ClassInventory.get().getKnownAnnotatedTypes(TunnelToServer.class)) {
      if (ci.isInterface() && ci.isPublic()) {
        beanManager.registerBean(createRemoteProxyBeanMetaData(ci.resolveClass()));
      }
      else {
        LOG.error("The annotation @{} can only be used on public interfaces, not on {}", TunnelToServer.class.getSimpleName(), ci.name());
      }
    }
  }

  protected BeanMetaData createRemoteProxyBeanMetaData(final Class<?> serviceInterface) {
    return new BeanMetaData(serviceInterface).withProducer(BEANS.get(RemoteProxyProducer.class));
  }

  @ApplicationScoped
  public static class RemoteProxyProducer implements IBeanInstanceProducer<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteProxyProducer.class);

    @Override
    public Object produce(final IBean<Object> bean) {
      final Class<? extends Object> serviceInterfaceClazz = bean.getBeanClazz();
      return new BeanProxyImplementor<Object>(bean, new IBeanInterceptor<Object>() {

        @Override
        public Object invoke(final IBeanInvocationContext<Object> context) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Remote service call to {}.{} ({})", serviceInterfaceClazz.getName(), context.getTargetMethod(), VerboseUtility.dumpObjects(context.getTargetArgs()));
          }
          return BEANS.get(IServiceTunnel.class).invokeService(serviceInterfaceClazz, context.getTargetMethod(), context.getTargetArgs());
        }

      }, null, serviceInterfaceClazz).getProxy();
    }
  }
}
