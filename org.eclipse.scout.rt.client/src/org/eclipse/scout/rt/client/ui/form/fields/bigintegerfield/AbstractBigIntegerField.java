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
package org.eclipse.scout.rt.client.ui.form.fields.bigintegerfield;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.form.fields.numberfield.AbstractNumberField;
import org.eclipse.scout.rt.shared.data.form.ValidationRule;

public abstract class AbstractBigIntegerField extends AbstractNumberField<BigInteger> implements IBigIntegerField {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractBigIntegerField.class);

  public AbstractBigIntegerField() {
    this(true);
  }

  public AbstractBigIntegerField(boolean callInitializer) {
    super(callInitializer);
  }

  /*
   * Configuration
   */
  @ConfigProperty(ConfigProperty.BIG_INTEGER)
  @Order(250)
  @ValidationRule(ValidationRule.MIN_VALUE)
  @Override
  protected BigInteger getConfiguredMinValue() {
    return new BigInteger("-999999999999999999999999999999999999999999999999999999999999");
  }

  @Override
  @ConfigProperty(ConfigProperty.BIG_INTEGER)
  @Order(260)
  @ValidationRule(ValidationRule.MAX_VALUE)
  protected BigInteger getConfiguredMaxValue() {
    return new BigInteger("999999999999999999999999999999999999999999999999999999999999");
  }

  /**
   * uses {@link #parseToBigDecimalInternal(String)} to parse text and returns the result as BigInteger
   */
  @Override
  protected BigInteger parseValueInternal(String text) throws ProcessingException {
    BigInteger retVal = null;
    BigDecimal parsedVal = parseToBigDecimalInternal(text);
    if (parsedVal != null) {
      retVal = parsedVal.toBigIntegerExact();
    }
    return retVal;
  }

}
