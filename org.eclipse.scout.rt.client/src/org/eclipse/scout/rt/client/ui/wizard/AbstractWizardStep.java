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

import java.util.List;

import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.IOrdered;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.beans.AbstractPropertyObserver;
import org.eclipse.scout.commons.beans.IPropertyObserver;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.extension.ui.wizard.IWizardStepExtension;
import org.eclipse.scout.rt.client.extension.ui.wizard.WizardStepChains.WizardStepActivateChain;
import org.eclipse.scout.rt.client.extension.ui.wizard.WizardStepChains.WizardStepDeactivateChain;
import org.eclipse.scout.rt.client.extension.ui.wizard.WizardStepChains.WizardStepDisposeChain;
import org.eclipse.scout.rt.client.extension.ui.wizard.WizardStepChains.WizardStepFormClosedChain;
import org.eclipse.scout.rt.client.extension.ui.wizard.WizardStepChains.WizardStepFormDiscardedChain;
import org.eclipse.scout.rt.client.extension.ui.wizard.WizardStepChains.WizardStepFormStoredChain;
import org.eclipse.scout.rt.client.ui.form.FormEvent;
import org.eclipse.scout.rt.client.ui.form.FormListener;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.shared.extension.AbstractExtension;
import org.eclipse.scout.rt.shared.extension.IExtensibleObject;
import org.eclipse.scout.rt.shared.extension.IExtension;
import org.eclipse.scout.rt.shared.extension.ObjectExtensions;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;

public abstract class AbstractWizardStep<FORM extends IForm> extends AbstractPropertyObserver implements IWizardStep<FORM>, IPropertyObserver, IExtensibleObject {
  private IWizard m_wizard;
  private FORM m_form;
  private FormListener m_formListener;
  private int m_activationCounter;
  private boolean m_initialized;
  private final ObjectExtensions<AbstractWizardStep<FORM>, IWizardStepExtension<FORM, ? extends AbstractWizardStep<FORM>>> m_objectExtensions;

  public AbstractWizardStep() {
    this(true);
  }

  public AbstractWizardStep(boolean callInitializer) {
    m_objectExtensions = new ObjectExtensions<AbstractWizardStep<FORM>, IWizardStepExtension<FORM, ? extends AbstractWizardStep<FORM>>>(this);
    if (callInitializer) {
      callInitializer();
    }
  }

  protected void callInitializer() {
    if (!m_initialized) {
      interceptInitConfig();
      m_initialized = true;
    }
  }

  /*
   * Configuration
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(10)
  protected boolean getConfiguredEnabled() {
    return true;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(20)
  protected String getConfiguredTitle() {
    return null;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(30)
  protected String getConfiguredTooltipText() {
    return null;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(35)
  protected String getConfiguredTitleHtml() {
    return null;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(38)
  protected String getConfiguredDescriptionHtml() {
    return null;
  }

  @ConfigProperty(ConfigProperty.ICON_ID)
  @Order(40)
  protected String getConfiguredIconId() {
    return null;
  }

  /**
   * Configures the view order of this wizard step. The view order determines the order in which the steps appear. The
   * order of steps with no view order configured ({@code < 0}) is initialized based on the {@link Order} annotation
   * of the wizard step class.
   * <p>
   * Subclasses can override this method. The default is {@link IOrdered#DEFAULT_ORDER}.
   *
   * @return View order of this wizard step.
   */
  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(60)
  protected double getConfiguredViewOrder() {
    return IOrdered.DEFAULT_ORDER;
  }

  /**
   * @param stepKind
   *          any of the STEP_* constants activate this step normally creates a
   *          form, calls {@link IForm#startWizardStep(IWizardStep, Class)} on
   *          the form and places the form inside the wizard
   *          {@link IWizard#setWizardForm(org.eclipse.scout.rt.client.ui.form.IForm)}
   */
  @Order(10)
  @ConfigOperation
  protected void execActivate(int stepKind) throws ProcessingException {
  }

  /**
   * @param stepKind
   *          any of the STEP_* constants deactivate this step
   */
  @Order(20)
  @ConfigOperation
  protected void execDeactivate(int stepKind) throws ProcessingException {
  }

  /**
   * dispose this step The default implementation closes the form at {@link #getForm()}
   */
  @Order(30)
  @ConfigOperation
  protected void execDispose() throws ProcessingException {
    FORM f = getForm();
    if (f != null) {
      f.doClose();
    }
  }

  /**
   * When the cached form is stored (it may still be open) this method is
   * called.
   *
   * @param activation
   *          true if this method is called by the wizard itself by {@link IWizardStep#activate(int)},
   *          {@link IWizardStep#deactivate(int)} or {@link IWizardStep#dispose()} The default implementation does
   *          nothing.
   */
  @Order(40)
  @ConfigOperation
  protected void execFormStored(boolean activation) throws ProcessingException {
  }

  /**
   * When the cached form is discarded (save was either not requested or it was
   * forcedly closed) this method is called.
   *
   * @param activation
   *          true if this method is called by the wizard itself by {@link IWizardStep#activate(int)},
   *          {@link IWizardStep#deactivate(int)} or {@link IWizardStep#dispose()} The default implementation does
   *          nothing.
   */
  @Order(50)
  @ConfigOperation
  protected void execFormDiscarded(boolean activation) throws ProcessingException {
  }

  /**
   * When the cached form is closed (after some store and/or a discard
   * operation) this method is called.
   *
   * @param activation
   *          true if this method is called by the wizard itself by {@link IWizardStep#activate(int)},
   *          {@link IWizardStep#deactivate(int)} or {@link IWizardStep#dispose()} The default implementation calls
   *          {@link IWizard#doNextStep()} iff activation=false and form was
   *          saved (formDataChanged=true)
   */
  @Order(60)
  @ConfigOperation
  protected void execFormClosed(boolean activation) throws ProcessingException {
    if (!activation) {
      if (getForm().isFormStored()) {
        getWizard().doNextStep();
      }
    }
  }

  /**
   * Calculates the column's view order, e.g. if the @Order annotation is set to 30.0, the method will
   * return 30.0. If no {@link Order} annotation is set, the method checks its super classes for an @Order annotation.
   *
   * @since 3.10.0-M4
   */
  protected double calculateViewOrder() {
    double viewOrder = getConfiguredViewOrder();
    Class<?> cls = getClass();
    if (viewOrder == IOrdered.DEFAULT_ORDER) {
      while (cls != null && IWizardStep.class.isAssignableFrom(cls)) {
        if (cls.isAnnotationPresent(Order.class)) {
          Order order = (Order) cls.getAnnotation(Order.class);
          return order.value();
        }
        cls = cls.getSuperclass();
      }
    }
    return viewOrder;
  }

  protected final void interceptInitConfig() {
    m_objectExtensions.initConfig(createLocalExtension(), new Runnable() {
      @Override
      public void run() {
        initConfig();
      }
    });
  }

  protected void initConfig() {
    setTitle(getConfiguredTitle());
    setTooltipText(getConfiguredTooltipText());
    setTitleHtml(getConfiguredTitleHtml());
    setDescriptionHtml(getConfiguredDescriptionHtml());
    setIconId(getConfiguredIconId());
    setEnabled(getConfiguredEnabled());
    setOrder(calculateViewOrder());
  }

  protected IWizardStepExtension<FORM, ? extends AbstractWizardStep<FORM>> createLocalExtension() {
    return new LocalWizardStepExtension<FORM, AbstractWizardStep<FORM>>(this);
  }

  @Override
  public final List<? extends IWizardStepExtension<FORM, ? extends AbstractWizardStep<FORM>>> getAllExtensions() {
    return m_objectExtensions.getAllExtensions();
  }

  @Override
  public <T extends IExtension<?>> T getExtension(Class<T> c) {
    return m_objectExtensions.getExtension(c);
  }

  /*
   * Runtime
   */

  @Override
  public FORM getForm() {
    return m_form;
  }

  @Override
  public void setForm(FORM f) {
    // remove old
    if (m_form != null) {
      if (m_formListener != null) {
        m_form.removeFormListener(m_formListener);
      }
    }
    m_form = f;
    // add old
    if (m_form != null) {
      if (m_formListener == null) {
        m_formListener = new FormListener() {
          @Override
          public void formChanged(FormEvent e) throws ProcessingException {
            try {
              switch (e.getType()) {
                case FormEvent.TYPE_STORE_AFTER: {
                  interceptFormStored(m_activationCounter > 0);
                  break;
                }
                case FormEvent.TYPE_DISCARDED: {
                  interceptFormDiscarded(m_activationCounter > 0);
                  break;
                }
                case FormEvent.TYPE_CLOSED: {
                  interceptFormClosed(m_activationCounter > 0);
                  break;
                }
              }
            }
            catch (ProcessingException pe) {
              SERVICES.getService(IExceptionHandlerService.class).handleException(pe);
            }
            catch (Throwable t) {
              SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected", t));
            }
            switch (e.getType()) {
              case FormEvent.TYPE_CLOSED: {
                setForm(null);
                break;
              }
            }
          }
        };
      }
      m_form.addFormListener(m_formListener);
    }
  }

  @Override
  public IWizard getWizard() {
    return m_wizard;
  }

  @Override
  public void setWizardInternal(IWizard w) {
    m_wizard = w;
  }

  @Override
  public String getIconId() {
    return propertySupport.getPropertyString(PROP_ICON_ID);
  }

  @Override
  public void setIconId(String s) {
    propertySupport.setPropertyString(PROP_ICON_ID, s);
  }

  @Override
  public String getTitle() {
    return propertySupport.getPropertyString(PROP_TITLE);
  }

  @Override
  public void setTitle(String s) {
    propertySupport.setPropertyString(PROP_TITLE, s);
  }

  @Override
  public String getTooltipText() {
    return propertySupport.getPropertyString(PROP_TOOLTIP_TEXT);
  }

  @Override
  public void setTooltipText(String s) {
    propertySupport.setPropertyString(PROP_TOOLTIP_TEXT, s);
  }

  @Override
  public String getTitleHtml() {
    return propertySupport.getPropertyString(PROP_TITLE_HTML);
  }

  @Override
  public void setTitleHtml(String s) {
    propertySupport.setPropertyString(PROP_TITLE_HTML, s);
  }

  @Override
  public String getDescriptionHtml() {
    return propertySupport.getPropertyString(PROP_DESCRIPTION_HTML);
  }

  @Override
  public void setDescriptionHtml(String s) {
    propertySupport.setPropertyString(PROP_DESCRIPTION_HTML, s);
  }

  @Override
  public boolean isEnabled() {
    return propertySupport.getPropertyBool(PROP_ENABLED);
  }

  @Override
  public void setEnabled(boolean b) {
    propertySupport.setPropertyBool(PROP_ENABLED, b);
  }

  @Override
  public double getOrder() {
    return propertySupport.getPropertyDouble(PROP_VIEW_ORDER);
  }

  @Override
  public void setOrder(double order) {
    propertySupport.setPropertyDouble(PROP_VIEW_ORDER, order);
  }

  @Override
  public void activate(int stepKind) throws ProcessingException {
    try {
      m_activationCounter++;
      interceptActivate(stepKind);
    }
    finally {
      m_activationCounter--;
    }
  }

  @Override
  public void deactivate(int stepKind) throws ProcessingException {
    try {
      m_activationCounter++;
      interceptDeactivate(stepKind);
    }
    finally {
      m_activationCounter--;
    }
  }

  @Override
  public void dispose() throws ProcessingException {
    try {
      m_activationCounter++;
      interceptDispose();
    }
    finally {
      m_activationCounter--;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getTitle() + "]";
  }

  /**
   * Needs to be overridden for dynamically added steps.
   */
  @Override
  public String classId() {
    return ConfigurationUtility.getAnnotatedClassIdWithFallback(getClass());
  }

  /**
   * The extension delegating to the local methods. This Extension is always at the end of the chain and will not call
   * any further chain elements.
   */
  protected static class LocalWizardStepExtension<FORM extends IForm, OWNER extends AbstractWizardStep<FORM>> extends AbstractExtension<OWNER> implements IWizardStepExtension<FORM, OWNER> {

    public LocalWizardStepExtension(OWNER owner) {
      super(owner);
    }

    @Override
    public void execDeactivate(WizardStepDeactivateChain<? extends IForm> chain, int stepKind) throws ProcessingException {
      getOwner().execDeactivate(stepKind);
    }

    @Override
    public void execDispose(WizardStepDisposeChain<? extends IForm> chain) throws ProcessingException {
      getOwner().execDispose();
    }

    @Override
    public void execFormClosed(WizardStepFormClosedChain<? extends IForm> chain, boolean activation) throws ProcessingException {
      getOwner().execFormClosed(activation);
    }

    @Override
    public void execActivate(WizardStepActivateChain<? extends IForm> chain, int stepKind) throws ProcessingException {
      getOwner().execActivate(stepKind);
    }

    @Override
    public void execFormDiscarded(WizardStepFormDiscardedChain<? extends IForm> chain, boolean activation) throws ProcessingException {
      getOwner().execFormDiscarded(activation);
    }

    @Override
    public void execFormStored(WizardStepFormStoredChain<? extends IForm> chain, boolean activation) throws ProcessingException {
      getOwner().execFormStored(activation);
    }

  }

  protected final void interceptDeactivate(int stepKind) throws ProcessingException {
    List<? extends IWizardStepExtension<FORM, ? extends AbstractWizardStep<? extends IForm>>> extensions = getAllExtensions();
    WizardStepDeactivateChain<FORM> chain = new WizardStepDeactivateChain<FORM>(extensions);
    chain.execDeactivate(stepKind);
  }

  protected final void interceptDispose() throws ProcessingException {
    List<? extends IWizardStepExtension<FORM, ? extends AbstractWizardStep<? extends IForm>>> extensions = getAllExtensions();
    WizardStepDisposeChain<FORM> chain = new WizardStepDisposeChain<FORM>(extensions);
    chain.execDispose();
  }

  protected final void interceptFormClosed(boolean activation) throws ProcessingException {
    List<? extends IWizardStepExtension<FORM, ? extends AbstractWizardStep<? extends IForm>>> extensions = getAllExtensions();
    WizardStepFormClosedChain<FORM> chain = new WizardStepFormClosedChain<FORM>(extensions);
    chain.execFormClosed(activation);
  }

  protected final void interceptActivate(int stepKind) throws ProcessingException {
    List<? extends IWizardStepExtension<FORM, ? extends AbstractWizardStep<? extends IForm>>> extensions = getAllExtensions();
    WizardStepActivateChain<FORM> chain = new WizardStepActivateChain<FORM>(extensions);
    chain.execActivate(stepKind);
  }

  protected final void interceptFormDiscarded(boolean activation) throws ProcessingException {
    List<? extends IWizardStepExtension<FORM, ? extends AbstractWizardStep<? extends IForm>>> extensions = getAllExtensions();
    WizardStepFormDiscardedChain<FORM> chain = new WizardStepFormDiscardedChain<FORM>(extensions);
    chain.execFormDiscarded(activation);
  }

  protected final void interceptFormStored(boolean activation) throws ProcessingException {
    List<? extends IWizardStepExtension<FORM, ? extends AbstractWizardStep<? extends IForm>>> extensions = getAllExtensions();
    WizardStepFormStoredChain<FORM> chain = new WizardStepFormStoredChain<FORM>(extensions);
    chain.execFormStored(activation);
  }
}
