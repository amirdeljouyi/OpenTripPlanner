package org.opentripplanner.framework.transaction._model.b;

import java.util.HashMap;
import org.opentripplanner.framework.transaction._model.a.A;
import org.opentripplanner.framework.transaction._model.a.AUpdateSystemEvent;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

public class BRepository
  extends AbstractRepository<org.opentripplanner.framework.transaction._model.b.B>
  implements RepositoryLifecycle<BSnapshot, BRepository> {

  public BRepository() {
    super(new HashMap<>());
  }

  @Override
  public BRepository copyOnWrite(
    org.opentripplanner.framework.transaction._model.b.BSnapshot readOnlySnapshot
  ) {
    return this;
  }

  @Override
  public org.opentripplanner.framework.transaction._model.b.BSnapshot freeze(
    BRepository mutableSnapshot
  ) {
    return new org.opentripplanner.framework.transaction._model.b.BSnapshot(copyOfEntitiesById());
  }

  public void aUpdatedHandler(AUpdateSystemEvent e) {
    A a = e.newA();
    add(new org.opentripplanner.framework.transaction._model.b.B(1_000, "B updated " + a.name()));
  }
}
