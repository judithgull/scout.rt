/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.extension.ui.form.fixture;

import org.eclipse.scout.commons.annotations.Extends;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.rt.client.extension.ui.form.AbstractFormExtension;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.AbstractStringField;

public class ExtendedFormExtension extends AbstractFormExtension<ExtendedForm> {

  public ExtendedFormExtension(ExtendedForm ownerForm) {
    super(ownerForm);
  }

  @Order(-10)
  @Extends(value = ExtendedForm.MainBox.class, pathToContainer = ExtendedForm.class)
  public class DetailBox extends AbstractGroupBox {

    @Order(10)
    public class StringField extends AbstractStringField {
    }
  }
}
