/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Daniel Wiehl (BSI Business Systems Integration AG) - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.jaxws;

import org.eclipse.scout.commons.ConfigIniUtility;
import org.eclipse.scout.rt.platform.IModule;
import org.eclipse.scout.rt.platform.cdi.ApplicationScoped;

import com.sun.xml.internal.ws.fault.SOAPFaultBuilder;

@SuppressWarnings("restriction")
@ApplicationScoped
public class JaxWs216Module implements IModule {

  public static final String PROP_STACKTRACE = "org.eclipse.scout.jaxws.stacktrace";
  public static final String PROP_DEFAULT_PRINCIPAL = "org.eclipse.scout.jaxws.txhandler.sessionfactory.principal";
  public static final String PROP_PUBLISH_STATUS_PAGE = "org.eclipse.scout.jaxws.publish_status_page";

  @Override
  public void start() {
    // apply properties
    boolean stacktraceEnabled = ConfigIniUtility.getPropertyBoolean(PROP_STACKTRACE, false);
    if (!stacktraceEnabled) {
      System.setProperty(SOAPFaultBuilder.class.getName() + ".disableCaptureStackTrace", "false");
    }
  }

  @Override
  public void stop() {
  }
}