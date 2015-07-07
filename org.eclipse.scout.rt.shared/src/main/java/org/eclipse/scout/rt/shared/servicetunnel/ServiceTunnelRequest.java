/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.servicetunnel;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.scout.commons.VerboseUtility;
import org.eclipse.scout.commons.nls.NlsLocale;
import org.eclipse.scout.rt.platform.Bean;
import org.eclipse.scout.rt.shared.services.common.context.IRunMonitorCancelService;
import org.eclipse.scout.rt.shared.ui.UserAgent;

@Bean
public class ServiceTunnelRequest implements Serializable {
  private static final long serialVersionUID = 0L;
  private static final AtomicLong requestSequenceGenerator = new AtomicLong();

  /**
   * @since 3.8
   */
  private final long m_requestSequence = requestSequenceGenerator.incrementAndGet();
  private String m_sessionId;
  private final String m_serviceInterfaceClassName;
  private final String m_operation;
  private final Class[] m_parameterTypes;
  private final Object[] m_args;
  private final Locale m_locale;
  private String m_userAgent;
  private String m_notificationNodeId;

  public ServiceTunnelRequest(String serviceInterfaceName, String op, Class[] parameterTypes, Object[] args) {
    m_serviceInterfaceClassName = serviceInterfaceName;
    m_operation = op;
    m_parameterTypes = parameterTypes;
    if (args == null) {
      args = new Object[0];
    }
    m_args = args;
    m_locale = NlsLocale.get();
  }

  /**
   * @return the request sequence for this session
   *         <p>
   *         The sequence can be used to find and manipulate transactions of the same session. Such a scenario is used
   *         when cancelling "old" lookup requests using {@link IRunMonitorCancelService#cancel(long)}
   */
  public long getRequestSequence() {
    return m_requestSequence;
  }

  public String getSessionId() {
    return m_sessionId;
  }

  public void setSessionId(String sessionId) {
    m_sessionId = sessionId;
  }

  /**
   * @return the service class name of the service to call.
   */
  public String getServiceInterfaceClassName() {
    return m_serviceInterfaceClassName;
  }

  public String getOperation() {
    return m_operation;
  }

  public Class[] getParameterTypes() {
    return m_parameterTypes;
  }

  public Object[] getArgs() {
    return m_args;
  }

  public Locale getLocale() {
    return m_locale;
  }

  /**
   * Represents the user interface on client side.<br/>
   * To parse an identifier use {@link UserAgent#createByIdentifier(String)}
   */
  public String getUserAgent() {
    return m_userAgent;
  }

  /**
   * Represents the user interface on client side.<br/>
   * To create an identifier use {@link UserAgent#createIdentifier()}.
   */
  public void setUserAgent(String userAgent) {
    m_userAgent = userAgent;
  }

  public String getClientNotificationNodeId() {
    return m_notificationNodeId;
  }

  public void setClientNotificationNodeId(String notificationNodeId) {
    m_notificationNodeId = notificationNodeId;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("Remote call [");
    buf.append("requestSequence='").append(m_requestSequence).append("', ");
    buf.append("sessionId='").append(m_sessionId).append("'\n");
    buf.append(m_serviceInterfaceClassName).append(".").append(m_operation);
    if (m_args != null && m_args.length > 0) {
      for (int i = 0; i < m_args.length; i++) {
        buf.append("\t\n");
        buf.append("arg[" + i + "]=" + VerboseUtility.dumpObject(m_args[i]));
      }
    }
    return buf.toString();
  }
}
