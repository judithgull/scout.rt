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
package org.eclipse.scout.rt.client.ui.wizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.scout.commons.WeakEventListener;
import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.client.ui.form.fields.htmlfield.AbstractHtmlField;

@ClassId("6936b8b8-6612-4efa-bf29-80a26f80b9da")
public abstract class AbstractWizardStatusField extends AbstractHtmlField {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractWizardStatusField.class);
  private IWizard m_wizard;
  private IWizardStatusHtmlProvider m_htmlProvider;
  private P_WizardListener m_scoutWizardListener;
  private P_WizardStepListener m_scoutWizardStepListener;
  private boolean m_dirty;
  public static final String STEP_ANCHOR_IDENTIFIER = "STEP_";

  public AbstractWizardStatusField() {
    this(true);
  }

  public AbstractWizardStatusField(boolean callInitializer) {
    super(callInitializer);
  }

  public IWizardStatusHtmlProvider getHtmlProvider() {
    return m_htmlProvider;
  }

  public void setHtmlProvider(IWizardStatusHtmlProvider htmlProvider) {
    m_htmlProvider = htmlProvider;
  }

  @Override
  protected int getConfiguredMaxLength() {
    return 1000000;
  }

  @Override
  protected boolean getConfiguredHtmlEditor() {
    return false;
  }

  @Override
  protected boolean getConfiguredScrollBarEnabled() {
    return false;
  }

  @Override
  protected boolean getConfiguredLabelVisible() {
    return false;
  }

  @Override
  protected void execInitField() throws ProcessingException {
    //automatically set wizard if the field is placed inside a wizard container form
    IForm f = getForm();
    while (f != null) {
      if (f instanceof IWizardContainerForm) {
        setWizard(((IWizardContainerForm) f).getWizard());
      }
      f = f.getOuterForm();
    }
  }

  public void setWizard(IWizard wizard) throws ProcessingException {
    if (m_wizard != null) {
      m_wizard.removeWizardListener(m_scoutWizardListener);
      m_wizard.removePropertyChangeListener(m_scoutWizardStepListener);
      m_scoutWizardListener = null;
      m_scoutWizardStepListener = null;
      m_wizard = null;
    }

    if (wizard != null) {
      m_wizard = wizard;
      m_scoutWizardListener = new P_WizardListener();
      m_wizard.addWizardListener(m_scoutWizardListener);
      m_wizard.addPropertyChangeListener(m_scoutWizardListener);
      // add step listeners
      m_scoutWizardStepListener = new P_WizardStepListener();
      for (IWizardStep step : m_wizard.getSteps()) {
        step.removePropertyChangeListener(m_scoutWizardStepListener);
        step.addPropertyChangeListener(m_scoutWizardStepListener);
      }
    }
    markDirty();
  }

  public IWizard getWizard() {
    return m_wizard;
  }

  @Override
  protected void execDisposeField() throws ProcessingException {
    m_dirty = false;
  }

  private void markDirty() {
    m_dirty = true;
    new ClientSyncJob("Wizard status - mark dirty", ClientSyncJob.getCurrentSession()) {
      @Override
      protected void runVoid(IProgressMonitor monitor) throws Throwable {
        if (m_dirty) {
          try {
            refreshStatus();
          }
          catch (ProcessingException e) {
            LOG.warn(null, e);
          }
        }
      }
    }.schedule();
  }

  public void refreshStatus() throws ProcessingException {
    m_dirty = false;
    try {
      if (m_htmlProvider == null) {
        m_htmlProvider = new DefaultWizardStatusHtmlProvider();
        m_htmlProvider.initialize(this);
      }
      setValue(m_htmlProvider.createHtml(m_wizard));
      // now scroll to the active step
      int index = 1;
      for (IWizardStep<?> step : m_wizard.getSteps()) {
        if (step == m_wizard.getActiveStep()) {
          setScrollToAnchor(STEP_ANCHOR_IDENTIFIER + index);
          break;
        }
        index++;
      }
    }
    catch (Exception e) {
      LOG.warn(null, e);
    }
  }

  private class P_WizardListener implements WizardListener, PropertyChangeListener, WeakEventListener {
    @Override
    public void wizardChanged(WizardEvent e) {
      switch (e.getType()) {
        case WizardEvent.TYPE_STATE_CHANGED: {
          // re-attach step listeners
          for (IWizardStep step : m_wizard.getSteps()) {
            step.removePropertyChangeListener(m_scoutWizardStepListener);
            step.addPropertyChangeListener(m_scoutWizardStepListener);
          }
          markDirty();
          break;
        }
      }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent e) {
      if (IWizard.PROP_TITLE.equals(e.getPropertyName()) ||
          IWizard.PROP_TITLE_HTML.equals(e.getPropertyName()) ||
          IWizard.PROP_TOOLTIP_TEXT.equals(e.getPropertyName())) {
        markDirty();
      }
    }
  }// end class P_ScoutWizardListener

  private class P_WizardStepListener implements PropertyChangeListener, WeakEventListener {
    @Override
    public void propertyChange(final PropertyChangeEvent e) {
      if (IWizardStep.PROP_DESCRIPTION_HTML.equals(e.getPropertyName()) ||
          IWizardStep.PROP_TITLE.equals(e.getPropertyName()) ||
          IWizardStep.PROP_TITLE_HTML.equals(e.getPropertyName()) ||
          IWizardStep.PROP_TOOLTIP_TEXT.equals(e.getPropertyName()) ||
          IWizardStep.PROP_ENABLED.equals(e.getPropertyName())) {
        markDirty();
      }
    }
  }// end class P_ScoutWizardStepListener

}
