package org.opentripplanner.framework.transaction.internal;

import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.Transaction;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;

/**
 * A {@link RepositoryHandle} that also allowing the {@link DefaultWriteContext} to obtain mutable
 * repository.
 */
class DefaultRepositoryHandle<S, M> implements RepositoryHandle<S, M> {

  private final TransactionalRepository<S, M> repo;

  DefaultRepositoryHandle(TransactionalRepository<S, M> repo) {
    this.repo = repo;
  }

  @Override
  public S readOnlySnapshot(Transaction transaction) {
    return repo.snapshot(transaction);
  }

  Supplier<M> mutableSnapshot() {
    return repo.mutableSnapshot();
  }
}
