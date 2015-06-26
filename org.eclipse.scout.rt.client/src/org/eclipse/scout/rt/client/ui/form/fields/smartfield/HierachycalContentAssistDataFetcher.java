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
package org.eclipse.scout.rt.client.ui.form.fields.smartfield;



/**
 *
 */
public class HierachycalContentAssistDataFetcher<LOOKUP_KEY> extends AbstractContentAssistFieldLookupRowFetcher<LOOKUP_KEY> {

  /**
   * @param contentAssistField
   */
  public HierachycalContentAssistDataFetcher(IContentAssistField<?, LOOKUP_KEY> contentAssistField) {
    super(contentAssistField);
  }

  @Override
  public void update(String searchText, boolean selectCurrentValue, boolean synchronous) {
    // in case of hierarchical simply delegate the searchText
    setResult(new ContentAssistFieldDataFetchResult<LOOKUP_KEY>(null, null, searchText, selectCurrentValue));
  }

}
