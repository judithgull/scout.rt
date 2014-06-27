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
package org.eclipse.scout.rt.shared.data.form.fields.treefield;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;

/**
 * Transfer object representing a node in a tree, used with a {@link AbstractTreeFieldData}
 */
public class TreeNodeData implements Serializable, Cloneable {
  private static final long serialVersionUID = 1L;

  private TreeNodeData m_parentNode;
  private List<TreeNodeData> m_childNodes;
  private List<? extends Object> m_values;
  private List<String> m_texts;

  public TreeNodeData() {
    m_childNodes = new ArrayList<TreeNodeData>(2);
  }

  @Override
  public Object clone() {
    try {
      TreeNodeData copy = (TreeNodeData) super.clone();
      if (this.m_childNodes != null) {
        copy.m_childNodes = new ArrayList<TreeNodeData>(this.m_childNodes.size());
        for (TreeNodeData n : this.m_childNodes) {
          copy.m_childNodes.add((TreeNodeData) n.clone());
        }
      }
      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public TreeNodeData getParentNode() {
    return m_parentNode;
  }

  public void setParentNode(TreeNodeData parentNode) {
    m_parentNode = parentNode;
  }

  public List<TreeNodeData> getChildNodes() {
    return m_childNodes;
  }

  public void setChildNodes(List<TreeNodeData> childNodes) {
    m_childNodes = childNodes;
    if (m_childNodes != null) {
      for (TreeNodeData n : m_childNodes) {
        n.setParentNode(this);
      }
    }
  }

  public List<Object> getValues() {
    return CollectionUtility.arrayList(m_values);
  }

  public void setValues(List<? extends Object> a) {
    m_values = CollectionUtility.arrayList(a);
  }

  public List<String> getTexts() {
    return CollectionUtility.arrayList(m_texts);
  }

  public void setTexts(List<String> a) {
    m_texts = CollectionUtility.arrayList(a);
  }

}
