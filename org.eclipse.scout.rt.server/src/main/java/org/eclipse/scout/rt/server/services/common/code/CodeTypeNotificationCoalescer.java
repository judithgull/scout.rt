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
package org.eclipse.scout.rt.server.services.common.code;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.rt.server.notification.INotificationCoalescer;
import org.eclipse.scout.rt.shared.services.common.code.CodeTypeChangedNotification;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;

/**
 *
 */
public class CodeTypeNotificationCoalescer implements INotificationCoalescer<CodeTypeChangedNotification> {

  @Override
  public Set<CodeTypeChangedNotification> coalesce(Set<CodeTypeChangedNotification> notifications) {
    Set<Class<? extends ICodeType<?, ?>>> codeTypeClasses = new HashSet<>();
    for (CodeTypeChangedNotification n : notifications) {
      codeTypeClasses.addAll(n.getCodeTypes());
    }
    return CollectionUtility.hashSet(new CodeTypeChangedNotification(codeTypeClasses));
  }

}
