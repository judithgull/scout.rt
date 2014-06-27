/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.form.fixture;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.AbstractStringField;

/**
 * Template GroupBox with a StringField and a nested groupbox using another template
 */
public abstract class AbstractTemplateUsingOtherTemplateGroupBox extends AbstractGroupBox {

  public TextField getTextField() {
    return getFieldByClass(TextField.class);
  }

  public UsingOtherTemplateBox getUsingOtherTemplateBox() {
    return getFieldByClass(UsingOtherTemplateBox.class);
  }

  @Order(10.0)
  public class TextField extends AbstractStringField {
  }

  @Order(20.0)
  public class UsingOtherTemplateBox extends AbstractTestGroupBox {
  }

}
