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
package org.eclipse.scout.rt.shared.services.common.jdbc;

import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.shared.validate.IValidationStrategy;
import org.eclipse.scout.rt.shared.validate.InputValidation;
import org.eclipse.scout.service.IService;

/**
 * @deprecated, do not use anymore. Use {@link ISqlService} on the server. Will be removed in the M-Release.
 */
@Deprecated
@Priority(-3)
@InputValidation(IValidationStrategy.PROCESS.class)
public interface ILegacySqlQueryService extends IService {

  String createPlainText(String s, Object... bindBases) throws ProcessingException;

  Object[][] select(String s, Object... bindBases) throws ProcessingException;

  Object[][] selectLimited(String s, int maxRowCount, Object... bindBases) throws ProcessingException;

  void selectInto(String s, Object... bindBases) throws ProcessingException;

  void selectIntoLimited(String s, int maxRowCount, Object... bindBases) throws ProcessingException;

  @SuppressWarnings("deprecation")
  LegacySearchFilter.WhereToken resolveSpecialConstraint(Object specialConstraint) throws ProcessingException;
}
