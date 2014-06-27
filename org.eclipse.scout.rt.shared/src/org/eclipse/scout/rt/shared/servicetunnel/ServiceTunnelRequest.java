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
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.security.auth.Subject;

import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.VerboseUtility;
import org.eclipse.scout.rt.shared.services.common.processing.IServerProcessingCancelService;
import org.eclipse.scout.rt.shared.ui.UserAgent;

public class ServiceTunnelRequest implements Serializable {
  private static final long serialVersionUID = 0L;
  private static final AtomicLong requestSequenceGenerator = new AtomicLong();

  private String m_serviceInterfaceClassName;
  private String m_operation;
  private Class[] m_parameterTypes;
  private Object[] m_args;
  private Locale m_locale;
  private String m_userAgent;
  private String m_version;
  private Object m_metaData;
  private Set<String> m_consumedNotificationIds;
  /**
   * @since 3.8
   */
  private final long m_requestSequence = requestSequenceGenerator.incrementAndGet();
  /**
   * @since 3.8
   */
  private String m_virtualSessionId;
  /**
   * @since 3.8
   */
  private transient Subject m_clientSubject;

  // for serialization
  private ServiceTunnelRequest() {
  }

  public ServiceTunnelRequest(String version, Class serviceInterfaceClass, Method operation, Object[] args) {
    this(version, serviceInterfaceClass.getName(), operation.getName(), operation.getParameterTypes(), args);
  }

  public ServiceTunnelRequest(String version, String serviceInterfaceName, String op, Class[] parameterTypes, Object[] args) {
    m_version = version;
    m_serviceInterfaceClassName = serviceInterfaceName;
    m_operation = op;
    m_parameterTypes = parameterTypes;
    m_args = args;
    if (m_args == null) {
      m_args = new Object[0];
    }
    m_locale = LocaleThreadLocal.get();
  }

  /**
   * @return the request sequence for this session
   *         <p>
   *         The sequence can be used to find and manipulate transactions of the same session. Such a scenario is used
   *         when cancelling "old" lookup requests using {@link IServerProcessingCancelService#cancel(long)}
   */
  public long getRequestSequence() {
    return m_requestSequence;
  }

  public String getServiceInterfaceClassName() {
    return m_serviceInterfaceClassName;
  }

  public String getVersion() {
    return m_version;
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

  public Object getMetaData() {
    return m_metaData;
  }

  public void setMetaData(Object o) {
    m_metaData = o;
  }

  /**
   * The subject under which the request is done
   * <p>
   * Client only method. The member is transient and will be null on the server.
   */
  public Subject getClientSubject() {
    return m_clientSubject;
  }

  /**
   * The subject under which the request is done
   * <p>
   * Client only method. The member is transient and will be null on the server.
   */
  public void setClientSubject(Subject requestSubject) {
    m_clientSubject = requestSubject;
  }

  /**
   * The web (ajax) session under which the request is done
   */
  public String getVirtualSessionId() {
    return m_virtualSessionId;
  }

  /**
   * The web (ajax) session under which the request is done
   */
  public void setVirtualSessionId(String virtualSessionId) {
    m_virtualSessionId = virtualSessionId;
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

  public void setConsumedNotifications(Set<String> notificationsIds) {
    m_consumedNotificationIds = notificationsIds;
  }

  public Set<String> getConsumedNotifications() {
    return m_consumedNotificationIds;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Service call " + m_serviceInterfaceClassName + "." + m_operation);
    if (m_args != null && m_args.length > 0) {
      for (int i = 0; i < m_args.length; i++) {
        buf.append("\n");
        buf.append("arg[" + i + "]=" + VerboseUtility.dumpObject(m_args[i]));
      }
    }
    return buf.toString();
  }

  /**
   * @return a single string with all package parts reduced to their first character,
   *         except the last package fragment. All is concatenated together with _ instead of '.' and the method name is
   *         appended with '__'
   *         <p>
   *         Example for IPingService is "oesrssc_ping_IPingService__ping"
   */
  public static String toSoapOperation(String className, String methodName) {
    if (className == null || methodName == null) {
      return null;
    }
    int i = className.lastIndexOf('.');
    if (i < 0) {
      return className + "__" + methodName;
    }
    String simpleName = className.substring(i + 1);
    String packageName = className.substring(0, i);
    i = packageName.lastIndexOf('.');
    if (i < 0) {
      return packageName + "_" + simpleName + "__" + methodName;
    }
    StringBuilder buf = new StringBuilder();
    for (String s : packageName.substring(0, i).split("[.]")) {
      buf.append(s.charAt(0));
    }
    buf.append("_");
    buf.append(packageName.substring(i + 1));
    buf.append("_");
    buf.append(simpleName);
    buf.append("__");
    buf.append(methodName);
    return buf.toString();
  }
}
