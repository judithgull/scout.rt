/*******************************************************************************
 * Copyright (c) 2014 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.spec.client.out.docx;

import java.io.File;

import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.docx4j.DocxAdapter;

/**
 *
 */
public class SpecDocxAdapter extends DocxAdapter {

  /**
   * @throws ProcessingException
   */
  public SpecDocxAdapter(File template) throws ProcessingException {
    super(template);
  }

  public SpecDocxAdapter() throws ProcessingException {
    super(DocxAdapter.createEmptyPackage());
  }

  public void addParagraph(String styleId, String text) {
    MainDocumentPart mainDocumentPart = getPackage().getMainDocumentPart();
    mainDocumentPart.addStyledParagraphOfText(styleId, text);
  }
}
