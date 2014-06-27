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
package org.eclipse.scout.rt.client.ui.form.fields.radiobuttongroup;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractRadioButton;
import org.eclipse.scout.rt.client.ui.form.fields.button.IRadioButton;
import org.eclipse.scout.rt.client.ui.form.fields.labelfield.AbstractLabelField;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;
import org.eclipse.scout.rt.shared.services.lookup.LocalLookupCall;
import org.eclipse.scout.rt.shared.services.lookup.LookupRow;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link AbstractRadioButtonGroup}
 * 
 * @since 4.0.0-M7
 */
@RunWith(ScoutClientTestRunner.class)
public class AbstractRadioButtonGroupTest {

  private P_StandardRadioButtonGroup m_group;

  @Before
  public void setUp() {
    m_group = new P_StandardRadioButtonGroup();
    m_group.initConfig();
  }

  @Test
  public void testInitialized() {
    Assert.assertEquals("RadioButtonGroup", m_group.getLabel());
    Assert.assertEquals(4, m_group.getFieldCount());
    Assert.assertEquals(3, m_group.getButtons().size());
    Assert.assertNull(m_group.getSelectedButton());
    Assert.assertNull(m_group.getSelectedKey());

    Assert.assertEquals(m_group.getRadioButton1(), m_group.getButtonFor(1L));
    Assert.assertEquals(m_group.getRadioButton2(), m_group.getButtonFor(2L));
    Assert.assertEquals(m_group.getRadioButton3(), m_group.getButtonFor(3L));
    Assert.assertNull(m_group.getButtonFor(4L));

    Assert.assertNull(m_group.getErrorStatus());

    Assert.assertEquals(Long.valueOf(1L), m_group.getRadioButton1().getRadioValue());
    Assert.assertEquals(Long.valueOf(2L), m_group.getRadioButton2().getRadioValue());
    Assert.assertEquals(Long.valueOf(3L), m_group.getRadioButton3().getRadioValue());
  }

  @Test
  public void testSelectionViaButton() {
    m_group.selectButton(m_group.getRadioButton2());
    Assert.assertEquals(m_group.getSelectedButton(), m_group.getRadioButton2());

    m_group.selectButton(m_group.getRadioButton3());
    Assert.assertEquals(m_group.getSelectedButton(), m_group.getRadioButton3());
  }

  @Test
  public void testInvalidSelectionViaButton() {
    m_group.selectButton(new AbstractRadioButton<Long>() {
    });
    Assert.assertNull(m_group.getSelectedButton());
  }

  @Test
  public void testSelectionViaKey() {
    m_group.selectKey(Long.valueOf(1L));
    Assert.assertEquals(Long.valueOf(1L), m_group.getSelectedKey());
    m_group.selectKey(Long.valueOf(3L));
    Assert.assertEquals(Long.valueOf(3L), m_group.getSelectedKey());
    m_group.selectKey(Long.valueOf(2L));
    Assert.assertEquals(Long.valueOf(2L), m_group.getSelectedKey());
  }

  @Test
  public void testInvalidSelectionViaKey() {
    try {
      m_group.selectKey(Long.valueOf(4L));
    }
    catch (Exception e) {
      Assert.assertNotNull(m_group.getErrorStatus());

      //select valid value again
      m_group.selectKey(1L);
      Assert.assertNull(m_group.getErrorStatus());
      return;
    }
    Assert.fail("ProcessingException was not thrown!");
  }

  @Test
  public void testEnabledDisabled() {
    assertAllButtonsEnabled(true, m_group);

    m_group.getRadioButton2().setEnabled(false);
    Assert.assertTrue(!m_group.getRadioButton2().isEnabled());

    m_group.setEnabled(false);
    assertAllButtonsEnabled(false, m_group);

    m_group.setEnabled(true);
    assertAllButtonsEnabled(true, m_group);
  }

  @Test
  public void testGetFieldById() {
    Assert.assertEquals(m_group.getRadioButton1(), m_group.getFieldById(m_group.getRadioButton1().getClass().getSimpleName()));
    Assert.assertNull(m_group.getFieldById("nonExisting"));
  }

  @Test
  public void testGetFieldIndex() {
    Assert.assertNotNull(m_group.getFieldIndex(m_group.getRadioButton2()));
    Assert.assertEquals(-1, m_group.getFieldIndex(new AbstractRadioButton<Integer>() {
    }));
  }

  @Test
  public void testGetButtons() {
    Assert.assertEquals(3, m_group.getButtons().size());

    AbstractRadioButtonGroup<Integer> emptyGroup = new AbstractRadioButtonGroup<Integer>() {
    };
    Assert.assertEquals(0, emptyGroup.getButtons().size());
  }

  @Test
  public void testLookupCall() {
    AbstractRadioButtonGroup<Long> lookupGroup = new P_RadioButtonGroupWithLookupCall();
    lookupGroup.initConfig();
    Assert.assertEquals(new P_CompanyLookupCall(), lookupGroup.getLookupCall());
    Assert.assertEquals(3, lookupGroup.getButtons().size());
    Assert.assertNull(lookupGroup.getSelectedButton());
  }

  private void assertAllButtonsEnabled(boolean enabled, IRadioButtonGroup<?> group) {
    for (IRadioButton<?> btn : m_group.getButtons()) {
      if (enabled) {
        Assert.assertTrue(btn.isEnabled());
      }
      else {
        Assert.assertTrue(!btn.isEnabled());
      }
    }
  }

  private class P_StandardRadioButtonGroup extends AbstractRadioButtonGroup<Long> {

    public RadioButton1 getRadioButton1() {
      return getFieldByClass(RadioButton1.class);
    }

    public RadioButton2 getRadioButton2() {
      return getFieldByClass(RadioButton2.class);
    }

    public RadioButton3 getRadioButton3() {
      return getFieldByClass(RadioButton3.class);
    }

    @Override
    protected String getConfiguredLabel() {
      return "RadioButtonGroup";
    }

    @Order(10.0)
    public class RadioButton1 extends AbstractRadioButton<Long> {
      @Override
      public Long getRadioValue() {
        return Long.valueOf(1L);
      }
    }

    @Order(15.0)
    public class LabelField extends AbstractLabelField {
      @Override
      protected String getConfiguredLabel() {
        return "Label";
      }
    }

    @Order(20.0)
    public class RadioButton2 extends AbstractRadioButton<Long> {
      @Override
      public Long getRadioValue() {
        return Long.valueOf(2L);
      }
    }

    @Order(30.0)
    public class RadioButton3 extends AbstractRadioButton<Long> {
      @Override
      public Long getRadioValue() {
        return Long.valueOf(3L);
      }
    }

  }

  private class P_RadioButtonGroupWithLookupCall extends AbstractRadioButtonGroup<Long> {
    @Override
    protected String getConfiguredLabel() {
      return "CodeTypeRadioButtonGroup";
    }

    @Override
    protected Class<? extends ILookupCall<Long>> getConfiguredLookupCall() {
      return P_CompanyLookupCall.class;
    }
  }

  public static class P_CompanyLookupCall extends LocalLookupCall<Long> {

    private static final long serialVersionUID = 1L;

    @Override
    protected List<ILookupRow<Long>> execCreateLookupRows() throws ProcessingException {
      ArrayList<ILookupRow<Long>> rows = new ArrayList<ILookupRow<Long>>();
      rows.add(new LookupRow<Long>(1L, "Business Systems Integration AG"));
      rows.add(new LookupRow<Long>(2L, "Eclipse"));
      rows.add(new LookupRow<Long>(3L, "Google"));
      rows.add(new LookupRow<Long>(null, "null value"));
      return rows;
    }
  }
}
