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

import java.util.List;

import org.eclipse.scout.rt.shared.extension.AbstractExtensionChain;
import org.eclipse.scout.rt.shared.extension.IExtension;

/**
 * @since 4.2
 */
public class TestingExtensionChain<EXTENSION> extends AbstractExtensionChain<EXTENSION> {

  public TestingExtensionChain(List<? extends IExtension<?>> extensions, Class<? extends IExtension> filterClass) {
    super(extensions, filterClass);
  }
}
