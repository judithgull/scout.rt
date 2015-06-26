/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.form.fields.filechooserfield;

import org.eclipse.scout.rt.client.ui.form.fields.filechooserfield.IFileChooserField;
import org.eclipse.scout.rt.ui.rap.ext.DropDownFileUpload;
import org.eclipse.scout.rt.ui.rap.form.fields.IRwtScoutFormField;
import org.eclipse.swt.widgets.ProgressBar;

public interface IRwtScoutFileUploadField extends IRwtScoutFormField<IFileChooserField> {

  String VARIANT_FILECHOOSER = "filechooserfield_file";
  String VARIANT_DISABLED_SUFFIX = "-disabled";

  DropDownFileUpload getUiBrowseButton();

  ProgressBar getUiProgressBar();

}
