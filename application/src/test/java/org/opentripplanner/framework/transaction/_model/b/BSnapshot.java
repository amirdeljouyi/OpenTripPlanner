package org.opentripplanner.framework.transaction._model.b;

import java.util.Map;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;

public class BSnapshot extends AbstractRepository<B> {

  public BSnapshot(Map<Integer, B> aById) {
    super(aById);
  }

  @Override
  public B add(B entity) {
    throw new UnsupportedOperationException("ASnapshot is immutable");
  }
}
