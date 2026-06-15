package org.opentripplanner.framework.transaction._model.a;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

public class ARepository extends AbstractRepository<A> {

  private final List<Consumer<AUpdateSystemEvent>> updateListeners = new ArrayList<>();

  public ARepository(Map<Integer, A> entitiesById) {
    super(entitiesById);
  }

  public ARepository() {
    this(new HashMap<>());
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

  ASnapshot freeze() {
    return new ASnapshot(copyOfEntitiesById());
  }
}
