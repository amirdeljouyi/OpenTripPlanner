package org.opentripplanner.framework.transaction._model.a;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;

public class ASnapshot extends AbstractRepository<A> {

  ASnapshot(Map<Integer, A> aById) {
    super(aById);
  }

  @Override
  public A add(A entity) {
    throw new UnsupportedOperationException("ASnapshot is immutable");
  }


  ARepository copyOnWrite() {
    return new ARepository(new HashMap<>(copyOfEntitiesById()));
  }
}
