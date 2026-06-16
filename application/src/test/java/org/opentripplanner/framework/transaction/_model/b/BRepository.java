package org.opentripplanner.framework.transaction._model.b;

import java.util.HashMap;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

/**
 * This repository implements the {@link RepositoryLifecycle} interface, but does not create a new
 * mutable repository for each transaction. Instead, it returns the same instance for each
 * transaction and freezes it into a snapshot when the transaction is committed. This does not
 * support atomic commits and rollback in case a task fails, but is more memory efficient since
 * only the freeze action triggers copying the internal data structure.
 */
public class BRepository
  extends AbstractRepository<B>
  implements RepositoryLifecycle<BSnapshot, BRepository> {

  public BRepository() {
    super(new HashMap<>());
  }

  @Override
  public BRepository copyOnWrite(BSnapshot readOnlySnapshot) {
    return this;
  }

  @Override
  public BSnapshot freeze(BRepository mutableSnapshot) {
    return new BSnapshot(copyOfEntitiesById());
  }
}
