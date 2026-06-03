package org.opentripplanner.framework.transaction._model.a;

import javax.annotation.Nullable;

public record AUpdateSystemEvent(A newA, @Nullable A oldA) {}
