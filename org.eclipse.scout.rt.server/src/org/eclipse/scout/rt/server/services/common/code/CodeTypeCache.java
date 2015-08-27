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
package org.eclipse.scout.rt.server.services.common.code;

/**
 * Maintains a cache of ICodeType objects that can be (re)loaded using the
 * methods reloadCodeType, reloadCodeTypes
 *
 * @deprecated use {@link org.eclipse.scout.rt.shared.services.common.code.CodeTypeCache} instead
 *             will be removed with N-Release
 */
@Deprecated
public class CodeTypeCache extends org.eclipse.scout.rt.shared.services.common.code.CodeTypeCache {
}