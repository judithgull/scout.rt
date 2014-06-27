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
package org.eclipse.scout.rt.client.ui.form.fields.decimalfield;

import org.eclipse.scout.rt.client.ui.form.fields.IBasicField;
import org.eclipse.scout.rt.client.ui.form.fields.numberfield.INumberField;
import org.eclipse.scout.rt.client.ui.valuecontainer.IDecimalValueContainer;

/**
 * Field type representing a fractional, decimal number such as Float, Double,
 * BigDecimal
 */
@SuppressWarnings("deprecation")
public interface IDecimalField<T extends Number> extends INumberField<T>, IDecimalValueContainer<T> {

  /**
   * @deprecated use the facade defined by {@link IBasicField#getUIFacade()}.
   *             Will be removed in the 5.0 Release.
   */
  @Override
  @Deprecated
  IDecimalFieldUIFacade getUIFacade();

}
