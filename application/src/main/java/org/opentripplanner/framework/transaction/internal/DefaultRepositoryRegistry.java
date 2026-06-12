package org.opentripplanner.framework.transaction.internal;

import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * Default implementation of {@link RepositoryRegistry}.
 *
 * <p>Wraps a {@link TransactionManager} to coordinate transactions across all registered
 * repositories. Each call to {@link #register(Object, RepositoryLifecycle)} creates a
 * {@link TransactionalRepository} internally and returns a {@link RepositoryHandle}, allowing
 * {@link org.opentripplanner.framework.transaction.internal.DefaultWriteContext} to obtain
 * mutable snapshot access via an internal cast without exposing it on the public
 * {@link RepositoryHandle} API.
 */
class DefaultRepositoryRegistry implements RepositoryRegistry {

  private final TransactionManager transactionManager = new TransactionManager();

  @Override
  public <S, M> RepositoryHandle<S, M> registerSnapshot(
    S initialSnapshot,
    RepositoryLifecycle<S, M> lifecycle
  ) {
    TransactionalRepository<S, M> repo = new TransactionalRepository<>(
      initialSnapshot,
      lifecycle,
      transactionManager
    );
    return new DefaultRepositoryHandle<>(repo);
  }

  @Override
  public <S, M> RepositoryHandle<S, M> registerRepository(
    M repository,
    RepositoryLifecycle<S, M> lifecycle
  ) {
    return registerSnapshot(lifecycle.freeze(repository), lifecycle);
  }

  @Override
  public TransactionScope scope() {
    return new DefaultTransactionScope(transactionManager.requestScopedTransaction());
  }

  /**
   * Returns the transaction manager for use during wiring of the
   * {@link UpdateManager}.
   */
  TransactionManager transactionManager() {
    return transactionManager;
  }
}
