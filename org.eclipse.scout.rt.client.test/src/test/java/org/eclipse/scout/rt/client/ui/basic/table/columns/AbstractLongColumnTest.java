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
package org.eclipse.scout.rt.client.ui.basic.table.columns;

import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;
import java.util.Locale;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 *
 */
@RunWith(PlatformTestRunner.class)
public class AbstractLongColumnTest extends AbstractLongColumn {

  @Test
  public void testFormattingInDecorateCellInternal() throws ProcessingException {
    ITableRow row = Mockito.mock(ITableRow.class);
    Cell cell = new Cell();
    Long testValue = Long.valueOf(-123456789);
    cell.setValue(testValue);

    for (Locale locale : DecimalFormat.getAvailableLocales()) {
      DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
      df.applyPattern(getFormat().toPattern());
      setFormat(df);

      decorateCellInternal(cell, row);
      assertEquals("cell text not formatted as expected", df.format(testValue), cell.getText());
    }
  }

}
