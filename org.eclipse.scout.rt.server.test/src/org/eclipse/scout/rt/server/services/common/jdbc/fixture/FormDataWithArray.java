/*******************************************************************************
 * Copyright (c) 2014 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.services.common.jdbc.fixture;

import java.util.Map;

import org.eclipse.scout.rt.shared.data.form.AbstractFormData;
import org.eclipse.scout.rt.shared.data.form.ValidationRule;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractValueFieldData;

public class FormDataWithArray extends AbstractFormData {

  private static final long serialVersionUID = 1L;

  public FormDataWithArray() {
  }

  public PersonNr getPersonNr() {
    return getFieldByClass(PersonNr.class);
  }

  public Roles getRoles() {
    return getFieldByClass(Roles.class);
  }

  public Value getValue() {
    return getFieldByClass(Value.class);
  }

  public static class PersonNr extends AbstractValueFieldData<Long> {

    private static final long serialVersionUID = 1L;

    public PersonNr() {
    }

    /**
     * list of derived validation rules.
     */
    @Override
    protected void initValidationRules(Map<String, Object> ruleMap) {
      super.initValidationRules(ruleMap);
      ruleMap.put(ValidationRule.MAX_VALUE, Long.MAX_VALUE);
      ruleMap.put(ValidationRule.MIN_VALUE, Long.MIN_VALUE);
    }
  }

  public static class Roles extends AbstractValueFieldData<Long[]> {

    private static final long serialVersionUID = 1L;

    public Roles() {
    }
  }

  public static class Value extends AbstractValueFieldData<String> {

    private static final long serialVersionUID = 1L;

    public Value() {
    }

    /**
     * list of derived validation rules.
     */
    @Override
    protected void initValidationRules(Map<String, Object> ruleMap) {
      super.initValidationRules(ruleMap);
      ruleMap.put(ValidationRule.MAX_LENGTH, 4000);
    }
  }
}
