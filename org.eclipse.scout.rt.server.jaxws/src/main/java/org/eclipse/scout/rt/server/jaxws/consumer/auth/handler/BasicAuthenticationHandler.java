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
package org.eclipse.scout.rt.server.jaxws.consumer.auth.handler;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.eclipse.scout.commons.Base64Utility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.server.jaxws.MessageContexts;
import org.eclipse.scout.rt.server.jaxws.consumer.InvocationContext;
import org.eclipse.scout.rt.server.jaxws.implementor.JaxWsImplementorSpecifics;

/**
 * Handler to include user's credentials in webservice requests by using Basic Access Authentication. This requires
 * requests to provide a valid user name and password to access content. User's credentials are transported in HTTP
 * headers. Basic authentication also works across firewalls and proxy servers.
 * <p>
 * However, the disadvantage of Basic authentication is that it transmits unencrypted base64-encoded passwords across
 * the network. Therefore, you only should use this authentication when you know that the connection between the client
 * and the server is secure. The connection should be established either over a dedicated line or by using Secure
 * Sockets Layer (SSL) encryption and Transport Layer Security (TLS).
 */
public class BasicAuthenticationHandler implements SOAPHandler<SOAPMessageContext> {

  protected static final String AUTH_BASIC_PREFIX = "Basic ";
  protected static final String AUTH_BASIC_AUTHORIZATION = "authorization";
  protected static final String BASIC_ENCODING = "ISO-8859-1";

  private final JaxWsImplementorSpecifics m_implementorSpecifics;

  public BasicAuthenticationHandler() {
    m_implementorSpecifics = BEANS.get(JaxWsImplementorSpecifics.class);
  }

  @Override
  public final boolean handleMessage(final SOAPMessageContext context) {
    if (MessageContexts.isInboundMessage(context)) {
      return true;
    }

    final String username = StringUtility.valueOf(context.get(InvocationContext.PROP_USERNAME));
    final String password = StringUtility.valueOf(context.get(InvocationContext.PROP_PASSWORD));
    try {
      final String credentials = Base64Utility.encode(StringUtility.join(":", username, password).getBytes(BASIC_ENCODING));
      m_implementorSpecifics.setHttpRequestHeader(context, AUTH_BASIC_AUTHORIZATION, AUTH_BASIC_PREFIX + credentials);
      return true;
    }
    catch (final UnsupportedEncodingException e) {
      throw new WebServiceException("Failed to encode Basic authentication header", e);
    }
  }

  @Override
  public Set<QName> getHeaders() {
    return Collections.emptySet();
  }

  @Override
  public void close(final MessageContext context) {
    // NOOP
  }

  @Override
  public boolean handleFault(final SOAPMessageContext context) {
    return true;
  }
}