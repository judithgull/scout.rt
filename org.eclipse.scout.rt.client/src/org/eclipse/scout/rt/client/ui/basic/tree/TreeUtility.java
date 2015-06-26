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
package org.eclipse.scout.rt.client.ui.basic.tree;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;

/**
 * @since 3.8.0
 */
public final class TreeUtility {

  private TreeUtility() {
  }

  /**
   * If every given node has the same parent, that common parent node will be returned, else null.
   */
  public static ITreeNode calculateCommonParentNode(Collection<? extends ITreeNode> nodes) {
    if (!CollectionUtility.hasElements(nodes)) {
      return null;
    }

    if (nodes.size() == 1) {
      return CollectionUtility.firstElement(nodes).getParentNode();
    }
    Iterator<? extends ITreeNode> nodeIt = nodes.iterator();
    ITreeNode test = nodeIt.next().getParentNode();
    while (nodeIt.hasNext()) {
      if (nodeIt.next().getParentNode() != test) {
        test = null;
        break;
      }
    }
    return test;
  }

  /**
   * Searches for the lowest common ancestor of the given node. Lowest means farthest away from root.
   */
  public static ITreeNode findLowestCommonAncestorNode(List<ITreeNode> nodes) {
    if (nodes == null || nodes.size() == 0) {
      return null;
    }

    if (nodes.size() == 1) {
      return nodes.get(0).getParentNode();
    }

    ITreeNode commonParent = nodes.get(0).getParentNode();

    for (int i = 1; i < nodes.size(); i++) {
      ITreeNode parentNode = nodes.get(i).getParentNode();
      if (parentNode != commonParent) {
        commonParent = findLowestCommonAncestorNode(commonParent, parentNode);
      }
    }

    return commonParent;
  }

  public static ITreeNode findLowestCommonAncestorNode(ITreeNode firstNode, ITreeNode secondNode) {
    if (firstNode == null || secondNode == null) {
      return null;
    }

    if (firstNode.getTree() != secondNode.getTree()) {
      return null;
    }

    if (firstNode.getParentNode() == secondNode.getParentNode()) {
      return firstNode.getParentNode();
    }

    if (secondNode.getTreeLevel() > firstNode.getTreeLevel()) {
      secondNode = findAncestorNodeAtLevel(secondNode, firstNode.getTreeLevel());
    }
    else if (firstNode.getTreeLevel() > secondNode.getTreeLevel()) {
      firstNode = findAncestorNodeAtLevel(firstNode, secondNode.getTreeLevel());
    }

    while (firstNode.getParentNode() != secondNode.getParentNode()) {
      firstNode = firstNode.getParentNode();
      secondNode = secondNode.getParentNode();
    }

    return firstNode.getParentNode();
  }

  /**
   * Returns the ancestor of the given node which resides at the given level.
   *
   * @param level
   *          The level to look for the ancestor. The level has to be <= than node.getLevel(); Otherwise null will
   *          returned.
   */
  public static ITreeNode findAncestorNodeAtLevel(ITreeNode node, int level) {
    if (node == null) {
      return null;
    }

    if (node.getTreeLevel() < level) {
      return null;
    }

    while (node.getTreeLevel() > level) {
      node = node.getParentNode();
    }

    return node;
  }
}
