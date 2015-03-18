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
package org.eclipse.scout.rt.platform.job.filter;

import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.rt.platform.job.IBlockingCondition;
import org.eclipse.scout.rt.platform.job.IFuture;

/**
 * Filter which accepts Futures which are waiting for a blocking condition to fall.
 *
 * @see IBlockingCondition
 * @since 5.1
 */
public class BlockedFutureFilter implements IFilter<IFuture<?>> {

  public static final IFilter<IFuture<?>> INSTANCE = new BlockedFutureFilter();

  private BlockedFutureFilter() {
  }

  @Override
  public boolean accept(final IFuture<?> future) {
    return future.isBlocked();
  }
}