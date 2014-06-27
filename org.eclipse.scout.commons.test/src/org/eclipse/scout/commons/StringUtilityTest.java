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
package org.eclipse.scout.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;

/**
 * JUnit tests for {@link StringUtility}
 */
public class StringUtilityTest {

  /**
   * Test for {@link StringUtility#concatenateTokens(String...)}
   */
  @Test
  public void testConcatenateTokens() {
    assertEquals("", StringUtility.concatenateTokens(""));
    assertEquals("", StringUtility.concatenateTokens((String[]) null));
    assertEquals("", StringUtility.concatenateTokens());
    assertEquals("", StringUtility.concatenateTokens("", ""));
    assertEquals("s0s1", StringUtility.concatenateTokens("s0", "", "s1"));
    assertEquals("s0", StringUtility.concatenateTokens("s0", "-", ""));
    assertEquals("s0", StringUtility.concatenateTokens("s0", "-", null));
    assertEquals("s0-s1", StringUtility.concatenateTokens("s0", "-", "s1"));
    assertEquals("s0-s1-", StringUtility.concatenateTokens("s0", "-", "s1", "-"));
    assertEquals("s0-s1-s2", StringUtility.concatenateTokens("s0", "-", "s1", "-", "s2"));
    assertEquals("s0-s1-s2-", StringUtility.concatenateTokens("s0", "-", "s1", "-", "s2", "-"));
    assertEquals("s0-s1-s2-s3", StringUtility.concatenateTokens("s0", "-", "s1", "-", "s2", "-", "s3"));
    assertEquals("s1-s2", StringUtility.concatenateTokens(null, "-", "s1", "-", "s2"));
    assertEquals("s1-s2", StringUtility.concatenateTokens("", "-", "s1", "-", "s2"));
    assertEquals("s2", StringUtility.concatenateTokens("", "-", null, "-", "s2"));
    assertEquals("s2s3", StringUtility.concatenateTokens("", "-", null, "-", "s2", "", "s3"));
    assertEquals("-", StringUtility.concatenateTokens("-", ""));
    assertEquals("ab", StringUtility.concatenateTokens("a", "-", null, "-", null, "b"));
  }

  @Test
  public void testImplodeDelimiters() {
    String a = "a";
    String b = "b";
    String c = "cccccc";
    String del1 = ";";
    String del2 = ",";
    String del3 = "";
    String delnil = null;
    String res1 = StringUtility.join(del1, a, b, c);
    assertEquals(a + del1 + b + del1 + c, res1);
    String res2 = StringUtility.join(del2, a, b, c);
    assertEquals(a + del2 + b + del2 + c, res2);
    String res3 = StringUtility.join(del3, a, b, c);
    assertEquals(a + b + c, res3);
    String res4 = StringUtility.join(delnil, a, b, c);
    assertEquals(a + b + c, res4);
  }

  @Test
  public void testImplodeNullInput() {
    String a = "a";
    String b = "b";
    String c = "cccccc";
    String del1 = ";";
    String res1 = StringUtility.join(del1);
    assertEquals("", res1);
    String res2 = StringUtility.join(del1, (String) null);
    assertEquals("", res2);
    String res3 = StringUtility.join(del1, a, (String) null, c);
    assertEquals(a + del1 + c, res3);
    String res4 = StringUtility.join(del1, (String) null, b, c);
    assertEquals(b + del1 + c, res4);
  }

  @Test
  public void testImplodeLong() {
    Long[] longs1 = new Long[]{15L, 4L};
    Long[] longs2 = new Long[]{};
    Long[] longs3 = new Long[]{34L, 340283503853L, null, -3431L};
    Long[] longs4 = new Long[]{null};
    String del1 = "'";
    String res1 = StringUtility.join(del1, longs1);
    assertEquals("15'4", res1);
    String res2 = StringUtility.join(del1, longs2);
    assertEquals("", res2);
    String res3 = StringUtility.join(del1, longs3);
    assertEquals("34'340283503853'-3431", res3);
    String res4 = StringUtility.join(del1, longs4);
    assertEquals("", res4);
  }

  @Test
  public void testJoinVararg() {
    assertEquals("", StringUtility.join(null));
    assertEquals("ab", StringUtility.join(null, "a", "b"));
    assertEquals("ab", StringUtility.join(null, "a", null, "b"));
    assertEquals("", StringUtility.join(null, (Object[]) null));
    assertEquals("1, true", StringUtility.join(", ", BigDecimal.ONE, Boolean.TRUE));
  }

  @Test
  public void testJoinStringArray() {
    assertEquals("", StringUtility.join(null, (String[]) null));
    assertEquals("ab", StringUtility.join(null, new String[]{"a", "b"}));
    assertEquals("a, b", StringUtility.join(", ", new String[]{"a", "b"}));
    assertEquals("a, b", StringUtility.join(", ", new String[]{"a", null, "b"}));
  }

  @Test
  public void testJoinLongArray() {
    assertEquals("", StringUtility.join(null, (Long[]) null));
    assertEquals("12", StringUtility.join(null, new Long[]{Long.valueOf(1), Long.valueOf(2)}));
    assertEquals("1, 2", StringUtility.join(", ", new Long[]{Long.valueOf(1), Long.valueOf(2)}));
    assertEquals("1, 2", StringUtility.join(", ", new Long[]{Long.valueOf(1), null, Long.valueOf(2)}));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testJoinCollection() {
    assertEquals("", StringUtility.join(null, (Collection<?>) null));
    assertEquals("abc", StringUtility.join(null, Arrays.asList("a", "b", "c")));
    assertEquals("123", StringUtility.join(null, Arrays.asList(1, 2, 3)));
    assertEquals("11", StringUtility.join(null, Arrays.asList(1, null, BigDecimal.ONE)));
  }

  @Test
  public void testMnemonics() {
    String s = "Button &Test";
    assertEquals('T', StringUtility.getMnemonic(s));
    assertEquals("Button Test", StringUtility.removeMnemonic(s));
    s = "Button & Test";
    assertEquals(0x00, StringUtility.getMnemonic(s));
    assertEquals(s, StringUtility.removeMnemonic(s));
    s = "&test";
    assertEquals('t', StringUtility.getMnemonic(s));
    assertEquals("test", StringUtility.removeMnemonic(s));
    s = "test &";
    assertEquals(0x00, StringUtility.getMnemonic(s));
    assertEquals(s, StringUtility.removeMnemonic(s));

    assertEquals('a', StringUtility.getMnemonic("&a"));
    assertEquals('a', StringUtility.getMnemonic("&abc"));
    assertEquals(0x00, StringUtility.getMnemonic(null));
    assertEquals(0x00, StringUtility.getMnemonic("sometext"));
    assertEquals('1', StringUtility.getMnemonic("&1"));
    assertEquals('1', StringUtility.getMnemonic("\\&1"));
    assertEquals('á', StringUtility.getMnemonic("&á"));
    assertEquals("á", StringUtility.removeMnemonic("&á"));
    assertEquals('&', StringUtility.getMnemonic("&&"));
    assertEquals(null, StringUtility.removeMnemonic(null));
    s = "A & B T&Êxt";
    assertEquals('Ê', StringUtility.getMnemonic(s));
    assertEquals("A & B TÊxt", StringUtility.removeMnemonic(s));
  }

  // UTF-8 length is 13 to avoid accidental buffer size matches
  static final String CHARACTERS = "aouäöüàé";

  @Test
  public void testDecompress_umlauts() throws Exception {

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 100000; i++) {
      builder.append(CHARACTERS);
    }

    String original = builder.toString();
    String decompressed = StringUtility.decompress(StringUtility.compress(original));

    assertEquals(original, decompressed);
  }

  @Test
  public void testTags() throws Exception {
    String s;
    s = "foo <a>text</a> bar";
    assertEquals("foo  bar", StringUtility.removeTag(s, "a"));
    s = "foo <a>text</a> <a>text</a> bar";
    assertEquals("foo   bar", StringUtility.removeTag(s, "a"));
    s = "foo <a>text</a> <a>bar";
    assertEquals("foo  <a>bar", StringUtility.removeTag(s, "a"));
    s = "foo <a>text</a> </a>bar";
    assertEquals("foo  </a>bar", StringUtility.removeTag(s, "a"));
    s = "foo <a/> bar";
    assertEquals("foo  bar", StringUtility.removeTag(s, "a"));
  }

  @Test
  public void testRegExPattern() throws Exception {
    String s;
    s = "test*";
    assertEquals("test.*", StringUtility.toRegExPattern(s));

    s = "test?";
    assertEquals("test.", StringUtility.toRegExPattern(s));

    s = "com.test.*";
    assertEquals("com\\.test\\..*", StringUtility.toRegExPattern(s));
  }

  @Test
  public void testHtmlEncodeBackslash() {
    assertEqualsAfterEncodeDecode("\"");
  }

  @Test
  public void testHtmlEncodeAmp() {
    assertEqualsAfterEncodeDecode("&");
  }

  @Test
  public void testHtmlEncodeLt() {
    assertEqualsAfterEncodeDecode("<");
  }

  @Test
  public void testHtmlEncodeGt() {
    assertEqualsAfterEncodeDecode(">");
  }

  @Test
  public void testHtmlEncodeApostrophe() {
    assertEqualsAfterEncodeDecode("'");
  }

  @Test
  public void testHtmlEncodeBr() {
    assertEqualsAfterEncodeDecode("\n");
  }

  @Test
  public void testHtmlEncodeTab() {
    assertEqualsAfterEncodeDecode("\t");
  }

  @Test
  public void testHtmlEncodeNull() {
    assertEqualsAfterEncodeDecode(null);
  }

  @Test
  public void testHtmlEncodeEmpty() {
    assertEqualsAfterEncodeDecode("");
  }

  @Test
  public void testHtmlEncodeSpace() {
    assertEqualsAfterEncodeDecode("\\s", true);
  }

  @Test
  public void testHtmlEncodeDecodeHref() {
    String testHtml = "<li><a href=\"/home.html\" class=\"active\" title=\"Home\">Home</a></li>";
    assertEqualsAfterEncodeDecode(testHtml);
  }

  /**
   * Test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=347254
   */
  @Test
  public void testHtmlEncodeAmpLT() {
    String testHtml = "<a &lt; b>";
    assertEqualsAfterEncodeDecode(testHtml);
    String htmlDecode = StringUtility.htmlDecode("&lt;a &amp;lt; b&gt;");
    assertEquals("<a &lt; b>", htmlDecode);
  }

  /**
   * Tests if the result string is equal to the original after applying encode and decode.
   *
   * @param original
   *          the original String
   */
  private static void assertEqualsAfterEncodeDecode(String original) {
    assertEqualsAfterEncodeDecode(original, false);
  }

  /**
   * Tests if the result string is equal to the original after applying encode and decode.
   *
   * @param original
   *          the original String
   * @param replaceSpace
   *          replace all spaces when encoding
   */
  private static void assertEqualsAfterEncodeDecode(String original, boolean replaceSpaces) {
    String encoded = StringUtility.htmlEncode(original, replaceSpaces);
    String decoded = StringUtility.htmlDecode(encoded);
    assertEquals(original, decoded);
  }

  /**
   * Tests for {@link StringUtility#getTag(String, String)}.
   */
  @Test
  public void testGetTag() throws Exception {
    String input;

    input = null;
    assertNull(StringUtility.getTag(null, null));
    assertNull(StringUtility.getTag("text", null));
    assertNull(StringUtility.getTag(null, "a"));

    //simple case:
    input = "Lorem ipsum dolor sit amet, <strong>consetetur sadipscing elitr</strong>, sed diam voluptua.";
    assertEquals("consetetur sadipscing elitr", StringUtility.getTag(input, "strong"));

    //with HTML attribute:
    input = "<table>\n" +
        "<tr id=\"a_tr_id\">\n" +
        "<td class=\"a_td_class\" valign=\"top\">Lorem <strong>Claritas: *</strong> ipsum</td>\n" +
        "</tr>\n" +
        "</table>\n";

    assertEquals("Claritas: *", StringUtility.getTag(input, "strong"));
    assertEquals("Lorem <strong>Claritas: *</strong> ipsum", StringUtility.getTag(input, "td"));
    assertEquals("<td class=\"a_td_class\" valign=\"top\">Lorem <strong>Claritas: *</strong> ipsum</td>", StringUtility.getTag(input, "tr"));

    input = "<html>some <b>bold</b>text</html>";
    assertEquals("some <b>bold</b>text", StringUtility.getTag(input, "html"));
    input = "<html><body>some <b>bold</b>text</body></html>";
    assertEquals("some <b>bold</b>text", StringUtility.getTag(input, "body"));
    input = "<html><body font-size: \"12px;\" style=\"background-color: red;\">some <b>bold</b>text</body></html>";
    assertEquals("some <b>bold</b>text", StringUtility.getTag(input, "body"));
    assertEquals(null, StringUtility.getTag(input, "invalidTag"));
  }

  /**
   * Tests for {@link StringUtility#removeTag(String, String)}
   */
  @Test
  public void testRemoveTag() {
    String input = "<html><body>some<b> bold </b> text</body></html>";
    assertEquals(null, StringUtility.removeTag(null, null));
    assertEquals(null, StringUtility.removeTag(null, "tag"));
    assertEquals(input, StringUtility.removeTag(input, null));
    assertEquals(input, StringUtility.removeTag(input, "nonExistingTag"));
    assertEquals("<html></html>", StringUtility.removeTag(input, "body"));
    assertEquals("<html><body>some text</body></html>", StringUtility.removeTag(input, "b"));
  }

  /**
   * Test for {@link StringUtility#replaceTags(String, String, String)}
   */
  @Test
  public void testReplaceTags() throws Exception {
    String input;

    //HTML attributes:
    input = "<table>" +
        "<tr id=\"a_tr_id\">" +
        "<td class=\"a_td_class\" valign=\"top\">Lorem <strong>Claritas: *</strong> ipsum</td>" +
        "</tr>" +
        "</table>";

    assertEquals("X", StringUtility.replaceTags(input, "table", "X"));
    assertEquals("<table>X</table>", StringUtility.replaceTags(input, "tr", "X"));
    assertEquals("<table><tr id=\"a_tr_id\">X</tr></table>", StringUtility.replaceTags(input, "td", "X"));
    assertEquals("<table><tr id=\"a_tr_id\"><td class=\"a_td_class\" valign=\"top\">Lorem X ipsum</td></tr></table>", StringUtility.replaceTags(input, "strong", "X"));
    assertEquals(input, StringUtility.replaceTags(input, "em", "X"));

    //multiple replacement
    input = "Lorem <em>ipsum</em> dolore <em>satis</em> est!";
    assertEquals("Lorem  dolore  est!", StringUtility.replaceTags(input, "em", ""));

    //multiple replacement
    input = "<meta name=\"timestamp\" content=\"01.01.2013\"/>\n" +
        "<meta name=\"date.modified\" content=\"20130314\"/>";
    assertEquals("", StringUtility.replaceTags(input, "meta", "").trim());
  }

  /**
   * Test for {@link StringUtility#containsNewLines(String)}
   */
  @Test
  public void testNewLines() {
    String text = "lorem " + '\n' + "ipsum";
    assertTrue(StringUtility.containsNewLines(text));
    text = "lorem" + System.getProperty("line.separator") + "ipsum";
    assertTrue(StringUtility.containsNewLines(text));
    text = "";
    assertFalse(StringUtility.containsNewLines(text));
    text = null;
    assertFalse(StringUtility.containsNewLines(text));
  }

  /**
   * Test for {@link StringUtility#parseBoolean(String)}
   *
   * @since 3.10.0-M4
   */
  @Test
  public void testParseBoolean() {
    assertEquals(false, StringUtility.parseBoolean("false"));
    assertEquals(false, StringUtility.parseBoolean("False"));
    assertEquals(false, StringUtility.parseBoolean("FALSE"));
    assertEquals(false, StringUtility.parseBoolean("no"));
    assertEquals(false, StringUtility.parseBoolean("0"));
    assertEquals(false, StringUtility.parseBoolean("x", false));
    assertEquals(false, StringUtility.parseBoolean("", false));
    assertEquals(false, StringUtility.parseBoolean(null));
    assertEquals(false, StringUtility.parseBoolean(null, false));
    assertEquals(true, StringUtility.parseBoolean("true"));
    assertEquals(true, StringUtility.parseBoolean("True"));
    assertEquals(true, StringUtility.parseBoolean("TRUE"));
    assertEquals(true, StringUtility.parseBoolean("yes"));
    assertEquals(true, StringUtility.parseBoolean("yes", true));
    assertEquals(true, StringUtility.parseBoolean("x", true));
    assertEquals(true, StringUtility.parseBoolean("", true));
    assertEquals(true, StringUtility.parseBoolean(null, true));
    assertEquals(false, StringUtility.parseBoolean("lse"));
    assertEquals(false, StringUtility.parseBoolean(","));
    assertEquals(true, StringUtility.parseBoolean(",", true));
    assertEquals(true, StringUtility.parseBoolean("alse", true));
    assertEquals(true, StringUtility.parseBoolean("rue", true));
    assertEquals(false, StringUtility.parseBoolean("false", true));
    assertEquals(true, StringUtility.parseBoolean("true", false));
  }

  @Test
  public void testRepeat() {
    assertEquals("AAA", StringUtility.repeat("A", 3));
    assertEquals("aBcDaBcDaBcDaBcDaBcD", StringUtility.repeat("aBcD", 5));

    assertEquals("", StringUtility.repeat("aBcD", 0));
    assertEquals("", StringUtility.repeat("aBcD", -42));

    assertEquals("", StringUtility.repeat("", 399));
  }

  @Test
  public void testNvl() {
    assertEquals("value", StringUtility.nvl("value", "subsitute"));
    assertEquals("subsitute", StringUtility.nvl(null, "subsitute"));
    assertEquals("", StringUtility.nvl("", "subsitute"));
    assertEquals("5", StringUtility.nvl(Integer.valueOf(5), "subsitute"));
  }

  @Test
  public void testHasText() {
    assertTrue(StringUtility.hasText("scout"));
    assertTrue(StringUtility.hasText("                                      &"));
    assertTrue(StringUtility.hasText("\t\nU\t\n"));
    assertFalse(StringUtility.hasText(null));
    assertFalse(StringUtility.hasText(""));
    assertFalse(StringUtility.hasText("                                   "));
    assertFalse(StringUtility.hasText("\t\n  \t\n"));
  }

  @Test
  public void testSubstituteWhenEmpty() {
    assertEquals("value", StringUtility.substituteWhenEmpty("value", "subsitute"));
    assertEquals("subsitute", StringUtility.substituteWhenEmpty(null, "subsitute"));
    assertEquals("subsitute", StringUtility.substituteWhenEmpty("", "subsitute"));
    assertEquals("subsitute", StringUtility.substituteWhenEmpty("\t\n  \t\n", "subsitute"));
    assertEquals("5", StringUtility.substituteWhenEmpty(Integer.valueOf(5), "subsitute"));
  }

  @Test
  public void testIsWithinNumberFormatLimits() {
    for (Locale locale : Locale.getAvailableLocales()) {
      DecimalFormat format = (DecimalFormat) DecimalFormat.getNumberInstance(locale);
      format.setMaximumIntegerDigits(3);
      format.setMaximumFractionDigits(2);
      char decimalSeparator = format.getDecimalFormatSymbols().getDecimalSeparator();

      assertFalse(StringUtility.isWithinNumberFormatLimits(format, "123", 2, 0, "45"));
      assertTrue(StringUtility.isWithinNumberFormatLimits(format, "1", 1, 0, "23"));

      assertFalse(StringUtility.isWithinNumberFormatLimits(format, "123", 2, 0, decimalSeparator + "456"));
      assertTrue(StringUtility.isWithinNumberFormatLimits(format, "1", 1, 0, decimalSeparator + "23"));

      assertFalse(StringUtility.isWithinNumberFormatLimits(format, "123", 1, 2, "567"));
      assertTrue(StringUtility.isWithinNumberFormatLimits(format, "123", 1, 2, "56"));

      assertFalse(StringUtility.isWithinNumberFormatLimits(format, "123", 1, 2, "567" + decimalSeparator + "7"));
      assertTrue(StringUtility.isWithinNumberFormatLimits(format, "123", 1, 2, "56" + decimalSeparator + "78"));
    }
  }
}
