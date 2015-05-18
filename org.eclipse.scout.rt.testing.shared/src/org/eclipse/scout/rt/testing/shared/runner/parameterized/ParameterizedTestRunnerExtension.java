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
package org.eclipse.scout.rt.testing.shared.runner.parameterized;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/**
 * This class contains the logic which is shared among {@link ParameterizedScoutServerTestRunner} and
 * {@link ParameterizedScoutClientTestRunner}
 */
public class ParameterizedTestRunnerExtension {

  private ParameterizedTestRunnerExtension() {
  }

  public static Object createTest(TestClass testClass, IScoutTestParameter testParameter) throws Exception {
    try {
      return testClass.getOnlyConstructor().newInstance(testParameter);
    }
    catch (Exception ex) {
      throw new Exception("Constructor can not be invoked with the parameters " + testParameter.getName() + ")", ex);
    }
  }

  @SuppressWarnings("unchecked")
  public static List<IScoutTestParameter> loadParameterList(TestClass testClass) {
    try {
      List<FrameworkMethod> parametersMethod = testClass.getAnnotatedMethods(Parameters.class);
      return (List<IScoutTestParameter>) parametersMethod.get(0).invokeExplosively(null);
    }
    catch (Throwable t) {
      throw new IllegalStateException("Parameters cannot be loaded", t);
    }
  }

  public static List<FrameworkMethod> createParameterizedTestMethods(List<FrameworkMethod> originalTestMethods, int numberOfParameterEntries) {
    List<FrameworkMethod> result = new LinkedList<FrameworkMethod>();
    List<FrameworkMethod> testMethods = originalTestMethods;

    for (int paramsIndex = 0; paramsIndex < numberOfParameterEntries; paramsIndex++) {
      for (FrameworkMethod test : testMethods) {
        ParameterizedFrameworkMethod parameterizedTest = new ParameterizedFrameworkMethod(test, paramsIndex);
        result.add(parameterizedTest);
      }
    }

    return result;
  }

  public static void validateOneParametersMethod(TestClass testClass, List<Throwable> errors) {
    List<FrameworkMethod> parametersMethods = testClass.getAnnotatedMethods(Parameters.class);

    if (parametersMethods.size() != 1) {
      errors.add(new Exception("Parameters method is missing. It must return List<" + IScoutTestParameter.class.getSimpleName() + ">"));
    }
    else {
      FrameworkMethod parametersMethod = parametersMethods.get(0);
      Method innerMethod = parametersMethod.getMethod();

      if (!Modifier.isStatic(innerMethod.getModifiers())) {
        errors.add(new Exception("Parameters method must be static"));
      }
      if (!Modifier.isPublic(innerMethod.getDeclaringClass().getModifiers())) {
        errors.add(new Exception("Class " + innerMethod.getDeclaringClass().getName() + " should be public"));
      }
      if (!Modifier.isPublic(innerMethod.getModifiers())) {
        errors.add(new Exception("Parameters method must be public"));
      }
      if (!List.class.isAssignableFrom(innerMethod.getReturnType())) {
        errors.add(new Exception("Parameters method return type must be List<" + IScoutTestParameter.class.getSimpleName() + ">"));
      }
    }
  }

  public static Description describeChild(TestClass testClass, ParameterizedFrameworkMethod parameterizedMethod, String testName, List<IScoutTestParameter> parameterList) {
    return Description.createTestDescription(testClass.getJavaClass(), String.format("%s [%s]", testName, parameterList.get(parameterizedMethod.getParamIndex()).getName()), parameterizedMethod.getAnnotations());
  }
}