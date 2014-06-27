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
package org.eclipse.scout.rt.client.ui.form.fields.datefield;

import java.util.Date;

import org.eclipse.scout.rt.client.ui.form.fields.IBasicFieldUIFacade;

public interface IDateFieldUIFacade extends IBasicFieldUIFacade {

  boolean setDateTimeTextFromUI(String newText);

  boolean setDateTextFromUI(String newText);

  boolean setTimeTextFromUI(String newText);

  void setDateTimeFromUI(Date d);

  void setDateFromUI(Date d);

  void setTimeFromUI(Date d);

  /**
   * see {@link AbstractDateField#execShiftDate(int, int)}
   */
  void fireDateShiftActionFromUI(int level, int value);

  /**
   * see {@link AbstractDateField#execShiftTime(int, int)}
   */
  void fireTimeShiftActionFromUI(int level, int value);

}
