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
package org.eclipse.scout.rt.shared.services.common.calendar;

import java.util.Date;
import java.util.Set;

import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.shared.services.common.file.RemoteFile;

@Priority(-3)
public interface IHolidayCalendarService extends ICalendarService {

  /**
   * default value: calendar/holidays.xml
   */
  Set<? extends ICalendarItem> getItems(RemoteFile f, Date minDate, Date maxDate) throws ProcessingException;

  /*
   * Not needed void storeItems(ICalendarItem[] items, boolean delta) throws
   * ProcessingException;
   */
}
