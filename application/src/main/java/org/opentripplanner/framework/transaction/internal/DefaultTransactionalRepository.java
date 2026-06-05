package org.opentripplanner.framework.transaction.internal;

import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.Transaction;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

class DefaultTransactionalRepository<S, T> {

  private final RepositoryLifecycle<S, T> lifecycle;
  private final Supplier<Transaction> transactionProvider;
  private final SnapshotCache<S> snapshotCash = new SnapshotCache<S>();
  private T mutableSnapshot;

  DefaultTransactionalRepository(
    S initialSnapshot,
    RepositoryLifecycle<S, T> lifecycle,
    TransactionManager manager
  ) {
    this.lifecycle = lifecycle;
    this.transactionProvider = manager.currentTransaction();
    snapshotCash.put(this.transactionProvider.get(), initialSnapshot);
    manager.register(this);
  }

  public S snapshot(Transaction transaction) {
    return snapshotCash.get(transaction);
  }

  Supplier<T> mutableSnapshot() {
    return this::currentMutableSnapshot;
  }

  void commit(Transaction currentTransaction, Transaction nextTransaction) {
    S snapshot = mutableSnapshot == null
      ? snapshotCash.get(currentTransaction)
      : lifecycle.freeze(mutableSnapshot);
    snapshotCash.put(nextTransaction, snapshot);
    this.mutableSnapshot = null;
  }

  private T currentMutableSnapshot() {
    if (mutableSnapshot == null) {
      this.mutableSnapshot = lifecycle.copyOnWrite(snapshot(transactionProvider.get()));
    }
    return mutableSnapshot;
  }
}
