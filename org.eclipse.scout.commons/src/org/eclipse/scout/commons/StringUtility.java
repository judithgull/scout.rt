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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.nls.NlsUtility;

public final class StringUtility {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(StringUtility.class);
  public static final Pattern PATTERN_TRIM_NEWLINES = Pattern.compile("^[\r\n]*(.*?)[\r\n]*$", Pattern.DOTALL);

  private static final String[] EMPTY_ARRAY = new String[0];

  public interface ITagProcessor {
    String/* tagReplacement */processTag(String tagName, String tagContent);
  }

  private StringUtility() {
  }

  /**
   * Checks whether a given string is {@code null} or empty. A string is considered empty if it
   * equals the empty string "" (internally this method checks whether the string length is 0).
   *
   * @param s
   *          the string to be checked
   * @return {@code true} if {@code s} is {@code null} or equals the empty string, {@code false} otherwise
   */
  public static boolean isNullOrEmpty(CharSequence s) {
    return s == null || s.length() == 0;
  }

  /**
   * Checks whether the given {@link CharSequence} contains visible characters. <br>
   * More formally:
   * Checks whether the given {@link CharSequence} contains at least one character bigger than the whitespace character:
   * '\u0020'.
   *
   * @param s
   *          The {@link CharSequence} to check.
   * @return <code>true</code> if the provided {@link CharSequence} contains visible characters. <code>false</code>
   *         otherwise.
   */
  public static boolean hasText(CharSequence s) {
    if (s == null) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) > '\u0020') {
        return true;
      }
    }
    return false;
  }

  /**
   * Wildcard Pattern may contain only: wildcards: *,%,?,_ characters:<br>
   * A-Z,a-z,0-9 % and * are replaced by .* ? and _ are replaced by . all
   * invalid characters are also replaced by .
   */
  public static String toRegExPattern(String wildcardPattern) {
    if (wildcardPattern == null) {
      wildcardPattern = "";
    }
    StringBuilder buf = new StringBuilder();
    char[] ch = wildcardPattern.toCharArray();
    for (int i = 0; i < ch.length; i++) {
      switch (ch[i]) {
        case ' ': {
          buf.append(ch[i]);
          break;
        }
        case '*':
        case '%': {
          buf.append(".*");
          break;
        }
        case '_':
        case '?': {
          buf.append(".");
          break;
        }
        case '$':
        case '@': {
          buf.append(".");
          break;
        }
        case '<':
        case '>':
        case '=': {
          buf.append(ch[i]);
          break;
        }
        case '.': {
          buf.append("\\.");
          break;
        }
        default: {
          if (ch[i] >= 32 && (Character.isJavaIdentifierStart(ch[i]) || Character.isJavaIdentifierPart(ch[i]))) {
            buf.append(ch[i]);
          }
          else {
            buf.append('.');
          }
        }
      }
    }
    if (buf.length() > 0) {
      try {
        return Pattern.compile(buf.toString()).pattern();
      }
      catch (PatternSyntaxException ex) {
        return "INVALID_PATTERN";
      }
    }
    else {
      return ".*";
    }
  }

  /**
   * @param humanReadableFilterText
   *          is not a regex and may contain *,%,? as wildcards for searching
   * @param patternFlags
   *          see {@link Pattern}
   * @return a {@link Pattern} based on the input pattern. If the input pattern contains no '*' an '*' is automatically
   *         appended. If the input pattern is null or empty then '*' is used instead
   * @since 3.8
   */
  public static Pattern toRegEx(String humanReadableFilterText, int patternFlags) {
    if (humanReadableFilterText == null) {
      humanReadableFilterText = "";
    }
    if (humanReadableFilterText.indexOf('*') < 0) {
      humanReadableFilterText += "*";
    }
    return Pattern.compile(toRegExPattern(humanReadableFilterText), patternFlags);
  }

  /**
   * Tokenize a String s by given character c. Take care about empty String
   * between two separating chars. {@link java.util.StringTokenizer} does not
   * care about empty string between separating chars.
   *
   * @param s
   *          String to tokenize.
   * @param c
   *          Separating character.
   * @return given String s tokenized by c.
   */
  public static String[] tokenize(String s, char c) {
    if (s == null) {
      return new String[0];
    }
    char[] cA = s.toCharArray();
    int count = 0;
    for (int i = 0; i < cA.length; i++) {
      if (cA[i] == c) {
        count++;
      }
    }
    String[] returnValue = new String[count + 1];
    int nextCindex = s.indexOf(c);
    /*
     * LOOP INV: nextCindex points to next index of c in s AND s is String to be
     * tokenized AND returnValue contains already tokenized Strings
     */
    count = 0;
    while (nextCindex >= 0) {
      returnValue[count++] = s.substring(0, nextCindex);
      s = s.substring(nextCindex + 1);
      nextCindex = s.indexOf(c);
    }
    returnValue[count++] = s;
    return returnValue;
  }

  private static final Set<String> BOOLEAN_TRUE = new HashSet<String>(3);
  static {
    BOOLEAN_TRUE.add("true");
    BOOLEAN_TRUE.add("yes");
    BOOLEAN_TRUE.add("1");
  }
  private static final Set<String> BOOLEAN_FLASE = new HashSet<String>(3);
  static {
    BOOLEAN_FLASE.add("false");
    BOOLEAN_FLASE.add("no");
    BOOLEAN_FLASE.add("0");
  }

  /**
   * This method parses a string and returns the associated boolean value.
   * If the string does not represent a valid boolean value, the defaultValue will be returned.
   * If no defaultValue is given, Boolean.False will be returned.
   * The Strings "true", "1", "yes" (case insensitive) are considered true whereas the strings "false", "0", "no" are
   * considered false.
   * <p>
   * Examples:
   * <ul>
   * <li>parseBoolean("true") -> Boolean.True
   * <li>parseBoolean("1") -> Boolean.True
   * <li>parseBoolean("yes") -> Boolean.True
   * <li>parseBoolean("False") -> Boolean.False
   * <li>parseBoolean("0") -> Boolean.False
   * <li>parseBoolean("test") -> Boolean.False
   * <li>parseBoolean("test", true) -> Boolean.True
   * </ul>
   */
  public static boolean parseBoolean(String s, boolean defaultValue) {
    if (s == null || s.length() == 0) {
      return defaultValue;
    }
    s = s.toLowerCase().trim();
    if (defaultValue) {
      return !BOOLEAN_FLASE.contains(s);
    }
    else {
      return BOOLEAN_TRUE.contains(s);
    }
  }

  /**
   * This method parses a string and returns the associated boolean value.
   *
   * @see #parseBoolean(String, boolean)
   */
  public static boolean parseBoolean(String s) {
    return parseBoolean(s, false);
  }

  /**
   * @return the number of lines in the string s empty and null strings have 0
   *         lines
   * @since Build 153
   */
  public static int getLineCount(String s) {
    if (s == null || s.length() == 0) {
      return 0;
    }
    int r = 1;
    int pos = 0;
    while ((pos = s.indexOf('\n', pos)) >= 0) {
      r++;
      // next
      pos++;
    }
    return r;
  }

  /**
   * @return the lines in the string s empty and null strings have 0 lines
   * @since Build 153
   */
  public static String[] getLines(String s) {
    int count = getLineCount(s);
    String[] lines = new String[count];
    if (count == 0) {
      return lines;
    }
    int index = 0;
    int begin = 0;
    int pos = 0;
    while ((pos = s.indexOf('\n', pos)) >= 0) {
      String unit = s.substring(begin, pos);
      int ulen = unit.length();
      if (ulen > 0 && unit.charAt(0) == '\r') {
        unit = unit.substring(1);
      }
      ulen = unit.length();
      if (ulen > 0 && unit.charAt(ulen - 1) == '\r') {
        unit = unit.substring(0, ulen - 1);
      }
      ulen = unit.length();
      lines[index] = unit;
      // next
      pos++;
      index++;
      begin = pos;
    }
    lines[index] = s.substring(begin);
    return lines;
  }

  /**
   * encode a string by escaping " -> "" and ' -> ''
   */
  public static String stringEsc(String s) {
    if (s == null) {
      return null;
    }
    s = s.replaceAll("[\"]", "\"\"");
    s = s.replaceAll("[']", "''");
    return s;
  }

  /**
   * decode a string by unescaping "" -> " and '' -> '
   */
  public static String stringUnesc(String s) {
    s = s.replaceAll("[\"][\"]", "\"");
    s = s.replaceAll("['][']", "'");
    return s;
  }

  public static String removeMnemonic(String text) {
    if (text == null) {
      return null;
    }
    Matcher m = MNEMONIC_PATTERN.matcher(text);
    return m.replaceAll("$1");
  }

  /**
   * Returns a new string resulting from replacing all new line characters ("\n", "\r\n", "\n\r" or "\r") with a single
   * blank (" ").
   * <p>
   * Examples:
   *
   * <pre>
   * "a\r\nb" -> "a b"
   * "a\nb" -> "a b"
   * </pre>
   *
   * </p>
   *
   * @param text
   *          the {@link String} thats new line characters should be removed
   * @return a string derived from this string by replacing every occurrence of new line character with a
   *         blank.
   */
  public static String removeNewLines(String text) {
    return replaceNewLines(text, " ");
  }

  /**
   * Returns a new string resulting from replacing all new line characters ("\n", "\r\n", "\n\r" or "\r") with the given
   * replacement string.
   *
   * @param text
   *          the {@link String} thats new line characters should be removed
   * @param replacement
   *          the {@link String} to be used as replacement for the new line characters
   * @return a string derived from this string by replacing every occurrence of new line character with the replacement
   *         string.
   */
  public static String replaceNewLines(String text, String replacement) {
    if (isNullOrEmpty(text)) {
      return text;
    }
    String s = text.replaceAll("\r\n|\n\r", replacement);
    s = s.replace("\n", replacement).replace("\r", replacement);
    return s;
  }

  /**
   * @return a single tag <foo/>
   */
  private static TagBounds getSingleTag(String text, String tagName, int pos) {
    if (text == null) {
      return TAG_BOUNDS_NOT_FOUND;
    }
    Pattern pat = Pattern.compile("<" + tagName + "(\\s[^<>]*)?/>", Pattern.DOTALL);
    Matcher m = pat.matcher(text);
    if (m.find(pos)) {
      return new TagBounds(m.start(), m.end());
    }
    return TAG_BOUNDS_NOT_FOUND;
  }

  /**
   * @return a start tag (ignores single tags) <foo> (not <foo/>)
   */
  private static TagBounds getStartTag(String text, String tagName, int pos) {
    if (text == null) {
      return TAG_BOUNDS_NOT_FOUND;
    }
    Pattern pat = Pattern.compile("<" + tagName + "(\\s[^<>]*[^/])?>", Pattern.DOTALL);
    Matcher m = pat.matcher(text);
    if (m.find(pos)) {
      return new TagBounds(m.start(), m.end());
    }
    return TAG_BOUNDS_NOT_FOUND;
  }

  /**
   * @return an end tag </foo>
   */
  private static TagBounds getEndTag(String text, String tagName, int pos) {
    if (text == null) {
      return TAG_BOUNDS_NOT_FOUND;
    }
    Pattern pat = Pattern.compile("</" + tagName + ">");
    Matcher m = pat.matcher(text);
    if (m.find(pos)) {
      return new TagBounds(m.start(), m.end());
    }
    return TAG_BOUNDS_NOT_FOUND;
  }

  /**
   * @return the contents between a start and a end tag, resp "" when there is a single tag. Returns <code>null</code>
   *         when the tag is not found in the text.
   */
  public static String getTag(String text, String tagName) {
    if (text == null) {
      return null;
    }
    TagBounds a;
    TagBounds b;
    if ((a = getStartTag(text, tagName, 0)).begin >= 0 && (b = getEndTag(text, tagName, a.end)).begin >= 0) {
      return text.substring(a.end, b.begin).trim();
    }
    if ((a = getSingleTag(text, tagName, 0)).begin >= 0) {
      return "";
    }
    return null;
  }

  public static String replaceTags(String text, String tagName, final String replacement) {
    return replaceTags(text, tagName, new ITagProcessor() {
      @Override
      public String processTag(String name, String tagContent) {
        return replacement;
      }
    });
  }

  /**
   * tag processor returns the replacement for every tag
   * <p>
   * the tag content is either "" for single tags or the tag content else
   * <p>
   * be careful to not replace the tag again with the tag, this will result in an endless loop
   */
  public static String replaceTags(String text, String tagName, ITagProcessor processor) {
    if (text == null) {
      return null;
    }
    TagBounds a;
    TagBounds b;
    while ((a = getSingleTag(text, tagName, 0)).begin >= 0) {
      String tagContent = "";
      String replacement = processor.processTag(tagName, tagContent);
      text = text.substring(0, a.begin) + replacement + text.substring(a.end);
    }
    while ((a = getStartTag(text, tagName, 0)).begin >= 0 && (b = getEndTag(text, tagName, a.end)).begin >= 0) {
      String tagContent = text.substring(a.end, b.begin);
      String replacement = processor.processTag(tagName, tagContent);
      text = text.substring(0, a.begin) + replacement + text.substring(b.end);
    }
    return text;
  }

  /**
   * Removes the tags from the given text. If the tag is not found, the original text is returned.
   * <p>
   * Example:
   * <p>
   * <code>String text = "&lt;html>some &lt;b&gt;bold&lt;/b> text&lt;/html&gt;";</code>
   * <p>
   * <code>removeTag(text, "b")</code> will return '&lt;html>some text&lt;/html&gt;'</code>
   */
  public static String removeTag(String text, String tagName) {
    if (text == null) {
      return null;
    }
    else if (tagName == null) {
      return text;
    }
    TagBounds a;
    TagBounds b;
    while ((a = getSingleTag(text, tagName, 0)).begin >= 0) {
      text = text.substring(0, a.begin) + text.substring(a.end);
    }
    while ((a = getStartTag(text, tagName, 0)).begin >= 0 && (b = getEndTag(text, tagName, a.end)).begin >= 0) {
      text = text.substring(0, a.begin) + text.substring(b.end);
    }
    return text;
  }

  /**
   * Remove the given tags from the text. See {@link #removeTag(String, String)} for more details.
   */
  public static String removeTags(String text, String[] tagNames) {
    if (text == null) {
      return null;
    }
    for (int i = 0; i < tagNames.length; i++) {
      text = removeTag(text, tagNames[i]);
    }
    return text;
  }

  public static String removeTags(String text) {
    if (text == null) {
      return null;
    }
    text = Pattern.compile("<[^>]+>", Pattern.DOTALL).matcher(text).replaceAll("");
    return text;
  }

  public static String removeTagBounds(String text, String tagName) {
    if (text == null) {
      return null;
    }
    TagBounds a;
    TagBounds b;
    while ((a = getSingleTag(text, tagName, 0)).begin >= 0) {
      text = text.substring(0, a.begin) + text.substring(a.end);
    }
    while ((a = getStartTag(text, tagName, 0)).begin >= 0 && (b = getEndTag(text, tagName, a.end)).begin >= 0) {
      text = text.substring(0, a.begin) + text.substring(a.end, b.begin) + text.substring(b.end);
    }
    return text;
  }

  public static String replaceTagBounds(String text, String tagName, String start, String end) {
    if (text == null) {
      return null;
    }
    TagBounds a;
    int b;
    int startPos = 0;
    while (startPos < text.length() && (a = getStartTag(text, tagName, startPos)).begin >= 0 && (b = text.indexOf("</" + tagName + ">", a.end)) > 0) {
      text =
          text.substring(0, a.begin) +
              start +
              text.substring(a.end, b) +
              end +
              text.substring(b + tagName.length() + 3);
      //next
      startPos = a.begin + start.length();
    }
    return text;
  }

  public static final Pattern MNEMONIC_PATTERN = Pattern.compile("&([\\S])");

  public static char getMnemonic(String text) {
    if (text == null) {
      return 0x0;
    }
    Matcher m = MNEMONIC_PATTERN.matcher(text);
    if (m.find()) {
      return m.group(1).charAt(0);
    }
    return 0x0;
  }

  public static String wrapText(String s, int lineSize) {
    if (s == null) {
      return null;
    }
    StringBuilder buf = new StringBuilder();
    char[] ch = s.toCharArray();
    int col = 0;
    for (int i = 0; i < ch.length; i++) {
      if (ch[i] == '\n' || ch[i] == '\r') {
        col = 0;
        buf.append(ch[i]);
      }
      else {
        col++;
        if (col > lineSize) {
          buf.append('\n');
          col = 1;
        }
        buf.append(ch[i]);
      }
    }
    return buf.toString();
  }

  public static String wrapWord(String s, int lineSize) {
    if (s == null) {
      return null;
    }
    StringBuilder buf = new StringBuilder();
    for (String line : s.split("[\\n\\r]")) {
      if (buf.length() > 0) {
        buf.append("\n");
      }
      StringBuilder wrappedLine = new StringBuilder();
      for (String word : line.split("[ \\t]")) {
        if (wrappedLine.length() > 0 && wrappedLine.length() + 1 + word.length() > lineSize) {
          buf.append(wrappedLine.toString());
          buf.append("\n");
          wrappedLine.setLength(0);
        }
        if (wrappedLine.length() > 0) {
          wrappedLine.append(" ");
        }
        wrappedLine.append(word);
      }
      if (wrappedLine.length() > 0) {
        buf.append(wrapText(wrappedLine.toString(), lineSize));
      }
    }
    return buf.toString().trim();
  }

  public static String unwrapText(String s) {
    if (s == null || s.length() == 0) {
      return null;
    }
    s = s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    s = s.replaceAll("[ ]+", " ");
    return s.trim();
  }

  public static boolean isQuotedText(CharSequence s) {
    if (s == null || s.length() == 0) {
      return false;
    }
    if (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
      return true;
    }
    else if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
      return true;
    }
    return false;
  }

  public static String unquoteText(String s) {
    if (s == null || s.length() == 0) {
      return null;
    }
    if (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
      s = s.substring(1, s.length() - 1);
    }
    else if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
      s = s.substring(1, s.length() - 1);
    }
    return s;
  }

  public static String valueOf(Object o) {
    if (o == null) {
      return "";
    }
    if (o instanceof byte[]) {
      return new String((byte[]) o);
    }
    if (o instanceof char[]) {
      return new String((char[]) o);
    }
    return o.toString();
  }

  public static String className(Object o) {
    if (o == null) {
      return "";
    }
    String s = o.getClass().getName();
    int i = s.lastIndexOf('.');
    if (i >= 0) {
      return s.substring(i + 1);
    }
    else {
      return s;
    }
  }

  /*
   * Converts unicodes to encoded &#92;uxxxx and writes out any of the
   * characters in specialSaveChars with a preceding slash
   */
  private static final String CONVERT_UTF_ASCII_HEX_CHARS = "0123456789abcdef";

  public static String convertUTFAscii(String s, boolean escapeControlChars) {
    if (s == null || s.length() == 0) {
      return s;
    }
    int len = s.length();
    StringBuilder buf = new StringBuilder(len * 2);
    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);
      switch (ch) {
        case '\\':
          buf.append("\\\\");
          break;
        case '\t':
          if (escapeControlChars) {
            buf.append("\\t");
          }
          else {
            buf.append(ch);
          }
          break;
        case '\n':
          if (escapeControlChars) {
            buf.append("\\n");
          }
          else {
            buf.append(ch);
          }
          break;
        case '\r':
          if (escapeControlChars) {
            buf.append("\\r");
          }
          else {
            buf.append(ch);
          }
          break;
        case '\f':
          if (escapeControlChars) {
            buf.append("\\f");
          }
          else {
            buf.append(ch);
          }
          break;
        default:
          if ((ch < 0x0020) || (ch > 0x007e)) {
            buf.append('\\');
            buf.append('u');
            buf.append(CONVERT_UTF_ASCII_HEX_CHARS.charAt((ch >> 12) & 0xF));
            buf.append(CONVERT_UTF_ASCII_HEX_CHARS.charAt((ch >> 8) & 0xF));
            buf.append(CONVERT_UTF_ASCII_HEX_CHARS.charAt((ch >> 4) & 0xF));
            buf.append(CONVERT_UTF_ASCII_HEX_CHARS.charAt(ch & 0xF));
          }
          else {
            buf.append(ch);
          }
      }// end switch
    }// end for
    return buf.toString();
  }

  /*
   * Converts encoded &#92;uxxxx to unicode chars and changes special saved
   * chars to their original forms
   */
  public static String convertAsciiUTF(String s) {
    if (s == null || s.length() == 0) {
      return s;
    }
    char ch;
    int len = s.length();
    StringBuilder buf = new StringBuilder(len);

    for (int k = 0; k < len;) {
      ch = s.charAt(k++);
      if (ch == '\\') {
        ch = s.charAt(k++);
        if (ch == 'u') {
          // Read the xxxx
          int value = Integer.parseInt(s.substring(k, k + 4), 16);
          k = k + 4;
          buf.append((char) value);
        }// end if u
        else {
          switch (ch) {
            case '\\':
              buf.append('\\');
              break;
            case 't':
              buf.append('\t');
              break;
            case 'r':
              buf.append('\r');
              break;
            case 'n':
              buf.append('\n');
              break;
            case 'f':
              buf.append('\f');
              break;
            default:
              buf.append(ch);
          }
        }// end else
      }// end if escape
      else {
        buf.append(ch);
      }// end if else
    }// end for k
    return buf.toString();
  }

  public static String emptyIfNull(Object o) {
    return o == null ? "" : o.toString();
  }

  public static String chr(int code) {
    return "" + ((char) Math.min(Math.max(code, 0), 255));
  }

  public static int asc(String s) {
    if (s == null || s.length() == 0) {
      return 0;
    }
    else {
      return s.charAt(0);
    }
  }

  public static byte[] hexToBytes(String s) {
    if (s == null) {
      return new byte[0];
    }
    int slen = s.length();
    byte[] a = new byte[slen / 2];
    for (int i = 0; i < slen; i = i + 2) {
      a[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
    }
    return a;
  }

  public static String bytesToHex(byte[] a) {
    if (a == null || a.length == 0) {
      return null;
    }
    StringBuilder buf = new StringBuilder(a.length * 2);
    int hi, lo;
    for (int i = 0; i < a.length; i++) {
      lo = (a[i]) & 0xff;
      hi = (lo >> 4);
      lo = lo & 0x0f;
      buf.append(Integer.toHexString(hi));
      buf.append(Integer.toHexString(lo));
    }
    return buf.toString();
  }

  /**
   * remove all \n, \r, \t and make all multiple spaces single space
   */
  public static String cleanup(String s) {
    return unwrapText(s);
  }

  /**
   * decision function: return DECODE(value,test1,result1,test2,result2,....,defaultResult)
   * <p>
   * decode('B','A',1,'B',2,'C',3,-1) --> 2
   */
  public static Object decode(Object... a) {
    Object ref = a[0];
    for (int i = 1, n = a.length; i < n; i = i + 2) {
      if (i + 1 < n) {// test-value and result-value
        Object test = a[i];
        if (ref == test || (ref != null && ref.equals(test))) {
          return a[i + 1];
        }
      }
      else {// default result value
        return a[i];
      }
    }
    return null;
  }

  /**
   * @return encoded text, ready to be included in a html text
   *         <xmp>replaces &, ", ', <, > and all whitespace</xmp>
   */
  public static String htmlEncode(String s) {
    return htmlEncode(s, false);
  }

  public static String htmlEncode(String s, boolean replaceSpace) {
    if (s == null) {
      return s;
    }
    if (s.length() == 0) {
      return s;
    }
    s = s.replace("&", "&amp;");
    s = s.replace("\"", "&quot;");
    s = s.replace("'", "&#39;");
    s = s.replace("<", "&lt;");
    s = s.replace(">", "&gt;");
    s = s.replace("\n\r", "<br/>");
    s = s.replace("\n", "<br/>");

    // temporarily replace tabs with specific tab-identifier to not be replaced by subsequent whitespace pattern
    String tabIdentifier = "#TAB#";
    s = s.replace("\t", tabIdentifier);

    if (replaceSpace) {
      s = s.replaceAll("\\s", "&nbsp;");
    }
    s = s.replace(tabIdentifier, "<span style=\"white-space:pre\">&#9;</span>");
    return s;
  }

  /**
   * @return decoded text, ready to be printed as text
   *         <xmp>replaces &, ", ', <, > and all whitespace</xmp>
   */
  public static String htmlDecode(String s) {
    if (s == null || s.length() == 0) {
      return s;
    }

    s = s.replace("&nbsp;", " ");
    s = s.replace("&quot;", "\"");
    s = s.replace("&apos;", "'");
    s = s.replace("&#39;", "'");
    s = s.replace("&lt;", "<");
    s = s.replace("&gt;", ">");
    s = s.replace("&amp;", "&");

    // whitespace patterns
    String zeroOrMoreWhitespaces = "\\s*?";
    String oneOrMoreWhitespaces = "\\s+?";

    // replace <br/> by \n
    s = s.replaceAll("<" + zeroOrMoreWhitespaces + "br" + zeroOrMoreWhitespaces + "/" + zeroOrMoreWhitespaces + ">", "\n");
    // replace HTML-tabs by \t
    s = s.replaceAll("<" + zeroOrMoreWhitespaces + "span" + oneOrMoreWhitespaces + "style" + zeroOrMoreWhitespaces + "=" + zeroOrMoreWhitespaces + "\"white-space:pre\"" + zeroOrMoreWhitespaces + ">&#9;<" + zeroOrMoreWhitespaces + "/" + zeroOrMoreWhitespaces + "span" + zeroOrMoreWhitespaces + ">", "\t");

    return s;
  }

  /**
   * Compares two Strings, ignoring case considerations.
   * Uses the method <code>java.lang.String.equalsIgnoreCase()</code>.
   * Null strings are converted to zero length strings before comparison.
   */
  public static boolean equalsIgnoreCase(String a, String b) {
    if (a == null) {
      a = "";
    }
    if (b == null) {
      b = "";
    }
    return a.equalsIgnoreCase(b);
  }

  /**
   * @deprecated Use {@link #notEqualsIgnoreCase(String, String)} instead. Will be removed in the
   *             N-Release.
   */
  @Deprecated
  public static boolean notEequalsIgnoreCase(String a, String b) {
    return notEqualsIgnoreCase(a, b);
  }

  /**
   * Compares two Strings, ignoring case considerations.
   * Uses the method <code>java.lang.String.equalsIgnoreCase()</code>.
   * Null strings are converted to zero length strings before comparison.
   */
  public static boolean notEqualsIgnoreCase(String a, String b) {
    if (a == null) {
      a = "";
    }
    if (b == null) {
      b = "";
    }
    return !a.equalsIgnoreCase(b);
  }

  public static final Pattern NEWLINE_PATTERN = Pattern.compile("[\\n\\r]+");

  public static boolean equalsIgnoreNewLines(String a, String b) {
    if (a == b) {
      return true;
    }
    if (a == null) {
      return false;
    }
    if (b == null) {
      return false;
    }
    return NEWLINE_PATTERN.matcher(a).replaceAll(" ").equals(NEWLINE_PATTERN.matcher(b).replaceAll(" "));
  }

  /**
   * Returns true if the given String contains a NewLine character, e.g. \n
   *
   * @since 3.10.0-M4
   */
  public static boolean containsNewLines(String s) {
    if (s != null && (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0)) {
      return true;
    }
    return false;
  }

  /**
   * converts all non-allowed characters in the text to the text contained in
   * replacementText
   * <p>
   * <b>Example</b>: <code>filterText("test-text/info.12345","a-zA-Z0-2","_")</code> yields "test_text_info12___" <br>
   * since JRE 1.5 same as: <code>"test-text/info.12345".replaceAll("[^a-zA-Z0-2]","_")</code>
   */
  public static String filterText(String text, String allowedCharacters, String replacementText) {
    if (text == null || allowedCharacters == null) {
      return text;
    }
    if (replacementText == null) {
      replacementText = "";
    }
    return text.replaceAll("[^" + allowedCharacters + "]", replacementText);
  }

  public static int find(String s, String what) {
    return find(s, what, 0);
  }

  public static int find(String s, String what, int start) {
    if (s == null || start >= s.length()) {
      return -1;
    }
    return s.indexOf(what, start);
  }

  /**
   * Format phone numbers (to international phone number format - eg +41 41 882
   * 32 21)
   *
   * @since
   * @param phoneNumber
   *          Unformatted/Formatted phone number with optional country code.
   *          Brackets for area code and special characters (like -) for local
   *          number is supported.
   * @param formattingPattern
   *          Defines the format of the phone number (eg. ## ### ## ## for 41
   *          841 44 44). The formattingPattern must not include the country
   *          code.
   * @param countryCode
   *          Country code (+41, 0041, 41)
   * @return If the phone number does not match the formatting pattern the
   *         original phone number will be returned. Otherwise the formatted
   *         phone number will be returned.
   */
  public static String formatPhone(String phoneNumber, String formattingPattern, String countryCode) {
    if (phoneNumber == null) {
      return null;
    }
    if (formattingPattern == null) {
      return phoneNumber;
    }
    if (countryCode == null) {
      return phoneNumber;
    }

    // declare local variables
    int numberPattern = "#".charAt(0);
    String formattedPhoneNumber = "";
    String normalizedNumber;
    boolean patternIsMatching = false;
    boolean hasCountryCode = false;

    // calc. pattern lenght without spaces, etc.
    int patternLengh = formattingPattern.replaceAll("[^#]", "").length();

    /* normalize phone number */
    normalizedNumber = phoneNumber.replaceAll("[^(0-9|\\+)]|(\\(|\\))", "");

    // check for country code
    hasCountryCode = normalizedNumber.matches("(^(0{2})[0-9]*)|(^\\+{1}[0-9]*)");

    /* normalize country code (no spaces; no leading +/00 */
    countryCode = countryCode.replaceAll("[^0-9]", "");
    countryCode = countryCode.replaceAll("^(0{0,2})", "");

    // phone number has country code
    if (hasCountryCode) {
      // remove country code
      normalizedNumber = normalizedNumber.replaceAll("^(0{0,2})", "");
      normalizedNumber = normalizedNumber.replaceAll("^(\\+)", "");
      normalizedNumber = normalizedNumber.replaceAll("^" + countryCode, "");
    }

    // add leading zeros of the area code. is required for a propper matching
    // with the pattern
    if (patternLengh == normalizedNumber.length() + 1 && !normalizedNumber.startsWith("0")) {
      normalizedNumber = "0" + normalizedNumber;
      patternIsMatching = true;

      // normalized phone number is matching pattern
    }
    else if (patternLengh == normalizedNumber.length()) {
      patternIsMatching = true;
    }

    // format the normalized phone number
    if (patternIsMatching) {
      int count = 0;
      for (int i = 0; i < formattingPattern.length(); i++) {
        if (formattingPattern.charAt(i) == numberPattern) {
          if (count < normalizedNumber.length()) {
            formattedPhoneNumber += normalizedNumber.charAt(count);
            count++;
          }
        }
        else {
          formattedPhoneNumber += formattingPattern.charAt(i);
        }
      }

      // remove zeros of the area code
      formattedPhoneNumber = formattedPhoneNumber.replaceAll("^[0]+", "");
      // add country code
      formattedPhoneNumber = "+" + countryCode + " " + formattedPhoneNumber;

    }
    else {
      // do nothing
      formattedPhoneNumber = phoneNumber;
    }

    return formattedPhoneNumber;
  }

  public static int length(CharSequence s) {
    if (s == null) {
      return 0;
    }
    else {
      return s.length();
    }
  }

  public static String lowercase(String s) {
    if (s == null) {
      return null;
    }
    else {
      return s.toLowerCase();
    }
  }

  public static String uppercase(String s) {
    if (s == null) {
      return null;
    }
    else {
      return s.toUpperCase();
    }
  }

  public static String[] split(String s, String regex) {
    if (s == null || s.length() == 0) {
      return new String[0];
    }
    return s.split(regex);
  }

  /**
   * @param s
   * @return
   * @since 3.8.2
   */
  public static String splitCamelCase(String s) {
    if (!hasText(s)) {
      return null;
    }
    return s.replaceAll(
        String.format("%s|%s|%s",
            "(?<=[A-Z])(?=[A-Z][a-z])",
            "(?<=[^A-Z])(?=[A-Z])",
            "(?<=[A-Za-z])(?=[^A-Za-z])"
            ),
        " "
        );
  }

  public static String substring(String s, int start) {
    if (s == null || start >= s.length()) {
      return "";
    }
    return s.substring(start);
  }

  public static String substring(String s, int start, int len) {
    if (s == null || start >= s.length()) {
      return "";
    }
    len = Math.min(s.length() - start, len);
    return s.substring(start, start + len);
  }

  public static String trim(String s) {
    if (s == null) {
      return null;
    }
    return s.trim();
  }

  /**
   * Returns a copy of the {@link String} with leading and trailing newlines omitted.
   *
   * @param s
   * @return
   */
  public static String trimNewLines(String s) {
    if (s == null) {
      return null;
    }
    Matcher matcher = PATTERN_TRIM_NEWLINES.matcher(s);
    if (matcher.find()) {
      s = matcher.group(1);
    }
    return s;
  }

  public static String lpad(String s, String fill, int len) {
    if (s == null || fill == null || s.length() >= len || fill.length() == 0) {
      return s;
    }
    StringBuilder buf = new StringBuilder(s);
    while (buf.length() < len) {
      buf.insert(0, fill);
    }
    return buf.substring(buf.length() - len, buf.length());
  }

  public static String rpad(String s, String fill, int len) {
    if (s == null || fill == null || s.length() >= len || fill.length() == 0) {
      return s;
    }
    StringBuilder buf = new StringBuilder(s);
    while (buf.length() < len) {
      buf.append(fill);
    }
    return buf.substring(0, len);
  }

  public static String ltrim(String s, Character c) {
    if (s == null) {
      return null;
    }
    if (c == null) {
      return s;
    }
    int len = s.length();
    int st = 0;
    char[] val = s.toCharArray();
    while ((st < len) && (val[st] == c)) {
      st++;
    }
    return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;
  }

  public static String rtrim(String s, Character c) {
    if (s == null) {
      return null;
    }
    if (c == null) {
      return s;
    }
    int len = s.length();
    int st = 0;
    char[] val = s.toCharArray();
    while ((st < len) && (val[len - 1] == c)) {
      len--;
    }
    return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;
  }

  /**
   * Returns the string-representation of <code>value</code>, or <code>valueWhenNull</code> if value is null.
   *
   * @param value
   * @param valueWhenNull
   * @see #substituteWhenEmpty(Object, String)
   */
  public static String nvl(Object value, String valueWhenNull) {
    if (value != null) {
      return value.toString();
    }
    else {
      return valueWhenNull;
    }
  }

  /**
   * replace plain text without using regex
   */
  public static String replace(String s, String sOld, String sNew) {
    sNew = (sNew == null ? "" : sNew);
    if (s == null || sOld == null) {
      return s;
    }
    return s.replace(sOld, sNew);
  }

  /**
   * replace plain text without using regex, ignoring case
   */
  public static String replaceNoCase(String s, String sOld, String sNew) {
    if (s == null || sOld == null || sNew == null) {
      return s;
    }
    StringBuilder buf = new StringBuilder();
    int oldLen = sOld.length();
    int pos = 0;
    sOld = sOld.toLowerCase();
    String sLower = s.toLowerCase();
    int i = sLower.indexOf(sOld);
    while (i >= 0) {
      buf.append(s.substring(pos, i));
      buf.append(sNew);
      pos = i + oldLen;
      i = sLower.indexOf(sOld, pos);
    }
    buf.append(s.substring(pos));
    return buf.toString();
  }

  private static class TagBounds {
    final int begin;
    final int end;

    public TagBounds(int begin, int end) {
      this.begin = begin;
      this.end = end;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof TagBounds) {
        return ((TagBounds) o).begin == begin && ((TagBounds) o).end == end;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return begin ^ end;
    }
  }

  private static final TagBounds TAG_BOUNDS_NOT_FOUND = new TagBounds(-1, -1);

  public static String unescapeWhitespace(String s) {
    if (s == null) {
      return null;
    }
    s = s.replaceAll("\\\\n", "\n");
    s = s.replaceAll("\\\\t", "\t");
    s = s.replaceAll("\\\\r", "\r");
    s = s.replaceAll("\\\\b", "\b");
    s = s.replaceAll("\\\\f", "\f");
    return s;
  }

  public static String escapeWhitespace(String s) {
    if (s == null) {
      return null;
    }
    s = s.replaceAll("\n", "\\\\n");
    s = s.replaceAll("\t", "\\\\t");
    s = s.replaceAll("\r", "\\\\r");
    s = s.replaceAll("\b", "\\\\b");
    s = s.replaceAll("\f", "\\\\f");
    return s;
  }

  /**
   * Concatenates the raw input of {@link Object}s separated by <code>delimiter</code>. On
   * each object {@link Object#toString()} is invoked.<br />
   * <code>null</code> values or those {@link Object#toString()} is empty are neglected.
   *
   * @param delimiter
   * @param values
   * @return never <code>null</code>, empty String in case no elements are appended
   * @since 3.8.1
   */
  public static String join(String delimiter, Object... parts) {
    if (parts == null || parts.length == 0) {
      return "";
    }
    boolean added = false;
    StringBuilder builder = new StringBuilder();
    for (Object o : parts) {
      if (o == null) {
        continue;
      }
      String s = o.toString();
      if (!isNullOrEmpty(s)) {
        if (added && delimiter != null) {
          builder.append(delimiter);
        }
        builder.append(s);
        added = true;
      }
    }
    return builder.toString();
  }

  /**
   * @see #join(String, Object...)
   * @since 4.0.0
   */
  public static String join(String delimiter, Long[] parts) {
    return join(delimiter, (Object[]) parts);
  }

  /**
   * @see #join(String, Object...)
   * @since 3.8.1
   */
  public static String join(String delimiter, String[] parts) {
    return join(delimiter, (Object[]) parts);
  }

  /**
   * @see #join(String, Object...)
   * @since 4.0.0
   */
  public static String join(String delimiter, Collection<?> parts) {
    return join(delimiter, parts == null ? EMPTY_ARRAY : parts.toArray());
  }

  /**
   * Boxes the string with the given prefix and suffix. The result is the empty
   * string, if the string to box has no text. <code>null</code> or empty
   * prefixes and suffixes are neglected.
   * <p>
   * <b>Example</b>: <code>box("(", "foo", ")");</code> returns <code>"(foo)"</code>.
   *
   * @param prefix
   * @param s
   *          the string to box.
   * @param suffix
   * @return Returns the boxed value.
   */
  public static String box(String prefix, String s, String suffix) {
    StringBuilder builder = new StringBuilder();
    if (hasText(s)) {
      if (!isNullOrEmpty(prefix)) {
        builder.append(prefix);
      }
      builder.append(s);
      if (!isNullOrEmpty(suffix)) {
        builder.append(suffix);
      }
    }
    return builder.toString();
  }

  /**
   * removes all suffixes from the string, starting with the last one. Ignores case!
   * <p>
   * <b>Example</b>: <br>
   * <code>removeSuffixes("CompanyFormData","Form","Data")</code> will result in "Company"<br>
   * but <code>removeSuffixes("CompanyFormData","Data","Form")</code> will result in "CompanyForm"
   */
  public static String removeSuffixes(String s, String... suffixes) {
    for (int i = suffixes.length - 1; i >= 0; i--) {
      if (suffixes[i] != null) {
        if (s.toLowerCase().endsWith(suffixes[i].toLowerCase())) {
          s = s.substring(0, s.length() - suffixes[i].length());
        }
      }
    }
    return s;
  }

  public static byte[] compress(String s) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(buffer, deflater); // writes the compressed data into the stream buffer
    StringReader in = null;
    try {
      in = new StringReader(s);

      char[] c = new char[102400];
      int len;
      while ((len = in.read(c)) > 0) {
        String str = new String(c, 0, len);
        byte[] b = str.getBytes("UTF-8");
        deflaterOutputStream.write(b, 0, b.length);
      }
    }
    catch (IOException e) {
      LOG.warn(null, e);
    }
    finally {
      try {
        deflaterOutputStream.flush();
      }
      catch (IOException e) {
      }
      try {
        buffer.flush();
      }
      catch (IOException e) {
      }
      deflater.finish();
      try {
        deflaterOutputStream.finish();
      }
      catch (IOException e) {
      }
      deflater.end();
      try {
        deflaterOutputStream.close();
      }
      catch (IOException e) {
      }
      try {
        buffer.close();
      }
      catch (IOException e) {
      }
      if (in != null) {
        in.close();
      }
    }

    return buffer.toByteArray();
  }

  public static String decompress(byte[] compressed) {
    ByteArrayInputStream in = new ByteArrayInputStream(compressed);
    Inflater inflater = new Inflater();
    InflaterInputStream inflaterInputStream = new InflaterInputStream(in, inflater);
    StringWriter out = new StringWriter();
    try {
      InputStreamReader reader = new InputStreamReader(inflaterInputStream, "UTF-8");
      char[] b = new char[102400];
      int len;
      while ((len = reader.read(b)) > 0) {
        String str = new String(b, 0, len);
        out.write(str, 0, str.length());
      }
    }
    catch (IOException e) {
      LOG.warn(null, e);
    }
    finally {
      try {
        inflaterInputStream.close();
      }
      catch (IOException e) {
      }
      inflater.end();
      out.flush();
      try {
        in.close();
      }
      catch (IOException e) {
      }
      try {
        out.close();
      }
      catch (IOException e) {
      }
    }

    return out.toString();
  }

  /**
   * removes all prefixes from the string, starting with the first one. Ignores case!
   * <p>
   * <b>Example</b>: <br>
   * <code>removePrefixes("CompanyFormData","Company","Form")</code> will result in "Data"<br>
   * but <code>removePrefixes("CompanyFormData","Form","Company")</code> will result in "FormData"
   */
  public static String removePrefixes(String s, String... prefixes) {
    for (int i = 0; i < prefixes.length; i++) {
      if (prefixes[i] != null && s.toLowerCase().startsWith(prefixes[i].toLowerCase())) {
        s = s.substring(prefixes[i].length());
      }
    }
    return s;
  }

  /**
   * Concatenates a set of strings with delimiters.
   * <p>
   * The parameters s0, s2, s4, ... are treated as normal Strings, whereas the parameters s1, s3, s5, ... are treated as
   * delimiters. The delimiters are only inserted, if the corresponding element before or after the delimiter is not
   * empty. <br>
   * If there is an even number of Strings, the last one will only be appended, if the concatenated String so far is not
   * <code>null</code>
   * <p>
   * <b>Example:</b>
   * <ul>
   * <li><code>concatenateTokens('2014','-','01','-','31')</code> --> <code>'2014-01-31'</code>
   * <li><code>concatenateTokens(null,'-','01','-','31')</code> --> <code>'01-31'</code>
   * </ul>
   */
  public static String concatenateTokens(String... s) {
    String retVal = "";
    if (s != null && s.length > 0) {
      StringBuilder b = new StringBuilder();
      String suffix = s[0];
      if (StringUtility.hasText(suffix)) {
        b.append(suffix.trim());
      }
      for (int i = 1, l = s.length - 1; i < l; i = i + 2) {
        String del = s[i];
        suffix = s[i + 1];
        if (StringUtility.hasText(suffix)) {
          if (b.length() > 0) {
            b.append(del);
          }
          b.append(suffix.trim());
        }
      }
      retVal = b.toString().trim();
      if ((s.length % 2) == 0 && retVal.length() > 0) {
        retVal = retVal + s[s.length - 1];
      }
    }
    return retVal;
  }

  /**
   * Delegate to {@link ListUtility.parse(String text)}
   */
  public static Collection<Object> stringToCollection(String text) {
    return CollectionUtility.parse(text);
  }

  /**
   * Delegate to {@link ListUtility.format(Collection c)}
   */
  public static String collectionToString(Collection<Object> c) {
    return collectionToString(c, false);
  }

  /**
   * Delegate to {@link ListUtility.format(Collection c, boolean quoteStrings)}
   */
  public static String collectionToString(Collection<Object> c, boolean quoteStrings) {
    return CollectionUtility.format(c, quoteStrings);
  }

  /**
   * compare two strings using a locale-dependent {@link Collator}
   */
  public static int compareIgnoreCase(String a, String b) {
    return compareIgnoreCase(NlsUtility.getDefaultLocale(), a, b);
  }

  /**
   * compare two strings using a locale-dependent {@link Collator}
   */
  public static int compareIgnoreCase(Locale locale, String a, String b) {
    if (a != null && a.length() == 0) {
      a = null;
    }
    if (b != null && b.length() == 0) {
      b = null;
    }
    //
    if (a == b) {
      return 0;
    }
    if (a == null) {
      return -1;
    }
    if (b == null) {
      return 1;
    }
    Collator collator = Collator.getInstance(locale);
    collator.setStrength(Collator.SECONDARY);
    return collator.compare(a, b);
  }

  public static boolean STRING_INTERN_ENABLED = true;

  /**
   * Delegate for {@link String#intern()}.
   */
  public static String intern(String s) {
    if (STRING_INTERN_ENABLED) {
      return s != null ? s.intern() : null;
    }
    return s;
  }

  /**
   * <p>
   * Attempts to match the entire region against the regex.
   * </p>
   * <p>
   * <small>Thereby, the pattern works case-insensitive and in dot-all mode. See {@link Pattern for more information}
   * </small>
   * </p>
   *
   * @param s
   * @param regex
   * @return
   */
  public static boolean contains(String s, String regex) {
    if (s == null || regex == null) {
      return false;
    }
    try {
      Pattern pattern = Pattern.compile(".*" + regex + ".*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
      return pattern.matcher(s).matches();
    }
    catch (Throwable t) {
      return false;
    }
  }

  /**
   * Creates a new string repeating the <code>token</code> <code>repetitions</code> times. If <code>repetitions</code>
   * <= 0 an empty string is returned.
   *
   * @param token
   * @param repetitions
   */
  public static String repeat(CharSequence token, int repetitions) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < repetitions; ++i) {
      sb.append(token);
    }
    return sb.toString();
  }

  /**
   * Similar to {@link #nvl(Object, String)} but returns <code>valueWhenEmpty</code> not only if value is null, but as
   * well when the String representation of <code>value</code> is empty or contains only whitespaces.
   *
   * @param property
   * @param string
   * @return
   */
  public static String substituteWhenEmpty(Object value, String valueWhenEmpty) {
    String stringValue = nvl(value, null);
    return hasText(stringValue) ? stringValue : valueWhenEmpty;
  }

  /**
   * Checks whether the given string modification still fulfills the given {@link DecimalFormat} max length constraints.
   *
   * @param format
   *          The {@link DecimalFormat} holding the constraints: {@link DecimalFormat#getMaximumIntegerDigits()},
   *          {@link DecimalFormat#getMaximumFractionDigits()}.
   * @param curText
   *          The current text (before the modification).
   * @param offset
   *          The offset of the modification relative to the curText parameter.
   * @param replaceLen
   *          How many characters that will be replaced starting at the given offset.
   * @param insertText
   *          The new text that should be inserted at the given replace range.
   * @return <code>true</code> if the given {@link DecimalFormat} length constraints are still fulfilled after the
   *         string modification has been applied or if the resulting string is no valid number. <code>false</code>
   *         otherwise.
   * @deprecated use {@link AbstractNumberField#isWithinNumberFormatLimits(DecimalFormat, String, int, int, String)}
   *             instead.
   *             Will be removed in the N-Release
   */
  @Deprecated
  public static boolean isWithinNumberFormatLimits(DecimalFormat format, String curText, int offset, int replaceLen, String insertText) {
    // !! IMPORTANT NOTE: There is also a JavaScript implementation of this method: org/eclipse/scout/rt/ui/rap/form/fields/numberfield/RwtScoutNumberField.js
    // When changing this implementation also consider updating the js version!
    if (insertText == null || insertText.length() < 1) {
      return true;
    }

    String futureText = null;
    if (curText == null) {
      futureText = insertText;
    }
    else {
      StringBuilder docTxt = new StringBuilder(curText.length() + insertText.length());
      docTxt.append(curText);
      docTxt.replace(offset, offset + replaceLen, insertText);
      futureText = docTxt.toString();
    }

    Pattern pat = Pattern.compile("[^1-9" + format.getDecimalFormatSymbols().getZeroDigit() + "]");
    String decimalSeparator = String.valueOf(format.getDecimalFormatSymbols().getDecimalSeparator());
    String[] parts = futureText.split(Pattern.quote(decimalSeparator));
    if (parts.length >= 1) {
      String intPartDigits = pat.matcher(parts[0]).replaceAll("");
      boolean intPartValid = StringUtility.length(intPartDigits) <= format.getMaximumIntegerDigits();
      if (!intPartValid) {
        return false;
      }
    }
    if (parts.length == 2) {
      String fracPartDigits = pat.matcher(parts[1]).replaceAll("");
      boolean fracPartValid = StringUtility.length(fracPartDigits) <= format.getMaximumFractionDigits();
      if (!fracPartValid) {
        return false;
      }
    }
    return true;
  }
}
