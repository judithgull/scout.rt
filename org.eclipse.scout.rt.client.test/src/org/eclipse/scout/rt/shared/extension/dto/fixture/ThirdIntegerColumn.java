/*******************************************************************************
 * Copyright (c) 2014 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.extension.dto.fixture;

import org.eclipse.scout.commons.annotations.Data;
import org.eclipse.scout.commons.annotations.Extends;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractIntegerColumn;

/**
 *
 */
@Order(3000.0)
@Extends(OrigPageWithTable.class)
@Data(ThirdIntegerColumnData.class)
public class ThirdIntegerColumn extends AbstractIntegerColumn {

}
