/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.annotations.ClassId;
import org.eclipse.scout.commons.annotations.IOrdered;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.Replace;
import org.eclipse.scout.commons.fixture.A;
import org.eclipse.scout.commons.fixture.AbstractC;
import org.junit.Test;

/**
 * JUnit tests for {@link ConfigurationUtility}
 * 
 * @since 3.8.0
 */
public class ConfigurationUtilityTest {
  private static final String TEST_CLASS_ID = "TEST_CLASS_ID";

  @Test
  public void sortByOrder() {
    // null and empty
    assertNull(ConfigurationUtility.sortByOrder(null));
    assertTrue(ConfigurationUtility.sortByOrder(Collections.emptyList()).isEmpty());
    //
    OrderStatic10 static10 = new OrderStatic10();
    OrderStatic20Dynamic0 static20Dynamic0 = new OrderStatic20Dynamic0();
    OrderDynamic30 dynamic30 = new OrderDynamic30();
    //
    List<Object> orderedElements = Arrays.asList(dynamic30, static20Dynamic0, static10);
    Collection<Object> sorted = ConfigurationUtility.sortByOrder(orderedElements);
    assertArrayEquals(new Object[]{static10, static20Dynamic0, dynamic30}, sorted.toArray());
    //
    ReplaceOrderStatic10 replaceOrderStatic10 = new ReplaceOrderStatic10();
    orderedElements = Arrays.<Object> asList(dynamic30, static20Dynamic0, replaceOrderStatic10);
    sorted = ConfigurationUtility.sortByOrder(orderedElements);
    assertArrayEquals(new Object[]{replaceOrderStatic10, static20Dynamic0, dynamic30}, sorted.toArray());
  }

  @Test
  public void getEnclosingContainerType() {
    // null
    assertNull(ConfigurationUtility.getEnclosingContainerType(null));
    // objects
    assertSame(A.class, ConfigurationUtility.getEnclosingContainerType(new A()));
    assertSame(A.class, ConfigurationUtility.getEnclosingContainerType(new A().b));
    assertSame(A.class, ConfigurationUtility.getEnclosingContainerType(new A().b.c1));
    assertSame(AbstractC.class, ConfigurationUtility.getEnclosingContainerType(new A().b.c1.d));
    assertSame(AbstractC.class, ConfigurationUtility.getEnclosingContainerType(new A().b.c1.d.e));
    assertSame(A.class, ConfigurationUtility.getEnclosingContainerType(new A().b.c2));
    assertSame(AbstractC.class, ConfigurationUtility.getEnclosingContainerType(new A().b.c2.d));
    assertSame(AbstractC.class, ConfigurationUtility.getEnclosingContainerType(new A().b.c2.d.e));
    // primitive values
    assertSame(Integer.class, ConfigurationUtility.getEnclosingContainerType(42));
    assertSame(Double.class, ConfigurationUtility.getEnclosingContainerType(42d));
  }

  @Test
  public void getEnclosingContainerTypeAbstractInnerClasses() {
    assertSame(ConfigurationUtilityTest.class, ConfigurationUtility.getEnclosingContainerType(new InnerA()));
    assertSame(ConfigurationUtilityTest.class, ConfigurationUtility.getEnclosingContainerType(new InnerA().b));
    assertSame(ConfigurationUtilityTest.class, ConfigurationUtility.getEnclosingContainerType(new InnerA().b.c1));
    assertSame(AbstractInnerC.class, ConfigurationUtility.getEnclosingContainerType(new InnerA().b.c1.d));
    assertSame(AbstractInnerC.class, ConfigurationUtility.getEnclosingContainerType(new InnerA().b.c1.d.e));
    assertSame(ConfigurationUtilityTest.class, ConfigurationUtility.getEnclosingContainerType(new InnerA().b.c2));
    assertSame(AbstractInnerC.class, ConfigurationUtility.getEnclosingContainerType(new InnerA().b.c2.d));
    assertSame(AbstractInnerC.class, ConfigurationUtility.getEnclosingContainerType(new InnerA().b.c2.d.e));
  }

  @Test(expected = NullPointerException.class)
  public void removeReplacedClassesNull() {
    ConfigurationUtility.removeReplacedClasses(null);
  }

  @Test
  public void removeReplacedClassesEmpty() {
    List<Class<?>> classes = Collections.emptyList();
    List<? extends Class<? extends Object>> actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertTrue(CollectionUtility.equalsCollection(classes, actual));
  }

  @Test
  public void removeReplacedClassesNoReplacements() {
    List<Class<?>> classes = classList(Original.class);
    List<? extends Class<? extends Object>> actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertTrue(CollectionUtility.equalsCollection(classes, actual));
    //
    classes = classList(Original.class, String.class);
    actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertTrue(CollectionUtility.equalsCollection(classes, actual));
    //
    classes = classList(Original.class, String.class, Long.class);
    actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertTrue(CollectionUtility.equalsCollection(classes, actual));
  }

  @Test
  public void removeReplacedClasses() {
    List<Class<?>> classes = classList(Original.class, Replacement.class);
    List<? extends Class<? extends Object>> actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertNotSame(classes, actual);
    assertTrue(CollectionUtility.equalsCollection(classList(Replacement.class), actual));
    //
    classes = classList(Replacement.class, Original.class);
    actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertNotSame(classes, actual);
    assertTrue(CollectionUtility.equalsCollection(classList(Replacement.class), actual));
  }

  @Test
  public void removeReplacedClassesReplacementHierarchy() {
    List<Class<?>> classes = classList(Original.class, Replacement.class, Replacement2.class);
    List<? extends Class<? extends Object>> actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertNotSame(classes, actual);
    assertTrue(CollectionUtility.equalsCollection(classList(Replacement2.class), actual));
    //
    classes = classList(Replacement3.class, Replacement.class, Original.class, Replacement2.class);
    actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertNotSame(classes, actual);
    assertTrue(CollectionUtility.equalsCollection(classList(Replacement3.class), actual));
  }

  @Test
  public void removeReplacedClassesMultyReplacementHierarchy() {
    List<Class<?>> classes = classList(Original.class, Replacement.class, OtherReplacement.class);
    List<? extends Class<? extends Object>> actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertNotSame(classes, actual);
    Set<Class<?>> expectedContents = new HashSet<Class<?>>();
    expectedContents.add(Replacement.class);
    expectedContents.add(OtherReplacement.class);
    assertEquals(2, actual.size());
    assertTrue(expectedContents.contains(actual.get(0)));
    assertTrue(expectedContents.contains(actual.get(1)));
  }

  @Test
  public void removeReplacedClassesPreserveOrder() {
    List<Class<?>> classes = classList(Original.class, String.class, Replacement.class);
    List<? extends Class<? extends Object>> actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertNotSame(classes, actual);
    assertTrue(CollectionUtility.equalsCollection(classList(Replacement.class, String.class), actual));
  }

  @Test
  public void removeReplacedClassesReplacementHierarchyNotCompletelyPartOfOriginalList() {
    List<Class<?>> classes = classList(Original.class, String.class, Replacement3.class);
    List<? extends Class<? extends Object>> actual = ConfigurationUtility.removeReplacedClasses(classes);
    assertNotNull(actual);
    assertNotSame(classes, actual);
    assertTrue(CollectionUtility.equalsCollection(classList(Replacement3.class, String.class), actual));
  }

  @Test
  public void getDeclaredPublicClasses() {
    Class[] result = ConfigurationUtility.getDeclaredPublicClasses(InnerA.class);
    assertSame(result[0], InnerA.InnerB.class);

    result = ConfigurationUtility.getDeclaredPublicClasses(PublicRoot.class);
    assertEquals(result.length, 4);
    assertArrayEquals(new Class[]{PublicRoot.AbstractClass1.class, PublicRoot.AbstractClass2.class, PublicRoot.PublicClass1.class, PublicRoot.PublicClass2.class}, result);
  }

  @Test(expected = NullPointerException.class)
  public void getReplacementMappingNull() {
    ConfigurationUtility.getReplacementMapping(null);
  }

  @Test
  public void getReplacementMappingEmpty() {
    List<Class<?>> classes = Collections.emptyList();
    Map<Class<?>, Class<?>> actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertTrue(actual.isEmpty());
  }

  @Test
  public void getReplacementMappingNoReplacements() {
    List<Class<?>> classes = classList(Original.class);
    Map<Class<?>, Class<?>> actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertNotNull(actual);
    assertTrue(actual.isEmpty());
    //
    classes = classList(Original.class, String.class);
    actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertNotNull(actual);
    assertTrue(actual.isEmpty());
    //
    classes = classList(Original.class, String.class, Long.class);
    actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertNotNull(actual);
    assertTrue(actual.isEmpty());
  }

  @Test
  public void getReplacementMapping() {
    List<Class<?>> classes = classList(Original.class, Replacement.class);
    Map<Class<?>, Class<?>> actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertSame(Replacement.class, actual.get(Original.class));
    //
    classes = classList(Replacement.class, Original.class);
    actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertSame(Replacement.class, actual.get(Original.class));
    //
    classes = classList(Original.class, String.class, Replacement.class);
    actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertSame(Replacement.class, actual.get(Original.class));
  }

  @Test
  public void getReplacementMappingReplacementHierarchy() {
    List<Class<?>> classes = classList(Original.class, Replacement.class, Replacement2.class);
    Map<Class<?>, Class<?>> actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertEquals(2, actual.size());
    assertSame(Replacement2.class, actual.get(Original.class));
    assertSame(Replacement2.class, actual.get(Replacement.class));
    //
    classes = classList(Replacement3.class, Replacement.class, Original.class, Replacement2.class);
    actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertEquals(3, actual.size());
    assertSame(Replacement3.class, actual.get(Original.class));
    assertSame(Replacement3.class, actual.get(Replacement.class));
    assertSame(Replacement3.class, actual.get(Replacement2.class));
  }

  @Test
  public void getReplacementMappingMultyReplacementHierarchy() {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(Original.class);
    classes.add(Replacement.class);
    classes.add(OtherReplacement.class);
    Map<Class<?>, Class<?>> actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertEquals(1, actual.size());
    Class<?> replacedClass = actual.get(Original.class);
    assertNotNull(replacedClass);
    assertTrue(replacedClass == Replacement.class || replacedClass == OtherReplacement.class);
  }

  @Test
  public void getReplacementMappingReplacementHierarchyNotCompletelyPartOfOriginalList() {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(Original.class);
    classes.add(String.class);
    classes.add(Replacement3.class);
    Map<Class<?>, Class<?>> actual = ConfigurationUtility.getReplacementMapping(classes);
    assertNotNull(actual);
    assertEquals(3, actual.size());
    assertSame(Replacement3.class, actual.get(Original.class));
    assertSame(Replacement3.class, actual.get(Replacement.class));
    assertSame(Replacement3.class, actual.get(Replacement2.class));
  }

  /**
   * For annotated classes the annotation value should be used as id.
   */
  @Test
  public void testAnnotatedClassId() {
    assertEquals(TEST_CLASS_ID, ConfigurationUtility.getAnnotatedClassIdWithFallback(ClassWithClassId.class));
    assertEquals(TEST_CLASS_ID, ConfigurationUtility.getAnnotatedClassIdWithFallback(ClassWithClassId.class, true));
  }

  /**
   * A replaced class should get the id of the class to be replaced
   */
  @Test
  public void testReplacementClassId() {
    assertEquals(TEST_CLASS_ID, ConfigurationUtility.getAnnotatedClassIdWithFallback(ReplacementClass.class));
    assertEquals(TEST_CLASS_ID, ConfigurationUtility.getAnnotatedClassIdWithFallback(ReplacementClass.class, true));
  }

  /**
   * If no annotation is available the correct fallback should be used.
   */
  @Test
  public void testAnnotatedClassIdFallback() {
    assertEquals(ClassWithoutClassId.class.getName(), ConfigurationUtility.getAnnotatedClassIdWithFallback(ClassWithoutClassId.class));
    assertEquals(ClassWithoutClassId.class.getSimpleName(), ConfigurationUtility.getAnnotatedClassIdWithFallback(ClassWithoutClassId.class, true));
  }

  public static List<Class<?>> classList(Class<?>... classes) {
    ArrayList<Class<?>> list = new ArrayList<Class<?>>();
    if (classes != null) {
      for (Class<?> c : classes) {
        list.add(c);
      }
    }
    return list;
  }

  @ClassId(TEST_CLASS_ID)
  class ClassWithClassId {
  }

  class ClassWithoutClassId {
  }

  @Replace
  class ReplacementClass extends ClassWithClassId {
  }

  public static class InnerA {
    public InnerB b = new InnerB();

    public class InnerB {
      public InnerC1 c1 = new InnerC1();
      public InnerC2 c2 = new InnerC2();

      public class InnerC1 extends AbstractInnerC {
      }

      public class InnerC2 extends AbstractInnerC {
      }
    }
  }

  public static abstract class AbstractInnerC {
    public InnerD d = new InnerD();

    public class InnerD {
      public InnerE e = new InnerE();

      public class InnerE {
      }
    }
  }

  @Order(10)
  public static class OrderStatic10 {
  }

  @Order(20)
  public static class OrderStatic20Dynamic0 implements IOrdered {
    @Override
    public double getOrder() {
      return 0;
    }
  }

  public static class OrderDynamic30 implements IOrdered {
    @Override
    public double getOrder() {
      return 30;
    }
  }

  @Replace
  public static class ReplaceOrderStatic10 extends OrderStatic10 implements IOrdered {
    @Override
    public double getOrder() {
      return 40;
    }
  }

  public static class Original {
  }

  @Replace
  public static class Replacement extends Original {
  }

  @Replace
  public static class Replacement2 extends Replacement {
  }

  @Replace
  public static class Replacement3 extends Replacement2 {
  }

  @Replace
  public static class OtherReplacement extends Original {
  }

  public class PublicRoot {
    public class PublicClass1 {
    }

    public class PublicClass2 {
    }

    public abstract class AbstractClass1 {
    }

    public abstract class AbstractClass2 {
    }
  }

}
