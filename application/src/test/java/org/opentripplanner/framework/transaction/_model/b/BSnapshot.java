package org.opentripplanner.framework.transaction._model.b;

import java.util.Map;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;

public class BSnapshot
  extends AbstractRepository<org.opentripplanner.framework.transaction._model.b.B> {

  BSnapshot(Map<Integer, org.opentripplanner.framework.transaction._model.b.B> aById) {
    super(aById);
  }

  @Override
  public org.opentripplanner.framework.transaction._model.b.B add(
    org.opentripplanner.framework.transaction._model.b.B entity
  ) {
    throw new UnsupportedOperationException("ASnapshot is immutable");
  }
}
