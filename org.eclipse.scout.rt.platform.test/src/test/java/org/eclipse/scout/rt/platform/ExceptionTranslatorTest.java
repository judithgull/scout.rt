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
package org.eclipse.scout.rt.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedAction;
import java.util.HashSet;

import javax.security.auth.Subject;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.security.SimplePrincipal;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.JobInput;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class ExceptionTranslatorTest {

  @Test
  public void testTranslate() {
    ExceptionTranslator exceptionTranslator = new ExceptionTranslator();

    ProcessingException pe1 = new ProcessingException();
    RuntimeException reWithPe = new RuntimeException(pe1);
    Exception e1 = new Exception();
    RuntimeException re1 = new RuntimeException();

    // test 'normal cases'
    assertSame(pe1, exceptionTranslator.translate(pe1));
    assertSame(pe1, exceptionTranslator.translate(reWithPe));
    assertSame(e1, exceptionTranslator.translate(e1).getCause());
    assertSame(re1, exceptionTranslator.translate(re1).getCause());

    // test 'UndeclaredThrowableException'
    UndeclaredThrowableException ute1 = new UndeclaredThrowableException(null);
    assertSame(ute1, exceptionTranslator.translate(ute1).getCause());

    UndeclaredThrowableException ute2 = new UndeclaredThrowableException(e1);
    assertSame(e1, exceptionTranslator.translate(ute2).getCause());

    UndeclaredThrowableException ute3 = new UndeclaredThrowableException(re1);
    assertSame(re1, exceptionTranslator.translate(ute3).getCause());

    UndeclaredThrowableException ute4 = new UndeclaredThrowableException(reWithPe);
    assertSame(reWithPe.getCause(), exceptionTranslator.translate(ute4));

    // test 'InvocationTargetException'
    InvocationTargetException ite1 = new InvocationTargetException(null);
    assertSame(ite1, exceptionTranslator.translate(ite1).getCause());

    InvocationTargetException ite2 = new InvocationTargetException(e1);
    assertSame(e1, exceptionTranslator.translate(ite2).getCause());

    InvocationTargetException ite3 = new InvocationTargetException(re1);
    assertSame(re1, exceptionTranslator.translate(ite3).getCause());

    InvocationTargetException ite4 = new InvocationTargetException(reWithPe);
    assertSame(reWithPe.getCause(), exceptionTranslator.translate(ite4));
  }

  @Test
  public void testJobContextMessage() throws Exception {
    final ExceptionTranslator exceptionTranslator = new ExceptionTranslator();

    // test context message with 'no job' and 'no subject'
    IFuture.CURRENT.remove();
    ProcessingException pe = Subject.doAs(null, new PrivilegedAction<ProcessingException>() {

      @Override
      public ProcessingException run() {
        return exceptionTranslator.translate(new ProcessingException());
      }
    });
    assertEquals(CollectionUtility.hashSet("identity=anonymous"), new HashSet<>(pe.getStatus().getContextMessages()));

    // test context message with 'anonymous job' and 'no subject'
    IFuture future = mock(IFuture.class);
    IFuture.CURRENT.set(future);
    when(future.getJobInput()).thenReturn(JobInput.fillEmpty());
    pe = Subject.doAs(null, new PrivilegedAction<ProcessingException>() {

      @Override
      public ProcessingException run() {
        return exceptionTranslator.translate(new ProcessingException());
      }
    });
    assertEquals(CollectionUtility.hashSet("identity=anonymous"), new HashSet<>(pe.getStatus().getContextMessages()));

    // test context message with 'job' and 'no subject'
    future = mock(IFuture.class);
    IFuture.CURRENT.set(future);
    when(future.getJobInput()).thenReturn(JobInput.fillEmpty().name("do-something"));
    pe = Subject.doAs(null, new PrivilegedAction<ProcessingException>() {

      @Override
      public ProcessingException run() {
        return exceptionTranslator.translate(new ProcessingException());
      }
    });
    assertEquals(CollectionUtility.hashSet("job=do-something", "identity=anonymous"), new HashSet<>(pe.getStatus().getContextMessages()));

    // test context message with 'job' and 'subject'
    future = mock(IFuture.class);
    IFuture.CURRENT.set(future);
    Subject subject = new Subject();
    subject.getPrincipals().add(new SimplePrincipal("anna"));
    subject.getPrincipals().add(new SimplePrincipal("john"));

    when(future.getJobInput()).thenReturn(JobInput.fillEmpty().id("7").name("do-something"));
    pe = Subject.doAs(subject, new PrivilegedAction<ProcessingException>() {

      @Override
      public ProcessingException run() {
        return exceptionTranslator.translate(new ProcessingException());
      }
    });
    assertEquals(CollectionUtility.hashSet("job=7:do-something", "identity=anna, john"), new HashSet<>(pe.getStatus().getContextMessages()));
  }
}