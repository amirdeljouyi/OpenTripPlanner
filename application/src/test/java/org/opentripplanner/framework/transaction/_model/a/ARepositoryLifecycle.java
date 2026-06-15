package org.opentripplanner.framework.transaction._model.a;

import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

/**
 * This life-cycle will create a new mutable repository for each transaction, and freeze it into a
 * snapshot when the transaction is committed. This support atomic commits and rollback in case a
 * task fails - after a partial update of the repository. To get atomic-commit every task must be
 * followed by a commit.
 */
public class ARepositoryLifecycle implements RepositoryLifecycle<ASnapshot, ARepository> {

  @Override
  public ARepository copyOnWrite(ASnapshot snapshot) {
    return snapshot.copyOnWrite();
  }

  @Override
  public ASnapshot freeze(ARepository repository) {
    return repository.freeze();
  }
}
