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
package org.eclipse.scout.rt.client.mobile.ui.form.fields.tabbox.fixture;

import org.eclipse.scout.commons.annotations.FormData;
import org.eclipse.scout.commons.annotations.FormData.DefaultSubtypeSdkCommand;
import org.eclipse.scout.commons.annotations.FormData.SdkCommand;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.AbstractStringField;

/**
 * Group Box with 2 text fields
 */
@FormData(value = AbstractTemplate2GroupBoxData.class, sdkCommand = SdkCommand.CREATE, defaultSubtypeSdkCommand = DefaultSubtypeSdkCommand.CREATE)
public abstract class AbstractTemplate3GroupBox extends AbstractGroupBox {

  public T3Text1Field getT3Text1Field() {
    return getFieldByClass(T3Text1Field.class);
  }

  @Order(10.0)
  public class T3Text1Field extends AbstractStringField {
  }

}
