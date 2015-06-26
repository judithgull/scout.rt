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
package org.eclipse.scout.rt.client.ui.basic.table.customizer;

import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;

/**
 * Perform table customization, such as adding custom columns
 */
public interface ITableCustomizer {

  /**
   * Append custom columns
   *
   * @param columns
   *          live and mutable collection of configured columns, not yet initialized
   */
  void injectCustomColumns(OrderedCollection<IColumn<?>> columns);

  /**
   * Add a new custom column to the table by for example showing a form with potential candidates
   */
  void addColumn() throws ProcessingException;

  /**
   * Modify an existing custom column
   */
  void modifyColumn(ICustomColumn<?> col) throws ProcessingException;

  /**
   * Remove an existing custom column
   */
  void removeColumn(ICustomColumn<?> col) throws ProcessingException;

  /**
   * Remove all existing custom columns
   */
  void removeAllColumns() throws ProcessingException;

  /**
   * Get the serialized data of the TableCustomizer for further processing (e.g. storing a bookmark)
   */
  byte[] getSerializedData() throws ProcessingException;

  /**
   * Import the serialized data, e.g. after restoring from a bookmark
   */
  void setSerializedData(byte[] data) throws ProcessingException;

}
