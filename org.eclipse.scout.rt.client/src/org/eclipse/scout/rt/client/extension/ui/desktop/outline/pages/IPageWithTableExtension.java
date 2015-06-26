package org.eclipse.scout.rt.client.extension.ui.desktop.outline.pages;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.extension.ui.desktop.outline.pages.PageWithTableChains.PageWithTableCreateChildPageChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.outline.pages.PageWithTableChains.PageWithTableCreateVirtualChildPageChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.outline.pages.PageWithTableChains.PageWithTableInitSearchFormChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.outline.pages.PageWithTableChains.PageWithTableLoadDataChain;
import org.eclipse.scout.rt.client.extension.ui.desktop.outline.pages.PageWithTableChains.PageWithTablePopulateTableChain;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.AbstractPageWithTable;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;

public interface IPageWithTableExtension<T extends ITable, OWNER extends AbstractPageWithTable<T>> extends IPageExtension<OWNER> {

  void execLoadData(PageWithTableLoadDataChain<? extends ITable> chain, SearchFilter filter) throws ProcessingException;

  IPage execCreateChildPage(PageWithTableCreateChildPageChain<? extends ITable> chain, ITableRow row) throws ProcessingException;

  void execPopulateTable(PageWithTablePopulateTableChain<? extends ITable> chain) throws ProcessingException;

  IPage execCreateVirtualChildPage(PageWithTableCreateVirtualChildPageChain<? extends ITable> chain, ITableRow row) throws ProcessingException;

  void execInitSearchForm(PageWithTableInitSearchFormChain<? extends ITable> chain) throws ProcessingException;
}
