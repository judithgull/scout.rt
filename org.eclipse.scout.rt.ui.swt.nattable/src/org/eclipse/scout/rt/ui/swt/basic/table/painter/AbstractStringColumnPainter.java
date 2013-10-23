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
package org.eclipse.scout.rt.ui.swt.basic.table.painter;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.layer.cell.CellDisplayConversionUtils;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.CellPainterWrapper;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.scout.rt.ui.swt.basic.table.configuration.EsCellStyleAttributes;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;

/**
 *
 */
public abstract class AbstractStringColumnPainter extends CellPainterWrapper implements IDynamicCellSizePainter {

  public static final String EMPTY = ""; //$NON-NLS-1$
  public static final String DOT = "..."; //$NON-NLS-1$

  /**
   * The regular expression to find predefined new lines in the text to show.
   * Is used for word wrapping to preserve user defined new lines.
   * To be platform independent \n and \r and the combination of both are used
   * to find user defined new lines.
   */
  public static final String NEW_LINE_REGEX = "\\n\\r|\\r\\n|\\n|\\r"; //$NON-NLS-1$

  public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

  private static Map<org.eclipse.swt.graphics.Font, FontData[]> fontDataCache = new WeakHashMap<org.eclipse.swt.graphics.Font, FontData[]>();
  private static Map<String, Integer> temporaryMap = new WeakHashMap<String, Integer>();

  private final int spacing = 5;

  public AbstractStringColumnPainter() {
    this(5);
  }

  public AbstractStringColumnPainter(int spacing) {
    this(null, spacing);
  }

  public AbstractStringColumnPainter(ICellPainter wrappedPainter) {
    this(wrappedPainter, 5);
  }

  public AbstractStringColumnPainter(ICellPainter wrappedPainter, int spacing) {
    super(wrappedPainter);
  }

  public int getSpacing() {
    return spacing;
  }

  protected boolean isWrapText(IStyle cellStyle) {
    Boolean wrapText = cellStyle.getAttributeValue(EsCellStyleAttributes.TEXT_WRAP);
    return wrapText != null && wrapText.booleanValue();
  }

  /**
   * Convert the data value of the cell using the {@link IDisplayConverter} from the {@link IConfigRegistry}
   */
  protected String convertDataType(ILayerCell cell, IConfigRegistry configRegistry) {
    return CellDisplayConversionUtils.convertDataType(cell, configRegistry);
  }

  /**
   * Setup the GC by the values defined in the given cell style.
   * 
   * @param gc
   * @param cellStyle
   */
  public void setupGCFromConfig(GC gc, IStyle cellStyle) {
    Color fg = cellStyle.getAttributeValue(CellStyleAttributes.FOREGROUND_COLOR);
    Color bg = cellStyle.getAttributeValue(CellStyleAttributes.BACKGROUND_COLOR);
    Font font = cellStyle.getAttributeValue(CellStyleAttributes.FONT);

    gc.setAntialias(GUIHelper.DEFAULT_ANTIALIAS);
    gc.setTextAntialias(GUIHelper.DEFAULT_TEXT_ANTIALIAS);
    gc.setFont(font);
    gc.setForeground(fg != null ? fg : GUIHelper.COLOR_LIST_FOREGROUND);
    gc.setBackground(bg != null ? bg : GUIHelper.COLOR_LIST_BACKGROUND);
  }

  /**
   * Computes dependent on the configuration of the TextPainter the text to display.
   * If word wrapping is enabled new lines are inserted if the available space is not
   * enough. If calculation of available space is enabled, the space is automatically
   * widened for the text to display, and if no calculation is enabled the text is cut
   * and modified to end with "..." to fit into the available space
   * 
   * @param cell
   *          the current cell to paint
   * @param gc
   *          the current GC
   * @param availableLength
   *          the available space for the text to display
   * @param text
   *          the text that should be modified for display
   * @return the modified text
   */
  protected String getTextToDisplay(ILayerCell cell, GC gc, int availableLength, String text, boolean multiline, boolean wrapText) {
    StringBuilder output = new StringBuilder();

    text = text.trim();

    //take the whole width of the text
    int textLength = getLengthFromCache(gc, text);
    if (wrapText) {
      String[] lines = text.split(NEW_LINE_REGEX);
      for (String line : lines) {
        if (output.length() > 0) {
          output.append(LINE_SEPARATOR);
        }

        String[] words = line.split("\\s"); //$NON-NLS-1$

        //concat the words with spaces and newlines
        String computedText = ""; //$NON-NLS-1$
        for (String word : words) {
          computedText = computeTextToDisplay(computedText, word, gc, availableLength);
        }

        output.append(computedText);
      }

    }
    else {
      output.append(modifyTextToDisplay(text, gc, availableLength + (2 * spacing)));
    }

    return output.toString();
  }

  /**
   * This method gets only called if word wrapping is enabled.
   * Concatenates the two given words by taking the availableSpace into account.
   * If concatenating those two words with a space as delimiter does fit into
   * the available space the return value is exactly this. Else instead of a
   * space there will be a new line character used as delimiter.
   * 
   * @param one
   *          the first word or the whole text before the next word
   * @param two
   *          the next word to add to the first parameter
   * @param gc
   *          the current GC
   * @param availableSpace
   *          the available space
   * @return the concatenated String of the first two parameters
   */
  private String computeTextToDisplay(String one, String two, GC gc, int availableSpace) {
    String result = one;
    //if one is empty or one ends with newline just add two
    if (one == null || one.length() == 0 || one.endsWith(LINE_SEPARATOR)) {
      result += two;
    }
    //if one does not contain a newline
    else if (one.indexOf(LINE_SEPARATOR) == -1) {
      //
      if (getLengthFromCache(gc, one) == availableSpace
          || getLengthFromCache(gc, one + " " + two) >= availableSpace) { //$NON-NLS-1$
        result += LINE_SEPARATOR;
        result += modifyTextToDisplay(two, gc, availableSpace);
      }
      else {
        result += ' ';
        result += two;
      }
    }
    else {
      //get the end of the last part after the last newline
      String endString = one.substring(one.lastIndexOf(LINE_SEPARATOR) + 1);
      if (getLengthFromCache(gc, endString) == availableSpace
          || getLengthFromCache(gc, endString + " " + two) >= availableSpace) { //$NON-NLS-1$
        result += LINE_SEPARATOR;
        result += two;
      }
      else {
        result += ' ';
        result += two;
      }
    }
    return result;
  }

  /**
   * Checks if the given text is bigger than the available space. If not the given
   * text is simply returned without modification. If the text does not fit into
   * the available space, it will be modified by cutting and adding three dots.
   * 
   * @param text
   *          the text to compute
   * @param gc
   *          the current GC
   * @param availableLength
   *          the available space
   * @return the modified text if it is bigger than the available space or the
   *         text as it was given if it fits into the available space
   */
  private String modifyTextToDisplay(String text, GC gc, int availableLength) {
    //length of the text on GC taking new lines into account
    //this means the textLength is the value of the longest line
    int textLength = getLengthFromCache(gc, text);
    if (textLength > availableLength) {
      //as looking at the text length without taking new lines into account
      //we have to look at every line itself
      StringBuilder result = new StringBuilder();
      String[] lines = text.split(NEW_LINE_REGEX);
      for (String line : lines) {
        if (result.length() > 0) {
          result.append(LINE_SEPARATOR);
        }

        //now modify every line if it is longer than the available space
        //this way every line will get ... if it doesn't fit
        int lineLength = getLengthFromCache(gc, line);
        if (lineLength > availableLength) {
          int numExtraChars = 0;

          int newStringLength = line.length();
          String trialLabelText = line + DOT;
          int newTextExtent = getLengthFromCache(gc, trialLabelText);

          while (newTextExtent > availableLength + 1 && newStringLength > 0) {
            double avgWidthPerChar = (double) newTextExtent / trialLabelText.length();
            numExtraChars += 1 + (int) ((newTextExtent - availableLength) / avgWidthPerChar);

            newStringLength = line.length() - numExtraChars;
            if (newStringLength > 0) {
              trialLabelText = line.substring(0, newStringLength) + DOT;
              newTextExtent = getLengthFromCache(gc, trialLabelText);
            }
          }

          if (numExtraChars > line.length()) {
            numExtraChars = line.length();
          }

          // now we have gone too short, lets add chars one at a time to exceed the width...
          String testString = line;
          for (int i = 0; i < line.length(); i++) {
            testString = line.substring(0, line.length() + i - numExtraChars) + DOT;
            textLength = getLengthFromCache(gc, testString);

            if (textLength >= availableLength) {

              //  now roll back one as this was the first number that exceeded
              if (line.length() + i - numExtraChars < 1) {
                line = EMPTY;
              }
              else {
                line = line.substring(0, line.length() + i - numExtraChars - 1) + DOT;
              }
              break;
            }
          }
        }
        result.append(line);
      }

      return result.toString();
    }
    return text;
  }

  protected String[] getLines(String text) {
    String[] lines = text.split(NEW_LINE_REGEX);
    return lines;
  }

  /**
   * Calculates the length of a given text by using the GC.
   * To minimize the count of calculations, the calculation
   * result will be stored within a Map, so the next time
   * the length of the same text is asked for, the result
   * is only returned by cache and is not calculated again.
   * 
   * @param gc
   *          the current GC
   * @param text
   *          the text to get the length for
   * @return the length of the text
   */
  protected int getLengthFromCache(GC gc, String text) {
    String originalString = text;
    StringBuilder buffer = new StringBuilder();
    buffer.append(text);
    if (gc.getFont() != null) {
      FontData[] datas = fontDataCache.get(gc.getFont());
      if (datas == null) {
        datas = gc.getFont().getFontData();
        fontDataCache.put(gc.getFont(), datas);
      }
      if (datas != null && datas.length > 0) {
        buffer.append(datas[0].getName());
        buffer.append(","); //$NON-NLS-1$
        buffer.append(datas[0].getHeight());
        buffer.append(","); //$NON-NLS-1$
        buffer.append(datas[0].getStyle());
      }
    }
    text = buffer.toString();
    Integer width = temporaryMap.get(text);
    if (width == null) {
      width = Integer.valueOf(gc.textExtent(originalString).x);
      temporaryMap.put(text, width);
    }

    return width.intValue();
  }
}
