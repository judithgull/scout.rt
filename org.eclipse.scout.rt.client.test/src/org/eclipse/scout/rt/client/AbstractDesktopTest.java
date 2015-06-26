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
package org.eclipse.scout.rt.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.rt.client.AbstractDesktopTest.CheckSaveTestForm.MainBox.MessageField;
import org.eclipse.scout.rt.client.ui.DataChangeListener;
import org.eclipse.scout.rt.client.ui.desktop.AbstractDesktop;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.client.ui.form.AbstractFormHandler;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractCancelButton;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractOkButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.AbstractStringField;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link AbstractDesktop}
 */
@RunWith(ScoutClientTestRunner.class)
public class AbstractDesktopTest {
  private static final Object TEST_DATA_TYPE_1 = new Object();
  private static final Object TEST_DATA_TYPE_2 = new Object();

  private CheckSaveTestForm m_testForm;

  @Before
  public void setup() throws ProcessingException {
    m_testForm = new CheckSaveTestForm();
  }

  @After
  public void tearDown() throws ProcessingException {
    m_testForm.doClose();
  }

  @Test
  public void testNoSaveNeeded() throws ProcessingException {
    m_testForm.startNew();
    assertFalse(m_testForm.isSaveNeeded());
  }

  @Test
  public void testSaveNeeded() throws ProcessingException {
    m_testForm.startNew();
    m_testForm.getMessageField().setValue("test");
    assertTrue(m_testForm.isSaveNeeded());
  }

  /**
   * {@link AbstractDesktop#doBeforeClosingInternal()}
   */
  @Test
  public void testClosingDoBeforeClosingInternal() throws ProcessingException {
    AbstractDesktop d = new AbstractDesktop() {
    };
    boolean closing = d.doBeforeClosingInternal();
    assertTrue(closing);
  }

  @Test
  public void testUnsavedForms() throws ProcessingException {
    m_testForm.startNew();
    m_testForm.getMessageField().setValue("test");
    IDesktop d = ClientSyncJob.getCurrentSession().getDesktop();
    assertTrue(d.getUnsavedForms().contains(m_testForm));
  }

  @Test
  public void testDataChangedSimple() throws ProcessingException {
    AbstractDesktop d = new AbstractDesktop() {
    };
    final Holder<Object[]> resultHolder = new Holder<Object[]>(Object[].class);
    d.addDataChangeListener(new DataChangeListener() {

      @Override
      public void dataChanged(Object... dataTypes) throws ProcessingException {
        resultHolder.setValue(dataTypes);
      }
    }, TEST_DATA_TYPE_1, TEST_DATA_TYPE_2);

    d.dataChanged(TEST_DATA_TYPE_1, TEST_DATA_TYPE_2);

    verifyDataChanged(resultHolder);
  }

  @Test
  public void testDataChangedChanging() throws ProcessingException {
    AbstractDesktop d = new AbstractDesktop() {
    };
    final Holder<Object[]> resultHolder = new Holder<Object[]>(Object[].class);
    d.addDataChangeListener(new DataChangeListener() {

      @Override
      public void dataChanged(Object... dataTypes) throws ProcessingException {
        resultHolder.setValue(dataTypes);
      }
    }, TEST_DATA_TYPE_1, TEST_DATA_TYPE_2);

    d.setDataChanging(true);
    d.dataChanged(TEST_DATA_TYPE_1);
    d.dataChanged(TEST_DATA_TYPE_1, TEST_DATA_TYPE_1, TEST_DATA_TYPE_1);
    d.dataChanged(TEST_DATA_TYPE_2, TEST_DATA_TYPE_2);
    d.dataChanged(TEST_DATA_TYPE_1, TEST_DATA_TYPE_2);
    d.dataChanged(TEST_DATA_TYPE_1);
    d.dataChanged(TEST_DATA_TYPE_2);
    d.setDataChanging(false);
    verifyDataChanged(resultHolder);
  }

  protected void verifyDataChanged(Holder<Object[]> resultHolder) throws ProcessingException {
    Object[] result = resultHolder.getValue();
    assertTrue(result.length == 2);
    assertTrue(result[0] == TEST_DATA_TYPE_1 && result[1] == TEST_DATA_TYPE_2 ||
        result[0] == TEST_DATA_TYPE_2 && result[1] == TEST_DATA_TYPE_1);
  }

  @ClassId("d090cc19-ba7a-4f79-b147-e58765a837fb")
  class CheckSaveTestForm extends AbstractForm {

    /**
     * @throws ProcessingException
     */
    public CheckSaveTestForm() throws ProcessingException {
      super();
    }

    @Override
    protected String getConfiguredTitle() {
      return TEXTS.get("AskIfSaveNeededForm");
    }

    public void startNew() throws ProcessingException {
      startInternal(new NewHandler());
    }

    public MainBox getMainBox() {
      return getFieldByClass(MainBox.class);
    }

    public MessageField getMessageField() {
      return getFieldByClass(MessageField.class);
    }

    @Order(10.0)
    public class MainBox extends AbstractGroupBox {

      @Order(10.0)
      public class MessageField extends AbstractStringField {
      }

      @Order(20.0)
      public class OkButton extends AbstractOkButton {
      }

      @Order(30.0)
      public class CancelButton extends AbstractCancelButton {
      }
    }

    public class NewHandler extends AbstractFormHandler {
    }

  }

}
