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
package org.eclipse.scout.rt.client.ui.form.fields.bigdecimalfield;

import java.math.BigDecimal;

import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.form.fields.decimalfield.AbstractDecimalField;
import org.eclipse.scout.rt.shared.data.form.ValidationRule;

public abstract class AbstractBigDecimalField extends AbstractDecimalField<BigDecimal> implements IBigDecimalField {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractBigDecimalField.class);

  public AbstractBigDecimalField() {
    this(true);
  }

  public AbstractBigDecimalField(boolean callInitializer) {
    super(callInitializer);
  }

  /*
   * Configuration
   */
  @Override
  @ConfigProperty(ConfigProperty.BIG_DECIMAL)
  @Order(300)
  @ValidationRule(ValidationRule.MIN_VALUE)
  protected BigDecimal getConfiguredMinValue() {
    return new BigDecimal("-999999999999999999999999999999999999999999999999999999999999");
  }

  @Override
  @ConfigProperty(ConfigProperty.BIG_DECIMAL)
  @Order(310)
  @ValidationRule(ValidationRule.MAX_VALUE)
  protected BigDecimal getConfiguredMaxValue() {
    return new BigDecimal("999999999999999999999999999999999999999999999999999999999999");
  }

  /**
   * uses {@link #parseToBigDecimalInternal(String)} to parse text
   */
  @Override
  protected BigDecimal parseValueInternal(String text) throws ProcessingException {
    return parseToBigDecimalInternal(text);
  }

}
