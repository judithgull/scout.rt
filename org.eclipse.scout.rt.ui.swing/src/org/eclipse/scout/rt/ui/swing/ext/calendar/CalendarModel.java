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
package org.eclipse.scout.rt.ui.swing.ext.calendar;

import java.awt.Color;
import java.util.Collection;
import java.util.Date;

public interface CalendarModel {
  Collection<Object> getItemsAt(Date dateTruncatedToDay);

  String getTooltip(Object item, Date representedDate);

  String getLabel(Object item, Date representedDate);

  Date getFromDate(Object item);

  Date getToDate(Object item);

  Color getColor(Object item);

  boolean isFullDay(Object item);

  boolean isDraggable(Object item);

  void moveItem(Object item, Date newDate);
}