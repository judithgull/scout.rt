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
package org.eclipse.scout.rt.client.ui.desktop.bookmark.internal;

import java.security.Permission;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.services.common.bookmark.BookmarkServiceEvent;
import org.eclipse.scout.rt.client.services.common.bookmark.BookmarkServiceListener;
import org.eclipse.scout.rt.client.services.common.bookmark.IBookmarkService;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.AbstractBookmarkTreeField;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.internal.ManageBookmarksForm.MainBox.CancelButton;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.internal.ManageBookmarksForm.MainBox.GlobalBox;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.internal.ManageBookmarksForm.MainBox.OkButton;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.internal.ManageBookmarksForm.MainBox.UserBox;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.internal.ManageBookmarksForm.MainBox.GlobalBox.GlobalBookmarkTreeField;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.internal.ManageBookmarksForm.MainBox.UserBox.UserBookmarkTreeField;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.client.ui.form.AbstractFormHandler;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractCancelButton;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractOkButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.security.DeleteGlobalBookmarkPermission;
import org.eclipse.scout.rt.shared.security.DeleteUserBookmarkPermission;
import org.eclipse.scout.rt.shared.security.ReadUserBookmarkPermission;
import org.eclipse.scout.rt.shared.security.UpdateGlobalBookmarkPermission;
import org.eclipse.scout.rt.shared.security.UpdateUserBookmarkPermission;
import org.eclipse.scout.service.SERVICES;

public class ManageBookmarksForm extends AbstractForm implements BookmarkServiceListener {

  public ManageBookmarksForm() throws ProcessingException {
    super();
  }

  @Override
  protected String getConfiguredTitle() {
    return ScoutTexts.get("Bookmarks");
  }

  @Override
  public void bookmarksChanged(BookmarkServiceEvent e) {
    getGlobalBookmarkTreeField().setBookmarkRootFolder(e.getBookmarkService().getBookmarkData().getGlobalBookmarks());
    getUserBookmarkTreeField().setBookmarkRootFolder(e.getBookmarkService().getBookmarkData().getUserBookmarks());
  }

  public void startModify() throws ProcessingException {
    startInternal(new ModifyHandler());
  }

  public MainBox getMainBox() {
    return (MainBox) getRootGroupBox();
  }

  public GlobalBox getGlobalBox() {
    return getFieldByClass(GlobalBox.class);
  }

  public UserBox getUserBox() {
    return getFieldByClass(UserBox.class);
  }

  public OkButton getOkButton() {
    return getFieldByClass(OkButton.class);
  }

  public CancelButton getCancelButton() {
    return getFieldByClass(CancelButton.class);
  }

  public UserBookmarkTreeField getUserBookmarkTreeField() {
    return getFieldByClass(UserBookmarkTreeField.class);
  }

  public GlobalBookmarkTreeField getGlobalBookmarkTreeField() {
    return getFieldByClass(GlobalBookmarkTreeField.class);
  }

  @Order(10f)
  public class MainBox extends AbstractGroupBox {

    @Order(11)
    public class GlobalBox extends AbstractGroupBox {
      @Override
      protected String getConfiguredLabel() {
        return ScoutTexts.get("GlobalBookmarks");
      }

      @Override
      protected void execInitField() throws ProcessingException {
        setVisiblePermission(new UpdateGlobalBookmarkPermission());
      }

      @Order(10)
      public class GlobalBookmarkTreeField extends AbstractBookmarkTreeField {
        @Override
        protected int getConfiguredGridW() {
          return 2;
        }

        @Override
        protected int getConfiguredGridH() {
          return 10;
        }

        @Override
        protected Permission getDeletePermission() {
          return new DeleteGlobalBookmarkPermission();
        }

        @Override
        protected Permission getUpdatePermission() {
          return new UpdateGlobalBookmarkPermission();
        }
      }
    }

    @Order(20f)
    public class UserBox extends AbstractGroupBox {
      @Override
      protected String getConfiguredLabel() {
        return ScoutTexts.get("Bookmarks");
      }

      @Override
      protected void execInitField() throws ProcessingException {
        setVisiblePermission(new ReadUserBookmarkPermission());
      }

      @Order(10)
      public class UserBookmarkTreeField extends AbstractBookmarkTreeField {
        @Override
        protected int getConfiguredGridW() {
          return 2;
        }

        @Override
        protected int getConfiguredGridH() {
          return 10;
        }

        @Override
        protected Permission getDeletePermission() {
          return new DeleteUserBookmarkPermission();
        }

        @Override
        protected Permission getUpdatePermission() {
          return new UpdateUserBookmarkPermission();
        }
      }
    }// end group box

    @Order(40f)
    public class OkButton extends AbstractOkButton {
    }

    @Order(50f)
    public class CancelButton extends AbstractCancelButton {
    }

  }// end main box

  @Order(20f)
  public class ModifyHandler extends AbstractFormHandler {

    @Override
    protected void execLoad() throws ProcessingException {
      IBookmarkService service = SERVICES.getService(IBookmarkService.class);
      //get notified about changes
      service.addBookmarkServiceListener(ManageBookmarksForm.this);
      service.loadBookmarks();//load most recent state
      getGlobalBookmarkTreeField().setBookmarkRootFolder(service.getBookmarkData().getGlobalBookmarks());
      getUserBookmarkTreeField().setBookmarkRootFolder(service.getBookmarkData().getUserBookmarks());
      getGlobalBookmarkTreeField().populateTree();
      getUserBookmarkTreeField().populateTree();
    }

    @Override
    protected void execPostLoad() throws ProcessingException {
      touch();
    }

    @Override
    protected void execStore() throws ProcessingException {
      SERVICES.getService(IBookmarkService.class).storeBookmarks();
    }

    @Override
    protected void execDiscard() throws ProcessingException {
      //revert all changes
      SERVICES.getService(IBookmarkService.class).loadBookmarks();
    }

    @Override
    protected void execFinally() throws ProcessingException {
      IBookmarkService service = SERVICES.getService(IBookmarkService.class);
      service.removeBookmarkServiceListener(ManageBookmarksForm.this);
    }
  }

}
