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
scout.DefaultFieldLoadingSupport = function(field, loadingIndicatorDelay) {
  this.field = field;
  this.loadingIndicatorDelay = scout.helpers.nvl(loadingIndicatorDelay, 250); // ms

  this._loadingIndicatorTimeoutId;
};

scout.DefaultFieldLoadingSupport.prototype.renderLoading = function() {
  // Clear any pending loading function
  clearTimeout(this._loadingIndicatorTimeoutId);

  if (!this.field) {
    return;
  }

  if (this.field.loading && !this._$loadingIndicator) {
    // --- 1. not loading -> loading ---

    var document = $.getDocument(this.field.$container);
    var renderLoading = function() {
      // Hide field content
      this.field.$container.addClass('loading');
      // Create loading indicator
      this._$loadingIndicator = $.makeDiv(document, 'loading-indicator').appendTo(this.field.$container);
    }.bind(this);

    if (this.loadingIndicatorDelay) {
      this._loadingIndicatorTimeoutId = setTimeout(renderLoading, this.loadingIndicatorDelay);
    } else {
      renderLoading();
    }

  } else if (!this.field.loading && this._$loadingIndicator) {
    // --- 2. loading -> not loading ---

    // Remove loading indicator
    this._$loadingIndicator.fadeOutAndRemove(function() {
      this._$loadingIndicator = null;
      // Show field's content (layout if necessary)
      this.field.$container.removeClass('loading');
      this.field.invalidateLayoutTree();
    }.bind(this));
  }
};