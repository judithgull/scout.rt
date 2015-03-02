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
package org.eclipse.scout.commons.job;

import java.util.concurrent.Future;

/**
 * Visitor for visiting {@link Futures}.
 *
 * @since 5.1
 */
public interface IFutureVisitor {

  /**
   * Is called upon visiting a {@link Future}.
   *
   * @return <code>true</code>=continue visiting, <code>false</code>=end visiting.
   */
  boolean visit(Future<?> future);
}