/*******************************************************************************
 * Copyright (c) 2010-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.extension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.scout.rt.platform.IOrdered;

import java.util.Set;

public abstract class AbstractContainerValidationService implements IExtensionRegistrationValidatorService {

  private final Map<Class<?>, Set<Class<?>>> m_possibleContributionsByContainer;
  private final Map<Class<? extends IOrdered>, Set<Class<? extends IOrdered>>> m_possibleMovesByContainer;

  protected AbstractContainerValidationService() {
    m_possibleContributionsByContainer = new HashMap<Class<?>, Set<Class<?>>>();
    m_possibleMovesByContainer = new HashMap<Class<? extends IOrdered>, Set<Class<? extends IOrdered>>>();
  }

  private static <T> void removeFromMap(Class<? extends T> key, Class<? extends T> value, Map<Class<? extends T>, Set<Class<? extends T>>> map) {
    Set<Class<? extends T>> values = map.get(key);
    if (values != null) {
      values.remove(value);
      if (values.isEmpty()) {
        map.remove(key);
      }
    }
  }

  private static <T> void addToMap(Class<? extends T> key, Class<? extends T> value, Map<Class<? extends T>, Set<Class<? extends T>>> map) {
    Set<Class<? extends T>> values = map.get(key);
    if (values == null) {
      values = new HashSet<Class<? extends T>>();
      map.put(key, values);
    }
    values.add(value);
  }

  private static <T> boolean isValid(Class<? extends T> key, Class<? extends T> value, Map<Class<? extends T>, Set<Class<? extends T>>> map) {
    for (Entry<Class<? extends T>, Set<Class<? extends T>>> entry : map.entrySet()) {
      Class<? extends T> curContainer = entry.getKey();
      if (curContainer.isAssignableFrom(key)) {
        Set<Class<? extends T>> values = entry.getValue();
        for (Class<?> cur : values) {
          if (cur.isAssignableFrom(value)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public void removePossibleContributionForContainer(Class<?> contribution, Class<?> container) {
    removeFromMap(container, contribution, m_possibleContributionsByContainer);
  }

  public void addPossibleContributionForContainer(Class<?> contribution, Class<?> container) {
    addToMap(container, contribution, m_possibleContributionsByContainer);
  }

  public void addPossibleMoveForContainer(Class<? extends IOrdered> model, Class<? extends IOrdered> container) {
    addToMap(model, container, m_possibleMovesByContainer);
  }

  public void removePossibleMoveFForContainer(Class<? extends IOrdered> model, Class<? extends IOrdered> container) {
    removeFromMap(container, model, m_possibleMovesByContainer);
  }

  @Override
  public boolean isValidContribution(Class<?> contribution, Class<?> container) throws IllegalExtensionException {
    return isValid(container, contribution, m_possibleContributionsByContainer);
  }

  @Override
  public boolean isValidMove(Class<? extends IOrdered> modelClass, Class<? extends IOrdered> newContainerClass) throws IllegalExtensionException {
    return isValid(modelClass, newContainerClass, m_possibleMovesByContainer);
  }
}
