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
package org.eclipse.scout.commons.filter;

/**
 * Factory and utility methods for Filters.
 *
 * @since 5.1
 */
public final class Filters {

  private Filters() {
  }

  /**
   * Returns an {@link AlwaysFilter} if the given filter is <code>null</code>.
   */
  public static <ELEMENT> IFilter<ELEMENT> alwaysFilterIfNull(final IFilter<ELEMENT> filter) {
    return (filter != null ? filter : new AlwaysFilter<ELEMENT>());
  }
}