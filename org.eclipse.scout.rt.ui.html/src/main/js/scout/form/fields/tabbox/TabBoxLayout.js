/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.TabBoxLayout = function(tabBox) {
  scout.TabBoxLayout.parent.call(this);
  this._tabBox = tabBox;
  this._statusWidth = scout.HtmlEnvironment.fieldStatusWidth;
};
scout.inherits(scout.TabBoxLayout, scout.AbstractLayout);

scout.TabBoxLayout.prototype.layout = function($container) {
  var containerSize, tabContentSize, tabAreaMargins, innerTabAreaSize,
    htmlContainer = scout.HtmlComponent.get($container),
    htmlTabContent = scout.HtmlComponent.get(this._tabBox._$tabContent),
    htmlTabArea = scout.HtmlComponent.get(this._tabBox._$tabArea),
    tabAreaWidth = 0,
    tabAreaHeight = 0,
    tabAreaSize = new scout.Dimension(),
    $pseudoStatus = this._tabBox.$pseudoStatus;

  containerSize = htmlContainer.getAvailableSize()
    .subtract(htmlContainer.getInsets());

  if (htmlTabArea.isVisible()) {
    tabAreaMargins = htmlTabArea.getMargins();
    tabAreaHeight = htmlTabArea.getPreferredSize().height;
    tabAreaWidth = containerSize.subtract(tabAreaMargins).width;
    if ($pseudoStatus.isVisible()) {
      $pseudoStatus.cssWidth(this._statusWidth);
      tabAreaWidth -= $pseudoStatus.outerWidth(true);
    }
    innerTabAreaSize = new scout.Dimension(tabAreaWidth, tabAreaHeight);
    htmlTabArea.setSize(innerTabAreaSize);
    tabAreaSize = innerTabAreaSize.add(tabAreaMargins);
  }

  tabContentSize = containerSize.subtract(htmlTabContent.getMargins());
  tabContentSize.height -= tabAreaSize.height;
  htmlTabContent.setSize(tabContentSize);
};

/**
 * Preferred size of the tab-box aligns every tab-item in a single line, so that each item is visible.
 */
scout.TabBoxLayout.prototype.preferredLayoutSize = function($container) {
  var htmlContainer = scout.HtmlComponent.get($container),
    htmlTabContent = scout.HtmlComponent.get(this._tabBox._$tabContent),
    htmlTabArea = scout.HtmlComponent.get(this._tabBox._$tabArea),
    tabAreaSize = new scout.Dimension(),
    tabContentSize = new scout.Dimension();

  if (htmlTabArea.isVisible()) {
    tabAreaSize = htmlTabArea.getPreferredSize()
      .add(htmlTabArea.getMargins());
  }

  tabContentSize = htmlTabContent.getPreferredSize()
    .add(htmlContainer.getInsets())
    .add(htmlTabContent.getMargins());

  return new scout.Dimension(
    Math.max(tabAreaSize.width, tabContentSize.width),
    tabContentSize.height + tabAreaSize.height);
};