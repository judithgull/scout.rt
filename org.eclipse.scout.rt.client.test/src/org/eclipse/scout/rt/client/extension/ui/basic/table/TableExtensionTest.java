/*******************************************************************************
 * Copyright (c) 2014 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.extension.ui.basic.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.eclipse.scout.extension.AbstractLocalExtensionTestCase;
import org.eclipse.scout.rt.client.extension.ui.basic.table.fixture.AbstractPersonTable;
import org.eclipse.scout.rt.client.extension.ui.basic.table.fixture.AllPersonTable;
import org.eclipse.scout.rt.client.extension.ui.basic.table.fixture.FirstNameColumn;
import org.eclipse.scout.rt.client.extension.ui.basic.table.fixture.OtherPersonTable;
import org.eclipse.scout.rt.client.extension.ui.basic.table.fixture.PersonTableExtension;
import org.eclipse.scout.rt.shared.extension.IExtensionRegistry;
import org.eclipse.scout.service.SERVICES;
import org.junit.Test;

public class TableExtensionTest extends AbstractLocalExtensionTestCase {

  @Test
  public void testExtendAbstractPersonTableAddFirstNameColumnExplicit() {
    SERVICES.getService(IExtensionRegistry.class).register(FirstNameColumn.class, AbstractPersonTable.class);
    doTestAddFirstnameField();
  }

  @Test
  public void testExtendAbstractPersonTableAddFirstNameColumnAnnotation() {
    SERVICES.getService(IExtensionRegistry.class).register(FirstNameColumn.class);
    doTestAddFirstnameField();
  }

  private void doTestAddFirstnameField() {
    AllPersonTable allPersonTable = new AllPersonTable();
    assertEquals(4, allPersonTable.getColumnCount());
    assertSame(allPersonTable.getNameColumn(), allPersonTable.getColumnSet().getColumn(0));
    assertSame(allPersonTable.getColumnSet().getColumnByClass(FirstNameColumn.class), allPersonTable.getColumnSet().getColumn(1));
    assertSame(allPersonTable.getCityColumn(), allPersonTable.getColumnSet().getColumn(2));
    assertSame(allPersonTable.getAgeColumn(), allPersonTable.getColumnSet().getColumn(3));

    OtherPersonTable otherPersonTable = new OtherPersonTable();
    assertEquals(4, otherPersonTable.getColumnCount());
    assertSame(otherPersonTable.getNameColumn(), otherPersonTable.getColumnSet().getColumn(0));
    assertSame(otherPersonTable.getColumnSet().getColumnByClass(FirstNameColumn.class), otherPersonTable.getColumnSet().getColumn(1));
    assertSame(otherPersonTable.getAgeColumn(), otherPersonTable.getColumnSet().getColumn(2));
    assertSame(otherPersonTable.getPhoneNumberColumn(), otherPersonTable.getColumnSet().getColumn(3));
  }

  @Test
  public void testExtendAbstractPersonTablePersonTableExtensionExplicit() {
    SERVICES.getService(IExtensionRegistry.class).register(PersonTableExtension.class, AbstractPersonTable.class);
    doTestPersonTableExtension();
  }

  @Test
  public void testExtendAbstractPersonTablePersonTableExtensionAnnotation() {
    SERVICES.getService(IExtensionRegistry.class).register(PersonTableExtension.class);
    doTestPersonTableExtension();
  }

  private void doTestPersonTableExtension() {
    AllPersonTable allPersonTable = new AllPersonTable();
    assertEquals(4, allPersonTable.getColumnCount());
    assertSame(allPersonTable.getNameColumn(), allPersonTable.getColumnSet().getColumn(0));
    assertSame(allPersonTable.getCityColumn(), allPersonTable.getColumnSet().getColumn(1));
    assertSame(allPersonTable.getAgeColumn(), allPersonTable.getColumnSet().getColumn(2));

    PersonTableExtension extension = allPersonTable.getExtension(PersonTableExtension.class);
    assertNotNull(extension);
    assertSame(extension.getStreetColumn(), allPersonTable.getColumnSet().getColumn(3));

    OtherPersonTable otherPersonTable = new OtherPersonTable();
    assertEquals(4, otherPersonTable.getColumnCount());
    assertSame(otherPersonTable.getNameColumn(), otherPersonTable.getColumnSet().getColumn(0));
    assertSame(otherPersonTable.getAgeColumn(), otherPersonTable.getColumnSet().getColumn(1));
    assertSame(otherPersonTable.getPhoneNumberColumn(), otherPersonTable.getColumnSet().getColumn(2));

    extension = otherPersonTable.getExtension(PersonTableExtension.class);
    assertNotNull(extension);
    assertSame(extension.getStreetColumn(), otherPersonTable.getColumnSet().getColumn(3));
  }
}
