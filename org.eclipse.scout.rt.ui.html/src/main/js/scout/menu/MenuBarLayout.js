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
scout.MenuBarLayout = function(menuBar) {
  scout.MenuBarLayout.parent.call(this);
  this._menuBar = menuBar;
  this._ellipsis;
};
scout.inherits(scout.MenuBarLayout, scout.AbstractLayout);

/**
 * @override AbstractLayout.js
 */
scout.MenuBarLayout.prototype.layout = function($container) {
  // check if all menu items have enough room to be displayed without ellipsis
  this._destroyEllipsis();
  this._menuBar.rebuildItemsInternal();

  var ellipsisSize, leftEnd = 0,
    rightEnd, overflown,
    oldOverflow = $container.css('overflow');

  // we cannot set overflow in MenuBar.css because overflow:hidden would cut off the
  // focus border on the left-most button in the menu-bar. That's why we must reset
  // the overflow-property after we've checked if the menu-bar is over-sized.
  $container.css('overflow', 'hidden');
  rightEnd = $container[0].clientWidth;

  // 1st find the left-most position of all right-aligned items
  // see: special comment for negative margins in Menu.css
  this._menuBar.menuItems.forEach(function(menuItem) {
    if (!menuItem.visible) {
      return;
    }
    var itemBounds = scout.graphics.bounds(menuItem.$container, true, true),
      tmpX;
    if (menuItem.rightAligned) {
      tmpX = itemBounds.x;
      if (tmpX < rightEnd) {
        rightEnd = tmpX;
      }
    } else {
      tmpX = itemBounds.x + itemBounds.width;
      if (tmpX > leftEnd) {
        leftEnd = tmpX;
      }
    }
  });

  // 1 instead of 0 is used to tolerate rounding issues, browsers return a rounded width instead of the precise one
  overflown = leftEnd - rightEnd >= 1;
  $container.css('overflow', oldOverflow);

  if (overflown) {
    var menuItemsCopy = [];

    // create ellipsis menu
    this._createAndRenderEllipsis($container);
    ellipsisSize = scout.graphics.getSize(this._ellipsis.$container, true);
    rightEnd -= ellipsisSize.width;

    // right-aligned menus are never put into the overflow ellipsis-menu
    // or in other words: they're not responsive.
    // for left-aligned menus: once we notice a menu-item that does not
    // fit into the available space, all following items must also be
    // put into the overflow ellipsis-menu. Otherwise you would put
    // an item with a long text into the ellipsis-menu, but the next
    // icon, with a short text would still be in the menu-bar. Which would
    // be confusing, as it would look like we've changed the order of the
    // menu-items.
    var overflowNextItems = false;
    this._menuBar.menuItems.forEach(function(menuItem) {
      if (menuItem.rightAligned) {
        // Always add right-aligned menus
        menuItemsCopy.push(menuItem);
      } else {
        var itemBounds = scout.graphics.bounds(menuItem.$container, true, true),
          rightOuterX = itemBounds.x + itemBounds.width;
        if (overflowNextItems || rightOuterX > rightEnd) {
          menuItem.remove();
          menuItem.overflow = true;
          this._ellipsis.childActions.push(menuItem);
          overflowNextItems = true;
        } else {
          // Only add left-aligned menu items when they're visible
          menuItemsCopy.push(menuItem);
        }
      }
    }, this);

    this._addEllipsisToMenuItems(menuItemsCopy);
    this._menuBar.visibleMenuItems = menuItemsCopy;
  } else {
    this._menuBar.visibleMenuItems = this._menuBar.menuItems;
  }
  this._menuBar.visibleMenuItems.forEach(function(menuItem) {
    // Make sure open popups are at the correct position after layouting
    if (menuItem.popup) {
      menuItem.popup.position();
    }
  });
};

/**
 * Add the ellipsis menu to the menu-items list. Order matters because we do not sort
 * menu-items again.
 */
scout.MenuBarLayout.prototype._addEllipsisToMenuItems = function(menuItemsCopy) {
  var i, menuItem, insertItemAt = 0;
  for (i = 0; i < menuItemsCopy.length; i++) {
    menuItem = menuItemsCopy[i];
    if (menuItem.rightAligned) {
      break;
    } else {
      insertItemAt = i + 1;
    }
  }
  scout.arrays.insert(menuItemsCopy, this._ellipsis, insertItemAt);
};

scout.MenuBarLayout.prototype._createAndRenderEllipsis = function($container) {
  var ellipsis = scout.create('Menu', {
    parent: this._menuBar,
    horizontalAlignment: 1,
    iconId: scout.icons.ELLIPSIS_V,
    tabbable: false
  });
  ellipsis.render($container);
  this._ellipsis = ellipsis;
};

scout.MenuBarLayout.prototype._destroyEllipsis = function() {
  if (this._ellipsis) {
    this._ellipsis.destroy();
    this._ellipsis = null;
  }
};

scout.MenuBarLayout.prototype.preferredLayoutSize = function($container) {
  // Menubar has an absolute css height set -> useCssSize = true
  return scout.graphics.prefSize($container, false, true);
};

/* --- STATIC HELPERS ------------------------------------------------------------- */

/**
 * @memberOf scout.MenuBarLayout
 */
scout.MenuBarLayout.size = function(htmlMenuBar, containerSize) {
  var menuBarSize = htmlMenuBar.getPreferredSize();
  menuBarSize.width = containerSize.width;
  menuBarSize = menuBarSize.subtract(htmlMenuBar.getMargins());
  return menuBarSize;
};