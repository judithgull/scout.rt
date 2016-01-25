package org.eclipse.scout.rt.ui.html;

import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpSession;

import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.util.Assertions;

/**
 * @since 5.2
 */
@ApplicationScoped
public class HttpSessionHelper {

  public static final String SESSION_STORE_ATTRIBUTE_NAME = "scout.htmlui.httpsession.sessionstore";
  public static final String HTTP_SESSION_LOCK_ATTRIBUTE_NAME = "scout.htmlui.httpsession.lock";

  public ReentrantLock httpSessionLock(HttpSession httpSession) {
    Assertions.assertNotNull(httpSession);
    synchronized (httpSession) {
      ReentrantLock lock = (ReentrantLock) httpSession.getAttribute(HTTP_SESSION_LOCK_ATTRIBUTE_NAME);
      if (lock == null) {
        lock = new ReentrantLock();
        httpSession.setAttribute(HTTP_SESSION_LOCK_ATTRIBUTE_NAME, lock);
      }
      return lock;
    }
  }

  protected SessionStore getSessionStore(HttpSession httpSession, boolean createIfNecessasry) {
    if (createIfNecessasry) {
      Assertions.assertNotNull(httpSession);
    }
    if (httpSession == null) {
      return null;
    }
    ReentrantLock lock = httpSessionLock(httpSession);
    lock.lock();
    try {
      SessionStore sessionStore = (SessionStore) httpSession.getAttribute(SESSION_STORE_ATTRIBUTE_NAME);
      if (sessionStore == null && createIfNecessasry) {
        sessionStore = new SessionStore(httpSession);
        httpSession.setAttribute(SESSION_STORE_ATTRIBUTE_NAME, sessionStore);
      }
      return sessionStore;
    }
    finally {
      lock.unlock();
    }
  }

  public SessionStore getSessionStore(HttpSession httpSession) {
    return getSessionStore(httpSession, false);
  }

  public SessionStore getOrCreateSessionStore(HttpSession httpSession) {
    return getSessionStore(httpSession, true);
  }
}
