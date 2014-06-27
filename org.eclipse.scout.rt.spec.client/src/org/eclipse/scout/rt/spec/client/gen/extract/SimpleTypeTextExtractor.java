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
package org.eclipse.scout.rt.spec.client.gen.extract;

import org.eclipse.scout.rt.shared.TEXTS;

/**
 * An {@link IDocTextExtractor} for the simple class name as text.
 */
public class SimpleTypeTextExtractor<T> extends TypeExtractor<T> {

  public SimpleTypeTextExtractor() {
    this(TEXTS.get("org.eclipse.scout.rt.spec.type"));
  }

  public SimpleTypeTextExtractor(String name) {
    super(name);
  }

  /**
   * @param object
   *          the scout model entity
   * @return If the generic type is {@link Class} the name of the class is extracted else the name of the object^s
   *         class.
   */
  @Override
  public String getText(T object) {
    if (object instanceof Class) {
      return ((Class) object).getSimpleName();
    }
    return object.getClass().getSimpleName();
  }

}
