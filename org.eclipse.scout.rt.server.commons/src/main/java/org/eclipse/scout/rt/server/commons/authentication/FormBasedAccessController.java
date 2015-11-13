/*
 * Copyright (c) BSI Business Systems Integration AG. All rights reserved.
 * http://www.bsiag.com/
 */
package org.eclipse.scout.rt.server.commons.authentication;

import java.io.IOException;
import java.security.Principal;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.rt.platform.BEANS;

/**
 * Authenticator for Form-based authentication. This authenticator is designed to collaborate with
 * 'scout-login-module.js' with a 'login.html' and 'logout.html' page in place, and is to be installed in UI server
 * only.
 * <p>
 * User authentication works as following:
 * <ol>
 * <li>The user provides credentials via 'login.html'</li>
 * <li>'scout-login-module.js' sends the credentials to '/auth' URL to be verified by this authenticator's
 * {@link ICredentialVerifier}</li>
 * <li>On successful authentication, the user's principal is put onto HTTP session, so that subsequent requests of that
 * user can be fast authenticated by {@link TrivialAccessController}. However, the filter-chain is not continued.
 * Instead, the JavaScript login module take appropriate actions upon HTTP 200 response code.
 * </ol>
 * Furthermore, this authenticator takes care for requests targeted to '/login' and '/logout', by internally dispatching
 * the request to the appropriate resource.
 * <p>
 * This controller is designed to be installed right ahead of {@link TrivialAccessController}. That allows to access
 * '/login' and '/logout' page even for authenticated users.
 *
 * @see scout-login-module.js
 * @see login.js
 * @see logout.js
 * @since 5.2
 */
public class FormBasedAccessController implements IAccessController {

  protected FormBasedAuthConfig m_config;

  public FormBasedAccessController init(final FormBasedAuthConfig config) {
    m_config = config;
    Assertions.assertNotNull(m_config.getCredentialVerifier(), "CredentialVerifier must not be null");
    Assertions.assertNotNull(m_config.getPrincipalProducer(), "PrincipalProducer must not be null");
    return this;
  }

  @Override
  public boolean handle(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
    if (!m_config.isEnabled()) {
      return false;
    }

    final String pathInfo = request.getPathInfo();
    if (pathInfo == null) {
      return false;
    }

    switch (pathInfo) {
      case "/login":
        return handleLoginRequest(request, response);
      case "/auth":
        return handleAuthRequest(request, response);
      case "/logout":
        return handleLogoutRequest(request, response);
      default:
        return false;
    }
  }

  @Override
  public void destroy() {
    // NOOP
  }

  /**
   * Invoke to handle a login request targeted to '/login'. The default implementation internally dispatches to
   * '/login.html' page so that the user can enter username and password.
   */
  protected boolean handleLoginRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    forwardToLoginForm(request, response);
    return true;
  }

  /**
   * Invoke to handle a logout request targeted to '/logout'. The default implementation invalidates HTTP session and
   * internally dispatches to '/logout.html' page.
   */
  protected boolean handleLogoutRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    BEANS.get(ServletFilterHelper.class).doLogout(request);
    BEANS.get(ServletFilterHelper.class).forwardToLogoutForm(request, response);
    return true;
  }

  /**
   * Method invoked to handle an authentication request targeted to '/auth'. The default implementation verifies
   * credentials sent via request parameters, and upon successful authentication puts the principal onto HTTP session.
   * However, the filter chain is not continued, meaning that 'login.js' is responsible to redirect the user to the
   * requested resource.
   */
  protected boolean handleAuthRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    final Entry<String, char[]> credentials = readCredentials(request);
    if (credentials == null) {
      handleForbidden(ICredentialVerifier.AUTH_CREDENTIALS_REQUIRED, response);
      return true;
    }

    // Never cache authentication requests.
    response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setDateHeader("Expires", 0); // prevents caching at the proxy server

    final int status = m_config.getCredentialVerifier().verify(credentials.getKey(), credentials.getValue());
    if (status != ICredentialVerifier.AUTH_OK) {
      handleForbidden(status, response);
      return true;
    }

    // OWASP: force a new HTTP session to be created.
    final HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    // Put authenticated Subject onto HTTP session.
    final Principal principal = m_config.getPrincipalProducer().produce(credentials.getKey());
    BEANS.get(ServletFilterHelper.class).putPrincipalOnSession(request, principal);
    return true;
  }

  /**
   * Method invoked if the user could not be verified. The default implementation waits some time to address brute-force
   * attacks, and sets a 403 HTTP status code.
   */
  protected void handleForbidden(final int status, final HttpServletResponse response) throws IOException, ServletException {
    if (m_config.get403WaitMillis() > 0L) {
      try {
        Thread.sleep(m_config.get403WaitMillis());
      }
      catch (final InterruptedException e) {
        // NOOP
      }
    }
    response.sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  /**
   * Invoke to forward to '/login.html' page so that the user can enter his credentials.
   */
  public void forwardToLoginForm(final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
    BEANS.get(ServletFilterHelper.class).forwardToLoginForm(req, resp);
  }

  /**
   * Reads the credentials sent by 'login.js' from request parameters.
   */
  protected Entry<String, char[]> readCredentials(final HttpServletRequest request) {
    final String user = request.getParameter("user");
    if (StringUtility.isNullOrEmpty(user)) {
      return null;
    }

    final String password = request.getParameter("password");
    if (StringUtility.isNullOrEmpty(password)) {
      return null;
    }

    return new SimpleEntry<>(user, password.toCharArray());
  }

  /**
   * Configuration for {@link FormBasedAccessController}.
   */
  public static class FormBasedAuthConfig {

    private boolean m_enabled = true;
    private long m_403WaitMillis = 500L;
    private ICredentialVerifier m_credentialVerifier;
    private IPrincipalProducer m_principalProducer = BEANS.get(SimplePrincipalProducer.class);

    public boolean isEnabled() {
      return m_enabled;
    }

    public FormBasedAuthConfig withEnabled(final boolean enabled) {
      m_enabled = enabled;
      return this;
    }

    public ICredentialVerifier getCredentialVerifier() {
      return m_credentialVerifier;
    }

    /**
     * Sets the {@link ICredentialVerifier} to verify user's credentials.
     */
    public FormBasedAuthConfig withCredentialVerifier(final ICredentialVerifier credentialVerifier) {
      m_credentialVerifier = credentialVerifier;
      return this;
    }

    public IPrincipalProducer getPrincipalProducer() {
      return m_principalProducer;
    }

    /**
     * Sets the {@link IPrincipalProducer} to produce a {@link Principal} for authenticated users. By default,
     * {@link SimplePrincipalProducer} is used.
     */
    public FormBasedAuthConfig withPrincipalProducer(final IPrincipalProducer principalProducer) {
      m_principalProducer = principalProducer;
      return this;
    }

    public long get403WaitMillis() {
      return m_403WaitMillis;
    }

    /**
     * Sets the time to wait to respond with a 403 response code. That is a simple mechanism to address brute-force
     * attacks, but may have a negative effect on DoS attacks. By default, this authenticator waits for 500ms.
     */
    public FormBasedAuthConfig with403WaitMillis(final long waitMillis) {
      m_403WaitMillis = waitMillis;
      return this;
    }
  }
}