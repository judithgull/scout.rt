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
package org.eclipse.scout.rt.shared.extension.fixture;

import java.util.Collections;
import java.util.List;

import org.eclipse.scout.rt.shared.extension.IExtensibleObject;
import org.eclipse.scout.rt.shared.extension.IExtension;

/**
 * @since 4.2
 */
public class TestingExtensibleObject implements IExtensibleObject {

  @Override
  public List<? extends IExtension<?>> getAllExtensions() {
    return Collections.emptyList();
  }

  @Override
  public <T extends IExtension<?>> T getExtension(Class<T> c) {
    throw new UnsupportedOperationException("not implemented");
  }
}
