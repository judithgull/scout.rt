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
package org.eclipse.scout.rt.client.ui.form;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.scout.commons.BeanUtility;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.StoppableThread;
import org.eclipse.scout.commons.XmlUtility;
import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.FormData;
import org.eclipse.scout.commons.annotations.FormData.SdkCommand;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.OrderedComparator;
import org.eclipse.scout.commons.beans.AbstractPropertyObserver;
import org.eclipse.scout.commons.beans.FastPropertyDescriptor;
import org.eclipse.scout.commons.beans.IPropertyFilter;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.exception.VetoException;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.holders.IHolder;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.status.IStatus;
import org.eclipse.scout.rt.client.BlockingCondition;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormAddSearchTermsChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormCheckFieldsChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormCloseTimerChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormCreateFormDataChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormDataChangedChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormDisposeFormChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormFormActivatedChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormInactivityTimerChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormInitFormChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormOnCloseRequestChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormOnVetoExceptionChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormResetSearchFilterChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormStoredChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormTimerChain;
import org.eclipse.scout.rt.client.extension.ui.form.FormChains.FormValidateChain;
import org.eclipse.scout.rt.client.extension.ui.form.IFormExtension;
import org.eclipse.scout.rt.client.extension.ui.form.MoveFormFieldsHandler;
import org.eclipse.scout.rt.client.services.common.search.ISearchFilterService;
import org.eclipse.scout.rt.client.ui.DataChangeListener;
import org.eclipse.scout.rt.client.ui.IEventHistory;
import org.eclipse.scout.rt.client.ui.WeakDataChangeListener;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.tool.IToolButton;
import org.eclipse.scout.rt.client.ui.basic.filechooser.FileChooser;
import org.eclipse.scout.rt.client.ui.desktop.AbstractDesktop;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;
import org.eclipse.scout.rt.client.ui.form.fields.ICompositeField;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.IFormFieldFilter;
import org.eclipse.scout.rt.client.ui.form.fields.IValidateContentDescriptor;
import org.eclipse.scout.rt.client.ui.form.fields.IValueField;
import org.eclipse.scout.rt.client.ui.form.fields.button.ButtonEvent;
import org.eclipse.scout.rt.client.ui.form.fields.button.ButtonListener;
import org.eclipse.scout.rt.client.ui.form.fields.button.IButton;
import org.eclipse.scout.rt.client.ui.form.fields.composer.IComposerField;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.IGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.tabbox.ITabBox;
import org.eclipse.scout.rt.client.ui.form.fields.wrappedform.IWrappedFormField;
import org.eclipse.scout.rt.client.ui.form.internal.FindFieldByFormDataIdVisitor;
import org.eclipse.scout.rt.client.ui.form.internal.FindFieldByXmlIdsVisitor;
import org.eclipse.scout.rt.client.ui.form.internal.FormDataPropertyFilter;
import org.eclipse.scout.rt.client.ui.messagebox.IMessageBox;
import org.eclipse.scout.rt.client.ui.messagebox.MessageBox;
import org.eclipse.scout.rt.client.ui.profiler.DesktopProfiler;
import org.eclipse.scout.rt.client.ui.wizard.IWizard;
import org.eclipse.scout.rt.client.ui.wizard.IWizardStep;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.data.form.AbstractFormData;
import org.eclipse.scout.rt.shared.data.form.FormDataUtility;
import org.eclipse.scout.rt.shared.data.form.IPropertyHolder;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractFormFieldData;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractValueFieldData;
import org.eclipse.scout.rt.shared.data.form.fields.tablefield.AbstractTableFieldData;
import org.eclipse.scout.rt.shared.data.form.properties.AbstractPropertyData;
import org.eclipse.scout.rt.shared.extension.AbstractExtension;
import org.eclipse.scout.rt.shared.extension.ContributionComposite;
import org.eclipse.scout.rt.shared.extension.ExtensionUtility;
import org.eclipse.scout.rt.shared.extension.IContributionOwner;
import org.eclipse.scout.rt.shared.extension.IExtensibleObject;
import org.eclipse.scout.rt.shared.extension.IExtension;
import org.eclipse.scout.rt.shared.extension.ObjectExtensions;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.service.SERVICES;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@FormData(value = AbstractFormData.class, sdkCommand = SdkCommand.USE)
public abstract class AbstractForm extends AbstractPropertyObserver implements IForm, IExtensibleObject, IContributionOwner {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractForm.class);

  private boolean m_initialized;
  private final EventListenerList m_listenerList = new EventListenerList();
  private IFormUIFacade m_uiFacade;
  private IWizardStep m_wizardStep;
  private boolean m_modal;// no property, is fixed
  private boolean m_cacheBounds; // no property is fixed
  private boolean m_askIfNeedSave;
  private boolean m_buttonsArmed;
  private boolean m_closeTimerArmed;
  private boolean m_formStored;
  private boolean m_formLoading;
  private final BlockingCondition m_blockingCondition;
  private boolean m_autoRegisterInDesktopOnStart;
  private int m_displayHint;// no property, is fixed
  private String m_displayViewId;// no property, is fixed
  private int m_closeType = IButton.SYSTEM_TYPE_NONE;
  private String m_cancelVerificationText;
  private File m_lastXmlFileForStorage;
  private IGroupBox m_mainBox;
  private IWrappedFormField m_wrappedFormField;
  private P_SystemButtonListener m_systemButtonListener;
  private List<IToolButton> m_toolbuttons;

  private IFormHandler m_handler;
  // access control
  private boolean m_enabledGranted;
  private boolean m_visibleGranted;
  // search
  private SearchFilter m_searchFilter;
  //validate content assistant
  private IValidateContentDescriptor m_currentValidateContentDescriptor;

  // current timers
  private P_CloseTimer m_scoutCloseTimer;
  private Map<String, P_Timer> m_scoutTimerMap;
  private DataChangeListener m_internalDataChangeListener;
  private final IEventHistory<FormEvent> m_eventHistory;

  // field replacement support
  private Map<Class<?>, Class<? extends IFormField>> m_fieldReplacements;
  private IContributionOwner m_contributionHolder;
  private final ObjectExtensions<AbstractForm, IFormExtension<? extends AbstractForm>> m_objectExtensions;

  private String m_classId;

  private int m_toolbarLocation;

  public AbstractForm() throws ProcessingException {
    this(true);
  }

  public AbstractForm(boolean callInitializer) throws ProcessingException {
    if (DesktopProfiler.getInstance().isEnabled()) {
      DesktopProfiler.getInstance().registerForm(this);
    }
    m_eventHistory = createEventHistory();
    m_enabledGranted = true;
    m_visibleGranted = true;
    m_formLoading = true;
    m_blockingCondition = new BlockingCondition(false);
    m_objectExtensions = new ObjectExtensions<AbstractForm, IFormExtension<? extends AbstractForm>>(this);
    if (callInitializer) {
      callInitializer();
    }
  }

  @Override
  public final List<? extends IFormExtension<? extends AbstractForm>> getAllExtensions() {
    return m_objectExtensions.getAllExtensions();
  }

  @Override
  public <T extends IExtension<?>> T getExtension(Class<T> c) {
    return m_objectExtensions.getExtension(c);
  }

  @Override
  public final List<Object> getAllContributions() {
    return m_contributionHolder.getAllContributions();
  }

  @Override
  public final <T> List<T> getContributionsByClass(Class<T> type) {
    return m_contributionHolder.getContributionsByClass(type);
  }

  @Override
  public final <T> T getContribution(Class<T> contribution) {
    return m_contributionHolder.getContribution(contribution);
  }

  protected void callInitializer() throws ProcessingException {
    if (!m_initialized) {
      interceptInitConfig();
      postInitConfig();
      m_initialized = true;
    }
  }

  protected IFormExtension<? extends AbstractForm> createLocalExtension() {
    return new LocalFormExtension<AbstractForm>(this);
  }

  /*
   * Configuration
   */
  /**
   * @return the localized title property of the form. Use {@link TEXTS}.
   */
  @ConfigProperty(ConfigProperty.TEXT)
  @Order(10)
  protected String getConfiguredTitle() {
    return null;
  }

  /**
   * @return the localized sub-title property of the form. Use {@link TEXTS}.
   */
  @ConfigProperty(ConfigProperty.TEXT)
  @Order(11)
  protected String getConfiguredSubTitle() {
    return null;
  }

  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(20)
  protected int/* seconds */getConfiguredCloseTimer() {
    return 0;
  }

  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(40)
  protected int/* seconds */getConfiguredCustomTimer() {
    return 0;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(90)
  protected String getConfiguredCancelVerificationText() {
    return ScoutTexts.get("FormSaveChangesQuestion");
  }

  @ConfigProperty(ConfigProperty.FORM_DISPLAY_HINT)
  @Order(100)
  protected int getConfiguredDisplayHint() {
    return DISPLAY_HINT_DIALOG;
  }

  @ConfigProperty(ConfigProperty.FORM_VIEW_ID)
  @Order(105)
  protected String getConfiguredDisplayViewId() {
    return null;
  }

  /**
   * @return if the form can be minimized.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(108)
  protected boolean getConfiguredMinimizeEnabled() {
    return false;
  }

  /**
   * @return if the form can be maximized.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(109)
  protected boolean getConfiguredMaximizeEnabled() {
    return false;
  }

  /**
   * @return defines if the form is initially minimized
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(110)
  protected boolean getConfiguredMinimized() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(112)
  protected boolean getConfiguredMaximized() {
    return false;
  }

  /**
   * Modal defines if a form is the only form a user can interact with within the same process as long as this form is
   * open.
   *
   * @return <code>true</code> if form is modal, <code>false</code> otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(120)
  protected boolean getConfiguredModal() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(140)
  protected boolean getConfiguredCacheBounds() {
    return false;
  }

  /**
   * AskIfNeedSave defines if a message box with yes, no and cancel option is shown to the user for confirmation
   * after having made at least one change in the form and then having pressed the cancel button.
   *
   * @return <code>true</code> if message box is shown for confirmation, <code>false</code> otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(150)
  protected boolean getConfiguredAskIfNeedSave() {
    return true;
  }

  /**
   * @return configured icon ID for this form
   */
  @ConfigProperty(ConfigProperty.ICON_ID)
  @Order(160)
  protected String getConfiguredIconId() {
    return null;
  }

  /**
   * one of {@link IForm#TOOLBAR_FORM_HEADER} or {@link IForm#TOOLBAR_VIEW_PART}. The {@link IForm#TOOLBAR_VIEW_PART} is
   * only considered if the form is displayed as a view (display hint {@link IForm#DISPLAY_HINT_VIEW} and SWT
   * representation. In all other cases the fallback {@link IForm#TOOLBAR_FORM_HEADER} is taken.
   *
   * @return {@link IForm#TOOLBAR_FORM_HEADER} | {@link IForm#TOOLBAR_VIEW_PART}
   */
  @ConfigProperty(ConfigProperty.TOOLBAR_LOCATION)
  @Order(170)
  protected int getConfiguredToolbarLocation() {
    return TOOLBAR_FORM_HEADER;
  }

  /**
   * This method is called to get an exclusive key of the form. The key is used
   * to open the same form with the same handler only once. Obviously this
   * behavior can only be used for view forms.
   *
   * @see AbstractDesktop#getSimilarViewForms(IForm)
   * @return null for exclusive form behavior an exclusive key to ensure similar
   *         handling.
   * @throws ProcessingException
   */
  @Override
  public Object computeExclusiveKey() throws ProcessingException {
    return null;
  }

  /**
   * Initialize the form and all of its fields.
   * By default any of the #start* methods of the form call this method
   * <p>
   * This method is called in the process of the initialization. The UI is not ready yet.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(10)
  protected void execInitForm() throws ProcessingException {
  }

  /**
   * This method is called when UI is ready.
   *
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(11)
  protected void execFormActivated() throws ProcessingException {
  }

  /**
   * see {@link IDesktop#dataChanged(Object...)}
   */
  @ConfigOperation
  @Order(13)
  protected void execDataChanged(Object... dataTypes) throws ProcessingException {
  }

  /**
   * This method is called in order to check field validity.<br>
   * This method is called before {@link IFormHandler#interceptCheckFields()} and
   * before the form is validated and stored.<br>
   * After this method, the form is checking fields itself and displaying a
   * dialog with missing and invalid fields.
   *
   * @return true when this check is done and further checks can continue, false
   *         to silently cancel the current process
   * @throws ProcessingException
   *           to cancel the current process with error handling and user
   *           notification such as a dialog
   */
  @ConfigOperation
  @Order(13)
  protected boolean execCheckFields() throws ProcessingException {
    return true;
  }

  /**
   * This method is called in order to update derived states like button
   * enablings.<br>
   * This method is called before {@link IFormHandler#interceptValidate()} and before
   * the form is stored.
   *
   * @return true when validate is successful, false to silently cancel the
   *         current process
   * @throws ProcessingException
   *           to cancel the current process with error handling and user
   *           notification such as a dialog
   */
  @ConfigOperation
  @Order(14)
  protected boolean execValidate() throws ProcessingException {
    return true;
  }

  /**
   * This method is called in order to update pages on the desktop after the
   * form stored data.<br>
   * This method is called after {@link IFormHandler#execStore()}.
   */
  @ConfigOperation
  @Order(16)
  protected void execStored() throws ProcessingException {
  }

  /**
   * @throws ProcessingException
   *           / {@link VetoException} if the exception should produce further
   *           info messages (default)
   */
  @ConfigOperation
  @Order(17)
  protected void execOnVetoException(VetoException e, int code) throws ProcessingException {
    throw e;
  }

  /**
   * @param kill
   *          true if a widget close icon (normally the X on the titlebar) was
   *          pressed or ESC was pressed
   * @param enabledButtonSystemTypes
   *          set of all {@link IButton#SYSTEM_TYPE_*} of all enabled and
   *          visible buttons of this form
   */
  @ConfigOperation
  @Order(18)
  protected void execOnCloseRequest(boolean kill, final HashSet<Integer> enabledButtonSystemTypes) throws ProcessingException {
    if (enabledButtonSystemTypes.contains(IButton.SYSTEM_TYPE_CLOSE)) {
      doClose();
    }
    else if (enabledButtonSystemTypes.contains(IButton.SYSTEM_TYPE_CANCEL)) {
      doCancel();
    }
    else if (!isAskIfNeedSave()) {
      doClose();
    }
    else {
      LOG.info("Trying to close a form (" + getClass().getName() + " - " + getTitle() + ") with no enabled close button! override getConfiguredAskIfNeedSave() to false to make this form is unsaveable.");
    }
  }

  @ConfigOperation
  @Order(19)
  protected void execDisposeForm() throws ProcessingException {
  }

  @ConfigOperation
  @Order(20)
  protected void execCloseTimer() throws ProcessingException {
    doClose();
  }

  @ConfigOperation
  @Order(30)
  protected void execInactivityTimer() throws ProcessingException {
    doClose();
  }

  @ConfigOperation
  @Order(40)
  protected void execTimer(String timerId) throws ProcessingException {
    LOG.info("execTimer " + timerId);
  }

  /**
   * add verbose information to the search filter
   */
  @ConfigOperation
  @Order(50)
  protected void execAddSearchTerms(SearchFilter search) {
  }

  private Class<? extends IGroupBox> getConfiguredMainBox() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.filterClassIgnoringInjectFieldAnnotation(dca, IGroupBox.class);
  }

  private List<Class<IFormField>> getConfiguredInjectedFields() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.filterClassesWithInjectFieldAnnotation(dca, IFormField.class);
  }

  protected List<Class<IToolButton>> getConfiguredToolButtons() {
    Class<?>[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.filterClasses(dca, IToolButton.class);
  }

  protected final void interceptInitConfig() throws ProcessingException {
    final Holder<ProcessingException> exceptionHolder = new Holder<ProcessingException>(ProcessingException.class, null);
    m_objectExtensions.initConfig(createLocalExtension(), new Runnable() {
      @Override
      public void run() {
        try {
          initConfig();
        }
        catch (ProcessingException e) {
          exceptionHolder.setValue(e);
        }
      }
    });
    if (exceptionHolder.getValue() != null) {
      throw exceptionHolder.getValue();
    }
  }

  protected void initConfig() throws ProcessingException {
    m_uiFacade = new P_UIFacade();
    m_scoutTimerMap = new HashMap<String, P_Timer>();
    m_autoRegisterInDesktopOnStart = true;
    m_contributionHolder = new ContributionComposite(this);
    setToolbarLocation(getConfiguredToolbarLocation());

    // tool buttons
    List<Class<IToolButton>> configuredToolButtons = getConfiguredToolButtons();
    List<IToolButton> toolButtonList = new ArrayList<IToolButton>(configuredToolButtons.size());
    for (Class<? extends IToolButton> clazz : configuredToolButtons) {
      try {
        IToolButton b = ConfigurationUtility.newInnerInstance(this, clazz);
        toolButtonList.add(b);
      }
      catch (Throwable t) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unable to create ToolButton '" + clazz.getName() + "'.", t));
      }
    }
    List<IToolButton> contributedToolButtons = m_contributionHolder.getContributionsByClass(IToolButton.class);
    toolButtonList.addAll(contributedToolButtons);
    ExtensionUtility.moveModelObjects(toolButtonList);
    Collections.sort(toolButtonList, new OrderedComparator());

    m_toolbuttons = toolButtonList;

    // prepare injected fields
    List<Class<IFormField>> fieldArray = getConfiguredInjectedFields();
    DefaultFormFieldInjection injectedFields = null;

    IGroupBox rootBox = getRootGroupBox();
    try {
      if (fieldArray.size() > 0) {
        injectedFields = new DefaultFormFieldInjection(this);
        injectedFields.addFields(fieldArray);
        FormFieldInjectionThreadLocal.push(injectedFields);
      }

      // add mainbox if getter returns null
      if (rootBox == null) {
        List<IGroupBox> contributedFields = m_contributionHolder.getContributionsByClass(IGroupBox.class);
        rootBox = CollectionUtility.firstElement(contributedFields);
        if (rootBox == null) {
          Class<? extends IGroupBox> mainBoxClass = getConfiguredMainBox();
          try {
            rootBox = ConfigurationUtility.newInnerInstance(this, mainBoxClass);
          }
          catch (Throwable t) {
            String mainBoxName = null;
            if (mainBoxClass == null) {
              mainBoxName = "null";
            }
            else {
              mainBoxName = mainBoxClass.getName();
            }
            SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + mainBoxName + "'.", t));
          }
        }
        m_mainBox = rootBox;
      }
    }
    finally {
      if (injectedFields != null) {
        m_fieldReplacements = injectedFields.getReplacementMapping();
        FormFieldInjectionThreadLocal.pop(injectedFields);
      }
    }
    if (rootBox != null) {
      rootBox.setFormInternal(this);
      rootBox.setMainBox(true);
      rootBox.updateKeyStrokes();
    }

    // move form fields
    new MoveFormFieldsHandler(this).moveFields();

    //
    if (getConfiguredCloseTimer() > 0) {
      setCloseTimer(getConfiguredCloseTimer());
    }
    if (getConfiguredCustomTimer() > 0) {
      setTimer("custom", getConfiguredCustomTimer());
    }
    setModal(getConfiguredModal());
    setDisplayHint(getConfiguredDisplayHint());
    setDisplayViewId(getConfiguredDisplayViewId());
    if (getConfiguredCancelVerificationText() != null) {
      setCancelVerificationText(getConfiguredCancelVerificationText());
    }
    if (getConfiguredTitle() != null) {
      setTitle(getConfiguredTitle());
    }
    if (getConfiguredSubTitle() != null) {
      setSubTitle(getConfiguredSubTitle());
    }
    setMinimizeEnabled(getConfiguredMinimizeEnabled());
    setMaximizeEnabled(getConfiguredMaximizeEnabled());
    setMinimized(getConfiguredMinimized());
    setMaximized(getConfiguredMaximized());
    setCacheBounds(getConfiguredCacheBounds());
    setAskIfNeedSave(getConfiguredAskIfNeedSave());
    setIconId(getConfiguredIconId());

    // visit all system buttons and attach observer
    m_systemButtonListener = new P_SystemButtonListener();// is auto-detaching
    IFormFieldVisitor v2 = new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (field instanceof IButton) {
          if (((IButton) field).getSystemType() != IButton.SYSTEM_TYPE_NONE) {
            ((IButton) field).addButtonListener(m_systemButtonListener);
          }
        }
        return true;
      }
    };
    visitFields(v2);
    getRootGroupBox().addPropertyChangeListener(new P_MainBoxPropertyChangeProxy());
    setButtonsArmed(true);
  }

  @Override
  public void setEnabledPermission(Permission p) {
    boolean b;
    if (p != null) {
      b = SERVICES.getService(IAccessControlService.class).checkPermission(p);
    }
    else {
      b = true;
    }
    setEnabledGranted(b);
  }

  @Override
  public boolean isEnabledGranted() {
    return m_enabledGranted;
  }

  @Override
  public void setEnabledGranted(boolean b) {
    m_enabledGranted = b;
    IGroupBox box = getRootGroupBox();
    if (box != null) {
      box.setEnabledGranted(b);
    }
  }

  @Override
  public void setVisiblePermission(Permission p) {
    boolean b;
    if (p != null) {
      b = SERVICES.getService(IAccessControlService.class).checkPermission(p);
    }
    else {
      b = true;
    }
    setVisibleGranted(b);
  }

  @Override
  public boolean isVisibleGranted() {
    return m_visibleGranted;
  }

  @Override
  public void setVisibleGranted(boolean b) {
    m_visibleGranted = b;
    IGroupBox box = getRootGroupBox();
    if (box != null) {
      box.setVisibleGranted(b);
    }
  }

  @Override
  public String getIconId() {
    return propertySupport.getPropertyString(PROP_ICON_ID);
  }

  @Override
  public void setIconId(String iconId) {
    propertySupport.setPropertyString(PROP_ICON_ID, iconId);
  }

  @Override
  public String getPerspectiveId() {
    return propertySupport.getPropertyString(PROP_PERSPECTIVE_ID);
  }

  @Override
  public void setPerspectiveId(String perspectiveId) {
    propertySupport.setPropertyString(PROP_PERSPECTIVE_ID, perspectiveId);
  }

  @Override
  public List<IToolButton> getToolButtons() {
    return CollectionUtility.arrayList(m_toolbuttons);
  }

  /**
   * @param configuredToolbuttonLocation
   */
  private void setToolbarLocation(int toolbarLocation) {
    m_toolbarLocation = toolbarLocation;
  }

  @Override
  public int getToolbarLocation() {
    return m_toolbarLocation;
  }

  @Override
  public <T extends IToolButton> T getToolButtonByClass(Class<T> clazz) {
    for (IToolButton b : m_toolbuttons) {
      if (b.getClass() == clazz) {
        @SuppressWarnings("unchecked")
        T button = (T) b;
        return button;
      }
    }
    return null;
  }

  /**
   * Register a {@link DataChangeListener} on the desktop for these dataTypes<br>
   * Example:
   *
   * <pre>
   * registerDataChangeListener(CRMEnum.Company, CRMEnum.Project, CRMEnum.Task);
   * </pre>
   */
  public void registerDataChangeListener(Object... dataTypes) {
    if (m_internalDataChangeListener == null) {
      m_internalDataChangeListener = new WeakDataChangeListener() {
        @Override
        public void dataChanged(Object... innerDataTypes) throws ProcessingException {
          interceptDataChanged(innerDataTypes);
        }
      };
    }
    IDesktop desktop = getDesktop();
    if (desktop == null) {
      desktop = ClientSyncJob.getCurrentSession().getVirtualDesktop();
    }
    desktop.addDataChangeListener(m_internalDataChangeListener, dataTypes);
  }

  /**
   * Unregister the {@link DataChangeListener} from the desktop for these
   * dataTypes<br>
   * Example:
   *
   * <pre>
   * unregisterDataChangeListener(CRMEnum.Company, CRMEnum.Project, CRMEnum.Task);
   * </pre>
   */
  public void unregisterDataChangeListener(Object... dataTypes) {
    if (m_internalDataChangeListener != null) {
      getDesktop().removeDataChangeListener(m_internalDataChangeListener, dataTypes);
    }
  }

  protected IForm startInternalExclusive(IFormHandler handler) throws ProcessingException {
    if (m_blockingCondition.isBlocking()) {
      throw new ProcessingException("The form " + getFormId() + " has already been started");
    }
    for (IForm simCandidate : getDesktop().getSimilarViewForms(this)) {
      if (handler != null && simCandidate.getHandler() != null && handler.getClass().getName() == simCandidate.getHandler().getClass().getName()) {
        if (simCandidate.getHandler().isOpenExclusive() && handler.isOpenExclusive()) {
          getDesktop().ensureVisible(simCandidate);
          return simCandidate;
        }
      }
    }
    return startInternal(handler);
  }

  @Override
  public void start() throws ProcessingException {
    if (getHandler() == null) {
      throw new ProcessingException("Handler must not be null.");
    }
    startInternal(getHandler());
  }

  /**
   * This method is called from the implemented handler methods in a explicit
   * form subclass
   */
  protected IForm startInternal(final IFormHandler handler) throws ProcessingException {
    if (m_blockingCondition.isBlocking()) {
      throw new ProcessingException("The form " + getFormId() + " has already been started");
    }
    setHandler(handler);
    m_closeType = IButton.SYSTEM_TYPE_NONE;
    m_blockingCondition.setBlocking(true);
    try {
      // check if form was made invisible ( = access control denied access)
      if (!getRootGroupBox().isVisible()) {
        disposeFormInternal();
        return this;
      }
      initForm();
      // check if form was made invisible ( = access control denied access)
      if (!getRootGroupBox().isVisible()) {
        // make sure the form is storing since it is not showing
        disposeFormInternal();
        return this;
      }
      loadStateInternal();
      // check if form was made invisible ( = access control denied access)
      if (!isFormOpen()) {
        disposeFormInternal();
        return this;
      }
      if (!getRootGroupBox().isVisible()) {
        disposeFormInternal();
        return this;
      }
      if (getHandler().isGuiLess()) {
        // make sure the form is storing since it is not showing
        storeStateInternal();
        markSaved();
        doFinally();
        disposeFormInternal();
        return this;
      }
    }
    catch (ProcessingException e) {
      e.addContextMessage(AbstractForm.this.getClass().getSimpleName());
      disposeFormInternal();
      if (e instanceof VetoException) {
        interceptOnVetoException((VetoException) e, e.getStatus().getCode());
      }
      else {
        throw e;
      }
    }
    catch (Throwable t) {
      disposeFormInternal();
      throw new ProcessingException("failed showing " + getTitle(), t);
    }
    // request a gui
    setButtonsArmed(true);
    setCloseTimerArmed(true);
    // register in desktop or wizard (legacy wizard only)
    if (isAutoAddRemoveOnDesktop()) {
      IDesktop desktop = getDesktop();
      if (desktop != null) {
        desktop.addForm(this);
      }
    }
    return this;
  }

  @Override
  public void startWizardStep(IWizardStep wizardStep, Class<? extends IFormHandler> handlerType) throws ProcessingException {
    setAutoAddRemoveOnDesktop(false);
    IFormHandler formHandler = null;
    if (handlerType != null) {
      try {
        formHandler = ConfigurationUtility.newInnerInstance(this, handlerType);
      }
      catch (Exception e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("error creating instance of class '" + handlerType.getName() + "'.", e));
      }
    }
    m_wizardStep = wizardStep;
    setModal(false);
    setAskIfNeedSave(false);
    // hide top level process buttons with a system type
    for (IFormField f : getRootGroupBox().getFields()) {
      if (f instanceof IButton) {
        IButton b = (IButton) f;
        if (b.getSystemType() != IButton.SYSTEM_TYPE_NONE) {
          // hide
          b.setVisible(false);
          b.setVisibleGranted(false);
        }
      }
    }
    //
    // start
    startInternal(formHandler);
  }

  @Override
  public void waitFor() throws ProcessingException {
    // check if the desktop is observing this process
    IDesktop desktop = ClientSyncJob.getCurrentSession().getDesktop();
    if (desktop == null || !desktop.isOpened()) {
      throw new ProcessingException("Cannot wait for " + getClass().getName() + ". There is no desktop or the desktop has not yet been opened in the ui", null, WAIT_FOR_ERROR_CODE);
    }
    // wait
    try {
      m_blockingCondition.waitFor();
    }
    catch (InterruptedException e) {
      throw new ProcessingException(ScoutTexts.get("UserInterrupted"), e);
    }
  }

  private void exportExtensionProperties(Object o, IPropertyHolder target) throws ProcessingException {
    if (!(o instanceof IExtensibleObject)) {
      return;
    }
    for (IExtension<?> ex : ((IExtensibleObject) o).getAllExtensions()) {
      Class<?> dto = FormDataUtility.getDataAnnotationValue(ex.getClass());
      if (dto != null && !Object.class.equals(dto)) {
        Object propertyTarget = target.getContribution(dto);
        Map<String, Object> fieldProperties = BeanUtility.getProperties(ex, AbstractFormField.class, new FormDataPropertyFilter());
        BeanUtility.setProperties(propertyTarget, fieldProperties, false, null);
      }
    }
  }

  @Override
  public void exportFormData(final AbstractFormData target) throws ProcessingException {
    // locally declared form properties
    Map<String, Object> properties = BeanUtility.getProperties(this, AbstractForm.class, new FormDataPropertyFilter());
    BeanUtility.setProperties(target, properties, false, null);
    // properties in extensions of form
    exportExtensionProperties(this, target);
    final Set<IFormField> exportedFields = new HashSet<IFormField>();

    // all fields
    Map<Integer, Map<String/* qualified field id */, AbstractFormFieldData>> breadthFirstMap = target.getAllFieldsRec();
    for (Map<String/* qualified field id */, AbstractFormFieldData> targetMap : breadthFirstMap.values()) {
      for (Map.Entry<String, AbstractFormFieldData> e : targetMap.entrySet()) {
        String fieldQId = e.getKey();
        AbstractFormFieldData data = e.getValue();

        FindFieldByFormDataIdVisitor v = new FindFieldByFormDataIdVisitor(fieldQId, this);
        visitFields(v);
        IFormField f = v.getField();
        if (f != null) {
          // field properties
          properties = BeanUtility.getProperties(f, AbstractFormField.class, new FormDataPropertyFilter());
          BeanUtility.setProperties(data, properties, false, null);
          exportExtensionProperties(f, data);

          // field state
          f.exportFormFieldData(data);

          // remember exported fields
          exportedFields.add(f);
        }
        else {
          LOG.warn("Cannot find field with id '" + fieldQId + "' in form '" + getClass().getName() + "' for DTO '" + data.getClass().getName() + "'.");
        }
      }
    }

    // visit remaining fields (there could be an extension with properties e.g. on a groupbox)
    final Holder<ProcessingException> exHolder = new Holder<ProcessingException>(ProcessingException.class);
    visitFields(new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (exportedFields.contains(field)) {
          // already exported -> skip
          return true;
        }

        try {
          exportExtensionProperties(field, target);
        }
        catch (ProcessingException e) {
          exHolder.setValue(e);
        }
        return exHolder.getValue() == null;
      }
    });
    if (exHolder.getValue() != null) {
      throw exHolder.getValue();
    }
  }

  @Override
  public void importFormData(AbstractFormData source) throws ProcessingException {
    importFormData(source, false, null);
  }

  @Override
  public void importFormData(AbstractFormData source, boolean valueChangeTriggersEnabled) throws ProcessingException {
    importFormData(source, valueChangeTriggersEnabled, null);
  }

  @Override
  public void importFormData(AbstractFormData source, boolean valueChangeTriggersEnabled, IPropertyFilter filter) throws ProcessingException {
    importFormData(source, valueChangeTriggersEnabled, filter, null);
  }

  private void removeNotSetProperties(IPropertyHolder dto, Map<String, Object> properties) {
    for (Iterator<String> it = properties.keySet().iterator(); it.hasNext();) {
      String propertyId = it.next();
      AbstractPropertyData pd = dto.getPropertyById(propertyId);
      if (pd != null && !pd.isValueSet()) {
        it.remove();
      }
    }
  }

  private void importProperties(IPropertyHolder source, Object target, Class<?> stopClass, IPropertyFilter filter) throws ProcessingException {
    // local properties
    Map<String, Object> properties = BeanUtility.getProperties(source, stopClass, filter);
    if (!properties.isEmpty()) {
      removeNotSetProperties(source, properties);
      BeanUtility.setProperties(target, properties, false, null);
    }

    // properties of the extensions
    List<Object> allContributions = source.getAllContributions();
    if (!allContributions.isEmpty()) {
      for (Object con : allContributions) {
        if (con instanceof IPropertyHolder) {
          IPropertyHolder data = (IPropertyHolder) con;
          Map<String, Object> extensionProperties = BeanUtility.getProperties(data, stopClass, filter);
          if (!extensionProperties.isEmpty()) {
            Object clientPart = getClientPartOfExtensionOrContributionRec(data, target);
            if (clientPart != null) {
              removeNotSetProperties(data, extensionProperties);
              BeanUtility.setProperties(clientPart, extensionProperties, false, null);
            }
            else {
              LOG.warn("cannot find extension for property data '" + data.getClass().getName() + "' in form '" + this.getClass().getName() + "'.");
            }
          }
        }
      }
    }
  }

  private Object getClientPartOfExtensionOrContribution(Object extToSearch, Object owner) {
    if (owner instanceof IExtensibleObject) {
      IExtensibleObject exOwner = (IExtensibleObject) owner;
      for (IExtension<?> ex : exOwner.getAllExtensions()) {
        Class<?> dto = FormDataUtility.getDataAnnotationValue(ex.getClass());
        if (extToSearch.getClass().equals(dto)) {
          return ex;
        }
      }
    }
    if (owner instanceof IContributionOwner) {
      IContributionOwner compOwner = (IContributionOwner) owner;
      for (Object o : compOwner.getAllContributions()) {
        FormData annotation = o.getClass().getAnnotation(FormData.class);
        if (annotation != null && annotation.value().equals(extToSearch.getClass())) {
          return o;
        }
      }
    }
    return null;
  }

  private Object getClientPartOfExtensionOrContributionRec(final Object extToSearch, Object owner) {
    Object ext = getClientPartOfExtensionOrContribution(extToSearch, owner);
    if (ext != null) {
      return ext;
    }

    // search for the extension in the children
    final IHolder<Object> result = new Holder<Object>(Object.class);
    IFormFieldVisitor visitor = new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        result.setValue(getClientPartOfExtensionOrContribution(extToSearch, field));
        return result.getValue() == null;
      }
    };
    if (owner instanceof IForm) {
      ((IForm) owner).visitFields(visitor);
    }
    else if (owner instanceof ICompositeField) {
      ((ICompositeField) owner).visitFields(visitor, 0);
    }
    else if (owner instanceof IWrappedFormField) {
      ((IWrappedFormField) owner).visitFields(visitor, 0);
    }
    return result.getValue();
  }

  private static Class<?> getFieldStopClass(Object data) {
    if (data instanceof AbstractTableFieldData) {
      return AbstractTableFieldData.class;
    }
    else if (data instanceof AbstractValueFieldData) {
      return AbstractValueFieldData.class;
    }
    else {
      return AbstractFormFieldData.class;
    }
  }

  @Override
  public void importFormData(AbstractFormData source, boolean valueChangeTriggersEnabled, IPropertyFilter filter, IFormFieldFilter formFieldFilter) throws ProcessingException {
    if (filter == null) {
      filter = new FormDataPropertyFilter();
    }

    // form properties
    importProperties(source, this, AbstractFormData.class, filter);

    // sort fields, first non-slave fields, then slave fields in transitive order
    LinkedList<IFormField> masterList = new LinkedList<IFormField>();
    LinkedList<IFormField> slaveList = new LinkedList<IFormField>();
    HashMap<IFormField, AbstractFormFieldData> dataMap = new HashMap<IFormField, AbstractFormFieldData>();

    // collect fields and split them into masters/slaves
    Map<Integer, Map<String/* qualified field id */, AbstractFormFieldData>> breadthFirstMap = source.getAllFieldsRec();
    for (Map<String/* qualified field id */, AbstractFormFieldData> sourceMap : breadthFirstMap.values()) {
      for (Map.Entry<String, AbstractFormFieldData> e : sourceMap.entrySet()) {
        String fieldQId = e.getKey();
        AbstractFormFieldData data = e.getValue();
        FindFieldByFormDataIdVisitor v = new FindFieldByFormDataIdVisitor(fieldQId, this);
        visitFields(v);
        IFormField f = v.getField();
        if (f != null) {
          if (formFieldFilter == null || formFieldFilter.accept(f)) {
            dataMap.put(f, data);
            if (f.getMasterField() != null) {
              int index = slaveList.indexOf(f.getMasterField());
              if (index >= 0) {
                slaveList.add(index + 1, f);
              }
              else {
                slaveList.addFirst(f);
              }
            }
            else {
              masterList.add(f);
            }
          }
        }
        else {
          LOG.warn("cannot find field data for '" + fieldQId + "' in form '" + getClass().getName() + "'.");
        }
      }
    }
    for (IFormField f : masterList) {
      importFormField(f, dataMap, valueChangeTriggersEnabled, filter);
    }
    for (IFormField f : slaveList) {
      importFormField(f, dataMap, valueChangeTriggersEnabled, filter);
    }
  }

  private void importFormField(IFormField f, Map<IFormField, AbstractFormFieldData> dataMap, boolean valueChangeTriggersEnabled, IPropertyFilter filter) throws ProcessingException {
    AbstractFormFieldData data = dataMap.get(f);
    // form field properties
    importProperties(data, f, getFieldStopClass(data), filter);
    // field state
    f.importFormFieldData(data, valueChangeTriggersEnabled);
  }

  public static String parseFormId(String className) {
    String s = className;
    int i = Math.max(s.lastIndexOf('$'), s.lastIndexOf('.'));
    s = s.substring(i + 1);
    return s;
  }

  @Override
  public String getFormId() {
    return parseFormId(getClass().getName());
  }

  /**
   * <p>
   * <li>If a classId was set with {@link #setClassId(String)} this value is returned.
   * <li>Else if the class is annotated with {@link ClassId}, the annotation value is returned.
   * <li>Otherwise the class name is returned.
   */
  @Override
  public String classId() {
    if (m_classId != null) {
      return m_classId;
    }
    String simpleClassId = ConfigurationUtility.getAnnotatedClassIdWithFallback(getClass());
    if (getOuterFormField() != null) {
      return simpleClassId + ID_CONCAT_SYMBOL + getOuterFormField().classId();
    }
    return simpleClassId;
  }

  @Override
  public void setClassId(String classId) {
    m_classId = classId;
  }

  @Override
  public IFormHandler getHandler() {
    return m_handler;
  }

  @Override
  public void setHandler(IFormHandler handler) {
    if (handler != m_handler) {
      if (m_handler != null) {
        m_handler.setFormInternal(null);
      }
      if (handler == null) {
        handler = new NullFormHandler();
      }
      m_handler = handler;
      m_handler.setFormInternal(this);
    }
  }

  @Override
  public IWizard getWizard() {
    return (getWizardStep() != null) ? getWizardStep().getWizard() : null;
  }

  @Override
  public IWizardStep getWizardStep() {
    return m_wizardStep;
  }

  @Override
  public IFormField getFocusOwner() {
    if (getDesktop() != null) {
      IFormField field = getDesktop().getFocusOwner();
      if (field != null) {
        IForm form = field.getForm();
        while (form != null) {
          if (form == this) {
            return field;
          }
          // next
          form = form.getOuterForm();
        }
      }
    }
    return null;
  }

  @Override
  public List<IFormField> getAllFields() {
    P_AbstractCollectingFieldVisitor<IFormField> v = new P_AbstractCollectingFieldVisitor<IFormField>() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        collect(field);
        return true;
      }
    };
    visitFields(v);
    return v.getCollection();
  }

  @Override
  public boolean visitFields(IFormFieldVisitor visitor) {
    return getRootGroupBox().visitFields(visitor, 0);
  }

  /**
   * Convenience for ClientJob.getCurrentSession().getDesktop()
   */
  public IDesktop getDesktop() {
    return ClientSyncJob.getCurrentSession().getDesktop();
  }

  @Override
  public final SearchFilter getSearchFilter() {
    if (m_searchFilter == null) {
      resetSearchFilter();
    }
    return m_searchFilter;
  }

  @Override
  public final void setSearchFilter(SearchFilter searchFilter) {
    m_searchFilter = searchFilter;
  }

  /**
   * Alias for {@link #resetSearchFilter()}
   */
  public void rebuildSearchFilter() {
    resetSearchFilter();
  }

  @Override
  public void resetSearchFilter() {
    if (m_searchFilter == null) {
      SearchFilter filter;
      ISearchFilterService sfs = SERVICES.getService(ISearchFilterService.class);
      if (sfs != null) {
        filter = sfs.createNewSearchFilter();
      }
      else {
        filter = new SearchFilter();
      }
      m_searchFilter = filter;
    }
    try {
      interceptResetSearchFilter(m_searchFilter);
    }
    catch (ProcessingException e) {
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
    catch (Throwable t) {
      SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected", t));
    }
  }

  /**
   * Called when saving the form via {@link #resetSearchFilter()}.
   * <p>
   * This operation fills up the search filter and subclass override sets the formData property of the
   * {@link SearchFilter#setFormData(AbstractFormData)} and adds verbose texts with
   * {@link SearchFilter#addDisplayText(String)}
   * <p>
   * May call {@link #setSearchFilter(SearchFilter)}
   * <p>
   * Implement {@link #interceptCreateFormData()} to automatically attach a filled form data to the search filter (since
   * 3.8).
   *
   * @param searchFilter
   *          is never null
   */
  @ConfigOperation
  @Order(10)
  protected void execResetSearchFilter(final SearchFilter searchFilter) throws ProcessingException {
    searchFilter.clear();
    // add verbose field texts
    // do not use visitor, so children can block traversal on whole subtrees
    getRootGroupBox().applySearch(searchFilter);
    // add verbose form texts
    interceptAddSearchTerms(searchFilter);
    // override may add form data
    try {
      AbstractFormData data = interceptCreateFormData();
      if (data != null) {
        exportFormData(data);
        getSearchFilter().setFormData(data);
      }
    }
    catch (ProcessingException e) {
      throw e;
    }
    catch (Throwable t) {
      throw new ProcessingException("Create form data", t);
    }
  }

  /**
   * Convenience to add form data to search filter and load/store form data.
   * override this method to create an empty search form data that can be used for example by
   * {@link #interceptResetSearchFilter(SearchFilter)}
   *
   * @since 3.8
   */
  @ConfigOperation
  @Order(11)
  protected AbstractFormData execCreateFormData() throws ProcessingException {
    return null;
  }

  @Override
  public boolean isFormStored() {
    return m_formStored;
  }

  @Override
  public void setFormStored(boolean b) {
    m_formStored = b;
  }

  @Override
  public boolean isFormLoading() {
    return m_formLoading;
  }

  private void setFormLoading(boolean b) {
    m_formLoading = b;
  }

  /**
   * Mainbox getter
   */
  @Override
  public IGroupBox getRootGroupBox() {
    return m_mainBox;
  }

  @Override
  public IForm getOuterForm() {
    return m_wrappedFormField != null ? m_wrappedFormField.getForm() : null;
  }

  @Override
  public IWrappedFormField getOuterFormField() {
    return m_wrappedFormField;
  }

  @Override
  public void setWrapperFieldInternal(IWrappedFormField w) {
    m_wrappedFormField = w;
  }

  @Override
  public IFormField getFieldById(final String id) {
    return getRootGroupBox().getFieldById(id);
  }

  @Override
  public <T extends IFormField> T getFieldById(String id, Class<T> type) {
    return getRootGroupBox().getFieldById(id, type);
  }

  @Override
  public <T extends IFormField> T getFieldByClass(Class<T> c) {
    return getRootGroupBox().getFieldByClass(c);
  }

  /**
   * override in subclasses to perform form initialization before handler starts
   */
  protected void postInitConfig() throws ProcessingException {
    FormUtility.postInitConfig(this);
    FormUtility.rebuildFieldGrid(this, true);
  }

  @Override
  public void initForm() throws ProcessingException {
    // form
    initFormInternal();
    // fields
    FormUtility.initFormFields(this);
    ActionUtility.initActions(getToolButtons());
    // custom
    interceptInitForm();
  }

  protected void initFormInternal() throws ProcessingException {
  }

  @Override
  public int getCloseSystemType() {
    return m_closeType;
  }

  @Override
  public void setCloseSystemType(int type) {
    m_closeType = type;
  }

  /**
   * do not use or override this internal method
   */
  protected void loadStateInternal() throws ProcessingException {
    fireFormLoadBefore();
    if (!isFormOpen()) {
      return;
    }
    getHandler().onLoad();
    if (!isFormOpen()) {
      return;
    }
    fireFormLoadAfter();
    if (!isFormOpen()) {
      return;
    }
    // set all values to 'unchanged'
    markSaved();
    setFormLoading(false);
    getHandler().onPostLoad();
    if (!isFormOpen()) {
      return;
    }
    // if not visible mode, mark form changed
    if (getHandler().isGuiLess()) {
      touch();
    }
    fireFormLoadComplete();
  }

  /**
   * Store state of form, regardless of validity and completeness do not use or
   * override this internal method directly
   */
  protected void storeStateInternal() throws ProcessingException {
    if (!m_blockingCondition.isBlocking()) {
      String msg = TEXTS.get("FormDisposedMessage", getTitle());
      LOG.error(msg);
      throw new VetoException(msg);
    }
    fireFormStoreBefore();
    m_formStored = true;
    try {
      rebuildSearchFilter();
      m_searchFilter.setCompleted(true);
      getHandler().onStore();
      interceptStored();
      if (!m_formStored) {
        //the form was marked as not stored in AbstractFormHandler#execStore() or AbstractForm#execStored().
        ProcessingException e = new ProcessingException("Form was marked as not stored.");
        e.consume();
        throw e;
      }
    }
    catch (ProcessingException e) {
      // clear search
      if (m_searchFilter != null) {
        m_searchFilter.clear();
      }
      // store was not successfully stored
      m_formStored = false;
      throwVetoExceptionInternal(e);
      // if exception was caught and suppressed, this form was after all successfully stored
      // normally this code is not reached since the exception will  be passed out
      m_formStored = true;
    }
    catch (Throwable t) {
      // clear search
      if (m_searchFilter != null) {
        m_searchFilter.clear();
      }
      throw new ProcessingException("form: " + getTitle(), t);
    }
    fireFormStoreAfter();
  }

  /**
   * do not use or override this internal method
   */
  protected void discardStateInternal() throws ProcessingException {
    fireFormDiscarded();
    getHandler().onDiscard();
  }

  @Override
  public void setCloseTimer(int seconds) {
    if (seconds > 0) {
      setCloseTimerArmed(true);
      m_scoutCloseTimer = new P_CloseTimer(seconds);
      m_scoutCloseTimer.start();
    }
    else {
      removeCloseTimer();
    }
  }

  /**
   * do not use or override this internal method
   */
  protected void throwVetoExceptionInternal(ProcessingException e) throws ProcessingException {
    if (e instanceof VetoException) {
      if (!e.isConsumed()) {
        interceptOnVetoException((VetoException) e, e.getStatus().getCode());
        // if it was not re-thrown it is assumed to be consumed
        e.consume();
      }
    }
    throw e;
  }

  @Override
  public void removeCloseTimer() {
    setCloseTimerArmed(false);
    m_scoutCloseTimer = null;
    setSubTitle(null);
  }

  @Override
  public void validateForm() throws ProcessingException {
    m_currentValidateContentDescriptor = null;
    if (!interceptCheckFields()) {
      VetoException veto = new VetoException("Validate " + getClass().getSimpleName());
      veto.consume();
      throw veto;
    }
    if (!getHandler().onCheckFields()) {
      VetoException veto = new VetoException("Validate " + getClass().getSimpleName());
      veto.consume();
      throw veto;
    }
    // check all fields that might be invalid
    final ArrayList<String> invalidTexts = new ArrayList<String>();
    final ArrayList<String> mandatoryTexts = new ArrayList<String>();
    P_AbstractCollectingFieldVisitor<IValidateContentDescriptor> v = new P_AbstractCollectingFieldVisitor<IValidateContentDescriptor>() {
      @Override
      public boolean visitField(IFormField f, int level, int fieldIndex) {
        IValidateContentDescriptor desc = f.validateContent();
        if (desc != null) {
          if (desc.getErrorStatus() != null) {
            invalidTexts.add(desc.getDisplayText() + ": " + desc.getErrorStatus().getMessage());
          }
          else {
            mandatoryTexts.add(desc.getDisplayText());
          }
          if (getCollectionCount() == 0) {
            collect(desc);
          }
        }
        return true;
      }
    };
    visitFields(v);
    if (v.getCollectionCount() > 0) {
      IValidateContentDescriptor firstProblem = v.getCollection().get(0);
      if (LOG.isInfoEnabled()) {
        LOG.info("there are fields with errors");
      }
      StringBuffer buf = new StringBuffer();
      if (mandatoryTexts.size() > 0) {
        buf.append(ScoutTexts.get("FormEmptyMandatoryFieldsMessage") + "\n\n");
        for (Iterator it = mandatoryTexts.iterator(); it.hasNext();) {
          buf.append("- " + it.next() + "\n");
        }
        buf.append("\n");
      }
      if (invalidTexts.size() > 0) {
        buf.append(ScoutTexts.get("FormInvalidFieldsMessage") + "\n\n");
        for (Iterator it = invalidTexts.iterator(); it.hasNext();) {
          buf.append("- " + it.next() + "\n");
        }
      }
      String introText = ScoutTexts.get("FormIncompleteIntro");
      m_currentValidateContentDescriptor = firstProblem;
      //
      VetoException veto = new VetoException(introText, buf.toString());
      throw veto;
    }
    if (!interceptValidate()) {
      VetoException veto = new VetoException("Validate " + getClass().getSimpleName());
      veto.consume();
      throw veto;
    }
    if (!getHandler().onValidate()) {
      VetoException veto = new VetoException("Validate " + getClass().getSimpleName());
      veto.consume();
      throw veto;
    }
  }

  /**
   * attach a statement (mode) that is executed every interval
   *
   * @since Build 195 09.02.2005, imo
   */
  @Override
  public void setTimer(String timerId, int seconds) {
    removeTimer(timerId);
    if (seconds > 0) {
      P_Timer tim = new P_Timer(seconds, timerId);
      m_scoutTimerMap.put(timerId, tim);
      tim.start();
    }
  }

  /**
   * remove a statement (mode) that is executed every interval
   *
   * @since Build 195 09.02.2005, imo
   */
  @Override
  public void removeTimer(String timerId) {
    P_Timer tim = m_scoutTimerMap.remove(timerId);
    if (tim != null) {
      tim.setStopSignal();
    }
  }

  @Override
  public void doClose() throws ProcessingException {
    if (!isFormOpen()) {
      return;
    }
    m_closeType = IButton.SYSTEM_TYPE_CLOSE;
    discardStateInternal();
    doFinally();
    disposeFormInternal();
  }

  @Override
  public void doCancel() throws ProcessingException {
    if (!isFormOpen()) {
      return;
    }
    m_closeType = IButton.SYSTEM_TYPE_CANCEL;
    try {
      // ensure all fields have the right save-needed-state
      checkSaveNeeded();
      // find any fields that needs save
      P_AbstractCollectingFieldVisitor<IFormField> collector = new P_AbstractCollectingFieldVisitor<IFormField>() {
        @Override
        public boolean visitField(IFormField field, int level, int fieldIndex) {
          if (field.isSaveNeeded()) {
            collect(field);
            return false;
          }
          else {
            return true;
          }
        }
      };
      visitFields(collector);
      if (collector.getCollectionCount() > 0 && isAskIfNeedSave()) {
        MessageBox messageBox = new MessageBox(
            null,
            getCancelVerificationText(),
            null,
            TEXTS.get("YesButton"),
            TEXTS.get("NoButton"),
            TEXTS.get("CancelButton")
            );
        messageBox.setSeverity(IStatus.INFO);
        int result = messageBox.startMessageBox();
        if (result == IMessageBox.YES_OPTION) {
          doOk();
          return;
        }
        else if (result == IMessageBox.NO_OPTION) {
          doClose();
          return;
        }
        else {
          VetoException e = new VetoException(ScoutTexts.get("UserCancelledOperation"));
          e.consume();
          throw e;
        }
      }
      discardStateInternal();
      doFinally();
      disposeFormInternal();
    }
    catch (ProcessingException e) {
      m_closeType = IButton.SYSTEM_TYPE_NONE;
      throw e;
    }
  }

  @Override
  public void doReset() {
    setFormLoading(true);
    // reset values
    P_AbstractCollectingFieldVisitor v = new P_AbstractCollectingFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (field instanceof IValueField) {
          IValueField f = (IValueField) field;
          f.resetValue();
        }
        else if (field instanceof IComposerField) {
          IComposerField f = (IComposerField) field;
          f.resetValue();
        }
        return true;
      }
    };
    visitFields(v);
    try {
      // init again
      initForm();
      // load again
      loadStateInternal();
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormReset") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  /**
   * Save data and close the form.
   * It will make this decision based on {@link #isSaveNeeded}.
   * Saving usually involves calling the <code>execStore</code> method
   * of the current form handler.
   *
   * @see AbstractFormHandler#execStore()
   */
  @Override
  public void doOk() throws ProcessingException {
    if (!isFormOpen()) {
      return;
    }
    try {
      m_closeType = IButton.SYSTEM_TYPE_NONE;
      checkSaveNeeded();
      validateForm();
      m_closeType = IButton.SYSTEM_TYPE_OK;
      if (isSaveNeeded()) {
        storeStateInternal();
        markSaved();
      }
      doFinally();
      disposeFormInternal();
    }
    catch (ProcessingException e) {
      m_closeType = IButton.SYSTEM_TYPE_NONE;
      throwVetoExceptionInternal(e);
    }
  }

  @Override
  public void doSaveWithoutMarkerChange() throws ProcessingException {
    if (!isFormOpen()) {
      return;
    }
    try {
      m_closeType = IButton.SYSTEM_TYPE_NONE;
      // ensure all fields have the right save-needed-state
      checkSaveNeeded();
      validateForm();
      m_closeType = IButton.SYSTEM_TYPE_SAVE_WITHOUT_MARKER_CHANGE;
      storeStateInternal();
      // do not set to "markSaved"
    }
    catch (ProcessingException e) {
      m_closeType = IButton.SYSTEM_TYPE_NONE;
      throwVetoExceptionInternal(e);
    }
  }

  @Override
  public void doSave() throws ProcessingException {
    if (!isFormOpen()) {
      return;
    }
    try {
      m_closeType = IButton.SYSTEM_TYPE_NONE;
      // ensure all fields have the right save-needed-state
      checkSaveNeeded();
      validateForm();
      m_closeType = IButton.SYSTEM_TYPE_SAVE;
      if (isSaveNeeded()) {
        storeStateInternal();
        markSaved();
      }
    }
    catch (ProcessingException e) {
      m_closeType = IButton.SYSTEM_TYPE_NONE;
      throwVetoExceptionInternal(e);
    }
  }

  @Override
  public void setAllEnabled(final boolean b) {
    P_AbstractCollectingFieldVisitor v = new P_AbstractCollectingFieldVisitor() {
      @Override
      public boolean visitField(IFormField f, int level, int fieldIndex) {
        boolean filteredB = b;
        /*
         * @since 3.0 all items are enabled/disabled. a dialog can still be
         * closed using the X in the window header if(f instanceof IButton){
         * IButton b=(IButton)f; if(b.isProcessButton()){
         * switch(b.getSystemType()){ case IButton.SYSTEM_TYPE_CLOSE: case
         * IButton.SYSTEM_TYPE_CANCEL:{ filteredB=true; break; } } } }
         */
        //
        f.setEnabled(filteredB);
        return true;
      }
    };
    visitFields(v);
  }

  @Override
  public void doFinally() {
    try {
      getHandler().onFinally();
    }
    catch (ProcessingException se) {
      se.addContextMessage(ScoutTexts.get("FormFinally") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(se);
    }
    catch (Throwable t) {
      ProcessingException e = new ProcessingException(ScoutTexts.get("FormFinally") + " " + getTitle(), t);
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  @Override
  public String getCancelVerificationText() {
    return m_cancelVerificationText;
  }

  @Override
  public void setCancelVerificationText(String text) {
    m_cancelVerificationText = text;
  }

  @Override
  public List<? extends IFormField> getInvalidFields() {
    // check all fields that might be invalid
    P_AbstractCollectingFieldVisitor<IFormField> v = new P_AbstractCollectingFieldVisitor<IFormField>() {
      @Override
      public boolean visitField(IFormField f, int level, int fieldIndex) {
        if (!f.isContentValid()) {
          collect(f);
        }
        return true;
      }
    };
    visitFields(v);
    return v.getCollection();
  }

  @Override
  public final void checkSaveNeeded() {
    // call checkSaveNeeded on all fields
    P_AbstractCollectingFieldVisitor<IFormField> v = new P_AbstractCollectingFieldVisitor<IFormField>() {
      @Override
      public boolean visitField(IFormField f, int level, int fieldIndex) {
        if (f instanceof IFormField) {
          f.checkSaveNeeded();
        }
        return true;
      }
    };
    visitFields(v);
  }

  private boolean/* ok */checkForVerifyingFields() {
    // check all fields that might be invalid
    P_AbstractCollectingFieldVisitor v = new P_AbstractCollectingFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (field instanceof IValueField) {
          IValueField f = (IValueField) field;
          if (f.isValueChanging() || f.isValueParsing()) {
            return false;
          }
        }
        return true;
      }
    };
    return visitFields(v);
  }

  private void closeFormInternal(boolean kill) {
    if (isFormOpen()) {
      try {
        // check if there is an active close, cancel or finish button
        final HashSet<Integer> enabledSystemTypes = new HashSet<Integer>();
        final HashSet<IButton> enabledSystemButtons = new HashSet<IButton>();
        IFormFieldVisitor v = new IFormFieldVisitor() {
          @Override
          public boolean visitField(IFormField field, int level, int fieldIndex) {
            if (field instanceof IButton) {
              IButton b = (IButton) field;
              if (b.isEnabled() && b.isVisible() && b.isEnabledProcessingButton()) {
                enabledSystemTypes.add(b.getSystemType());
                enabledSystemButtons.add(b);
              }
            }
            return true;
          }
        };
        try {
          visitFields(v);
          for (IButton b : enabledSystemButtons) {
            b.setEnabledProcessingButton(false);
          }
          interceptOnCloseRequest(kill, enabledSystemTypes);
        }
        finally {
          for (IButton b : enabledSystemButtons) {
            b.setEnabledProcessingButton(true);
          }
        }
      }
      catch (ProcessingException se) {
        se.addContextMessage(ScoutTexts.get("FormClosing") + " " + getTitle());
        SERVICES.getService(IExceptionHandlerService.class).handleException(se);
      }
      catch (Throwable t) {
        ProcessingException e = new ProcessingException(ScoutTexts.get("FormClosing") + " " + getTitle(), t);
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
    }
  }

  @Override
  public void touch() {
    getRootGroupBox().touch();
  }

  @Override
  public boolean isSaveNeeded() {
    return getRootGroupBox().isSaveNeeded();
  }

  @Override
  public void markSaved() {
    getRootGroupBox().markSaved();
  }

  @Override
  public boolean isEmpty() {
    return getRootGroupBox().isEmpty();
  }

  /**
   * do not use or override this internal method
   */
  protected void disposeFormInternal() {
    if (!isFormOpen()) {
      return;
    }
    try {
      setButtonsArmed(false);
      setCloseTimerArmed(false);
      //
      m_scoutCloseTimer = null;
      for (Iterator it = m_scoutTimerMap.values().iterator(); it.hasNext();) {
        ((P_Timer) it.next()).setStopSignal();
      }
      m_scoutTimerMap.clear();
      m_formLoading = true;
    }
    catch (Throwable t) {
      LOG.warn("Form " + getClass().getName(), t);
    }
    // dispose fields
    FormUtility.disposeFormFields(this);
    // dispose form configuration
    try {
      interceptDisposeForm();
    }
    catch (Throwable t) {
      LOG.warn("Form " + getClass().getName(), t);
    }
    try {
      // deregister of wizard / desktop
      if (isAutoAddRemoveOnDesktop()) {
        IDesktop desktop = getDesktop();
        if (desktop != null) {
          desktop.removeForm(this);
        }
      }
    }
    finally {
      // unlock
      m_blockingCondition.release();
      // fire
      fireFormClosed();
    }
  }

  @Override
  public boolean isShowing() {
    IDesktop desktop = getDesktop();
    if (desktop != null) {
      return desktop.isShowing(this);
    }
    else {
      return false;
    }
  }

  @Override
  public boolean isFormClosed() {
    return !isFormOpen();
  }

  @Override
  public boolean isFormOpen() {
    return m_blockingCondition.isBlocking();
  }

  @Override
  public Object getProperty(String name) {
    return propertySupport.getProperty(name);
  }

  @Override
  public void setProperty(String name, Object value) {
    propertySupport.setProperty(name, value);
  }

  @Override
  public boolean hasProperty(String name) {
    return propertySupport.hasProperty(name);
  }

  @Override
  public void loadFromXmlString(String xml) throws ProcessingException {
    if (xml == null) {
      return;
    }
    Document xmlDocument = XmlUtility.getXmlDocument(xml);
    loadFromXml(xmlDocument.getDocumentElement());
  }

  @Override
  public String storeToXmlString() throws ProcessingException {
    try {
      Document e = storeToXml();
      return XmlUtility.wellformDocument(e);
    }
    catch (Exception e) {
      if (e instanceof ProcessingException) {
        throw (ProcessingException) e;
      }
      else {
        throw new ProcessingException("form : " + getTitle(), e);
      }
    }
  }

  @Override
  public Document storeToXml() throws ProcessingException {
    Document doc = XmlUtility.createNewXmlDocument("form-state");
    storeToXml(doc.getDocumentElement());
    return doc;
  }

  @Override
  public void storeToXml(Element root) throws ProcessingException {
    root.setAttribute("formId", getFormId());
    root.setAttribute("formQname", getClass().getName());
    // add custom properties
    Element xProps = root.getOwnerDocument().createElement("properties");
    root.appendChild(xProps);
    IPropertyFilter filter = new IPropertyFilter() {
      @Override
      public boolean accept(FastPropertyDescriptor descriptor) {
        if (descriptor.getPropertyType().isInstance(IFormField.class)) {
          return false;
        }
        if (!descriptor.getPropertyType().isPrimitive() && !Serializable.class.isAssignableFrom(descriptor.getPropertyType())) {
          return false;
        }
        if (descriptor.getReadMethod() == null || descriptor.getWriteMethod() == null) {
          return false;
        }
        return true;
      }
    };
    Map<String, Object> props = BeanUtility.getProperties(this, AbstractForm.class, filter);
    for (Entry<String, Object> entry : props.entrySet()) {
      try {
        Element xProp = root.getOwnerDocument().createElement("property");
        xProps.appendChild(xProp);
        xProp.setAttribute("name", entry.getKey());
        XmlUtility.setObjectAttribute(xProp, "value", entry.getValue());
      }
      catch (Exception e) {
        throw new ProcessingException("property " + entry.getKey() + " with value " + entry.getValue(), e);
      }
    }
    // add fields
    final Element xFields = root.getOwnerDocument().createElement("fields");
    root.appendChild(xFields);
    final Holder<ProcessingException> exceptionHolder = new Holder<ProcessingException>(ProcessingException.class);
    P_AbstractCollectingFieldVisitor v = new P_AbstractCollectingFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        Element xField = xFields.getOwnerDocument().createElement("field");
        try {
          field.storeToXml(xField);
          xFields.appendChild(xField);
        }
        catch (ProcessingException e) {
          exceptionHolder.setValue(e);
          return false;
        }
        return true;
      }
    };
    visitFields(v);
    if (exceptionHolder.getValue() != null) {
      throw exceptionHolder.getValue();
    }
  }

  @Override
  public void loadFromXml(Element root) throws ProcessingException {
    String formId = getFormId();
    String xmlId = root.getAttribute("formId");
    if (!formId.equals(xmlId)) {
      throw new ProcessingException("xml id=" + xmlId + " does not match form id=" + formId);
    }

    // load properties
    Map<String, Object> props = new HashMap<String, Object>();
    Element xProps = XmlUtility.getFirstChildElement(root, "properties");
    if (xProps != null) {
      for (Element xProp : XmlUtility.getChildElements(xProps, "property")) {
        String name = xProp.getAttribute("name");
        try {
          Object o = XmlUtility.getObjectAttribute(xProp, "value");
          props.put(name, o);
        }
        catch (Exception e) {
          LOG.warn("property " + name, e);
        }
      }
    }

    BeanUtility.setProperties(this, props, true, null);
    // load fields
    Element xFields = XmlUtility.getFirstChildElement(root, "fields");
    if (xFields != null) {
      for (Element xField : XmlUtility.getChildElements(xFields, "field")) {
        List<String> xmlFieldIds = new LinkedList<String>();
        // add enclosing field path to xml field IDs
        for (Element element : XmlUtility.getChildElements(xField, "enclosingField")) {
          xmlFieldIds.add(element.getAttribute("fieldId"));
        }
        xmlFieldIds.add(xField.getAttribute("fieldId"));
        FindFieldByXmlIdsVisitor v = new FindFieldByXmlIdsVisitor(xmlFieldIds.toArray(new String[xmlFieldIds.size()]));
        visitFields(v);
        IFormField f = v.getField();
        if (f != null) {
          f.loadFromXml(xField);
        }
      }
    }
    // in all tabboxes select the first tab that contains data, iff the current
    // tab has no values set
    getRootGroupBox().visitFields(new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (field instanceof ITabBox) {
          ITabBox tabBox = (ITabBox) field;
          IGroupBox selbox = tabBox.getSelectedTab();
          if (selbox == null || !selbox.isSaveNeeded()) {
            for (IGroupBox g : tabBox.getGroupBoxes()) {
              if (g.isSaveNeeded()) {
                tabBox.setSelectedTab(g);
                break;
              }
            }
          }
        }
        return true;
      }
    }, 0);
  }

  @Override
  public void doExportXml(boolean saveAs) {
    while (true) {
      File path = m_lastXmlFileForStorage;
      if (saveAs || path == null) {
        File dir = m_lastXmlFileForStorage;
        if (dir != null) {
          dir = dir.getParentFile();
        }
        List<File> a = new FileChooser(dir, Collections.singletonList("xml"), false).startChooser();
        if (a.isEmpty()) {
          break;
        }
        else {
          path = a.get(0);
        }
      }

      // export search parameters
      try (Writer w = new OutputStreamWriter(new FileOutputStream(path), "UTF-8")) {
        XmlUtility.wellformDocument(storeToXml(), w);
        if (path != null) {
          m_lastXmlFileForStorage = path;
        }
        break;
      }
      catch (Exception t) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException(ScoutTexts.get("FormExportXml") + " " + getTitle(), t));
        saveAs = true;
      }
    }// end while nok
  }

  @Override
  public void doImportXml() {
    File dir = m_lastXmlFileForStorage;
    if (dir != null) {
      dir = dir.getParentFile();
    }
    List<File> a = new FileChooser(dir, Collections.singletonList("xml"), true).startChooser();
    if (a.size() == 1) {
      File newPath = a.get(0);
      try (InputStream in = new FileInputStream(newPath)) {
        Document doc = XmlUtility.getXmlDocument(in);
        // load xml to search
        m_lastXmlFileForStorage = newPath;
        loadFromXml(doc.getDocumentElement());
      }
      catch (Exception e) {
        LOG.warn("loading: " + newPath + " Exception: " + e);
        new MessageBox(
            null,
            TEXTS.get("LoadFormXmlFailedText"),
            null,
            TEXTS.get("OkButton"),
            null,
            null).startMessageBox();
        m_lastXmlFileForStorage = null;
      }
    }
  }

  @Override
  public void printForm(PrintDevice device, Map<String, Object> parameters) {
    try {
      firePrint(null, device, parameters);
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormPrint") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  @Override
  public void printField(IFormField field, PrintDevice device, Map<String, Object> parameters) {
    try {
      firePrint(field, device, parameters);
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormPrint") + " " + (field != null ? field.getLabel() : getTitle()));
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  @Override
  public void activate() {
    if (getDesktop() != null) {
      getDesktop().ensureVisible(this);
    }
  }

  @Override
  public void requestFocus(IFormField f) {
    if (f == null || f.getForm() != this) {
      return;
    }
    fireRequestFocus(f);
  }

  /**
   * @return Returns a map having old field classes as keys and replacement field classes as values. <code>null</code>
   *         is returned if no form fields are replaced. Do not use this internal method.
   * @since 3.8.2
   */
  public Map<Class<?>, Class<? extends IFormField>> getFormFieldReplacementsInternal() {
    return m_fieldReplacements;
  }

  /**
   * Registers the given form field replacements on this form. Do not use this internal method.
   *
   * @param replacements
   *          Map having old field classes as key and replacing field classes as values.
   * @since 3.8.2
   */
  public void registerFormFieldReplacementsInternal(Map<Class<?>, Class<? extends IFormField>> replacements) {
    if (replacements == null || replacements.isEmpty()) {
      return;
    }
    if (m_fieldReplacements == null) {
      m_fieldReplacements = new HashMap<Class<?>, Class<? extends IFormField>>();
    }
    m_fieldReplacements.putAll(replacements);
  }

  /**
   * Model Observer.
   */
  @Override
  public void addFormListener(FormListener listener) {
    m_listenerList.add(FormListener.class, listener);
  }

  @Override
  public void removeFormListener(FormListener listener) {
    m_listenerList.remove(FormListener.class, listener);
  }

  protected IEventHistory<FormEvent> createEventHistory() {
    return new DefaultFormEventHistory(5000L);
  }

  @Override
  public IEventHistory<FormEvent> getEventHistory() {
    return m_eventHistory;
  }

  private void fireFormLoadBefore() throws ProcessingException {
    fireFormEvent(new FormEvent(this, FormEvent.TYPE_LOAD_BEFORE));
  }

  private void fireFormLoadAfter() throws ProcessingException {
    fireFormEvent(new FormEvent(this, FormEvent.TYPE_LOAD_AFTER));
  }

  private void fireFormLoadComplete() throws ProcessingException {
    fireFormEvent(new FormEvent(this, FormEvent.TYPE_LOAD_COMPLETE));
  }

  private void fireFormStoreBefore() throws ProcessingException {
    fireFormEvent(new FormEvent(this, FormEvent.TYPE_STORE_BEFORE));
  }

  private void fireFormDiscarded() {
    try {
      fireFormEvent(new FormEvent(this, FormEvent.TYPE_DISCARDED));
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormFireDiscarded") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  private void fireFormStoreAfter() throws ProcessingException {
    fireFormEvent(new FormEvent(this, FormEvent.TYPE_STORE_AFTER));
  }

  private void firePrint(IFormField root, PrintDevice device, Map<String, Object> parameters) throws ProcessingException {
    fireFormEvent(new FormEvent(this, FormEvent.TYPE_PRINT, root, device, parameters));
  }

  private void fireFormPrinted(File outputFile) {
    try {
      fireFormEvent(new FormEvent(this, FormEvent.TYPE_PRINTED, outputFile));
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormFirePrinted") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  /**
   * send request that form was activated by gui
   */
  private void fireFormActivated() {
    try {
      interceptFormActivated();
      fireFormEvent(new FormEvent(this, FormEvent.TYPE_ACTIVATED));

    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormFireActivated") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  /**
   * send request that form was closed by gui
   */
  private void fireFormClosed() {
    try {
      fireFormEvent(new FormEvent(this, FormEvent.TYPE_CLOSED));
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormFireClosed") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  protected void fireFormEvent(FormEvent e) throws ProcessingException {
    EventListener[] listeners = m_listenerList.getListeners(FormListener.class);
    if (listeners != null && listeners.length > 0) {
      ProcessingException pe = null;
      for (int i = 0; i < listeners.length; i++) {
        try {
          ((FormListener) listeners[i]).formChanged(e);
        }
        catch (ProcessingException ex) {
          if (pe == null) {
            pe = ex;
          }
        }
        catch (Throwable t) {
          if (pe == null) {
            pe = new ProcessingException("Unexpected", t);
          }
        }
      }
      if (pe != null) {
        throw pe;
      }
    }
    IEventHistory<FormEvent> h = getEventHistory();
    if (h != null) {
      h.notifyEvent(e);
    }
  }

  @Override
  public void structureChanged(IFormField causingField) {
    fireFormStructureChanged(causingField);
  }

  private void fireFormStructureChanged(IFormField causingField) {
    try {
      fireFormEvent(new FormEvent(this, FormEvent.TYPE_STRUCTURE_CHANGED, causingField));
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormFireStructureChanged") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  private void fireFormToFront() {
    try {
      fireFormEvent(new FormEvent(this, FormEvent.TYPE_TO_FRONT));
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormFireToFront") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  private void fireFormToBack() {
    try {
      fireFormEvent(new FormEvent(this, FormEvent.TYPE_TO_BACK));
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormFireToBack") + " " + getTitle());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  private void fireRequestFocus(IFormField f) {
    try {
      fireFormEvent(new FormEvent(this, FormEvent.TYPE_REQUEST_FOCUS, f));
    }
    catch (ProcessingException e) {
      e.addContextMessage(ScoutTexts.get("FormFireRequestFocus") + " " + getTitle() + " for " + f.getLabel());
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  @Override
  public String getTitle() {
    return propertySupport.getPropertyString(PROP_TITLE);
  }

  @Override
  public void setTitle(String title) {
    propertySupport.setPropertyString(PROP_TITLE, title);
  }

  @Override
  public String getSubTitle() {
    return propertySupport.getPropertyString(PROP_SUB_TITLE);
  }

  @Override
  public void setSubTitle(String subTitle) {
    propertySupport.setPropertyString(PROP_SUB_TITLE, subTitle);
  }

  @Override
  public boolean isMaximizeEnabled() {
    return propertySupport.getPropertyBool(PROP_MAXIMIZE_ENABLED);
  }

  @Override
  public void setMaximizeEnabled(boolean b) {
    propertySupport.setPropertyBool(PROP_MAXIMIZE_ENABLED, b);
  }

  @Override
  public boolean isMinimizeEnabled() {
    return propertySupport.getPropertyBool(PROP_MINIMIZE_ENABLED);
  }

  @Override
  public void setMinimizeEnabled(boolean b) {
    propertySupport.setPropertyBool(PROP_MINIMIZE_ENABLED, b);
  }

  @Override
  public boolean isMaximized() {
    return propertySupport.getPropertyBool(PROP_MAXIMIZED);
  }

  @Override
  public void setMaximized(boolean b) {
    if (isMaximizeEnabled()) {
      if (b) {
        propertySupport.setPropertyBool(PROP_MINIMIZED, false);
      }
      // maximized state of ui could be out of sync, fire always
      propertySupport.setPropertyAlwaysFire(PROP_MAXIMIZED, b);
    }
  }

  @Override
  public boolean isMinimized() {
    return propertySupport.getPropertyBool(PROP_MINIMIZED);
  }

  @Override
  public void setMinimized(boolean b) {
    if (isMinimizeEnabled()) {
      if (b) {
        propertySupport.setPropertyBool(PROP_MAXIMIZED, false);
      }
      // minimized state of ui could be out of sync, fire always
      propertySupport.setPropertyAlwaysFire(PROP_MINIMIZED, b);

    }
  }

  @Override
  public boolean isAutoAddRemoveOnDesktop() {
    return m_autoRegisterInDesktopOnStart;
  }

  @Override
  public void setAutoAddRemoveOnDesktop(boolean b) {
    m_autoRegisterInDesktopOnStart = b;
  }

  @Override
  public boolean isModal() {
    return m_modal;
  }

  @Override
  public void setModal(boolean b) {
    if (b) {
      switch (getDisplayHint()) {
        case DISPLAY_HINT_POPUP_DIALOG:
        case DISPLAY_HINT_DIALOG: {
          m_modal = b;
          break;
        }
        default: {
          LOG.warn("cannot set property 'modal' to true with current display hint type");
        }
      }
    }
    else {
      m_modal = b;
    }
  }

  @Override
  public void setCacheBounds(boolean cacheBounds) {
    m_cacheBounds = cacheBounds;
  }

  @Override
  public boolean isCacheBounds() {
    return m_cacheBounds;
  }

  @Override
  public String computeCacheBoundsKey() {
    return getClass().getName();
  }

  @Override
  public int getDisplayHint() {
    return m_displayHint;
  }

  @Override
  public void setDisplayHint(int i) {
    switch (i) {
      case DISPLAY_HINT_DIALOG: {
        m_displayHint = i;
        break;
      }
      case DISPLAY_HINT_POPUP_WINDOW: {
        m_displayHint = i;
        setModal(false);
        break;
      }
      case DISPLAY_HINT_POPUP_DIALOG: {
        m_displayHint = i;
        break;
      }
      case DISPLAY_HINT_VIEW: {
        m_displayHint = i;
        setModal(false);
        break;
      }
      default: {
        throw new IllegalArgumentException("invalid displayHint " + i);
      }
    }
  }

  @Override
  public String getDisplayViewId() {
    return m_displayViewId;
  }

  @Override
  public void setDisplayViewId(String viewId) {
    m_displayViewId = viewId;
  }

  @Override
  public boolean isAskIfNeedSave() {
    return m_askIfNeedSave;
  }

  @Override
  public void setAskIfNeedSave(boolean b) {
    m_askIfNeedSave = b;
  }

  @Override
  public void toFront() {
    fireFormToFront();
  }

  @Override
  public void toBack() {
    fireFormToBack();
  }

  @Override
  public boolean isButtonsArmed() {
    return m_buttonsArmed;
  }

  @Override
  public void setButtonsArmed(boolean b) {
    m_buttonsArmed = b;
  }

  @Override
  public boolean isCloseTimerArmed() {
    return m_closeTimerArmed;
  }

  @Override
  public void setCloseTimerArmed(boolean b) {
    m_closeTimerArmed = b;
  }

  @Override
  public String toString() {
    return "Form " + getFormId();
  }

  @Override
  public IFormUIFacade getUIFacade() {
    return m_uiFacade;
  }

  protected void handleSystemButtonEventInternal(ButtonEvent e) {
    switch (e.getType()) {
      case ButtonEvent.TYPE_CLICKED: {
        // disable close timer
        setCloseTimerArmed(false);
        if (isButtonsArmed()) {
          if (checkForVerifyingFields()) {
            try {
              IButton src = (IButton) e.getSource();
              switch (src.getSystemType()) {
                case IButton.SYSTEM_TYPE_CANCEL: {
                  doCancel();
                  break;
                }
                case IButton.SYSTEM_TYPE_CLOSE: {
                  doClose();
                  break;
                }
                case IButton.SYSTEM_TYPE_OK: {
                  doOk();
                  break;
                }
                case IButton.SYSTEM_TYPE_RESET: {
                  doReset();
                  break;
                }
                case IButton.SYSTEM_TYPE_SAVE: {
                  doSave();
                  break;
                }
                case IButton.SYSTEM_TYPE_SAVE_WITHOUT_MARKER_CHANGE: {
                  doSaveWithoutMarkerChange();
                  break;
                }
              }
            }
            catch (ProcessingException se) {
              se.addContextMessage(ScoutTexts.get("FormButtonClicked") + " " + getTitle() + "." + e.getButton().getLabel());
              SERVICES.getService(IExceptionHandlerService.class).handleException(se);
            }
            if (m_currentValidateContentDescriptor != null) {
              m_currentValidateContentDescriptor.activateProblemLocation();
              m_currentValidateContentDescriptor = null;
            }
          }// end
        }
        break;
      }
    }
  }

  /**
   * Button controller for ok,cancel, save etc.
   */
  private class P_SystemButtonListener implements ButtonListener {
    @Override
    public void buttonChanged(ButtonEvent e) {
      // auto-detaching
      if (m_systemButtonListener != this) {
        ((IButton) e.getSource()).removeButtonListener(this);
        return;
      }
      handleSystemButtonEventInternal(e);
    }
  }// end private class

  private class P_MainBoxPropertyChangeProxy implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent e) {
      if (IFormField.PROP_SAVE_NEEDED.equals(e.getPropertyName())) {
        propertySupport.firePropertyChange(PROP_SAVE_NEEDED, e.getOldValue(), e.getNewValue());
      }
      else if (IFormField.PROP_EMPTY.equals(e.getPropertyName())) {
        propertySupport.firePropertyChange(PROP_EMPTY, e.getOldValue(), e.getNewValue());
      }
    }
  }

  /**
   * form custom timer
   */
  private class P_Timer extends StoppableThread {
    private final long m_intervalMillis;
    private final String m_timerId;
    private boolean m_execPending = false;// do not execute while exec is pending
    private final IClientSession m_session;

    public P_Timer(long seconds, String timerId) {
      setDaemon(true);
      m_intervalMillis = seconds * 1000L;
      m_timerId = timerId;
      m_session = ClientSyncJob.getCurrentSession();
    }

    @Override
    public void run() {
      long next = ((System.currentTimeMillis() + 999L) / 1000L) * 1000L + m_intervalMillis;
      while (m_scoutTimerMap != null && !isStopSignal()) {
        try {
          sleep(250);
        }
        catch (InterruptedException ex) {
        }
        // active?
        if ((!m_execPending) && (!isStopSignal())) {
          // next ready?
          if (next < System.currentTimeMillis()) {
            m_execPending = true;
            new ClientSyncJob("Form timer", m_session) {
              @Override
              protected void runVoid(IProgressMonitor monitor) throws Throwable {
                try {
                  if (LOG.isInfoEnabled()) {
                    LOG.info("timer " + m_timerId);
                  }
                  interceptTimer(m_timerId);
                }
                catch (ProcessingException se) {
                  se.addContextMessage(ScoutTexts.get("FormTimerActivated") + " " + getTitle() + "." + m_timerId);
                  SERVICES.getService(IExceptionHandlerService.class).handleException(se);
                }
                finally {
                  m_execPending = false;// continue scheduling
                }
              }
            }.schedule();
          }
        }
        // update next
        while (next < System.currentTimeMillis()) {
          next = next + m_intervalMillis;
        }
      }
    }
  }// end private class

  /**
   * form close timer
   */
  private class P_CloseTimer extends StoppableThread {
    private long m_seconds;
    private final IClientSession m_session;

    public P_CloseTimer(long seconds) {
      setName("IForm.P_CloseTimer");
      setDaemon(true);
      m_seconds = seconds;
      m_session = ClientSyncJob.getCurrentSession();
    }

    @Override
    public void run() {
      while (this == m_scoutCloseTimer && m_seconds > 0 && isCloseTimerArmed()) {
        new ClientSyncJob("Form close countdown", m_session) {
          @Override
          protected void runVoid(IProgressMonitor monitor) throws Throwable {
            setSubTitle("" + m_seconds);
          }
        }.schedule();
        try {
          sleep(1000);
        }
        catch (InterruptedException ex) {
        }
        m_seconds--;
      }
      if (this == m_scoutCloseTimer) {
        new ClientSyncJob("Form close timer", m_session) {
          @Override
          protected void runVoid(IProgressMonitor monitor) throws Throwable {
            try {
              if (isCloseTimerArmed()) {
                interceptCloseTimer();
              }
              else {
                setSubTitle(null);
              }
            }
            catch (ProcessingException se) {
              se.addContextMessage(ScoutTexts.get("FormCloseTimerActivated") + " " + getTitle());
              SERVICES.getService(IExceptionHandlerService.class).handleException(se);
            }
          }
        }.schedule();
      }
    }
  }// end private class

  private abstract static class P_AbstractCollectingFieldVisitor<T> implements IFormFieldVisitor {
    private final ArrayList<T> m_list = new ArrayList<T>();

    public void collect(T o) {
      m_list.add(o);
    }

    public int getCollectionCount() {
      return m_list.size();
    }

    public List<T> getCollection() {
      return m_list;
    }

  }

  private class P_UIFacade implements IFormUIFacade {
    @Override
    public void fireFormActivatedFromUI() {
      fireFormActivated();
    }

    @Override
    public void fireFormClosingFromUI() {
      // check if some field is verifying input. In this case cancel ui call
      if (!checkForVerifyingFields()) {
        return;
      }
      closeFormInternal(false);
    }

    @Override
    public void fireFormKilledFromUI() {
      closeFormInternal(true);
    }

    @Override
    public void fireFormPrintedFromUI(File outputFile) {
      fireFormPrinted(outputFile);
    }
  }// end private class

  /**
   * The extension delegating to the local methods. This Extension is always at the end of the chain and will not call
   * any further chain elements.
   */
  protected static class LocalFormExtension<FORM extends AbstractForm> extends AbstractExtension<FORM> implements IFormExtension<FORM> {

    /**
     * @param owner
     */
    public LocalFormExtension(FORM owner) {
      super(owner);
    }

    @Override
    public void execCloseTimer(FormCloseTimerChain chain) throws ProcessingException {
      getOwner().execCloseTimer();
    }

    @Override
    public void execInactivityTimer(FormInactivityTimerChain chain) throws ProcessingException {
      getOwner().execInactivityTimer();
    }

    @Override
    public void execStored(FormStoredChain chain) throws ProcessingException {
      getOwner().execStored();
    }

    @Override
    public boolean execCheckFields(FormCheckFieldsChain chain) throws ProcessingException {
      return getOwner().execCheckFields();
    }

    @Override
    public void execResetSearchFilter(FormResetSearchFilterChain chain, SearchFilter searchFilter) throws ProcessingException {
      getOwner().execResetSearchFilter(searchFilter);
    }

    @Override
    public void execAddSearchTerms(FormAddSearchTermsChain chain, SearchFilter search) {
      getOwner().execAddSearchTerms(search);
    }

    @Override
    public void execOnVetoException(FormOnVetoExceptionChain chain, VetoException e, int code) throws ProcessingException {
      getOwner().execOnVetoException(e, code);
    }

    @Override
    public void execFormActivated(FormFormActivatedChain chain) throws ProcessingException {
      getOwner().execFormActivated();
    }

    @Override
    public void execDisposeForm(FormDisposeFormChain chain) throws ProcessingException {
      getOwner().execDisposeForm();
    }

    @Override
    public void execTimer(FormTimerChain chain, String timerId) throws ProcessingException {
      getOwner().execTimer(timerId);
    }

    @Override
    public AbstractFormData execCreateFormData(FormCreateFormDataChain chain) throws ProcessingException {
      return getOwner().execCreateFormData();
    }

    @Override
    public void execInitForm(FormInitFormChain chain) throws ProcessingException {
      getOwner().execInitForm();
    }

    @Override
    public boolean execValidate(FormValidateChain chain) throws ProcessingException {
      return getOwner().execValidate();
    }

    @Override
    public void execOnCloseRequest(FormOnCloseRequestChain chain, boolean kill, HashSet<Integer> enabledButtonSystemTypes) throws ProcessingException {
      getOwner().execOnCloseRequest(kill, enabledButtonSystemTypes);
    }

    @Override
    public void execDataChanged(FormDataChangedChain chain, Object... dataTypes) throws ProcessingException {
      getOwner().execDataChanged(dataTypes);
    }

  }

  protected final void interceptCloseTimer() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormCloseTimerChain chain = new FormCloseTimerChain(extensions);
    chain.execCloseTimer();
  }

  protected final void interceptInactivityTimer() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormInactivityTimerChain chain = new FormInactivityTimerChain(extensions);
    chain.execInactivityTimer();
  }

  protected final void interceptStored() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormStoredChain chain = new FormStoredChain(extensions);
    chain.execStored();
  }

  protected final boolean interceptCheckFields() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormCheckFieldsChain chain = new FormCheckFieldsChain(extensions);
    return chain.execCheckFields();
  }

  protected final void interceptResetSearchFilter(SearchFilter searchFilter) throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormResetSearchFilterChain chain = new FormResetSearchFilterChain(extensions);
    chain.execResetSearchFilter(searchFilter);
  }

  protected final void interceptAddSearchTerms(SearchFilter search) {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormAddSearchTermsChain chain = new FormAddSearchTermsChain(extensions);
    chain.execAddSearchTerms(search);
  }

  protected final void interceptOnVetoException(VetoException e, int code) throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormOnVetoExceptionChain chain = new FormOnVetoExceptionChain(extensions);
    chain.execOnVetoException(e, code);
  }

  protected final void interceptFormActivated() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormFormActivatedChain chain = new FormFormActivatedChain(extensions);
    chain.execFormActivated();
  }

  protected final void interceptDisposeForm() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormDisposeFormChain chain = new FormDisposeFormChain(extensions);
    chain.execDisposeForm();
  }

  protected final void interceptTimer(String timerId) throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormTimerChain chain = new FormTimerChain(extensions);
    chain.execTimer(timerId);
  }

  protected final AbstractFormData interceptCreateFormData() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormCreateFormDataChain chain = new FormCreateFormDataChain(extensions);
    return chain.execCreateFormData();
  }

  protected final void interceptInitForm() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormInitFormChain chain = new FormInitFormChain(extensions);
    chain.execInitForm();
  }

  protected final boolean interceptValidate() throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormValidateChain chain = new FormValidateChain(extensions);
    return chain.execValidate();
  }

  protected final void interceptOnCloseRequest(boolean kill, HashSet<Integer> enabledButtonSystemTypes) throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormOnCloseRequestChain chain = new FormOnCloseRequestChain(extensions);
    chain.execOnCloseRequest(kill, enabledButtonSystemTypes);
  }

  protected final void interceptDataChanged(Object... dataTypes) throws ProcessingException {
    List<? extends IFormExtension<? extends AbstractForm>> extensions = getAllExtensions();
    FormDataChangedChain chain = new FormDataChangedChain(extensions);
    chain.execDataChanged(dataTypes);
  }
}