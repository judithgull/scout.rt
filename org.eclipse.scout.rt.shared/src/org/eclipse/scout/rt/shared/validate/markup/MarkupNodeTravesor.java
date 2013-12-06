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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Node;

/**
 * @since 3.10.0-M4
 */
public class MarkupNodeTravesor implements IMarkupNodeTravesor {
  private final IMarkupVisitor m_visitor;

  public MarkupNodeTravesor(IMarkupVisitor visitor) {
    m_visitor = visitor;
  }

  @Override
  public void traverse(Node root) {
    // traverses nodes using BFS
    int depth = 0;
    LinkedList<NodeBean> toVisit = new LinkedList<NodeBean>();
    List<Node> rootList = new ArrayList<Node>();
    rootList.add(root);
    insert(toVisit, depth, rootList);

    while (!toVisit.isEmpty()) {
      NodeBean bean = toVisit.removeFirst();
      Node node = bean.getNode();

      boolean continueVisiting = m_visitor.visit(bean);
      if (continueVisiting && node.childNodeSize() > 0) {
        List<Node> childNodes = node.childNodes();
        insert(toVisit, bean.getDepth() + 1, childNodes);
      }
    }
  }

  private void insert(List<NodeBean> nodeBeans, int depth, List<Node> nodes) {
    for (Node node : nodes) {
      nodeBeans.add(new NodeBean(node, depth));
    }
  }
}
