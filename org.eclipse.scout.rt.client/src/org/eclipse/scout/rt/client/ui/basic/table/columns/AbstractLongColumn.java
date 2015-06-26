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

import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.extension.ui.basic.table.columns.ILongColumnExtension;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.form.fields.longfield.AbstractLongField;
import org.eclipse.scout.rt.client.ui.form.fields.longfield.ILongField;

/**
 * Column holding Long
 */
@ClassId("97d8375f-84fa-4673-876f-9b274f218cce")
public abstract class AbstractLongColumn extends AbstractNumberColumn<Long> implements ILongColumn {

  public AbstractLongColumn() {
    super();
  }

  /*
   * Configuration
   */

  @Override
  @ConfigProperty(ConfigProperty.LONG)
  @Order(160)
  protected Long getConfiguredMaxValue() {
    return null;
  }

  @Override
  @ConfigProperty(ConfigProperty.LONG)
  @Order(170)
  protected Long getConfiguredMinValue() {
    return null;
  }

  /*
   * Runtime
   */

  @Override
  protected Long parseValueInternal(ITableRow row, Object rawValue) throws ProcessingException {
    Long validValue = null;
    if (rawValue == null) {
      validValue = null;
    }
    else if (rawValue instanceof Long) {
      validValue = (Long) rawValue;
    }
    else if (rawValue instanceof Number) {
      validValue = ((Number) rawValue).longValue();
    }
    else {
      throw new ProcessingException("invalid Long value in column '" + getClass().getSimpleName() + "': " + rawValue + " class=" + rawValue.getClass());
    }
    return validValue;
  }

  @Override
  protected ILongField getEditorField() {
    return new AbstractLongField() {
    };
  }

  protected static class LocalLongColumnExtension<OWNER extends AbstractLongColumn> extends LocalNumberColumnExtension<Long, OWNER> implements ILongColumnExtension<OWNER> {

    public LocalLongColumnExtension(OWNER owner) {
      super(owner);
    }
  }

  @Override
  protected ILongColumnExtension<? extends AbstractLongColumn> createLocalExtension() {
    return new LocalLongColumnExtension<AbstractLongColumn>(this);
  }

}
