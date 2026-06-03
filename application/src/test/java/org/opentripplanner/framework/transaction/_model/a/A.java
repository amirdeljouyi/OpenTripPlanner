package org.opentripplanner.framework.transaction._model.a;

import org.opentripplanner.framework.transaction._model.base.Entity;

public class A implements Entity {

  private final int id;
  private final String name;

  public A(int id, String name) {
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
    return "A{" + "id=" + id + ", name='" + name + '\'' + '}';
  }
}
