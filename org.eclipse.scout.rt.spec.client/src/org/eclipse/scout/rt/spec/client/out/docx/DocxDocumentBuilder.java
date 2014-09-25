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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.spec.client.internal.Activator;

/**
 *
 */
public class DocxDocumentBuilder extends DocumentBuilder {

  private SpecDocxAdapter m_docxAdapter;
  private File m_destFile;

  enum Style {
    NORMAL("Standard"),
    HEADING_1("Heading1"),
    HEADING_2("Heading2");

    private String name;

    Style(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  private static class Paragraph {
    StringBuilder sb = new StringBuilder();
    Style style = Style.NORMAL;
  }

  private Paragraph m_currentParagraph = null;
  private BlockType m_currentBlock = null;

  public DocxDocumentBuilder(File out) {
    m_destFile = out;
    URL resource = Activator.getDefault().getBundle().getResource("resources/spec/empty.docx");
    URI uri = null;
    try {
      URL url = FileLocator.resolve(resource);
      uri = url.toURI();
    }
    catch (URISyntaxException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    File template = new File(uri);
    System.out.println(template.exists());

    try {
      m_docxAdapter = new SpecDocxAdapter(template);
    }
    catch (ProcessingException e) {
      e.printStackTrace();
    }

//    try {
//      m_docxAdapter = new SpecDocxAdapter();
//    }
//    catch (ProcessingException e) {
//      e.printStackTrace();
//    }
  }

  @Override
  public void beginDocument() {
    System.out.println("beginDocument()");
//    m_docxAdapter.getPackage().getMainDocumentPart().addStyledParagraphOfText("Heading2", "This is the begining...");
//    m_docxAdapter.addParagraph("berschrift1", "berschrift1...");
//    m_docxAdapter.addParagraph("BSI Standard", "BSI Standard...");
  }

  @Override
  public void endDocument() {
    System.out.println("endDocument()");
    try {
      m_docxAdapter.saveAs(m_destFile.getAbsolutePath());
    }
    catch (ProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void beginBlock(BlockType type, Attributes attributes) {
    System.out.println("beginBlock: " + type.name() + " - " + attributeString(attributes));
    m_currentBlock = type;
    if (type == BlockType.PARAGRAPH) {
      m_currentParagraph = new Paragraph();
    }
  }

  @Override
  public void endBlock() {
    System.out.println("endBlock");
    if (m_currentBlock == BlockType.PARAGRAPH) {
      addCurrentParagraph();
    }
    m_currentBlock = null;
  }

  private void addCurrentParagraph() {
    m_docxAdapter.getPackage().getMainDocumentPart().addStyledParagraphOfText(m_currentParagraph.style.getName(), m_currentParagraph.sb.toString());
  }

  @Override
  public void beginSpan(SpanType type, Attributes attributes) {
    System.out.println("beginSpan: " + type.name() + " - " + attributeString(attributes));
  }

  @Override
  public void endSpan() {
    System.out.println("endSpan");
  }

  @Override
  public void beginHeading(int level, Attributes attributes) {
    System.out.println("beginHeading: " + level + " - " + attributeString(attributes));
    m_currentParagraph = new Paragraph();
    switch (level) {
      case 1:
      case 2:
        m_currentParagraph.style = Style.HEADING_1;
        break;
      case 3:
        m_currentParagraph.style = Style.HEADING_2;
        break;
      default:
        m_currentParagraph.style = Style.NORMAL;
    }
  }

  @Override
  public void endHeading() {
    System.out.println("endHeading");
    addCurrentParagraph();
  }

  @Override
  public void characters(String text) {
    System.out.println("characters: " + text);
    if (m_currentParagraph != null) {
      m_currentParagraph.sb.append(text);
    }
  }

  @Override
  public void entityReference(String entity) {
    System.out.println("entityReference: " + entity);
  }

  @Override
  public void image(Attributes attributes, String url) {
    System.out.println("image: " + url + " - " + attributeString(attributes));
  }

  @Override
  public void link(Attributes attributes, String hrefOrHashName, String text) {
    System.out.println("link: " + hrefOrHashName + " - " + text + " - " + attributeString(attributes));
  }

  @Override
  public void imageLink(Attributes linkAttributes, Attributes imageAttributes, String href, String imageUrl) {
    System.out.println("imageLink: " + href + " - " + imageUrl + " - " + attributeString(linkAttributes) + " - " + attributeString(imageAttributes));
  }

  @Override
  public void acronym(String text, String definition) {
    System.out.println("acronym: " + text + " - " + definition);
  }

  @Override
  public void lineBreak() {
    System.out.println("lineBreak");
  }

  @Override
  public void charactersUnescaped(String literal) {
    System.out.println("charactersUnescaped" + " - " + literal);
  }

  private String attributeString(Attributes attr) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    sb.append("id: ").append(attr.getId()).append("; ");
    sb.append("title: ").append(attr.getTitle()).append("; ");
    sb.append("class: ").append(attr.getClass()).append("; ");
    sb.append("language: ").append(attr.getLanguage()).append("; ");
    sb.append("cssClass: ").append(attr.getCssClass()).append("; ");
    sb.append("cssStyle: ").append(attr.getCssStyle()).append("; ");
    sb.append("]");
    return sb.toString();
  }

}
