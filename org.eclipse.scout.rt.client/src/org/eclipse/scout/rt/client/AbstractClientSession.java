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
package org.eclipse.scout.rt.client;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.security.auth.Subject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.security.SimplePrincipal;
import org.eclipse.scout.rt.client.extension.ClientSessionChains.ClientSessionLoadSessionChain;
import org.eclipse.scout.rt.client.extension.ClientSessionChains.ClientSessionStoreSessionChain;
import org.eclipse.scout.rt.client.extension.IClientSessionExtension;
import org.eclipse.scout.rt.client.services.common.clientnotification.ClientNotificationConsumerEvent;
import org.eclipse.scout.rt.client.services.common.clientnotification.IClientNotificationConsumerListener;
import org.eclipse.scout.rt.client.services.common.clientnotification.IClientNotificationConsumerService;
import org.eclipse.scout.rt.client.servicetunnel.http.IClientServiceTunnel;
import org.eclipse.scout.rt.client.ui.DataChangeListener;
import org.eclipse.scout.rt.client.ui.IIconLocator;
import org.eclipse.scout.rt.client.ui.IconLocator;
import org.eclipse.scout.rt.client.ui.desktop.DesktopListener;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.internal.VirtualDesktop;
import org.eclipse.scout.rt.shared.OfflineState;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.TextsThreadLocal;
import org.eclipse.scout.rt.shared.extension.AbstractExtension;
import org.eclipse.scout.rt.shared.extension.IExtensibleObject;
import org.eclipse.scout.rt.shared.extension.IExtension;
import org.eclipse.scout.rt.shared.extension.ObjectExtensions;
import org.eclipse.scout.rt.shared.services.common.context.SharedContextChangedNotification;
import org.eclipse.scout.rt.shared.services.common.context.SharedVariableMap;
import org.eclipse.scout.rt.shared.services.common.prefs.IUserPreferencesStorageService;
import org.eclipse.scout.rt.shared.services.common.security.ILogoutService;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.eclipse.scout.service.SERVICES;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public abstract class AbstractClientSession implements IClientSession, IExtensibleObject {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractClientSession.class);

  // context
  private Bundle m_bundle;
  // state
  private final Object m_stateLock;
  private volatile boolean m_active;
  private volatile boolean m_isStopping;
  private Throwable m_loadError;
  private int m_exitCode = IApplication.EXIT_OK;
  // model
  private IDesktop m_desktop;
  private VirtualDesktop m_virtualDesktop;
  private IClientServiceTunnel m_serviceTunnel;
  private Subject m_offlineSubject;
  private Subject m_subject;
  private final SharedVariableMap m_sharedVariableMap;
  private boolean m_singleThreadSession;
  private String m_virtualSessionId;
  private IMemoryPolicy m_memoryPolicy;
  private IIconLocator m_iconLocator;
  private final Map<String, Object> m_clientSessionData;
  private ScoutTexts m_scoutTexts;
  private Locale m_locale;
  private UserAgent m_userAgent;
  private Vector<ILocaleListener> m_localeListener = new Vector<ILocaleListener>();
  private long m_maxShutdownWaitTime = 4567;
  private final ObjectExtensions<AbstractClientSession, IClientSessionExtension<? extends AbstractClientSession>> m_objectExtensions;

  public AbstractClientSession(boolean autoInitConfig) {
    m_clientSessionData = new HashMap<String, Object>();
    m_stateLock = new Object();
    m_isStopping = false;
    m_sharedVariableMap = new SharedVariableMap();
    m_locale = LocaleThreadLocal.get();
    m_objectExtensions = new ObjectExtensions<AbstractClientSession, IClientSessionExtension<? extends AbstractClientSession>>(this);
    if (autoInitConfig) {
      interceptInitConfig();
    }
  }

  @Override
  public final List<? extends IClientSessionExtension<? extends AbstractClientSession>> getAllExtensions() {
    return m_objectExtensions.getAllExtensions();
  }

  @Override
  public <T extends IExtension<?>> T getExtension(Class<T> c) {
    return m_objectExtensions.getExtension(c);
  }

  protected IClientSessionExtension<? extends AbstractClientSession> createLocalExtension() {
    return new LocalClientSessionExtension<AbstractClientSession>(this);
  }

  protected final void interceptInitConfig() {
    m_objectExtensions.initConfig(createLocalExtension(), new Runnable() {
      @Override
      public void run() {
        initConfig();
      }
    });
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(100)
  protected boolean getConfiguredSingleThreadSession() {
    return false;
  }

  @Override
  public String getUserId() {
    return getSharedContextVariable("userId", String.class);
  }

  /**
   * <p>
   * Returns the {@link ScoutTexts} instance assigned to the type (class) of the current ClientSession.
   * </p>
   * <p>
   * Override this method to set the application specific texts implementation
   * </p>
   */
  @Override
  public ScoutTexts getTexts() {
    return m_scoutTexts;
  }

  @Override
  public final Locale getLocale() {
    return m_locale;
  }

  @Override
  public final void setLocale(Locale locale) {
    if (locale != null) {
      Locale oldLocale = m_locale;
      m_locale = locale;
      if (!locale.equals(LocaleThreadLocal.get())) {
        LocaleThreadLocal.set(locale);
      }
      if (!locale.equals(oldLocale)) {
        notifyLocaleListeners(locale);
      }
    }
  }

  @Override
  public Subject getOfflineSubject() {
    return m_offlineSubject;
  }

  protected void setOfflineSubject(Subject offlineSubject) {
    m_offlineSubject = offlineSubject;
  }

  @Override
  public Bundle getBundle() {
    return m_bundle;
  }

  @Override
  public boolean isActive() {
    return m_active;
  }

  private void setActive(boolean b) {
    synchronized (m_stateLock) {
      m_active = b;
      m_stateLock.notifyAll();
    }
  }

  @Override
  public boolean isLoaded() {
    return m_active;
  }

  @Override
  public Map<String, Object> getSharedVariableMap() {
    return CollectionUtility.copyMap(m_sharedVariableMap);
  }

  /**
   * do not use this internal method directly
   */
  protected <T> T getSharedContextVariable(String name, Class<T> type) {
    Object o = m_sharedVariableMap.get(name);
    return TypeCastUtility.castValue(o, type);
  }

  @Override
  public final Throwable getLoadError() {
    return m_loadError;
  }

  @Override
  public final Object getStateLock() {
    return m_stateLock;
  }

  /*
   * Properties
   */

  protected void initConfig() {
    m_singleThreadSession = getConfiguredSingleThreadSession();
    m_virtualDesktop = new VirtualDesktop();
    setMemoryPolicy(new LargeMemoryPolicy());
    // add client notification listener
    IClientNotificationConsumerService clientNotificationConsumerService = SERVICES.getService(IClientNotificationConsumerService.class);
    if (clientNotificationConsumerService != null) {
      clientNotificationConsumerService.addClientNotificationConsumerListener(this, new IClientNotificationConsumerListener() {
        @Override
        public void handleEvent(final ClientNotificationConsumerEvent e, boolean sync) {
          if (e.getClientNotification().getClass() == SharedContextChangedNotification.class) {
            if (sync) {
              try {
                updateSharedVariableMap(((SharedContextChangedNotification) e.getClientNotification()).getSharedVariableMap());
              }
              catch (Throwable t) {
                LOG.error("update of shared contex", t);
                // nop
              }
            }
            else {
              new ClientSyncJob("Update shared context", ClientSyncJob.getCurrentSession()) {
                @Override
                protected void runVoid(IProgressMonitor monitor) throws Throwable {
                  updateSharedVariableMap(((SharedContextChangedNotification) e.getClientNotification()).getSharedVariableMap());
                }
              }.schedule();
            }
          }
        }
      });
    }
  }

  private void updateSharedVariableMap(SharedVariableMap newMap) {
    m_sharedVariableMap.updateInternal(newMap);
  }

  @Override
  public final void startSession(Bundle bundle) {
    m_bundle = bundle;
    if (isActive()) {
      throw new IllegalStateException("session is active");
    }
    try {
      String policy = bundle.getBundleContext().getProperty("org.eclipse.scout.memory");
      if ("small".equals(policy)) {
        setMemoryPolicy(new SmallMemoryPolicy());
      }
      else if ("medium".equals(policy)) {
        setMemoryPolicy(new MediumMemoryPolicy());
      }
      m_iconLocator = createIconLocator();
      m_scoutTexts = new ScoutTexts();
      // explicitly set the just created instance to the ThreadLocal because it was not available yet, when the job was started.
      TextsThreadLocal.set(m_scoutTexts);
      interceptLoadSession();
      setActive(true);
    }
    catch (Throwable t) {
      m_loadError = t;
      LOG.error("load session", t);
    }
  }

  @ConfigOperation
  @Order(10)
  protected void execLoadSession() throws ProcessingException {
  }

  @ConfigOperation
  @Order(20)
  protected void execStoreSession() throws ProcessingException {
  }

  @Override
  public IDesktop getVirtualDesktop() {
    return m_desktop != null ? m_desktop : m_virtualDesktop;
  }

  @Override
  public IDesktop getDesktop() {
    return m_desktop;
  }

  @Override
  public void setDesktop(IDesktop a) throws ProcessingException {
    if (a == null) {
      throw new IllegalArgumentException("argument must not be null");
    }
    if (m_desktop != null) {
      throw new IllegalStateException("desktop is active");
    }
    m_desktop = a;
    if (m_desktop != null) {
      if (m_virtualDesktop != null) {
        for (DesktopListener listener : m_virtualDesktop.getDesktopListeners()) {
          m_desktop.addDesktopListener(listener);
        }
        for (Map.Entry<String, EventListenerList> e : m_virtualDesktop.getPropertyChangeListenerMap().entrySet()) {
          String propName = e.getKey();
          EventListenerList list = e.getValue();
          if (propName == null) {
            for (PropertyChangeListener listener : list.getListeners(PropertyChangeListener.class)) {
              m_desktop.addPropertyChangeListener(listener);
            }
          }
          else {
            for (PropertyChangeListener listener : list.getListeners(PropertyChangeListener.class)) {
              m_desktop.addPropertyChangeListener(propName, listener);
            }
          }
        }
        for (Map.Entry<Object, EventListenerList> e : m_virtualDesktop.getDataChangeListenerMap().entrySet()) {
          Object dataType = e.getKey();
          EventListenerList list = e.getValue();
          if (dataType == null) {
            for (DataChangeListener listener : list.getListeners(DataChangeListener.class)) {
              m_desktop.addDataChangeListener(listener);
            }
          }
          else {
            for (DataChangeListener listener : list.getListeners(DataChangeListener.class)) {
              m_desktop.addDataChangeListener(listener, dataType);
            }
          }
        }
        m_virtualDesktop = null;
      }
      m_desktop.initDesktop();
    }
    else {
      m_virtualDesktop = new VirtualDesktop();
    }
  }

  /**
   * Close the session
   */
  @Override
  public void stopSession() {
    stopSession(IApplication.EXIT_OK);
  }

  @Override
  public void stopSession(int exitCode) {
    synchronized (m_stateLock) {
      if (isStopping()) {
        // we are already stopping. ignore event
        return;
      }
      m_isStopping = true;
    }
    if (!m_desktop.doBeforeClosingInternal()) {
      m_isStopping = false;
      return;
    }
    m_exitCode = exitCode;
    try {
      interceptStoreSession();
    }
    catch (Throwable t) {
      LOG.error("Failed to store the client session.", t);
    }
    if (m_desktop != null) {
      try {
        m_desktop.closeInternal();
      }
      catch (Throwable t) {
        LOG.error("Failed to close the desktop.", t);
      }
      m_desktop = null;
    }
    if (!m_localeListener.isEmpty()) {
      m_localeListener.clear();
    }

    if (getMaxShutdownWaitTime() > 0) {
      scheduleSessionInactivation();
    }
    else {
      inactivateSession();
    }
  }

  /**
   * Delay the client session inactivation for a maximal period of time until all client jobs of this session have
   * finished. This method does not block the caller.
   */
  protected void scheduleSessionInactivation() {
    new ClientAsyncJob("Wait for client jobs to finish before inactivating the session", this) {
      @Override
      protected IStatus runStatus(IProgressMonitor monitor) {
        final long timeout = getMaxShutdownWaitTime();

        // Wait for the client jobs to finish for a maximal period of time.
        for (ClientJob clientJob : findClientJobs()) {
          try {
            clientJob.join(timeout);
          }
          catch (InterruptedException e) {
            LOG.info(String.format("Interrupted while waiting for the client job to finish. [job=%s]", clientJob), e);
          }
        }

        // Inactivate the client session.
        inactivateSession();

        return Status.OK_STATUS;
      }
    }.schedule();
  }

  /**
   * Finds all client jobs that belong to this client session. If called on behalf of a client job, that job is not
   * returned.
   *
   * @return {@link Set} of client jobs.
   */
  protected Set<ClientJob> findClientJobs() {
    Job currentJob = ClientJob.getJobManager().currentJob();
    Set<ClientJob> clientJobs = new HashSet<ClientJob>();
    for (Job job : Job.getJobManager().find(ClientJob.class)) {
      ClientJob candidateJob = (ClientJob) job;
      if (candidateJob.getClientSession() == AbstractClientSession.this && candidateJob != currentJob) {
        clientJobs.add(candidateJob);
      }
    }
    return clientJobs;
  }

  protected void inactivateSession() {
    Set<ClientJob> runningClientJobs = findClientJobs();
    if (!runningClientJobs.isEmpty()) {
      LOG.warn(""
          + "Some running client jobs found while client session is going to shutdown. "
          + "If waiting for a condition or running a scheduled executor, the associated worker threads may never been released. "
          + "Please ensure to terminate all client jobs when the session is going down. [session={0}, user={1}, jobs={2}]"
          , new Object[]{AbstractClientSession.this, getUserId(), runningClientJobs});
    }

    if (getServiceTunnel() != null) {
      try {
        SERVICES.getService(ILogoutService.class).logout();
      }
      catch (Throwable e) {
        LOG.info("Failed to logout from server.", e);
      }
    }
    setActive(false);
    if (LOG.isInfoEnabled()) {
      LOG.info("Client session was shutdown successfully [session={0}, user={1}]", AbstractClientSession.this, getUserId());
    }
  }

  protected boolean isStopping() {
    return m_isStopping;
  }

  @Override
  public int getExitCode() {
    return m_exitCode;
  }

  @Override
  public IClientServiceTunnel getServiceTunnel() {
    return m_serviceTunnel;
  }

  protected void setServiceTunnel(IClientServiceTunnel tunnel) {
    m_serviceTunnel = tunnel;
  }

  @Override
  public IMemoryPolicy getMemoryPolicy() {
    return m_memoryPolicy;
  }

  @Override
  public void setMemoryPolicy(IMemoryPolicy p) {
    if (m_memoryPolicy != null) {
      m_memoryPolicy.removeNotify();
    }
    m_memoryPolicy = p;
    if (m_memoryPolicy != null) {
      m_memoryPolicy.addNotify();
    }
  }

  public void goOnline() throws ProcessingException {
    if (OfflineState.isOfflineDefault()) {
      OfflineState.setOfflineDefault(false);
    }
  }

  @Override
  public void goOffline() throws ProcessingException {
    Preferences pref = SERVICES.getService(IUserPreferencesStorageService.class).loadPreferences();
    if (getUserId() != null) {
      if (OfflineState.isOnlineDefault()) {
        try {
          pref.put("offline.user", getUserId());
          pref.flush();
          pref.sync();
        }
        catch (BackingStoreException e) {
          LOG.error("Could not write userId to preferences!");
        }
      }
    }
    // create new backend subject
    String offlineUser = pref.get("offline.user", "anonymous");
    m_offlineSubject = new Subject();
    m_offlineSubject.getPrincipals().add(new SimplePrincipal(offlineUser));
    OfflineState.setOfflineDefault(true);
  }

  @Override
  public boolean isSingleThreadSession() {
    return m_singleThreadSession;
  }

  @Override
  public String getVirtualSessionId() {
    return m_virtualSessionId;
  }

  @Override
  public void setVirtualSessionId(String sessionId) {
    m_virtualSessionId = sessionId;
  }

  @Override
  public Subject getSubject() {
    return m_subject;
  }

  @Override
  public void setSubject(Subject subject) {
    m_subject = subject;
  }

  protected IIconLocator createIconLocator() {
    return new IconLocator(this);
  }

  @Override
  public IIconLocator getIconLocator() {
    return m_iconLocator;
  }

  @Override
  public void setData(String key, Object data) {
    if (data == null) {
      m_clientSessionData.remove(key);
    }
    else {
      m_clientSessionData.put(key, data);
    }
  }

  @Override
  public Object getData(String key) {
    return m_clientSessionData.get(key);
  }

  @Override
  public UserAgent getUserAgent() {
    if (m_userAgent == null) {
      LOG.warn("UserAgent has not been initialied correctly. Using default.");
      m_userAgent = UserAgent.createDefault();
    }
    return m_userAgent;
  }

  @Override
  public void setUserAgent(UserAgent userAgent) {
    m_userAgent = userAgent;
  }

  @Override
  public void addLocaleListener(ILocaleListener listener) {
    m_localeListener.add(listener);
  }

  @Override
  public void removeLocaleListener(ILocaleListener listener) {
    m_localeListener.remove(listener);
  }

  protected void notifyLocaleListeners(Locale locale) {
    LocaleChangeEvent event = new LocaleChangeEvent(this, locale);
    Iterator it = ((Vector) m_localeListener.clone()).iterator();
    while (it.hasNext()) {
      ILocaleListener listener = (ILocaleListener) it.next();
      listener.localeChanged(event);
    }
  }

  /**
   * Sets the maximum time (in milliseconds) to wait for each client job to finish when stopping the session before
   * it is set to inactive. When a value &lt;= 0 is set, the session is set to inactive immediately, without
   * waiting for client jobs to finish.
   */
  public void setMaxShutdownWaitTime(long maxShutdownWaitTime) {
    m_maxShutdownWaitTime = maxShutdownWaitTime;
  }

  /**
   * @return the maximum time (in milliseconds) to wait for each client job to finish when stopping the session before
   *         it is set to inactive. The default value is 4567, which should be reasonable for most use cases.
   */
  public long getMaxShutdownWaitTime() {
    return m_maxShutdownWaitTime;
  }

  /**
   * The extension delegating to the local methods. This Extension is always at the end of the chain and will not call
   * any further chain elements.
   */
  protected static class LocalClientSessionExtension<OWNER extends AbstractClientSession> extends AbstractExtension<OWNER> implements IClientSessionExtension<OWNER> {

    public LocalClientSessionExtension(OWNER owner) {
      super(owner);
    }

    @Override
    public void execStoreSession(ClientSessionStoreSessionChain chain) throws ProcessingException {
      getOwner().execStoreSession();
    }

    @Override
    public void execLoadSession(ClientSessionLoadSessionChain chain) throws ProcessingException {
      getOwner().execLoadSession();
    }

  }

  protected final void interceptStoreSession() throws ProcessingException {
    List<? extends IClientSessionExtension<? extends AbstractClientSession>> extensions = getAllExtensions();
    ClientSessionStoreSessionChain chain = new ClientSessionStoreSessionChain(extensions);
    chain.execStoreSession();
  }

  protected final void interceptLoadSession() throws ProcessingException {
    List<? extends IClientSessionExtension<? extends AbstractClientSession>> extensions = getAllExtensions();
    ClientSessionLoadSessionChain chain = new ClientSessionLoadSessionChain(extensions);
    chain.execLoadSession();
  }
}
