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
package org.eclipse.scout.rt.ui.swing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;

import javax.security.auth.Subject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.scout.commons.LocaleUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.security.SimplePrincipal;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.services.common.exceptionhandler.ErrorHandler;
import org.eclipse.scout.rt.client.services.common.exceptionhandler.UserInterruptedException;
import org.eclipse.scout.rt.platform.IApplication;
import org.eclipse.scout.rt.platform.Platform;
import org.eclipse.scout.rt.shared.ui.UiDeviceType;
import org.eclipse.scout.rt.shared.ui.UiLayer;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.eclipse.scout.rt.ui.swing.ext.job.SwingProgressHandler;
import org.eclipse.scout.rt.ui.swing.splash.SplashProgressMonitor;

/**
 * This abstract class is the base class for all Scout Swing (Eclipse) applications.
 *
 * @author awe
 */
abstract class BaseSwingApplication implements IApplication {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(BaseSwingApplication.class);

  private SplashProgressMonitor m_monitor;

  void initialize() {
    if (!Platform.isDevelopmentMode()) {
      try {
        File sysoutRedirector = File.createTempFile(System.getProperty("user.name", "anonymous") + "-sysout", ".log", new File("systemLogFiels"));
        File syserrRedirector = File.createTempFile(System.getProperty("user.name", "anonymous") + "-syserr", ".log", new File("systemLogFiels"));
        sysoutRedirector.getParentFile().mkdirs();
        syserrRedirector.getParentFile().mkdirs();
        System.setOut(new PrintStream(new FileOutputStream(sysoutRedirector, true)));
        System.setErr(new PrintStream(new FileOutputStream(syserrRedirector, true)));
      }
      catch (Throwable t) {
        // nop
      }
    }
    try {
//      org.eclipse.scout.rt.platform.Platform.get().start();

      // The default @{link Locale} has to be set prior to SwingEnvironment is created, because UIDefaultsInjector resolves NLS texts.
      execInitLocale();

      //attach default job handler
      SwingProgressHandler.install();

      SwingUtilities.invokeAndWait(
          new Runnable() {
            @Override
            public void run() {
              initializeSwing();
            }
          }
          );
    }
    catch (Exception e) {
      LOG.warn(null, e);
      System.exit(0);
    }
    m_monitor = new SplashProgressMonitor(getSwingEnvironment(), showSplashScreenProgressInPercentage());

    m_monitor.showSplash();
  }

  /**
   * This method is used to perform initialization in the Swing thread.
   */
  abstract void initializeSwing();

  /**
   * Returns the Swing environment used to display the splash screen.
   *
   * @return
   */
  abstract ISwingEnvironment getSwingEnvironment();

  /**
   * @return Returns <code>true</code> if the splash screen shows the current status in percentage as well (if required
   *         data is available). Default is <code>false</code>.
   */
  protected boolean showSplashScreenProgressInPercentage() {
    return false;
  }

  public final SplashProgressMonitor getProgressMonitor() {
    return m_monitor;
  }

  /**
   * This abstract template application creates a JAAS subject based on the system property "user.name"
   * <p>
   * The start is then delegated to {@link #startInSubject(IApplicationContext)}
   * <p>
   * Normally {@link #startInSubject(IApplicationContext)} is overridden
   */
  @Override
  public void start() throws Exception {
    if (Platform.isOsgiRunning()) {
      return;
    }
    if (Subject.getSubject(AccessController.getContext()) != null) {
      // there is a subject context
      startInSubject();
      exit();
    }
    else {
      Subject subject = new Subject();
      subject.getPrincipals().add(new SimplePrincipal(System.getProperty("user.name")));
      Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
        @Override
        public Object run() throws Exception {
          startInSubject();
          exit();
          return null;
        }
      });
    }
  }

  abstract void startInSubject() throws Exception;

  /**
   * Exit delegate to handle os-specific exit behavior.
   * <p>
   * Mac OS X normally only closes the window, but we want to close the app (with Quit).
   */
  protected void exit() {
//    if (Platform.OS_MACOSX.equals(Platform.getOS())) {
//      System.exit(0);
//    }
  }

  protected void execInitLocale() {
    String[] localeParams = new String[]{"locale", "nl", "osgi.nl.user", "osgi.nl"};
    String localeProp = null;
    for (int i = 0; i < localeParams.length && localeProp == null; i++) {
      localeProp = System.getProperty(localeParams[i], null);
    }

    Locale locale = LocaleUtility.parse(localeProp);
    if (locale != null) {
      Locale.setDefault(locale);
    }
  }

  protected UserAgent initUserAgent() {
    return UserAgent.create(UiLayer.SWING, UiDeviceType.DESKTOP);
  }

  boolean isClientSessionValid(IClientSession clientSession) {
    if (clientSession.isActive()) {
      return true;
    }
    showLoadError(clientSession.getLoadError());
    return false;
  }

  protected void stopSplashScreen() {
    m_monitor.done();
    m_monitor = null;
  }

  protected void showLoadError(Throwable error) {
    ErrorHandler handler = new ErrorHandler(error);
    if (!(handler.getText().indexOf(UserInterruptedException.class.getSimpleName()) >= 0)) {
      SwingUtility.showMessageDialogSynthCapable(
          null,
          StringUtility.join("\n\n", handler.getText(), handler.getDetail()),
          handler.getTitle(),
          JOptionPane.ERROR_MESSAGE
          );
    }
  }

}
