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
package org.eclipse.scout.rt.spec.client.config.entity;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.commons.ITypeWithClassId;
import org.eclipse.scout.rt.spec.client.filter.DefaultDocFilter;
import org.eclipse.scout.rt.spec.client.filter.IDocFilter;
import org.eclipse.scout.rt.spec.client.gen.extract.DescriptionExtractor;
import org.eclipse.scout.rt.spec.client.gen.extract.IDocTextExtractor;
import org.eclipse.scout.rt.spec.client.gen.extract.SimpleTypeTextExtractor;
import org.eclipse.scout.rt.spec.client.gen.extract.TypeExtractor;

/**
 *
 */
public class DefaultEntityConfig<T extends ITypeWithClassId> implements IDocEntityConfig<T> {

  private int m_topHeadingLevel = 2;

  /**
   * Default filters: Ignores Types annotated with {@link org.eclipse.scout.commons.annotations.Doc Doc#ignore()}==false
   */
  @Override
  public List<IDocFilter<T>> getFilters() {
    List<IDocFilter<T>> filters = new ArrayList<IDocFilter<T>>();
    filters.add(new DefaultDocFilter<T>());
    return filters;
  }

  @Override
  public List<IDocTextExtractor<T>> getPropertyTextExtractors() {
    ArrayList<IDocTextExtractor<T>> p = new ArrayList<IDocTextExtractor<T>>();
    p.add(new TypeExtractor<T>());
    p.add(new DescriptionExtractor<T>());
    return p;
  }

  @Override
  public IDocTextExtractor<T> getTitleExtractor() {
    return new SimpleTypeTextExtractor<T>();
  }

  @Override
  public int getTopHeadingLevel() {
    return m_topHeadingLevel;
  }

  public void setTopHeadingLevel(int topHeadingLevel) {
    m_topHeadingLevel = topHeadingLevel;
  }

}
