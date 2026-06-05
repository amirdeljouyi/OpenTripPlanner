package org.opentripplanner.framework.transaction.internal;

import org.opentripplanner.framework.transaction.Transaction;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * Default request-scoped implementation of {@link TransactionScope}.
 *
 * <p>Captures the current {@link Transaction} at construction time and holds a strong reference
 * to it for its lifetime. This prevents the corresponding snapshot cache entries in the underlying
 * {@code WeakHashMap} from being garbage-collected while a request is active, and guarantees that
 * all {@link #snapshot(RepositoryHandle)} calls within the same scope resolve against the same
 * transaction.
 */
class DefaultTransactionScope implements TransactionScope {

  private final Transaction transaction;

  DefaultTransactionScope(Transaction transaction) {
    this.transaction = transaction;
  }

  @Override
  public <S> S snapshot(RepositoryHandle<S, ?> handle) {
    return handle.readOnlySnapshot(transaction);
  }

  @Override
  public String toString() {
    return "Scope(" + transaction + ')';
  }
}
