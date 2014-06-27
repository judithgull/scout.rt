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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.shared.AbstractIcons;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.rt.shared.services.common.bookmark.BookmarkFolder;
import org.eclipse.scout.rt.shared.services.common.bookmark.IBookmarkVisitor;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;
import org.eclipse.scout.rt.shared.services.lookup.LocalLookupCall;
import org.eclipse.scout.rt.shared.services.lookup.LookupRow;

/**
 * hierarchy lookup call for bookmark folder tree
 */
public class BookmarkFolderLookupCall extends LocalLookupCall<BookmarkFolder> {
  private static final long serialVersionUID = 1L;

  private BookmarkFolder m_rootFolder;

  public BookmarkFolder getRootFolder() {
    return m_rootFolder;
  }

  public void setRootFolder(BookmarkFolder rootFolder) {
    m_rootFolder = rootFolder;
  }

  @Override
  protected List<ILookupRow<BookmarkFolder>> execCreateLookupRows() throws ProcessingException {
    final ArrayList<ILookupRow<BookmarkFolder>> rows = new ArrayList<ILookupRow<BookmarkFolder>>();
    if (m_rootFolder != null) {
      m_rootFolder.visit(new IBookmarkVisitor() {
        @Override
        public boolean visitFolder(List<BookmarkFolder> path) {
          if (path.size() >= 2) {
            BookmarkFolder f = path.get(path.size() - 1);
            if (!Bookmark.INBOX_FOLDER_NAME.equals(f.getTitle())) {
              BookmarkFolder parent = null;
              if (path.size() >= 3) {
                parent = path.get(path.size() - 2);
              }
              LookupRow<BookmarkFolder> row = new LookupRow<BookmarkFolder>(f, f.getTitle(), f.getIconId() != null ? f.getIconId() : AbstractIcons.TreeNode);
              row.setParentKey(parent);
              rows.add(row);
            }
          }
          return true;
        }

        @Override
        public boolean visitBookmark(List<BookmarkFolder> path, Bookmark b) {
          return true;
        }
      });
    }
    return rows;
  }
}
