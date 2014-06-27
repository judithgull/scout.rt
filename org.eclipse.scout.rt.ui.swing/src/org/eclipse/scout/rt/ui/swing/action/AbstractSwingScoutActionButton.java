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
package org.eclipse.scout.rt.ui.swing.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.SwingConstants;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.ui.swing.ISwingEnvironment;
import org.eclipse.scout.rt.ui.swing.SwingUtility;
import org.eclipse.scout.rt.ui.swing.basic.IconGroup;
import org.eclipse.scout.rt.ui.swing.basic.IconGroup.IconState;
import org.eclipse.scout.rt.ui.swing.basic.SwingScoutComposite;

/**
 * Composition between a scout IButton and a swing
 * JButton/JToggleButton/JRadioButton
 */
public abstract class AbstractSwingScoutActionButton<T extends IAction> extends SwingScoutComposite<T> implements ISwingScoutAction<T> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractSwingScoutActionButton.class);

  //ticket 86811: avoid double-action in queue
  private boolean m_handleActionPending;

  public AbstractSwingScoutActionButton() {
  }

  @Override
  protected void initializeSwing() {
    AbstractButton swingButton = createButton(getSwingEnvironment());
    SwingUtility.installDefaultFocusHandling(swingButton);
    swingButton.setHorizontalTextPosition(SwingConstants.RIGHT);
    swingButton.setVerifyInputWhenFocusTarget(true);
    swingButton.setRequestFocusEnabled(true);
    setSwingField(swingButton);
    // attach swing listeners
    swingButton.addActionListener(new P_SwingActionListener());
  }

  @Override
  public Action getSwingAction() {
    return null;
  }

  @Override
  public AbstractButton getSwingField() {
    return (AbstractButton) super.getSwingField();
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    IAction b = getScoutObject();
    setVisibleFromScout(b.isVisible());
    setEnabledFromScout(b.isEnabled());
    setTextFromScout(b.getText());
    setTooltipTextFromScout(b.getTooltipText());
    setIconIdFromScout(b.getIconId());
    setSelectionFromScout(b.isSelected());
  }

  protected void setIconIdFromScout(String iconId) {
    if (iconId == null) {
      getSwingField().setIcon(null);
      getSwingField().setDisabledIcon(null);
      getSwingField().setPressedIcon(null);
      getSwingField().setSelectedIcon(null);
      getSwingField().setRolloverIcon(null);
    }
    else {
      IconGroup iconGroup = new IconGroup(getSwingEnvironment(), iconId);
      getSwingField().setIcon(iconGroup.getIcon(IconState.NORMAL));
      if (iconGroup.hasIcon(IconState.DISABLED)) {
        getSwingField().setDisabledIcon(iconGroup.getIcon(IconState.DISABLED));
      }
      if (iconGroup.hasIcon(IconState.SELECTED)) {
        getSwingField().setSelectedIcon(iconGroup.getIcon(IconState.SELECTED));
      }
      if (iconGroup.hasIcon(IconState.ROLLOVER)) {
        getSwingField().setRolloverIcon(iconGroup.getIcon(IconState.ROLLOVER));
      }
    }
  }

  protected void setTextFromScout(String s) {
    AbstractButton b = getSwingField();
    String label = StringUtility.removeMnemonic(s);
    b.setText(label);
    if (StringUtility.getMnemonic(s) != 0x0) {
      b.setMnemonic(StringUtility.getMnemonic(s));
    }
  }

  protected void setVisibleFromScout(boolean b) {
    getSwingField().setVisible(b);
  }

  protected void setEnabledFromScout(boolean b) {
    getSwingField().setEnabled(b);
  }

  protected void setSelectionFromScout(boolean b) {
    getSwingField().setSelected(b);
  }

  protected void setTooltipTextFromScout(String s) {
    s = SwingUtility.createHtmlLabelText(s, true);
    if (getSwingField() != null) {
      getSwingField().setToolTipText(s);
    }
  }

  /**
   * in swing thread
   */
  @Override
  protected void handleScoutPropertyChange(String name, Object newValue) {
    super.handleScoutPropertyChange(name, newValue);
    if (name.equals(IAction.PROP_ENABLED)) {
      setEnabledFromScout(((Boolean) newValue).booleanValue());
    }
    else if (name.equals(IAction.PROP_TEXT)) {
      setTextFromScout((String) newValue);
    }
    else if (name.equals(IAction.PROP_TOOLTIP_TEXT)) {
      setTooltipTextFromScout((String) newValue);
    }
    else if (name.equals(IAction.PROP_VISIBLE)) {
      setVisibleFromScout(((Boolean) newValue).booleanValue());
    }
    else if (name.equals(IAction.PROP_ICON_ID)) {
      setIconIdFromScout((String) newValue);
    }
    else if (name.equals(IAction.PROP_SELECTED)) {
      setSelectionFromScout(((Boolean) newValue).booleanValue());
    }
  }

  protected void handleSwingAction(ActionEvent e) {
    if (SwingUtility.runInputVerifier()) {
      if (!m_handleActionPending) {
        m_handleActionPending = true;
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            try {
              getScoutObject().getUIFacade().fireActionFromUI();
            }
            finally {
              m_handleActionPending = false;
            }
          }
        };
        getSwingEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
  }

  protected abstract AbstractButton createButton(ISwingEnvironment env);

  /*
   * Listeners
   */
  private class P_SwingActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      handleSwingAction(e);
    }
  }// end class

}
