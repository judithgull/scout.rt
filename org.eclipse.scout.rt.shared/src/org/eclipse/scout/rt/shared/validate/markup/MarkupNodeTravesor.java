/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.validate.markup;

import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Node;

/**
 * @since 3.10.0-M3
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
    insert(toVisit, depth, root);

    while (!toVisit.isEmpty()) {
      NodeBean bean = toVisit.removeFirst();
      Node node = bean.getNode();
      depth = bean.getDepth();

      m_visitor.head(node, depth);
      if (!m_visitor.isRemovedNode(node) && node.childNodeSize() > 0) {
        List<Node> childNodes = node.childNodes();
        insert(toVisit, depth + 1, childNodes.toArray(new Node[childNodes.size()]));
      }
    }
  }

  private void insert(List<NodeBean> nodeBeans, int depth, Node... nodes) {
    for (Node node : nodes) {
      nodeBeans.add(new NodeBean(node, depth));
    }
  }

  private static class NodeBean {
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
}
