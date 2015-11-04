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
package org.eclipse.scout.rt.server.jaxws.implementor;

import java.io.Closeable;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.handler.MessageContext;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.exception.PlatformException;

/**
 * JAX-WS implementor specifics of 'JAX-WS CXF implementation'.
 *
 * @since 5.1
 */
public class JaxWsCxfSpecifics extends JaxWsImplementorSpecifics {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(JaxWsCxfSpecifics.class);

  @Override
  @PostConstruct
  protected void initConfig() {
    super.initConfig();
    m_properties.put(PROP_HTTP_RESPONSE_CODE, "org.apache.cxf.message.Message.RESPONSE_CODE");
    m_properties.put(PROP_SOCKET_CONNECT_TIMEOUT, "javax.xml.ws.client.connectionTimeout");
    m_properties.put(PROP_SOCKET_READ_TIMEOUT, "javax.xml.ws.client.receiveTimeout");
  }

  @Override
  public String getVersionInfo() {
    try {
      final Package pck = Class.forName("org.apache.cxf.jaxws.JaxWsClientProxy").getPackage();
      return String.format("JAX-WS Apache CXF %s (%s)", pck.getImplementationVersion(), pck.getImplementationVendor());
    }
    catch (final ClassNotFoundException e) {
      throw new PlatformException("Application configured to run with JAX-WS Apache CXF, but implementor could not be found on classpath.");
    }
  }

  @Override
  public void setHttpResponseHeader(final Map<String, Object> ctx, final String key, final String value) {
    final HttpServletResponse httpServletResponse = (HttpServletResponse) ctx.get(MessageContext.SERVLET_RESPONSE);
    httpServletResponse.setHeader(key, value);
  }

  @Override
  public void closeSocket(final Object port, final String operation) {
    try {
      ((Closeable) port).close();
    }
    catch (final Throwable e) {
      LOG.error(String.format("Failed to close Socket for: %s", operation), e);
    }
  }
}