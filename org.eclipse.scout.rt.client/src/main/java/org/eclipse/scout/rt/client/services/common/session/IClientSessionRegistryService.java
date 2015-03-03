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
package org.eclipse.scout.rt.client.services.common.session;

import javax.security.auth.Subject;

import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.eclipse.scout.service.IService;

@Priority(-3)
public interface IClientSessionRegistryService extends IService {

  /**
   * <p>
   * A new session is created and started. The session is started by the caller thread.
   * </p>
   * <p>
   * Note: If the creation of the session requires a special jaas context call it only inside a
   * {@link Subject#doAs(Subject, java.security.PrivilegedAction)} section.
   * </p>
   *
   * @param userAgent
   *          the current {@link UserAgent} which will be set on the client session.
   * @return a new client session of type clazz
   * @see {@link IClientSession#startSession(org.osgi.framework.Bundle)}, {@link IClientSession#getVirtualSessionId()},
   *      {@link IClientSession#getUserAgent()},{@link IClientSession#isActive()}
   */
  <T extends IClientSession> T newClientSession(Class<T> clazz, Subject subject, UserAgent userAgent);

}