package org.opentripplanner.framework.transaction._model.a;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

public class ARepository
  extends AbstractRepository<A>
  implements RepositoryLifecycle<ASnapshot, ARepository> {

  private List<Consumer<AUpdateSystemEvent>> updateListeners = new ArrayList<>();

  public ARepository() {
    super(new HashMap<>());
  }

  @Override
  public ARepository copyOnWrite(ASnapshot readOnlySnapshot) {
    return this;
  }

  @Override
  public ASnapshot freeze(ARepository mutableSnapshot) {
    return new ASnapshot(copyOfEntitiesById());
  }

  @Override
  public A add(A entity) {
    var oldEntity = super.add(entity);
    for (var it : updateListeners) {
      it.accept(new AUpdateSystemEvent(entity, oldEntity));
    }
    return oldEntity;
  }

  public void addUpdateListener(Consumer<AUpdateSystemEvent> updateListener) {
    updateListeners.add(updateListener);
  }
}
