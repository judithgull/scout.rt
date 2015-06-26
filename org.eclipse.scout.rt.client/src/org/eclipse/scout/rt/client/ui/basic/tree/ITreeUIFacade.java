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

import java.net.URL;
import java.util.List;

import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.rt.client.ui.MouseButton;

public interface ITreeUIFacade {

  boolean isUIProcessing();

  void setNodeExpandedFromUI(ITreeNode node, boolean on);

  void setNodeSelectedAndExpandedFromUI(ITreeNode node);

  void setNodesSelectedFromUI(List<ITreeNode> nodes);

  /**
   * Single mouse click on a node or (for checkable trees) the space key
   */
  void fireNodeClickFromUI(ITreeNode node, MouseButton mouseButton);

  /**
   * Double mouse click on a node or enter
   */
  void fireNodeActionFromUI(ITreeNode node);

  TransferObject fireNodesDragRequestFromUI();

  /**
   * Called after the drag operation was finished
   *
   * @since 4.0-M7
   */
  void fireDragFinishedFromUI();

  void fireNodeDropActionFromUI(ITreeNode node, TransferObject dropData);

  /**
   * Called if the drop node is changed during a drag and drop operation
   *
   * @since 4.0-M7
   */
  void fireNodeDropTargetChangedFromUI(ITreeNode node);

  void fireHyperlinkActionFromUI(ITreeNode node, URL url);

}
