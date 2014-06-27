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
package org.eclipse.scout.rt.shared.services.lookup;

import java.util.List;

import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.service.IService;

@Priority(-3)
public interface IBatchLookupService extends IService {

  /**
   * Lookup by performing a "key" filter and activating the {@code<key>} tags<br>
   * Batch processing
   */
  List<List<ILookupRow<?>>> getBatchDataByKey(BatchLookupCall call) throws ProcessingException;

  /**
   * Lookup by performing a "text" filter and activating the {@code<text>} tags<br>
   * Batch processing
   */
  List<List<ILookupRow<?>>> getBatchDataByText(BatchLookupCall call) throws ProcessingException;

  /**
   * Lookup by performing a "all" filter and activating the {@code<all>} tags<br>
   * Batch processing
   */
  List<List<ILookupRow<?>>> getBatchDataByAll(BatchLookupCall call) throws ProcessingException;

  /**
   * Lookup by performing a "recursion" filter and activating the {@code<rec>} tags<br>
   * Batch processing
   */
  List<List<ILookupRow<?>>> getBatchDataByRec(BatchLookupCall call) throws ProcessingException;
}
