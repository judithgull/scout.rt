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
package org.eclipse.scout.rt.platform.job.listener;

import java.util.EventListener;

/**
 * Listener to be notified about job lifecycle events.
 *
 * @since 5.1
 * @see JobEvent
 */
public interface IJobListener extends EventListener {

  /**
   * Method invoked to notify about a job lifecycle event and only, if accepted by the filter provided when registered
   * this listener.
   *
   * @param event
   *          describes the event occurred.
   */
  void changed(JobEvent event);
}