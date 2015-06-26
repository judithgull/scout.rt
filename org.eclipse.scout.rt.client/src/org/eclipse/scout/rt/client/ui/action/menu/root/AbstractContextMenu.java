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
package org.eclipse.scout.rt.client.ui.action.menu.root;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.beans.IPropertyObserver;
import org.eclipse.scout.commons.holders.BooleanHolder;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.extension.ui.action.menu.root.IContextMenuExtension;
import org.eclipse.scout.rt.client.ui.action.ActionUtility;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.IActionFilter;
import org.eclipse.scout.rt.client.ui.action.IActionVisitor;
import org.eclipse.scout.rt.client.ui.action.menu.AbstractMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenuType;

/**
 *
 */
public abstract class AbstractContextMenu extends AbstractMenu implements IContextMenu {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractMenu.class);

  private final EventListenerList m_listeners = new EventListenerList();
  private final IPropertyObserver m_owner;

  private PropertyChangeListener m_menuVisibilityListener = new P_VisibilityOfMenuItemChangedListener();

  public AbstractContextMenu(IPropertyObserver owner, List<? extends IMenu> initialChildList) {
    this(owner, initialChildList, true);
  }

  public AbstractContextMenu(IPropertyObserver owner, List<? extends IMenu> initialChildList, boolean callInitializer) {
    super(false);
    m_owner = owner;
    if (callInitializer) {
      callInitializer();
    }
    setChildActions(initialChildList);
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    calculateLocalVisibility();
  }

  @Override
  public IPropertyObserver getOwner() {
    return m_owner;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<? extends IMenuType> getCurrentMenuTypes() {
    return (Set<? extends IMenuType>) propertySupport.getProperty(PROP_CURRENT_MENU_TYPES);
  }

  protected void setCurrentMenuTypes(Set<? extends IMenuType> menuTypes) {
    propertySupport.setProperty(PROP_CURRENT_MENU_TYPES, menuTypes);
  }

  @Override
  public void addContextMenuListener(ContextMenuListener listener) {
    m_listeners.add(ContextMenuListener.class, listener);
  }

  @Override
  public void removeContextMenuListener(ContextMenuListener listener) {
    m_listeners.remove(ContextMenuListener.class, listener);
  }

  protected void fireContextMenuEvent(ContextMenuEvent event) {
    for (ContextMenuListener l : m_listeners.getListeners(ContextMenuListener.class)) {
      try {
        l.contextMenuChanged(event);
      }
      catch (Exception e) {
        LOG.error("Error during listener notification '" + l + "'.", e);
      }
    }
  }

  @Override
  public void addChildActions(Collection<? extends IMenu> actionList) {
    super.addChildActions(actionList);
  }

  @Override
  protected void afterChildMenusAdd(Collection<? extends IMenu> newChildMenus) {
    super.afterChildMenusAdd(newChildMenus);
    addScoutMenuVisibilityListenerRec(newChildMenus);
    calculateLocalVisibility();
    fireContextMenuEvent(new ContextMenuEvent(this, ContextMenuEvent.TYPE_STRUCTURE_CHANGED));
  }

  @Override
  protected void afterChildMenusRemove(Collection<? extends IMenu> childMenusToRemove) {
    super.afterChildMenusRemove(childMenusToRemove);
    removeScoutMenuVisibilityListenerRec(childMenusToRemove);
    calculateLocalVisibility();
    fireContextMenuEvent(new ContextMenuEvent(this, ContextMenuEvent.TYPE_STRUCTURE_CHANGED));
  }

  /**
   * @param oldValue
   * @param newValue
   */
  protected void handleChildActionsChanged(List<IMenu> oldValue, List<IMenu> newValue) {
    removeScoutMenuVisibilityListenerRec(oldValue);
    addScoutMenuVisibilityListenerRec(newValue);
    fireContextMenuEvent(new ContextMenuEvent(this, ContextMenuEvent.TYPE_STRUCTURE_CHANGED));
  }

  protected void addScoutMenuVisibilityListenerRec(Collection<? extends IMenu> menus) {
    if (menus != null) {
      for (IMenu m : menus) {
        m.addPropertyChangeListener(IMenu.PROP_CHILD_ACTIONS, m_menuVisibilityListener);
        m.addPropertyChangeListener(IMenu.PROP_VISIBLE, m_menuVisibilityListener);
        addScoutMenuVisibilityListenerRec(m.getChildActions());
      }
    }
  }

  protected void removeScoutMenuVisibilityListenerRec(Collection<? extends IMenu> menus) {
    if (menus != null) {
      for (IMenu m : menus) {
        m.removePropertyChangeListener(IMenu.PROP_CHILD_ACTIONS, m_menuVisibilityListener);
        m.removePropertyChangeListener(IMenu.PROP_VISIBLE, m_menuVisibilityListener);
        removeScoutMenuVisibilityListenerRec(m.getChildActions());
      }
    }
  }

  @Override
  public void callAboutToShow(final IActionFilter filter) {
    acceptVisitor(new IActionVisitor() {
      @SuppressWarnings("deprecation")
      @Override
      public int visit(IAction action) {
        if (action instanceof IMenu && (filter == null || filter.accept(action))) {
          IMenu menu = (IMenu) action;
          menu.aboutToShow();
          menu.prepareAction();
        }
        return CONTINUE;
      }
    });
  }

  protected void calculateLocalVisibility() {

    final IActionFilter activeFilter = ActionUtility.createMenuFilterMenuTypes(getCurrentMenuTypes(), true);
    if (activeFilter != null) {
      final BooleanHolder visibleHolder = new BooleanHolder(false);
      acceptVisitor(new IActionVisitor() {
        @Override
        public int visit(IAction action) {
          if (action instanceof IMenu) {
            IMenu menu = (IMenu) action;
            if (menu.hasChildActions() || menu.isSeparator() || menu instanceof IContextMenu) {
              return CONTINUE;
            }
            else if (activeFilter.accept(menu)) {
              visibleHolder.setValue(true);
              return CANCEL;
            }
          }
          return CONTINUE;
        }
      });
      setVisible(visibleHolder.getValue());
    }
  }

  private class P_VisibilityOfMenuItemChangedListener implements PropertyChangeListener {
    @SuppressWarnings("unchecked")
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if (IMenu.PROP_CHILD_ACTIONS.equals(evt.getPropertyName())) {
        handleChildActionsChanged((List<IMenu>) evt.getOldValue(), (List<IMenu>) evt.getNewValue());
      }
      calculateLocalVisibility();
    }
  }

  protected static class LocalContextMenuExtension<OWNER extends AbstractContextMenu> extends LocalMenuExtension<OWNER> implements IContextMenuExtension<OWNER> {

    public LocalContextMenuExtension(OWNER owner) {
      super(owner);
    }
  }

  @Override
  protected IContextMenuExtension<? extends AbstractContextMenu> createLocalExtension() {
    return new LocalContextMenuExtension<AbstractContextMenu>(this);
  }

}
