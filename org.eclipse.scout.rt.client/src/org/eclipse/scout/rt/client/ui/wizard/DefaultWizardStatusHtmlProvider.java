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
package org.eclipse.scout.rt.client.ui.wizard;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.scout.commons.IOUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.services.common.icon.IconSpec;
import org.eclipse.scout.rt.client.ui.IIconLocator;
import org.eclipse.scout.rt.shared.AbstractIcons;
import org.eclipse.scout.rt.shared.services.common.file.RemoteFile;
import org.eclipse.scout.rt.shared.ui.UserAgentUtility;

/**
 *
 */
public class DefaultWizardStatusHtmlProvider implements IWizardStatusHtmlProvider {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(DefaultWizardStatusHtmlProvider.class);

  private String m_htmlTemplate;

  /**
   * initialize, load html template and inline images
   */
  @Override
  public void initialize(AbstractWizardStatusField htmlField) throws ProcessingException {
    m_htmlTemplate = initHtmlTemplate();

    // collect attachments for HTML field
    List<RemoteFile> attachments = collectAttachments();
    if (attachments != null && attachments.size() > 0) {
      htmlField.setAttachments(attachments);
    }
  }

  @Override
  public String initHtmlTemplate() throws ProcessingException {
    try {
      return new String(IOUtility.getContent(org.eclipse.scout.rt.client.Activator.getDefault().getBundle().getResource("resources/html/defaultWizardStatus.html").openStream()), "iso-8859-1");
    }
    catch (Throwable t) {
      throw new ProcessingException("Unexpected", t);
    }
  }

  protected String getHtmlTemplate() {
    return m_htmlTemplate;
  }

  @Override
  public String createHtml(IWizard w) throws ProcessingException {
    String html = m_htmlTemplate;
    String topPart = "";
    String bottomPart = "";
    StringBuilder listPart = new StringBuilder();
    if (w != null) {
      if (w.getTooltipText() != null) {
        topPart = "<div class=\"infoBox\">" + StringUtility.nvl(w.getTitleHtml(), w.getTooltipText()) + "</div>";
      }
      if (w.getActiveStep() != null) {
        if (w.getActiveStep().getTooltipText() != null || w.getActiveStep().getDescriptionHtml() != null) {
          bottomPart = "<div class=\"infoBox\">" + StringUtility.nvl(w.getActiveStep().getDescriptionHtml(), w.getActiveStep().getTooltipText()) + "</div>";
        }
      }
      int index = 1;
      for (IWizardStep<?> step : w.getSteps()) {
        String s = createHtmlForStep(step, index, (step == w.getActiveStep()));
        if (StringUtility.hasText(s)) {
          listPart.append(s);
          index++;
        }
      }
    }
    html = html.replace("#FONT_SIZE_UNIT#", UserAgentUtility.getFontSizeUnit());
    html = html.replace("#TOP#", topPart);
    html = html.replace("#LIST#", listPart.toString());
    html = html.replace("#BOTTOM#", bottomPart);
    return html;
  }

  /**
   * Adds a step to the HTML document. Uses old school HTML 3.2 with transparent graphics to enforce heights and widths
   * background colors since HTMLEditorToolkit of swing does not support CSS level 2.
   */
  protected String createHtmlForStep(IWizardStep<?> step, int index, boolean selected) {
    String cssClass;
    if (selected) {
      cssClass = "selected";
    }
    else if (step.isEnabled()) {
      cssClass = "default";
    }
    else {
      cssClass = "disabled";
    }
    StringBuilder buf = new StringBuilder();
    String spacerCssClass = "selected".equals(cssClass) ? "spacerselected" : "spacer";
    appendHtmlForSpacerLine(buf, spacerCssClass, 7, AbstractWizardStatusField.STEP_ANCHOR_IDENTIFIER + index);
    buf.append("<tr class=\"" + cssClass + "\">\n");
    buf.append(" <td width=\"15\"><img src=\"empty.png\" width=\"1\" height=\"30\"></td>\n");
    buf.append(" <td width=\"24\" valign=\"top\" class=\"bullet\" style=\"padding:0px;padding-top:" + (UserAgentUtility.isRichClient() ? "4" : "5") + "px;\">" + index + "</td>\n");
    buf.append(" <td width=\"17\"></td>\n");
    buf.append(" <td style=\"padding-top:2px;\">" + StringUtility.nvl(step.getTitleHtml(), step.getTitle()) + "</td>\n");
    buf.append(" <td width=\"15\"></td>\n");
    buf.append("</tr>\n");
    appendHtmlForSpacerLine(buf, spacerCssClass, 11, null);
    appendHtmlForSpacerLine(buf, "line", 1, null);
    return buf.toString();
  }

  protected void appendHtmlForSpacerLine(StringBuilder buf, String cssClass, int height, String anchor) {
    buf.append("<tr class=\"" + cssClass + "\"><td colspan=\"5\">");
    if (!StringUtility.isNullOrEmpty(anchor)) {
      buf.append("<a name=\"" + anchor + "\"/>");
    }
    buf.append("<img src=\"empty.png\" width=\"1\" height=\"" + height + "\"></td></tr>\n");
  }

  /**
   * To be overwritten in order to provide custom attachments. <br/>
   * The default implementation provides default icons for
   * wizard steps.
   */
  protected List<RemoteFile> collectAttachments() {
    List<RemoteFile> attachments = new LinkedList<RemoteFile>();

    loadIcon(attachments, AbstractIcons.Empty + ".png");
    loadIcon(attachments, AbstractIcons.WizardBullet + ".png");
    loadIcon(attachments, AbstractIcons.WizardBullet + "_disabled.png");
    loadIcon(attachments, AbstractIcons.WizardBullet + "_selected.png");

    return attachments;
  }

  /**
   * To load an icon into the given attachments live list
   */
  protected void loadIcon(List<RemoteFile> attachments, String iconName) {
    if (attachments == null || iconName == null) {
      return;
    }
    String tempIconName = iconName;
    ByteArrayInputStream is = null;
    try {
      int index;
      // determine file format
      String format = null;
      index = tempIconName.lastIndexOf('.');
      if (index > 0) {
        format = tempIconName.substring(index);
        tempIconName = tempIconName.substring(0, index);
      }
      // determine icon base name
      String baseIconName = tempIconName;
      index = tempIconName.lastIndexOf('_');
      if (index > 0) {
        baseIconName = tempIconName.substring(0, index);
      }

      // load icon
      IIconLocator iconLocator = ClientSyncJob.getCurrentSession().getIconLocator();
      IconSpec iconSpec = iconLocator.getIconSpec(tempIconName);
      if (iconSpec == null && !tempIconName.equals(baseIconName)) {
        iconSpec = iconLocator.getIconSpec(baseIconName);
      }

      if (iconSpec != null) {
        RemoteFile iconFile = new RemoteFile(StringUtility.join("", tempIconName, format), 0);
        is = new ByteArrayInputStream(iconSpec.getContent());
        iconFile.readData(is);
        is.close();
        attachments.add(iconFile);
      }
    }
    catch (Throwable t) {
      LOG.warn("Failed to load icon '" + tempIconName + "'", t);
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (Throwable t) {
          // nop
        }
      }
    }
  }
}
