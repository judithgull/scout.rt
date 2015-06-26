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
package org.eclipse.scout.rt.client.ui.form.fields.customfield;

import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.rt.client.extension.ui.form.fields.customfield.ICustomFieldExtension;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;

/**
 * default Convenience implementation base for a custom field
 */
@ClassId("fa42ce51-a1c7-4d90-a680-6709f97da0f7")
public abstract class AbstractCustomField extends AbstractFormField implements ICustomField {

  public AbstractCustomField() {
    this(true);
  }

  public AbstractCustomField(boolean callInitializer) {
    super(callInitializer);
  }

  /*
   * Configuration
   */

  @Override
  protected void initConfig() {
    super.initConfig();
  }

  protected static class LocalCustomFieldExtension<OWNER extends AbstractCustomField> extends LocalFormFieldExtension<OWNER> implements ICustomFieldExtension<OWNER> {

    public LocalCustomFieldExtension(OWNER owner) {
      super(owner);
    }
  }

  @Override
  protected ICustomFieldExtension<? extends AbstractCustomField> createLocalExtension() {
    return new LocalCustomFieldExtension<AbstractCustomField>(this);
  }

  /*
   * Runtime
   */
}
