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
package org.eclipse.scout.rt.client.ui.desktop.bookmark;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.BookmarkForm.MainBox.GroupBox.DescriptionField;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.BookmarkForm.MainBox.GroupBox.FolderField;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.BookmarkForm.MainBox.GroupBox.KeyStrokeField;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.BookmarkForm.MainBox.GroupBox.TitleField;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.client.ui.form.AbstractFormHandler;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractCancelButton;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractOkButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.AbstractSmartField;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.AbstractStringField;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.rt.shared.services.common.bookmark.BookmarkFolder;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;

public class BookmarkForm extends AbstractForm implements IBookmarkForm {
  private BookmarkFolder m_bookmarkRootFolder;
  private Bookmark m_bookmark;

  public BookmarkForm() throws ProcessingException {
    super();
  }

  @Override
  public BookmarkFolder getBookmarkRootFolder() {
    return m_bookmarkRootFolder;
  }

  @Override
  public void setBookmarkRootFolder(BookmarkFolder bookmarkRootFolder) {
    m_bookmarkRootFolder = bookmarkRootFolder;
    ((BookmarkFolderLookupCall) getFolderField().getLookupCall()).setRootFolder(m_bookmarkRootFolder);
  }

  @Override
  public Bookmark getBookmark() {
    if (m_bookmark != null) {
      m_bookmark.setTitle(getTitleField().getValue());
      m_bookmark.setKeyStroke(getKeyStrokeField().getValue());
      m_bookmark.setText(getDescriptionField().getValue());
    }
    return m_bookmark;
  }

  @Override
  public void setBookmark(Bookmark bookmark) {
    m_bookmark = bookmark;
    if (bookmark != null) {
      getTitleField().setValue(bookmark.getTitle());
      getKeyStrokeField().setValue(bookmark.getKeyStroke());
      getDescriptionField().setValue(bookmark.getText());
    }
  }

  @Override
  protected String getConfiguredTitle() {
    return ScoutTexts.get("Bookmark");
  }

  @Override
  public void startModify() throws ProcessingException {
    startInternal(new ModifyHandler());
  }

  @Override
  public void startNew() throws ProcessingException {
    startInternal(new NewHandler());
  }

  public MainBox getMainBox() {
    return (MainBox) getRootGroupBox();
  }

  public TitleField getTitleField() {
    return getFieldByClass(TitleField.class);
  }

  public KeyStrokeField getKeyStrokeField() {
    return getFieldByClass(KeyStrokeField.class);
  }

  public DescriptionField getDescriptionField() {
    return getFieldByClass(DescriptionField.class);
  }

  public FolderField getFolderField() {
    return getFieldByClass(FolderField.class);
  }

  @Order(10f)
  public class MainBox extends AbstractGroupBox {
    @Override
    protected int getConfiguredGridColumnCount() {
      return 1;
    }

    @Order(10)
    public class GroupBox extends AbstractGroupBox {

      @Order(10)
      public class TitleField extends AbstractStringField {
        @Override
        protected String getConfiguredLabel() {
          return ScoutTexts.get("Name");
        }

        @Override
        protected boolean getConfiguredMandatory() {
          return true;
        }

        @Override
        protected int getConfiguredMaxLength() {
          return 4000;
        }
      }

      @Order(20)
      public class FolderField extends AbstractSmartField<BookmarkFolder> {
        @Override
        protected String getConfiguredLabel() {
          return ScoutTexts.get("BookmarkFolder");
        }

        @Override
        protected boolean getConfiguredBrowseHierarchy() {
          return true;
        }

        @Override
        protected Class<? extends ILookupCall<BookmarkFolder>> getConfiguredLookupCall() {
          return BookmarkFolderLookupCall.class;
        }
      }

      @Order(30)
      public class KeyStrokeField extends AbstractSmartField<String> {
        @Override
        protected String getConfiguredLabel() {
          return ScoutTexts.get("KeyStroke");
        }

        @Override
        protected Class<? extends ILookupCall<String>> getConfiguredLookupCall() {
          return KeyStrokeLookupCall.class;
        }

        @Override
        protected void execPrepareLookup(ILookupCall<String> call) throws ProcessingException {
          ((KeyStrokeLookupCall) call).setCurrentKeyStroke(getValue());
        }
      }

      @Order(40)
      public class DescriptionField extends AbstractStringField {
        @Override
        protected String getConfiguredLabel() {
          return ScoutTexts.get("Path");
        }

        @Override
        protected boolean getConfiguredLabelVisible() {
          return false;
        }

        @Override
        protected boolean getConfiguredMultilineText() {
          return true;
        }

        @Override
        protected boolean getConfiguredEnabled() {
          return false;
        }

        @Override
        protected int getConfiguredMaxLength() {
          return 4000;
        }

        @Override
        protected int getConfiguredGridW() {
          return 1;
        }

        @Override
        protected int getConfiguredGridH() {
          return 6;
        }
      }
    }

    @Order(40f)
    public class OkButton extends AbstractOkButton {
    }

    @Order(50f)
    public class CancelButton extends AbstractCancelButton {
    }
  }

  @Order(20f)
  public class ModifyHandler extends AbstractFormHandler {
  }

  @Order(20f)
  public class NewHandler extends AbstractFormHandler {
    @Override
    protected void execPostLoad() throws ProcessingException {
      touch();
    }
  }

  @Override
  public BookmarkFolder getFolder() throws ProcessingException {
    return getFolderField().getValue();
  }

  @Override
  public void setFolder(BookmarkFolder folder) throws ProcessingException {
    getFolderField().setValue(folder);
  }
}
