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

import java.awt.Color;
import java.io.File;

import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder;

import com.aspose.words.Document;
import com.aspose.words.ListTemplate;
import com.aspose.words.Style;
import com.aspose.words.StyleType;

/**
 *
 */
public class AsposeDocxDocumentBuilder extends DocumentBuilder {

  private File m_destFile;
  private com.aspose.words.DocumentBuilder m_builder;
  private Document m_doc;
  private Style m_heading_1;
  private Style m_heading_2;

  public AsposeDocxDocumentBuilder(File out) {
    m_destFile = out;

    try {
      m_doc = new Document();
      m_builder = new com.aspose.words.DocumentBuilder(m_doc);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    try {
      initStyles();
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void initStyles() throws Exception {
    m_heading_1 = m_doc.getStyles().add(StyleType.PARAGRAPH, "SpecHeading1");
    m_heading_1.getFont().setName("Calibri");
    m_heading_1.getFont().setSize(16d);
    m_heading_1.getFont().setAllCaps(true);
    m_heading_1.getFont().setColor(new Color(254, 153, 21));
    m_heading_1.getParagraphFormat().setSpaceAfter(14d);
    m_heading_1.getListFormat().setList(m_doc.getLists().add(ListTemplate.NUMBER_DEFAULT));
    m_heading_1.getListFormat().setListLevelNumber(0);

    m_heading_2 = m_doc.getStyles().add(StyleType.PARAGRAPH, "SpecHeading2");
    m_heading_2.getFont().setName("Calibri");
    m_heading_2.getFont().setSize(12d);
    m_heading_2.getFont().setAllCaps(true);
    m_heading_2.getFont().setColor(new Color(0, 130, 161));
    m_heading_2.getParagraphFormat().setSpaceAfter(10d);
    m_heading_2.getListFormat().setList(m_doc.getLists().add(ListTemplate.NUMBER_DEFAULT));
    m_heading_2.getListFormat().setListLevelNumber(1);
  }

  @Override
  public void beginDocument() {
    System.out.println("beginDocument()");

//    try {
//      m_builder.writeln("Hello World!");
//    }
//    catch (Exception e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }

  }

  @Override
  public void endDocument() {
    System.out.println("endDocument()");
    try {
      m_doc.save(m_destFile.getAbsolutePath());
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void beginBlock(BlockType type, Attributes attributes) {
    System.out.println("beginBlock: " + type.name() + " - " + attributeString(attributes));
    if (type == BlockType.PARAGRAPH) {
      try {
        m_builder.insertParagraph();
      }
      catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Override
  public void endBlock() {
    System.out.println("endBlock");
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
    switch (level) {
      case 1:
      case 2:
        m_builder.getParagraphFormat().setStyle(m_heading_1);
        break;
      case 3:
        m_builder.getParagraphFormat().setStyle(m_heading_2);
        break;
      default:
        m_builder.getParagraphFormat().setStyle(m_doc.getStyles().get("Normal"));
    }

  }

  @Override
  public void endHeading() {
    System.out.println("endHeading");
    m_builder.getParagraphFormat().setStyle(m_doc.getStyles().get("Normal"));
  }

  @Override
  public void characters(String text) {
    System.out.println("characters: " + text);
    try {
      m_builder.writeln(text);
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
