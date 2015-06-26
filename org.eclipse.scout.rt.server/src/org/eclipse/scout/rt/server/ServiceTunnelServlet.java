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
package org.eclipse.scout.rt.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.security.AccessController;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.osgi.BundleInspector;
import org.eclipse.scout.commons.serialization.SerializationUtility;
import org.eclipse.scout.rt.server.admin.html.AdminSession;
import org.eclipse.scout.rt.server.commons.cache.IClientIdentificationService;
import org.eclipse.scout.rt.server.commons.cache.IHttpSessionCacheService;
import org.eclipse.scout.rt.server.commons.servletfilter.HttpServletEx;
import org.eclipse.scout.rt.server.commons.servletfilter.helper.HttpAuthJaasFilter;
import org.eclipse.scout.rt.server.internal.Activator;
import org.eclipse.scout.rt.server.internal.ServerSessionClassFinder;
import org.eclipse.scout.rt.server.services.common.session.IServerSessionRegistryService;
import org.eclipse.scout.rt.shared.servicetunnel.DefaultServiceTunnelContentHandler;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnelContentHandler;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnelRequest;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnelResponse;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.eclipse.scout.service.SERVICES;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Use this servlet to dispatch scout gui service requests using {@link IServiceTunnelRequest},
 * {@link IServiceTunnelResponse} and any {@link IServiceTunnelContentHandler} implementation.
 * <p>
 * Override the methods
 * {@link DefaultTransactionDelegate#validateInput(org.eclipse.scout.rt.shared.validate.IValidationStrategy, Object, java.lang.reflect.Method, Object[])
 * DefaultTransactionDelegate#validateInput} and
 * {@link DefaultTransactionDelegate#validateOutput(org.eclipse.scout.rt.shared.validate.IValidationStrategy, Object, java.lang.reflect.Method, Object, Object[])
 * DefaultTransactionDelegate#validateOutput} to do central input/output validation.
 * <p>
 * By default there is a jaas convenience filter {@link HttpAuthJaasFilter} on /process and a {@link SoapWsseJaasFilter}
 * on /ajax with priority 1000
 * <p>
 * When using RAP (rich ajax platform) as the ui web app then there must be a {@link WebSessionIdPrincipal} in the
 * subject, in order to map those requests to virtual sessions instead of (the unique) http session.
 */
@SuppressWarnings("deprecation")
public class ServiceTunnelServlet extends HttpServletEx {
  public static final String HTTP_DEBUG_PARAM = "org.eclipse.scout.rt.server.http.debug";

  /**
   * HTTP connection distinguishes between different {@link IClientSession} connecting concurrently
   */
  public static final String MULTI_CLIENT_SESSION_COOKIESTORE = "org.eclipse.scout.rt.multiClientSessionCookieStoreEnabled";

  private static final long serialVersionUID = 1L;
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(ServiceTunnelServlet.class);

  private transient IServiceTunnelContentHandler m_contentHandler;
  private transient Bundle[] m_orderedBundleList;
  private final Object m_orderedBundleListLock = new Object();
  private final Object m_msgEncoderLock = new Object();
  private Class<? extends IServerSession> m_serverSessionClass;
  private Version m_requestMinVersion;
  private final boolean m_debug;
  private final boolean m_isMultiClientSessionCookieStore;

  private final VirtualSessionCache m_ajaxSessionCache = new VirtualSessionCache();
  private volatile boolean m_isAjaxSessionTimeoutInitialized = false;

  public ServiceTunnelServlet(boolean multiClientSessionCookieStore, boolean debug) {
    m_isMultiClientSessionCookieStore = multiClientSessionCookieStore;
    m_debug = debug;
  }

  public ServiceTunnelServlet() {
    this(StringUtility.parseBoolean(Activator.getDefault().getBundle().getBundleContext().getProperty(MULTI_CLIENT_SESSION_COOKIESTORE), true),
        StringUtility.parseBoolean(Activator.getDefault().getBundle().getBundleContext().getProperty(HTTP_DEBUG_PARAM)));
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    m_requestMinVersion = initRequestMinVersion(config);
  }

  protected void lazyInit(HttpServletRequest req, HttpServletResponse res) throws ServletException {
    if (m_serverSessionClass == null) {
      m_serverSessionClass = locateServerSessionClass(req, res);
    }
    if (m_serverSessionClass == null) {
      m_serverSessionClass = loadSessionClassByParam();
    }
    if (m_serverSessionClass == null) {
      //legacy support
      m_serverSessionClass = new ServerSessionClassFinder().findClassByConvention(req.getServletPath());
    }
    if (m_serverSessionClass == null) {
      throw new ServletException("Expected init-param \"session\"");
    }
  }

  /**
   * Load the IServerSession class by servlet config parameter
   *
   * @throws ServletException
   */
  @SuppressWarnings("unchecked")
  private Class<? extends IServerSession> loadSessionClassByParam() throws ServletException {
    String qname = getServletConfig().getInitParameter("session");
    if (qname != null) {
      int i = qname.lastIndexOf('.');
      try {
        m_serverSessionClass = (Class<? extends IServerSession>) Platform.getBundle(qname.substring(0, i)).loadClass(qname);
      }
      catch (ClassNotFoundException e) {
        throw new ServletException("Loading class " + qname, e);
      }
    }
    return null;
  }

  protected Class<? extends IServerSession> locateServerSessionClass(HttpServletRequest req, HttpServletResponse res) {
    try {
      return SERVICES.getService(IServerJobService.class).getServerSessionClass();
    }
    catch (ProcessingException e) {
      LOG.debug("No server session found");
    }
    return null;
  }

  /**
   * <p>
   * Reads the minimum version a request must have.
   * </p>
   * <p>
   * The version has to be defined as init parameter in the servlet configuration. <br/>
   * This can be done by adding a new init-param at the {@link DefaultHttpProxyHandlerServlet} on the extension point
   * org.eclipse.equinox.http.registry.servlets and setting its name to min-version and its value to the desired version
   * (like 1.2.3).
   * </p>
   * <p>
   * If there is no min-version defined it uses the Bundle-Version of the bundle which contains the running product.
   * </p>
   *
   * @param config
   * @return
   */
  protected Version initRequestMinVersion(ServletConfig config) {
    Version version = null;
    String v = config.getInitParameter("min-version");
    if (v != null) {
      Version tmp = Version.parseVersion(v);
      version = new Version(tmp.getMajor(), tmp.getMinor(), tmp.getMicro());
    }
    else if (Platform.getProduct() != null) {
      v = (String) Platform.getProduct().getDefiningBundle().getHeaders().get("Bundle-Version");
      Version tmp = Version.parseVersion(v);
      version = new Version(tmp.getMajor(), tmp.getMinor(), tmp.getMicro());
    }

    return version;
  }

  /**
   * create the (reusable) content handler (soap, xml, binary) for marshalling scout/osgi remote service calls
   * <p>
   * This method is part of the protected api and can be overridden.
   */
  protected IServiceTunnelContentHandler createContentHandler(Class<? extends IServerSession> sessionClass) {
    DefaultServiceTunnelContentHandler e = new DefaultServiceTunnelContentHandler();
    e.initialize();
    return e;
  }

  private IServiceTunnelContentHandler getServiceTunnelContentHandler() {
    synchronized (m_msgEncoderLock) {
      if (m_contentHandler == null) {
        m_contentHandler = createContentHandler(m_serverSessionClass);
      }
    }
    return m_contentHandler;
  }

  /**
   * @deprecated use {@link SerializationUtility} instead. Will be removed in the N release.
   * @return
   */
  @Deprecated
  protected Bundle[] getOrderedBundleList() {
    synchronized (m_orderedBundleListLock) {
      if (m_orderedBundleList == null) {
        String[] bundleOrderPrefixes = SerializationUtility.getBundleOrderPrefixes();
        m_orderedBundleList = BundleInspector.getOrderedBundleList(bundleOrderPrefixes);
      }
    }
    return m_orderedBundleList;
  }

  protected IServerSession lookupServerSession(HttpServletRequest req, HttpServletResponse res, Subject subject, IServiceTunnelRequest serviceRequest) throws ProcessingException, ServletException {
    UserAgent userAgent = UserAgent.createByIdentifier(serviceRequest.getUserAgent());
    String virtualSessionId = serviceRequest.getVirtualSessionId();
    if (virtualSessionId != null && !m_isMultiClientSessionCookieStore) {
      return lookupScoutServerSessionOnVirtualSession(req, res, virtualSessionId, subject, userAgent);
    }
    else {
      return lookupScoutServerSessionOnHttpSession(req, res, subject, userAgent);
    }
  }

  protected IServerSession lookupScoutServerSessionOnHttpSession(HttpServletRequest req, HttpServletResponse res, Subject subject, UserAgent userAgent) throws ProcessingException, ServletException {
    //external request: apply locking, this is the session initialization phase
    IHttpSessionCacheService cacheService = SERVICES.getService(IHttpSessionCacheService.class);
    IServerSession serverSession = (IServerSession) cacheService.getAndTouch(IServerSession.class.getName(), req, res);
    if (serverSession == null) {
      synchronized (req.getSession()) {
        serverSession = (IServerSession) cacheService.get(IServerSession.class.getName(), req, res); // double checking
        if (serverSession == null) {
          serverSession = SERVICES.getService(IServerSessionRegistryService.class).newServerSession(m_serverSessionClass, subject, userAgent);
          serverSession.setIdInternal(SERVICES.getService(IClientIdentificationService.class).getClientId(req, res));
          cacheService.put(IServerSession.class.getName(), serverSession, req, res);
        }
      }
    }
    return serverSession;
  }

  private IServerSession lookupScoutServerSessionOnVirtualSession(HttpServletRequest req, HttpServletResponse res, String ajaxSessionId, Subject subject, UserAgent userAgent) throws ProcessingException, ServletException {
    initializeAjaxSessionTimeout(req);
    IServerSession serverSession = m_ajaxSessionCache.get(ajaxSessionId);
    if (serverSession == null) {
      synchronized (m_ajaxSessionCache) {
        serverSession = m_ajaxSessionCache.get(ajaxSessionId);
        if (serverSession == null) { // double checking
          return createAndCacheNewServerSession(ajaxSessionId, subject, userAgent, req, res);
        }
      }
    }
    m_ajaxSessionCache.touch(ajaxSessionId);
    return serverSession;
  }

  /**
   * Initialize Ajax Session timeout only once from the HTTP session
   */
  protected void initializeAjaxSessionTimeout(HttpServletRequest req) {
    if (!m_isAjaxSessionTimeoutInitialized) {
      final long defaultSessionTimeout = 1800L;
      final long millisecondsInSeconds = 1000L;
      m_ajaxSessionCache.setSessionTimeoutMillis(Math.max(millisecondsInSeconds * defaultSessionTimeout, millisecondsInSeconds * req.getSession().getMaxInactiveInterval()));
      m_isAjaxSessionTimeoutInitialized = true;
    }
  }

  private IServerSession createAndCacheNewServerSession(String ajaxSessionId, Subject subject, UserAgent userAgent, HttpServletRequest req, HttpServletResponse res) throws ProcessingException {
    IServerSession serverSession = SERVICES.getService(IServerSessionRegistryService.class).newServerSession(m_serverSessionClass, subject, userAgent);
    serverSession.setIdInternal(SERVICES.getService(IClientIdentificationService.class).getClientId(req, res));
    m_ajaxSessionCache.put(ajaxSessionId, serverSession);
    return serverSession;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    Subject subject = Subject.getSubject(AccessController.getContext());
    if (subject == null) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    //invoke
    Map<Class, Object> backup = ThreadContext.backup();
    try {
      lazyInit(req, res);
      //
      //legacy, deprecated, do not use servlet request/response in scout code
      ThreadContext.putHttpServletRequest(req);
      ThreadContext.putHttpServletResponse(res);
      //
      UserAgent userAgent = UserAgent.createDefault();
      IServerSession serverSession = lookupScoutServerSessionOnHttpSession(req, res, subject, userAgent);
      //
      ServerJob job = new AdminServiceJob(serverSession, subject, req, res);
      job.runNow(new NullProgressMonitor());
      job.throwOnError();
    }
    catch (ProcessingException e) {
      throw new ServletException(e);
    }
    finally {
      ThreadContext.restore(backup);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    Subject subject = Subject.getSubject(AccessController.getContext());
    if (subject == null) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    try {
      lazyInit(req, res);
      Map<Class, Object> backup = ThreadContext.backup();
      Locale oldLocale = LocaleThreadLocal.get(false);
      try {
        ThreadContext.putHttpServletRequest(req);
        ThreadContext.putHttpServletResponse(res);
        //read request
        IServiceTunnelRequest serviceRequest = deserializeInput(req.getInputStream());
        LocaleThreadLocal.set(serviceRequest.getLocale());
        //virtual or http session?
        IServerSession serverSession = lookupServerSession(req, res, subject, serviceRequest);
        //invoke
        AtomicReference<IServiceTunnelResponse> serviceResponseHolder = new AtomicReference<IServiceTunnelResponse>();
        ServerJob job = createServiceTunnelServerJob(serverSession, serviceRequest, serviceResponseHolder, subject);
        job.setTransactionSequence(serviceRequest.getRequestSequence());
        job.runNow(new NullProgressMonitor());
        job.throwOnError();
        serializeOutput(res, serviceResponseHolder.get());
      }
      finally {
        ThreadContext.restore(backup);
        LocaleThreadLocal.set(oldLocale);
      }
    }
    catch (Throwable t) {
      //ignore disconnect errors
      // we don't want to throw an exception, if the client closed the connection
      Throwable cause = t;
      while (cause != null) {
        if (cause instanceof SocketException) {
          return;
        }
        else if (cause.getClass().getSimpleName().equalsIgnoreCase("EofException")) {
          return;
        }
        else if (cause instanceof InterruptedIOException) {
          return;
        }
        // next
        cause = cause.getCause();
      }
      LOG.error("Client=" + req.getRemoteUser() + "@" + req.getRemoteAddr() + "/" + req.getRemoteHost(), t);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  protected IServiceTunnelRequest deserializeInput(InputStream in) throws Exception {
    return getServiceTunnelContentHandler().readRequest(in);
  }

  protected void serializeOutput(HttpServletResponse httpResponse, IServiceTunnelResponse res) throws Exception {
    // security: do not send back error stack trace
    if (res.getException() != null) {
      res.getException().setStackTrace(new StackTraceElement[0]);
    }
    //
    httpResponse.setDateHeader("Expires", -1);
    httpResponse.setHeader("Cache-Control", "no-cache");
    httpResponse.setHeader("pragma", "no-cache");
    httpResponse.setContentType("text/xml");
    getServiceTunnelContentHandler().writeResponse(httpResponse.getOutputStream(), res);
  }

  /**
   * Create the {@link ServerJob} that runs the request as a single atomic transaction
   */
  protected ServerJob createServiceTunnelServerJob(IServerSession serverSession, IServiceTunnelRequest serviceRequest, AtomicReference<IServiceTunnelResponse> serviceResponseHolder, Subject subject) {
    return new RemoteServiceJob(serverSession, serviceRequest, serviceResponseHolder, subject);
  }

  /**
   * runnable content of the {@link ServerJob}, thzis is the atomic transaction
   * <p>
   * This method is part of the protected api and can be overridden.
   */
  protected IServiceTunnelResponse runServerJobTransaction(IServiceTunnelRequest req) throws Exception {
    return runServerJobTransactionWithDelegate(req, m_requestMinVersion, m_debug);
  }

  /**
   * @deprecated use {@link #runServerJobTransactionWithDelegate(IServiceTunnelRequest, Version, boolean)} instead. Will
   *             be removed with the N-Release.
   */
  @Deprecated
  protected IServiceTunnelResponse runServerJobTransactionWithDelegate(IServiceTunnelRequest req, Bundle[] loaderBundles, Version requestMinVersion, boolean debug) throws Exception {
    return runServerJobTransactionWithDelegate(req, requestMinVersion, debug);
  }

  protected IServiceTunnelResponse runServerJobTransactionWithDelegate(IServiceTunnelRequest req, Version requestMinVersion, boolean debug) throws Exception {
    return new DefaultTransactionDelegate(requestMinVersion, debug).invoke(req);
  }

  private class RemoteServiceJob extends ServerJob {

    private final IServiceTunnelRequest m_serviceRequest;
    private final AtomicReference<IServiceTunnelResponse> m_serviceResponseHolder;

    public RemoteServiceJob(IServerSession serverSession, IServiceTunnelRequest serviceRequest, AtomicReference<IServiceTunnelResponse> serviceResponseHolder, Subject subject) {
      super("RemoteServiceCall", serverSession, subject);
      m_serviceRequest = serviceRequest;
      m_serviceResponseHolder = serviceResponseHolder;
    }

    public IServiceTunnelRequest getServiceRequest() {
      return m_serviceRequest;
    }

    public AtomicReference<IServiceTunnelResponse> getServiceResponseHolder() {
      return m_serviceResponseHolder;
    }

    @Override
    protected IStatus runTransaction(IProgressMonitor monitor) throws Exception {
      IServiceTunnelResponse serviceRes = runServerJobTransaction(getServiceRequest());
      getServiceResponseHolder().set(serviceRes);
      return Status.OK_STATUS;
    }
  }

  private class AdminServiceJob extends ServerJob {

    protected HttpServletRequest m_request;
    protected HttpServletResponse m_response;

    public AdminServiceJob(IServerSession serverSession, Subject subject, HttpServletRequest request, HttpServletResponse response) {
      super("AdminServiceCall", serverSession, subject);
      m_request = request;
      m_response = response;
    }

    @Override
    protected IStatus runTransaction(IProgressMonitor monitor) throws Exception {
      String key = AdminSession.class.getName();
      AdminSession as = (AdminSession) SERVICES.getService(IHttpSessionCacheService.class).getAndTouch(key, m_request, m_response);
      if (as == null) {
        as = new AdminSession();
        SERVICES.getService(IHttpSessionCacheService.class).put(key, as, m_request, m_response);
      }
      as.serviceRequest(m_request, m_response);
      return Status.OK_STATUS;
    }
  }

}
