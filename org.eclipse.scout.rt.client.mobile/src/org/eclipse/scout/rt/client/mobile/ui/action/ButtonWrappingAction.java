package org.eclipse.scout.rt.client.mobile.ui.action;

import java.beans.PropertyChangeEvent;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.mobile.ui.form.AbstractMobileAction;
import org.eclipse.scout.rt.client.ui.action.tree.IActionNode;
import org.eclipse.scout.rt.client.ui.form.fields.button.IButton;

/**
 * A {@link IActionNode} which wraps a {@link IButton}. <br/>
 * {@link PropertyChangeEvent}s fired by the button are delegated
 * to the action
 * 
 * @since 3.9.0
 */
public class ButtonWrappingAction extends AbstractMobileAction {
  private ButtonToActionPropertyDelegator m_propertyDelegator;

  public ButtonWrappingAction(IButton wrappedButton) {
    super(false);

    m_propertyDelegator = new ButtonToActionPropertyDelegator(wrappedButton, this);

    callInitializer();
  }

  @Override
  protected void initConfig() {
    super.initConfig();

    m_propertyDelegator.init();
  }

  @Override
  protected void execAction() throws ProcessingException {
    getWrappedButton().doClick();
  }

  @Override
  protected void execSelectionChanged(boolean selection) throws ProcessingException {
    getWrappedButton().setSelected(selection);
  }

  public IButton getWrappedButton() {
    return m_propertyDelegator.getSender();
  }

}
