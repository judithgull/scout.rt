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
package org.eclipse.scout.testing.client.form;

import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;

/**
 * Dynamic group box to build an ad-hoc application for testing
 */
public class DynamicGroupBox extends AbstractGroupBox {
  private IFormField[] m_injectedFields;

  public DynamicGroupBox(IFormField... fields) {
    super(false);
    m_injectedFields = fields;
    callInitializer();
  }

  /**
   * This is the place to inject fields dynamically
   */
  @Override
  protected void injectFieldsInternal(OrderedCollection<IFormField> fields) {
    if (m_injectedFields != null) {
      for (IFormField f : m_injectedFields) {
        fields.addLast(f);
      }
    }
  }
}
