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
package org.eclipse.scout.rt.spec.client.text;

import org.eclipse.scout.rt.shared.services.common.text.AbstractDynamicNlsTextProviderService;
import org.eclipse.scout.rt.shared.services.common.text.IDocumentationTextProviderService;

/**
 * Text provider service for texts used by org.eclipse.scout.rt.spec.client plugin.
 */
public class SpecDocsTextProviderService extends AbstractDynamicNlsTextProviderService implements IDocumentationTextProviderService {
  @Override
  protected String getDynamicNlsBaseName() {
    return "resources.docs.Docs";
  }
}
