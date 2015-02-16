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
package org.eclipse.scout.rt.server.commons.servletfilter;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a {@link Servlet} used in Scout application and provides the ongoing {@link HttpServletRequest} and
 * {@link HttpServletResponse} in the form of {@link ThreadLocal}s.
 */
public interface IServlet extends Servlet {

  /**
   * The {@link HttpServletRequest} which is currently associated with the current thread.
   */
  ThreadLocal<HttpServletRequest> CURRENT_HTTP_SERVLET_REQUEST = new ThreadLocal<>();

  /**
   * The {@link HttpServletResponse} which is currently associated with the current thread.
   */
  ThreadLocal<HttpServletResponse> CURRENT_HTTP_SERVLET_RESPONSE = new ThreadLocal<>();
}