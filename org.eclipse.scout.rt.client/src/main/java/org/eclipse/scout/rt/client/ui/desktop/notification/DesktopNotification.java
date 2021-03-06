package org.eclipse.scout.rt.client.ui.desktop.notification;

import org.eclipse.scout.rt.platform.status.IStatus;
import org.eclipse.scout.rt.platform.status.Status;

public class DesktopNotification implements IDesktopNotification {

  private final IStatus m_status;
  private final long m_duration;
  private final boolean m_closable;

  /**
   * Creates a closable, simple info notification with a text and the default duration.
   */
  public DesktopNotification(String text) {
    m_status = new Status(text, IStatus.INFO);
    m_duration = DEFAULT_DURATION;
    m_closable = true;
  }

  public DesktopNotification(IStatus status, long duration, boolean closeable) {
    m_status = status;
    m_duration = duration;
    m_closable = closeable;
  }

  @Override
  public IStatus getStatus() {
    return m_status;
  }

  @Override
  public long getDuration() {
    return m_duration;
  }

  @Override
  public boolean isCloseable() {
    return m_closable;
  }

}
