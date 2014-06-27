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
package org.eclipse.scout.rt.ui.swt.ext;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ImageViewer extends Canvas {

  protected static final int FOCUS_BORDER_OFFSET_PX = 2;
  protected static final Rectangle FOCUS_BORDER_OFFSET = new Rectangle(FOCUS_BORDER_OFFSET_PX, FOCUS_BORDER_OFFSET_PX, -2 * FOCUS_BORDER_OFFSET_PX, -2 * FOCUS_BORDER_OFFSET_PX);
  protected static final Rectangle NO_FOCUS_BORDER_OFFSET = new Rectangle(0, 0, 0, 0);

  private int m_xAglin = SWT.CENTER;
  private int m_yAglin = SWT.CENTER;
  private boolean m_autoFit = false;

  private Image m_image;
  private Image m_scaledImage;
  private KeyListener m_keyListener; // if added, makes the canvas focusable

  public ImageViewer(Composite parent) {
    super(parent, SWT.NONE);
    addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        handleSwtPaintEvent(e.gc);
      }
    });
    addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        freeResources();
      }
    });
    addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent e) {
        redraw();
      }

      @Override
      public void focusGained(FocusEvent e) {
        redraw();
      }
    });
  }

  private void freeResources() {
    if (m_scaledImage != null && !m_scaledImage.isDisposed()) {
      m_scaledImage.dispose();
      m_scaledImage = null;
    }
  }

  private Image scaleImage(Image img) {
    freeResources();
    if (m_autoFit && img != null) {
      Rectangle clientArea = getClientArea();
      if (clientArea.width == 0 || clientArea.height == 0) {
        return img;
      }
      Rectangle imageBounds = img.getBounds();
      if (imageBounds.width == 0 || imageBounds.height == 0) {
        return null;
      }
      double scaleFactor = (double) clientArea.width / (double) imageBounds.width;
      scaleFactor = Math.min(scaleFactor, (double) clientArea.height / (double) img.getBounds().height);
      m_scaledImage = new Image(getDisplay(), img.getImageData().scaledTo((int) (scaleFactor * img.getBounds().width), (int) (scaleFactor * img.getBounds().height)));
      return m_scaledImage;
    }
    else {
      return img;
    }
  }

  /**
   * Returns the client area available for the image. This is the original (full) client area minus the space used by
   * the focus border.
   */
  @Override
  public Rectangle getClientArea() {
    Rectangle clientArea = super.getClientArea();

    if (isFocusable()) {
      // make space for the focus border
      clientArea.x += FOCUS_BORDER_OFFSET.x;
      clientArea.y += FOCUS_BORDER_OFFSET.y;
      clientArea.width += FOCUS_BORDER_OFFSET.width;
      clientArea.height += FOCUS_BORDER_OFFSET.height;
    }

    return clientArea;
  }

  protected Point getImageLocation(Rectangle bounds, Rectangle imgBounds) {
    Rectangle focusBorderOffset = getFocusBorderOffset();
    Point location = new Point(focusBorderOffset.x, focusBorderOffset.y);
    if (imgBounds.width <= bounds.width) {
      switch (getAlignmentX()) {
        case SWT.CENTER:
          location.x = (bounds.width - imgBounds.width + focusBorderOffset.width) / 2;
          break;
        case SWT.RIGHT:
          location.x = bounds.width - imgBounds.width + focusBorderOffset.width;
          break;
        default:
          location.x = focusBorderOffset.x;
          break;
      }
    }
    if (imgBounds.height <= bounds.height) {
      switch (getAlignmentY()) {
        case SWT.NONE:
          location.y = (bounds.height - imgBounds.height + focusBorderOffset.height) / 2;
          break;
        case SWT.BOTTOM:
          location.y = bounds.height - imgBounds.height + focusBorderOffset.height;
          break;
        default:
          location.y = focusBorderOffset.y;
          break;
      }
    }
    return location;
  }

  protected void handleSwtPaintEvent(GC gc) {
    Image img = getImage();

    if (img != null) {
      // scale img
      if (isAutoFit()) {
        img = scaleImage(img);
      }
      Rectangle imgBounds = img.getBounds();
      Rectangle focusBorderOffset = getFocusBorderOffset();
      Point imageLocation = getImageLocation(gc.getClipping(), imgBounds);

      // draw
      gc.drawImage(img, imageLocation.x, imageLocation.y);

      if (isFocusable() && isFocusControl()) {
        // draw focus border
        int lineStyleBackup = gc.getLineStyle();
        Color foregroundBackup = gc.getForeground();
        try {
          // do not use gc.drawFocus() because this does not draw properly in some cases.
          gc.setLineStyle(SWT.LINE_DOT);
          gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_BLUE));
          gc.drawRectangle(imageLocation.x - focusBorderOffset.x, imageLocation.y - focusBorderOffset.y, imgBounds.width - focusBorderOffset.width, imgBounds.height - focusBorderOffset.height);
        }
        finally {
          gc.setLineStyle(lineStyleBackup);
          gc.setForeground(foregroundBackup);
        }
      }
    }
  }

  protected Rectangle getFocusBorderOffset() {
    if (isFocusable()) {
      return FOCUS_BORDER_OFFSET;
    }
    return NO_FOCUS_BORDER_OFFSET;
  }

  @Override
  public Point computeSize(int hint, int hint2, boolean changed) {
    Point size = super.computeSize(hint, hint2, changed);
    if (getImage() != null) {
      Rectangle imgBounds = getImage().getBounds();
      size.x = imgBounds.width;
      size.y = imgBounds.height;
    }
    return size;
  }

  public void setAlignmentX(int alignment) {
    m_xAglin = alignment;
  }

  public int getAlignmentX() {
    return m_xAglin;
  }

  public void setAlignmentY(int alignment) {
    m_yAglin = alignment;
  }

  public int getAlignmentY() {
    return m_yAglin;
  }

  public synchronized boolean isFocusable() {
    return m_keyListener != null;
  }

  public synchronized void setFocusable(boolean focusable) {
    if (focusable) {
      if (m_keyListener == null) {
        m_keyListener = new KeyAdapter() {
          @Override
          public void keyReleased(KeyEvent e) {
            traverse(SWT.TRAVERSE_NONE, e);
          }
        };
        addKeyListener(m_keyListener);
      }
    }
    else {
      if (m_keyListener != null) {
        removeKeyListener(m_keyListener);
        m_keyListener = null;
      }
    }
  }

  public boolean isAutoFit() {
    return m_autoFit;
  }

  public void setAutoFit(boolean autoFit) {
    m_autoFit = autoFit;
  }

  public void setImage(Image img) {
    m_image = img;
  }

  public Image getImage() {
    return m_image;
  }
}
