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
package org.eclipse.scout.rt.shared.data.model;

import java.util.Map;

public interface IDataModel {

  /**
   * call init before using the data model structure
   */
  void init();

  IDataModelAttribute[] getAttributes();

  IDataModelEntity[] getEntities();

  /**
   * @return meta data for the attribute, default returns null
   *         <p>
   *         see {@link DataModelUtility}
   */
  Map<String, String> getMetaDataOfAttribute(IDataModelAttribute a);

}
