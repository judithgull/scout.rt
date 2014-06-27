/**
 *
 */
package org.eclipse.scout.rt.ui.rap.action;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.scout.commons.OptimisticLock;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.keystroke.KeyStroke;
import org.eclipse.scout.rt.client.ui.action.view.IViewButton;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.SWT;

public class RwtScoutToolbarAction extends Action {
  private static final long serialVersionUID = 1L;

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(RwtScoutToolbarAction.class);

  private IAction m_scoutAction;
  private IRwtEnvironment m_swtEnvironment;
  private final OptimisticLock m_updateSwtFromScoutLock;
  private boolean m_updateUi = true;

  private P_ScoutPropertyChangeListener m_scoutPropertyListener;

  private IToolBarManager m_toolbarMananger;

  public RwtScoutToolbarAction(IAction scoutAction, IToolBarManager manager, IRwtEnvironment environment) {
    super((scoutAction.getText() == null) ? (" ") : scoutAction.getText(), transformScoutStyle(scoutAction));
    m_toolbarMananger = manager;
    m_swtEnvironment = environment;
    m_updateSwtFromScoutLock = new OptimisticLock();
    m_scoutAction = scoutAction;
    setId(getScoutObject().getActionId());
    attachScout();
    m_scoutAction.addPropertyChangeListener(new P_ScoutPropertyChangeListener());
  }

  private static int transformScoutStyle(IAction scoutAction) {
    if (scoutAction.isToggleAction()) {
      return SWT.TOGGLE;
    }
    return AS_PUSH_BUTTON;
  }

  protected void attachScout() {
    try {
      setUpdateUi(false);
      updateEnabledFromScout();
      updateIconFromScout();
      updateKeystrokeFromScout();
      updateSelectedFromScout();
      updateTextFromScout();
      updateTooltipTextFromScout();
    }
    finally {
      setUpdateUi(true);
    }
  }

  /**
   * @return the swtEnvironment
   */
  public IRwtEnvironment getEnvironment() {
    return m_swtEnvironment;
  }

  protected IAction getScoutObject() {
    return m_scoutAction;
  }

  /**
   * @param updateUi
   *          the updateUi to set
   */
  public void setUpdateUi(boolean updateUi) {
    if (updateUi != m_updateUi) {
      m_updateUi = updateUi;
      if (updateUi) {
        m_toolbarMananger.update(true);
      }
    }
  }

  /**
   * @return the updateUi
   */
  public boolean isUpdateUi() {
    return m_updateUi;
  }

  protected void updateEnabledFromScout() {
    setEnabled(getScoutObject().isEnabled());
    if (isUpdateUi()) {
      m_toolbarMananger.update(true);
    }
  }

  protected void updateIconFromScout() {
    setImageDescriptor(getEnvironment().getImageDescriptor(getScoutObject().getIconId()));
    if (isUpdateUi()) {
      m_toolbarMananger.update(true);
    }
  }

  protected void updateKeystrokeFromScout() {
    String keyStroke = getScoutObject().getKeyStroke();
    if (keyStroke != null) {
      int keyCode = RwtUtility.getRwtKeyCode(new KeyStroke(keyStroke));
      int stateMask = RwtUtility.getRwtStateMask(new KeyStroke(keyStroke));
      setAccelerator(stateMask | keyCode);
    }
    else {
      setAccelerator(SWT.NONE);
    }
    if (isUpdateUi()) {
      m_toolbarMananger.update(true);
    }
  }

  protected void updateTextFromScout() {

    setText(getScoutObject().getText());
    if (isUpdateUi()) {
      m_toolbarMananger.update(true);
    }
  }

  protected void updateTooltipTextFromScout() {
    setToolTipText(getScoutObject().getTooltipText());
    if (isUpdateUi()) {
      m_toolbarMananger.update(true);
    }
  }

  protected void updateSelectedFromScout() {
    setChecked(getScoutObject().isSelected());
    if (isUpdateUi()) {
      m_toolbarMananger.update(true);
    }
  }

  protected void updateVisibleFromScout() {
    LOG.warn("set visible on SWT action is not supported");
  }

  @Override
  public void run() {
    handleSwtAction();
  }

  protected void handleSwtAction() {
    try {
      if (getUpdateSwtFromScoutLock().acquire()) {
        if (getScoutObject().isToggleAction() && getScoutObject() instanceof IViewButton && getScoutObject().isSelected()) {
          // reset UI selection
          updateSelectedFromScout();
        }
        else {
          Runnable t = new Runnable() {
            @Override
            public void run() {
//              if (getScoutObject().isToggleAction()) {
//                if (getScoutObject() instanceof IViewButton && getScoutObject().isSelected()) {
//                  // void
//                }
//                else {
//                  getScoutObject().getUIFacade().setSelectedFromUI(!getScoutObject().isSelected());
//                }
//              }

              getScoutObject().getUIFacade().fireActionFromUI();
            }
          };
          getEnvironment().invokeScoutLater(t, 0);
        }
      }
    }
    finally {
      getUpdateSwtFromScoutLock().release();
    }
  }

  /**
   * @return the lock used in the Swt thread when applying scout changes
   */
  public OptimisticLock getUpdateSwtFromScoutLock() {
    return m_updateSwtFromScoutLock;
  }

  protected void handleScoutPropertyChange(String propertyName, Object newValue) {
    if (IAction.PROP_ENABLED.equals(propertyName)) {
      updateEnabledFromScout();
    }
    else if (IAction.PROP_ICON_ID.equals(propertyName)) {
      updateIconFromScout();
    }
    else if (IAction.PROP_KEYSTROKE.equals(propertyName)) {
      updateKeystrokeFromScout();
    }
    else if (IAction.PROP_SELECTED.equals(propertyName)) {
      updateSelectedFromScout();
    }
    else if (IAction.PROP_TEXT.equals(propertyName)) {
      updateTextFromScout();
    }
    else if (IAction.PROP_TOOLTIP_TEXT.equals(propertyName)) {
      updateTooltipTextFromScout();
    }
    else if (IAction.PROP_VISIBLE.equals(propertyName)) {
      updateVisibleFromScout();
    }

  }

  private class P_ScoutPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      Runnable t = new Runnable() {
        @Override
        public void run() {
          try {
            getUpdateSwtFromScoutLock().acquire();
            //
            handleScoutPropertyChange(evt.getPropertyName(), evt.getNewValue());
          }
          finally {
            getUpdateSwtFromScoutLock().release();
          }
        }

      };
      getEnvironment().invokeUiLater(t);

    }
  }

}
