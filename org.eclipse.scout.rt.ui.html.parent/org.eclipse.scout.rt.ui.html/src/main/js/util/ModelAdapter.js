scout.ModelAdapter = function() {
  this.session;
  this.parent;
  this.children = [];
  this._adapterProperties = [];
  this.rendered = false;
  this.destroyed = false;
};

/**
 * This method is called when the adapter receives the first event, not when the adapter was created
 * @param model
 * @param session
 */
scout.ModelAdapter.prototype.init = function(model, session) {
  this.session = session;

  // copy all properties from model to this adapter instance
  this._eachProperty(model, function(propertyName, value) {
    this[propertyName] = value;
  }.bind(this));

  this.session.registerModelAdapter(this);
};

scout.ModelAdapter.prototype.render = function($parent) {
  if (this.rendered) {
    throw new Error('Already rendered');
  }
  if (this.destroyed) {
    throw new Error('Object is destroyed');
  }
  this._render($parent);
  this._renderProperties();
  this.rendered = true;
  if (this.session.offline) {
    this.goOffline();
  }
};

/**
 * Adds property name(s) of model properties which must be converted automatically to a model adapter.
 *
 * @param properties String or String-array with property names.
 */
scout.ModelAdapter.prototype._addAdapterProperties = function(properties) {
  if (Array.isArray(properties)) {
    this._adapterProperties = this._adapterProperties.concat(properties);
  } else {
    this._adapterProperties.push(properties);
  }
};

/**
 * This method creates the UI through DOM manipulation. At this point we should not apply model
 * properties on the UI, since sub-classes may need to contribute to the DOM first. You must not
 * apply model values to the UI here, since this is done in the _renderProperties method later.
 * The default impl. does nothing.
 */
scout.ModelAdapter.prototype._render = function() {
  // NOP
};

/**
 * This method calls the UI setter methods after the _render method has been executed.
 * Here values of the model are applied to the DOM / UI. The default impl. does nothing.
 */
scout.ModelAdapter.prototype._renderProperties = function() {
  // NOP
};

scout.ModelAdapter.prototype.remove = function() {
  var i, child;

  if (this.rendered) {
    for (i = 0; i < this.children.length; i++) {
      child = this.children[i];
      child.remove();
    }

    this._remove();
    this.rendered = false;
  }
  this.dispose();
};

scout.ModelAdapter.prototype._remove = function() {
  if (this.$container) {
    this.$container.remove();
    this.$container = null;
  }
};

scout.ModelAdapter.prototype.dispose = function() {
  // NOP implement to free resources
};

scout.ModelAdapter.prototype.destroy = function() {
  this.remove();

  //Disconnect from parent
  if (this.parent) {
    this.parent.removeChild(this);
    this.parent = null;
  }

  this.session.unregisterModelAdapter(this); //FIXME CGU unregister children?
  this.destroyed = true;
};

scout.ModelAdapter.prototype.addChild = function(childAdapter) {
  this.children.push(childAdapter);
};

scout.ModelAdapter.prototype.removeChild = function(childAdapter) {
  scout.arrays.remove(this.children, childAdapter);
};

/**
 * Loops through all properties of the given model. Creates an ModelAdapter instance
 * for the given property when the propertyName is in the _adapterProperties array.
 */
scout.ModelAdapter.prototype._eachProperty = function(model, func) {
  var propertyName, value, i, j, adapter, adapters;

  // Loop through primitive properties
  for (propertyName in model) {
    if (this._adapterProperties.indexOf(propertyName) > -1) {
      continue; // will be handled below
    }
    value = model[propertyName];
    func(propertyName, value);
  }

  //Loop through adapter properties (any order will do).
  for (i = 0; i < this._adapterProperties.length; i++) {
    propertyName = this._adapterProperties[i];
    value = model[propertyName];
    if (value === undefined) {
      continue;
    }
    // Distinguishing between undefined and '' is important
    // The value is '' if the server wants to explicitly remove the adapter
    // -> func() needs to be called for 'empty' adapters
    if (value !== '') {
      if (Array.isArray(value)) {
        adapters = [];
        for (j = 0; j < value.length; j++) {
          adapter = this.session.getOrCreateModelAdapter(value[j], this);
          this.onChildAdapterCreated(propertyName, adapter);
          adapters.push(adapter);
        }
        value = adapters;
      } else {
        value = this.session.getOrCreateModelAdapter(value, this);
        this.onChildAdapterCreated(propertyName, value);
      }
    }

    func(propertyName, value);
  }
};

/**
 * Processes the JSON event from the server and sets dynamically properties on the adapter (-model)
 * and calls the right function to update the UI. For each property a corresponding function-name
 * must exist (property-name 'myValue', function-name 'setMyValue').
 *
 * This happens in two steps:
 * 1.) Synchronizing: when a sync[propertyName] method exists, call that method - otherwise simply set the property [propertyName]
 * 2.) Rendering: Call render[propertyName] function to update UI
 *
 * You can always rely that these two steps are processed in that order, but you cannot rely that
 * individual properties are processed in a certain order.
 */
scout.ModelAdapter.prototype.onModelPropertyChange = function(event) {
  var oldValues = {};

  // step 1 synchronizing - apply properties on adapter or calls syncPropertyName if it exists
  this._syncPropertiesOnPropertyChange(oldValues, event.properties);

  // step 2 rendering - call render methods to update UI, but only if it is displayed (rendered)
  if (this.rendered) {
    this._renderPropertiesOnPropertyChange(oldValues, event.properties);
  }
};

/**
 * The default impl. only logs a warning that the event is not supported.
 */
scout.ModelAdapter.prototype.onModelAction = function(event) {
  $.log.warn('Model action "' + event.type + '" is not supported by model-adapter ' + this.objectType);
};

scout.ModelAdapter.prototype._syncPropertiesOnPropertyChange = function(oldValues, newValues) {
  this._eachProperty(newValues, function(propertyName, value) {
    var onFuncName = '_sync' + scout.ModelAdapter.preparePropertyNameForFunctionCal(propertyName);
    oldValues[propertyName] = this[propertyName];
    if (this[onFuncName]) {
      this[onFuncName](value);
    } else {
      this[propertyName] = value;
    }
  }.bind(this));
};

scout.ModelAdapter.prototype._renderPropertiesOnPropertyChange = function(oldValues, newValues) {
  this._eachProperty(newValues, function(propertyName, value) {
    var renderFuncName = '_render' + scout.ModelAdapter.preparePropertyNameForFunctionCal(propertyName);
    $.log.debug('call ' + renderFuncName + '(' + value + ')');
    // Call the render function for regular properties, for adapters see onChildAdapterChange
    if (this._adapterProperties.indexOf(propertyName) > -1) {
      this.onChildAdapterChange(propertyName, oldValues[propertyName], value);
    }
    else {
      if (!this[renderFuncName]) {
        throw new Error('Render function ' + renderFuncName + ' does not exist in model adapter');
      }
      this[renderFuncName](value);
    }
  }.bind(this));
};

scout.ModelAdapter.preparePropertyNameForFunctionCal = function(propertyName) {
  return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
};

/**
 * Removes the existing adapter specified by oldValue. Renders the new adapters if this.$container is set.<br>
 * To prevent this behavior just implement the method _renderPropertyName or _removePropertyName (e.g _removeTable).
 */
scout.ModelAdapter.prototype.onChildAdapterChange = function(propertyName, oldValue, newValue) {
  var funcName = scout.ModelAdapter.preparePropertyNameForFunctionCal(propertyName);
  var renderFuncName = '_render' + funcName;
  var removeFuncName = '_remove' + funcName;
  var i;

  // Remove old adapter, if there is one
  if (oldValue) {
    if (!this[removeFuncName]) {
      if (Array.isArray(oldValue)) {
        for (i = 0; i < oldValue.length; i++) {
          oldValue[i].remove();
        }
      } else {
        oldValue.remove();
      }
    } else {
      this[removeFuncName](oldValue);
    }
  }

  // Render new adapter, if there is one
  if (newValue) {
    if (!this[renderFuncName] && this.$container) {
      if (Array.isArray(oldValue)) {
        for (i = 0; i < oldValue.length; i++) {
          newValue[i].render(this.$container);
        }
      } else {
        newValue.render(this.$container);
      }
    } else {
      this[renderFuncName](newValue);
    }
  }
};

/**
 * Maybe overridden to influence creation. Default is emtpy.
 */
scout.ModelAdapter.prototype.onChildAdapterCreated = function(propertyName) {

};

scout.ModelAdapter.prototype.goOffline = function() {
  var i;
  for (i = 0; i < this.children.length; i++) {
    if (!this.children[i].rendered) {
      //going offline must not modify model state -> only necessary to inform rendered objects
      continue;
    }
    this.children[i].goOffline();
  }
};

scout.ModelAdapter.prototype.goOnline = function() {
  var i;
  for (i = 0; i < this.children.length; i++) {
    if (!this.children[i].rendered) {
      //going offline must not modify model state -> only necessary to inform rendered objects
      continue;
    }
    this.children[i].goOnline();
  }
};
