package org.opentripplanner.framework.transaction.internal;

import java.util.Objects;
import org.opentripplanner.framework.transaction.Transaction;

final class DefaultTransaction implements Transaction {

  private final long id;

  public DefaultTransaction(long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultTransaction that = (DefaultTransaction) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "TXN-" + id;
  }
}
