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

/**
 * Marker Interface for html table
 * 
 * @since 5.1 (backported)
 */
public interface IHtmlTable extends IHtmlElement {

  /**
   * @deprecated use {@link IHtmlElement#cssClass(CharSequence)} or {@link IHtmlElement#style(CharSequence)}
   */
  @Deprecated
  IHtmlTable cellspacing(int pixel);

  /**
   * @deprecated use {@link IHtmlElement#cssClass(CharSequence)} or {@link IHtmlElement#style(CharSequence)}
   */
  @Deprecated
  IHtmlTable cellpadding(int pixel);

  /**
   * Add a css class
   */
  @Override
  IHtmlTable cssClass(CharSequence cssClass);

  /**
   * Add a css style
   */
  @Override
  IHtmlTable style(CharSequence style);

  /**
   * Add an application local link
   * 
   * @param path
   *          path to identify what is the link referring to.
   */
  @Override
  IHtmlTable appLink(CharSequence path);

  /**
   * {@inheritDoc}
   */
  @Override
  IHtmlTable addAttribute(String name, CharSequence value);
}
