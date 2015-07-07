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
package org.eclipse.scout.rt.shared.services.common.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.platform.BEANS;

/**
 * Convenience accessor for service ICodeService
 */
public final class CODES {

  private CODES() {
  }

  public static <T extends ICodeType<?, ?>> T getCodeType(Class<T> type) {
    return BEANS.get(ICodeService.class).getCodeType(type);
  }

  /**
   * @param id
   * @return
   *         Note that this method does not load code types, but only searches code types already loaded into the code
   *         service using {@link #getAllCodeTypes(String)}, {@link #getCodeType(Class)} etc.
   */
  public static <T> ICodeType<T, ?> findCodeTypeById(T id) {
    return BEANS.get(ICodeService.class).findCodeTypeById(id);
  }

  /**
   * @param id
   * @return
   *         Note that this method does not load code types, but only searches code types already loaded into the code
   *         service using {@link #getAllCodeTypes(String)}, {@link #getCodeType(Class)} etc.
   */
  public static <T> ICodeType<T, ?> findCodeTypeById(Long partitionId, T codeTypeId) {
    return BEANS.get(ICodeService.class).findCodeTypeById(partitionId, codeTypeId);
  }

  @SuppressWarnings("unchecked")
  public static List<ICodeType<?, ?>> getCodeTypes(Class<?>... types) {
    if (types == null) {
      return CollectionUtility.emptyArrayList();
    }
    List<Class<? extends ICodeType<?, ?>>> typeList = new ArrayList<Class<? extends ICodeType<?, ?>>>(types.length);
    for (Class<?> t : types) {
      if (ICodeType.class.isAssignableFrom(t)) {
        typeList.add((Class<? extends ICodeType<?, ?>>) t);
      }
    }
    return BEANS.get(ICodeService.class).getCodeTypes(typeList);
  }

  public static List<ICodeType<?, ?>> getCodeTypes(List<Class<? extends ICodeType<?, ?>>> types) {
    return BEANS.get(ICodeService.class).getCodeTypes(types);
  }

  public static <CODE_ID_TYPE, CODE extends ICode<CODE_ID_TYPE>> CODE getCode(Class<CODE> type) {
    return BEANS.get(ICodeService.class).getCode(type);
  }

  public static <T extends ICodeType> T reloadCodeType(Class<T> type) throws ProcessingException {
    return BEANS.get(ICodeService.class).reloadCodeType(type);
  }

  @SuppressWarnings("unchecked")
  public static List<ICodeType<?, ?>> reloadCodeTypes(Class<?>... types) throws ProcessingException {
    if (types == null) {
      return CollectionUtility.emptyArrayList();
    }
    List<Class<? extends ICodeType<?, ?>>> typeList = new ArrayList<Class<? extends ICodeType<?, ?>>>(types.length);
    for (Class<?> t : types) {
      if (ICodeType.class.isAssignableFrom(t)) {
        typeList.add((Class<? extends ICodeType<?, ?>>) t);
      }
    }
    return BEANS.get(ICodeService.class).reloadCodeTypes(typeList);
  }

  public static List<ICodeType<?, ?>> reloadCodeTypes(List<Class<? extends ICodeType<?, ?>>> types) throws ProcessingException {
    return BEANS.get(ICodeService.class).reloadCodeTypes(types);
  }

  public static Collection<ICodeType<?, ?>> getAllCodeTypes(String classPrefix) {
    return BEANS.get(ICodeService.class).getAllCodeTypes(classPrefix);
  }
}
