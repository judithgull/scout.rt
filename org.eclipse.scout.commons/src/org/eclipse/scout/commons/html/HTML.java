/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons.html;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.scout.commons.html.internal.EmptyHtmlNodeBuilder;
import org.eclipse.scout.commons.html.internal.HtmlContentBuilder;
import org.eclipse.scout.commons.html.internal.HtmlDocumentBuilder;
import org.eclipse.scout.commons.html.internal.HtmlImageBuilder;
import org.eclipse.scout.commons.html.internal.HtmlInputBuilder;
import org.eclipse.scout.commons.html.internal.HtmlLinkBuilder;
import org.eclipse.scout.commons.html.internal.HtmlListElement;
import org.eclipse.scout.commons.html.internal.HtmlNodeBuilder;
import org.eclipse.scout.commons.html.internal.HtmlPlainBuilder;
import org.eclipse.scout.commons.html.internal.HtmlTableBuilder;
import org.eclipse.scout.commons.html.internal.HtmlTableDataBuilder;
import org.eclipse.scout.commons.html.internal.HtmlTableRowBuilder;
import org.eclipse.scout.commons.html.internal.StyleElementBuilder;

/**
 * Convenience for building a html document or parts of it with encoded binds. <br>
 * Only the most common cases are supported, not intended to be complete.
 * 
 * @since 5.1 (backported)
 */
public final class HTML {

  private static final Pattern htmlMarkupAmp = Pattern.compile("[&]");
  private static final Pattern htmlMarkupLt = Pattern.compile("[<]");
  private static final Pattern htmlMarkupGt = Pattern.compile("[>]");
  private static final Pattern htmlMarkupQuot = Pattern.compile("[\"]");
  private static final Pattern htmlMarkupApo = Pattern.compile("[']");
  private static final Pattern htmlMarkupSlash = Pattern.compile("[/]");

  /**
   * Utility class
   */
  private HTML() {
  }

  /**
   * Create a html element with encoded text for &lt;head&gt;text&lt;/head&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.head("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlContent head(CharSequence... elements) {
    return new HtmlNodeBuilder("head", elements);
  }

  /**
   * Create a html element with encoded text for &lt;body&gt;text&lt;/body&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.body("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlContent body(CharSequence... elements) {
    return new HtmlNodeBuilder("body", elements);
  }

  /**
   * Create a html element with encoded text for &lt;b&gt;text&lt;/b&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.bold("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement bold(CharSequence... text) {
    return new HtmlNodeBuilder("b", text);
  }

  /**
   * Create a html element with encoded text for &lt;i&gt;text&lt;/i&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.italic("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement italic(CharSequence... text) {
    return new HtmlNodeBuilder("i", text);
  }

  /**
   * Create a html element with encoded text for &lt;p&gt;text&lt;/p&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.p("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement p(CharSequence... text) {
    return new HtmlNodeBuilder("p", text);
  }

  /**
   * Create a html element for &lt;br&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.br().toEncodedHtml(); <br>
   * </p>
   */
  public static IHtmlElement br() {
    return new EmptyHtmlNodeBuilder("br");
  }

  /**
   * Create a html element with encoded text for &lt;h1&gt;text&lt;/h1&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.h1("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement h1(CharSequence... text) {
    return new HtmlNodeBuilder("h1", text);
  }

  /**
   * Create a html element with encoded text for &lt;h2&gt;text&lt;/h2&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.h2("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement h2(CharSequence... text) {
    return new HtmlNodeBuilder("h2", text);
  }

  /**
   * Create a html element with encoded text for &lt;h3&gt;text&lt;/h3&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.h3("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement h3(CharSequence... text) {
    return new HtmlNodeBuilder("h3", text);
  }

  /**
   * Create a html element with encoded text for &lt;h4&gt;text&lt;/h4&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.h4("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement h4(CharSequence... text) {
    return new HtmlNodeBuilder("h4", text);
  }

  /**
   * Create a html element with encoded text for &lt;h5&gt;text&lt;/h5&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.h5("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement h5(CharSequence... text) {
    return new HtmlNodeBuilder("h5", text);
  }

  /**
   * Create a html element with encoded text for &lt;h6&gt;text&lt;/h6&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.h6("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement h6(CharSequence... text) {
    return new HtmlNodeBuilder("h6", text);
  }

  /**
   * Create a html element with encoded text for &lt;div&gt;content&lt;/div&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.div("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param content
   *          content as bind
   */
  public static IHtmlElement div(CharSequence... content) {
    return new HtmlNodeBuilder("div", Arrays.asList(content));
  }

  /**
   * Create a html element with encoded text for &lt;div&gt;content&lt;/div&gt;.
   * 
   * @param content
   *          content as bind
   */
  public static IHtmlElement div(List<CharSequence> contents) {
    return new HtmlNodeBuilder("div", contents);
  }

  /**
   * Create a html element with encoded text for &lt;span&gt;content&lt;/span&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.span("text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement span(CharSequence... text) {
    return new HtmlNodeBuilder("span", text);
  }

  /**
   * Create a html element with encoded text for a link &lt;a href="url"&gt;text&lt;/a&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.link("http://xyz.com","text").toEncodedHtml(); <br>
   * </p>
   * 
   * @param text
   *          text as bind
   */
  public static IHtmlElement link(CharSequence url, CharSequence text) {
    return new HtmlLinkBuilder(url, text);
  }

  /**
   * Create a html element for an image: &lt;img src="path"&gt;&lt;/img&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.img("image.png").toEncodedHtml(); <br>
   * </p>
   * 
   * @param src
   *          image source path as bind
   */
  public static IHtmlElement img(CharSequence src) {
    return new HtmlImageBuilder(src);
  }

  /**
   * Create a html element for an image: &lt;img src="binaryResource:resourceName"&gt;&lt;/img&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.imgByBinaryResource("resourceName").toEncodedHtml(); <br>
   * </p>
   * 
   * @param src
   *          image source path as bind
   */
  public static IHtmlElement imgByBinaryResource(CharSequence binaryResource) {
    return new HtmlImageBuilder("binaryResource:" + binaryResource);
  }

  /**
   * Create a html element for an image: &lt;img src="iconid:icon_name"&gt;&lt;/img&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.imgByIconId("icon_name").toEncodedHtml(); <br>
   * </p>
   * 
   * @param iconId
   *          image source path as bind
   */
  public static IHtmlElement imgByIconId(CharSequence iconId) {
    return new HtmlImageBuilder("iconId:" + iconId);
  }

  /**
   * Create a html list element with encoded text: &lt;li&gt;text&lt;/li&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.li("text").toEncodedHtml(); <br>
   * </p>
   */
  public static IHtmlListElement li(CharSequence text) {
    return new HtmlListElement(text);
  }

  /**
   * Create an unordered html list element with encoded text: &lt;ul&gt;...&lt;/ul&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.ul(HTML.li("text"),...).toEncodedHtml(); <br>
   * </p>
   */
  public static IHtmlElement ul(IHtmlListElement... li) {
    return ul(Arrays.asList(li));
  }

  /**
   * Create an unordered html list element with encoded text: &lt;ul&gt;...&lt;/ul&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.ul(HTML.li("text"),...).toEncodedHtml(); <br>
   * </p>
   */
  public static IHtmlElement ul(List<IHtmlListElement> li) {
    return new HtmlNodeBuilder("ul", li);
  }

  /**
   * Create an ordered html list element with encoded text: &lt;ul&gt;...&lt;/ul&gt;.
   * <p>
   * Example:<br>
   * String encodedHtml = HTML.ol(HTML.li("text"),...).toEncodedHtml(); <br>
   * </p>
   */
  public static IHtmlContent ol(IHtmlListElement... li) {
    return new HtmlNodeBuilder("ol", li);
  }

  /**
   * Create a html element with encoded text for table data: &lt;td&gt;text&lt;/td&gt;.
   * <p>
   * Example:<br>
   * <code>
   * HTML.table(<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell1"),<br>
        &nbsp;&nbsp;HTML.cell("cell2")<br>
        &nbsp;),<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell3"),<br>
        &nbsp;&nbsp;HTML.cell("cell4")<br>
        )<br>
        ).cellspacing(1).cellpadding(2)<br>
   * </p>
   * </code>
   * 
   * @param text
   *          text with binds
   */
  public static IHtmlTableCell cell(CharSequence... text) {
    return new HtmlTableDataBuilder(Arrays.asList(text));
  }

  /**
   * Create a html element with encoded text for a table row: &lt;tr&gt;...&lt;/tr&gt;.
   * Example:<br>
   * <code>
   * HTML.table(<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell1"),<br>
        &nbsp;&nbsp;HTML.cell("cell2")<br>
        &nbsp;),<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell3"),<br>
        &nbsp;&nbsp;HTML.cell("cell4")<br>
        )<br>
        ).cellspacing(1).cellpadding(2)<br>
   * </p>
   * </code>
   * 
   * @param td
   *          table data within row
   */
  public static IHtmlTableRow row(IHtmlTableCell... td) {
    return row(Arrays.asList(td));
  }

  /**
   * Create a html element with encoded text for a table row: &lt;tr&gt;...&lt;/tr&gt;.
   * Example:<br>
   * <code>
   * HTML.table(<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell1"),<br>
        &nbsp;&nbsp;HTML.cell("cell2")<br>
        &nbsp;),<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell3"),<br>
        &nbsp;&nbsp;HTML.cell("cell4")<br>
        )<br>
        ).cellspacing(1).cellpadding(2)<br>
   * </p>
   * </code>
   * 
   * @param td
   *          table data within row
   */
  public static IHtmlTableRow row(List<IHtmlTableCell> td) {
    return new HtmlTableRowBuilder(td);
  }

  /**
   * Create a html element with encoded text for a table.
   * Example:<br>
   * <code>
   * HTML.table(<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell1"),<br>
        &nbsp;&nbsp;HTML.cell("cell2")<br>
        &nbsp;),<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell3"),<br>
        &nbsp;&nbsp;HTML.cell("cell4")<br>
        )<br>
        ).cellspacing(1).cellpadding(2)<br>
   * </p>
   * </code>
   */
  public static IHtmlTable table(IHtmlTableRow... rows) {
    return new HtmlTableBuilder(Arrays.asList(rows));
  }

  /**
   * Create a html element with encoded text for a table.
   * Example:<br>
   * <code>
   * HTML.table(<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell1"),<br>
        &nbsp;&nbsp;HTML.cell("cell2")<br>
        &nbsp;),<br>
        &nbsp;HTML.row(<br>
        &nbsp;&nbsp;HTML.cell("cell3"),<br>
        &nbsp;&nbsp;HTML.cell("cell4")<br>
        )<br>
        ).cellspacing(1).cellpadding(2)<br>
   * </p>
   * </code>
   */
  public static IHtmlTable table(List<IHtmlTableRow> rows) {
    return new HtmlTableBuilder(rows);
  }

  /**
   * Creates an application local link
   * String encodedHtml = HTML.appLink("path","text").toEncodedHtml(); <br>
   * 
   * @param ref
   *          what the link is referring to
   * @param text
   *          the link text
   */
  public static IHtmlElement appLink(CharSequence ref, CharSequence text) {
    return span(text).appLink(ref);
  }

  /**
   * Creates HTML content from multiple elements. e.g. <b>Bold Text</b> Text <b> More bold text </b>
   */
  public static IHtmlContent fragment(CharSequence... elements) {
    return new HtmlContentBuilder(elements);
  }

  /**
   * Creates HTML content from multiple elements. e.g. <b>Bold Text</b> Text <b> More bold text </b>
   */
  public static IHtmlContent fragment(List<? extends CharSequence> elements) {
    return new HtmlContentBuilder(elements);
  }

  /**
   * Creates HTML content with &ltstyle type="text/css"&gt; cssStype &lt;style&gt;
   */
  public static IHtmlElement cssStyle(CharSequence... cssContent) {
    return new StyleElementBuilder(cssContent).type("text/css");
  }

  /**
   * Creates HTML content with HTML 5 doctype: &lt!DOCTYPE
   * html&gt;&lt;html&gt;&lt;head&gt;...&lt;/head&gt;body&gt;...&lt;/body&gt;&lt;html&gt;.
   */
  public static IHtmlDocument html5(CharSequence head, CharSequence body) {
    return new HtmlDocumentBuilder(head(head), body(body)).doctype();
  }

  /**
   * Creates HTML content with binds: &lt;html&gt;&lt;head&gt;...&lt;/head&gt;body&gt;...&lt;/body&gt;&lt;/html&gt;.
   */
  public static IHtmlDocument html(CharSequence head, CharSequence body) {
    return new HtmlDocumentBuilder(head(head), body(body));
  }

  /**
   * Creates HTML content that will not be encoded with {@link IHtmlContent#toEncodedHtml()}. <b>Use with caution!</b>
   */
  public static IHtmlContent plain(CharSequence... text) {
    return new HtmlPlainBuilder(text);
  }

  /**
   * Creates a html input field.
   */
  public static IHtmlInput input() {
    return new HtmlInputBuilder();
  }
}
