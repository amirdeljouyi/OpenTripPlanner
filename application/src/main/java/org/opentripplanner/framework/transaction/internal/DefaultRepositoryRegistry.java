package org.opentripplanner.framework.transaction.internal;

import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.RepositoryHandle;
import org.opentripplanner.framework.transaction.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.RepositoryScope;
import org.opentripplanner.framework.transaction.Transaction;
import org.opentripplanner.framework.transaction.TransactionalRepository;
import org.opentripplanner.framework.transaction.UpdateManager;

/**
 * Default implementation of {@link RepositoryRegistry}.
 *
 * <p>Wraps a {@link InMemoryRepositoryTransactionManager} to coordinate transactions across all
 * registered repositories. Each call to {@link #register(Object, RepositoryLifecycle)} creates a
 * {@link InMemoryTransactionalRepository} internally and returns a {@link RepositoryHandle} that
 * also implements the package-private {@link WritableHandle} interface, allowing
 * {@link org.opentripplanner.framework.transaction.internal.DefaultWriteContext} to obtain
 * mutable snapshot access via an internal cast without exposing it on the public
 * {@link RepositoryHandle} API.
 */
class DefaultRepositoryRegistry implements RepositoryRegistry {

  private final InMemoryRepositoryTransactionManager transactionManager =
    new InMemoryRepositoryTransactionManager();

  @Override
  public <S, M> RepositoryHandle<S, M> register(
    S initialSnapshot,
    RepositoryLifecycle<S, M> lifecycle
  ) {
    TransactionalRepository<S, M> repo = new InMemoryTransactionalRepository<>(
      initialSnapshot,
      lifecycle,
      transactionManager
    );
    return new WritableRepositoryHandle<>(repo);
  }

  @Override
  public RepositoryScope scope() {
    return new DefaultRepositoryScope(transactionManager.requestScopedTransaction());
  }

  /**
   * Returns the transaction manager for use during wiring of the
   * {@link UpdateManager}.
   */
  InMemoryRepositoryTransactionManager transactionManager() {
    return transactionManager;
  }

  /**
   * A {@link RepositoryHandle} that also implements {@link WritableHandle}, allowing the
   * {@link org.opentripplanner.framework.transaction.internal.DefaultWriteContext} to obtain
   * mutable snapshot access via an internal cast.
   */
  private static class WritableRepositoryHandle<S, M>
    implements RepositoryHandle<S, M>, WritableHandle<M> {

    private final TransactionalRepository<S, M> repo;

    WritableRepositoryHandle(TransactionalRepository<S, M> repo) {
      this.repo = repo;
    }

    @Override
    public S readOnlySnapshot(Transaction transaction) {
      return repo.snapshot(transaction);
    }

    @Override
    public Supplier<M> mutableSnapshot() {
      return ((InMemoryTransactionalRepository<S, M>) repo).mutableSnapshot();
    }
  }
}
