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
package org.eclipse.scout.rt.server.services.common.notification;

import java.util.Locale;

import org.eclipse.scout.rt.server.context.ServerRunContext;

/**
 * The container of all transactional notifications during a server request. Is kept on the {@link ServerRunContext}.
 */
public class NotificationContainer {
  /**
   * The {@link Locale} which is currently associated with the current thread.
   */
  public static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private NotificationContainer() {
  }

  /**
   */
  public static String get() {
    return CURRENT.get();
  }

}
