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
package org.eclipse.scout.rt.ui.swt.window.popup;

import java.util.EventListener;

import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.ui.swt.ISwtEnvironment;
import org.eclipse.scout.rt.ui.swt.extension.UiDecorationExtensionPoint;
import org.eclipse.scout.rt.ui.swt.form.ISwtScoutForm;
import org.eclipse.scout.rt.ui.swt.util.SwtUtility;
import org.eclipse.scout.rt.ui.swt.window.ISwtScoutPart;
import org.eclipse.scout.rt.ui.swt.window.SwtScoutPartEvent;
import org.eclipse.scout.rt.ui.swt.window.SwtScoutPartListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.Form;

/**
 * Popup window bound to a component (ownerComponent). The popup closes when
 * there is either a click outside this window or the component loses focus
 * (focusComponent), or the component becomes invisible.
 */
public class SwtScoutPopup implements ISwtScoutPart {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(SwtScoutPopup.class);
  public static final String PROP_POPUP_OWNER = "propPopupOwner";

  private ISwtEnvironment m_env;
  private Control m_ownerComponent;
  private Rectangle m_ownerBounds;
  private Shell m_swtWindow;
  private Composite m_swtWindowContentPane;
  private EventListenerList m_listenerList;
  private IForm m_scoutForm;
  private boolean m_positionBelowReferenceField;
  private boolean m_opened;
  private boolean m_popupOnField;

  private int m_widthHint;
  private int m_heightHint;
  private int m_maxHeightHint;
  private int m_maxWidthHint;

  private ISwtScoutForm m_uiForm;

  public SwtScoutPopup(ISwtEnvironment env, Control ownerComponent, Rectangle ownerBounds, int style) {
    m_env = env;
    m_positionBelowReferenceField = true;
    m_ownerComponent = ownerComponent;
    m_ownerBounds = ownerBounds;
    m_listenerList = new EventListenerList();

    m_widthHint = SWT.DEFAULT;
    m_heightHint = SWT.DEFAULT;
    m_maxHeightHint = SWT.DEFAULT;
    m_maxWidthHint = SWT.DEFAULT;

    m_swtWindow = new Shell(ownerComponent.getShell(), style);
    m_swtWindow.setData("extendedStyle", SWT.POP_UP);
    m_swtWindow.setLayout(new FillLayout());
    m_swtWindow.addDisposeListener(new P_SwtWindowDisposeListener());

    // content pane
    m_swtWindowContentPane = env.getFormToolkit().createComposite(m_swtWindow, SWT.NONE);
    m_swtWindowContentPane.setLayout(new FillLayout());
  }

  @Override
  public void setBusy(boolean b) {
    //nop
  }

  public Shell getShell() {
    return m_swtWindow;
  }

  public void setBounds(Rectangle bounds) {
    getShell().setBounds(bounds);
    getShell().layout(true, true);
  }

  public boolean isPopupOnField() {
    return m_popupOnField;
  }

  public void setPopupOnField(boolean popupOnField) {
    m_popupOnField = popupOnField;
  }

  public boolean isPopupBelow() {
    return m_positionBelowReferenceField;
  }

  public int getWidthHint() {
    return m_widthHint;
  }

  public void setWidthHint(int widthHint) {
    if (widthHint > 0) {
      m_widthHint = widthHint;
    }
    else {
      m_widthHint = SWT.DEFAULT;
    }
  }

  public int getHeightHint() {
    return m_heightHint;
  }

  public void setHeightHint(int heightHint) {
    if (heightHint > 0) {
      m_heightHint = heightHint;
    }
    else {
      m_heightHint = SWT.DEFAULT;
    }
  }

  public int getMaxHeightHint() {
    return m_maxHeightHint;
  }

  public void setMaxHeightHint(int maxHeightHint) {
    if (maxHeightHint > 0) {
      m_maxHeightHint = maxHeightHint;
    }
    else {
      m_maxHeightHint = SWT.DEFAULT;
    }
  }

  public int getMaxWidthHint() {
    return m_maxWidthHint;
  }

  public void setMaxWidthHint(int maxWidthHint) {
    if (maxWidthHint > 0) {
      m_maxWidthHint = maxWidthHint;
    }
    else {
      m_maxWidthHint = SWT.DEFAULT;
    }
  }

  public void showForm(IForm scoutForm) throws ProcessingException {
    m_opened = true;
    if (m_scoutForm == null) {
      m_scoutForm = scoutForm;
      m_uiForm = m_env.createForm(getSwtContentPane(), scoutForm);
      autoAdjustBounds();
      if (m_opened) {
        handleSwtWindowOpening();
        //open and activate, do NOT just call setVisible(true)
        m_swtWindow.open();
        autoAdjustBounds();
        if (m_opened) {
          handleSwtWindowOpened();
        }
      }
    }
    else {
      throw new ProcessingException("The popup is already open. The form '" + scoutForm.getTitle() + " (" + scoutForm.getClass().getName() + ")' can not be opened!");
    }

  }

  @Override
  public void closePart() {
    m_opened = false;
    try {
      if (!m_swtWindow.isDisposed()) {
        m_swtWindow.setVisible(false);
        m_swtWindow.dispose();
      }
    }
    catch (Throwable t) {
      LOG.error("Failed closing popup for " + m_scoutForm, t);
    }
  }

  @Override
  public IForm getForm() {
    return m_scoutForm;
  }

  @Override
  public Form getSwtForm() {
    return null;
  }

  @Override
  public ISwtScoutForm getUiForm() {
    return m_uiForm;
  }

  public void autoAdjustBounds() {
    if (getShell().isDisposed()) {
      return;
    }
    if (m_ownerComponent.isDisposed()) {
      LOG.warn("Unexpected: Owner component of popup is disposed");
      return;
    }
    //invalidate all layouts
    Point dim = getShell().computeSize(m_widthHint, m_heightHint, true);

    // adjust width
    dim.x = Math.max(dim.x, UiDecorationExtensionPoint.getLookAndFeel().getLogicalGridLayoutDefaultColumnWidth());
    if (m_maxWidthHint != SWT.DEFAULT) {
      dim.x = Math.min(dim.x, m_maxWidthHint);
    }
    // adjust height
    if (m_maxHeightHint != SWT.DEFAULT) {
      dim.y = Math.min(dim.y, m_maxHeightHint);
    }

    Point p = m_ownerComponent.toDisplay(new Point(-m_ownerComponent.getBorderWidth(), 0));
    Point above = new Point(p.x, p.y);
    if (m_popupOnField) {
      above.y += m_ownerComponent.getBounds().height;
    }

    Rectangle aboveView = SwtUtility.intersectRectangleWithScreen(getShell().getDisplay(), new Rectangle(above.x, above.y - dim.y, dim.x, dim.y), false, false);
    Point below = new Point(p.x, p.y);
    if (!m_popupOnField) {
      below.y += m_ownerComponent.getBounds().height;
    }

    Rectangle belowView = SwtUtility.intersectRectangleWithScreen(getShell().getDisplay(), new Rectangle(below.x, below.y, dim.x, dim.y), false, false);
    // decide based on the preference positionBelowReferenceField
    Rectangle currentView = m_positionBelowReferenceField ? belowView : aboveView;
    Rectangle alternateView = m_positionBelowReferenceField ? aboveView : belowView;
    if (currentView.height >= alternateView.height) {
      getShell().setBounds(currentView);
    }
    else {
      getShell().setBounds(alternateView);
      // toggle preference
      m_positionBelowReferenceField = !m_positionBelowReferenceField;
    }
  }

  public Composite getSwtContentPane() {
    return m_swtWindowContentPane;
  }

  public void addSwtScoutPartListener(SwtScoutPartListener listener) {
    m_listenerList.add(SwtScoutPartListener.class, listener);
  }

  public void removeSwtScoutPartListener(SwtScoutPartListener listener) {
    m_listenerList.remove(SwtScoutPartListener.class, listener);
  }

  protected void fireSwtScoutPartEvent(SwtScoutPartEvent e) {
    if (m_swtWindow != null) {
      EventListener[] listeners = m_listenerList.getListeners(SwtScoutPartListener.class);
      if (listeners != null && listeners.length > 0) {
        for (EventListener listener : listeners) {
          try {
            ((SwtScoutPartListener) listener).partChanged(e);
          }
          catch (Throwable t) {
            LOG.error("Unexpected:", t);
          }
        }
      }
    }
  }

  @Override
  public boolean isVisible() {
    return m_swtWindow != null && m_swtWindow.getVisible();
  }

  @Override
  public void activate() {
    m_swtWindow.getShell().setActive();
  }

  @Override
  public boolean isActive() {
    return m_swtWindow != null && m_swtWindow.getDisplay().getActiveShell() == m_swtWindow;
  }

  @Override
  public void setStatusLineMessage(Image image, String message) {
    // void
  }

  protected void handleSwtWindowOpening() {
    fireSwtScoutPartEvent(new SwtScoutPartEvent(SwtScoutPopup.this, SwtScoutPartEvent.TYPE_OPENING));
  }

  protected void handleSwtWindowOpened() {
    fireSwtScoutPartEvent(new SwtScoutPartEvent(SwtScoutPopup.this, SwtScoutPartEvent.TYPE_OPENED));
    fireSwtScoutPartEvent(new SwtScoutPartEvent(SwtScoutPopup.this, SwtScoutPartEvent.TYPE_ACTIVATED));
  }

  protected Control getOwnerComponent() {
    return m_ownerComponent;
  }

  protected void handleSwtWindowClosed() {
    fireSwtScoutPartEvent(new SwtScoutPartEvent(SwtScoutPopup.this, SwtScoutPartEvent.TYPE_CLOSED));
    Runnable job = new Runnable() {
      @Override
      public void run() {
        m_scoutForm.getUIFacade().fireFormKilledFromUI();
      }
    };
    m_env.invokeScoutLater(job, 0);
  }

  private class P_SwtWindowDisposeListener implements DisposeListener {
    @Override
    public void widgetDisposed(DisposeEvent e) {
      handleSwtWindowClosed();
    }
  }// end private class

}
