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
scout.FormToolButton = function() {
  scout.FormToolButton.parent.call(this);
  this._addAdapterProperties('form');
};
scout.inherits(scout.FormToolButton, scout.Menu);

scout.FormToolButton.prototype._renderForm = function() {
  if (!this.rendered) {
    // Don't execute initially since _renderSelected will be executed
    return;
  }
  this._renderSelected();
};

/**
 * @override
 */
scout.FormToolButton.prototype._renderText = function() {
  scout.FormToolButton.parent.prototype._renderText.call(this);
  if (this.rendered && this.popup) {
    this.popup.rerenderHead();
    this.popup.position();
  }
};

/**
 *
 * @override
 * form of a formToolbutton can be set to null and is set to a real form by model
 * so we have to update and clone it after it set.
 */
scout.FormToolButton.prototype.cloneAdapter = function(modelOverride) {
  var cloneAdapter = scout.FormToolButton.parent.prototype.cloneAdapter.call(this);
  cloneAdapter.formChangeListener = this._handleOriginalFormChange.bind(cloneAdapter);
  this.on('propertyChange', cloneAdapter.formChangeListener);
  cloneAdapter.visible=false;
  return cloneAdapter;
};

scout.FormToolButton.prototype._handleOriginalFormChange = function(event){
  if(event.newProperties.form){
    this.form =  event.newProperties.form.cloneAdapter({
      parent: this
    });
  } else if (event.newProperties.form === null){
    this.form = null;
  }
};

/**
 * @override
 */
scout.FormToolButton.prototype._remove = function() {
  if(this.cloneOf){
    this.cloneOf.off('propertyChange', this.formChangeListener);
  }
  scout.FormToolButton.parent.prototype._remove.call(this);
};

/**
 * @override
 */
scout.FormToolButton.prototype._createPopup = function() {
  return scout.create('FormToolPopup', {
    parent: this,
    formToolButton: this,
    openingDirectionX: this.popupOpeningDirectionX,
    openingDirectionY: this.popupOpeningDirectionY
  });
};

/**
 * @override
 */
scout.FormToolButton.prototype._doActionTogglesPopup = function() {
  return !!this.form;
};

/**
 * @override
 */
scout.FormToolButton.prototype._doAction = function(event) {
  //clones in submenus and contextmenues do not execute any action
  if(this.formToolButton.form.cloneOf){
    return false;
  }
  scout.FormToolButton.parent.prototype._doAction.call(this,event);
};


/**
 * @override
 */
scout.FormToolButton.prototype._createActionKeyStroke = function() {
  return new scout.FormToolButtonActionKeyStroke(this);
};

/**
 * FormToolButtonActionKeyStroke
 */
scout.FormToolButtonActionKeyStroke = function(action) {
  scout.FormToolButtonActionKeyStroke.parent.call(this, action);
};
scout.inherits(scout.FormToolButtonActionKeyStroke, scout.ActionKeyStroke);

scout.FormToolButtonActionKeyStroke.prototype.handle = function(event) {
  this.field.toggle(event);
};

scout.FormToolButtonActionKeyStroke.prototype._postRenderKeyBox = function($drawingArea) {
  if (this.field.iconId) {
    var wIcon = $drawingArea.find('.icon').width();
    var wKeybox = $drawingArea.find('.key-box').outerWidth();
    var containerPadding = Number($drawingArea.css('padding-left').replace('px', ''));
    var leftKeyBox = wIcon / 2 - wKeybox / 2 + containerPadding;
    $drawingArea.find('.key-box').css('left', leftKeyBox + 'px');
  }
};
