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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;

public class TreeEvent extends EventObject {
  private static final long serialVersionUID = 1L;

  /**
   * valid attributes are parentNode,childNodes
   */
  public static final int TYPE_NODES_INSERTED = 10;
  /**
   * valid attributes are parentNode,childNodes
   */
  public static final int TYPE_NODES_UPDATED = 20;
  /**
   * no attributes
   */
  public static final int TYPE_NODE_FILTER_CHANGED = 400;
  /**
   * valid attributes are parentNode,childNodes
   */
  public static final int TYPE_NODES_DELETED = 30;
  /**
   * valid attributes are nodes, deselectedNodes parentNode is null
   */
  public static final int TYPE_BEFORE_NODES_SELECTED = 35;
  /**
   * valid attributes are nodes, deselectedNodes parentNode is null
   */
  public static final int TYPE_NODES_SELECTED = 40;
  /**
   * valid attributes are parentNode,childNodes
   */
  public static final int TYPE_CHILD_NODE_ORDER_CHANGED = 50;
  /**
   * valid attributes are node
   */
  public static final int TYPE_NODE_EXPANDED = 100;
  /**
   * valid attributes are node
   */
  public static final int TYPE_NODE_COLLAPSED = 101;
  /**
   * valid attributes are node
   */
  public static final int TYPE_NODE_ACTION = 705;

  /**
   * valid attributes are parentNode (if common parent of all nodes), nodes
   * register the drag object using the setDragObject method
   */
  public static final int TYPE_NODES_DRAG_REQUEST = 730;

  /**
   * Drag operation is finished
   */
  public static final int TYPE_DRAG_FINISHED = 735;

  /**
   * valid attributes are node get the drop object using the getDropObject
   * method
   */
  public static final int TYPE_NODE_DROP_ACTION = 740;
  /**
   * Gui targeted event valid attributes are node
   */
  public static final int TYPE_NODE_REQUEST_FOCUS = 200;
  /**
   * Gui targeted event valid attributes are node
   */
  public static final int TYPE_NODE_ENSURE_VISIBLE = 300;

  public static final int TYPE_REQUEST_FOCUS = 800;

  /**
   * valid attributes are node
   */
  public static final int TYPE_NODE_CLICK = 820;

  /**
   * Advise to scroll to selection
   */
  public static final int TYPE_SCROLL_TO_SELECTION = 830;

  /**
   * Node has changed in a way that may affect its presentation (e.g. text, font, color...) but no structural changes
   * occurred
   * 
   * @since 3.10.0-M5
   */
  public static final int TYPE_NODE_CHANGED = 850;

  /**
   * A node has changed during a drag and drop operation
   * 
   * @since 4.0-M7
   */
  public static final int TYPE_NODE_DROP_TARGET_CHANGED = 860;

  // next 870

  private final int m_type;
  private ITreeNode m_commonParentNode;
  private Collection<? extends ITreeNode> m_nodes;
  private Collection<? extends ITreeNode> m_deselectedNodes;
  private Collection<? extends ITreeNode> m_newSelectedNodes;
  private List<IMenu> m_popupMenus;
  private boolean m_consumed;
  private TransferObject m_dragObject;
  private TransferObject m_dropObject;

  public TreeEvent(ITree source, int type) {
    super(source);
    m_type = type;
  }

  public TreeEvent(ITree source, int type, ITreeNode node) {
    super(source);
    m_type = type;
    if (node != null) {
      m_nodes = CollectionUtility.hashSet(node);
    }
    m_commonParentNode = TreeUtility.calculateCommonParentNode(m_nodes);
  }

  public TreeEvent(ITree source, int type, Collection<? extends ITreeNode> nodes) {
    super(source);
    m_type = type;
    if (nodes != null) {
      m_nodes = nodes;
    }
    m_commonParentNode = TreeUtility.calculateCommonParentNode(m_nodes);
  }

  public TreeEvent(ITree source, int type, ITreeNode parentNode, Collection<? extends ITreeNode> childNodes) {
    super(source);
    m_type = type;
    if (childNodes != null) {
      m_nodes = childNodes;
    }
    m_commonParentNode = parentNode;
    if (m_commonParentNode == null) {
      m_commonParentNode = TreeUtility.calculateCommonParentNode(m_nodes);
    }
  }

  public ITree getTree() {
    return (ITree) getSource();
  }

  public int getType() {
    return m_type;
  }

  public ITreeNode getCommonParentNode() {
    return m_commonParentNode;
  }

  public ITreeNode getDeselectedNode() {
    if (CollectionUtility.hasElements(m_deselectedNodes)) {
      return CollectionUtility.firstElement(m_deselectedNodes);
    }
    else {
      return null;
    }
  }

  public Collection<ITreeNode> getDeselectedNodes() {
    return CollectionUtility.arrayList(m_deselectedNodes);
  }

  protected void setDeselectedNodes(Collection<ITreeNode> deselectedNodes) {
    m_deselectedNodes = deselectedNodes;
  }

  public ITreeNode getNewSelectedNode() {
    if (CollectionUtility.hasElements(m_newSelectedNodes)) {
      return CollectionUtility.firstElement(m_newSelectedNodes);
    }
    else {
      return null;
    }
  }

  public Collection<ITreeNode> getNewSelectedNodes() {
    return CollectionUtility.arrayList(m_newSelectedNodes);
  }

  protected void setNewSelectedNodes(Collection<ITreeNode> newSelectedNodes) {
    m_newSelectedNodes = newSelectedNodes;
  }

  public ITreeNode getNode() {
    if (CollectionUtility.hasElements(m_nodes)) {
      return CollectionUtility.firstElement(m_nodes);
    }
    else {
      return null;
    }
  }

  public Collection<ITreeNode> getNodes() {
    return CollectionUtility.arrayList(m_nodes);
  }

  public ITreeNode getChildNode() {
    return getNode();
  }

  public Collection<ITreeNode> getChildNodes() {
    return getNodes();
  }

  public void addPopupMenu(IMenu menu) {
    if (menu != null) {
      if (m_popupMenus == null) {
        m_popupMenus = new ArrayList<IMenu>();
      }
      m_popupMenus.add(menu);
    }
  }

  /**
   * used by TYPE_ROW_POPUP to add actions
   */
  public void addPopupMenus(List<IMenu> menus) {
    if (menus != null) {
      if (m_popupMenus == null) {
        m_popupMenus = new ArrayList<IMenu>();
      }
      m_popupMenus.addAll(menus);
    }
  }

  /**
   * used by TYPE_ROW_POPUP to add actions
   */
  public List<IMenu> getPopupMenus() {
    return CollectionUtility.arrayList(m_popupMenus);
  }

  /**
   * used by TYPE_ROW_POPUP to add actions
   */
  public int getPopupMenuCount() {
    if (m_popupMenus != null) {
      return m_popupMenus.size();
    }
    else {
      return 0;
    }
  }

  /**
   * used by TYPE_ROW_DRAG_REQUEST
   */
  public TransferObject getDragObject() {
    return m_dragObject;
  }

  public void setDragObject(TransferObject t) {
    m_dragObject = t;
  }

  /**
   * used by TYPE_ROW_DROP_ACTION
   */
  public TransferObject getDropObject() {
    return m_dropObject;
  }

  protected void setDropObject(TransferObject t) {
    m_dropObject = t;
  }

  public boolean isConsumed() {
    return m_consumed;
  }

  public void consume() {
    m_consumed = true;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("TreeEvent[");
    // nodes
    if (CollectionUtility.hasElements(m_nodes) && getTree() != null) {
      if (m_nodes.size() == 1) {
        buf.append("\"" + CollectionUtility.firstElement(m_nodes) + "\"");
      }
      else {
        buf.append("{");
        Iterator<? extends ITreeNode> nodeIt = m_nodes.iterator();
        buf.append("\"").append(nodeIt.next()).append("\"");
        while (nodeIt.hasNext()) {
          buf.append(",").append("\"").append(nodeIt.next()).append("\"");
        }
        buf.append("}");
      }
    }
    else {
      buf.append("{}");
    }
    buf.append(" ");
    // decode type
    try {
      Field[] f = getClass().getDeclaredFields();
      for (int i = 0; i < f.length; i++) {
        if (Modifier.isPublic(f[i].getModifiers()) && Modifier.isStatic(f[i].getModifiers()) && f[i].getName().startsWith("TYPE_")) {
          if (((Number) f[i].get(null)).intValue() == m_type) {
            buf.append(f[i].getName());
            break;
          }
        }
      }
    }
    catch (Throwable t) {
      buf.append("#" + m_type);
    }
    buf.append("]");
    return buf.toString();
  }

}
