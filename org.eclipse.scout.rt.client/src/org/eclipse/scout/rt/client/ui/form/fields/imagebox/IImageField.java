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
package org.eclipse.scout.rt.client.ui.form.fields.imagebox;

import java.awt.geom.AffineTransform;

import org.eclipse.scout.rt.client.ui.IDNDSupport;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.shared.data.basic.BoundsSpec;

/**
 * You may add IKeyStrokes to the image field<br>
 * A default set of key strokes could look as follows
 * <ul>
 * <li>The AutoFitKeyStroke reacts on 'a' and calls doAutoFit()
 * <li>The PanUpKeyStroke reacts on 'up' and calls doRelativePan(0,-getPanDelta())
 * <li>The PanDownKeyStroke reacts on 'down' and calls doRelativePan(0,getPanDelta())
 * <li>The PanLeftKeyStroke reacts on 'left' and calls doRelativePan(-getPanDelta(),0)
 * <li>The PanRightKeyStroke reacts on 'right' and calls doRelativePan(getPanDelta(),0)
 * <li>The RotateLeftKeyStroke reacts on 'ctrl-left' and calls doRelativeRotate(-getRotateDelta())
 * <li>The RotateRightKeyStroke reacts on 'ctrl-right' and calls doRelativeRotate(getRotateDelta())
 * <li>The ZoomInKeyStroke reacts on '+' and calls doRelativeZoom(getZoomDeltaValue(),getZoomDeltaValue())
 * <li>The ZoomOutKeyStroke reacts on '-' and calls doRelativeZoom(1.0/getZoomDeltaValue(),1.0/getZoomDeltaValue())
 * </ul>
 */
public interface IImageField extends IFormField, IDNDSupport {
  String PROP_IMAGE_ID = "imageId";
  String PROP_IMAGE = "image";
  String PROP_IMAGE_TRANSFORM = "imageTransform";
  String PROP_CANVAS = "canvas";
  String PROP_ANALYSIS_RECTANGLE = "analysisRectangle";
  String PROP_AUTO_FIT = "autoFit";
  String PROP_FOCUS_VISIBLE = "focusVisible";
  String PROP_SCROLL_BAR_ENABLED = "scrollBarEnabled";

  String getImageId();

  void setImageId(String imageId);

  Object getImage();

  void setImage(Object imgObj);

  byte[] getByteArrayValue();

  IMenu[] getMenus();

  @Override
  IKeyStroke[] getKeyStrokes();

  boolean isAutoFit();

  void setAutoFit(boolean b);

  BoundsSpec getAnalysisRectangle();

  void setAnalysisRectangle(int x, int y, int width, int heigth);

  void setAnalysisRectangle(BoundsSpec rect);

  AffineTransform getAffineTransform();

  void setAffineTransform(AffineTransform t);

  void setCanvas(int x, int y, int width, int height);

  void setCanvas(BoundsSpec bounds);

  BoundsSpec getCanvas();

  boolean isFocusVisible();

  void setFocusVisible(boolean b);

  void addImageFieldListener(ImageFieldListener listener);

  void removeImageFieldListener(ImageFieldListener listener);

  IImageFieldUIFacade getUIFacade();

  boolean isScrollBarEnabled();

  void setScrollBarEnabled(boolean b);
}
