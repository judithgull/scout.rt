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
package org.eclipse.scout.rt.client.ui.form.fields.imagebox;

import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.rt.shared.data.basic.AffineTransformSpec;

/**
 * , Samuel Moser
 */
public interface IImageFieldUIFacade {

  void setImageTransformFromUI(AffineTransformSpec t);

  TransferObject fireDragRequestFromUI();

  void fireDropActionFromUi(TransferObject transferObject);
}
