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
package org.eclipse.scout.rt.spec.client.out;

/**
 * A table containing headers and cells with descriptions.
 */
public interface IDocTable {

  /**
   * @return cell texts
   */
  String[][] getCellTexts();

  /**
   * @return header texts
   */
  String[] getHeaderTexts();

  /**
   * @return whether headers and entries are filled into table as columns instead of rows
   */
  boolean isTransposedLayout();

}
