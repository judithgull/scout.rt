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
package org.eclipse.scout.rt.spec.client.out.docx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.mediawiki.core.MediaWikiLanguage;
import org.eclipse.scout.commons.exception.ProcessingException;

/**
 * Converts a mediawiki file to html
 */
public class DocxConverter {

  private static final String ENCODING = "utf-8";

  public DocxConverter() {
  }

  public void convertWikiToDocx(File in, File out) throws ProcessingException {
    try {

      AsposeDocxDocumentBuilder builder = new AsposeDocxDocumentBuilder(out);

      MarkupParser parser = new MarkupParser(new MediaWikiLanguage());
      parser.setBuilder(builder);
      Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(in.getPath()), ENCODING));
      parser.parse(reader);

    }
    catch (FileNotFoundException e) {
      throw new ProcessingException("Could not convert document to html", e);
    }
    catch (IOException e) {
      throw new ProcessingException("Could not convert document to html", e);
    }
  }
}
