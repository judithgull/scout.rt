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

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.XmlDeclaration;

/**
 * @since 3.10.0-M4
 */
public class MarkupVisitor implements IMarkupVisitor {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(MarkupVisitor.class);

  private final Element m_root;
  private final IMarkupList m_markupList;
  private final Set<Node> m_removedNodes;

  public MarkupVisitor(Element root, IMarkupList markupList) {
    m_root = root;
    m_markupList = markupList;
    m_removedNodes = new HashSet<Node>();
  }

  @Override
  public boolean visit(NodeBean nodeBean) {
    Node node = nodeBean.getNode();
    int depth = nodeBean.getDepth();

    LOG.warn("Visit node: " + node.nodeName() + ", depth: " + depth);

    if (!checkAllowedNodes(node)) {
      return false;
    }

    if (node instanceof Element) {
      Element elem = (Element) node;
      LOG.warn("\tChecking element tag: " + elem.tagName());

      if (!m_markupList.isAllowedTag(elem.tagName()) && elem != m_root) {
        LOG.warn("\tRemove element tag: " + elem.tagName());
        node.remove();
        return false;
      }

      for (Attribute attr : elem.attributes()) {
        LOG.warn("\t\tChecking attribute key: " + attr.getKey() + ", attribute value: " + attr.getValue());
        if (!m_markupList.isAllowedAttribute(elem.tagName(), attr.getKey())) {
          LOG.warn("\t\tRemove attribute key: " + attr.getKey() + ", attribute value: " + attr.getValue());
          elem.removeAttr(attr.getKey());
        }
      }
    }

    return true;
  }

  private boolean checkAllowedNodes(Node node) {
    // remove comments nodes if not allowed
    if (m_markupList.isCommentsAllowed() && node instanceof Comment) {
      LOG.warn("Removed comments node " + node.nodeName());
      node.remove();
      return false;
    }

    // remove data nodes if not allowed
    if (!m_markupList.isDataAllowed() && node instanceof DataNode) {
      LOG.warn("Removed data node " + node.nodeName());
      node.remove();
      return false;
    }

    // remove document type nodes if not allowed
    if (!m_markupList.isDocumentTypeAllowed() && node instanceof DocumentType) {
      LOG.warn("Removed document type node " + node.nodeName());
      node.remove();
      return false;
    }

    // remove xml declaration nodes if not allowed
    if (!m_markupList.isXmlDeclarationAllowed() && node instanceof XmlDeclaration) {
      LOG.warn("Removed xml declaration node " + node.nodeName());
      node.remove();
      return false;
    }

    return true;
  }
}
