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
package org.eclipse.scout.rt.server.jaxws;

import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;

import org.eclipse.scout.rt.platform.config.AbstractBooleanConfigProperty;
import org.eclipse.scout.rt.platform.config.AbstractPositiveIntegerConfigProperty;
import org.eclipse.scout.rt.platform.config.AbstractPositiveLongConfigProperty;
import org.eclipse.scout.rt.platform.config.AbstractStringConfigProperty;
import org.eclipse.scout.rt.platform.config.AbstractSubjectConfigProperty;
import org.eclipse.scout.rt.server.jaxws.handler.LogHandler;
import org.eclipse.scout.rt.server.jaxws.implementor.JaxWsImplementorSpecifics;
import org.eclipse.scout.rt.server.jaxws.provider.auth.authenticator.ConfigFileAuthenticator;
import org.eclipse.scout.rt.server.jaxws.provider.auth.authenticator.IAuthenticator;
import org.eclipse.scout.rt.server.jaxws.provider.auth.method.BasicAuthenticationMethod;
import org.eclipse.scout.rt.server.jaxws.provider.handler.HandlerProxy;

/**
 * 'config.properties' used in JAX-WS RT.
 */
public final class JaxWsConfigProperties {

  private JaxWsConfigProperties() {
  }

  /**
   * Technical {@link Subject} used to authenticate webservice requests; used by {@link IAuthenticator}.
   */
  public static class JaxWsAuthenticatorSubjectProperty extends AbstractSubjectConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.provider.user.authenticator";
    }

    @Override
    protected Subject getDefaultValue() {
      return convertToSubject("jaxws-authenticator");
    }
  }

  /**
   * Technical {@link Subject} used to invoke JAX-WS handlers if the request is not authenticated yet; used by
   * {@link HandlerProxy}.
   */
  public static class JaxWsHandlerSubjectProperty extends AbstractSubjectConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.provider.user.handler";
    }

    @Override
    protected Subject getDefaultValue() {
      return convertToSubject("jaxws-handler");
    }
  }

  /**
   * Indicates whether to log SOAP messages in debug or info level; used by {@link LogHandler}.
   */
  public static class JaxWsLogHandlerDebugProperty extends AbstractBooleanConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.loghandler.debug";
    }

    @Override
    protected Boolean getDefaultValue() {
      return false;
    }
  }

  /**
   * Qualified name of the {@link JaxWsImplementorSpecifics} to use.
   */
  public static class JaxWsImplementorProperty extends AbstractStringConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.implementor";
    }


  }

  /**
   * Users granted to access webservices; used by {@link ConfigFileAuthenticator}.
   */
  public static class JaxWsAuthCredentialsProperty extends AbstractStringConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.provider.authentication.credentials";
    }


  }

  /**
   * Security Realm used for Basic Authentication; used by {@link BasicAuthenticationMethod}.
   */
  public static class JaxWsBasicAuthRealmProperty extends AbstractStringConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.provider.authentication.basic.realm";
    }

    @Override
    protected String getDefaultValue() {
      return "JAX-WS";
    }
  }

  /**
   * To indicate whether to use a preemptive port cache for webservice clients.
   * <p>
   * Depending on the implementor used, cached ports may increase performance, because port creation is an expensive
   * operation due to WSDL and schema validation. The cache is based on a 'corePoolSize', meaning that that number of
   * ports is created on a preemptive basis. If more ports than that number is required, they are are created on demand
   * and also added to the cache until expired, which is useful at a high load.
   *
   * @see JaxWsPortCacheCorePoolSizeProperty
   * @see JaxWsPortCacheTTLProperty
   */
  public static class JaxWsPortCacheEnabledProperty extends AbstractBooleanConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.consumer.portCache.enabled";
    }

    @Override
    protected Boolean getDefaultValue() {
      return Boolean.TRUE;
    }
  }

  /**
   * Number of ports to be preemptively cached to speed up webservice calls.
   *
   * @see JaxWsPortCacheEnabledProperty
   */
  public static class JaxWsPortCacheCorePoolSizeProperty extends AbstractPositiveIntegerConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.consumer.portCache.corePoolSize";
    }

    @Override
    protected Integer getDefaultValue() {
      return 10;
    }
  }

  /**
   * Maximum time in seconds to retain ports in the cache if the 'corePoolSize' is exceeded. That typically occurs at
   * high load, or if 'corePoolSize' is undersized.
   *
   * @see JaxWsPortCacheEnabledProperty
   */
  public static class JaxWsPortCacheTTLProperty extends AbstractPositiveLongConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.consumer.portCache.ttl";
    }

    @Override
    protected Long getDefaultValue() {
      return TimeUnit.MINUTES.toSeconds(15);
    }
  }

  /**
   * Connect timeout in seconds to abort a webservice request, if establishment of the HTTP connection takes longer than
   * this timeout. A timeout of <code>null</code> means an infinite timeout.
   */
  public static class JaxWsConnectTimeoutProperty extends AbstractPositiveIntegerConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.consumer.connectTimeout";
    }

    @Override
    protected Integer getDefaultValue() {
      return null; // infinite timeout
    }
  }

  /**
   * Read timeout in seconds to abort a webservice request, if it takes longer than this timeout for data to be
   * available for read. A timeout of <code>null</code> means an infinite timeout.
   */
  public static class JaxWsReadTimeoutProperty extends AbstractPositiveIntegerConfigProperty {

    @Override
    public String getKey() {
      return "jaxws.consumer.readTimeout";
    }

    @Override
    protected Integer getDefaultValue() {
      return null; // infinite timeout
    }
  }
}
