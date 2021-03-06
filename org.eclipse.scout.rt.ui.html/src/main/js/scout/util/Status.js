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
scout.Status = function(model) {
  $.extend(this, model);
};

scout.Status.Severity = {
  OK: 0x01,
  INFO: 0x100,
  WARNING: 0x10000,
  ERROR: 0x1000000
};

scout.Status.cssClasses = 'has-error has-warning has-info';

scout.Status.prototype.cssClass = function() {
  return scout.Status.cssClassForSeverity(this.severity);
};

scout.Status.prototype.isError = function() {
  return this.severity === scout.Status.Severity.ERROR;
};

scout.Status.cssClassForSeverity = function(severity) {
  var isInfo = (severity > scout.Status.Severity.OK);
  var isWarning = (severity > scout.Status.Severity.INFO);
  var isError = (severity > scout.Status.Severity.WARNING);

  if (isError) {
    return 'has-error';
  }
  if (isWarning) {
    return 'has-warning';
  }
  if (isInfo) {
    return 'has-info';
  }
  return '';
};

scout.Status.animateStatusMessage = function($status, message) {
  if (scout.strings.endsWith(message, '...')) {
    var $elipsis = $status.makeSpan('elipsis');
    for (var i = 0; i < 3; i++) {
      $elipsis.append($status.makeSpan('animate-dot delay-' + i, '.'));
    }
    message = message.substring(0, message.length - 3);
    $status.empty().text(message).append($elipsis);
  } else {
    $status.text(message);
  }
};
