package org.opentripplanner.framework.transaction;

public interface TransactionalRepository<S, T> {
  S snapshot(Transaction transaction);
}
