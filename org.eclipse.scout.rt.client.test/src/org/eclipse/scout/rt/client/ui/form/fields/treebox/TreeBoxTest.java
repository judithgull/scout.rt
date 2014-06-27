/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.form.fields.treebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;
import org.eclipse.scout.rt.shared.services.lookup.LocalLookupCall;
import org.eclipse.scout.rt.shared.services.lookup.LookupRow;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JUnit tests for {@link AbstractTreeBox}.
 */
@RunWith(ScoutClientTestRunner.class)
public class TreeBoxTest {

  /**
   * Select a parent node in a tree box and check whether only this node is selected..
   * {@link AbstractTreeBox#getConfiguredAutoCheckChildNodes} returns false on the {@link SimpleTreeBox}.
   * Bug 368107 - Check child nodes when parent node is checked
   */
  @Test
  public void testDefaultBehavior() throws Exception {
    SimpleTreeBox treeBox = new SimpleTreeBox();
    treeBox.initField();
    ITree tree = treeBox.getTree();

    ITreeNode node = tree.findNode(1L); // A
    assertNotNull(node);
    tree.setNodeChecked(node, true);

    Set<Long> valueSet = new HashSet<Long>(treeBox.getValue());

    // only one node selected
    assertEquals(1, valueSet.size());

    // and the selected one is the node explicitly set before
    assertEquals(true, valueSet.contains(1L)); // A
  }

  /**
   * Select a parent node in a tree box with auto check child nodes activated, and check whether this node and all child
   * nodes are selected. {@link AbstractTreeBox#getConfiguredAutoCheckChildNodes} returns true on the
   * {@link AutoSelectTreeBox}.
   * Bug 368107 - Check child nodes when parent node is checked
   */
  @Test
  public void testAutoSelectBehavior() throws Exception {
    AutoSelectTreeBox treeBox = new AutoSelectTreeBox();
    treeBox.initField();
    ITree tree = treeBox.getTree();

    ITreeNode node = tree.findNode(1L); // A
    assertNotNull(node);
    tree.setNodeChecked(node, true);

    Set<Long> valueSet = new HashSet<Long>(treeBox.getValue());

    // parent node and 3 childs nodes selected
    assertEquals(4, valueSet.size());

    // and the selected ones are correct
    assertEquals(true, valueSet.contains(1L)); // A
    assertEquals(true, valueSet.contains(5L)); // A-A
    assertEquals(true, valueSet.contains(6L)); // A-B
    assertEquals(true, valueSet.contains(7L)); // A-C
  }

  /**
   * Select a parent node in a tree box with auto check child nodes activated, and check whether this node and all child
   * nodes are selected (extended test). {@link AbstractTreeBox#getConfiguredAutoCheckChildNodes} returns true on the
   * {@link AutoSelectTreeBox}.
   * Bug 368107 - Check child nodes when parent node is checked
   */
  @Test
  public void testAutoSelectBehaviorExtended() throws Exception {
    AutoSelectTreeBox treeBox = new AutoSelectTreeBox();
    treeBox.initField();
    ITree tree = treeBox.getTree();

    ITreeNode node = tree.findNode(9L); // C-B
    assertNotNull(node);
    tree.setNodeChecked(node, true);

    Set<Long> valueSet = new HashSet<Long>(treeBox.getValue());

    // parent node and 4 childs nodes selected
    assertEquals(5, valueSet.size());

    // and the selected ones are correct
    assertEquals(true, valueSet.contains(9L)); // C-B
    assertEquals(true, valueSet.contains(11L)); // C-B-A
    assertEquals(true, valueSet.contains(12L)); // C-B-B
    assertEquals(true, valueSet.contains(13L)); // C-B-C
    assertEquals(true, valueSet.contains(14L)); // C-B-D

    // deselected node C-B-B
    node = tree.findNode(12L); // C-B-B
    assertNotNull(node);
    tree.setNodeChecked(node, false);

    valueSet = new HashSet<Long>(treeBox.getValue());

    // parent node and all minus one childs nodes selected
    assertEquals(4, valueSet.size());

    // and the selected ones are correct
    assertEquals(true, valueSet.contains(9L)); // C-B
    assertEquals(true, valueSet.contains(11L)); // C-B-A
    assertEquals(true, valueSet.contains(13L)); // C-B-C
    assertEquals(true, valueSet.contains(14L)); // C-B-D

    // deselected C-C node
    node = tree.findNode(9L); // C-B
    assertNotNull(node);
    tree.setNodeChecked(node, false);

    // no nodes selected
    Set<Long> values = treeBox.getValue();
    assertTrue(CollectionUtility.isEmpty(values));
  }

  @Test
  public void testNullKeys() throws Exception {
    AutoSelectTreeBox treeBox = new AutoSelectTreeBox();
    treeBox.initField();
    treeBox.checkAllKeys();
    Assert.assertEquals(14, treeBox.getCheckedKeyCount()); // assert the null key is not available
  }

  public class SimpleTreeBox extends AbstractTreeBox<Long> {

    @Override
    protected Class<? extends ILookupCall<Long>> getConfiguredLookupCall() {
      return TreeBoxLookupCall.class;
    }

  }

  public class AutoSelectTreeBox extends AbstractTreeBox<Long> {

    @Override
    protected boolean getConfiguredAutoCheckChildNodes() {
      return true;
    }

    @Override
    protected Class<? extends ILookupCall<Long>> getConfiguredLookupCall() {
      return TreeBoxLookupCall.class;
    }
  }

  public static class TreeBoxLookupCall extends LocalLookupCall<Long> {

    private static final long serialVersionUID = 1L;

    @Override
    protected List<ILookupRow<Long>> execCreateLookupRows() throws ProcessingException {
      List<ILookupRow<Long>> list = new ArrayList<ILookupRow<Long>>();
      list.add(new LookupRow<Long>(1L, "A", null, null, null, null, null, true, /*parent*/null, true));
      list.add(new LookupRow<Long>(2L, "B", null, null, null, null, null, true, /*parent*/null, true));
      list.add(new LookupRow<Long>(3L, "C", null, null, null, null, null, true, /*parent*/null, true));
      list.add(new LookupRow<Long>(4L, "D", null, null, null, null, null, true, /*parent*/null, true));
      list.add(new LookupRow<Long>(5L, "A-A", null, null, null, null, null, true, /*parent*/1L, true));
      list.add(new LookupRow<Long>(6L, "A-B", null, null, null, null, null, true, /*parent*/1L, true));
      list.add(new LookupRow<Long>(7L, "A-C", null, null, null, null, null, true, /*parent*/1L, true));
      list.add(new LookupRow<Long>(8L, "C-A", null, null, null, null, null, true, /*parent*/3L, true));
      list.add(new LookupRow<Long>(9L, "C-B", null, null, null, null, null, true, /*parent*/3L, true));
      list.add(new LookupRow<Long>(10L, "C-C", null, null, null, null, null, true, /*parent*/3L, true));
      list.add(new LookupRow<Long>(11L, "C-B-A", null, null, null, null, null, true, /*parent*/9L, true));
      list.add(new LookupRow<Long>(12L, "C-B-B", null, null, null, null, null, true, /*parent*/9L, true));
      list.add(new LookupRow<Long>(13L, "C-B-C", null, null, null, null, null, true, /*parent*/9L, true));
      list.add(new LookupRow<Long>(14L, "C-B-D", null, null, null, null, null, true, /*parent*/9L, true));
      list.add(new LookupRow<Long>(null, "null key", null, null, null, null, null, true, /*parent*/9L, true));
      return list;
    }
  }

}
