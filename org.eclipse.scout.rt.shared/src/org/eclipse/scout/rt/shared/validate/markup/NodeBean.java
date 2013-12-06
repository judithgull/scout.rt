/*******************************************************************************
 * Copyright (c) 2010, 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.validate.markup;

import org.jsoup.nodes.Node;

/**
 * @since 3.10.0-M4
 */
public class NodeBean {
  private final Node m_node;
  private final int m_depth;

  public NodeBean(Node node, int depth) {
    m_node = node;
    m_depth = depth;
  }

  public final Node getNode() {
    return m_node;
  }

  public final int getDepth() {
    return m_depth;
  }
}
