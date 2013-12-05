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

import java.util.HashSet;
import java.util.Set;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * @since 3.10.0-M4
 */
public class MarkupVisitor implements IMarkupVisitor {
  private final Element m_root;
  private final IMarkupList m_markupList;
  private final Set<Node> m_removedNodes;

  public MarkupVisitor(Element root, IMarkupList markupList) {
    m_root = root;
    m_markupList = markupList;
    m_removedNodes = new HashSet<Node>();
  }

  @Override
  public void head(Node node, int depth) {
    System.out.println("Visit node: " + node.nodeName() + ", depth: " + depth);

    if (!(node instanceof TextNode) && !(node instanceof Element) && !(node instanceof DataNode)) {
      removeNode(node);
      return;
    }

    if (node instanceof Element) {
      Element elem = (Element) node;
      System.out.println("\tCheck element: " + elem.tagName());
      if (!m_markupList.isAllowedTag(elem.tagName()) && elem != m_root) {
        System.out.println("\tRemove element: " + elem.tagName());
        removeNode(elem);
        return;
      }

      for (Attribute attr : elem.attributes()) {
        System.out.println("\t\tCheck attribute: " + attr.getKey() + ", value: " + attr.getValue());
        if (!m_markupList.isAllowedAttribute(elem.tagName(), attr.getKey())) {
          System.out.println("\t\tRemove attribute: " + attr.getKey() + ", value: " + attr.getValue());
          elem.removeAttr(attr.getKey());
        }
      }
    }
  }

  @Override
  public boolean isRemovedNode(Node node) {
    return m_removedNodes.contains(node);
  }

  private void removeNode(Node node) {
    node.remove();
    m_removedNodes.add(node);
  }

  @Override
  public void tail(Node node, int depth) {
    // no operation
  }
}
