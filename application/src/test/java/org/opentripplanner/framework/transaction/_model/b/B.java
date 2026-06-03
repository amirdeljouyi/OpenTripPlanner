package org.opentripplanner.framework.transaction._model.b;

import org.opentripplanner.framework.transaction._model.base.Entity;

public class B implements Entity {

  private final int id;
  private final String name;

  public B(Integer id, String name) {
    this.id = id;
    this.name = name;
  }

  public Integer id() {
    return id;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return "B{" + "id=" + id + ", name='" + name + '\'' + '}';
  }
}
