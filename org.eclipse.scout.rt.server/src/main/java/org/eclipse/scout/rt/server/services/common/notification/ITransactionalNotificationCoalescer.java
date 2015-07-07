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

import java.io.Serializable;
import java.util.Set;

import org.eclipse.scout.rt.platform.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public interface ITransactionalNotificationCoalescer<NOTIFICATION extends Serializable> {

  Set<NOTIFICATION> coalesce(Set<NOTIFICATION> notifications);

}
