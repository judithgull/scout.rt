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
package org.eclipse.scout.rt.client.ui.desktop.outline.pages;

import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.basic.tree.IVirtualTreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.VirtualTreeNode;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.form.IForm;

/**
 * For performance optimizations, child pages are only loaded when needed.
 * Until then they are virtualized using VirtualPage objects.
 * <p>
 * A call to {@link IPage#getChildPage(int)}, {@link IPage#getChildPages()} or selecting a virtual page effectively
 * constructs the child page.
 * <p>
 * This construction involves calling
 * {@link AbstractPageWithTable#execCreateChildPage(org.eclipse.scout.rt.client.ui.basic.table.ITableRow)} resp.
 * {@link AbstractPageWithNodes#execCreateChildPages(java.util.Collection)}
 */
public class VirtualPage extends VirtualTreeNode implements IPage, IVirtualTreeNode {

  public VirtualPage() {
  }

  @Override
  public void initPage() throws ProcessingException {
  }

  @Override
  public String getUserPreferenceContext() {
    return null;
  }

  @Override
  public IOutline getOutline() {
    return (IOutline) getTree();
  }

  @Override
  public IPage getParentPage() {
    return (IPage) getParentNode();
  }

  @Override
  public IPage getChildPage(final int childIndex) {
    return null;
  }

  @Override
  public List<IPage> getChildPages() {
    return CollectionUtility.emptyArrayList();
  }

  @Override
  public void pageActivatedNotify() {
  }

  @Override
  public void pageDeactivatedNotify() {
  }

  @Override
  public IForm getDetailForm() {
    return null;
  }

  @Override
  public void setDetailForm(IForm form) {
  }

  @Override
  public void dataChanged(Object... dataTypes) {
  }

  @Override
  public final void reloadPage() throws ProcessingException {
  }

  @Override
  public boolean isTableVisible() {
    return false;
  }

  @Override
  public void setTableVisible(boolean b) {
  }

  @Override
  public IProcessingStatus getPagePopulateStatus() {
    return null;
  }

  @Override
  public void setPagePopulateStatus(IProcessingStatus status) {
  }

  /**
   * not defined on a virtual pages.
   */
  @Override
  public String classId() {
    return null;
  }

  @Override
  public <T> T getAdapter(Class<T> clazz) {
    return null;
  }
}
