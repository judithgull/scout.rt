scout.NavigateDownButton = function(outline, node) {
  scout.NavigateDownButton.parent.call(this, outline, node);
  this._text1 = 'Continue';
  this._text2 = 'Show';
  this.systemType = scout.Button.SYSTEM_TYPE.OK;
  this.id = 'NavigateDownButton';
  this.keyStroke = 'Enter';
};
scout.inherits(scout.NavigateDownButton, scout.AbstractNavigationButton);

scout.NavigateDownButton.prototype._isDetail = function() {
  // Button is in "detail mode" if there are both detail form and detail table visible and detail form is _not_ hidden.
  return !!(this.node.detailFormVisible && this.node.detailForm &&
    this.node.detailTableVisible && this.node.detailTable && this.node.detailFormVisibleByUi);
};

scout.NavigateDownButton.prototype._toggleDetail = function() {
  return false;
};

scout.NavigateDownButton.prototype._buttonEnabled = function() {
  if (this.node.leaf) {
    return false;
  }
  if (this._isDetail()) {
    return true;
  }

  // when it's not a leaf and not a detail - the button is only enabled when a single row is selected
  var table = this.node.detailTable;
  if (table) {
    return table.selectedRows.length === 1;
  } else {
    return true;
  }
};

scout.NavigateDownButton.prototype._drill = function() {
  var drillNode;

  if (this.node.detailTable) {
    var rows = this.node.detailTable.selectedRows;
    if (rows.length > 0) {
      var row = rows[0];
      drillNode = this.outline.nodesMap[row.nodeId];
    }
  } else {
    drillNode = this.node.childNodes[0];
  }
  if (drillNode) {
    $.log.debug('drill down to node ' + drillNode);
    this.outline.setNodesSelected(drillNode); // this also expands the parent node, if required
    this.outline.setNodeExpanded(drillNode, false);
  }
};
