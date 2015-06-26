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
package org.eclipse.scout.rt.client.ui.form.fields.snapbox;

import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.rt.client.extension.ui.form.fields.snapbox.ISnapBoxExtension;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractCompositeField;
import org.eclipse.scout.rt.client.ui.form.fields.snapbox.internal.SnapBoxGrid;

@ClassId("57ba326b-0e46-40bd-aa06-031f6f908b0e")
public abstract class AbstractSnapBox extends AbstractCompositeField implements ISnapBox {

  private SnapBoxGrid m_grid;

  public AbstractSnapBox() {
    this(true);
  }

  public AbstractSnapBox(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected void initConfig() {
    m_grid = new SnapBoxGrid(this);
    super.initConfig();
  }

  @Override
  public void rebuildFieldGrid() {
    m_grid.validate();
    if (isInitialized()) {
      if (getForm() != null) {
        getForm().structureChanged(this);
      }
    }
  }

  // box is only visible when it has at least one visible item
  @Override
  protected void handleFieldVisibilityChanged() {
    super.handleFieldVisibilityChanged();
    if (isInitialized()) {
      rebuildFieldGrid();
    }
  }

  @Override
  public final int getGridColumnCount() {
    return m_grid.getGridColumnCount();
  }

  @Override
  public final int getGridRowCount() {
    return m_grid.getGridRowCount();
  }

  protected static class LocalSnapBoxExtension<OWNER extends AbstractSnapBox> extends LocalCompositeFieldExtension<OWNER> implements ISnapBoxExtension<OWNER> {

    public LocalSnapBoxExtension(OWNER owner) {
      super(owner);
    }
  }

  @Override
  protected ISnapBoxExtension<? extends AbstractSnapBox> createLocalExtension() {
    return new LocalSnapBoxExtension<AbstractSnapBox>(this);
  }

}
