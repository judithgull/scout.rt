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
package org.eclipse.scout.commons;

/**
 * Helper class to ensure the application's assumptions about expected values.
 * 
 * @since 5.1 (backported)
 */
public final class Assertions {

  private Assertions() {
    // private constructor for utility classes.
  }

  /**
   * Asserts the given value to be <code>null</code>.
   * 
   * @param value
   *          the value to be tested.
   * @return the given value if <code>null</code>.
   * @throws AssertionException
   *           if the given value is not <code>null</code>.
   */
  public static <T> T assertNull(final T value) {
    return assertNull(value, "expected 'null' object but was 'non-null'");
  }

  /**
   * Asserts the given value to be <code>null</code>.
   * 
   * @param value
   *          the value to be tested.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return the given value if <code>null</code>.
   * @throws AssertionException
   *           if the given value is not <code>null</code>.
   */
  public static <T> T assertNull(final T value, final String msg, final Object... msgArgs) {
    if (value != null) {
      fail(msg, msgArgs);
    }
    return value;
  }

  /**
   * Asserts the given value not to be <code>null</code>.
   * 
   * @param value
   *          the value to be tested.
   * @return the given value if not <code>null</code>.
   * @throws AssertionException
   *           if the given value is <code>null</code>.
   */
  public static <T> T assertNotNull(final T value) {
    return assertNotNull(value, "expected 'non-null' object but was 'null'");
  }

  /**
   * Asserts the given value not to be <code>null</code>.
   * 
   * @param value
   *          the value to be tested.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return the given value if not <code>null</code>.
   * @throws AssertionException
   *           if the given value is <code>null</code>.
   */
  public static <T> T assertNotNull(final T value, final String msg, final Object... msgArgs) {
    if (value == null) {
      fail(msg, msgArgs);
    }
    return value;
  }

  /**
   * Asserts the given value not to be <code>null</code> or <code>empty</code>.
   * 
   * @param value
   *          the value to be tested.
   * @return the given value if not <code>null</code> or <code>empty</code>.
   * @throws AssertionException
   *           if the given value is <code>null</code> or <code>empty</code>.
   */
  public static String assertNotNullOrEmpty(final String value) {
    if (value == null) {
      fail("expected 'non-null' String but was 'null'.");
    }
    else if (value.isEmpty()) {
      fail("expected 'non-empty' String but was 'empty'.");
    }
    return value;
  }

  /**
   * Asserts the given value not to be <code>null</code> or <code>empty</code>.
   * 
   * @param value
   *          the value to be tested.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return the given value if not <code>null</code> or <code>empty</code>.
   * @throws AssertionException
   *           if the given value is <code>null</code> or <code>empty</code>.
   */
  public static String assertNotNullOrEmpty(final String value, final String msg, final Object... msgArgs) {
    if (value == null || value.isEmpty()) {
      fail(msg, msgArgs);
    }
    return value;
  }

  /**
   * Asserts the given value to be <code>true</code>.
   * 
   * @param value
   *          the value to be tested.
   * @return always <code>true</code>.
   * @throws AssertionException
   *           if the given value is <code>false</code>.
   */
  public static boolean assertTrue(final boolean value) {
    return assertTrue(value, "expected 'true' but was 'false'.");
  }

  /**
   * Asserts the given value to be <code>true</code>.
   * 
   * @param value
   *          the value to be tested.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return always <code>true</code>.
   * @throws AssertionException
   *           if the given value is <code>false</code>.
   */
  public static boolean assertTrue(final boolean value, final String msg, final Object... msgArgs) {
    if (!value) {
      fail(msg, msgArgs);
    }
    return value;
  }

  /**
   * Asserts the given value to be <code>false</code>.
   * 
   * @param value
   *          the value to be tested.
   * @return always <code>false</code>.
   * @throws AssertionException
   *           if the given value is <code>true</code>.
   */
  public static boolean assertFalse(final boolean value) {
    return assertFalse(value, "expected 'false' but was 'true'.");
  }

  /**
   * Asserts the given value to be <code>false</code>.
   * 
   * @param value
   *          the value to be tested.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return always <code>false</code>.
   * @throws AssertionException
   *           if the given value is <code>true</code>.
   */
  public static boolean assertFalse(final boolean value, final String msg, final Object... msgArgs) {
    if (value) {
      fail(msg, msgArgs);
    }
    return value;
  }

  /**
   * Asserts <code>value1</code> to be equals with <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if equals with <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not equals with <code>value2</code>.
   */
  public static <T> T assertEquals(final T value1, final Object value2) {
    return assertEquals(value1, value2, "expected value1 to be equals with value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> to be equals with <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if equals with <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not equals with <code>value2</code>.
   */
  public static <T> T assertEquals(final T value1, final Object value2, final String msg, final Object... msgArgs) {
    if (!CompareUtility.equals(value1, value2)) {
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * Asserts <code>value1</code> not to be equals with <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if not equals with <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is equals with <code>value2</code>.
   */
  public static <T> T assertNotEquals(final T value1, final Object value2) {
    return assertNotEquals(value1, value2, "expected value1 to be equals with value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> not to be equals with <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if not equals with <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is equals with <code>value2</code>.
   */
  public static <T> T assertNotEquals(final T value1, final Object value2, final String msg, final Object... msgArgs) {
    if (CompareUtility.equals(value1, value2)) {
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * Asserts <code>value1</code> to be same as <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if same as <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not same as <code>value2</code>.
   */
  public static <T> T assertSame(final T value1, final Object value2) {
    return assertSame(value1, value2, "expected value1 to be equals with value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> to be same as <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if same as <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not same as <code>value2</code>.
   */
  public static <T> T assertSame(final T value1, final Object value2, final String msg, final Object... msgArgs) {
    if (value1 != value2) { //NOSONAR
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * Asserts <code>value1</code> not to be same as <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if not same as <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is same as <code>value2</code>.
   */
  public static <T> T assertNotSame(final T value1, final Object value2) {
    return assertNotSame(value1, value2, "expected value1 to be equals with value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> not to be same as <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if not same as <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is same as <code>value2</code>.
   */
  public static <T> T assertNotSame(final T value1, final Object value2, final String msg, final Object... msgArgs) {
    if (value1 == value2) { //NOSONAR
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * Asserts <code>value1</code> to be equal <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if equal <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not equal <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertEqual(final T value1, final T value2) {
    return assertEqual(value1, value2, "expected value1 to be equals with value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> to be equal <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if equal <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not equal <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertEqual(final T value1, final T value2, final String msg, final Object... msgArgs) {
    if (value1.compareTo(value2) != 0) {
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * Asserts <code>value1</code> to be greater <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if greater <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not greater <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertGreater(final T value1, final T value2) {
    return assertGreater(value1, value2, "expected value1 to be '>' value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> to be greater <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if greater <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not greater <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertGreater(final T value1, final T value2, final String msg, final Object... msgArgs) {
    if (value1.compareTo(value2) <= 0) {
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * Asserts <code>value1</code> to be greater or equals <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if greater or equal <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not greater or equals <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertGreaterOrEqual(final T value1, final T value2) {
    return assertGreaterOrEqual(value1, value2, "expected value1 to be '>=' value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> to be greater or equal <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if greater or equal <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not greater or equal <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertGreaterOrEqual(final T value1, final T value2, final String msg, final Object... msgArgs) {
    if (value1.compareTo(value2) < 0) {
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * Asserts <code>value1</code> to be less <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if less <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not less <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertLess(final T value1, final T value2) {
    return assertLess(value1, value2, "expected value1 to be '<' value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> to be less <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if less <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not less <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertLess(final T value1, final T value2, final String msg, final Object... msgArgs) {
    if (value1.compareTo(value2) >= 0) {
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * Asserts <code>value1</code> to be less or equal <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @return <code>value1</code> if less or equal <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not less or equal <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertLessOrEqual(final T value1, final T value2) {
    return assertLessOrEqual(value1, value2, "expected value1 to be '<=' value2 [value1=%s, value2=%s]", value1, value2);
  }

  /**
   * Asserts <code>value1</code> to be less or equal <code>value2</code>.
   * 
   * @param value1
   *          the value to be tested.
   * @param value2
   *          the value to be tested against.
   * @param msg
   *          message contained in the {@link AssertionException} in case of an assertion error.
   * @param msgArgs
   *          arguments to be used in the message.
   * @return <code>value1</code> if less or equal <code>value2</code>.
   * @throws AssertionException
   *           if <code>value1</code> is not less or equal <code>value2</code>.
   */
  public static <T extends Comparable<T>> T assertLessOrEqual(final T value1, final T value2, final String msg, final Object... msgArgs) {
    if (value1.compareTo(value2) > 0) {
      fail(msg, msgArgs);
    }
    return value1;
  }

  /**
   * To always throw an {@code AssertionException}.
   * 
   * @param msg
   *          the message describing the assertion.
   * @param msgArgs
   *          message arguments to be referenced in the given message by <code>%s</code>.
   */
  public static <T> T fail(final String msg, final Object... msgArgs) {
    final String message = (msg != null ? String.format(msg, msgArgs) : "n/a");
    throw new AssertionException(String.format("Assertion error: %s", message));
  }

  /**
   * Indicates an assertion error about the application's assumptions about expected values.
   */
  public static class AssertionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AssertionException(final String msg, final Object... msgArgs) {
      super(String.format(msg, msgArgs));
    }
  }
}
