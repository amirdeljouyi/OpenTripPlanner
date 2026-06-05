package org.opentripplanner.framework.transaction.api;

public interface RepositoryLifecycle<S, M> {
  M copyOnWrite(S readOnlySnapshot);

  S freeze(M mutableSnapshot);
}
