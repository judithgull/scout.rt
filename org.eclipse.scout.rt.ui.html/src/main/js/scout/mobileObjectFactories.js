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
scout.mobileObjectFactories = scout.defaultObjectFactories.concat([{
  objectType: 'Desktop',
  create: function() {
    return new scout.MobileDesktop();
  },
  deviceType: scout.Device.Type.MOBILE
}, {
  objectType: 'Table',
  create: function() {
    return new scout.MobileTable();
  },
  deviceType: scout.Device.Type.MOBILE
}]);