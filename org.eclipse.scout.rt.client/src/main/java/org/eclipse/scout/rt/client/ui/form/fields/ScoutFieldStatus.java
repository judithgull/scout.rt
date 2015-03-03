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
package org.eclipse.scout.rt.client.ui.form.fields;

import org.eclipse.scout.commons.status.IStatus;
import org.eclipse.scout.commons.status.Status;
import org.eclipse.scout.rt.client.IFieldStatus;
import org.eclipse.scout.rt.shared.AbstractIcons;

/**
 * Status type for form fields with additional property "iconId"
 */
public class ScoutFieldStatus extends Status implements IFieldStatus {
  private static final long serialVersionUID = 1L;
  private final String m_iconId;

  public ScoutFieldStatus(String message, int severity) {
    this(message, null, severity);
  }

  public ScoutFieldStatus(String message, String iconId, int severity) {
    this(message, iconId, severity, 0);
  }

  public ScoutFieldStatus(String message, int severity, int code) {
    this(message, null, severity, code);
  }

  public ScoutFieldStatus(String message, String iconId, int severity, int code) {
    super(message, severity, code);
    m_iconId = iconId;
  }

  /**
   * icon id are defined either in {@link AbstractIcons} or in the project
   * specific subclass named Icons
   */
  @Override
  public String getIconId() {
    if (m_iconId != null) {
      return m_iconId;
    }
    else {
      return getIconIdFromSeverity(getSeverity());
    }
  }

  public static String getIconIdFromSeverity(int severity) {
    if (severity >= IStatus.ERROR) {
      return AbstractIcons.StatusError;
    }
    else if (severity >= IStatus.WARNING) {
      return AbstractIcons.StatusWarning;
    }
    else {
      return AbstractIcons.StatusInfo;
    }
  }

}