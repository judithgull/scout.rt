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
package org.eclipse.scout.rt.client.ui.desktop.navigation.internal;

import java.util.List;

import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientJob;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.desktop.navigation.INavigationHistoryService;
import org.eclipse.scout.rt.client.ui.desktop.navigation.NavigationHistoryListener;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.service.AbstractService;

@Priority(-1)
public class NavigationHistoryService extends AbstractService implements INavigationHistoryService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(NavigationHistoryService.class);

  @Override
  public Bookmark addStep(int level, String name, String iconId) {
    return getUserNavigationHistory().addStep(level, name, iconId);
  }

  @Override
  public Bookmark addStep(int level, IPage page) {
    return getUserNavigationHistory().addStep(level, page);
  }

  @Override
  public Bookmark getActiveBookmark() {
    return getUserNavigationHistory().getActiveBookmark();
  }

  @Override
  public List<Bookmark> getBookmarks() {
    return getUserNavigationHistory().getBookmarks();
  }

  @Override
  public List<Bookmark> getBackwardBookmarks() {
    return getUserNavigationHistory().getBackwardBookmarks();
  }

  @Override
  public boolean hasBackwardBookmarks() {
    return getUserNavigationHistory().hasBackwardBookmarks();
  }

  @Override
  public List<Bookmark> getForwardBookmarks() {
    return getUserNavigationHistory().getForwardBookmarks();
  }

  @Override
  public boolean hasForwardBookmarks() {
    return getUserNavigationHistory().hasForwardBookmarks();
  }

  @Override
  public void stepForward() throws ProcessingException {
    getUserNavigationHistory().stepForward();
  }

  @Override
  public void stepBackward() throws ProcessingException {
    getUserNavigationHistory().stepBackward();

  }

  @Override
  public void stepTo(Bookmark b) throws ProcessingException {
    getUserNavigationHistory().stepTo(b);
  }

  @Override
  public List<IMenu> getMenus() {
    return getUserNavigationHistory().getMenus();
  }

  @Override
  public int getSize() {
    return getUserNavigationHistory().getSize();
  }

  @Override
  public int getIndex() {
    return getUserNavigationHistory().getIndex();
  }

  @Override
  public void addNavigationHistoryListener(NavigationHistoryListener listener) {
    getUserNavigationHistory().addNavigationHistoryListener(listener);
  }

  @Override
  public void removeNavigationHistoryListener(NavigationHistoryListener listener) {
    getUserNavigationHistory().removeNavigationHistoryListener(listener);
  }

  private UserNavigationHistory getUserNavigationHistory() {
    IClientSession session = ClientJob.getCurrentSession();
    if (session == null) {
      return new UserNavigationHistory();
    }
    UserNavigationHistory data = (UserNavigationHistory) session.getData(SERVICE_DATA_KEY);
    if (data == null) {
      data = new UserNavigationHistory();
      session.setData(SERVICE_DATA_KEY, data);
    }
    return data;
  }
}
