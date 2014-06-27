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

import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ISmartField;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;
import org.eclipse.scout.rt.shared.services.lookup.CodeLookupCall;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;

/**
 *
 */
public class SmartFieldTypeExtractor extends AbstractNamedTextExtractor<ISmartField<?>> {

  protected LinkableTypeExtractor<ICodeType> m_codeTypeExtroactor = new LinkableTypeExtractor<ICodeType>(ICodeType.class, true);
  protected LinkableTypeExtractor<ILookupCall<?>> m_linkableTypeExtractor = new LinkableTypeExtractor<ILookupCall<?>>(ILookupCall.class, true);

  public SmartFieldTypeExtractor() {
    super(TEXTS.get("org.eclipse.scout.rt.spec.type"));
  }

  @Override
  public String getText(ISmartField<?> smartfield) {
    StringBuilder text = new StringBuilder();
    Class codeTypeClass = getCodeTypeClass(smartfield);
    if (codeTypeClass != null) {
      text.append(TEXTS.get("org.eclipse.scout.rt.spec.codetype")).append(": ");
      text.append(m_codeTypeExtroactor.getText(getCodeTypeClass(smartfield)));
    }
    else if (smartfield.getLookupCall() != null) {
      text.append(TEXTS.get("org.eclipse.scout.rt.spec.lookupcall")).append(": ");
      text.append(m_linkableTypeExtractor.getText(smartfield.getLookupCall()));
    }
    else {
      text.append(TEXTS.get("org.eclipse.scout.rt.spec.na"));
    }
    return text.toString();
  }

  protected Class getCodeTypeClass(ISmartField<?> smartfield) {
    Class codeTypeClass = smartfield.getCodeTypeClass();
    if (codeTypeClass == null) {
      ILookupCall<?> lookupCall = smartfield.getLookupCall();
      if (lookupCall instanceof CodeLookupCall) {
        codeTypeClass = ((CodeLookupCall) lookupCall).getCodeTypeClass();
      }
    }
    return codeTypeClass;
  }

}
