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
package org.eclipse.scout.rt.client.ui.form.fields.tabbox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.extension.ui.form.fields.IFormFieldExtension;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tabbox.ITabBoxExtension;
import org.eclipse.scout.rt.client.extension.ui.form.fields.tabbox.TabBoxChains.TabBoxTabSelectedChain;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractCompositeField;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.IGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.tabbox.internal.TabBoxGrid;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;

@ClassId("14555c41-2d65-414a-94b1-d4328cbd818c")
public abstract class AbstractTabBox extends AbstractCompositeField implements ITabBox {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractTabBox.class);

  private ITabBoxUIFacade m_uiFacade;
  private TabBoxGrid m_grid;

  public AbstractTabBox() {
    this(true);
  }

  public AbstractTabBox(boolean callInitializer) {
    super(callInitializer);
  }

  /*
   * Configuration
   */

  @Override
  protected boolean getConfiguredGridUseUiHeight() {
    return true;
  }

  @Override
  protected int getConfiguredGridW() {
    return FULL_WIDTH;
  }

  @ConfigOperation
  @Order(70)
  protected void execTabSelected(IGroupBox selectedBox) throws ProcessingException {
  }

  @ConfigProperty(ConfigProperty.INTEGER)
  protected int getConfiguredMarkStrategy() {
    return MARK_STRATEGY_EMPTY;
  }

  @Override
  public int getMarkStrategy() {
    return propertySupport.getPropertyInt(PROP_MARK_STRATEGY);
  }

  @Override
  public void setMarkStrategy(int markStrategy) {
    propertySupport.setPropertyInt(PROP_MARK_STRATEGY, markStrategy);
  }

  @Override
  protected void initConfig() {
    m_uiFacade = new P_UIFacade();
    m_grid = new TabBoxGrid(this);
    setMarkStrategy(getConfiguredMarkStrategy());
    super.initConfig();
    addPropertyChangeListener(PROP_SELECTED_TAB, new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        // single observer exec
        try {
          interceptTabSelected(getSelectedTab());
        }
        catch (ProcessingException ex) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(ex);
        }
        catch (Throwable t) {
          SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected", t));
        }
      }
    });
  }

  /*
   * Runtime
   */

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
    IGroupBox selectedBox = getSelectedTab();
    if (selectedBox == null) {
      for (IGroupBox box : getGroupBoxes()) {
        if (box.isVisible()) {
          setSelectedTab(box);
          break;
        }
      }
    }
    else if (!selectedBox.isVisible()) {
      int index = getFieldIndex(selectedBox);
      List<IGroupBox> boxes = getGroupBoxes();
      // next to right side
      for (int i = index + 1; i < getFieldCount(); i++) {
        IGroupBox box = boxes.get(i);
        if (box.isVisible()) {
          setSelectedTab(box);
          break;
        }
      }
      if (getSelectedTab() == selectedBox) {
        // next to left side
        for (int i = index - 1; i >= 0; i--) {
          IGroupBox box = boxes.get(i);
          if (box.isVisible()) {
            setSelectedTab(box);
            break;
          }
        }
      }
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

  @Override
  public List<IGroupBox> getGroupBoxes() {
    List<IGroupBox> result = new ArrayList<IGroupBox>();
    for (IFormField field : getFields()) {
      if (field instanceof IGroupBox) {
        result.add((IGroupBox) field);
      }
      else {
        LOG.warn("Tabboxes only allow instance of IGroupBox as inner fields. '" + field.getClass().getName() + "' is not instance of IGroupBox!");
      }
    }
    return result;
  }

  @Override
  public void setSelectedTab(IGroupBox box) {
    if (box.getParentField() == this) {
      propertySupport.setProperty(PROP_SELECTED_TAB, box);
    }
  }

  @Override
  public IGroupBox getSelectedTab() {
    return (IGroupBox) propertySupport.getProperty(PROP_SELECTED_TAB);
  }

  @Override
  public ITabBoxUIFacade getUIFacade() {
    return m_uiFacade;
  }

  private class P_UIFacade implements ITabBoxUIFacade {
    @Override
    public void setSelectedTabFromUI(IGroupBox box) {
      setSelectedTab(box);
    }
  }

  protected final void interceptTabSelected(IGroupBox selectedBox) throws ProcessingException {
    List<? extends IFormFieldExtension<? extends AbstractFormField>> extensions = getAllExtensions();
    TabBoxTabSelectedChain chain = new TabBoxTabSelectedChain(extensions);
    chain.execTabSelected(selectedBox);
  }

  protected static class LocalTabBoxExtension<OWNER extends AbstractTabBox> extends LocalCompositeFieldExtension<OWNER> implements ITabBoxExtension<OWNER> {

    public LocalTabBoxExtension(OWNER owner) {
      super(owner);
    }

    @Override
    public void execTabSelected(TabBoxTabSelectedChain chain, IGroupBox selectedBox) throws ProcessingException {
      getOwner().execTabSelected(selectedBox);
    }
  }

  @Override
  protected ITabBoxExtension<? extends AbstractTabBox> createLocalExtension() {
    return new LocalTabBoxExtension<AbstractTabBox>(this);
  }
}
