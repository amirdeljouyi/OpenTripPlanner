package org.opentripplanner.framework.transaction._model.a;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;

public class ARepository extends AbstractRepository<A> {

  public ARepository(Map<Integer, A> entitiesById) {
    super(entitiesById);
  }

  public ARepository() {
    this(new HashMap<>());
  }

  @Override
  public A add(A entity) {
    return super.add(entity);
  }

  ASnapshot freeze() {
    return new ASnapshot(copyOfEntitiesById());
  }
}
