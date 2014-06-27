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
package org.eclipse.scout.rt.client.ui.form.fields.tablefield;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractBigDecimalColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractDoubleColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractIntegerColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractLongColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractStringColumn;
import org.junit.Before;
import org.junit.Test;

public class AbstractTableFieldTest extends AbstractTableField<AbstractTableFieldTest.Table> {
  private static final String[] LOREM_IPSUM = new String[]{
      "Lorem ipsum dolor sit amet,",
      "consetetur sadipscing elitr,",
      "sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat,",
      "sed diam voluptua.",
      "At vero eos et accusam et justo duo dolores et ea rebum.",
      "Stet clita kasd gubergren,",
      "no sea takimata sanctus est Lorem ipsum dolor sit amet.",
      "Lorem ipsum dolor sit amet,",
      "consetetur sadipscing elitr,",
      "sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat,",
      "sed diam voluptua.",
      "At vero eos et accusam et justo duo dolores et ea rebum.",
      "Stet clita kasd gubergren,",
      "no sea takimata sanctus est Lorem ipsum dolor sit amet."
  };

  private static final BigDecimal FAR_BELOW_ZERO = new BigDecimal("-999999999999999999999999999999999999999999999999999999999999");

  @Before
  public void setUp() throws ProcessingException {
    for (int i = 0; i < 10; i++) {
      ITableRow row = getTable().createRow();
      getTable().getIntegerColumn().setValue(row, i);
      getTable().getLongColumn().setValue(row, Long.valueOf(i * 2));
      getTable().getString1Column().setValue(row, i + ". row");
      getTable().getString2Column().setValue(row, getTextFor(i, " "));
      getTable().getString3Column().setValue(row, getTextFor(i, " "));
      getTable().getString4Column().setValue(row, getTextFor(i, "\n"));
      getTable().getBigDecimalColumn().setValue(row, FAR_BELOW_ZERO.add(BigDecimal.valueOf(1.11).multiply(BigDecimal.valueOf(i))));
      getTable().getDoubleColumn().setValue(row, BigDecimal.valueOf(1.11).multiply(BigDecimal.valueOf(i)).doubleValue());
      getTable().addRow(row);

      getTable().selectAllRows();
    }
  }

  @Test
  public void testSumNumberColumsInCreateDefaultTableStatus() {
    String tableStatus = createDefaultTableStatus();
    assertTrue("TableStatus does not contain sum of Integer-column as expected. (tableStatus [" + tableStatus + "])", tableStatus.contains("Integer: " + getTable().getIntegerColumn().getFormat().format(45)));
    assertTrue("TableStatus does not contain sum of Long-column as expected. (tableStatus [" + tableStatus + "])", tableStatus.contains("Long: " + getTable().getLongColumn().getFormat().format(90)));
    assertTrue("TableStatus does not contain sum of Double-column as expected. (tableStatus [" + tableStatus + "])", tableStatus.contains("Double: " + getTable().getDoubleColumn().getFormat().format(49.95d)));
    String formattedBigDecimalSum = "BigDecimal: " + getTable().getBigDecimalColumn().getFormat().format(FAR_BELOW_ZERO.multiply(BigDecimal.TEN).add(BigDecimal.valueOf(49.95d)));
    // XXX[aho]
//    assertTrue("TableStatus does not contain sum of BigDecimal-column as expected. (expected [" + formattedBigDecimalSum + "] in tableStatus [" + tableStatus + "])", tableStatus.contains(formattedBigDecimalSum));
  }

  private String getTextFor(int size, String separator) {
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < size; j++) {
      sb.append(LOREM_IPSUM[j % LOREM_IPSUM.length]);
      sb.append(separator);
    }
    return sb.toString();
  }

  /**
   * table
   */
  @Order(10.0)
  public class Table extends AbstractTable {

    @Override
    protected boolean getConfiguredMultilineText() {
      return true;
    }

    public IntegerColumn getIntegerColumn() {
      return getColumnSet().getColumnByClass(IntegerColumn.class);
    }

    public LongColumn getLongColumn() {
      return getColumnSet().getColumnByClass(LongColumn.class);
    }

    public String1Column getString1Column() {
      return getColumnSet().getColumnByClass(String1Column.class);
    }

    public String2Column getString2Column() {
      return getColumnSet().getColumnByClass(String2Column.class);
    }

    public String3Column getString3Column() {
      return getColumnSet().getColumnByClass(String3Column.class);
    }

    public String4Column getString4Column() {
      return getColumnSet().getColumnByClass(String4Column.class);
    }

    public BigDecimalColumn getBigDecimalColumn() {
      return getColumnSet().getColumnByClass(BigDecimalColumn.class);
    }

    public DoubleColumn getDoubleColumn() {
      return getColumnSet().getColumnByClass(DoubleColumn.class);
    }

    @Order(1.0)
    public class IntegerColumn extends AbstractIntegerColumn {

      @Override
      protected boolean getConfiguredEditable() {
        return true;
      }

      @Override
      protected String getConfiguredHeaderText() {
        return "Integer";
      }

      @Override
      protected void execDecorateCell(Cell cell, ITableRow row) throws ProcessingException {
        cell.setEnabled(getIntegerColumn().getValue(row) != 2);
      }

      @Override
      protected int getConfiguredWidth() {
        return 70;
      }

    }

    @Order(5.0)
    public class LongColumn extends AbstractLongColumn {

      @Override
      protected boolean getConfiguredEditable() {
        return true;
      }

      @Override
      protected String getConfiguredHeaderText() {
        return "Long";
      }

      @Override
      protected void execDecorateCell(Cell cell, ITableRow row) throws ProcessingException {
        cell.setEnabled(getIntegerColumn().getValue(row) != 2);
      }

      @Override
      protected int getConfiguredWidth() {
        return 70;
      }

    }

    @Order(10.0)
    public class String1Column extends AbstractStringColumn {

      @Override
      protected String getConfiguredHeaderText() {
        return "String1";
      }

      @Override
      protected boolean getConfiguredEditable() {
        return true;
      }

      @Override
      protected int getConfiguredWidth() {
        return 100;
      }
    }

    @Order(20.0)
    public class String2Column extends AbstractStringColumn {

      @Override
      protected String getConfiguredHeaderText() {
        return "String2 (TextWrap false)";
      }

      @Override
      protected boolean getConfiguredEditable() {
        return true;
      }

      @Override
      protected boolean getConfiguredTextWrap() {
        return false;
      }

      @Override
      protected int getConfiguredWidth() {
        return 200;
      }
    }

    @Order(30.0)
    public class String3Column extends AbstractStringColumn {

      @Override
      protected String getConfiguredHeaderText() {
        return "String3 (TextWrap true)";
      }

      @Override
      protected boolean getConfiguredEditable() {
        return true;
      }

      @Override
      protected boolean getConfiguredTextWrap() {
        return true;
      }

      @Override
      protected int getConfiguredWidth() {
        return 200;
      }
    }

    @Order(30.0)
    public class String4Column extends AbstractStringColumn {

      @Override
      protected String getConfiguredHeaderText() {
        return "String4 (TextWrap false, multiline text)";
      }

      @Override
      protected boolean getConfiguredEditable() {
        return true;
      }

      @Override
      protected boolean getConfiguredTextWrap() {
        return false;
      }

      @Override
      protected int getConfiguredWidth() {
        return 200;
      }
    }

    @Order(35.0)
    public class DoubleColumn extends AbstractDoubleColumn {

      @Override
      protected boolean getConfiguredEditable() {
        return true;
      }

      @Override
      protected String getConfiguredHeaderText() {
        return "Double";
      }

      @Override
      protected void execDecorateCell(Cell cell, ITableRow row) throws ProcessingException {
        cell.setEnabled(getIntegerColumn().getValue(row) != 2);
      }

      @Override
      protected int getConfiguredWidth() {
        return 70;
      }

    }

    @Order(40)
    public class BigDecimalColumn extends AbstractBigDecimalColumn {
      @Override
      protected String getConfiguredHeaderText() {
        return "BigDecimal";
      }

      @Override
      protected boolean getConfiguredEditable() {
        return true;
      }

      @Override
      protected int getConfiguredMaxFractionDigits() {
        return 25;
      }

      @Override
      protected int getConfiguredFractionDigits() {
        return 25;
      }

      @Override
      protected int getConfiguredWidth() {
        return 160;
      }
    }

  }

}
