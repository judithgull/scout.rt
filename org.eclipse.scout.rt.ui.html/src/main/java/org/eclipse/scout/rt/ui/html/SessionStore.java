package org.eclipse.scout.rt.ui.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.client.job.ModelJobs;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.util.Assertions;
import org.eclipse.scout.rt.platform.util.concurrent.IRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 5.2
 */
public class SessionStore implements HttpSessionBindingListener {
  private static final Logger LOG = LoggerFactory.getLogger(SessionStore.class);

  private final HttpSession m_httpSession;
  private final String m_httpSessionId; // because getId() cannot be called on an invalidated session
  private volatile boolean m_httpSessionValid = true;

  protected final Map<String, IClientSession> m_clientSessionMap = new HashMap<>();
  protected final Map<String, IUiSession> m_uiSessionMap = new HashMap<>();
  protected final Map<IClientSession, Set<IUiSession>> m_uiSessionsByClientSession = new HashMap<>();

  /**
   * Key: clientSessionId
   */
  protected final Map<String, IFuture<?>> m_housekeepingFutures = new HashMap<String, IFuture<?>>();

  protected final ReadLock m_readLock;
  protected final WriteLock m_writeLock;

  public SessionStore(HttpSession httpSession) {
    Assertions.assertNotNull(httpSession);
    m_httpSession = httpSession;
    m_httpSessionId = httpSession.getId();

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    m_readLock = lock.readLock();
    m_writeLock = lock.writeLock();

    LOG.info("Created new session store for HTTP session with ID {}", m_httpSessionId);
  }

  public HttpSession getHttpSession() {
    return m_httpSession;
  }

  public String getHttpSessionId() {
    return m_httpSessionId;
  }

  public boolean isHttpSessionValid() {
    return m_httpSessionValid;
  }

  /**
   * Can be used by subclasses to set {@link #m_httpSessionValid} to <code>false</code> (but not back to
   * <code>true</code>).
   */
  protected final void setHttpSessionValid(boolean httpSessionValid) {
    Assertions.assertTrue(m_httpSessionValid);
    m_httpSessionValid = httpSessionValid;
  }

  /**
   * @return a copy of the client session map
   */
  public Map<String, IClientSession> getClientSessionMap() {
    m_readLock.lock();
    try {
      return new HashMap<>(m_clientSessionMap);
    }
    finally {
      m_readLock.unlock();
    }
  }

  /**
   * @return a copy of the UI session map
   */
  public Map<String, IUiSession> getUiSessionMap() {
    m_readLock.lock();
    try {
      return new HashMap<>(m_uiSessionMap);
    }
    finally {
      m_readLock.unlock();
    }
  }

  /**
   * @return a copy of the "UI Sessions by client session" map
   */
  public Map<IClientSession, Set<IUiSession>> getUiSessionsByClientSession() {
    m_readLock.lock();
    try {
      Map<IClientSession, Set<IUiSession>> copy = new HashMap<>();
      for (Entry<IClientSession, Set<IUiSession>> entry : m_uiSessionsByClientSession.entrySet()) {
        copy.put(entry.getKey(), (entry.getValue() == null ? null : new HashSet<>(entry.getValue())));
      }
      return copy;
    }
    finally {
      m_readLock.unlock();
    }
  }

  public int countUiSessions() {
    m_readLock.lock();
    try {
      return m_uiSessionMap.size();
    }
    finally {
      m_readLock.unlock();
    }
  }

  public int countClientSessions() {
    m_readLock.lock();
    try {
      return m_clientSessionMap.size();
    }
    finally {
      m_readLock.unlock();
    }
  }

  public boolean isEmpty() {
    m_readLock.lock();
    try {
      return m_uiSessionMap.isEmpty() && m_clientSessionMap.isEmpty() && m_uiSessionsByClientSession.isEmpty();
    }
    finally {
      m_readLock.unlock();
    }
  }

  public IUiSession getUiSession(String uiSessionId) {
    m_readLock.lock();
    try {
      return m_uiSessionMap.get(uiSessionId);
    }
    finally {
      m_readLock.unlock();
    }
  }

  public void putUiSession(IUiSession uiSession) {
    Assertions.assertNotNull(uiSession);
    LOG.info("Put UI session in store: {} (clientSessionId={})", uiSession.getUiSessionId(), uiSession.getClientSessionId());
    m_writeLock.lock();
    try {
      m_uiSessionMap.put(uiSession.getUiSessionId(), uiSession);
    }
    finally {
      m_writeLock.unlock();
    }
  }

  public void removeUiSession(final IUiSession uiSession) {
    if (uiSession == null) {
      return;
    }
    LOG.info("Remove UI session from store: {} (clientSessionId={})", uiSession.getUiSessionId(), uiSession.getClientSessionId());
    m_writeLock.lock();
    try {
      // Remove uiSession
      m_uiSessionMap.remove(uiSession.getUiSessionId());

      // Unlink uiSession from clientSession
      final IClientSession clientSession = uiSession.getClientSession();
      Set<IUiSession> map = m_uiSessionsByClientSession.get(clientSession);
      if (map != null) {
        map.remove(uiSession);
      }
      LOG.info("{} UI Sessions remaining for client session {}", (map == null ? 0 : map.size()), clientSession.getId());
      startHousekeeping(clientSession);
    }
    finally {
      m_writeLock.unlock();
    }
  }

  /**
   * must be called from within a lock!
   */
  protected void startHousekeeping(final IClientSession clientSession) {
    // No client session, no house keeping necessary
    if (clientSession == null) {
      return;
    }

    // If client session is already inactive or stopping, simply update the maps, but take no further action.
    if (!clientSession.isActive() || clientSession.isStopping()) {
      removeClientSession(clientSession);
      return;
    }

    // Check if client session is still used after a few moments
    LOG.info("Schedule housekeeping for client session with ID {} (httpSessionId={})...", clientSession.getId(), m_httpSessionId);
    final IFuture<Void> future = Jobs.schedule(new IRunnable() {
      @Override
      public void run() throws Exception {
        m_writeLock.lock();
        try {
          if (IFuture.CURRENT.get().isCancelled()) {
            return;
          }
          m_housekeepingFutures.remove(clientSession.getId());

          // Check if the client session is referenced by any UI Session
          Set<IUiSession> uiSessions = m_uiSessionsByClientSession.get(clientSession);
          LOG.info("Housekeeping: client session {0} referenced by {0} UI sessions", clientSession.getId(), uiSessions.size());
          if (uiSessions == null || uiSessions.isEmpty()) {
            LOG.info("Housekeeping: Shutting down client session with ID {} because it is not used anymore", clientSession.getId());
            ModelJobs.schedule(new IRunnable() {
              @Override
              public void run() throws Exception {
//                // XXX necessary?
//                if (clientSession.isActive() && clientSession.getDesktop() != null) {
//                  clientSession.getDesktop().getUIFacade().fireDesktopClosingFromUI(true);
//                }
                if (clientSession.isActive() && !clientSession.isStopping()) {
                  clientSession.stop();
                }
              }
            }, ModelJobs.newInput(ClientRunContexts.copyCurrent().withSession(clientSession, true))
                .withName("Shutdown session by housekeeping"))
                .awaitDone();

            removeClientSession(clientSession);
            LOG.info("Housekeeping: Client session with ID {} terminated.", clientSession.getId());
          }
        }
        finally {
          m_writeLock.unlock();
        }
      }
    }, Jobs.newInput()
        .withName("Session housekeeping")
        .withExecutionTrigger(Jobs.newExecutionTrigger()
            .withStartIn(10, TimeUnit.SECONDS))); // XXX which amount of time?

    // Put the future in a list, so we can cancel it if the session is requested again
    m_housekeepingFutures.put(clientSession.getId(), future);
  }

  public IClientSession getClientSessionForUse(String clientSessionId) {
    m_writeLock.lock();
    try {
      IFuture<?> future = m_housekeepingFutures.get(clientSessionId);
      if (future != null) {
        LOG.info("Housekeeping for client session {} cancelled", clientSessionId);
        future.cancel(false);
        m_housekeepingFutures.remove(clientSessionId);
      }
      return getClientSession(clientSessionId);
    }
    finally {
      m_writeLock.unlock();
    }
  }

  public IClientSession getClientSession(String clientSessionId) {
    m_readLock.lock();
    try {
      return m_clientSessionMap.get(clientSessionId);
    }
    finally {
      m_readLock.unlock();
    }
  }

  public void putClientSession(IClientSession clientSession) {
    Assertions.assertNotNull(clientSession);
    LOG.info("Put client session in session store: {}", clientSession.getId());
    m_writeLock.lock();
    try {
      m_clientSessionMap.put(clientSession.getId(), clientSession);
    }
    finally {
      m_writeLock.unlock();
    }
  }

  public void removeClientSession(final IClientSession clientSession) {
    if (clientSession == null) {
      return;
    }
    LOG.info("Remove client session from session store: {}", clientSession.getId());
    m_writeLock.lock();
    try {
      m_clientSessionMap.remove(clientSession.getId());
      // Also remove all associated UI sessions
      Set<IUiSession> map = m_uiSessionsByClientSession.get(clientSession);
      if (map != null) {
        LOG.info("Remove {} associated UI sessions for client session {}", map.size(), clientSession.getId());
        for (IUiSession uiSession : map) {
          m_uiSessionMap.remove(uiSession.getUiSessionId());
        }
      }
      m_uiSessionsByClientSession.remove(clientSession);
    }
    finally {
      m_writeLock.unlock();
    }
  }

  public void linkSessions(IClientSession clientSession, UiSession uiSession) {
    m_writeLock.lock();
    try {
      m_uiSessionMap.put(uiSession.getUiSessionId(), uiSession);
      Set<IUiSession> map = m_uiSessionsByClientSession.get(clientSession);
      if (map == null) {
        map = new HashSet<>();
        m_uiSessionsByClientSession.put(clientSession, map);
      }
      map.add(uiSession);
    }
    finally {
      m_writeLock.unlock();
    }
  }

  @Override
  public void valueBound(HttpSessionBindingEvent event) {
  }

  @Override
  public void valueUnbound(HttpSessionBindingEvent event) {
    m_httpSessionValid = false;
    final List<IFuture<?>> futures = new ArrayList<>();

    m_writeLock.lock();
    LOG.info("Detected invalidation of HTTP session {}, cleaning up {} client sessions and {} UI sessions", m_httpSessionId, m_clientSessionMap.size(), m_uiSessionMap.size());
    try {
      // 1. Stop all client sessions
      for (final IClientSession clientSession : m_clientSessionMap.values()) {
        futures.add(ModelJobs.schedule(new IRunnable() {
          @Override
          public void run() {
            LOG.info("Shutting down client session with ID {} due to invalidation of HTTP session", clientSession.getId());
            // Dispose model (if session was not already stopped earlier by itself).
            // Session inactivation is executed delayed (see AbstractClientSession#getMaxShutdownWaitTime(), that's why desktop may already be null
//          if (clientSession.isActive() && clientSession.getDesktop() != null) {
//            clientSession.getDesktop().getUIFacade().fireDesktopClosingFromUI(true);
//          } XXX necessary?
            if (clientSession.isActive() && !clientSession.isStopping()) {
              clientSession.stop();
            }
            LOG.info("Client session with ID {} terminated.", clientSession.getId());
          }
        }, ModelJobs.newInput(ClientRunContexts.copyCurrent().withSession(clientSession, true))
            .withName("Closing desktop due to HTTP session invalidation")));
      }
    }
    finally {
      m_writeLock.unlock();
    }

    LOG.info("Waiting for {} client sessions to stop...", futures.size());
    if (ModelJobs.isModelThread()) {
      // In case someone called HttpSession.invalidate() in a model job, we move the current job to the end
      // of the queue. Otherwise, the following awaitDone() call would run in a timeout, because the
      // scheduled shutdown job from above for the same session would be blocked by the current job.
      ModelJobs.yield();
    }
    Jobs.getJobManager().awaitDone(Jobs.newFutureFilterBuilder()
        .andMatchFuture(futures)
        .toFilter(), 1, TimeUnit.MINUTES);

    // 3. Dispose all UI sessions (in case stopping the client session did not clean up everything)
    // XXX

    LOG.info("Session shutdown complete.");

    m_readLock.lock();
    try {
      if (!isEmpty()) {
        LOG.warn("Leak detection - Session store not empty after shutdown: [uiSessionMap: {}, clientSessionMap: {}, uiSessionsByClientSession: {}]", m_uiSessionMap.size(), m_clientSessionMap.size(), m_uiSessionsByClientSession.size());
      }
    }
    finally {
      m_readLock.unlock();
    }
  }
}
