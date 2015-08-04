scout.MenuBar = function(session, menuSorter) {
  scout.MenuBar.parent.call(this);
  this.init(session);

  this.menuSorter = menuSorter;
  this.position = 'top'; // or 'bottom'
  this.size = 'small'; // or 'large'
  this.tabbable = true;
  this._internalMenuItems = []; // original list of menuItems that was passed to updateItems(), only used to check if menubar has changed
  this.menuItems = []; // list of menuItems (ordered, may contain additional UI separators, some menus may not be rendered)
  this._orderedMenuItems = null; // Object containing "left" and "right" menus
  this._defaultMenu = null;

  /**
   * This array is === menuItems when menu-bar is not over-sized.
   * When the menu-bar is over-sized, we this property is set be the MenuBarLayout
   * which adds an additional ellipsis-menu, and removes menu items that doesn't
   * fit into the available menu-bar space.
   */
  this.visibleMenuItems = [];

  this.keyStrokeAdapter = this._createKeyStrokeAdapter();

  this._menuItemPropertyChangeListener = function(event) {
    // We do not update the items directly, because this listener may be fired many times in one
    // user request (because many menus change one or more properties). Therefore, we just invalidate
    // the MenuBarLayout. It will be updated automatically after the user request has finished,
    // because the layout calls rebuildItems().
    if (event.changedProperties.length > 0) {
      if (event.changedProperties.length === 1 && event.changedProperties[0] === 'enabled') {
        // Optimization: don't invalidate layout when only the enabled state has changed (this should not affect the layout).
        this.updateDefaultMenu();
        return;
      }
      this.htmlComp.invalidateLayoutTree();
    }
  }.bind(this);
};
scout.inherits(scout.MenuBar, scout.Widget);

/**
 * @implements Widgets.js
 */
scout.MenuBar.prototype._createKeyStrokeAdapter = function() {
  return new scout.MenuBarKeyStrokeAdapter(this);
};

/**
 * @override Widget.js
 */
scout.MenuBar.prototype._render = function($parent) {
  // Visibility may change when updateItems() function is called, see updateVisibility().
  this.$container = $.makeDiv('menubar')
    .attr('id', 'MenuBar-' + scout.createUniqueId())
    .toggleClass('main-menubar', this.size === 'large')
    .setVisible(this.menuItems.length > 0);

  this.htmlComp = new scout.HtmlComponent(this.$container, this.session);
  this.htmlComp.setLayout(new scout.MenuBarLayout(this));

  if (this.position === 'top') {
    $parent.prepend(this.$container);
  } else {
    this.$container.addClass('bottom');
    $parent.append(this.$container);
  }
};

scout.MenuBar.prototype.bottom = function() {
  this.position = 'bottom';
};

scout.MenuBar.prototype.top = function() {
  this.position = 'top';
};

scout.MenuBar.prototype.large = function() {
  this.size = 'large';
};

scout.MenuBar.prototype._remove = function() {
  scout.MenuBar.parent.prototype._remove.call(this);
  this._removeMenuItems();
  // Reset internal state (don't do it inside _removeMenuItems, because that is also called from _updateItems())
  this._internalMenuItems = [];
  this.menuItems = [];
  this._orderedMenuItems = null;
  this.visibleMenuItems = [];
};

scout.MenuBar.prototype._removeMenuItems = function() {
  this.menuItems.forEach(function(item) {
    item.off('propertyChange', this._menuItemPropertyChangeListener);
    // remove DOM for existing menu items and destroy items that have
    // been created by the menuSorter (e.g. separators).
    if (item.createdBy === this.menuSorter) {
      item.destroy();
    } else {
      item.overflow = false;
      item.remove();
    }
  }, this);
};

/**
 * Forces the MenuBarLayout to be revalidated, which includes rebuilding the menu items.
 */
scout.MenuBar.prototype.rebuildItems = function(revalidateLayoutTree) {
  this.htmlComp.revalidateLayoutTree();
};

/**
 * Rebuilds the menu items without relayouting the menubar.
 * Do not call this internal method from outside (except from the MenuBarLayout).
 */
scout.MenuBar.prototype.rebuildItemsInternal = function() {
  this._updateItems(this._internalMenuItems);
};

scout.MenuBar.prototype.updateItems = function(menuItems) {
  menuItems = scout.arrays.ensure(menuItems);

  // Only update if list of menus changed. Don't compare this.menuItems, because that list
  // may contain additional UI separators, and may not be in the same order
  if (!scout.arrays.equals(this._internalMenuItems, menuItems) ||
      menuItems.some(function(elem) { return !elem.rendered; })) {
    this._internalMenuItems = menuItems;

    // Rebuild items
    this.rebuildItemsInternal();

    // Re-layout menubar
    this.htmlComp.invalidateLayoutTree();
  }
  else {
    // Don't rebuild menubar, but update "markers"
    this.updateVisibility();
    this.updateDefaultMenu();
    this.updateLastItemMarker();
  }
};

scout.MenuBar.prototype._updateItems = function(menuItems) {
  this._removeMenuItems();
  // Also remove the "ellipsis" menu generated by MenubarLayout
  this.$container.children('.menu-item').remove();

  // The menuSorter may add separators to the list of items, that's why we
  // store the return value of menuSorter in this.menuItems and not the
  // menuItems passed to the updateItems method. We must do this because
  // otherwise we could not remove the added separator later.
  this._orderedMenuItems = this.menuSorter.order(menuItems, this);
  this.menuItems = this._orderedMenuItems.left.concat(this._orderedMenuItems.right);
  this.visibleMenuItems = this.menuItems;
  this._lastVisibleItemLeft = null;
  this._lastVisibleItemRight = null;
  this._defaultMenu = null;

  // Important: "right" items are rendered first! This is a fix for Firefox issue with
  // float:right. In Firefox elements with float:right must come first in the HTML order
  // of elements. Otherwise a strange layout bug occurs.
  this._renderMenuItems(this._orderedMenuItems.right, true);
  this._renderMenuItems(this._orderedMenuItems.left, false);
  this.updateDefaultMenu();
  var lastVisibleItem = this._lastVisibleItemRight || this._lastVisibleItemLeft;
  if (lastVisibleItem) {
    lastVisibleItem.$container.addClass('last');
  }

  // Make first valid MenuItem tabbable so that it can be focused. All other items
  // are not tabbable. But they can be selected with the arrow keys.
  if ((!this._defaultMenu || !this._defaultMenu.enabled) && this.tabbable) {
    this.menuItems.some(function(item) {
      if (item.isTabTarget()) {
        this.setTabbableMenu(item);
        return true;
      } else {
        return false;
      }
    }.bind(this));
  }

  this.updateVisibility();
};

scout.MenuBar.prototype.setTabbableMenu = function(menu) {
  if (!this.tabbable || menu === this.tabbableMenu) {
    return;
  }
  if (this.tabbableMenu) {
    this.tabbableMenu.setTabbable(false);
  }
  menu.setTabbable(true);
  this.tabbableMenu = menu;
};

/**
 * Ensures that the last visible right-aligned item has the class 'last' (to remove the margin-right).
 * Call this method whenever the visibility of single items change. The 'last' class is assigned
 * initially in _renderMenuItems().
 */
scout.MenuBar.prototype.updateLastItemMarker = function() {
  // Remove the last class from all items
  this.$container.children('.last').removeClass('last');
  // Find last visible right aligned menu item
  var lastMenuItem;
  for (var i = 0; i < this.visibleMenuItems.length; i++) {
    var menuItem = this.visibleMenuItems[i];
    if (menuItem.rightAligned) {
      lastMenuItem = menuItem;
    }
  }
  // Assign the class to the found item
  if (lastMenuItem) {
    lastMenuItem.$container.addClass('last');
  }
};

scout.MenuBar.prototype.updateVisibility = function() {
  var oldVisible = this.htmlComp.isVisible(),
    visible = !this.hiddenByUi && this.menuItems.some(function(m) { return m.visible; });

  // Update visibility, layout and key-strokes
  if (visible !== oldVisible) {
    this.$container.setVisible(visible);
    this.htmlComp.invalidateLayoutTree();
    if (visible) {
      this._installKeyStrokeAdapter();
    } else {
      this._uninstallKeyStrokeAdapter();
    }
  }
};


/**
 * First rendered item that is enabled and reacts to ENTER keystroke shall be marked as 'defaultMenu'
 */
scout.MenuBar.prototype.updateDefaultMenu = function() {
  if (this._orderedMenuItems) {
    var found = this._updateDefaultMenuInItems(this._orderedMenuItems.right);
    if (!found) {
      this._updateDefaultMenuInItems(this._orderedMenuItems.left);
    }
  }
};

scout.MenuBar.prototype._updateDefaultMenuInItems = function(items) {
  var found = false;
  items.some(function(item) {
    if (item.visible && item.enabled && item.keyStrokeKeyPart === scout.keys.ENTER) {
      if (this._defaultMenu && this._defaultMenu !== item) {
        this._defaultMenu.$container.removeClass('default-menu');
      }
      this._defaultMenu = item;
      this._defaultMenu.$container.addClass('default-menu');
      this.setTabbableMenu(this._defaultMenu);
      found = true;
      return true;
    }
  }.bind(this));
  return found;
};

scout.MenuBar.prototype._renderMenuItems = function(menuItems, right) {
  // Reverse the list if alignment is right to preserve the visible order specified by the
  // Scout model (in HTML, elements with 'float: right' are displayed in reverse order)
  if (right) {
    menuItems.reverse();
  }
  var tooltipPosition = (this.position === 'top' ? 'bottom' : 'top');
  menuItems.forEach(function(item) {
    // Ensure all all items are non-tabbable by default. One of the items will get a tabindex
    // assigned again later in updateItems().
    item.setTabbable(false);
    if (this.tabbableMenu === item) {
      this.tabbableMenu = undefined;
    }
    item.tooltipPosition = tooltipPosition;
    item.render(this.$container);
    if (right) {
      // Mark as right-aligned
      item.rightAligned = true;
      item.$container.addClass('right-aligned');
      if (item.visible && !this._lastVisibleItemRight) {
        this._lastVisibleItemRight = item;
      }
    } else {
      if (item.visible) {
        this._lastVisibleItemLeft = item;
      }
    }

    // Attach a propertyChange listener to the item, so the menubar can be updated when one of
    // its items changes (e.g. visible, keystroke etc.)
    item.on('propertyChange', this._menuItemPropertyChangeListener);
  }.bind(this));
};
