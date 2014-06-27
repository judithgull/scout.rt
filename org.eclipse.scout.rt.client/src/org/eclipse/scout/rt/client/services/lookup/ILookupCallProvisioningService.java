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
package org.eclipse.scout.rt.client.services.lookup;

import org.eclipse.scout.rt.client.ui.basic.table.columns.ISmartColumn;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.IListBox;
import org.eclipse.scout.rt.client.ui.form.fields.radiobuttongroup.IRadioButtonGroup;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.IContentAssistField;
import org.eclipse.scout.rt.client.ui.form.fields.treebox.ITreeBox;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;
import org.eclipse.scout.service.IService;

/**
 * This service creates lookup call instances from lookup call templates.
 * <p>
 * Callers are fields and components that deal with getConfiguredLookupCall and getConfiguredCodeType and create new
 * instances usign clone().
 * <p>
 * Known are {@link IContentAssistField}, {@link IListBox}, {@link ITreeBox}, {@link IRadioButtonGroup},
 * {@link ISmartColumn}
 * <p>
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=388242
 * 
 * @since 3.8.1
 */
public interface ILookupCallProvisioningService extends IService {

  /**
   * @return a new cloned and provisioned instance of the lookup call template.
   */
  <T> ILookupCall<T> newClonedInstance(ILookupCall<T> templateCall, IProvisioningContext context);

}
