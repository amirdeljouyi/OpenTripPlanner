package org.opentripplanner.framework.transaction._model.a;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;

public class ASnapshot extends AbstractRepository<A> {

  private final List<Consumer<AUpdateSystemEvent>> updateListeners = new ArrayList<>();

  ASnapshot(Map<Integer, A> aById, List<Consumer<AUpdateSystemEvent>> updateListeners) {
    super(aById);
    this.updateListeners.addAll(updateListeners);
  }

  @Override
  public A add(A entity) {
    throw new UnsupportedOperationException("ASnapshot is immutable");
  }

  ARepository copyOnWrite() {
    return new ARepository(new HashMap<>(copyOfEntitiesById()), updateListeners);
  }
}
