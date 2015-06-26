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
package org.eclipse.scout.rt.ui.rap.form.fields.filechooserfield;

import java.io.File;
import java.util.List;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.IOUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.basic.filechooser.IFileChooser;
import org.eclipse.scout.rt.client.ui.form.fields.filechooserfield.IFileChooserField;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.ui.rap.LogicalGridLayout;
import org.eclipse.scout.rt.ui.rap.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.rap.form.fields.RwtScoutValueFieldComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class RwtScoutFileDownloadField extends RwtScoutValueFieldComposite<IFileChooserField> implements IRwtScoutFileDownloadField {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(RwtScoutFileDownloadField.class);

  private Label m_dummyText;

  public RwtScoutFileDownloadField() {
  }

  @Override
  protected void initializeUi(Composite parent) {
    super.initializeUi(parent);
    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    StatusLabelEx label = getUiEnvironment().getFormToolkit().createStatusLabel(container, getScoutObject());
    m_dummyText = getUiEnvironment().getFormToolkit().createLabel(container, "", SWT.NONE);
    m_dummyText.setEnabled(false);

    setUiContainer(container);
    setUiLabel(label);
    setUiField(m_dummyText);

    // layout
    container.setLayout(new LogicalGridLayout(1, 0));
  }

  @Override
  public Label getUiField() {
    return (Label) super.getUiField();
  }

  @Override
  protected void setValueFromScout() {
    File f = getScoutObject().getValueAsFile();
    if (f != null) {
      String name = f.getName();
      if (name.endsWith(".tmp")) {
        m_dummyText.setText("(" + TEXTS.get("Automatic") + ")");
      }
      else {
        m_dummyText.setText(name);
      }
    }
    else {
      m_dummyText.setText("");
    }
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    // notify Scout
    Runnable t = new Runnable() {
      @Override
      public void run() {
        try {
          IFileChooser chooser = getScoutObject().getFileChooser();
          String fileName = chooser.getFileName();
          if (fileName == null) {
            List<String> exts = getScoutObject().getFileExtensions();
            fileName = "download." + (CollectionUtility.hasElements(exts) ? CollectionUtility.firstElement(exts) : "tmp");
          }
          final File tempFile = new File(IOUtility.createTempDirectory("download"), fileName);
          tempFile.deleteOnExit();
          getScoutObject().getUIFacade().setTextFromUI(tempFile.getAbsolutePath());
        }
        catch (Exception e) {
          LOG.error("Failed creating temporary file for " + getScoutObject().getClass(), e);
        }
      }
    };
    getUiEnvironment().invokeScoutLater(t, 0);
    // end notify
  }
}
