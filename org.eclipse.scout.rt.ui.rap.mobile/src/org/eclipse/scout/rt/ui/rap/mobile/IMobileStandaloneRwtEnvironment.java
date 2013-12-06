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
package org.eclipse.scout.rt.ui.rap.mobile;

import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.shared.validate.markup.IMarkupValidator;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.mobile.form.fields.tablefield.IRwtScoutList;
import org.eclipse.scout.rt.ui.rap.mobile.form.fields.tablefield.RwtScoutListValidator;

/**
 * @since 3.10.0-M4
 */
public interface IMobileStandaloneRwtEnvironment extends IRwtEnvironment {

  RwtScoutListValidator createListValidator(IColumn<?> column, IRwtScoutList uiList, IMarkupValidator markupValidator);

}
