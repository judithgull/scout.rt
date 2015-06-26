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
package org.eclipse.scout.rt.ui.rap.ext;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.rt.client.ui.form.fields.ScoutFieldStatus;
import org.eclipse.scout.rt.shared.AbstractIcons;
import org.eclipse.scout.rt.shared.data.basic.FontSpec;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.LogicalGridData;
import org.eclipse.scout.rt.ui.rap.basic.comp.CLabelEx;
import org.eclipse.scout.rt.ui.rap.extension.ILookAndFeelDecorations;
import org.eclipse.scout.rt.ui.rap.extension.UiDecorationExtensionPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class StatusLabelEx extends Composite implements ILabelComposite {
  private static final long serialVersionUID = 1L;
  private static final String STAR_MARKER = "*";
  private static final String WHITE_SPACE = " ";

  private static final boolean GRAB_HORIZONTAL_LABEL = true; // The label must grab horizontal space in order to allow horizontal alignment of the label.
  private static final boolean GRAB_HORIZONTAL_STATUS = false;

  // Represents the effective visibility status set for this composite.
  // This flag is required to still visualize error and mandatory status, even if this composite is not visible.
  private boolean m_visible = true;
  private IProcessingStatus m_status;
  private boolean m_mandatory;
  private boolean m_enabled;
  private Control m_label;

  private Label m_statusLabel;
  private final Image m_infoImg;
  private final Image m_warningImg;
  private final Image m_errorImg;

  private String m_preMarker = "";
  private String m_postMarker = "";
  private Font m_nonMandatoryFont;
  protected Font m_mandatoryFont;
  private Color m_nonMandatoryForegroundColor;
  protected Color m_mandatoryForegroundColor;
  private String m_nonMandatoryText = "";

  public StatusLabelEx(Composite parent, int style) {
    super(parent, SWT.NO_FOCUS);
    m_infoImg = getUiEnvironment().getIcon(AbstractIcons.StatusInfo);
    m_warningImg = getUiEnvironment().getIcon(AbstractIcons.StatusWarning);
    m_errorImg = getUiEnvironment().getIcon(AbstractIcons.StatusError);

    createContent(this, style);
    createLayout();

    m_nonMandatoryFont = m_label.getFont();
    m_nonMandatoryForegroundColor = m_label.getForeground();
    m_enabled = super.getEnabled();
  }

  protected void createLayout() {
    GridLayoutFactory.swtDefaults().numColumns(2).spacing(0, 0).margins(0, 0).applyTo(this);
  }

  protected void createContent(Composite parent, int style) {
    m_label = new CLabelEx(parent, style | getUiEnvironment().getFormToolkit().getFormToolkit().getOrientation());
    getUiEnvironment().getFormToolkit().getFormToolkit().adapt(m_label, false, false);

    m_statusLabel = new Label(parent, SWT.NONE);
    getUiEnvironment().getFormToolkit().getFormToolkit().adapt(m_statusLabel, false, false);

    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(GRAB_HORIZONTAL_LABEL, true).applyTo(m_label); // do not make the label to grab horizontal space so that scaling in sequence box works properly.
    GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).grab(GRAB_HORIZONTAL_STATUS, false).indent(SWT.NONE, 3).applyTo(m_statusLabel);
  }

  protected IRwtEnvironment getUiEnvironment() {
    return (IRwtEnvironment) getDisplay().getData(IRwtEnvironment.class.getName());
  }

  /**
   * Reads the mandatory settings if not already read
   */
  protected void initMandatorySettings() {
    if (m_mandatoryFont == null) {
      FontSpec labelFontSpec = UiDecorationExtensionPoint.getLookAndFeel().getMandatoryLabelFont();
      if (labelFontSpec != null) {
        m_mandatoryFont = getUiEnvironment().getFont(labelFontSpec, getNonMandatoryFont());
      }
    }

    if (m_mandatoryForegroundColor == null) {
      String labelTextColor = UiDecorationExtensionPoint.getLookAndFeel().getMandatoryLabelTextColor();
      if (labelTextColor != null) {
        m_mandatoryForegroundColor = getUiEnvironment().getColor(labelTextColor);
      }
    }

    if (!StringUtility.hasText(m_postMarker) && !StringUtility.hasText(m_preMarker)) {
      int starPos = UiDecorationExtensionPoint.getLookAndFeel().getMandatoryStarMarkerPosition();
      if (starPos != ILookAndFeelDecorations.STAR_MARKER_NONE) {
        switch (starPos) {
          case ILookAndFeelDecorations.STAR_MARKER_AFTER_LABEL:
            m_postMarker = STAR_MARKER + WHITE_SPACE;
            break;
          case ILookAndFeelDecorations.STAR_MARKER_BEFORE_LABEL:
            m_preMarker = STAR_MARKER + WHITE_SPACE;
            break;
        }
      }
    }

  }

  @Override
  public void setLayoutWidthHint(int w) {
    Object o = getLayoutData();
    if (o instanceof LogicalGridData) {
      LogicalGridData data = (LogicalGridData) o;
      data.widthHint = w;
    }
  }

  /**
   * @param b
   * @return if the layout has to be updated up to the top container.
   */
  @Override
  public boolean setMandatory(boolean mandatory) {
    if (isMandatory() == mandatory) {
      return false;
    }
    m_mandatory = mandatory;

    if (mandatory) {
      initMandatorySettings();
    }

    boolean updateLayout = false;
    if (getMandatoryFont() != null) {
      updateLabelFont();
      updateLayout = true;
    }
    if (getMandatoryForegroundColor() != null) {
      updateLabelForeground();
      updateLayout = true;
    }
    if (getPreMarker() != null) {
      updateText();
      updateLayout = true;
    }
    if (getPostMarker() != null) {
      updateMandatoryStatus();
      updateLayout = true;
    }

    return updateLayout;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (m_enabled == enabled) {
      return;
    }

    // only mark/display it disabled otherwise the tooltip caused by shortened label text is only visible in enabled state
    m_enabled = enabled;

    if (enabled) {
      setForeground(null);
    }
    else {
      setForeground(getUiEnvironment().getColor(UiDecorationExtensionPoint.getLookAndFeel().getColorForegroundDisabled()));
    }
  }

  /**
   * @return always returns <code>true</code> because RAP only presents tooltips for enabled widgets.
   * @see EventHandler.js#_onmouseevent_post: <code>... if(vDispatchTarget.getEnabled()) ...</code>
   * @see Bugzilla https://bugs.eclipse.org/bugs/show_bug.cgi?id=445192
   * @see Bugzilla https://bugs.eclipse.org/bugs/show_bug.cgi?id=383073
   */
  @Override
  public boolean getEnabled() {
    return true;
  }

  protected void updateMandatoryStatus() {
    if (m_status != null) {
      return;
    }
    if (isMandatory()) {
      m_statusLabel.setText(m_postMarker);
      m_statusLabel.setVisible(true);
      ((GridData) getStatusLabel().getLayoutData()).exclude = false;
    }
    else {
      m_statusLabel.setVisible(false);
      ((GridData) getStatusLabel().getLayoutData()).exclude = true;
    }
    layout(true, true);
  }

  protected void updateLabelForeground() {
    //Update the foreground only if the field is enabled otherwise the disabled state would not be visible
    if (isEnabled() && isMandatory()) {
      m_label.setForeground(getMandatoryForegroundColor());
    }
    else {
      m_label.setForeground(getNonMandatoryForegroundColor());
    }
  }

  protected void updateLabelFont() {
    if (isMandatory()) {
      m_label.setFont(getMandatoryFont());
    }
    else {
      m_label.setFont(getNonMandatoryFont());
    }
  }

  protected void updateText() {
    if (isMandatory()) {
      setLabelText(m_preMarker + getNonMandatoryText());
    }
    else {
      setLabelText(getNonMandatoryText());
    }
  }

  protected void setLabelText(String text) {
    if (m_label instanceof CLabel) {
      ((CLabel) m_label).setText(text);
    }
  }

  protected String getLabelText() {
    if (m_label instanceof CLabel) {
      return ((CLabel) m_label).getText();
    }

    return null;
  }

  @Override
  public void setStatus(IProcessingStatus status) {
    m_status = status;
    if (m_status == null) {
      getStatusLabel().setToolTipText("");
      getStatusLabel().setImage(null);
      getStatusLabel().setVisible(false);
      if (getStatusLabel().getLayoutData() instanceof GridData) {
        ((GridData) getStatusLabel().getLayoutData()).exclude = true;
      }
      if (isMandatory()) {
        updateMandatoryStatus();
      }
    }
    else {
      String iconId = m_status instanceof ScoutFieldStatus ? ((ScoutFieldStatus) m_status).getIconId() : null;
      if (iconId != null) {
        getStatusLabel().setImage(getUiEnvironment().getIcon(iconId));
      }
      else {
        switch (m_status.getSeverity()) {
          case IProcessingStatus.FATAL:
          case IProcessingStatus.ERROR:
            getStatusLabel().setImage(m_errorImg);
            break;
          case IProcessingStatus.WARNING:
            getStatusLabel().setImage(m_warningImg);
            break;
          default:
            getStatusLabel().setImage(m_infoImg);
            break;
        }
      }
      // tooltip
      StringBuffer buf = new StringBuffer();
      if (m_status.getTitle() != null) {
        buf.append(m_status.getTitle());
      }
      if (m_status.getMessage() != null) {
        if (buf.length() > 0) {
          buf.append("\n");
        }
        buf.append(m_status.getMessage());
      }
      getStatusLabel().setToolTipText(buf.toString());
      getStatusLabel().setVisible(true);
      if (getStatusLabel().getLayoutData() instanceof GridData) {
        ((GridData) getStatusLabel().getLayoutData()).exclude = false;
      }
    }
    layout(true, true);
  }

  @Override
  public void setVisible(boolean visible) {
    m_visible = visible;
    super.setVisible(visible);
  }

  @Override
  public void setStatusVisible(boolean visible) {
    // Status
    boolean statusVisible = visible;
    Label statusLabel = getStatusLabel();
    if (statusLabel != null) {
      GridDataFactory.createFrom((GridData) statusLabel.getLayoutData()).exclude(!statusVisible);
      statusLabel.setVisible(statusVisible);
    }

    // Label
    boolean labelVisible = m_visible && StringUtility.hasText(m_nonMandatoryText);
    Control label = getLabel();
    if (label != null) {
      GridDataFactory.createFrom((GridData) label.getLayoutData()).exclude(!labelVisible);
      label.setVisible(labelVisible);
    }

    layout(true, true);

    // Make this compound label visible if status is to be displayed.
    if (statusVisible) {
      super.setVisible(true); // super-call to not change the effective 'm_visible' value.
    }
    else {
      super.setVisible(labelVisible); // super-call to not change the effective 'm_visible' value.
    }
  }

  @Override
  public void setGrabHorizontalEnabled(boolean enabled) {
    // Grabbing should be disabled if being used within a sequence box, so label, mandatory and error flag are displayed as expected.
    ((GridData) m_label.getLayoutData()).grabExcessHorizontalSpace = (enabled ? GRAB_HORIZONTAL_LABEL : false);
    ((GridData) m_statusLabel.getLayoutData()).grabExcessHorizontalSpace = (enabled ? GRAB_HORIZONTAL_STATUS : false);
  }

// delegate methods
  @Override
  public String getText() {
    return getLabelText();
  }

  @Override
  public void setText(String text) {
    if (text == null) {
      text = "";
    }

    m_nonMandatoryText = text;
    updateText();
  }

  @Override
  public void setBackground(Color color) {
    super.setBackground(color);

    m_label.setBackground(color);
  }

  @Override
  public void setForeground(Color color) {
    super.setForeground(color);

    m_nonMandatoryForegroundColor = color;
    updateLabelForeground();
  }

  @Override
  public void setFont(Font font) {
    super.setFont(font);

    m_nonMandatoryFont = font;
    updateLabelFont();
  }

  @Override
  public Font getFont() {
    return m_label.getFont();
  }

  public Font getNonMandatoryFont() {
    return m_nonMandatoryFont;
  }

  public void setNonMandatoryFont(Font nonMandatoryFont) {
    m_nonMandatoryFont = nonMandatoryFont;
  }

  public Font getMandatoryFont() {
    return m_mandatoryFont;
  }

  public void setMandatoryFont(Font mandatoryFont) {
    m_mandatoryFont = mandatoryFont;
  }

  public Color getNonMandatoryForegroundColor() {
    return m_nonMandatoryForegroundColor;
  }

  public void setNonMandatoryForegroundColor(Color nonMandatoryForegroundColor) {
    m_nonMandatoryForegroundColor = nonMandatoryForegroundColor;
  }

  public Color getMandatoryForegroundColor() {
    return m_mandatoryForegroundColor;
  }

  public void setMandatoryForegroundColor(Color mandatoryForegroundColor) {
    m_mandatoryForegroundColor = mandatoryForegroundColor;
  }

  public String getPreMarker() {
    return m_preMarker;
  }

  public String getPostMarker() {
    return m_postMarker;
  }

  public Label getStatusLabel() {
    return m_statusLabel;
  }

  protected void setStatusLabel(Label statusLabel) {
    m_statusLabel = statusLabel;
  }

  public boolean isMandatory() {
    return m_mandatory;
  }

  public String getNonMandatoryText() {
    return m_nonMandatoryText;
  }

  protected void setLabel(Control label) {
    m_label = label;
  }

  public Control getLabel() {
    return m_label;
  }
}
