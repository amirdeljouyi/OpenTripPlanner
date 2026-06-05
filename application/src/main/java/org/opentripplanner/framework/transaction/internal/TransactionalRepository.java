package org.opentripplanner.framework.transaction.internal;

import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.Transaction;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

class TransactionalRepository<S, M> {

  private final RepositoryLifecycle<S, M> lifecycle;
  private final Supplier<Transaction> transactionProvider;
  private final SnapshotCache<S> snapshotCash = new SnapshotCache<S>();
  private M mutableSnapshot;

  TransactionalRepository(
    S initialSnapshot,
    RepositoryLifecycle<S, M> lifecycle,
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

  Supplier<M> mutableSnapshot() {
    return this::currentMutableSnapshot;
  }

  void commit(Transaction currentTransaction, Transaction nextTransaction) {
    S snapshot = mutableSnapshot == null
      ? snapshotCash.get(currentTransaction)
      : lifecycle.freeze(mutableSnapshot);
    snapshotCash.put(nextTransaction, snapshot);
    this.mutableSnapshot = null;
  }

  private M currentMutableSnapshot() {
    if (mutableSnapshot == null) {
      this.mutableSnapshot = lifecycle.copyOnWrite(snapshot(transactionProvider.get()));
    }
    return mutableSnapshot;
  }
}
