/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.ui.rap.ILogicalGridLayout;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Scrollable;

public final class RwtLayoutUtility {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(RwtLayoutUtility.class);

  private RwtLayoutUtility() {
  }

  private static boolean dumpSizeTreeRunning;

  public static void invalidateLayout(IRwtEnvironment uiEnvironment, Control c) {
    uiEnvironment.getLayoutValidateManager().invalidate(c);
  }

  public static void dumpSizeTree(Control c) {
    if (!dumpSizeTreeRunning) {
      try {
        dumpSizeTreeRunning = true;
        dumpSizeTreeRec(c, "");
      }
      finally {
        dumpSizeTreeRunning = false;
      }
    }
  }

  public static Point computeSizeEx(Control control, int wHint, int hHint, boolean flushCache) {
    Point size = null;
    if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
      size = control.computeSize(wHint, hHint, flushCache);
    }
    else {
      //WORKAROUND until swt components such as scrollables are correctly calculating their own scroll bars in
      int trimW, trimH;
      if (control instanceof Scrollable) {
        Rectangle rect = ((Scrollable) control).computeTrim(0, 0, 0, 0);
        trimW = rect.width;
        trimH = rect.height;
      }
      else {
        trimW = trimH = control.getBorderWidth() * 2;
      }
      int wHintFixed = wHint == SWT.DEFAULT ? wHint : Math.max(0, wHint - trimW);
      int hHintFixed = hHint == SWT.DEFAULT ? hHint : Math.max(0, hHint - trimH);
      size = control.computeSize(wHintFixed, hHintFixed, flushCache);
    }
    return size;
  }

  private static void dumpSizeTreeRec(Control c, String prefix) {
    Point d = c.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
    String lay = "null";
    if (c instanceof Composite) {
      Layout lm = ((Composite) c).getLayout();
      if (lm != null) {
        lay = lm.getClass().getSimpleName();
      }
    }
    Rectangle r = c.getBounds();
    StringBuffer buf = new StringBuffer();
    buf.append("[" + r.x + "," + r.y + "," + r.width + "," + r.height + "]");
    buf.append(" pref=(" + d.x + "," + d.y + ")");
    buf.append(" " + c.getClass().getSimpleName());
    buf.append(" layout=" + lay);
    buf.append(c.getVisible() ? " VISIBLE" : " INVISIBLE");
    Object gd = c.getLayoutData();
    if (gd != null) {
      buf.append(" logicalGridData=" + gd);
    }
    // details
    if (c instanceof Composite) {
      Layout layout = ((Composite) c).getLayout();
      if (layout instanceof ILogicalGridLayout) {
        StringWriter w = new StringWriter();
        ((ILogicalGridLayout) layout).dumpLayoutInfo((Composite) c, new PrintWriter(w, true));
        buf.append("\n  " + w.toString().replace("\n", "\n  "));
      }
      else if (layout instanceof GridLayout) {
        buf.append("\n  " + layout.toString());
      }
    }
    String msg = prefix + buf.toString().replace("\n", "\n" + prefix);
    System.out.println(msg);
    // children
    if (c instanceof Composite) {
      for (Control child : ((Composite) c).getChildren()) {
        dumpSizeTreeRec(child, prefix + "  ");
      }
    }
  }

  public static GridLayout createGridLayoutNoSpacing(int columnCount, boolean makeColumnsEqualWidth) {
    GridLayout layout = new GridLayout(columnCount, makeColumnsEqualWidth);
    layout.horizontalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    return layout;
  }

  public static Point computeMinimumSize(Control c, boolean changed) {
    if (c instanceof Composite) {
      Layout layout = ((Composite) c).getLayout();
      if (layout instanceof ILogicalGridLayout) {
        return ((ILogicalGridLayout) layout).computeMinimumSize((Composite) c, changed);
      }
    }
    return c.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
  }
}
