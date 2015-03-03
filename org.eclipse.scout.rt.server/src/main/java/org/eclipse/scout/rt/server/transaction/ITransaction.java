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
package org.eclipse.scout.rt.server.transaction;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.services.common.jdbc.AbstractSqlTransactionMember;
import org.eclipse.scout.rt.server.transaction.internal.ActiveTransactionRegistry;
import org.eclipse.scout.rt.shared.services.common.processing.IServerProcessingCancelService;

/**
 * Whenever a remote service call is handled by the ServiceTunnelServlet it is dispatched to a
 * DefaultTransactionDelegate that runs a ITransaction as a ServerJob.
 * That transaction does {@link ActiveTransactionRegistry#register(ITransaction)} /
 * {@link ActiveTransactionRegistry#unregister(ITransaction)} with the requestId as the {@link ITransaction#getId()
 * transaction id}. Resources such as jdbc connections take part on the
 * transaction as {@link ITransactionMember}s.
 * Whenever a sql statement is run, it registers/unregisters on the
 * {@link AbstractSqlTransactionMember#registerActiveStatement(java.sql.Statement)} /
 * {@link AbstractSqlTransactionMember#unregisterActiveStatement(java.sql.Statement)}.
 * Thus canceling a {@link ITransaction#cancel()} also cancels all its members {@link ITransactionMember#cancel()} and
 * that cancels the (potentially) running statement.
 * A canceled transaction can only do a rollback and does not accept new members.
 *
 * @since 3.4
 */
public interface ITransaction {

  /**
   * The {@link ITransaction} which is currently associated with the current thread.
   */
  ThreadLocal<ITransaction> CURRENT = new ThreadLocal<>();

  /**
   * Default transaction-id if the transaction is not to be registered within the {@link IServerSession} and therefore
   * provides no 'cancellation' support.
   */
  long TX_ZERO_ID = 0L;

  /**
   * @deprecated use {@link #getId()} instead; will be removed in 5.2.0;
   */
  @Deprecated
  long getTransactionSequence();

  /**
   * This method returns the <code>id</code> to identify this transaction uniquely among the {@link IServerSession}.
   *
   * @return unique transaction <code>id</code> among the {@link IServerSession} or {@link ITransaction#TX_ZERO_ID} if
   *         not to be registered within the {@link IServerSession}; is primarily used to identify the transaction if
   *         the user likes to cancel a transaction.
   * @see IServerProcessingCancelService#cancel(long)
   */
  long getId();

  /**
   * register the member (even if the transaction is canceled)
   *
   * @throws ProcessingException
   *           with an {@link InterruptedException} when the transaction is canceled
   */
  void registerMember(ITransactionMember member) throws ProcessingException;

  ITransactionMember getMember(String memberId);

  ITransactionMember[] getMembers();

  void unregisterMember(ITransactionMember member);

  void unregisterMember(String memberId);

  boolean hasFailures();

  Throwable[] getFailures();

  void addFailure(Throwable t);

  /**
   * Two-phase commit
   * <p>
   * Temporary commits the transaction members
   * <p>
   *
   * @return true without any exception if the commit phase 1 was successful on all members.
   *         <p>
   *         Subsequently there will be a call to {@link #commitPhase2()} or {@link #rollback()}
   */
  boolean commitPhase1() throws ProcessingException;

  /**
   * commit phase 2 of the transaction members (commit phase 1 confirmation)
   */
  void commitPhase2();

  /**
   * rollback on the transaction members (commit phase 1 cancel and rollback)
   */
  void rollback();

  /**
   * release any members allocated by the transaction members
   */
  void release();

  /**
   * an external process tries to cancel the transaction
   *
   * @return true if cancel was successful and transaction was in fact canceled, false otherwise
   */
  boolean cancel();

  boolean isCancelled();

}