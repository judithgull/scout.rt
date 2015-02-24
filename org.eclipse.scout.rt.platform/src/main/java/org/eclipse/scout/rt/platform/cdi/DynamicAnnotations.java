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
package org.eclipse.scout.rt.platform.cdi;

import java.lang.annotation.Annotation;

import org.eclipse.scout.commons.annotations.Priority;

/**
 *
 */
public final class DynamicAnnotations {

  private DynamicAnnotations() {
  }

  public static Priority createPriority(final float priority) {
    return new Priority() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return Priority.class;
      }

      @Override
      public float value() {
        return priority;
      }
    };
  }

  public static ApplicationScoped createApplicationScoped() {
    return new ApplicationScoped() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return ApplicationScoped.class;
      }
    };
  }

  public static CreateImmediately createCreateImmediately() {
    return new CreateImmediately() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return CreateImmediately.class;
      }
    };
  }
}