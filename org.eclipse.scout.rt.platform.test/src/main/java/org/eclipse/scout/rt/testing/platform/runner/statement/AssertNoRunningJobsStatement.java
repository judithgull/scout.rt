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
package org.eclipse.scout.rt.testing.platform.runner.statement;

import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.scout.rt.platform.filter.AlwaysFilter;
import org.eclipse.scout.rt.platform.filter.IFilter;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.util.Assertions;
import org.eclipse.scout.rt.platform.visitor.IVisitor;
import org.junit.runners.model.Statement;

/**
 * Statement to assert no running jobs.
 */
public class AssertNoRunningJobsStatement extends Statement {

  protected static final IFilter<IFuture<?>> ALL_JOBS_FILTER = new AlwaysFilter<IFuture<?>>();
  protected final Statement m_next;

  public AssertNoRunningJobsStatement(final Statement next) {
    m_next = Assertions.assertNotNull(next, "next statement must not be null");
  }

  @Override
  public void evaluate() throws Throwable {
    assertNoRunningJobs("Test not started because some jobs of previous tests did not complete yet.");
    try {
      m_next.evaluate();
    }
    finally {
      assertNoRunningJobs("Test failed because some jobs did not complete yet.");
    }
  }

  private void assertNoRunningJobs(final String message) {
    final Set<String> runningJobs = findRunningJobs();
    if (!runningJobs.isEmpty()) {
      fail(String.format("%s [jobs=%s]", message, runningJobs));
    }
  }

  private Set<String> findRunningJobs() {
    final Set<String> runningJobs = new HashSet<>();
    Jobs.getJobManager().visit(ALL_JOBS_FILTER, new IVisitor<IFuture<?>>() {

      @Override
      public boolean visit(final IFuture<?> future) {
        runningJobs.add(future.getJobInput().getName());
        return true;
      }
    });
    return runningJobs;
  }
}
