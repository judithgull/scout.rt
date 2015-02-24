/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.services.common.prefs;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.Base64Utility;
import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.cdi.OBJ;
import org.eclipse.scout.rt.shared.ISession;
import org.eclipse.scout.service.SERVICES;

/**
 * Default implementation for the preferences using a simple hash map and {@link String} key-value pairs.
 *
 * @since 5.1
 */
public class Preferences implements IPreferences {

  /**
   * Specifies a {@link PreferenceChangeEvent} that the full preference node has been cleared (all preferences have been
   * removed).
   */
  public static final int EVENT_KIND_CLEAR = 1 << 0;

  /**
   * Specifies a {@link PreferenceChangeEvent} that a new preference has been added.
   */
  public static final int EVENT_KIND_ADD = 1 << 1;

  /**
   * Specifies a {@link PreferenceChangeEvent} that an existing preference has been updated to a new value.
   */
  public static final int EVENT_KIND_CHANGE = 1 << 2;

  /**
   * Specifies a {@link PreferenceChangeEvent} that an existing preference has been removed.
   */
  public static final int EVENT_KIND_REMOVE = 1 << 3;

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(Preferences.class);
  private static final long serialVersionUID = 1L;

  private final String m_name;
  private final ISession m_session;
  private final Map<String, String> m_prefs;
  private final EventListenerList m_eventListeners;
  private boolean m_dirty;

  protected Preferences(String name, ISession userScope) {
    m_name = name;
    m_session = userScope;
    m_prefs = new LinkedHashMap<>();
    m_eventListeners = new EventListenerList();
    m_dirty = false;
  }

  /**
   * Gets the {@link IPreferences} for the given <code>nodeId</code> and the given <code>userScope</code>
   *
   * @param userScope
   *          The {@link ISession} for which the settings should be retrieved. Must not be <code>null</code>.
   * @param nodeId
   *          The id of the node to retrieve. Must not be <code>null</code>.
   * @return The {@link IPreferences} for the given node and scope.
   * @throws ProcessingException
   *           On an error while loading the preferences.
   * @throws IllegalArgumentException
   *           if the session or nodeId is <code>null</code>.
   */
  public static IPreferences get(ISession userScope, String nodeId) throws ProcessingException {
    IUserPreferencesService service = OBJ.NEW(IUserPreferencesService.class, null);
    if (service == null) {
      LOG.warn("No preferences service could be found!");
      return null;
    }

    return service.getPreferences(userScope, nodeId);
  }

  @Override
  public synchronized boolean flush() throws ProcessingException {
    if (!isDirty()) {
      return false;
    }

    IUserPreferencesStorageService service = SERVICES.getService(IUserPreferencesStorageService.class);
    if (service == null) {
      LOG.warn("No preferences storage service could be found!");
      return false;
    }

    service.flush(this);
    m_dirty = false;
    return true;
  }

  @Override
  public boolean put(String key, String value) {
    return putInternal(key, value);
  }

  @Override
  public String get(String key, String defaultValue) {
    String value = getInternal(key);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  @Override
  public synchronized boolean remove(String key) {
    if (key == null) {
      throw new IllegalArgumentException("null key is not allowed.");
    }

    String old = m_prefs.remove(key);
    if (old != null) {
      m_dirty = true;
      PreferenceChangeEvent event = new PreferenceChangeEvent(this, EVENT_KIND_REMOVE, key, old, null);
      fireEvent(event);
      return true;
    }
    return false;
  }

  @Override
  public synchronized boolean clear() {
    if (m_prefs.isEmpty()) {
      return false;
    }

    m_prefs.clear();
    m_dirty = true;

    PreferenceChangeEvent event = new PreferenceChangeEvent(this, EVENT_KIND_CLEAR, null, null, null);
    fireEvent(event);
    return true;
  }

  @Override
  public boolean putInt(String key, int value) {
    return putInternal(key, Integer.toString(value));
  }

  @Override
  public int getInt(String key, int def) {
    int result = def;
    String value = getInternal(key);
    if (value != null) {
      try {
        result = Integer.parseInt(value);
      }
      catch (NumberFormatException e) {
        LOG.warn("Invalid int-value for property '{}' configured: {}", key, value);
      }
    }
    return result;
  }

  @Override
  public boolean putLong(String key, long value) {
    return putInternal(key, Long.toString(value));
  }

  @Override
  public long getLong(String key, long def) {
    long result = def;
    String value = getInternal(key);
    if (value != null) {
      try {
        result = Long.parseLong(value);
      }
      catch (NumberFormatException e) {
        LOG.warn("Invalid long-value for property '{}' configured: {}", key, value);
      }
    }
    return result;
  }

  @Override
  public boolean putBoolean(String key, boolean value) {
    return putInternal(key, String.valueOf(value));
  }

  @Override
  public boolean getBoolean(String key, boolean def) {
    boolean result = def;
    String value = getInternal(key);
    if (value != null) {
      if ("true".equalsIgnoreCase(value)) {
        result = true;
      }
      else if ("false".equalsIgnoreCase(value)) {
        result = false;
      }
    }
    return result;
  }

  @Override
  public boolean putFloat(String key, float value) {
    return putInternal(key, Float.toString(value));
  }

  @Override
  public float getFloat(String key, float def) {
    float result = def;
    String value = getInternal(key);
    if (value != null) {
      try {
        result = Float.parseFloat(value);
      }
      catch (NumberFormatException e) {
        LOG.warn("Invalid float-value for property '{}' configured: {}", key, value);
      }
    }
    return result;
  }

  @Override
  public boolean putDouble(String key, double value) {
    return putInternal(key, Double.toString(value));
  }

  @Override
  public double getDouble(String key, double def) {
    double result = def;
    String value = getInternal(key);
    if (value != null) {
      try {
        result = Double.parseDouble(value);
      }
      catch (NumberFormatException e) {
        LOG.warn("Invalid double-value for property '{}' configured: {}", key, value);
      }
    }
    return result;
  }

  @Override
  public boolean putByteArray(String key, byte[] value) {
    return putInternal(key, Base64Utility.encode(value));
  }

  @Override
  public byte[] getByteArray(String key, byte[] def) {
    byte[] result = def;
    String value = getInternal(key);
    if (value != null) {
      result = Base64Utility.decode(value);
    }
    return result;
  }

  protected synchronized String getInternal(String key) {
    if (key == null) {
      throw new IllegalArgumentException("null key is not allowed.");
    }
    return m_prefs.get(key);
  }

  protected synchronized boolean putInternal(String key, String value) {
    if (key == null) {
      throw new IllegalArgumentException("null key is not allowed.");
    }

    boolean isRemove = value == null;
    String oldValue = null;
    if (isRemove) {
      oldValue = m_prefs.remove(key);
    }
    else {
      oldValue = m_prefs.put(key, value);
    }

    if (!CompareUtility.equals(oldValue, value)) {
      m_dirty = true;

      // listener notification
      PreferenceChangeEvent event = null;
      if (isRemove) {
        event = new PreferenceChangeEvent(this, EVENT_KIND_REMOVE, key, oldValue, null);
      }
      else {
        boolean isUpdate = oldValue != null;
        if (isUpdate) {
          event = new PreferenceChangeEvent(this, EVENT_KIND_CHANGE, key, oldValue, value);
        }
        else {
          event = new PreferenceChangeEvent(this, EVENT_KIND_ADD, key, null, value);
        }
      }
      fireEvent(event);
      return true;
    }
    return false;
  }

  protected void fireEvent(PreferenceChangeEvent event) {
    for (IPreferenceChangeListener listener : m_eventListeners.getListeners(IPreferenceChangeListener.class)) {
      try {
        listener.preferenceChange(event);
      }
      catch (Exception e) {
        LOG.error("Error on event listener notification for listener '" + listener.getClass().getName() + "'.", e);
      }
    }
  }

  protected boolean isDirty() {
    return m_dirty;
  }

  protected void setDirty(boolean dirty) {
    m_dirty = dirty;
  }

  @Override
  public synchronized Set<String> keys() {
    return new LinkedHashSet<String>(m_prefs.keySet());
  }

  @Override
  public String name() {
    return m_name;
  }

  @Override
  public ISession userScope() {
    return m_session;
  }

  @Override
  public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
    m_eventListeners.add(IPreferenceChangeListener.class, listener);
  }

  @Override
  public void removePreferenceChangeListener(IPreferenceChangeListener listener) {
    m_eventListeners.remove(IPreferenceChangeListener.class, listener);
  }

  @Override
  public synchronized int size() {
    return m_prefs.size();
  }
}